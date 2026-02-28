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

package ai.protify.core.provider;

import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.config.Configuration;
import ai.protify.core.internal.request.ProtifyAIRequest;
import ai.protify.core.internal.util.Logger;
import ai.protify.core.internal.util.LoggerFactory;
import ai.protify.core.message.AIMessage;
import ai.protify.core.request.AIInput;
import ai.protify.core.request.AIRequest;
import ai.protify.core.response.AIResponse;
import ai.protify.core.tool.AITool;
import ai.protify.core.tool.AIToolResult;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public abstract class ProtifyAIProviderRequest implements AIProviderRequest {

    private static final Logger log = LoggerFactory.getLogger(ProtifyAIProviderRequest.class);

    private Configuration configuration;
    private AIProvider provider;
    private String modelName;
    private List<AIInput> inputs;
    private List<AITool> tools = Collections.emptyList();
    private List<AIToolResult> toolResults = Collections.emptyList();
    private AIResponse previousAssistantResponse;
    private List<AIMessage> messages = Collections.emptyList();

    @Override
    public void initialize(AIRequest request,
                           Configuration derivedConfiguration) {
        this.provider = request.getClient().getProvider();
        this.inputs = request.getInputs();
        this.modelName = request.getClient().getModelName();
        this.configuration = derivedConfiguration;
        this.tools = request.getTools();

        this.messages = request.getMessages();

        if (request instanceof ProtifyAIRequest) {
            ProtifyAIRequest protifyRequest = (ProtifyAIRequest) request;
            this.toolResults = protifyRequest.getToolResults();
            this.previousAssistantResponse = protifyRequest.getPreviousAssistantResponse();
        }

        filterUnsupportedParameters();
    }

    @Override
    public AIProvider getProvider() {
        return this.provider;
    }

    @Override
    public String getModelName() {
        return this.modelName;
    }

    @Override
    public List<AIInput> getInputs() {
        return this.inputs;
    }

    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }

    @Override
    public List<AITool> getTools() {
        return this.tools;
    }

    @Override
    public List<AIToolResult> getToolResults() {
        return this.toolResults;
    }

    @Override
    public AIResponse getPreviousAssistantResponse() {
        return this.previousAssistantResponse;
    }

    @Override
    public List<AIMessage> getMessages() {
        return this.messages;
    }

    protected Set<AIConfigProperty> getUnsupportedParameters() {
        return Collections.emptySet();
    }

    protected Set<AIConfigProperty> getUnsupportedParametersForModel(String model) {
        return Collections.emptySet();
    }

    private void filterUnsupportedParameters() {
        Set<AIConfigProperty> unsupported = EnumSet.noneOf(AIConfigProperty.class);
        unsupported.addAll(getUnsupportedParameters());
        unsupported.addAll(getUnsupportedParametersForModel(this.modelName));

        for (AIConfigProperty property : unsupported) {
            if (this.configuration.getProperty(property) != null) {
                log.warn("Parameter '{}' is not supported by model '{}' and will be excluded from the request.",
                        property.getName(), this.modelName);
                this.configuration.suppressProperty(property);
            }
        }
    }
}
