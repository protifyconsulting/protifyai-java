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

import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.provider.anthropic.model.AnthropicContentBlock;
import ai.protify.core.internal.provider.anthropic.model.AnthropicMessage;
import ai.protify.core.internal.provider.anthropic.model.AnthropicRequestBody;
import ai.protify.core.internal.provider.anthropic.model.AnthropicTool;
import ai.protify.core.internal.util.json.JsonBuilder;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class AnthropicRequest extends ProtifyAIProviderRequest {

    private String json;
    private String loggableJson;
    private boolean stream = false;

    void setStream(boolean stream) {
        this.stream = stream;
        this.json = null;
        this.loggableJson = null;
    }

    public String toJson() {
        if (this.json == null) {
            this.json = ProtifyJson.toJson(buildRequestBody());
        }
        return this.json;
    }

    public String toLoggableJson() {
        boolean prettyPrint = super.getConfiguration().getProperty(AIConfigProperty.PRETTY_PRINT_JSON);

        if (this.loggableJson == null) {
            this.loggableJson = createLoggableJson(prettyPrint);
        }
        return this.loggableJson;
    }

    private AnthropicRequestBody buildRequestBody() {
        Double temperature = super.getConfiguration().getProperty(AIConfigProperty.TEMPERATURE);
        Double topP = super.getConfiguration().getProperty(AIConfigProperty.TOP_P);
        Integer topK = super.getConfiguration().getProperty(AIConfigProperty.TOP_K);
        Integer maxTokens = super.getConfiguration().getProperty(AIConfigProperty.MAX_OUTPUT_TOKENS);
        String instructions = super.getConfiguration().getProperty(AIConfigProperty.INSTRUCTIONS);

        AnthropicRequestBody body = new AnthropicRequestBody();
        body.setModel(super.getModelName());
        body.setMaxTokens(maxTokens);
        body.setTemperature(temperature);
        body.setTopP(topP);
        body.setTopK(topK);
        body.setStream(stream);

        if (instructions != null) {
            body.setSystem(instructions);
        }

        // Map tools
        if (super.getTools() != null && !super.getTools().isEmpty()) {
            List<AnthropicTool> anthropicTools = super.getTools().stream()
                    .map(AnthropicTool::from)
                    .collect(Collectors.toList());
            body.setTools(anthropicTools);
        }

        // Build messages
        body.setMessages(buildMessages());

        return body;
    }

    private List<AnthropicMessage> buildMessages() {
        // Conversation mode: use full message history
        List<AIMessage> conversationMessages = super.getMessages();
        if (conversationMessages != null && !conversationMessages.isEmpty()) {
            return buildMessagesFromConversation(conversationMessages);
        }

        // Legacy single-turn mode
        List<AnthropicMessage> messages = new ArrayList<>();

        List<AIInput> inputs = super.getInputs();
        AIResponse previousResponse = super.getPreviousAssistantResponse();
        List<AIToolResult> toolResults = super.getToolResults();

        // Original user message
        if (inputs != null && !inputs.isEmpty()) {
            List<AnthropicContentBlock> contentBlocks = buildContentBlocks(inputs);
            messages.add(new AnthropicMessage("user", contentBlocks));
        }

        // If we have a previous response with tool calls, add the assistant message and tool results
        if (previousResponse != null && previousResponse.hasToolCalls()) {
            // Reconstruct assistant message with tool_use blocks
            List<AnthropicContentBlock> assistantContent = new ArrayList<>();

            // Add text content if present
            String text = previousResponse.text();
            if (text != null && !text.isEmpty()) {
                assistantContent.add(AnthropicContentBlock.text(text));
            }

            // Add tool_use blocks
            for (AIToolCall call : previousResponse.getToolCalls()) {
                assistantContent.add(AnthropicContentBlock.toolUse(
                        call.getId(), call.getName(), call.getArguments()));
            }
            messages.add(new AnthropicMessage("assistant", assistantContent));

            // Add user message with tool_result blocks
            if (toolResults != null && !toolResults.isEmpty()) {
                List<AnthropicContentBlock> toolResultBlocks = new ArrayList<>();
                for (AIToolResult result : toolResults) {
                    toolResultBlocks.add(AnthropicContentBlock.toolResult(
                            result.getToolCallId(), result.getContent(), result.isError()));
                }
                messages.add(new AnthropicMessage("user", toolResultBlocks));
            }
        }

        return messages;
    }

    private List<AnthropicMessage> buildMessagesFromConversation(List<AIMessage> conversationMessages) {
        List<AnthropicMessage> messages = new ArrayList<>();

        for (AIMessage msg : conversationMessages) {
            List<AnthropicContentBlock> contentBlocks = new ArrayList<>();

            if ("user".equals(msg.getRole())) {
                // User message with tool results
                if (!msg.getToolResults().isEmpty()) {
                    for (AIToolResult result : msg.getToolResults()) {
                        contentBlocks.add(AnthropicContentBlock.toolResult(
                                result.getToolCallId(), result.getContent(), result.isError()));
                    }
                } else {
                    // User text message, possibly with file inputs
                    if (msg.getText() != null) {
                        contentBlocks.add(AnthropicContentBlock.text(msg.getText()));
                    }
                    if (!msg.getInputs().isEmpty()) {
                        contentBlocks.addAll(buildContentBlocks(msg.getInputs()));
                    }
                }
            } else if ("assistant".equals(msg.getRole())) {
                // Assistant text
                if (msg.getText() != null) {
                    contentBlocks.add(AnthropicContentBlock.text(msg.getText()));
                }
                // Assistant tool calls
                if (msg.hasToolCalls()) {
                    for (AIToolCall call : msg.getToolCalls()) {
                        contentBlocks.add(AnthropicContentBlock.toolUse(
                                call.getId(), call.getName(), call.getArguments()));
                    }
                }
            }

            if (!contentBlocks.isEmpty()) {
                messages.add(new AnthropicMessage(msg.getRole(), contentBlocks));
            }
        }

        return messages;
    }

    private List<AnthropicContentBlock> buildContentBlocks(List<AIInput> inputs) {
        List<AnthropicContentBlock> blocks = new ArrayList<>();
        for (AIInput input : inputs) {
            if (InputType.TEXT == input.getType()) {
                blocks.add(AnthropicContentBlock.text(((AITextInput) input).getText()));
            } else if (InputType.IMAGE == input.getType()) {
                AIFileInput fileInput = (AIFileInput) input;
                String mediaType = extractMediaType(fileInput.getData());
                String base64Data = extractBase64Data(fileInput.getData());
                blocks.add(AnthropicContentBlock.image(mediaType, base64Data));
            } else if (InputType.PDF == input.getType()) {
                AIFileInput fileInput = (AIFileInput) input;
                String base64Data = extractBase64Data(fileInput.getData());
                blocks.add(AnthropicContentBlock.document(base64Data));
            }
        }
        return blocks;
    }

    private String createLoggableJson(boolean prettyPrint) {
        Double temperature = super.getConfiguration().getProperty(AIConfigProperty.TEMPERATURE);
        Double topP = super.getConfiguration().getProperty(AIConfigProperty.TOP_P);
        Integer topK = super.getConfiguration().getProperty(AIConfigProperty.TOP_K);
        Integer maxTokens = super.getConfiguration().getProperty(AIConfigProperty.MAX_OUTPUT_TOKENS);
        String instructions = super.getConfiguration().getProperty(AIConfigProperty.INSTRUCTIONS);

        List<AIInput> inputs = super.getInputs();

        JsonBuilder builder = new JsonBuilder(true, prettyPrint);
        builder.appendNewLine();
        builder.append("{");
        builder.appendNewLine();

        Map<String, Object> properties = ProtifyJson.mapOf(
                "model", super.getModelName(),
                "max_tokens", maxTokens,
                "temperature", temperature,
                "top_p", topP,
                "top_k", topK,
                "stream", stream
        );

        builder.appendProperties(properties, 1);

        if (instructions != null) {
            builder.appendProperty("system", instructions, 1);
        }

        appendMessages(builder, inputs);
        builder.append("}");

        return builder.toString();
    }

    private void appendMessages(JsonBuilder builder, List<AIInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            builder.deleteLastChar();
            return;
        }

        builder.appendIndent(1);
        builder.append("\"messages\":[");
        builder.appendNewLine();
        builder.appendIndent(2);
        builder.append("{");
        builder.appendNewLine();
        builder.appendProperty("role", "user", 3);
        builder.appendIndent(3);
        builder.append("\"content\":[");
        builder.appendNewLine();

        for (AIInput input : inputs) {
            if (InputType.TEXT == input.getType()) {
                appendTextContent(builder, (AITextInput) input);
            } else if (InputType.IMAGE == input.getType()) {
                appendImageContent(builder, (AIFileInput) input);
            } else if (InputType.PDF == input.getType()) {
                appendDocumentContent(builder, (AIFileInput) input);
            }
        }

        builder.deleteLastChar();
        builder.appendIndent(3);
        builder.append("]");
        builder.appendNewLine();
        builder.appendIndent(2);
        builder.append("}");
        builder.appendNewLine();
        builder.appendIndent(1);
        builder.append("]");
        builder.appendNewLine();
    }

    private void appendTextContent(JsonBuilder builder, AITextInput textInput) {
        Map<String, Object> properties = ProtifyJson.mapOf(
                "type", "text",
                "text", ProtifyJson.escapeJson(textInput.getText())
        );
        appendContentBlock(builder, properties);
    }

    private void appendImageContent(JsonBuilder builder, AIFileInput fileInput) {
        String mediaType = extractMediaType(fileInput.getData());
        String base64Data = extractBase64Data(fileInput.getData());

        builder.appendIndent(4);
        builder.append("{");
        builder.appendNewLine();
        builder.appendProperty("type", "image", 5);
        builder.appendIndent(5);
        builder.append("\"source\":{");
        builder.appendNewLine();

        Map<String, Object> sourceProps = ProtifyJson.mapOf(
                "type", "base64",
                "media_type", mediaType,
                "data", base64Data
        );
        builder.appendProperties(sourceProps, 6);
        builder.deleteLastChar();
        builder.appendIndent(5);
        builder.append("}");
        builder.appendNewLine();
        builder.appendIndent(4);
        builder.append("},");
        builder.appendNewLine();
    }

    private void appendDocumentContent(JsonBuilder builder, AIFileInput fileInput) {
        String base64Data = extractBase64Data(fileInput.getData());

        builder.appendIndent(4);
        builder.append("{");
        builder.appendNewLine();
        builder.appendProperty("type", "document", 5);
        builder.appendIndent(5);
        builder.append("\"source\":{");
        builder.appendNewLine();

        Map<String, Object> sourceProps = ProtifyJson.mapOf(
                "type", "base64",
                "media_type", "application/pdf",
                "data", base64Data
        );
        builder.appendProperties(sourceProps, 6);
        builder.deleteLastChar();
        builder.appendIndent(5);
        builder.append("}");
        builder.appendNewLine();
        builder.appendIndent(4);
        builder.append("},");
        builder.appendNewLine();
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

    private void appendContentBlock(JsonBuilder builder, Map<String, Object> properties) {
        builder.appendIndent(4);
        builder.append("{");
        builder.appendNewLine();
        builder.appendProperties(properties, 5);
        builder.deleteLastChar();
        builder.appendIndent(4).append("},");
        builder.appendNewLine();
    }
}
