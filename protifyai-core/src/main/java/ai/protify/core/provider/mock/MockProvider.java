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

package ai.protify.core.provider.mock;

import ai.protify.core.provider.AIProvider;
import ai.protify.core.provider.AIProviderClient;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.MimeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class MockProvider implements AIProvider {

    private final Queue<AIResponse> responseQueue = new ConcurrentLinkedQueue<>();
    private final List<MockProviderRequest> recordedRequests = new CopyOnWriteArrayList<>();
    private volatile AIResponse defaultResponse;
    private volatile Function<MockProviderRequest, AIResponse> responseFunction;
    private volatile long streamTokenDelayMillis;

    MockProvider(List<AIResponse> responses, AIResponse defaultResponse,
                 Function<MockProviderRequest, AIResponse> responseFunction,
                 long streamTokenDelayMillis) {
        this.responseQueue.addAll(responses);
        this.defaultResponse = defaultResponse;
        this.responseFunction = responseFunction;
        this.streamTokenDelayMillis = streamTokenDelayMillis;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MockProvider withResponse(String text) {
        return new Builder().defaultResponse(text).build();
    }

    public static MockProvider withResponse(AIResponse response) {
        return new Builder().defaultResponse(response).build();
    }

    public static MockProvider withResponseFunction(Function<MockProviderRequest, AIResponse> responseFunction) {
        return new Builder().responseFunction(responseFunction).build();
    }

    // --- AIProvider implementation ---

    @Override
    public String getName() {
        return "Mock";
    }

    @Override
    public Map<String, String> getHeaders(String credential) {
        return Map.of("Content-Type", "application/json");
    }

    @Override
    @SuppressWarnings("java:S1452")
    public Class<? extends AIProviderClient<?>> getProviderClientType() {
        return MockProviderClient.class;
    }

    @Override
    public boolean isMimeTypeSupported(MimeType mimeType) {
        return true;
    }

    @Override
    public String getApiKeyVarName() {
        return "MOCK_API_KEY";
    }

    // --- Response management ---

    public void enqueueResponse(String text) {
        responseQueue.add(MockResponse.of(text));
    }

    public void enqueueResponse(AIResponse response) {
        responseQueue.add(response);
    }

    public void setDefaultResponse(String text) {
        this.defaultResponse = MockResponse.of(text);
    }

    public void setDefaultResponse(AIResponse response) {
        this.defaultResponse = response;
    }

    public void setResponseFunction(Function<MockProviderRequest, AIResponse> responseFunction) {
        this.responseFunction = responseFunction;
    }

    public void setStreamTokenDelayMillis(long millis) {
        this.streamTokenDelayMillis = millis;
    }

    long getStreamTokenDelayMillis() {
        return streamTokenDelayMillis;
    }

    AIResponse nextResponse(MockProviderRequest request) {
        AIResponse queued = responseQueue.poll();
        if (queued != null) {
            return queued;
        }
        if (responseFunction != null) {
            return responseFunction.apply(request);
        }
        if (defaultResponse != null) {
            return defaultResponse;
        }
        return MockResponse.of("");
    }

    // --- Request recording ---

    void recordRequest(MockProviderRequest request) {
        recordedRequests.add(request);
    }

    public List<MockProviderRequest> getRecordedRequests() {
        return Collections.unmodifiableList(recordedRequests);
    }

    public MockProviderRequest getLastRequest() {
        if (recordedRequests.isEmpty()) {
            return null;
        }
        return recordedRequests.get(recordedRequests.size() - 1);
    }

    public int getRequestCount() {
        return recordedRequests.size();
    }

    public void clearRecordedRequests() {
        recordedRequests.clear();
    }

    public void reset() {
        responseQueue.clear();
        recordedRequests.clear();
        defaultResponse = null;
        responseFunction = null;
    }

    @Override
    public String toString() {
        return "AIProvider{name='Mock'}";
    }

    // --- Builder ---

    public static class Builder {

        private final List<AIResponse> responses = new ArrayList<>();
        private AIResponse defaultResponse;
        private Function<MockProviderRequest, AIResponse> responseFunction;
        private long streamTokenDelayMillis;

        public Builder response(String text) {
            responses.add(MockResponse.of(text));
            return this;
        }

        public Builder response(AIResponse response) {
            responses.add(response);
            return this;
        }

        public Builder defaultResponse(String text) {
            this.defaultResponse = MockResponse.of(text);
            return this;
        }

        public Builder defaultResponse(AIResponse response) {
            this.defaultResponse = response;
            return this;
        }

        public Builder responseFunction(Function<MockProviderRequest, AIResponse> responseFunction) {
            this.responseFunction = responseFunction;
            return this;
        }

        public Builder streamTokenDelayMillis(long millis) {
            this.streamTokenDelayMillis = millis;
            return this;
        }

        public MockProvider build() {
            return new MockProvider(responses, defaultResponse, responseFunction, streamTokenDelayMillis);
        }
    }
}
