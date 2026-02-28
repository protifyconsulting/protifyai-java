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

import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.provider.bedrock.model.BedrockContentBlock;
import ai.protify.core.internal.provider.bedrock.model.BedrockInferenceConfig;
import ai.protify.core.internal.provider.bedrock.model.BedrockMessage;
import ai.protify.core.internal.provider.bedrock.model.BedrockRequestBody;
import ai.protify.core.internal.provider.bedrock.model.BedrockToolConfig;
import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.message.AIMessage;
import ai.protify.core.provider.ProtifyAIProviderRequest;
import ai.protify.core.request.AIFileInput;
import ai.protify.core.request.AIInput;
import ai.protify.core.request.AITextInput;
import ai.protify.core.request.InputType;
import ai.protify.core.response.AIResponse;
import ai.protify.core.tool.AIToolCall;
import ai.protify.core.tool.AIToolResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class BedrockRequest extends ProtifyAIProviderRequest {

    private static final Set<AIConfigProperty> UNSUPPORTED = Collections.unmodifiableSet(
            EnumSet.of(AIConfigProperty.TOP_K));

    @Override
    protected Set<AIConfigProperty> getUnsupportedParameters() {
        return UNSUPPORTED;
    }

    private String json;
    private String loggableJson;

    public String toJson() {
        if (this.json == null) {
            this.json = ProtifyJson.toJson(buildRequestBody());
        }
        return this.json;
    }

    public String toLoggableJson() {
        if (this.loggableJson == null) {
            this.loggableJson = toJson();
        }
        return this.loggableJson;
    }

    BedrockRequestBody buildRequestBody() {
        Double temperature = super.getConfiguration().getProperty(AIConfigProperty.TEMPERATURE);
        Double topP = super.getConfiguration().getProperty(AIConfigProperty.TOP_P);
        Integer maxTokens = super.getConfiguration().getProperty(AIConfigProperty.MAX_OUTPUT_TOKENS);
        String instructions = super.getConfiguration().getProperty(AIConfigProperty.INSTRUCTIONS);

        BedrockRequestBody body = new BedrockRequestBody();

        // System prompt
        if (instructions != null && !instructions.isEmpty()) {
            body.setSystem(Collections.singletonList(BedrockContentBlock.text(instructions)));
        }

        // Inference config
        if (temperature != null || topP != null || maxTokens != null) {
            BedrockInferenceConfig config = new BedrockInferenceConfig();
            config.setTemperature(temperature);
            config.setTopP(topP);
            config.setMaxTokens(maxTokens);
            body.setInferenceConfig(config);
        }

        // Tools
        if (super.getTools() != null && !super.getTools().isEmpty()) {
            body.setToolConfig(BedrockToolConfig.from(super.getTools()));
        }

        // Messages
        body.setMessages(buildMessages());

        return body;
    }

    private List<BedrockMessage> buildMessages() {
        List<AIMessage> conversationMessages = super.getMessages();
        if (conversationMessages != null && !conversationMessages.isEmpty()) {
            return buildMessagesFromConversation(conversationMessages);
        }

        List<BedrockMessage> messages = new ArrayList<>();

        List<AIInput> inputs = super.getInputs();
        AIResponse previousResponse = super.getPreviousAssistantResponse();
        List<AIToolResult> toolResults = super.getToolResults();

        // User message
        if (inputs != null && !inputs.isEmpty()) {
            messages.add(new BedrockMessage("user", buildContentBlocks(inputs)));
        }

        // Previous assistant response with tool calls
        if (previousResponse != null && previousResponse.hasToolCalls()) {
            List<BedrockContentBlock> assistantContent = new ArrayList<>();
            String text = previousResponse.text();
            if (text != null && !text.isEmpty()) {
                assistantContent.add(BedrockContentBlock.text(text));
            }
            for (AIToolCall call : previousResponse.getToolCalls()) {
                assistantContent.add(BedrockContentBlock.toolUse(
                        call.getId(), call.getName(), call.getArguments()));
            }
            messages.add(new BedrockMessage("assistant", assistantContent));

            // Tool results as user message
            if (toolResults != null && !toolResults.isEmpty()) {
                List<BedrockContentBlock> toolResultBlocks = new ArrayList<>();
                for (AIToolResult result : toolResults) {
                    toolResultBlocks.add(BedrockContentBlock.toolResult(
                            result.getToolCallId(), result.getContent()));
                }
                messages.add(new BedrockMessage("user", toolResultBlocks));
            }
        }

        return messages;
    }

    private List<BedrockMessage> buildMessagesFromConversation(List<AIMessage> conversationMessages) {
        List<BedrockMessage> messages = new ArrayList<>();

        for (AIMessage msg : conversationMessages) {
            List<BedrockContentBlock> content = new ArrayList<>();

            if ("user".equals(msg.getRole())) {
                if (!msg.getToolResults().isEmpty()) {
                    for (AIToolResult result : msg.getToolResults()) {
                        content.add(BedrockContentBlock.toolResult(
                                result.getToolCallId(), result.getContent()));
                    }
                } else {
                    if (msg.getText() != null) {
                        content.add(BedrockContentBlock.text(msg.getText()));
                    }
                    if (!msg.getInputs().isEmpty()) {
                        content.addAll(buildContentBlocks(msg.getInputs()));
                    }
                }
                if (!content.isEmpty()) {
                    messages.add(new BedrockMessage("user", content));
                }
            } else if ("assistant".equals(msg.getRole())) {
                if (msg.getText() != null) {
                    content.add(BedrockContentBlock.text(msg.getText()));
                }
                if (msg.hasToolCalls()) {
                    for (AIToolCall call : msg.getToolCalls()) {
                        content.add(BedrockContentBlock.toolUse(
                                call.getId(), call.getName(), call.getArguments()));
                    }
                }
                if (!content.isEmpty()) {
                    messages.add(new BedrockMessage("assistant", content));
                }
            }
        }

        return messages;
    }

    private List<BedrockContentBlock> buildContentBlocks(List<AIInput> inputs) {
        List<BedrockContentBlock> blocks = new ArrayList<>();
        for (AIInput input : inputs) {
            if (InputType.TEXT == input.getType()) {
                blocks.add(BedrockContentBlock.text(((AITextInput) input).getText()));
            } else if (InputType.IMAGE == input.getType()) {
                AIFileInput fileInput = (AIFileInput) input;
                String mediaType = extractMediaType(fileInput.getData());
                String base64Data = extractBase64Data(fileInput.getData());
                String format = mediaTypeToFormat(mediaType);
                blocks.add(BedrockContentBlock.image(format, base64Data));
            }
        }
        return blocks;
    }

    private static String extractMediaType(String dataUrl) {
        if (dataUrl != null && dataUrl.startsWith("data:")) {
            int semicolon = dataUrl.indexOf(';');
            if (semicolon > 5) {
                return dataUrl.substring(5, semicolon);
            }
        }
        return "application/octet-stream";
    }

    private static String extractBase64Data(String dataUrl) {
        if (dataUrl != null && dataUrl.contains(",")) {
            return dataUrl.substring(dataUrl.indexOf(',') + 1);
        }
        return dataUrl;
    }

    private static String mediaTypeToFormat(String mediaType) {
        switch (mediaType) {
            case "image/png": return "png";
            case "image/jpeg": return "jpeg";
            case "image/gif": return "gif";
            case "image/webp": return "webp";
            default: return "png";
        }
    }
}
