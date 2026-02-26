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

package com.protify.ai.internal.pipeline;

import com.protify.ai.response.AIResponse;

public class PipelineAIResponse implements AIResponse {

    private final String text;

    public PipelineAIResponse(String text) {
        this.text = text;
    }

    public static PipelineAIResponse of(String text) {
        return new PipelineAIResponse(text);
    }

    @Override
    public String getProviderResponse() {
        return "Custom Response";
    }

    @Override
    public String text() {
        return text;
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
    public String getResponseId() {
        return "";
    }

    @Override
    public String getModelName() {
        return "";
    }

    @Override
    public long getInputTokens() {
        return 0;
    }

    @Override
    public long getOutputTokens() {
        return 0;
    }

    @Override
    public long getTotalTokens() {
        return 0;
    }

    @Override
    public long getProcessingTimeMillis() {
        return 0;
    }

    @Override
    public boolean isCachedResponse() {
        return false;
    }
}
