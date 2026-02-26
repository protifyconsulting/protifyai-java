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

package ai.protify.core.internal.pipeline;

import ai.protify.core.pipeline.AIPipelineResponse;
import ai.protify.core.response.AIResponse;
import ai.protify.core.tool.AIToolCall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProtifyAIPipelineResponse implements AIPipelineResponse {

    private final AIResponse finalResponse;
    private final List<AIResponse> stepResponses;

    public ProtifyAIPipelineResponse(AIResponse finalResponse, List<AIResponse> stepResponses) {
        this.finalResponse = finalResponse;
        this.stepResponses = Collections.unmodifiableList(new ArrayList<>(stepResponses));
    }

    // --- Delegate to final response ---

    @Override
    public String text() {
        return finalResponse.text();
    }

    @Override
    public String getCorrelationId() {
        return finalResponse.getCorrelationId();
    }

    @Override
    public String getPipelineId() {
        return finalResponse.getPipelineId();
    }

    @Override
    public String getResponseId() {
        return finalResponse.getResponseId();
    }

    @Override
    public String getModelName() {
        return finalResponse.getModelName();
    }

    @Override
    public long getInputTokens() {
        return finalResponse.getInputTokens();
    }

    @Override
    public long getOutputTokens() {
        return finalResponse.getOutputTokens();
    }

    @Override
    public long getTotalTokens() {
        return finalResponse.getTotalTokens();
    }

    @Override
    public long getProcessingTimeMillis() {
        return finalResponse.getProcessingTimeMillis();
    }

    @Override
    public boolean isCachedResponse() {
        return finalResponse.isCachedResponse();
    }

    @Override
    public String getProviderResponse() {
        return finalResponse.getProviderResponse();
    }

    @Override
    public boolean hasToolCalls() {
        return finalResponse.hasToolCalls();
    }

    @Override
    public List<AIToolCall> getToolCalls() {
        return finalResponse.getToolCalls();
    }

    @Override
    public String getStopReason() {
        return finalResponse.getStopReason();
    }

    // --- Pipeline-level aggregation ---

    @Override
    public List<AIResponse> getStepResponses() {
        return stepResponses;
    }

    @Override
    public int getStepCount() {
        return stepResponses.size();
    }

    @Override
    public AIResponse getStepResponse(int index) {
        return stepResponses.get(index);
    }

    @Override
    public long getTotalInputTokens() {
        long total = 0;
        for (AIResponse step : stepResponses) {
            total += step.getInputTokens();
        }
        return total;
    }

    @Override
    public long getTotalOutputTokens() {
        long total = 0;
        for (AIResponse step : stepResponses) {
            total += step.getOutputTokens();
        }
        return total;
    }

    @Override
    public long getTotalTokensUsed() {
        long total = 0;
        for (AIResponse step : stepResponses) {
            total += step.getTotalTokens();
        }
        return total;
    }

    @Override
    public long getTotalProcessingTimeMillis() {
        long total = 0;
        for (AIResponse step : stepResponses) {
            total += step.getProcessingTimeMillis();
        }
        return total;
    }
}
