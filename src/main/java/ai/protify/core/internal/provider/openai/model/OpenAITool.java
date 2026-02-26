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

package ai.protify.core.internal.provider.openai.model;

import ai.protify.core.tool.AITool;
import ai.protify.core.tool.AIToolParameter;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OpenAITool {

    private String type;
    private String name;
    private String description;
    private Map<String, Object> parameters;

    private OpenAITool() {
    }

    public static OpenAITool from(AITool tool) {
        OpenAITool ot = new OpenAITool();
        ot.type = "function";
        ot.name = tool.getName();
        ot.description = tool.getDescription();
        ot.parameters = buildParameters(tool);
        return ot;
    }

    private static Map<String, Object> buildParameters(AITool tool) {
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

        schema.put("additionalProperties", false);

        return schema;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }
}
