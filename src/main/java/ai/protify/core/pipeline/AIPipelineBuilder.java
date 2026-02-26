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

import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.pipeline.ProtifyAIPipeline;
import ai.protify.core.request.AIRequest;
import ai.protify.core.resiliency.RetryPolicy;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;
import ai.protify.core.internal.pipeline.PipelineAIResponse;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AIPipelineBuilder {

    private Supplier<AIRequest> initialStep;
    private final List<PipelineStep> steps = new ArrayList<>();
    private Consumer<String> onStepComplete;
    private boolean inheritParentConfig = false;

    private final Map<AIConfigProperty, Object> pipelineProperties = new EnumMap<>(AIConfigProperty.class);

    public AIPipelineBuilder instructions(String instructions) {
        this.pipelineProperties.put(AIConfigProperty.INSTRUCTIONS, instructions);
        return this;
    }

    public AIPipelineBuilder prettyPrint(boolean prettyPrint) {
        this.pipelineProperties.put(AIConfigProperty.PRETTY_PRINT_JSON, prettyPrint);
        return this;
    }

    public AIPipelineBuilder logRequests(boolean logRequests) {
        this.pipelineProperties.put(AIConfigProperty.LOG_REQUESTS, logRequests);
        return this;
    }

    public AIPipelineBuilder logResponses(boolean logResponses) {
        this.pipelineProperties.put(AIConfigProperty.LOG_RESPONSES, logResponses);
        return this;
    }

    public AIPipelineBuilder temperature(double temperature) {
        this.pipelineProperties.put(AIConfigProperty.TEMPERATURE, temperature);
        return this;
    }

    public AIPipelineBuilder topP(double topP) {
        this.pipelineProperties.put(AIConfigProperty.TOP_P, topP);
        return this;
    }

    public AIPipelineBuilder topK(int topK) {
        this.pipelineProperties.put(AIConfigProperty.TOP_K, topK);
        return this;
    }

    public AIPipelineBuilder maxOutputTokens(int maxOutputTokens) {
        this.pipelineProperties.put(AIConfigProperty.MAX_OUTPUT_TOKENS, maxOutputTokens);
        return this;
    }

    public AIPipelineBuilder truncateLongRequestInputs(boolean truncate) {
        this.pipelineProperties.put(AIConfigProperty.LOG_TRUNCATE_LARGE_INPUT, truncate);
        return this;
    }

    public AIPipelineBuilder retryPolicy(RetryPolicy retryPolicy) {
        this.pipelineProperties.put(AIConfigProperty.RETRY_POLICY, retryPolicy);
        return this;
    }

    public AIPipelineBuilder inheritParentConfig() {
        this.inheritParentConfig = true;
        return this;
    }

    public AIPipelineBuilder onStepComplete(Consumer<String> callback) {
        this.onStepComplete = callback;
        return this;
    }

    public AIPipelineBuilder withInitialStep(Supplier<AIRequest> initialStep) {
        this.initialStep = initialStep;
        return this;
    }

    public AIPipelineBuilder addStep(PipelineStep step) {
        steps.add(step);
        return this;
    }

    public AIPipelineBuilder addRequestStep(Function<AIPipelineContext, AIRequest> nextRequestLogic) {
        this.steps.add(new PipelineStep() {
            @Override
            public AIResponse execute(AIPipelineContext context) {
                return nextRequestLogic.apply(context).execute(context);
            }

            @Override
            public AIStreamResponse executeStream(AIPipelineContext context) {
                return nextRequestLogic.apply(context).executeStream(context);
            }
        });
        return this;
    }

    public AIPipelineBuilder addParallelStep(List<PipelineStep> parallelSteps) {
        this.steps.add(previousResponse -> {
            List<CompletableFuture<AIResponse>> futures = parallelSteps.stream()
                    .map(step -> CompletableFuture.supplyAsync(() -> step.execute(previousResponse)))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            String joinedText = futures.stream()
                    .map(f -> f.join().text())
                    .collect(Collectors.joining("\n---\n"));

            return PipelineAIResponse.of(joinedText);
        });
        return this;
    }

    public AIPipelineBuilder addConditionalStep(Function<ConditionalStepBuilder, ConditionalStepBuilder> config) {
        ConditionalStepBuilder builder = new ConditionalStepBuilder();
        config.apply(builder);
        this.steps.add(builder.build());
        return this;
    }

    public AIPipelineBuilder addSafeStep(Function<SafeStepBuilder, SafeStepBuilder> config) {
        SafeStepBuilder builder = new SafeStepBuilder();
        config.apply(builder);
        this.steps.add(builder.build());
        return this;
    }

    public AIPipelineBuilder addLoopStep(Function<LoopStepBuilder, LoopStepBuilder> config) {
        LoopStepBuilder builder = new LoopStepBuilder();
        config.apply(builder);
        this.steps.add(builder.build());
        return this;
    }

    public AIPipeline build() {
        return new ProtifyAIPipeline(initialStep, steps, onStepComplete, pipelineProperties, inheritParentConfig);
    }
}
