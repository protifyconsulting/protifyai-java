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

package com.protify.ai.internal.provider.anthropic.model;

import com.protify.ai.internal.util.json.ProtifyJsonProperty;

import java.util.List;

public final class AnthropicRequestBody {

    private String model;

    @ProtifyJsonProperty("max_tokens")
    private Integer maxTokens;

    private Double temperature;

    @ProtifyJsonProperty("top_p")
    private Double topP;

    @ProtifyJsonProperty("top_k")
    private Integer topK;

    private boolean stream;
    private String system;
    private List<AnthropicMessage> messages;
    private List<AnthropicTool> tools;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @ProtifyJsonProperty("max_tokens")
    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    @ProtifyJsonProperty("top_p")
    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    @ProtifyJsonProperty("top_k")
    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public List<AnthropicMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<AnthropicMessage> messages) {
        this.messages = messages;
    }

    public List<AnthropicTool> getTools() {
        return tools;
    }

    public void setTools(List<AnthropicTool> tools) {
        this.tools = tools;
    }
}
