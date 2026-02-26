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

package com.protify.ai.internal.util.json;

import java.util.List;
import java.util.Map;

public class JsonBuilder  {

    private final StringBuilder builder;
    private final boolean prettyPrint;
    private final boolean truncate;

    public JsonBuilder(boolean truncate, boolean prettyPrint) {
        this.builder = new StringBuilder();
        this.truncate = truncate;
        this.prettyPrint = prettyPrint;
    }

    public JsonBuilder append(String value) {
        builder.append(value);
        return this;
    }

    public JsonBuilder appendLarge(String data) {
        if (truncate && data != null && data.length() > 50) {
            builder.append(data, 0, 50).append("... [truncated]");
        } else {
            builder.append(data);
        }
        return this;
    }

    public JsonBuilder appendNewLine() {
        if (this.prettyPrint) builder.append("\n");
        return this;
    }

    public JsonBuilder appendIndent(int level) {
        if (this.prettyPrint) {
            builder.append("  ".repeat(Math.max(0, level)));
        }
        return this;
    }

    public JsonBuilder appendProperties(Map<String, Object> properties, int level) {
        if (properties == null || properties.isEmpty()) return this;

        for (String key : properties.keySet()) {
            appendProperty(key, properties.get(key), level);
        }
        return this;
    }

    public JsonBuilder appendArray(String propertyName, List<Map<String, Object>> arrayContent, int level) {
        if (arrayContent == null || arrayContent.isEmpty()) return this;
        this.append("\"").append(propertyName).append("\":[");
        for (Map<String, Object> map : arrayContent) {
            this.appendNewLine().appendIndent(level).append("{");
            appendProperties(map, level);
            this.appendNewLine().appendIndent(level).append("},");
        }
        this.deleteLastChar();
        this.appendNewLine().appendIndent(level).append("]");
        return this;
    }

    public JsonBuilder appendProperty(String property, Object value, int level) {
        if (value == null) return this;

        this.appendIndent(level).append("\"").append(property).append("\":");
        if (value instanceof String) {
            this.append("\"").appendLarge(value.toString()).append("\"");
        } else {
            this.append(value.toString());
        }
        return this.append(",").appendNewLine();
    }

    public JsonBuilder deleteLastChar() {
        if (this.prettyPrint) {
            builder.deleteCharAt(builder.length() - 2);
        } else {
            builder.deleteCharAt(builder.length() - 1);
        }
        return this;
    }

    @Override
    public String toString() { return builder.toString(); }

}
