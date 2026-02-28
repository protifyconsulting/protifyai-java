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

package ai.protify.core.internal.provider.openai;

import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.provider.openai.model.*;
import ai.protify.core.request.*;
import ai.protify.core.internal.util.json.JsonBuilder;
import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.message.AIMessage;
import ai.protify.core.provider.ProtifyAIProviderRequest;
import ai.protify.core.response.AIResponse;
import ai.protify.core.tool.AIToolCall;
import ai.protify.core.tool.AIToolResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class OpenAIRequest extends ProtifyAIProviderRequest {

    private static final Set<AIConfigProperty> UNSUPPORTED = Collections.unmodifiableSet(
            EnumSet.of(AIConfigProperty.TOP_K));

    private static final Set<AIConfigProperty> GPT5_UNSUPPORTED = Collections.unmodifiableSet(
            EnumSet.of(AIConfigProperty.TEMPERATURE));

    @Override
    protected Set<AIConfigProperty> getUnsupportedParameters() {
        return UNSUPPORTED;
    }

    @Override
    protected Set<AIConfigProperty> getUnsupportedParametersForModel(String model) {
        if (model != null && model.startsWith("gpt-5")) {
            return GPT5_UNSUPPORTED;
        }
        return Collections.emptySet();
    }

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

    private OpenAIRequestBody buildRequestBody() {
        Double temperature = super.getConfiguration().getProperty(AIConfigProperty.TEMPERATURE);
        Double topP = super.getConfiguration().getProperty(AIConfigProperty.TOP_P);
        Integer maxTokens = super.getConfiguration().getProperty(AIConfigProperty.MAX_OUTPUT_TOKENS);
        String instructions = super.getConfiguration().getProperty(AIConfigProperty.INSTRUCTIONS);

        OpenAIRequestBody body = new OpenAIRequestBody();
        body.setModel(super.getModelName());
        body.setTemperature(temperature);
        body.setTopP(topP);
        body.setMaxOutputTokens(maxTokens);
        body.setInstructions(instructions);
        body.setStream(stream);

        // Map tools
        if (super.getTools() != null && !super.getTools().isEmpty()) {
            List<OpenAITool> openAITools = super.getTools().stream()
                    .map(OpenAITool::from)
                    .collect(Collectors.toList());
            body.setTools(openAITools);
        }

        // Build input
        body.setInput(buildInput());

        return body;
    }

    @SuppressWarnings("unchecked")
    private List<Object> buildInput() {
        // Conversation mode: use full message history
        List<AIMessage> conversationMessages = super.getMessages();
        if (conversationMessages != null && !conversationMessages.isEmpty()) {
            return buildInputFromConversation(conversationMessages);
        }

        // Legacy single-turn mode
        List<Object> inputItems = new ArrayList<>();

        List<AIInput> inputs = super.getInputs();
        AIResponse previousResponse = super.getPreviousAssistantResponse();
        List<AIToolResult> toolResults = super.getToolResults();

        // Original user message
        if (inputs != null && !inputs.isEmpty()) {
            List<OpenAIContentBlock> contentBlocks = buildContentBlocks(inputs);
            inputItems.add(new OpenAIInputMessage("user", contentBlocks));
        }

        // If we have a previous response with tool calls, add reasoning + function_call items and outputs
        if (previousResponse != null && previousResponse.hasToolCalls()) {
            // Add the raw output items from the previous response (reasoning + function_call)
            // GPT-5 models require reasoning items to be included alongside function_call items
            Object output = ProtifyJson.parse(previousResponse.getProviderResponse()).get("output");
            if (output instanceof List) {
                for (Object item : (List<?>) output) {
                    if (item instanceof Map) {
                        Map<String, Object> outputItem = (Map<String, Object>) item;
                        String type = (String) outputItem.get("type");
                        if ("function_call".equals(type) || "reasoning".equals(type)) {
                            inputItems.add(outputItem);
                        }
                    }
                }
            }

            // Add function_call_output items for each tool result
            if (toolResults != null && !toolResults.isEmpty()) {
                for (AIToolResult result : toolResults) {
                    inputItems.add(OpenAIFunctionCallOutput.of(result.getToolCallId(), result.getContent()));
                }
            }
        }

        return inputItems;
    }

    private List<Object> buildInputFromConversation(List<AIMessage> conversationMessages) {
        List<Object> inputItems = new ArrayList<>();

        for (AIMessage msg : conversationMessages) {
            if ("user".equals(msg.getRole())) {
                // User message with tool results
                if (!msg.getToolResults().isEmpty()) {
                    for (AIToolResult result : msg.getToolResults()) {
                        inputItems.add(OpenAIFunctionCallOutput.of(result.getToolCallId(), result.getContent()));
                    }
                } else {
                    // User text message, possibly with file inputs
                    List<OpenAIContentBlock> contentBlocks = new ArrayList<>();
                    if (msg.getText() != null) {
                        contentBlocks.add(OpenAIContentBlock.text(msg.getText()));
                    }
                    if (!msg.getInputs().isEmpty()) {
                        contentBlocks.addAll(buildContentBlocks(msg.getInputs()));
                    }
                    if (!contentBlocks.isEmpty()) {
                        inputItems.add(new OpenAIInputMessage("user", contentBlocks));
                    }
                }
            } else if ("assistant".equals(msg.getRole())) {
                // Assistant tool calls — emit as raw function_call maps
                if (msg.hasToolCalls()) {
                    for (AIToolCall call : msg.getToolCalls()) {
                        Map<String, Object> functionCall = new LinkedHashMap<>();
                        functionCall.put("type", "function_call");
                        functionCall.put("call_id", call.getId());
                        functionCall.put("name", call.getName());
                        functionCall.put("arguments", call.getArgumentsJson());
                        inputItems.add(functionCall);
                    }
                }
                // Assistant text — emit as output_text content block
                if (msg.getText() != null) {
                    List<OpenAIContentBlock> contentBlocks = new ArrayList<>();
                    contentBlocks.add(OpenAIContentBlock.outputText(msg.getText()));
                    inputItems.add(new OpenAIInputMessage("assistant", contentBlocks));
                }
            }
        }

        return inputItems;
    }

    private List<OpenAIContentBlock> buildContentBlocks(List<AIInput> inputs) {
        List<OpenAIContentBlock> blocks = new ArrayList<>();
        for (AIInput input : inputs) {
            if (InputType.TEXT == input.getType()) {
                blocks.add(OpenAIContentBlock.text(((AITextInput) input).getText()));
            } else if (InputType.IMAGE == input.getType()) {
                AIFileInput fileInput = (AIFileInput) input;
                if (FileDataReferenceType.PROVIDER_ID == fileInput.getReferenceType()) {
                    blocks.add(OpenAIContentBlock.imageFromFileId(fileInput.getData()));
                } else {
                    blocks.add(OpenAIContentBlock.imageFromUrl(fileInput.getData()));
                }
            } else if (InputType.PDF == input.getType()) {
                AIFileInput fileInput = (AIFileInput) input;
                if (FileDataReferenceType.PROVIDER_ID == fileInput.getReferenceType()) {
                    blocks.add(OpenAIContentBlock.fileFromId(fileInput.getData()));
                } else if (FileDataReferenceType.DATA_URL == fileInput.getReferenceType()) {
                    blocks.add(OpenAIContentBlock.fileFromUrl(fileInput.getData()));
                } else {
                    blocks.add(OpenAIContentBlock.fileFromUrl(fileInput.getData(), fileInput.getFilename()));
                }
            }
        }
        return blocks;
    }

    private String createLoggableJson(boolean prettyPrint) {
        Double temperature = super.getConfiguration().getProperty(AIConfigProperty.TEMPERATURE);
        Double topP = super.getConfiguration().getProperty(AIConfigProperty.TOP_P);
        Integer maxTokens = super.getConfiguration().getProperty(AIConfigProperty.MAX_OUTPUT_TOKENS);
        String instructions = super.getConfiguration().getProperty(AIConfigProperty.INSTRUCTIONS);

        List<AIInput> inputs = super.getInputs();

        JsonBuilder builder = new JsonBuilder(true, prettyPrint);
        builder.appendNewLine();
        builder.append("{");
        builder.appendNewLine();

        Map<String, Object> properties = ProtifyJson.mapOf(
                "model", super.getModelName(),
                "temperature", temperature,
                "top_p", topP,
                "max_output_tokens", maxTokens,
                "instructions", instructions,
                "stream", stream
        );

        builder.appendProperties(properties, 1);
        appendInputs(builder, inputs);
        builder.append("}");

        return builder.toString();
    }

    private void appendInputs(JsonBuilder builder, List<AIInput> inputs) {
        if (inputs != null && !inputs.isEmpty()) {
            builder.appendIndent(1);
            builder.append("\"input\":[");
            builder.appendNewLine();
            builder.appendIndent(2);
            builder.append("{");
            builder.appendNewLine();
            builder.appendIndent(3);
            builder.append("\"role\":\"").append("user").append("\"").append(",");
            builder.appendNewLine();
            builder.appendIndent(3);
            builder.append("\"content\":[");
            builder.appendNewLine();
            for (AIInput input : inputs) {
                if (InputType.TEXT == input.getType()) {
                    appendTextInput(builder, (AITextInput) input);
                } else if (InputType.IMAGE == input.getType()) {
                    appendImageInput(builder, (AIFileInput) input);
                } else if (InputType.PDF == input.getType()) {
                    appendPdfInput(builder, (AIFileInput) input);
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
            builder.append("],");
            builder.appendNewLine();
        }
        builder.deleteLastChar();
    }

    private void appendTextInput(JsonBuilder builder, AITextInput textInput) {
        Map<String, Object> properties = ProtifyJson.mapOf(
                "type", "input_text",
                "text", ProtifyJson.escapeJson(textInput.getText())
        );
        appendInput(builder, properties);
    }

    private void appendImageInput(JsonBuilder builder, AIFileInput fileInput) {
        Map<String, Object> properties = ProtifyJson.mapOf("type", "input_image");

        if (FileDataReferenceType.PROVIDER_ID == fileInput.getReferenceType()) {
            properties.put("file_id", fileInput.getData());
        } else {
            properties.put("image_url", fileInput.getData());
        }
        appendInput(builder, properties);
    }

    private void appendPdfInput(JsonBuilder builder, AIFileInput fileInput) {
        Map<String, Object> properties = ProtifyJson.mapOf("type", "input_file");

        if (FileDataReferenceType.PROVIDER_ID == fileInput.getReferenceType()) {
            properties.put("file_id", fileInput.getData());
        } else if (FileDataReferenceType.DATA_URL == fileInput.getReferenceType()) {
            properties.put("file_url", fileInput.getData());
        } else {
            properties.put("file_url", fileInput.getData());
            properties.put("filename", fileInput.getFilename());
        }

        appendInput(builder, properties);
    }

    private void appendInput(JsonBuilder builder, Map<String, Object> properties) {
        builder.appendIndent(4);
        builder.append("{");
        builder.appendNewLine();
        builder.appendProperties(properties, 5);
        builder.deleteLastChar();
        builder.appendIndent(4).append("},");
        builder.appendNewLine();
    }

}
