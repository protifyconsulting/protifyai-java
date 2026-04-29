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

package ai.protify.core.internal.util.http;

import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.config.Configuration;
import ai.protify.core.provider.AIProviderRequest;
import ai.protify.core.provider.mock.MockProvider;
import ai.protify.core.provider.mock.MockProviderRequest;
import ai.protify.core.resiliency.RetryPolicy;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ProtifyHttpClientStreamRetryTest {

    private HttpServer server;
    private int port;
    private final AtomicInteger requestCount = new AtomicInteger(0);

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        requestCount.set(0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Streaming retries on 503 then succeeds")
    void streamRetriesOn503ThenSucceeds() throws Exception {
        int failuresBeforeSuccess = 2;
        server.createContext("/", exchange -> {
            int count = requestCount.incrementAndGet();
            if (count <= failuresBeforeSuccess) {
                respondWithStatus(exchange, 503, "{\"error\":\"service unavailable\"}");
            } else {
                respondWithSseStream(exchange, List.of("hello", "world"));
            }
        });

        List<String> tokens = new ArrayList<>();
        AIProviderRequest request = buildRequest(retryPolicyWith(3));

        CompletableFuture<Void> future = ProtifyHttpClient.getInstance()
                .postStream(request, "http://127.0.0.1:" + port + "/",
                        tokens::add, () -> { });

        future.get(5, TimeUnit.SECONDS);

        assertEquals(failuresBeforeSuccess + 1, requestCount.get(),
                "Should have retried " + failuresBeforeSuccess + " times before success");
        assertEquals(List.of("hello", "world"), tokens);
    }

    @Test
    @DisplayName("Streaming exhausts retries and fails")
    void streamExhaustsRetriesAndFails() {
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            respondWithStatus(exchange, 503, "{\"error\":\"still busy\"}");
        });

        List<String> tokens = new ArrayList<>();
        AIProviderRequest request = buildRequest(retryPolicyWith(2));

        CompletableFuture<Void> future = ProtifyHttpClient.getInstance()
                .postStream(request, "http://127.0.0.1:" + port + "/",
                        tokens::add, () -> { });

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> future.get(5, TimeUnit.SECONDS));

        // 1 initial attempt + 2 retries = 3 total
        assertEquals(3, requestCount.get());
        assertTrue(tokens.isEmpty(), "No tokens should be emitted on failure");
        assertNotNull(ex.getCause());
    }

    @Test
    @DisplayName("Streaming with maxRetries=0 fails immediately on 503")
    void streamNoRetriesFailsImmediately() {
        server.createContext("/", exchange -> {
            requestCount.incrementAndGet();
            respondWithStatus(exchange, 503, "{\"error\":\"busy\"}");
        });

        AIProviderRequest request = buildRequest(RetryPolicy.DEFAULT); // maxRetries=0

        CompletableFuture<Void> future = ProtifyHttpClient.getInstance()
                .postStream(request, "http://127.0.0.1:" + port + "/",
                        token -> { }, () -> { });

        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        assertEquals(1, requestCount.get());
    }

    // --- helpers ---

    private static RetryPolicy retryPolicyWith(int maxRetries) {
        return RetryPolicy.builder()
                .maxRetries(maxRetries)
                .delayMillis(10L)
                .jitterMillis(0L)
                .maxDelayMillis(100L)
                .maxElapsedTimeMillis(5_000L)
                .retryOnHttpStatusCodes(Set.of(429, 500, 502, 503, 504, 408))
                .retryOnExceptions(Set.of(RuntimeException.class))
                .build();
    }

    private static AIProviderRequest buildRequest(RetryPolicy retryPolicy) {
        Map<AIConfigProperty, Object> props = new EnumMap<>(AIConfigProperty.class);
        props.put(AIConfigProperty.RETRY_POLICY, retryPolicy);
        props.put(AIConfigProperty.REQUEST_TIMEOUT_MS, 5000);
        props.put(AIConfigProperty.PROVIDER_API_KEY, "test-key");

        Configuration configuration = new Configuration(props);
        MockProvider provider = MockProvider.withResponse("unused");

        MockProviderRequest request = new MockProviderRequest();
        injectField(request, "configuration", configuration);
        injectField(request, "provider", provider);
        return request;
    }

    private static void injectField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set field " + fieldName, e);
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static void respondWithStatus(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void respondWithSseStream(HttpExchange exchange, List<String> tokens) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            sb.append("data: ").append(token).append("\n\n");
        }
        sb.append("data: [DONE]\n\n");

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}