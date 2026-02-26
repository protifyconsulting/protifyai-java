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

package com.protify.ai.internal.message;

import com.protify.ai.message.AIMessage;
import com.protify.ai.request.AIInput;
import com.protify.ai.response.AIResponse;
import com.protify.ai.tool.AIToolCall;
import com.protify.ai.tool.AIToolResult;

import java.util.*;

public final class ProtifyAIMessage implements AIMessage {

    private final String role;
    private final String text;
    private final List<AIInput> inputs;
    private final List<AIToolCall> toolCalls;
    private final List<AIToolResult> toolResults;

    private ProtifyAIMessage(String role,
                             String text,
                             List<AIInput> inputs,
                             List<AIToolCall> toolCalls,
                             List<AIToolResult> toolResults) {
        this.role = role;
        this.text = text;
        this.inputs = inputs != null ? Collections.unmodifiableList(inputs) : Collections.emptyList();
        this.toolCalls = toolCalls != null ? Collections.unmodifiableList(toolCalls) : Collections.emptyList();
        this.toolResults = toolResults != null ? Collections.unmodifiableList(toolResults) : Collections.emptyList();
    }

    public static ProtifyAIMessage userText(String text) {
        return new ProtifyAIMessage("user", text, null, null, null);
    }

    public static ProtifyAIMessage userWithInputs(String text, List<AIInput> inputs) {
        return new ProtifyAIMessage("user", text, inputs, null, null);
    }

    public static ProtifyAIMessage userWithToolResults(List<AIToolResult> toolResults) {
        return new ProtifyAIMessage("user", null, null, null, toolResults);
    }

    public static ProtifyAIMessage assistantText(String text) {
        return new ProtifyAIMessage("assistant", text, null, null, null);
    }

    public static ProtifyAIMessage fromResponse(AIResponse response) {
        List<AIToolCall> toolCalls = response.hasToolCalls()
                ? new ArrayList<>(response.getToolCalls())
                : null;
        return new ProtifyAIMessage("assistant", response.text(), null, toolCalls, null);
    }

    @Override
    public String getRole() {
        return role;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public List<AIInput> getInputs() {
        return inputs;
    }

    @Override
    public List<AIToolCall> getToolCalls() {
        return toolCalls;
    }

    @Override
    public List<AIToolResult> getToolResults() {
        return toolResults;
    }

    @Override
    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    public Map<String, Object> toSerializableMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("role", role);
        if (text != null) {
            map.put("text", text);
        }
        if (!toolCalls.isEmpty()) {
            List<Map<String, Object>> calls = new ArrayList<>();
            for (AIToolCall call : toolCalls) {
                Map<String, Object> callMap = new LinkedHashMap<>();
                callMap.put("id", call.getId());
                callMap.put("name", call.getName());
                callMap.put("arguments", call.getArgumentsJson());
                calls.add(callMap);
            }
            map.put("toolCalls", calls);
        }
        if (!toolResults.isEmpty()) {
            List<Map<String, Object>> results = new ArrayList<>();
            for (AIToolResult result : toolResults) {
                Map<String, Object> resultMap = new LinkedHashMap<>();
                resultMap.put("toolCallId", result.getToolCallId());
                resultMap.put("content", result.getContent());
                if (result.isError()) {
                    resultMap.put("error", true);
                }
                results.add(resultMap);
            }
            map.put("toolResults", results);
        }
        // File inputs are intentionally excluded from serialization
        return map;
    }

    @SuppressWarnings("unchecked")
    public static ProtifyAIMessage fromSerializableMap(Map<String, Object> map) {
        String role = (String) map.get("role");
        String text = (String) map.get("text");

        List<AIToolCall> toolCalls = null;
        if (map.containsKey("toolCalls")) {
            toolCalls = new ArrayList<>();
            List<Map<String, Object>> callsList = (List<Map<String, Object>>) map.get("toolCalls");
            for (Map<String, Object> callMap : callsList) {
                toolCalls.add(new SerializedToolCall(
                        (String) callMap.get("id"),
                        (String) callMap.get("name"),
                        (String) callMap.get("arguments")
                ));
            }
        }

        List<AIToolResult> toolResults = null;
        if (map.containsKey("toolResults")) {
            toolResults = new ArrayList<>();
            List<Map<String, Object>> resultsList = (List<Map<String, Object>>) map.get("toolResults");
            for (Map<String, Object> resultMap : resultsList) {
                boolean error = resultMap.containsKey("error") && Boolean.TRUE.equals(resultMap.get("error"));
                toolResults.add(new AIToolResult(
                        (String) resultMap.get("toolCallId"),
                        (String) resultMap.get("content"),
                        error
                ));
            }
        }

        return new ProtifyAIMessage(role, text, null, toolCalls, toolResults);
    }

    private static final class SerializedToolCall implements AIToolCall {
        private final String id;
        private final String name;
        private final String argumentsJson;

        SerializedToolCall(String id, String name, String argumentsJson) {
            this.id = id;
            this.name = name;
            this.argumentsJson = argumentsJson;
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
            if (argumentsJson == null) return Collections.emptyMap();
            Object parsed = com.protify.ai.internal.util.json.ProtifyJson.parse(argumentsJson).get("");
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) parsed;
                return result;
            }
            return Collections.emptyMap();
        }

        @Override
        public String getArgumentsJson() {
            return argumentsJson;
        }
    }
}
