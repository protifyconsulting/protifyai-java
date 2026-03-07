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

package ai.protify.core;

import ai.protify.core.mcp.MCPClient;
import ai.protify.core.request.AIRequestBuilder;
import ai.protify.core.response.AIResponse;
import ai.protify.core.tool.AITool;
import ai.protify.core.tool.AIToolParameter;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP integration tests using the Anthropic Everything Server
 * ({@code @modelcontextprotocol/server-everything}).
 *
 * <p>Tier 1 tests (no tag) exercise the MCP client against deterministic
 * server tools — no API keys or LLM calls required.</p>
 *
 * <p>Tier 2 tests (tagged "smoke") pair MCP tools with a live LLM to
 * verify the full tool-use loop.</p>
 *
 * <p>Requires {@code npx} on the PATH.</p>
 */
@Tag("smoke")
public class MCPIntegrationTest {

    private static MCPClient mcp;

    @BeforeAll
    static void startServer() {
        ProtifyAI.initialize();
        mcp = MCPClient.stdio("npx", "-y", "@modelcontextprotocol/server-everything");
        mcp.connect();
    }

    @AfterAll
    static void stopServer() {
        if (mcp != null) {
            mcp.close();
        }
    }

    // ---------------------------------------------------------------
    // Tier 1: Pure MCP (deterministic, no LLM)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Tier 1: MCP Client")
    class MCPClientTests {

        @Test
        @DisplayName("Server is connected after connect()")
        void serverIsConnected() {
            assertTrue(mcp.isConnected());
        }

        @Test
        @DisplayName("listTools returns tools from the everything server")
        void listToolsReturnsTools() {
            List<AITool> tools = mcp.listTools();

            assertNotNull(tools);
            assertFalse(tools.isEmpty(), "Expected at least one tool from everything server");

            // Verify well-known tools exist
            assertTrue(tools.stream().anyMatch(t -> "echo".equals(t.getName())),
                    "Expected 'echo' tool");
            assertTrue(tools.stream().anyMatch(t -> "get-sum".equals(t.getName())),
                    "Expected 'get-sum' tool");
        }

        @Test
        @DisplayName("listTools returns correct schema for echo tool")
        void echoToolSchema() {
            AITool echoTool = mcp.listTools().stream()
                    .filter(t -> "echo".equals(t.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("echo tool not found"));

            assertNotNull(echoTool.getDescription());
            assertFalse(echoTool.getDescription().isEmpty());

            Map<String, AIToolParameter> params = echoTool.getParameters();
            assertNotNull(params);
            assertTrue(params.containsKey("message"), "echo tool should have 'message' parameter");

            List<String> required = echoTool.getRequiredParameters();
            assertNotNull(required);
            assertTrue(required.contains("message"), "message should be required");
        }

        @Test
        @DisplayName("listTools returns correct schema for get-sum tool")
        void getSumToolSchema() {
            AITool sumTool = mcp.listTools().stream()
                    .filter(t -> "get-sum".equals(t.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("get-sum tool not found"));

            Map<String, AIToolParameter> params = sumTool.getParameters();
            assertNotNull(params);
            assertTrue(params.containsKey("a"), "get-sum should have 'a' parameter");
            assertTrue(params.containsKey("b"), "get-sum should have 'b' parameter");
        }

        @Test
        @DisplayName("callTool — echo returns the input message")
        void callEcho() {
            String result = mcp.callTool("echo", Map.of("message", "hello world"));

            assertNotNull(result);
            assertTrue(result.contains("hello world"),
                    "Expected echo response to contain 'hello world', got: " + result);
        }

        @Test
        @DisplayName("callTool — echo with special characters")
        void callEchoSpecialChars() {
            String input = "quotes \"here\" and backslash \\ and newline";
            String result = mcp.callTool("echo", Map.of("message", input));

            assertNotNull(result);
            assertTrue(result.contains("quotes \"here\""),
                    "Expected echo to preserve special characters, got: " + result);
        }

        @Test
        @DisplayName("callTool — get-sum adds two numbers")
        void callGetSum() {
            String result = mcp.callTool("get-sum", Map.of("a", 7, "b", 8));

            assertNotNull(result);
            assertTrue(result.contains("15"),
                    "Expected sum result to contain '15', got: " + result);
        }

        @Test
        @DisplayName("callTool — get-sum with negative numbers")
        void callGetSumNegative() {
            String result = mcp.callTool("get-sum", Map.of("a", -10, "b", 3));

            assertNotNull(result);
            assertTrue(result.contains("-7"),
                    "Expected sum result to contain '-7', got: " + result);
        }

        @Test
        @DisplayName("callTool — get-sum with zero")
        void callGetSumZero() {
            String result = mcp.callTool("get-sum", Map.of("a", 0, "b", 0));

            assertNotNull(result);
            assertTrue(result.contains("0"),
                    "Expected sum result to contain '0', got: " + result);
        }

        @Test
        @DisplayName("listTools is idempotent (cached)")
        void listToolsIdempotent() {
            List<AITool> first = mcp.listTools();
            List<AITool> second = mcp.listTools();

            assertSame(first, second, "listTools should return cached list on second call");
        }
    }

    // ---------------------------------------------------------------
    // Tier 2: MCP + LLM (requires API key)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Tier 2: MCP + LLM")
    class MCPWithLLMTests {

        @Test
        @DisplayName("LLM uses echo tool via automatic tool loop")
        void llmUsesEchoTool() {
            AIClient client = AIClient.builder()
                    .model(AIModel.CLAUDE_HAIKU_4_5)
                    .build();

            AIRequestBuilder builder = client.newRequest()
                    .addInput("Use the echo tool to echo the message 'test123'. Return ONLY the tool's response, nothing else.")
                    .maxOutputTokens(4096);

            for (AITool tool : mcp.listTools()) {
                builder.addTool(tool, args -> mcp.callTool(tool.getName(), args));
            }

            AIResponse response = builder.maxToolRounds(5).build().execute();

            assertNotNull(response.text());
            assertTrue(response.text().contains("test123"),
                    "Expected LLM response to contain echo result 'test123', got: " + response.text());
        }

        @Test
        @DisplayName("LLM uses get-sum tool via automatic tool loop")
        void llmUsesGetSumTool() {
            AIClient client = AIClient.builder()
                    .model(AIModel.CLAUDE_HAIKU_4_5)
                    .build();

            AIRequestBuilder builder = client.newRequest()
                    .addInput("What is 42 + 58? Use the get-sum tool to calculate this. Return ONLY the number from the tool's response.")
                    .maxOutputTokens(4096);

            for (AITool tool : mcp.listTools()) {
                builder.addTool(tool, args -> mcp.callTool(tool.getName(), args));
            }

            AIResponse response = builder.maxToolRounds(5).build().execute();

            assertNotNull(response.text());
            assertTrue(response.text().contains("100"),
                    "Expected LLM response to contain '100', got: " + response.text());
        }

        @Test
        @DisplayName("LLM uses multiple MCP tools in sequence")
        void llmUsesMultipleTools() {
            AIClient client = AIClient.builder()
                    .model(AIModel.CLAUDE_HAIKU_4_5)
                    .build();

            AIRequestBuilder builder = client.newRequest()
                    .addInput("First, use the get-sum tool to add 10 and 20. "
                            + "Then use the echo tool to echo the result. "
                            + "Return only the final echo output.")
                    .maxOutputTokens(4096);

            for (AITool tool : mcp.listTools()) {
                builder.addTool(tool, args -> mcp.callTool(tool.getName(), args));
            }

            AIResponse response = builder.maxToolRounds(10).build().execute();

            assertNotNull(response.text());
            assertTrue(response.text().contains("30"),
                    "Expected response to contain '30', got: " + response.text());
        }

        @Test
        @DisplayName("MCP tools work with manual tool loop")
        void mcpToolsManualLoop() {
            AIClient client = AIClient.builder()
                    .model(AIModel.CLAUDE_HAIKU_4_5)
                    .build();

            List<AITool> tools = mcp.listTools();
            AITool sumTool = tools.stream()
                    .filter(t -> "get-sum".equals(t.getName()))
                    .findFirst()
                    .orElseThrow();

            AIResponse response = client.newRequest()
                    .addInput("What is 5 + 7? Use the get-sum tool.")
                    .addTool(sumTool)
                    .maxOutputTokens(4096)
                    .build().execute();

            assertTrue(response.hasToolCalls(), "Expected tool call");

            var toolCall = response.getToolCalls().get(0);
            assertEquals("get-sum", toolCall.getName());

            String toolResult = mcp.callTool(toolCall.getName(), toolCall.getArguments());
            assertTrue(toolResult.contains("12"),
                    "Expected MCP tool result to contain '12', got: " + toolResult);

            AIResponse finalResponse = client.newRequest()
                    .addInput("What is 5 + 7? Use the get-sum tool.")
                    .addTool(sumTool)
                    .previousResponse(response)
                    .addToolResult(new ai.protify.core.tool.AIToolResult(toolCall.getId(), toolResult))
                    .maxOutputTokens(4096)
                    .build().execute();

            assertNotNull(finalResponse.text());
            assertTrue(finalResponse.text().contains("12"),
                    "Expected final response to contain '12', got: " + finalResponse.text());
        }
    }
}
