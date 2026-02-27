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

public class ChatResponseMessage {

    private String role;
    private String content;

    @ProtifyJsonProperty("tool_calls")
    private List<ChatToolCall> toolCalls;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @ProtifyJsonProperty("tool_calls")
    public List<ChatToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ChatToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }
}
