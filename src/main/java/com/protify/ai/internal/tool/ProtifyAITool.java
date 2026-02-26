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

package com.protify.ai.internal.tool;

import com.protify.ai.tool.AITool;
import com.protify.ai.tool.AIToolParameter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ProtifyAITool implements AITool {

    private final String name;
    private final String description;
    private final Map<String, AIToolParameter> parameters;
    private final List<String> requiredParameters;

    public ProtifyAITool(String name, String description,
                         Map<String, AIToolParameter> parameters,
                         List<String> requiredParameters) {
        this.name = name;
        this.description = description;
        this.parameters = Collections.unmodifiableMap(parameters);
        this.requiredParameters = Collections.unmodifiableList(requiredParameters);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Map<String, AIToolParameter> getParameters() {
        return parameters;
    }

    @Override
    public List<String> getRequiredParameters() {
        return requiredParameters;
    }
}
