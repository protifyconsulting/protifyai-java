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

import ai.protify.core.response.AIResponse;
import ai.protify.core.tool.AIToolCall;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MockResponse implements AIResponse {

    private final String text;
    private final String modelName;
    private final String responseId;
    private final long inputTokens;
    private final long outputTokens;
    private final long processingTimeMillis;
    private final String providerResponse;
    private final String reasoningContent;
    private final String stopReason;
    private final List<AIToolCall> toolCalls;

    MockResponse(String text, String modelName, String responseId,
                 long inputTokens, long outputTokens, long processingTimeMillis,
                 String providerResponse, String reasoningContent, String stopReason,
                 List<AIToolCall> toolCalls) {
        this.text = text;
        this.modelName = modelName;
        this.responseId = responseId != null ? responseId : UUID.randomUUID().toString();
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.processingTimeMillis = processingTimeMillis;
        this.providerResponse = providerResponse;
        this.reasoningContent = reasoningContent;
        this.stopReason = stopReason;
        this.toolCalls = toolCalls != null ? toolCalls : Collections.emptyList();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MockResponse of(String text) {
        return new Builder().text(text).build();
    }

    @Override
    public String text() {
        return text;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public String getResponseId() {
        return responseId;
    }

    @Override
    public String getCorrelationId() {
        return "";
    }

    @Override
    public String getPipelineId() {
        return "";
    }

    @Override
    public long getInputTokens() {
        return inputTokens;
    }

    @Override
    public long getOutputTokens() {
        return outputTokens;
    }

    @Override
    public long getTotalTokens() {
        return inputTokens + outputTokens;
    }

    @Override
    public long getProcessingTimeMillis() {
        return processingTimeMillis;
    }

    @Override
    public boolean isCachedResponse() {
        return false;
    }

    @Override
    public String getProviderResponse() {
        return providerResponse != null ? providerResponse : "";
    }

    @Override
    public String getReasoningContent() {
        return reasoningContent;
    }

    @Override
    public String getStopReason() {
        return stopReason;
    }

    @Override
    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    @Override
    public List<AIToolCall> getToolCalls() {
        return toolCalls;
    }

    public static class Builder {

        private String text = "";
        private String modelName = "mock-model";
        private String responseId;
        private long inputTokens;
        private long outputTokens;
        private long processingTimeMillis;
        private String providerResponse;
        private String reasoningContent;
        private String stopReason;
        private List<AIToolCall> toolCalls;

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder responseId(String responseId) {
            this.responseId = responseId;
            return this;
        }

        public Builder inputTokens(long inputTokens) {
            this.inputTokens = inputTokens;
            return this;
        }

        public Builder outputTokens(long outputTokens) {
            this.outputTokens = outputTokens;
            return this;
        }

        public Builder processingTimeMillis(long processingTimeMillis) {
            this.processingTimeMillis = processingTimeMillis;
            return this;
        }

        public Builder providerResponse(String providerResponse) {
            this.providerResponse = providerResponse;
            return this;
        }

        public Builder reasoningContent(String reasoningContent) {
            this.reasoningContent = reasoningContent;
            return this;
        }

        public Builder stopReason(String stopReason) {
            this.stopReason = stopReason;
            return this;
        }

        public Builder toolCalls(List<AIToolCall> toolCalls) {
            this.toolCalls = toolCalls;
            return this;
        }

        public MockResponse build() {
            return new MockResponse(text, modelName, responseId,
                    inputTokens, outputTokens, processingTimeMillis,
                    providerResponse, reasoningContent, stopReason, toolCalls);
        }
    }
}
