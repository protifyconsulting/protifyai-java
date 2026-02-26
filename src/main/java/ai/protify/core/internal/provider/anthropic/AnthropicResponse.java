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

package ai.protify.core.internal.provider.anthropic;

import ai.protify.core.internal.provider.anthropic.model.AnthropicResponseBody;
import ai.protify.core.internal.provider.anthropic.model.AnthropicResponseContent;
import ai.protify.core.internal.response.ProtifyAIResponse;
import ai.protify.core.internal.tool.ProtifyAIToolCall;
import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.tool.AIToolCall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AnthropicResponse extends ProtifyAIResponse {

    private final AnthropicResponseBody body;

    public AnthropicResponse(boolean cachedResponse, String pipelineId, String correlationId,
                             String modelName, String rawResponse, AnthropicResponseBody body) {
        super(cachedResponse, pipelineId, correlationId, modelName, rawResponse);
        this.body = body;
    }

    @Override
    public String getResponseId() {
        return body.getId() != null ? body.getId() : "";
    }

    @Override
    public String getModelName() {
        return body.getModel() != null ? body.getModel() : "";
    }

    @Override
    public long getInputTokens() {
        return body.getUsage() != null ? body.getUsage().getInputTokens() : 0;
    }

    @Override
    public long getOutputTokens() {
        return body.getUsage() != null ? body.getUsage().getOutputTokens() : 0;
    }

    @Override
    public long getTotalTokens() {
        return getInputTokens() + getOutputTokens();
    }

    @Override
    public long getProcessingTimeMillis() {
        return 0;
    }

    @Override
    public boolean isCachedResponse() {
        return false;
    }

    @Override
    public String text() {
        if (body.getContent() != null) {
            for (AnthropicResponseContent block : body.getContent()) {
                if ("text".equals(block.getType()) && block.getText() != null) {
                    return block.getText();
                }
            }
        }
        return "";
    }

    @Override
    public String getStopReason() {
        return body.getStopReason();
    }

    @Override
    public boolean hasToolCalls() {
        return "tool_use".equals(getStopReason());
    }

    @Override
    public List<AIToolCall> getToolCalls() {
        if (body.getContent() == null) {
            return Collections.emptyList();
        }

        List<AIToolCall> toolCalls = new ArrayList<>();
        for (AnthropicResponseContent block : body.getContent()) {
            if ("tool_use".equals(block.getType())) {
                String id = block.getId() != null ? block.getId() : "";
                String name = block.getName() != null ? block.getName() : "";
                Map<String, Object> input = block.getInput() != null
                        ? block.getInput()
                        : Collections.emptyMap();
                String argumentsJson = ProtifyJson.toJsonMap(input);
                toolCalls.add(new ProtifyAIToolCall(id, name, input, argumentsJson));
            }
        }
        return toolCalls;
    }
}
