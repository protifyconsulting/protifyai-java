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

package com.protify.ai.request;

import com.protify.ai.AIClient;
import com.protify.ai.internal.config.AIConfigProperty;
import com.protify.ai.internal.request.ProtifyAIRequest;
import com.protify.ai.resiliency.RetryPolicy;
import com.protify.ai.response.AIResponse;
import com.protify.ai.tool.AITool;
import com.protify.ai.tool.AIToolHandler;
import com.protify.ai.tool.AIToolResult;

import java.util.*;

public class AIRequestBuilder {

    private final AIClient client;
    private final List<AIInput> inputs = new ArrayList<>();
    private final Map<AIConfigProperty, Object> properties = new EnumMap<>(AIConfigProperty.class);
    private final List<AITool> tools = new ArrayList<>();
    private final Map<String, AIToolHandler> toolHandlers = new LinkedHashMap<>();
    private final List<AIToolResult> toolResults = new ArrayList<>();
    private AIResponse previousAssistantResponse;
    private int maxToolRounds = 10;

    public AIRequestBuilder(AIClient client) {
        this.client = client;
    }

    public AIRequestBuilder addInput(String input) {
        this.inputs.add(AITextInput.of(input));
        return this;
    }

    public AIRequestBuilder addInput(AIResponse response) {
        this.inputs.add(AITextInput.of(response.text()));
        return this;
    }

    public AIRequestBuilder addInput(AIInput input) {
        this.inputs.add(input);
        return this;
    }

    public AIRequestBuilder addTool(AITool tool) {
        this.tools.add(tool);
        return this;
    }

    public AIRequestBuilder addTool(AITool tool, AIToolHandler handler) {
        this.tools.add(tool);
        this.toolHandlers.put(tool.getName(), handler);
        return this;
    }

    public AIRequestBuilder addTools(List<AITool> tools) {
        this.tools.addAll(tools);
        return this;
    }

    public AIRequestBuilder previousResponse(AIResponse response) {
        this.previousAssistantResponse = response;
        return this;
    }

    public AIRequestBuilder addToolResult(AIToolResult result) {
        this.toolResults.add(result);
        return this;
    }

    public AIRequestBuilder addToolResults(List<AIToolResult> results) {
        this.toolResults.addAll(results);
        return this;
    }

    public AIRequestBuilder maxToolRounds(int maxToolRounds) {
        this.maxToolRounds = maxToolRounds;
        return this;
    }

    public AIRequestBuilder overridePipelineConfig(boolean override) {
        this.properties.put(AIConfigProperty.OVERRIDE_PIPELINE_CONFIG, override);
        return this;
    }

    public AIRequestBuilder instructions(String instructions) {
        this.properties.put(AIConfigProperty.INSTRUCTIONS, instructions);
        return this;
    }

    public AIRequestBuilder prettyPrint(boolean prettyPrint) {
        this.properties.put(AIConfigProperty.PRETTY_PRINT_JSON, prettyPrint);
        return this;
    }

    public AIRequestBuilder logRequests(boolean logRequests) {
        this.properties.put(AIConfigProperty.LOG_REQUESTS, logRequests);
        return this;
    }

    public AIRequestBuilder logResponses(boolean logResponses) {
        this.properties.put(AIConfigProperty.LOG_RESPONSES, logResponses);
        return this;
    }

    public AIRequestBuilder temperature(double temperature) {
        this.properties.put(AIConfigProperty.TEMPERATURE, temperature);
        return this;
    }

    public AIRequestBuilder topP(double topP) {
        this.properties.put(AIConfigProperty.TOP_P, topP);
        return this;
    }

    public AIRequestBuilder topK(int topK) {
        this.properties.put(AIConfigProperty.TOP_K, topK);
        return this;
    }

    public AIRequestBuilder maxOutputTokens(int maxOutputTokens) {
        this.properties.put(AIConfigProperty.MAX_OUTPUT_TOKENS, maxOutputTokens);
        return this;
    }

    public AIRequestBuilder truncateLongRequestInputs(boolean truncate) {
        this.properties.put(AIConfigProperty.LOG_TRUNCATE_LARGE_INPUT, truncate);
        return this;
    }

    public AIRequestBuilder retryPolicy(RetryPolicy retryPolicy) {
        this.properties.put(AIConfigProperty.RETRY_POLICY, retryPolicy);
        return this;
    }

    public AIRequest build() {
        return new ProtifyAIRequest(
                client,
                inputs,
                properties,
                tools,
                toolHandlers,
                toolResults,
                previousAssistantResponse,
                maxToolRounds
        );
    }
}
