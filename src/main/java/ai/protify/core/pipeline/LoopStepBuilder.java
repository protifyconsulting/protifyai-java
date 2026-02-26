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

package ai.protify.core.pipeline;

import ai.protify.core.internal.response.ProtifyAIStreamResponse;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;

import java.util.function.Predicate;

public class LoopStepBuilder {

    private PipelineStep step;
    private Predicate<AIPipelineContext> until;
    private int maxIterations = 5;
    private PipelineStep onMaxIterations;

    public LoopStepBuilder step(PipelineStep step) {
        this.step = step;
        return this;
    }

    public LoopStepBuilder until(Predicate<AIPipelineContext> until) {
        this.until = until;
        return this;
    }

    public LoopStepBuilder maxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
        return this;
    }

    public LoopStepBuilder onMaxIterations(PipelineStep onMaxIterations) {
        this.onMaxIterations = onMaxIterations;
        return this;
    }

    PipelineStep build() {
        if (step == null) {
            throw new IllegalStateException("LoopStepBuilder requires a step");
        }
        if (until == null) {
            throw new IllegalStateException("LoopStepBuilder requires an until condition");
        }

        PipelineStep innerStep = step;
        Predicate<AIPipelineContext> condition = until;
        int max = maxIterations;
        PipelineStep maxFallback = onMaxIterations;

        return new PipelineStep() {
            @Override
            public AIResponse execute(AIPipelineContext context) {
                for (int i = 0; i < max; i++) {
                    AIResponse result = innerStep.execute(context);
                    context.setPreviousStepResponse(result);
                    if (condition.test(context)) {
                        return result;
                    }
                }
                if (maxFallback != null) {
                    return maxFallback.execute(context);
                }
                return context.response();
            }

            @Override
            public AIStreamResponse executeStream(AIPipelineContext context) {
                return ProtifyAIStreamResponse.completed(execute(context));
            }
        };
    }
}
