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

package ai.protify.core.conversation;

import ai.protify.core.AIClient;
import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.conversation.ProtifyAIConversation;
import ai.protify.core.message.AIMessage;
import ai.protify.core.tool.AITool;
import ai.protify.core.tool.AIToolHandler;

import java.util.*;

public class AIConversationBuilder {

    private final AIClient client;
    private final Map<AIConfigProperty, Object> properties = new EnumMap<>(AIConfigProperty.class);
    private final List<AITool> tools = new ArrayList<>();
    private final Map<String, AIToolHandler> toolHandlers = new LinkedHashMap<>();

    private AIConversationStore store;
    private String id;
    private int maxToolRounds = 10;

    public AIConversationBuilder(AIClient client) {
        this.client = client;
    }

    public AIConversationBuilder store(AIConversationStore store) {
        this.store = store;
        return this;
    }

    public AIConversationBuilder id(String id) {
        this.id = id;
        return this;
    }

    public AIConversationBuilder instructions(String instructions) {
        this.properties.put(AIConfigProperty.INSTRUCTIONS, instructions);
        return this;
    }

    public AIConversationBuilder temperature(double temperature) {
        this.properties.put(AIConfigProperty.TEMPERATURE, temperature);
        return this;
    }

    public AIConversationBuilder maxOutputTokens(int maxOutputTokens) {
        this.properties.put(AIConfigProperty.MAX_OUTPUT_TOKENS, maxOutputTokens);
        return this;
    }

    public AIConversationBuilder topP(double topP) {
        this.properties.put(AIConfigProperty.TOP_P, topP);
        return this;
    }

    public AIConversationBuilder topK(int topK) {
        this.properties.put(AIConfigProperty.TOP_K, topK);
        return this;
    }

    public AIConversationBuilder addTool(AITool tool) {
        this.tools.add(tool);
        return this;
    }

    public AIConversationBuilder addTool(AITool tool, AIToolHandler handler) {
        this.tools.add(tool);
        this.toolHandlers.put(tool.getName(), handler);
        return this;
    }

    public AIConversationBuilder maxToolRounds(int maxToolRounds) {
        this.maxToolRounds = maxToolRounds;
        return this;
    }

    public AIConversation build() {
        String conversationId = this.id;
        List<AIMessage> existingMessages = Collections.emptyList();

        if (conversationId != null && store != null) {
            AIConversationState loaded = store.load(conversationId);
            if (loaded != null) {
                existingMessages = loaded.getMessages();
            }
        }

        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
        }

        return new ProtifyAIConversation(
                client,
                conversationId,
                new ArrayList<>(existingMessages),
                store,
                properties,
                tools,
                toolHandlers,
                maxToolRounds
        );
    }
}
