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

package com.protify.ai.internal.provider.openai.model;

import com.protify.ai.internal.util.json.ProtifyJsonProperty;

import java.util.List;

public class OpenAIOutputItem {

    private String type;

    // "message" fields
    private String role;
    private List<OpenAIOutputContent> content;

    // "function_call" fields
    @ProtifyJsonProperty("call_id")
    private String callId;

    private String name;
    private String arguments;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<OpenAIOutputContent> getContent() {
        return content;
    }

    public void setContent(List<OpenAIOutputContent> content) {
        this.content = content;
    }

    @ProtifyJsonProperty("call_id")
    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }
}
