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

package com.protify.ai.provider;

import com.protify.ai.internal.config.Configuration;
import com.protify.ai.internal.request.ProtifyAIRequest;
import com.protify.ai.message.AIMessage;
import com.protify.ai.request.AIInput;
import com.protify.ai.request.AIRequest;
import com.protify.ai.response.AIResponse;
import com.protify.ai.tool.AITool;
import com.protify.ai.tool.AIToolResult;

import java.util.Collections;
import java.util.List;

public abstract class ProtifyAIProviderRequest implements AIProviderRequest {

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
}
