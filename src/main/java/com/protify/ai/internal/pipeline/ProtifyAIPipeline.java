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

package com.protify.ai.internal.pipeline;

import com.protify.ai.internal.config.AIConfigProperty;
import com.protify.ai.pipeline.AIPipeline;
import com.protify.ai.pipeline.AIPipelineContext;
import com.protify.ai.pipeline.AIPipelineResponse;
import com.protify.ai.pipeline.PipelineStep;
import com.protify.ai.request.AIRequest;
import com.protify.ai.response.AIResponse;
import com.protify.ai.response.AIStreamResponse;
import com.protify.ai.internal.util.Logger;
import com.protify.ai.internal.util.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProtifyAIPipeline implements AIPipeline {

        /*
        .addAgentStep(agent -> agent // Agentic logic INSIDE a pipeline step
        .withGoal("Identify any legal risks in the 'Indemnity' section")
        .withTools(legalDatabaseTool)

        add retry policy to AIPipelineBuilder

        PipelineContext object to get data shared across all steps

        Pipeline.execute should also have Pipeline.executeAsync

        Validation hooks between steps:
        .addStep(prev -> prev.text().toUpperCase())
        .validate(prev -> prev.text().contains("REQUIRED_TERM")) // If this fails, the pipeline throws a PipelineValidationException

        What a "Specific" Validation Hook would look like:
Instead of just a Function<AIResponse, AIResponse>, you might offer:
Predicate Validation: validate(Predicate<AIResponse> condition, String errorMessage)
Simple: "Does the response contain at least 100 words?"
Schema Validation: validateJsonSchema(String schema)
Advanced: "Did the LLM actually return a valid JSON table like I asked?"

        Token usage tracking

    )
     */

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtifyAIPipeline.class);

    private final Map<AIConfigProperty, Object> pipelineProperties;
    private final AIPipelineContext context;
    private final Supplier<AIRequest> initialStep;
    private final List<PipelineStep> steps = new ArrayList<>();
    private final Consumer<String> onStepComplete;
    private final boolean inheritParentConfig;

    public ProtifyAIPipeline(Supplier<AIRequest> initialStep,
                             List<PipelineStep> steps,
                             Consumer<String> onStepComplete,
                             Map<AIConfigProperty, Object> properties,
                             boolean inheritParentConfig) {
        this.initialStep = initialStep;
        this.steps.addAll(steps);
        this.onStepComplete = onStepComplete;
        this.pipelineProperties = properties;
        this.inheritParentConfig = inheritParentConfig;
        this.context = new ProtifyAIPipelineContext(properties);
    }

    // --- Standalone execution (no parent context) ---

    @Override
    public AIPipelineResponse execute() {
        return doExecute(this.context);
    }

    @Override
    public CompletableFuture<AIPipelineResponse> executeAsync() {
        return CompletableFuture.supplyAsync(this::execute);
    }

    @Override
    public AIStreamResponse executeStream() {
        return doExecuteStream(this.context);
    }

    // --- PipelineStep implementation (used as a sub-step within a parent pipeline) ---

    @Override
    public AIResponse execute(AIPipelineContext parentContext) {
        return doExecute(createInnerContext(parentContext));
    }

    @Override
    public AIStreamResponse executeStream(AIPipelineContext parentContext) {
        return doExecuteStream(createInnerContext(parentContext));
    }

    // --- Shared execution logic ---

    private AIPipelineResponse doExecute(AIPipelineContext ctx) {

        if (initialStep == null) {
            throw new IllegalStateException("Pipeline must have an initial step defined.");
        }

        List<AIResponse> stepResponses = new ArrayList<>();

        AIResponse currentResponse = initialStep.get().execute(ctx);
        stepResponses.add(currentResponse);
        ctx.setPreviousStepResponse(currentResponse);
        notifyListener(currentResponse.text());
        LOGGER.debug("Pipeline initial step executed.  Response {} ", currentResponse.text());

        for (PipelineStep step : steps) {
            ctx.setPreviousStepResponse(currentResponse);
            currentResponse = step.execute(ctx);
            stepResponses.add(currentResponse);
            LOGGER.debug("Pipeline step executed.  Response {} ", currentResponse.text());
            notifyListener(currentResponse.text());
        }
        return new ProtifyAIPipelineResponse(currentResponse, stepResponses);
    }

    private AIStreamResponse doExecuteStream(AIPipelineContext ctx) {

        if (initialStep == null) {
            throw new IllegalStateException("Pipeline must have an initial step defined.");
        }

        if (steps.isEmpty()) {
            return initialStep.get().executeStream(ctx);
        }

        AIResponse currentResponse = initialStep.get().execute(ctx);
        ctx.setPreviousStepResponse(currentResponse);
        notifyListener(currentResponse.text());
        LOGGER.debug("Pipeline initial step executed (stream mode).  Response {} ", currentResponse.text());

        for (int i = 0; i < steps.size() - 1; i++) {
            ctx.setPreviousStepResponse(currentResponse);
            currentResponse = steps.get(i).execute(ctx);
            LOGGER.debug("Pipeline intermediate step executed (stream mode).  Response {} ", currentResponse.text());
            notifyListener(currentResponse.text());
        }

        ctx.setPreviousStepResponse(currentResponse);
        PipelineStep lastStep = steps.get(steps.size() - 1);
        return lastStep.executeStream(ctx);
    }

    // --- Context bridging ---

    private AIPipelineContext createInnerContext(AIPipelineContext parentContext) {
        Map<AIConfigProperty, Object> mergedProperties;

        if (inheritParentConfig) {
            mergedProperties = new EnumMap<>(AIConfigProperty.class);
            mergedProperties.putAll(parentContext.getPipelineProperties());
            mergedProperties.putAll(this.pipelineProperties);
        } else {
            mergedProperties = new EnumMap<>(AIConfigProperty.class);
            mergedProperties.putAll(this.pipelineProperties);
        }

        AIPipelineContext innerContext = new ProtifyAIPipelineContext(mergedProperties);
        innerContext.setPreviousStepResponse(parentContext.getPreviousStepResponse());
        return innerContext;
    }

    private void notifyListener(String output) {
        if (onStepComplete != null) {
            onStepComplete.accept(output);
        }
    }

}
