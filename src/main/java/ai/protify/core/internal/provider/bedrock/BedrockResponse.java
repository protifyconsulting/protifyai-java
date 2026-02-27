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

package ai.protify.core.internal.provider.bedrock;

import ai.protify.core.internal.provider.bedrock.model.BedrockContentBlock;
import ai.protify.core.internal.provider.bedrock.model.BedrockMessage;
import ai.protify.core.internal.provider.bedrock.model.BedrockResponseBody;
import ai.protify.core.internal.response.ProtifyAIResponse;
import ai.protify.core.internal.tool.ProtifyAIToolCall;
import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.tool.AIToolCall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BedrockResponse extends ProtifyAIResponse {

    private final BedrockResponseBody body;

    public BedrockResponse(boolean cachedResponse, String pipelineId, String correlationId,
                           String modelName, String rawResponse, BedrockResponseBody body) {
        super(cachedResponse, pipelineId, correlationId, modelName, rawResponse);
        this.body = body;
    }

    @Override
    public String getResponseId() {
        return super.getResponseId();
    }

    @Override
    public String getModelName() {
        return super.getModelName();
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
    public String text() {
        BedrockMessage message = getOutputMessage();
        if (message != null && message.getContent() != null) {
            StringBuilder sb = new StringBuilder();
            for (BedrockContentBlock block : message.getContent()) {
                if (block.getText() != null) {
                    sb.append(block.getText());
                }
            }
            if (sb.length() > 0) {
                return sb.toString();
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
        return "tool_use".equals(body.getStopReason());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AIToolCall> getToolCalls() {
        BedrockMessage message = getOutputMessage();
        if (message == null || message.getContent() == null) {
            return Collections.emptyList();
        }

        List<AIToolCall> toolCalls = new ArrayList<>();
        for (BedrockContentBlock block : message.getContent()) {
            if (block.getToolUse() != null) {
                BedrockContentBlock.BedrockToolUseBlock tu = block.getToolUse();
                String id = tu.getToolUseId() != null ? tu.getToolUseId() : "";
                String name = tu.getName() != null ? tu.getName() : "";
                Map<String, Object> args = tu.getInput() != null ? tu.getInput() : Collections.emptyMap();
                String argumentsJson = ProtifyJson.toJsonMap(args);
                toolCalls.add(new ProtifyAIToolCall(id, name, args, argumentsJson));
            }
        }
        return toolCalls;
    }

    private BedrockMessage getOutputMessage() {
        if (body.getOutput() != null) {
            return body.getOutput().getMessage();
        }
        return null;
    }
}
