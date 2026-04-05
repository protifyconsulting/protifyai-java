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

package ai.protify.core.provider.mock;

import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.tool.AIToolCall;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class MockToolCall implements AIToolCall {

    private final String id;
    private final String name;
    private final Map<String, Object> arguments;

    public MockToolCall(String name, Map<String, Object> arguments) {
        this(UUID.randomUUID().toString(), name, arguments);
    }

    public MockToolCall(String id, String name, Map<String, Object> arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments != null ? arguments : Collections.emptyMap();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Object> getArguments() {
        return arguments;
    }

    @Override
    public String getArgumentsJson() {
        return ProtifyJson.toJson(arguments);
    }
}
