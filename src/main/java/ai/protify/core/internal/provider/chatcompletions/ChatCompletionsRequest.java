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

import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.provider.chatcompletions.model.ChatContentBlock;
import ai.protify.core.internal.provider.chatcompletions.model.ChatMessage;
import ai.protify.core.internal.provider.chatcompletions.model.ChatRequestBody;
import ai.protify.core.internal.provider.chatcompletions.model.ChatTool;
import ai.protify.core.internal.provider.chatcompletions.model.ChatToolCall;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ChatCompletionsRequest extends ProtifyAIProviderRequest {

    private static final Set<AIConfigProperty> UNSUPPORTED = Collections.unmodifiableSet(
            EnumSet.of(AIConfigProperty.TOP_K));

    @Override
    protected Set<AIConfigProperty> getUnsupportedParameters() {
        return UNSUPPORTED;
    }

    private String json;
    private String loggableJson;
    private boolean stream = false;

    public void setStream(boolean stream) {
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

    private ChatRequestBody buildRequestBody() {
        Double temperature = super.getConfiguration().getProperty(AIConfigProperty.TEMPERATURE);
        Double topP = super.getConfiguration().getProperty(AIConfigProperty.TOP_P);
        Integer maxTokens = super.getConfiguration().getProperty(AIConfigProperty.MAX_OUTPUT_TOKENS);
        String instructions = super.getConfiguration().getProperty(AIConfigProperty.INSTRUCTIONS);

        ChatRequestBody body = new ChatRequestBody();
        body.setModel(super.getModelName());
        body.setTemperature(temperature);
        body.setTopP(topP);
        body.setMaxTokens(maxTokens);
        body.setStream(stream);

        if (super.getTools() != null && !super.getTools().isEmpty()) {
            List<ChatTool> chatTools = super.getTools().stream()
                    .map(ChatTool::from)
                    .collect(Collectors.toList());
            body.setTools(chatTools);
        }

        body.setMessages(buildMessages(instructions));
        return body;
    }

    private List<ChatMessage> buildMessages(String instructions) {
        List<AIMessage> conversationMessages = super.getMessages();
        if (conversationMessages != null && !conversationMessages.isEmpty()) {
            return buildMessagesFromConversation(conversationMessages, instructions);
        }

        List<ChatMessage> messages = new ArrayList<>();

        if (instructions != null) {
            messages.add(ChatMessage.system(instructions));
        }

        List<AIInput> inputs = super.getInputs();
        AIResponse previousResponse = super.getPreviousAssistantResponse();
        List<AIToolResult> toolResults = super.getToolResults();

        if (inputs != null && !inputs.isEmpty()) {
            messages.add(buildUserMessage(inputs));
        }

        if (previousResponse != null && previousResponse.hasToolCalls()) {
            List<ChatToolCall> toolCallList = new ArrayList<>();
            for (AIToolCall call : previousResponse.getToolCalls()) {
                toolCallList.add(ChatToolCall.of(call.getId(), call.getName(),
                        ProtifyJson.toJsonMap(call.getArguments())));
            }
            String text = previousResponse.text();
            messages.add(ChatMessage.assistantWithToolCalls(
                    (text != null && !text.isEmpty()) ? text : null, toolCallList));

            if (toolResults != null) {
                for (AIToolResult result : toolResults) {
                    messages.add(ChatMessage.toolResult(result.getToolCallId(), result.getContent()));
                }
            }
        }

        return messages;
    }

    private List<ChatMessage> buildMessagesFromConversation(List<AIMessage> conversationMessages,
                                                            String instructions) {
        List<ChatMessage> messages = new ArrayList<>();

        if (instructions != null) {
            messages.add(ChatMessage.system(instructions));
        }

        for (AIMessage msg : conversationMessages) {
            if ("user".equals(msg.getRole())) {
                if (!msg.getToolResults().isEmpty()) {
                    for (AIToolResult result : msg.getToolResults()) {
                        messages.add(ChatMessage.toolResult(result.getToolCallId(), result.getContent()));
                    }
                } else {
                    List<AIInput> msgInputs = msg.getInputs();
                    if (!msgInputs.isEmpty()) {
                        List<ChatContentBlock> blocks = new ArrayList<>();
                        if (msg.getText() != null) {
                            blocks.add(ChatContentBlock.text(msg.getText()));
                        }
                        blocks.addAll(buildContentBlocks(msgInputs));
                        messages.add(ChatMessage.userMultipart(blocks));
                    } else if (msg.getText() != null) {
                        messages.add(ChatMessage.user(msg.getText()));
                    }
                }
            } else if ("assistant".equals(msg.getRole())) {
                if (msg.hasToolCalls()) {
                    List<ChatToolCall> toolCallList = new ArrayList<>();
                    for (AIToolCall call : msg.getToolCalls()) {
                        toolCallList.add(ChatToolCall.of(call.getId(), call.getName(),
                                ProtifyJson.toJsonMap(call.getArguments())));
                    }
                    messages.add(ChatMessage.assistantWithToolCalls(msg.getText(), toolCallList));
                } else {
                    messages.add(ChatMessage.assistant(msg.getText()));
                }
            }
        }

        return messages;
    }

    private ChatMessage buildUserMessage(List<AIInput> inputs) {
        boolean hasFiles = inputs.stream().anyMatch(i -> i.getType() != InputType.TEXT);

        if (!hasFiles) {
            StringBuilder sb = new StringBuilder();
            for (AIInput input : inputs) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(((AITextInput) input).getText());
            }
            return ChatMessage.user(sb.toString());
        }

        List<ChatContentBlock> blocks = new ArrayList<>();
        for (AIInput input : inputs) {
            if (InputType.TEXT == input.getType()) {
                blocks.add(ChatContentBlock.text(((AITextInput) input).getText()));
            } else if (InputType.IMAGE == input.getType() || InputType.PDF == input.getType()) {
                AIFileInput fileInput = (AIFileInput) input;
                blocks.add(ChatContentBlock.imageUrl(fileInput.getData()));
            }
        }
        return ChatMessage.userMultipart(blocks);
    }

    private List<ChatContentBlock> buildContentBlocks(List<AIInput> inputs) {
        List<ChatContentBlock> blocks = new ArrayList<>();
        for (AIInput input : inputs) {
            if (InputType.TEXT == input.getType()) {
                blocks.add(ChatContentBlock.text(((AITextInput) input).getText()));
            } else if (InputType.IMAGE == input.getType() || InputType.PDF == input.getType()) {
                AIFileInput fileInput = (AIFileInput) input;
                blocks.add(ChatContentBlock.imageUrl(fileInput.getData()));
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
                "max_tokens", maxTokens,
                "temperature", temperature,
                "top_p", topP,
                "stream", stream
        );
        builder.appendProperties(properties, 1);
        appendMessages(builder, inputs, instructions);
        builder.append("}");

        return builder.toString();
    }

    private void appendMessages(JsonBuilder builder, List<AIInput> inputs, String instructions) {
        builder.appendIndent(1);
        builder.append("\"messages\":[");
        builder.appendNewLine();

        if (instructions != null) {
            builder.appendIndent(2).append("{");
            builder.appendNewLine();
            builder.appendProperty("role", "system", 3);
            builder.appendProperty("content", ProtifyJson.escapeJson(instructions), 3);
            builder.deleteLastChar();
            builder.appendIndent(2).append("},");
            builder.appendNewLine();
        }

        if (inputs != null && !inputs.isEmpty()) {
            builder.appendIndent(2).append("{");
            builder.appendNewLine();
            builder.appendProperty("role", "user", 3);
            StringBuilder textContent = new StringBuilder();
            for (AIInput input : inputs) {
                if (InputType.TEXT == input.getType()) {
                    if (textContent.length() > 0) textContent.append("\\n");
                    textContent.append(ProtifyJson.escapeJson(((AITextInput) input).getText()));
                }
            }
            builder.appendProperty("content", textContent.toString(), 3);
            builder.deleteLastChar();
            builder.appendIndent(2).append("}");
            builder.appendNewLine();
        }

        builder.appendIndent(1).append("]");
        builder.appendNewLine();
    }
}
