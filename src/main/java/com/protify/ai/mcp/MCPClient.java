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

package com.protify.ai.mcp;

import com.protify.ai.internal.mcp.HttpSseMCPTransport;
import com.protify.ai.internal.mcp.ProtifyMCPClient;
import com.protify.ai.internal.mcp.StdioMCPTransport;
import com.protify.ai.tool.AITool;

import java.util.List;
import java.util.Map;

public interface MCPClient extends AutoCloseable {

    void connect();

    List<AITool> listTools();

    String callTool(String name, Map<String, Object> arguments);

    boolean isConnected();

    void close();

    static MCPClient stdio(String... command) {
        return new ProtifyMCPClient(new StdioMCPTransport(command));
    }

    static MCPClient http(String url) {
        return new ProtifyMCPClient(new HttpSseMCPTransport(url));
    }
}
