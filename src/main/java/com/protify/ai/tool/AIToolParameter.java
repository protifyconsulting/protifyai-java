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

package com.protify.ai.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AIToolParameter {

    private final String type;
    private final String description;
    private final List<String> enumValues;
    private final Map<String, AIToolParameter> properties;
    private final AIToolParameter items;

    private AIToolParameter(String type, String description, List<String> enumValues,
                            Map<String, AIToolParameter> properties, AIToolParameter items) {
        this.type = type;
        this.description = description;
        this.enumValues = enumValues;
        this.properties = properties;
        this.items = items;
    }

    public static AIToolParameter string(String description) {
        return new AIToolParameter("string", description, null, null, null);
    }

    public static AIToolParameter number(String description) {
        return new AIToolParameter("number", description, null, null, null);
    }

    public static AIToolParameter integer(String description) {
        return new AIToolParameter("integer", description, null, null, null);
    }

    public static AIToolParameter bool(String description) {
        return new AIToolParameter("boolean", description, null, null, null);
    }

    public static AIToolParameter stringEnum(String description, List<String> values) {
        return new AIToolParameter("string", description, Collections.unmodifiableList(values), null, null);
    }

    public static AIToolParameter object(String description, Map<String, AIToolParameter> properties) {
        return new AIToolParameter("object", description, null, Collections.unmodifiableMap(properties), null);
    }

    public static AIToolParameter array(String description, AIToolParameter items) {
        return new AIToolParameter("array", description, null, null, items);
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }

    public Map<String, AIToolParameter> getProperties() {
        return properties;
    }

    public AIToolParameter getItems() {
        return items;
    }

    public Map<String, Object> toSchemaMap() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", type);
        if (description != null) {
            schema.put("description", description);
        }
        if (enumValues != null && !enumValues.isEmpty()) {
            schema.put("enum", enumValues);
        }
        if (properties != null && !properties.isEmpty()) {
            Map<String, Object> propsMap = new LinkedHashMap<>();
            for (Map.Entry<String, AIToolParameter> entry : properties.entrySet()) {
                propsMap.put(entry.getKey(), entry.getValue().toSchemaMap());
            }
            schema.put("properties", propsMap);
        }
        if (items != null) {
            schema.put("items", items.toSchemaMap());
        }
        return schema;
    }
}
