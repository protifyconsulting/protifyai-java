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

package ai.protify.core.internal.request;

import ai.protify.core.AIClient;
import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.config.BaseConfiguration;
import ai.protify.core.internal.config.Configuration;
import ai.protify.core.internal.config.DerivedProperties;
import ai.protify.core.message.AIMessage;
import ai.protify.core.pipeline.AIPipelineContext;
import ai.protify.core.provider.AIProviderClient;
import ai.protify.core.provider.AIProviderRequest;
import ai.protify.core.request.AIInput;
import ai.protify.core.request.AIRequest;
import ai.protify.core.request.AITextInput;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;
import ai.protify.core.tool.AITool;
import ai.protify.core.tool.AIToolCall;
import ai.protify.core.tool.AIToolHandler;
import ai.protify.core.tool.AIToolResult;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ProtifyAIRequest implements AIRequest {

    private final AIProviderClient<AIProviderRequest> internalProviderClient;
    private final Configuration configuration;

    private final AIClient client;
    private final List<AIInput> inputs;
    private final List<AITool> tools;
    private final Map<String, AIToolHandler> toolHandlers;
    private final List<AIToolResult> toolResults;
    private final AIResponse previousAssistantResponse;
    private final int maxToolRounds;
    private final List<AIMessage> messages;

    @SuppressWarnings("unchecked")
    public ProtifyAIRequest(
            AIClient client,
            List<AIInput> inputs,
            Map<AIConfigProperty, Object> properties) {
        this(client, inputs, properties, Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyList(), null, 10);
    }

    @SuppressWarnings("unchecked")
    public ProtifyAIRequest(
            AIClient client,
            List<AIInput> inputs,
            Map<AIConfigProperty, Object> properties,
            List<AITool> tools,
            Map<String, AIToolHandler> toolHandlers,
            List<AIToolResult> toolResults,
            AIResponse previousAssistantResponse,
            int maxToolRounds) {
        this(client, inputs, properties, tools, toolHandlers, toolResults,
                previousAssistantResponse, maxToolRounds, Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    public ProtifyAIRequest(
            AIClient client,
            List<AIInput> inputs,
            Map<AIConfigProperty, Object> properties,
            List<AITool> tools,
            Map<String, AIToolHandler> toolHandlers,
            List<AIToolResult> toolResults,
            AIResponse previousAssistantResponse,
            int maxToolRounds,
            List<AIMessage> messages) {
        this.client = client;
        this.inputs = inputs;
        this.configuration = new Configuration(properties);
        this.tools = tools != null ? tools : Collections.emptyList();
        this.toolHandlers = toolHandlers != null ? toolHandlers : Collections.emptyMap();
        this.toolResults = toolResults != null ? toolResults : Collections.emptyList();
        this.previousAssistantResponse = previousAssistantResponse;
        this.maxToolRounds = maxToolRounds;
        this.messages = messages != null ? messages : Collections.emptyList();

        this.internalProviderClient = (AIProviderClient<AIProviderRequest>) client.getProviderClient();
    }

    @Override
    public AIResponse execute() {
        Configuration derivedConfiguration = this.deriveConfiguration(null);
        AIProviderRequest providerRequest = this.client.getProviderClient()
                .transformRequest(this, derivedConfiguration);
        AIResponse response = this.internalProviderClient.execute(providerRequest);

        if (!toolHandlers.isEmpty() && response.hasToolCalls()) {
            response = executeToolLoop(response, derivedConfiguration);
        }

        return response;
    }

    @Override
    public AIResponse execute(AIPipelineContext pipelineContext) {

        if (pipelineContext.getPreviousStepResponse() != null) {
            this.inputs.add(AITextInput.of(pipelineContext.getPreviousStepResponse().text()));
        }

        Configuration pipelineCfg = new Configuration(pipelineContext.getPipelineProperties());
        Configuration derivedConfiguration = this.deriveConfiguration(pipelineCfg);
        AIProviderRequest providerRequest = this.client.getProviderClient()
                .transformRequest(this, derivedConfiguration);
        AIResponse response = this.internalProviderClient.execute(providerRequest);

        if (!toolHandlers.isEmpty() && response.hasToolCalls()) {
            response = executeToolLoop(response, derivedConfiguration);
        }

        return response;
    }

    private AIResponse executeToolLoop(AIResponse response, Configuration derivedConfiguration) {
        int rounds = 0;
        while (response.hasToolCalls() && rounds < maxToolRounds) {
            rounds++;

            List<AIToolResult> results = new ArrayList<>();
            for (AIToolCall call : response.getToolCalls()) {
                AIToolHandler handler = toolHandlers.get(call.getName());
                if (handler != null) {
                    try {
                        String result = handler.execute(call.getArguments());
                        results.add(new AIToolResult(call.getId(), result));
                    } catch (Exception e) {
                        results.add(new AIToolResult(call.getId(), e.getMessage(), true));
                    }
                }
            }

            ProtifyAIRequest followUp = new ProtifyAIRequest(
                    client,
                    new ArrayList<>(inputs),
                    new EnumMap<>(AIConfigProperty.class),
                    tools,
                    toolHandlers,
                    results,
                    response,
                    maxToolRounds - rounds
            );

            AIProviderRequest providerRequest = this.client.getProviderClient()
                    .transformRequest(followUp, derivedConfiguration);
            response = this.internalProviderClient.execute(providerRequest);
        }
        return response;
    }

    @Override
    public CompletableFuture<AIResponse> executeAsync() {
        return CompletableFuture.supplyAsync(this::execute);
    }

    @Override
    public CompletableFuture<AIResponse> executeAsync(AIPipelineContext pipelineContext) {
        return CompletableFuture.supplyAsync(() -> execute(pipelineContext));
    }

    @Override
    public AIStreamResponse executeStream() {
        Configuration derivedConfiguration = this.deriveConfiguration(null);
        AIProviderRequest providerRequest = this.client.getProviderClient()
                .transformRequest(this, derivedConfiguration);
        return this.internalProviderClient.executeStream(providerRequest);
    }

    @Override
    public AIStreamResponse executeStream(AIPipelineContext pipelineContext) {
        if (pipelineContext.getPreviousStepResponse() != null) {
            this.inputs.add(AITextInput.of(pipelineContext.getPreviousStepResponse().text()));
        }

        Configuration pipelineCfg = new Configuration(pipelineContext.getPipelineProperties());
        Configuration derivedConfiguration = this.deriveConfiguration(pipelineCfg);
        AIProviderRequest providerRequest = this.client.getProviderClient()
                .transformRequest(this, derivedConfiguration);
        return this.internalProviderClient.executeStream(providerRequest);
    }

    @Override
    public AIClient getClient() {
        return this.client;
    }

    @Override
    public AIProviderClient<?> getProviderClient() {
        return this.client.getProviderClient();
    }

    @Override
    public List<AIInput> getInputs() {
        return this.inputs;
    }

    @Override
    public List<AITool> getTools() {
        return this.tools;
    }

    @Override
    public List<AIMessage> getMessages() {
        return this.messages;
    }

    public List<AIToolResult> getToolResults() {
        return this.toolResults;
    }

    public AIResponse getPreviousAssistantResponse() {
        return this.previousAssistantResponse;
    }

    public Map<String, AIToolHandler> getToolHandlers() {
        return this.toolHandlers;
    }

    public int getMaxToolRounds() {
        return this.maxToolRounds;
    }

    @Override
    public String toJson() {
        Configuration derivedConfiguration = this.deriveConfiguration(null);
        AIProviderRequest providerRequest = this.client.getProviderClient()
                .transformRequest(this, derivedConfiguration);
        return providerRequest.toJson();
    }

    @Override
    public String toLoggableJson() {
        Configuration derivedConfiguration = this.deriveConfiguration(null);
        AIProviderRequest providerRequest = this.client.getProviderClient()
                .transformRequest(this, derivedConfiguration);
        return providerRequest.toLoggableJson();
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    public Configuration deriveConfiguration(Configuration pipelineCfg) {
        boolean clientOverride = Boolean.TRUE.equals(this.client.getConfiguration().getProperty(AIConfigProperty.OVERRIDE_PIPELINE_CONFIG));
        boolean requestOverride = Boolean.TRUE.equals(this.configuration.getProperty(AIConfigProperty.OVERRIDE_PIPELINE_CONFIG));

        Map<AIConfigProperty, Object> derivedProperties = DerivedProperties.generate(
                BaseConfiguration.getBaseConfiguration(),
                pipelineCfg,
                this.getClient().getConfiguration(),
                this.getConfiguration(),
                clientOverride,
                requestOverride
        );

        if (inputs.isEmpty() && derivedProperties.get(AIConfigProperty.INSTRUCTIONS) == null) {
            throw new IllegalArgumentException("No instructions or inputs provided.  Nothing to send/process!");
        }
        return new Configuration(derivedProperties);
    }
}
