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

package com.protify.ai.internal.config;

import com.protify.ai.internal.SupportedModel;
import com.protify.ai.provider.AIProvider;
import com.protify.ai.resiliency.RetryPolicy;

/*  Master enum of all properties available that can be set to
    influence a client request's behavior.

    Also has enum sets for different things that can be configured.
    This keeps the configuration's types consistent across the library.
 */
public enum AIConfigProperty {
    PROVIDER_API_KEY("providers.apiKey", true, null, String.class),
    PROTIFY_API_KEY("providers.protifyApiKey", true, null, String.class),
    API_KEY_URL("providers.apiKeyUrl", false, null, String.class),
    API_KEY_URL_TIMEOUT_MS("providers.apiKeyUrlTimeoutMs", false, 5000, Integer.class),
    API_KEY_CACHE_TTL_SECS("providers.apiKeyCacheTtlSecs", false, 300, Integer.class),

    MODEL("clients.model", false, null, SupportedModel.class),
    MODEL_PROVIDER("clients.modelProvider", false, null, AIProvider.class),
    MODEL_EXPLICIT_VERSION("clients.modelExplicitVersion", false, null, String.class),

    REQUEST_TIMEOUT_MS("request.timeoutMillis", false, 60000, Integer.class),

    RETRY_POLICY( "retryPolicy", false, null, RetryPolicy.class),

    RETRY_MAX_RETRIES("request.retryPolicy.maxRetries", false, null, Integer.class),
    RETRY_DELAY_MS("request.retryPolicy.delayMillis", false, null, Long.class),
    RETRY_JITTER_MS("request.retryPolicy.jitterMillis", false, null, Long.class),
    RETRY_MAX_DELAY_MS("request.retryPolicy.maxDelayMillis", false, null, Long.class),
    RETRY_MAX_ELAPSED_TIME_MS("request.retryPolicy.maxElapsedTimeMillis", false, null, Long.class),
    RETRY_BACKOFF_STRATEGY("request.retryPolicy.backoffStrategy", false, null, String.class),
    RETRY_ON_HTTP_STATUS("request.retryPolicy.retryOnHttpStatusCodes", false, null, String.class),
    RETRY_ON_EXCEPTIONS("request.retryPolicy.retryOnExceptions", false, null, String.class),
    RETRY_RESPECT_RETRY_AFTER("request.retryPolicy.respectRetryAfter", false, null, Boolean.class),

    RESPONSE_CACHE_MAX_ENTRIES("response.cache.maxEntries", false, 1000, Integer.class),
    RESPONSE_CACHE_TTL_SECS("response.cache.ttlSecs", false, 3600, Integer.class),

    OVERRIDE_PIPELINE_CONFIG("overridePipelineConfig", false, false, Boolean.class),

    MAX_OUTPUT_TOKENS("clientDefaults.maxOutputTokens", false, null, Integer.class),
    TEMPERATURE("clientDefaults.temperature", false, null, Double.class),
    TOP_P("clientDefaults.topP", false, null, Double.class),
    TOP_K("clientDefaults.topK", false, null, Integer.class),
    INSTRUCTIONS("clientDefaults.instructions", false, null, String.class),
    PRETTY_PRINT_JSON("logging.json.prettyPrint", false, false, Boolean.class),
    LOG_REQUESTS("logging.logRequests", false, false, Boolean.class),
    LOG_TRUNCATE_LARGE_INPUT("logging.truncateLargeInput", false, true, Boolean.class),
    LOG_RESPONSES("logging.logResponses", false, false, Boolean.class);

    private final String name;
    private final boolean secret;
    private final Object defaultValue;
    private final Class<?> type;

    AIConfigProperty(String name, boolean secret, Object value, Class<?> type) {
        this.name = name;
        this.secret = secret;
        this.defaultValue = value;
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public boolean isSecret() {
        return secret;
    }

    public <T> T getDefaultValue(Class<T> type) {
        if (defaultValue == null) return null;
        return type.cast(defaultValue);
    }

    public Class<?> getType() {
        return type;
    }
}
