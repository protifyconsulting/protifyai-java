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

package com.protify.ai.internal.provider.openai;

import com.protify.ai.internal.provider.openai.model.OpenAIOutputContent;
import com.protify.ai.internal.provider.openai.model.OpenAIOutputItem;
import com.protify.ai.internal.provider.openai.model.OpenAIResponseBody;
import com.protify.ai.internal.response.ProtifyAIResponse;
import com.protify.ai.internal.tool.ProtifyAIToolCall;
import com.protify.ai.internal.util.json.ProtifyJson;
import com.protify.ai.tool.AIToolCall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OpenAIResponse extends ProtifyAIResponse {

    private final OpenAIResponseBody body;

    public OpenAIResponse(boolean cachedResponse, String pipelineId, String correlationId,
                          String modelName, String rawResponse, OpenAIResponseBody body) {
        super(cachedResponse, pipelineId, correlationId, modelName, rawResponse);
        this.body = body;
    }

    @Override
    public String getCorrelationId() {
        return "";
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
        return body.getUsage() != null ? body.getUsage().getTotalTokens() : 0;
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
        if (body.getOutput() != null) {
            for (OpenAIOutputItem item : body.getOutput()) {
                if ("message".equals(item.getType()) && item.getContent() != null) {
                    for (OpenAIOutputContent block : item.getContent()) {
                        if (block.getText() != null) {
                            return block.getText();
                        }
                    }
                }
            }
        }
        return "";
    }

    @Override
    public String getStopReason() {
        return body.getStatus();
    }

    @Override
    public boolean hasToolCalls() {
        if (body.getOutput() == null) {
            return false;
        }
        for (OpenAIOutputItem item : body.getOutput()) {
            if ("function_call".equals(item.getType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AIToolCall> getToolCalls() {
        if (body.getOutput() == null) {
            return Collections.emptyList();
        }

        List<AIToolCall> toolCalls = new ArrayList<>();
        for (OpenAIOutputItem item : body.getOutput()) {
            if ("function_call".equals(item.getType())) {
                String id = item.getCallId() != null ? item.getCallId() : "";
                String name = item.getName() != null ? item.getName() : "";
                String argumentsJson = item.getArguments() != null ? item.getArguments() : "{}";

                Map<String, Object> arguments;
                try {
                    Object parsed = ProtifyJson.parse(argumentsJson).get("");
                    arguments = parsed instanceof Map ? (Map<String, Object>) parsed : Collections.emptyMap();
                } catch (Exception e) {
                    arguments = Collections.emptyMap();
                }

                toolCalls.add(new ProtifyAIToolCall(id, name, arguments, argumentsJson));
            }
        }
        return toolCalls;
    }
}
