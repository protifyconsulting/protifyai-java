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

package ai.protify.core.internal.response;

import ai.protify.core.internal.pipeline.PipelineAIResponse;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ProtifyAIStreamResponse implements AIStreamResponse {

    // Single lock guards both `listeners` and `accumulated` so a listener registered
    // mid-stream atomically observes the accumulated buffer at registration time
    // and never misses or duplicates a subsequent token.
    private final Object lock = new Object();
    private final List<Consumer<String>> listeners = new ArrayList<>();
    private final StringBuilder accumulated = new StringBuilder();
    private final CompletableFuture<AIResponse> completion = new CompletableFuture<>();

    @Override
    public void onToken(Consumer<String> listener) {
        String replay;
        synchronized (lock) {
            replay = accumulated.toString();
            listeners.add(listener);
        }
        // Replay outside the lock to avoid blocking pushToken on slow listeners.
        if (!replay.isEmpty()) {
            listener.accept(replay);
        }
    }

    @Override
    public AIResponse toResponse() {
        return completion.join();
    }

    public void pushToken(String token) {
        List<Consumer<String>> snapshot;
        synchronized (lock) {
            accumulated.append(token);
            snapshot = new ArrayList<>(listeners);
        }
        for (Consumer<String> listener : snapshot) {
            listener.accept(token);
        }
    }

    public void complete(AIResponse response) {
        completion.complete(response);
    }

    public void completeWithAccumulatedText() {
        String text;
        synchronized (lock) {
            text = accumulated.toString();
        }
        completion.complete(PipelineAIResponse.of(text));
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
