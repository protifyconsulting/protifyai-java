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

package ai.protify.core.internal.provider.gemini;

import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.provider.gemini.model.GeminiContent;
import ai.protify.core.internal.provider.gemini.model.GeminiGenerationConfig;
import ai.protify.core.internal.provider.gemini.model.GeminiPart;
import ai.protify.core.internal.provider.gemini.model.GeminiRequestBody;
import ai.protify.core.internal.provider.gemini.model.GeminiTool;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class GeminiRequest extends ProtifyAIProviderRequest {

    private String json;
    private String loggableJson;

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

    private GeminiRequestBody buildRequestBody() {
        Double temperature = super.getConfiguration().getProperty(AIConfigProperty.TEMPERATURE);
        Double topP = super.getConfiguration().getProperty(AIConfigProperty.TOP_P);
        Integer topK = super.getConfiguration().getProperty(AIConfigProperty.TOP_K);
        Integer maxTokens = super.getConfiguration().getProperty(AIConfigProperty.MAX_OUTPUT_TOKENS);
        String instructions = super.getConfiguration().getProperty(AIConfigProperty.INSTRUCTIONS);

        GeminiRequestBody body = new GeminiRequestBody();

        // System instruction
        if (instructions != null) {
            body.setSystemInstruction(new GeminiContent("user",
                    Collections.singletonList(GeminiPart.text(instructions))));
        }

        // Generation config
        if (temperature != null || topP != null || topK != null || maxTokens != null) {
            GeminiGenerationConfig config = new GeminiGenerationConfig();
            config.setTemperature(temperature);
            config.setTopP(topP);
            config.setTopK(topK);
            config.setMaxOutputTokens(maxTokens);
            body.setGenerationConfig(config);
        }

        // Tools
        if (super.getTools() != null && !super.getTools().isEmpty()) {
            body.setTools(Collections.singletonList(GeminiTool.from(super.getTools())));
        }

        // Contents
        body.setContents(buildContents());

        return body;
    }

    private List<GeminiContent> buildContents() {
        // Conversation mode: use full message history
        List<AIMessage> conversationMessages = super.getMessages();
        if (conversationMessages != null && !conversationMessages.isEmpty()) {
            return buildContentsFromConversation(conversationMessages);
        }

        // Single-turn mode
        List<GeminiContent> contents = new ArrayList<>();

        List<AIInput> inputs = super.getInputs();
        AIResponse previousResponse = super.getPreviousAssistantResponse();
        List<AIToolResult> toolResults = super.getToolResults();

        // User message
        if (inputs != null && !inputs.isEmpty()) {
            List<GeminiPart> parts = buildParts(inputs);
            contents.add(new GeminiContent("user", parts));
        }

        // Previous assistant response with tool calls
        if (previousResponse != null && previousResponse.hasToolCalls()) {
            List<GeminiPart> assistantParts = new ArrayList<>();

            String text = previousResponse.text();
            if (text != null && !text.isEmpty()) {
                assistantParts.add(GeminiPart.text(text));
            }

            for (AIToolCall call : previousResponse.getToolCalls()) {
                assistantParts.add(GeminiPart.functionCall(call.getName(), call.getArguments()));
            }
            contents.add(new GeminiContent("model", assistantParts));

            // Tool results as user message
            if (toolResults != null && !toolResults.isEmpty()) {
                List<GeminiPart> resultParts = new ArrayList<>();
                for (AIToolResult result : toolResults) {
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("result", result.getContent());
                    resultParts.add(GeminiPart.functionResponse(
                            findToolName(previousResponse, result.getToolCallId()), response));
                }
                contents.add(new GeminiContent("user", resultParts));
            }
        }

        return contents;
    }

    private List<GeminiContent> buildContentsFromConversation(List<AIMessage> conversationMessages) {
        List<GeminiContent> contents = new ArrayList<>();

        for (AIMessage msg : conversationMessages) {
            List<GeminiPart> parts = new ArrayList<>();
            String role;

            if ("user".equals(msg.getRole())) {
                role = "user";
                if (!msg.getToolResults().isEmpty()) {
                    for (AIToolResult result : msg.getToolResults()) {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("result", result.getContent());
                        parts.add(GeminiPart.functionResponse(result.getToolCallId(), response));
                    }
                } else {
                    if (msg.getText() != null) {
                        parts.add(GeminiPart.text(msg.getText()));
                    }
                    if (!msg.getInputs().isEmpty()) {
                        parts.addAll(buildParts(msg.getInputs()));
                    }
                }
            } else {
                role = "model";
                if (msg.getText() != null) {
                    parts.add(GeminiPart.text(msg.getText()));
                }
                if (msg.hasToolCalls()) {
                    for (AIToolCall call : msg.getToolCalls()) {
                        parts.add(GeminiPart.functionCall(call.getName(), call.getArguments()));
                    }
                }
            }

            if (!parts.isEmpty()) {
                contents.add(new GeminiContent(role, parts));
            }
        }

        return contents;
    }

    private List<GeminiPart> buildParts(List<AIInput> inputs) {
        List<GeminiPart> parts = new ArrayList<>();
        for (AIInput input : inputs) {
            if (InputType.TEXT == input.getType()) {
                parts.add(GeminiPart.text(((AITextInput) input).getText()));
            } else if (InputType.IMAGE == input.getType()) {
                AIFileInput fileInput = (AIFileInput) input;
                String mediaType = extractMediaType(fileInput.getData());
                String base64Data = extractBase64Data(fileInput.getData());
                parts.add(GeminiPart.inlineData(mediaType, base64Data));
            } else if (InputType.PDF == input.getType()) {
                AIFileInput fileInput = (AIFileInput) input;
                String base64Data = extractBase64Data(fileInput.getData());
                parts.add(GeminiPart.inlineData("application/pdf", base64Data));
            }
        }
        return parts;
    }

    private String findToolName(AIResponse response, String toolCallId) {
        if (response != null && response.getToolCalls() != null) {
            for (AIToolCall call : response.getToolCalls()) {
                if (call.getId().equals(toolCallId)) {
                    return call.getName();
                }
            }
        }
        return toolCallId;
    }

    private String createLoggableJson(boolean prettyPrint) {
        String instructions = super.getConfiguration().getProperty(AIConfigProperty.INSTRUCTIONS);
        List<AIInput> inputs = super.getInputs();

        JsonBuilder builder = new JsonBuilder(true, prettyPrint);
        builder.appendNewLine();
        builder.append("{");
        builder.appendNewLine();

        if (instructions != null) {
            builder.appendIndent(1);
            builder.append("\"systemInstruction\":{");
            builder.appendNewLine();
            builder.appendProperty("role", "user", 2);
            builder.appendIndent(2);
            builder.append("\"parts\":[{\"text\":\"" + ProtifyJson.escapeJson(instructions) + "\"}]");
            builder.appendNewLine();
            builder.appendIndent(1);
            builder.append("},");
            builder.appendNewLine();
        }

        appendContents(builder, inputs);
        builder.append("}");

        return builder.toString();
    }

    private void appendContents(JsonBuilder builder, List<AIInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            builder.deleteLastChar();
            return;
        }

        builder.appendIndent(1);
        builder.append("\"contents\":[");
        builder.appendNewLine();
        builder.appendIndent(2);
        builder.append("{");
        builder.appendNewLine();
        builder.appendProperty("role", "user", 3);
        builder.appendIndent(3);
        builder.append("\"parts\":[");
        builder.appendNewLine();

        for (AIInput input : inputs) {
            if (InputType.TEXT == input.getType()) {
                Map<String, Object> properties = ProtifyJson.mapOf(
                        "text", ProtifyJson.escapeJson(((AITextInput) input).getText())
                );
                builder.appendIndent(4);
                builder.append("{");
                builder.appendNewLine();
                builder.appendProperties(properties, 5);
                builder.deleteLastChar();
                builder.appendIndent(4).append("},");
                builder.appendNewLine();
            } else if (InputType.IMAGE == input.getType() || InputType.PDF == input.getType()) {
                AIFileInput fileInput = (AIFileInput) input;
                String mimeType = InputType.PDF == input.getType()
                        ? "application/pdf" : extractMediaType(fileInput.getData());
                String base64Data = extractBase64Data(fileInput.getData());

                builder.appendIndent(4);
                builder.append("{");
                builder.appendNewLine();
                builder.appendIndent(5);
                builder.append("\"inlineData\":{");
                builder.appendNewLine();
                Map<String, Object> dataProps = ProtifyJson.mapOf(
                        "mimeType", mimeType,
                        "data", base64Data
                );
                builder.appendProperties(dataProps, 6);
                builder.deleteLastChar();
                builder.appendIndent(5);
                builder.append("}");
                builder.appendNewLine();
                builder.appendIndent(4);
                builder.append("},");
                builder.appendNewLine();
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
}
