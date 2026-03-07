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

package ai.protify.core.provider;

import ai.protify.core.internal.config.Configuration;
import ai.protify.core.message.AIMessage;
import ai.protify.core.request.AIInput;
import ai.protify.core.request.AIRequest;
import ai.protify.core.response.AIResponse;
import ai.protify.core.tool.AITool;
import ai.protify.core.tool.AIToolResult;

import java.util.Collections;
import java.util.List;

public interface AIProviderRequest {

    void initialize(AIRequest request, Configuration derivedConfiguration);

    String getModelName();

    AIProvider getProvider();

    List<AIInput> getInputs();

    Configuration getConfiguration();

    String toJson();

    String toLoggableJson();

    default List<AITool> getTools() {
        return Collections.emptyList();
    }

    default List<AIToolResult> getToolResults() {
        return Collections.emptyList();
    }

    default AIResponse getPreviousAssistantResponse() {
        return null;
    }

    default List<AIMessage> getMessages() {
        return Collections.emptyList();
    }
}
