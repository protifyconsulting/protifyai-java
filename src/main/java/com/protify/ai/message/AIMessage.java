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

package com.protify.ai.message;

import com.protify.ai.internal.message.ProtifyAIMessage;
import com.protify.ai.request.AIInput;
import com.protify.ai.response.AIResponse;
import com.protify.ai.tool.AIToolCall;
import com.protify.ai.tool.AIToolResult;

import java.util.Collections;
import java.util.List;

public interface AIMessage {

    String getRole();

    String getText();

    default boolean hasToolCalls() {
        return !getToolCalls().isEmpty();
    }

    default List<AIToolCall> getToolCalls() {
        return Collections.emptyList();
    }

    default List<AIToolResult> getToolResults() {
        return Collections.emptyList();
    }

    default List<AIInput> getInputs() {
        return Collections.emptyList();
    }

    static AIMessage user(String text) {
        return ProtifyAIMessage.userText(text);
    }

    static AIMessage assistant(String text) {
        return ProtifyAIMessage.assistantText(text);
    }

    static AIMessage assistant(AIResponse response) {
        return ProtifyAIMessage.fromResponse(response);
    }
}
