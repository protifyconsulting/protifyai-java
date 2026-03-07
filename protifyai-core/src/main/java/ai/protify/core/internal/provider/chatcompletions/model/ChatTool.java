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

package ai.protify.core.internal.provider.chatcompletions.model;

import ai.protify.core.tool.AITool;
import ai.protify.core.tool.AIToolParameter;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ChatTool {

    private String type;
    private ChatFunctionDef function;

    private ChatTool() {
    }

    public static ChatTool from(AITool tool) {
        ChatTool t = new ChatTool();
        t.type = "function";
        t.function = ChatFunctionDef.from(tool);
        return t;
    }

    public String getType() {
        return type;
    }

    public ChatFunctionDef getFunction() {
        return function;
    }

    public static final class ChatFunctionDef {

        private String name;
        private String description;
        private Map<String, Object> parameters;

        private ChatFunctionDef() {
        }

        static ChatFunctionDef from(AITool tool) {
            ChatFunctionDef def = new ChatFunctionDef();
            def.name = tool.getName();
            def.description = tool.getDescription();
            def.parameters = buildParameters(tool);
            return def;
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
