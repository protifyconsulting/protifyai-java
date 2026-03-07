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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ConditionalStepBuilder {

    private final List<ConditionalBranch> branches = new ArrayList<>();
    private PipelineStep otherwiseStep;

    public ConditionalStepBuilder when(Predicate<AIPipelineContext> condition, PipelineStep step) {
        branches.add(new ConditionalBranch(condition, step));
        return this;
    }

    public ConditionalStepBuilder otherwise(PipelineStep step) {
        this.otherwiseStep = step;
        return this;
    }

    PipelineStep build() {
        List<ConditionalBranch> branchesCopy = List.copyOf(branches);
        PipelineStep fallback = otherwiseStep;

        return new PipelineStep() {
            @Override
            public AIResponse execute(AIPipelineContext context) {
                for (ConditionalBranch branch : branchesCopy) {
                    if (branch.condition.test(context)) {
                        return branch.step.execute(context);
                    }
                }
                if (fallback != null) {
                    return fallback.execute(context);
                }
                return context.response();
            }

            @Override
            public AIStreamResponse executeStream(AIPipelineContext context) {
                for (ConditionalBranch branch : branchesCopy) {
                    if (branch.condition.test(context)) {
                        return branch.step.executeStream(context);
                    }
                }
                if (fallback != null) {
                    return fallback.executeStream(context);
                }
                return ProtifyAIStreamResponse.completed(context.response());
            }
        };
    }

    private static class ConditionalBranch {
        final Predicate<AIPipelineContext> condition;
        final PipelineStep step;

        ConditionalBranch(Predicate<AIPipelineContext> condition, PipelineStep step) {
            this.condition = condition;
            this.step = step;
        }
    }
}
