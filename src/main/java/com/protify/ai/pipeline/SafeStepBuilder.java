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

package com.protify.ai.pipeline;

import com.protify.ai.internal.response.ProtifyAIStreamResponse;
import com.protify.ai.response.AIResponse;
import com.protify.ai.response.AIStreamResponse;

import java.util.function.BiFunction;

public class SafeStepBuilder {

    private PipelineStep step;
    private int maxRetries = 0;
    private BiFunction<AIPipelineContext, Exception, AIResponse> onError;

    public SafeStepBuilder step(PipelineStep step) {
        this.step = step;
        return this;
    }

    public SafeStepBuilder maxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public SafeStepBuilder onError(BiFunction<AIPipelineContext, Exception, AIResponse> onError) {
        this.onError = onError;
        return this;
    }

    PipelineStep build() {
        if (step == null) {
            throw new IllegalStateException("SafeStepBuilder requires a step");
        }

        PipelineStep innerStep = step;
        int retries = maxRetries;
        BiFunction<AIPipelineContext, Exception, AIResponse> fallback = onError;

        return new PipelineStep() {
            @Override
            public AIResponse execute(AIPipelineContext context) {
                Exception lastException = null;
                for (int attempt = 0; attempt <= retries; attempt++) {
                    try {
                        return innerStep.execute(context);
                    } catch (Exception ex) {
                        lastException = ex;
                    }
                }
                if (fallback != null) {
                    return fallback.apply(context, lastException);
                }
                throw lastException instanceof RuntimeException
                        ? (RuntimeException) lastException
                        : new RuntimeException(lastException);
            }

            @Override
            public AIStreamResponse executeStream(AIPipelineContext context) {
                Exception lastException = null;
                for (int attempt = 0; attempt <= retries; attempt++) {
                    try {
                        if (attempt < retries) {
                            innerStep.execute(context);
                            return innerStep.executeStream(context);
                        }
                        return innerStep.executeStream(context);
                    } catch (Exception ex) {
                        lastException = ex;
                    }
                }
                if (fallback != null) {
                    return ProtifyAIStreamResponse.completed(fallback.apply(context, lastException));
                }
                throw lastException instanceof RuntimeException
                        ? (RuntimeException) lastException
                        : new RuntimeException(lastException);
            }
        };
    }
}
