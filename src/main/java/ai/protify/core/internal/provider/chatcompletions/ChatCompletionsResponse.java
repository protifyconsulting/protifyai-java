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

package ai.protify.core.internal.provider.chatcompletions;

import ai.protify.core.internal.provider.chatcompletions.model.ChatChoice;
import ai.protify.core.internal.provider.chatcompletions.model.ChatResponseBody;
import ai.protify.core.internal.provider.chatcompletions.model.ChatResponseMessage;
import ai.protify.core.internal.provider.chatcompletions.model.ChatToolCall;
import ai.protify.core.internal.response.ProtifyAIResponse;
import ai.protify.core.internal.tool.ProtifyAIToolCall;
import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.tool.AIToolCall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ChatCompletionsResponse extends ProtifyAIResponse {

    private final ChatResponseBody body;

    public ChatCompletionsResponse(boolean cachedResponse, String pipelineId, String correlationId,
                                   String modelName, String rawResponse, ChatResponseBody body) {
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
        return body.getUsage() != null ? body.getUsage().getPromptTokens() : 0;
    }

    @Override
    public long getOutputTokens() {
        return body.getUsage() != null ? body.getUsage().getCompletionTokens() : 0;
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
        ChatResponseMessage message = getFirstChoiceMessage();
        if (message != null && message.getContent() != null) {
            return message.getContent();
        }
        return "";
    }

    @Override
    public String getStopReason() {
        if (body.getChoices() != null && !body.getChoices().isEmpty()) {
            return body.getChoices().get(0).getFinishReason();
        }
        return null;
    }

    @Override
    public boolean hasToolCalls() {
        return "tool_calls".equals(getStopReason());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AIToolCall> getToolCalls() {
        ChatResponseMessage message = getFirstChoiceMessage();
        if (message == null || message.getToolCalls() == null) {
            return Collections.emptyList();
        }

        List<AIToolCall> toolCalls = new ArrayList<>();
        for (ChatToolCall tc : message.getToolCalls()) {
            String id = tc.getId() != null ? tc.getId() : "";
            String name = "";
            String argumentsJson = "{}";
            if (tc.getFunction() != null) {
                name = tc.getFunction().getName() != null ? tc.getFunction().getName() : "";
                argumentsJson = tc.getFunction().getArguments() != null ? tc.getFunction().getArguments() : "{}";
            }

            Map<String, Object> arguments;
            try {
                Object parsed = ProtifyJson.parse(argumentsJson).get("");
                arguments = parsed instanceof Map ? (Map<String, Object>) parsed : Collections.emptyMap();
            } catch (Exception e) {
                arguments = Collections.emptyMap();
            }

            toolCalls.add(new ProtifyAIToolCall(id, name, arguments, argumentsJson));
        }
        return toolCalls;
    }

    private ChatResponseMessage getFirstChoiceMessage() {
        if (body.getChoices() != null && !body.getChoices().isEmpty()) {
            return body.getChoices().get(0).getMessage();
        }
        return null;
    }
}
