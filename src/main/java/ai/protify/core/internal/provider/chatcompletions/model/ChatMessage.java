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

package ai.protify.core.internal.provider.chatcompletions.model;

import ai.protify.core.internal.util.json.ProtifyJsonProperty;

import java.util.List;

public final class ChatMessage {

    private String role;
    private Object content;

    @ProtifyJsonProperty("tool_calls")
    private List<ChatToolCall> toolCalls;

    @ProtifyJsonProperty("tool_call_id")
    private String toolCallId;

    private ChatMessage() {
    }

    public static ChatMessage system(String content) {
        ChatMessage msg = new ChatMessage();
        msg.role = "system";
        msg.content = content;
        return msg;
    }

    public static ChatMessage user(String content) {
        ChatMessage msg = new ChatMessage();
        msg.role = "user";
        msg.content = content;
        return msg;
    }

    public static ChatMessage userMultipart(List<ChatContentBlock> content) {
        ChatMessage msg = new ChatMessage();
        msg.role = "user";
        msg.content = content;
        return msg;
    }

    public static ChatMessage assistant(String content) {
        ChatMessage msg = new ChatMessage();
        msg.role = "assistant";
        msg.content = content;
        return msg;
    }

    public static ChatMessage assistantWithToolCalls(String content, List<ChatToolCall> toolCalls) {
        ChatMessage msg = new ChatMessage();
        msg.role = "assistant";
        msg.content = content;
        msg.toolCalls = toolCalls;
        return msg;
    }

    public static ChatMessage toolResult(String toolCallId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.role = "tool";
        msg.content = content;
        msg.toolCallId = toolCallId;
        return msg;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    @ProtifyJsonProperty("tool_calls")
    public List<ChatToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ChatToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    @ProtifyJsonProperty("tool_call_id")
    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }
}
