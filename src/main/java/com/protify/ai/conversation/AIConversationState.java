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

package com.protify.ai.conversation;

import com.protify.ai.internal.message.ProtifyAIMessage;
import com.protify.ai.internal.util.json.ProtifyJson;
import com.protify.ai.message.AIMessage;

import java.util.*;

public class AIConversationState {

    private String conversationId;
    private List<AIMessage> messages;

    public AIConversationState() {
        this.messages = new ArrayList<>();
    }

    public AIConversationState(String conversationId, List<AIMessage> messages) {
        this.conversationId = conversationId;
        this.messages = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
    }

    public String getId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public List<AIMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void setMessages(List<AIMessage> messages) {
        this.messages = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
    }

    public String toJson() {
        List<Map<String, Object>> serializedMessages = new ArrayList<>();
        for (AIMessage message : messages) {
            if (message instanceof ProtifyAIMessage) {
                serializedMessages.add(((ProtifyAIMessage) message).toSerializableMap());
            } else {
                // Wrap non-internal messages as simple text messages
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("role", message.getRole());
                if (message.getText() != null) {
                    map.put("text", message.getText());
                }
                serializedMessages.add(map);
            }
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("conversationId", conversationId);
        root.put("messages", serializedMessages);

        return ProtifyJson.toJsonMap(root);
    }

    @SuppressWarnings("unchecked")
    public static AIConversationState fromJson(String json) {
        Object root = ProtifyJson.parse(json).get("");
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("Invalid conversation state JSON");
        }

        Map<String, Object> map = (Map<String, Object>) root;
        String conversationId = (String) map.get("conversationId");

        List<AIMessage> messages = new ArrayList<>();
        Object messagesObj = map.get("messages");
        if (messagesObj instanceof List) {
            for (Object item : (List<?>) messagesObj) {
                if (item instanceof Map) {
                    messages.add(ProtifyAIMessage.fromSerializableMap((Map<String, Object>) item));
                }
            }
        }

        return new AIConversationState(conversationId, messages);
    }
}
