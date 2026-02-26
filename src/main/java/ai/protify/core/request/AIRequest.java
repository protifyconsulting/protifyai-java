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

package ai.protify.core.request;

import ai.protify.core.AIClient;
import ai.protify.core.internal.config.Configuration;
import ai.protify.core.message.AIMessage;
import ai.protify.core.pipeline.AIPipelineContext;
import ai.protify.core.provider.AIProviderClient;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;
import ai.protify.core.tool.AITool;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AIRequest {

    AIResponse execute();

    AIResponse execute(AIPipelineContext context);

    CompletableFuture<AIResponse> executeAsync();

    CompletableFuture<AIResponse> executeAsync(AIPipelineContext context);

    AIStreamResponse executeStream();

    AIStreamResponse executeStream(AIPipelineContext context);

    String toJson();

    String toLoggableJson();

    List<AIInput> getInputs();

    AIClient getClient();

    @SuppressWarnings({"java:S1452"})
    AIProviderClient<?> getProviderClient();

    Configuration getConfiguration();

    default List<AITool> getTools() {
        return Collections.emptyList();
    }

    default List<AIMessage> getMessages() {
        return Collections.emptyList();
    }
}