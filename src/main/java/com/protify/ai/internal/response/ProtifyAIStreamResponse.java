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

package com.protify.ai.internal.response;

import com.protify.ai.internal.pipeline.PipelineAIResponse;
import com.protify.ai.response.AIResponse;
import com.protify.ai.response.AIStreamResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ProtifyAIStreamResponse implements AIStreamResponse {

    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private final StringBuilder accumulated = new StringBuilder();
    private final CompletableFuture<AIResponse> completion = new CompletableFuture<>();

    @Override
    public void onToken(Consumer<String> listener) {
        listeners.add(listener);
    }

    @Override
    public AIResponse toResponse() {
        return completion.join();
    }

    public void pushToken(String token) {
        accumulated.append(token);
        for (Consumer<String> listener : listeners) {
            listener.accept(token);
        }
    }

    public void complete(AIResponse response) {
        completion.complete(response);
    }

    public void completeWithAccumulatedText() {
        completion.complete(PipelineAIResponse.of(accumulated.toString()));
    }

    public void completeExceptionally(Throwable ex) {
        completion.completeExceptionally(ex);
    }

    public static ProtifyAIStreamResponse completed(AIResponse response) {
        ProtifyAIStreamResponse stream = new ProtifyAIStreamResponse();
        stream.completion.complete(response);
        return stream;
    }
}
