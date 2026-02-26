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

package com.protify.ai.internal.conversation;

import com.protify.ai.AIClient;
import com.protify.ai.conversation.AIConversation;
import com.protify.ai.conversation.AIConversationState;
import com.protify.ai.conversation.AIConversationStore;
import com.protify.ai.internal.config.AIConfigProperty;
import com.protify.ai.internal.message.ProtifyAIMessage;
import com.protify.ai.internal.request.ProtifyAIRequest;
import com.protify.ai.message.AIMessage;
import com.protify.ai.provider.AIProviderClient;
import com.protify.ai.provider.AIProviderRequest;
import com.protify.ai.request.AIInput;
import com.protify.ai.request.AITextInput;
import com.protify.ai.response.AIResponse;
import com.protify.ai.tool.AITool;
import com.protify.ai.tool.AIToolCall;
import com.protify.ai.tool.AIToolHandler;
import com.protify.ai.tool.AIToolResult;

import java.util.*;

public class ProtifyAIConversation implements AIConversation {

    private final AIClient client;
    private final String conversationId;
    private final List<AIMessage> messages;
    private final AIConversationStore store;
    private final Map<AIConfigProperty, Object> properties;
    private final List<AITool> tools;
    private final Map<String, AIToolHandler> toolHandlers;
    private final int maxToolRounds;

    public ProtifyAIConversation(
            AIClient client,
            String conversationId,
            List<AIMessage> messages,
            AIConversationStore store,
            Map<AIConfigProperty, Object> properties,
            List<AITool> tools,
            Map<String, AIToolHandler> toolHandlers,
            int maxToolRounds) {
        this.client = client;
        this.conversationId = conversationId;
        this.messages = messages;
        this.store = store;
        this.properties = properties;
        this.tools = tools;
        this.toolHandlers = toolHandlers;
        this.maxToolRounds = maxToolRounds;
    }

    @Override
    public AIResponse send(String text) {
        List<AIInput> inputs = new ArrayList<>();
        inputs.add(AITextInput.of(text));
        return doSend(text, inputs);
    }

    @Override
    public AIResponse send(String text, AIInput... additionalInputs) {
        List<AIInput> inputs = new ArrayList<>();
        if (text != null) {
            inputs.add(AITextInput.of(text));
        }
        Collections.addAll(inputs, additionalInputs);
        return doSend(text, inputs);
    }

    @Override
    public AIResponse send(AIInput... inputs) {
        List<AIInput> inputList = new ArrayList<>(Arrays.asList(inputs));
        return doSend(null, inputList);
    }

    @SuppressWarnings("unchecked")
    private AIResponse doSend(String text, List<AIInput> inputs) {
        // 1. Create user message and append to history
        AIMessage userMessage;
        if (inputs.size() == 1 && inputs.get(0) instanceof AITextInput) {
            userMessage = ProtifyAIMessage.userText(text);
        } else {
            userMessage = ProtifyAIMessage.userWithInputs(text, inputs);
        }
        messages.add(userMessage);

        // 2. Build request with full message history
        ProtifyAIRequest request = new ProtifyAIRequest(
                client,
                new ArrayList<>(),
                properties,
                tools,
                Collections.emptyMap(),
                Collections.emptyList(),
                null,
                maxToolRounds,
                new ArrayList<>(messages)
        );

        // 3. Execute request
        AIProviderClient<AIProviderRequest> providerClient =
                (AIProviderClient<AIProviderRequest>) client.getProviderClient();
        AIProviderRequest providerRequest = client.getProviderClient()
                .transformRequest(request, request.deriveConfiguration(null));
        AIResponse response = providerClient.execute(providerRequest);

        // 4. Build assistant message and append
        AIMessage assistantMessage = ProtifyAIMessage.fromResponse(response);
        messages.add(assistantMessage);

        // 5. Tool loop if needed
        if (!toolHandlers.isEmpty() && response.hasToolCalls()) {
            response = executeToolLoop(response, providerClient, request);
        }

        // 6. Save state if store configured
        if (store != null) {
            store.save(getState());
        }

        return response;
    }

    @SuppressWarnings("unchecked")
    private AIResponse executeToolLoop(AIResponse response,
                                       AIProviderClient<AIProviderRequest> providerClient,
                                       ProtifyAIRequest baseRequest) {
        int rounds = 0;
        while (response.hasToolCalls() && rounds < maxToolRounds) {
            rounds++;

            // Execute handlers and collect results
            List<AIToolResult> results = new ArrayList<>();
            for (AIToolCall call : response.getToolCalls()) {
                AIToolHandler handler = toolHandlers.get(call.getName());
                if (handler != null) {
                    try {
                        String result = handler.execute(call.getArguments());
                        results.add(new AIToolResult(call.getId(), result));
                    } catch (Exception e) {
                        results.add(new AIToolResult(call.getId(), e.getMessage(), true));
                    }
                }
            }

            // Create tool-result user message and append to history
            AIMessage toolResultMessage = ProtifyAIMessage.userWithToolResults(results);
            messages.add(toolResultMessage);

            // Build new request with updated message history
            ProtifyAIRequest followUp = new ProtifyAIRequest(
                    client,
                    new ArrayList<>(),
                    properties,
                    tools,
                    Collections.emptyMap(),
                    Collections.emptyList(),
                    null,
                    maxToolRounds - rounds,
                    new ArrayList<>(messages)
            );

            AIProviderRequest providerRequest = client.getProviderClient()
                    .transformRequest(followUp, followUp.deriveConfiguration(null));
            response = providerClient.execute(providerRequest);

            // Replace last assistant message with updated response
            AIMessage newAssistantMessage = ProtifyAIMessage.fromResponse(response);
            messages.add(newAssistantMessage);
        }

        return response;
    }

    @Override
    public String getId() {
        return conversationId;
    }

    @Override
    public List<AIMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    @Override
    public AIConversationState getState() {
        return new AIConversationState(conversationId, messages);
    }

    @Override
    public void clear() {
        messages.clear();
    }
}
