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

package ai.protify.core.internal.pipeline;

import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.pipeline.AIPipelineContext;
import ai.protify.core.response.AIResponse;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProtifyAIPipelineContext implements AIPipelineContext {

    private AIResponse previousStepResponse;
    private final Map<AIConfigProperty, Object> pipelineProperties;
    private Map<String, Object> customContext = new ConcurrentHashMap<>();

    public ProtifyAIPipelineContext(Map<AIConfigProperty, Object> pipelineProperties) {
        this.pipelineProperties = pipelineProperties;
    }

    public AIResponse getPreviousStepResponse() {
        return previousStepResponse;
    }

    public void setPreviousStepResponse(AIResponse previousStepResponse) {
        this.previousStepResponse = previousStepResponse;
    }

    public Map<AIConfigProperty, Object> getPipelineProperties() {
        return pipelineProperties;
    }

    public void addCustomProperty(String key, Object value) {
        customContext.put(key, value);
    }

    public Object getCustomProperty(String key) {
        return customContext.get(key);
    }

    public void removeCustomProperty(String key) {
        customContext.remove(key);
    }

    public boolean hasCustomProperty(String key) {
        return customContext.containsKey(key);
    }

    public void clearCustomProperties() {
        customContext.clear();
    }

    public Set<String> getCustomPropertyKeys() {
        return customContext.keySet();
    }

    public Map<String, Object> getCustomContext() {
        return customContext;
    }

    public AIResponse response() {
        return getPreviousStepResponse();
    }

    public String text() {
        return getPreviousStepResponse().text();
    }
}
