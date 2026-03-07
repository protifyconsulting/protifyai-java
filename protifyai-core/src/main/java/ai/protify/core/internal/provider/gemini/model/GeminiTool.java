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

package ai.protify.core.internal.provider.gemini.model;

import ai.protify.core.internal.util.json.ProtifyJsonProperty;
import ai.protify.core.tool.AITool;
import ai.protify.core.tool.AIToolParameter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class GeminiTool {

    @ProtifyJsonProperty("functionDeclarations")
    private List<GeminiFunctionDeclaration> functionDeclarations;

    private GeminiTool() {
    }

    public static GeminiTool from(List<AITool> tools) {
        GeminiTool gt = new GeminiTool();
        gt.functionDeclarations = tools.stream()
                .map(GeminiFunctionDeclaration::from)
                .collect(Collectors.toList());
        return gt;
    }

    @ProtifyJsonProperty("functionDeclarations")
    public List<GeminiFunctionDeclaration> getFunctionDeclarations() {
        return functionDeclarations;
    }

    public static final class GeminiFunctionDeclaration {

        private String name;
        private String description;
        private Map<String, Object> parameters;

        private GeminiFunctionDeclaration() {
        }

        static GeminiFunctionDeclaration from(AITool tool) {
            GeminiFunctionDeclaration decl = new GeminiFunctionDeclaration();
            decl.name = tool.getName();
            decl.description = tool.getDescription();
            decl.parameters = buildParameters(tool);
            return decl;
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

            return schema;
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
}
