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

package com.protify.ai.request;

import com.protify.ai.AIClient;
import com.protify.ai.internal.config.Configuration;
import com.protify.ai.message.AIMessage;
import com.protify.ai.pipeline.AIPipelineContext;
import com.protify.ai.provider.AIProviderClient;
import com.protify.ai.response.AIResponse;
import com.protify.ai.response.AIStreamResponse;
import com.protify.ai.tool.AITool;

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