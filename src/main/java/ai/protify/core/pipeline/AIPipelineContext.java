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
import ai.protify.core.response.AIResponse;

import java.util.Map;
import java.util.Set;

public interface AIPipelineContext {

    AIResponse getPreviousStepResponse();

    void setPreviousStepResponse(AIResponse previousStepResponse);

    Map<AIConfigProperty, Object> getPipelineProperties();

    void addCustomProperty(String key, Object value);

    Object getCustomProperty(String key);

    void removeCustomProperty(String key);

    boolean hasCustomProperty(String key);

    void clearCustomProperties();

    Set<String> getCustomPropertyKeys();

    Map<String, Object> getCustomContext();

    AIResponse response();

    String text();

}
