/*
 * Copyright(c) 2026 Protify Consulting LLC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.protify.ai.internal.util.http;

import com.protify.ai.internal.config.AIConfigProperty;
import com.protify.ai.internal.config.BaseConfiguration;
import com.protify.ai.internal.config.Configuration;
import com.protify.ai.internal.config.CredentialHelperFactory;
import com.protify.ai.internal.exception.*;
import com.protify.ai.internal.exception.TimeoutException;
import com.protify.ai.internal.util.FileUtil;
import com.protify.ai.internal.util.Logger;
import com.protify.ai.internal.util.LoggerFactory;
import com.protify.ai.provider.AIProvider;
import com.protify.ai.provider.AIProviderRequest;
import com.protify.ai.resiliency.RetryPolicy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * A thread-safe, singleton HTTP client for Protify AI, featuring built-in
 * LRU caching and thundering-herd protection without external dependencies.
 */
public class ProtifyHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtifyHttpClient.class.getName());

    @SuppressWarnings({"java:S3077"})
    private static volatile ProtifyHttpClient instance;

    // Cache with true LRU (access-order) and fixed size limit
    private final Map<String, CacheEntry> cache;
    // Tracks requests currently over the wire to prevent duplicate calls for the same payload
    private final Map<String, CompletableFuture<ProtifyHttpResponse>> inFlight = new ConcurrentHashMap<>();

    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    private final long ttlMillis;

    public static void initialize() {
        getInstance();
    }

    private ProtifyHttpClient(long ttlMillis, int maxCacheSize) {
        this.ttlMillis = ttlMillis;

        // true = access-order (LRU). removeEldestEntry ensures we never exceed maxCacheSize.
        Map<String, CacheEntry> internalMap = new LinkedHashMap<>(maxCacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > maxCacheSize;
            }
        };
        this.cache = Collections.synchronizedMap(internalMap);

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        int threadPoolSize = Math.min(4, Math.max(2, Runtime.getRuntime().availableProcessors() - 2));
        this.scheduler = Executors.newScheduledThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("protify-http-scheduler");
            return t;
        });

        // Background maintenance task for TTL expiration
        this.scheduler.scheduleAtFixedRate(this::evictExpired, 1, 1, TimeUnit.MINUTES);
        LOGGER.info("ProtifyHttpClient initialized with cache TTL: " +
                "{} seconds, max cache size: {}", TimeUnit.MILLISECONDS.toSeconds(ttlMillis), maxCacheSize);
    }

    public static ProtifyHttpClient getInstance() {
        if (instance == null) {
            synchronized (ProtifyHttpClient.class) {
                if (instance == null) {
                    BaseConfiguration config = BaseConfiguration.getInstance();
                    Integer ttlProp = config.getProperty(AIConfigProperty.RESPONSE_CACHE_TTL_SECS);
                    int cacheTTL = (ttlProp != null) ? ttlProp : 3600;

                    Integer maxCacheSizeProp= config.getProperty(AIConfigProperty.RESPONSE_CACHE_MAX_ENTRIES);
                    int maxCacheSize = (maxCacheSizeProp != null) ? maxCacheSizeProp : 1000;

                    instance = new ProtifyHttpClient(TimeUnit.SECONDS.toMillis(cacheTTL), maxCacheSize);
                }
            }
        }
        return instance;
    }

    /**
     * Initializes the singleton with specific configurations.
     * Call this during application startup before the first getInstance().
     */
    public static void initialize(long ttlMillis, int maxCacheSize) {
        synchronized (ProtifyHttpClient.class) {
            instance = new ProtifyHttpClient(ttlMillis, maxCacheSize);
        }
    }

    public ProtifyHttpResponse post(AIProviderRequest request, String uri) {
        try {
            return postAsync(request, uri).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
            throw new ProtifyApiException("Failed to execute request", e.getCause());
        }
    }

    public CompletableFuture<ProtifyHttpResponse> postAsync(AIProviderRequest request, String uri) {

        Configuration configuration = request.getConfiguration();
        AIProvider provider = request.getProvider();
        String credential = CredentialHelperFactory.getInstance().getCredential(provider, configuration);
        String jsonBody = request.toJson();

        String hash = FileUtil.computeSHA256(jsonBody);

        // 1. Check local cache (get() updates access order for LRU)
        CacheEntry entry = cache.get(hash);
        if (entry != null) {
            if (!entry.isExpired(ttlMillis)) {
                return CompletableFuture.completedFuture(new ProtifyHttpResponse(true,
                        entry.response.getResponseBody(), entry.response.getStatusCode(), 0));
            } else {
                cache.remove(hash); // Proactive removal
            }
        }

        // 2. Handle in-flight requests to prevent "Thundering Herd"
        return inFlight.computeIfAbsent(hash, h -> {
            long startTime = System.currentTimeMillis();
            LOGGER.debug("No cached response, sending async request to {}", uri);

            int timeoutMillis = configuration.getProperty(AIConfigProperty.REQUEST_TIMEOUT_MS);
            RetryPolicy retryPolicy = configuration.getProperty(AIConfigProperty.RETRY_POLICY);
            return internalPostWithRetryAsync(provider, credential, uri, jsonBody, timeoutMillis, retryPolicy, 0)
                    .thenApply(response -> {
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        ProtifyHttpResponse res = new ProtifyHttpResponse(false, response.body(), response.statusCode(), elapsedTime);

                        if (response.statusCode() == 200) {
                            cache.put(hash, new CacheEntry(res));
                        }
                        return res;
                    })
                    .whenComplete((res, ex) -> inFlight.remove(hash));
        });
    }

    private void evictExpired() {
        synchronized (cache) {
            cache.values().removeIf(entry -> entry.isExpired(ttlMillis));
        }
    }

    private CompletableFuture<HttpResponse<String>> internalPostWithRetryAsync(
            AIProvider provider, String credential, String uri, String jsonBody, long timeoutMillis, RetryPolicy retryPolicy, int attempt) {

        return internalPostAsync(provider, credential, uri, jsonBody, timeoutMillis)
                .handle((response, ex) -> {
                    if (ex == null && response.statusCode() == 200) return CompletableFuture.completedFuture(response);

                    Exception error = (ex != null) ? (Exception) ex : translateStatusToException(response.statusCode());

                    if (attempt < retryPolicy.getMaxRetries() && shouldRetry(error, retryPolicy)) {
                        LOGGER.info( "Attempt {} failed, retrying in {}ms: {}",
                                attempt + 1, retryPolicy.getDelayMillis(), error.getMessage());

                        return delay(retryPolicy.getDelayMillis())
                                .thenCompose(v -> internalPostWithRetryAsync(provider, credential, uri, jsonBody, timeoutMillis, retryPolicy, attempt + 1));
                    }

                    return CompletableFuture.<HttpResponse<String>>failedFuture(
                            error instanceof RuntimeException ? (RuntimeException) error : new ProtifyApiException(error.getMessage(), error));
                }).thenCompose(f -> f);
    }

    private CompletableFuture<HttpResponse<String>> internalPostAsync(AIProvider provider, String credential, String uri, String jsonBody, long timeoutMillis) {
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(Duration.ofMillis(timeoutMillis))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        provider.getHeaders(credential).forEach(reqBuilder::header);
        HttpRequest request = reqBuilder.build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    private CompletableFuture<Void> delay(long millis) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        scheduler.schedule(() -> future.complete(null), millis, TimeUnit.MILLISECONDS);
        return future;
    }

    private boolean shouldRetry(Exception e, RetryPolicy policy) {
        /*
        if (policy.getIgnoreExceptions() != null && policy.getIgnoreExceptions().stream().anyMatch(clz -> clz.isInstance(e))) return false;
        if (policy.getRetryOnExceptions() == null || policy.getRetryOnExceptions().isEmpty()) return true;
        return policy.getRetryOnExceptions().stream().anyMatch(clz -> clz.isInstance(e));

         */
        return true;
    }

    public CompletableFuture<Void> postStream(AIProviderRequest request, String uri,
                                               Consumer<String> onEvent, Runnable onComplete) {

        Configuration configuration = request.getConfiguration();
        AIProvider provider = request.getProvider();
        String credential = CredentialHelperFactory.getInstance().getCredential(provider, configuration);
        String jsonBody = request.toJson();

        int timeoutMillis = configuration.getProperty(AIConfigProperty.REQUEST_TIMEOUT_MS);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(Duration.ofMillis(timeoutMillis))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        provider.getHeaders(credential).forEach(reqBuilder::header);
        HttpRequest httpRequest = reqBuilder.build();

        SSELineParser parser = new SSELineParser((event, data) -> onEvent.accept(data));

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        throw translateStatusToException(response.statusCode());
                    }
                    response.body().forEach(parser::feedLine);
                    parser.finish();
                    onComplete.run();
                });
    }

    private RuntimeException translateStatusToException(int statusCode) {
        switch (statusCode) {
            case 401: case 403: return new AccessDeniedException("Access denied: " + statusCode);
            case 408: case 504: case 502: return new TimeoutException("Timeout: " + statusCode);
            case 429: return new RateLimitExceededException("Rate limit: " + statusCode);
            case 400: return new BadRequestException("Bad request: " + statusCode);
            case 404: return new NotFoundException("Not found: " + statusCode);
            case 503: return new ServiceUnavailableException("Unavailable: " + statusCode);
            case 529: return new ServiceOverloadedException("Overloaded: " + statusCode);
            default: return new ServiceException("HTTP failure: " + statusCode);
        }
    }

    private static class CacheEntry {
        final ProtifyHttpResponse response;
        final long createdAt;

        CacheEntry(ProtifyHttpResponse response) {
            this.response = response;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired(long ttlMillis) {
            return (System.currentTimeMillis() - createdAt) > ttlMillis;
        }
    }
}
