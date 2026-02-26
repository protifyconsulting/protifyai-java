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

package ai.protify.core.internal.mcp;

import ai.protify.core.internal.tool.ProtifyAITool;
import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.internal.util.json.ProtifyJsonObject;
import ai.protify.core.mcp.MCPClient;
import ai.protify.core.tool.AITool;
import ai.protify.core.tool.AIToolParameter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ProtifyMCPClient implements MCPClient {

    private final MCPTransport transport;
    private final AtomicInteger requestId = new AtomicInteger(1);
    private boolean connected = false;
    private List<AITool> cachedTools;

    public ProtifyMCPClient(MCPTransport transport) {
        this.transport = transport;
    }

    @Override
    public void connect() {
        transport.open();

        // Send initialize request
        Map<String, Object> initParams = new LinkedHashMap<>();
        initParams.put("protocolVersion", "2024-11-05");

        Map<String, Object> clientInfo = new LinkedHashMap<>();
        clientInfo.put("name", "protify-ai-sdk");
        clientInfo.put("version", "1.0.0");
        initParams.put("clientInfo", clientInfo);

        Map<String, Object> capabilities = new LinkedHashMap<>();
        initParams.put("capabilities", capabilities);

        String response = sendJsonRpc("initialize", initParams);
        validateResponse(response);

        // Send initialized notification (no id, no response expected)
        sendNotification("notifications/initialized", Collections.emptyMap());

        this.connected = true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AITool> listTools() {
        if (cachedTools != null) {
            return cachedTools;
        }

        String response = sendJsonRpc("tools/list", Collections.emptyMap());
        ProtifyJsonObject json = ProtifyJson.parse(response);

        Object error = json.get("error");
        if (error != null) {
            throw new IllegalStateException("MCP tools/list error: " + error);
        }

        Object toolsObj = json.get("result.tools");
        if (!(toolsObj instanceof List)) {
            return Collections.emptyList();
        }

        List<AITool> tools = new ArrayList<>();
        for (Object item : (List<?>) toolsObj) {
            if (item instanceof Map) {
                Map<String, Object> toolMap = (Map<String, Object>) item;
                tools.add(mapToAITool(toolMap));
            }
        }

        this.cachedTools = Collections.unmodifiableList(tools);
        return cachedTools;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String callTool(String name, Map<String, Object> arguments) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", name);
        params.put("arguments", arguments);

        String response = sendJsonRpc("tools/call", params);
        ProtifyJsonObject json = ProtifyJson.parse(response);

        Object error = json.get("error");
        if (error instanceof Map) {
            Map<String, Object> errorMap = (Map<String, Object>) error;
            Object code = errorMap.get("code");
            Object message = errorMap.get("message");
            throw new IllegalStateException("MCP error " + code + ": " + message);
        }

        // Extract result.content.0.text
        String text = json.getString("result.content.0.text");
        return text != null ? text : "";
    }

    @Override
    public boolean isConnected() {
        return connected && transport.isOpen();
    }

    @Override
    public void close() {
        this.connected = false;
        this.cachedTools = null;
        transport.close();
    }

    private String sendJsonRpc(String method, Map<String, Object> params) {
        JsonRpcRequest request = new JsonRpcRequest(requestId.getAndIncrement(), method, params);
        String jsonStr = ProtifyJson.toJson(request);
        return transport.sendRequest(jsonStr);
    }

    private void sendNotification(String method, Map<String, Object> params) {
        // Notifications have no id, but we'll use a simple JSON construction
        StringBuilder sb = new StringBuilder();
        sb.append("{\"jsonrpc\":\"2.0\",\"method\":\"").append(ProtifyJson.escapeJson(method)).append("\"");
        if (params != null && !params.isEmpty()) {
            sb.append(",\"params\":").append(formatMap(params));
        }
        sb.append("}");
        transport.sendRequest(sb.toString());
    }

    private void validateResponse(String response) {
        ProtifyJsonObject json = ProtifyJson.parse(response);
        Object error = json.get("error");
        if (error != null) {
            throw new IllegalStateException("MCP initialization error: " + error);
        }
    }

    @SuppressWarnings("unchecked")
    private AITool mapToAITool(Map<String, Object> toolMap) {
        String name = toolMap.get("name") != null ? toolMap.get("name").toString() : "";
        String description = toolMap.get("description") != null ? toolMap.get("description").toString() : "";

        Map<String, AIToolParameter> parameters = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        Object inputSchema = toolMap.get("inputSchema");
        if (inputSchema instanceof Map) {
            Map<String, Object> schema = (Map<String, Object>) inputSchema;

            Object props = schema.get("properties");
            if (props instanceof Map) {
                Map<String, Object> propsMap = (Map<String, Object>) props;
                for (Map.Entry<String, Object> entry : propsMap.entrySet()) {
                    if (entry.getValue() instanceof Map) {
                        parameters.put(entry.getKey(), mapToAIToolParameter((Map<String, Object>) entry.getValue()));
                    }
                }
            }

            Object req = schema.get("required");
            if (req instanceof List) {
                for (Object item : (List<?>) req) {
                    required.add(item.toString());
                }
            }
        }

        return new ProtifyAITool(name, description, parameters, required);
    }

    @SuppressWarnings("unchecked")
    private AIToolParameter mapToAIToolParameter(Map<String, Object> paramMap) {
        String type = paramMap.get("type") != null ? paramMap.get("type").toString() : "string";
        String desc = paramMap.get("description") != null ? paramMap.get("description").toString() : "";

        Object enumValues = paramMap.get("enum");
        if (enumValues instanceof List && "string".equals(type)) {
            List<String> values = new ArrayList<>();
            for (Object v : (List<?>) enumValues) {
                values.add(v.toString());
            }
            return AIToolParameter.stringEnum(desc, values);
        }

        switch (type) {
            case "number":
                return AIToolParameter.number(desc);
            case "integer":
                return AIToolParameter.integer(desc);
            case "boolean":
                return AIToolParameter.bool(desc);
            case "array":
                AIToolParameter items = null;
                Object itemsObj = paramMap.get("items");
                if (itemsObj instanceof Map) {
                    items = mapToAIToolParameter((Map<String, Object>) itemsObj);
                }
                return AIToolParameter.array(desc, items);
            case "object":
                Map<String, AIToolParameter> subProps = new LinkedHashMap<>();
                Object props = paramMap.get("properties");
                if (props instanceof Map) {
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) props).entrySet()) {
                        if (entry.getValue() instanceof Map) {
                            subProps.put(entry.getKey(), mapToAIToolParameter((Map<String, Object>) entry.getValue()));
                        }
                    }
                }
                return AIToolParameter.object(desc, subProps);
            default:
                return AIToolParameter.string(desc);
        }
    }

    private static String formatMap(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(ProtifyJson.escapeJson(entry.getKey())).append("\":");
            sb.append(formatJsonValue(entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String formatJsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "\"" + ProtifyJson.escapeJson((String) value) + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map) return formatMap((Map<String, Object>) value);
        if (value instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : (List<?>) value) {
                if (!first) sb.append(",");
                sb.append(formatJsonValue(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + ProtifyJson.escapeJson(value.toString()) + "\"";
    }
}
