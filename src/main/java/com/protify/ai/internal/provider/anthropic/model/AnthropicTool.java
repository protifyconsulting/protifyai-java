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
import com.protify.ai.tool.AITool;
import com.protify.ai.tool.AIToolParameter;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AnthropicTool {

    private String name;
    private String description;

    @ProtifyJsonProperty("input_schema")
    private Map<String, Object> inputSchema;

    private AnthropicTool() {
    }

    public static AnthropicTool from(AITool tool) {
        AnthropicTool at = new AnthropicTool();
        at.name = tool.getName();
        at.description = tool.getDescription();
        at.inputSchema = buildInputSchema(tool);
        return at;
    }

    private static Map<String, Object> buildInputSchema(AITool tool) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        for (Map.Entry<String, AIToolParameter> entry : tool.getParameters().entrySet()) {
            properties.put(entry.getKey(), entry.getValue().toSchemaMap());
        }
        schema.put("properties", properties);

        if (!tool.getRequiredParameters().isEmpty()) {
            schema.put("required", tool.getRequiredParameters());
        }

        return schema;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @ProtifyJsonProperty("input_schema")
    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }
}
