package ai.protify.core.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AIToolTest {

    // ---------------------------------------------------------------
    // 1. AITool builder
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("AITool builder")
    class ToolBuilder {

        @Test
        @DisplayName("Builds tool with name and description")
        void basicTool() {
            AITool tool = AITool.builder("get_weather")
                    .description("Get the current weather")
                    .build();

            assertEquals("get_weather", tool.getName());
            assertEquals("Get the current weather", tool.getDescription());
            assertTrue(tool.getParameters().isEmpty());
            assertTrue(tool.getRequiredParameters().isEmpty());
        }

        @Test
        @DisplayName("Builds tool with optional parameters")
        void optionalParameters() {
            AITool tool = AITool.builder("search")
                    .description("Search for items")
                    .addParameter("query", AIToolParameter.string("The search query"))
                    .addParameter("limit", AIToolParameter.integer("Max results"))
                    .build();

            assertEquals(2, tool.getParameters().size());
            assertNotNull(tool.getParameters().get("query"));
            assertNotNull(tool.getParameters().get("limit"));
            assertTrue(tool.getRequiredParameters().isEmpty());
        }

        @Test
        @DisplayName("Builds tool with required parameters")
        void requiredParameters() {
            AITool tool = AITool.builder("search")
                    .description("Search")
                    .addRequiredParameter("query", AIToolParameter.string("The query"))
                    .addParameter("limit", AIToolParameter.integer("Max results"))
                    .build();

            assertEquals(2, tool.getParameters().size());
            assertEquals(1, tool.getRequiredParameters().size());
            assertEquals("query", tool.getRequiredParameters().get(0));
        }

        @Test
        @DisplayName("Builds tool with multiple required parameters")
        void multipleRequiredParameters() {
            AITool tool = AITool.builder("create_event")
                    .description("Create a calendar event")
                    .addRequiredParameter("title", AIToolParameter.string("Event title"))
                    .addRequiredParameter("date", AIToolParameter.string("Event date"))
                    .addParameter("description", AIToolParameter.string("Event description"))
                    .build();

            assertEquals(3, tool.getParameters().size());
            assertEquals(2, tool.getRequiredParameters().size());
            assertTrue(tool.getRequiredParameters().contains("title"));
            assertTrue(tool.getRequiredParameters().contains("date"));
        }
    }

    // ---------------------------------------------------------------
    // 2. AIToolParameter types
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("AIToolParameter types")
    class ParameterTypes {

        @Test
        @DisplayName("String parameter")
        void stringParam() {
            AIToolParameter param = AIToolParameter.string("A name");
            assertEquals("string", param.getType());
            assertEquals("A name", param.getDescription());
        }

        @Test
        @DisplayName("Number parameter")
        void numberParam() {
            AIToolParameter param = AIToolParameter.number("A price");
            assertEquals("number", param.getType());
            assertEquals("A price", param.getDescription());
        }

        @Test
        @DisplayName("Integer parameter")
        void integerParam() {
            AIToolParameter param = AIToolParameter.integer("A count");
            assertEquals("integer", param.getType());
        }

        @Test
        @DisplayName("Boolean parameter")
        void boolParam() {
            AIToolParameter param = AIToolParameter.bool("Is active");
            assertEquals("boolean", param.getType());
        }

        @Test
        @DisplayName("String enum parameter")
        void stringEnumParam() {
            AIToolParameter param = AIToolParameter.stringEnum("Unit", List.of("celsius", "fahrenheit"));
            assertEquals("string", param.getType());
            assertEquals(2, param.getEnumValues().size());
            assertTrue(param.getEnumValues().contains("celsius"));
            assertTrue(param.getEnumValues().contains("fahrenheit"));
        }

        @Test
        @DisplayName("Object parameter with nested properties")
        void objectParam() {
            Map<String, AIToolParameter> props = Map.of(
                    "street", AIToolParameter.string("Street address"),
                    "city", AIToolParameter.string("City name")
            );
            AIToolParameter param = AIToolParameter.object("An address", props);
            assertEquals("object", param.getType());
            assertEquals(2, param.getProperties().size());
        }

        @Test
        @DisplayName("Array parameter with item type")
        void arrayParam() {
            AIToolParameter items = AIToolParameter.string("A tag");
            AIToolParameter param = AIToolParameter.array("Tags", items);
            assertEquals("array", param.getType());
            assertNotNull(param.getItems());
            assertEquals("string", param.getItems().getType());
        }

        @Test
        @DisplayName("toSchemaMap produces correct structure for string")
        void schemaMapString() {
            AIToolParameter param = AIToolParameter.string("A description");
            Map<String, Object> schema = param.toSchemaMap();
            assertEquals("string", schema.get("type"));
            assertEquals("A description", schema.get("description"));
        }

        @Test
        @DisplayName("toSchemaMap produces correct structure for enum")
        void schemaMapEnum() {
            AIToolParameter param = AIToolParameter.stringEnum("Color", List.of("red", "blue"));
            Map<String, Object> schema = param.toSchemaMap();
            assertEquals("string", schema.get("type"));
            @SuppressWarnings("unchecked")
            List<String> enumValues = (List<String>) schema.get("enum");
            assertNotNull(enumValues);
            assertTrue(enumValues.contains("red"));
        }
    }

    // ---------------------------------------------------------------
    // 3. AIToolResult
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("AIToolResult")
    class ToolResult {

        @Test
        @DisplayName("Creates successful result")
        void successResult() {
            AIToolResult result = new AIToolResult("call-123", "The weather is sunny");
            assertEquals("call-123", result.getToolCallId());
            assertEquals("The weather is sunny", result.getContent());
            assertFalse(result.isError());
        }

        @Test
        @DisplayName("Creates error result")
        void errorResult() {
            AIToolResult result = new AIToolResult("call-456", "Tool execution failed", true);
            assertEquals("call-456", result.getToolCallId());
            assertEquals("Tool execution failed", result.getContent());
            assertTrue(result.isError());
        }

        @Test
        @DisplayName("Creates non-error result with explicit false")
        void nonErrorResult() {
            AIToolResult result = new AIToolResult("call-789", "ok", false);
            assertFalse(result.isError());
        }
    }

    // ---------------------------------------------------------------
    // 4. AIToolHandler
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("AIToolHandler")
    class ToolHandler {

        @Test
        @DisplayName("Handler executes with arguments")
        void handlerExecutes() {
            AIToolHandler handler = AIToolHandler.of(args ->
                    "Temperature in " + args.get("city") + " is 72F"
            );
            Map<String, Object> args = Map.of("city", "New York");
            String result = handler.execute(args);
            assertEquals("Temperature in New York is 72F", result);
        }

        @Test
        @DisplayName("Handler works with empty arguments")
        void handlerEmptyArgs() {
            AIToolHandler handler = AIToolHandler.of(args -> "No args needed");
            String result = handler.execute(Map.of());
            assertEquals("No args needed", result);
        }
    }
}
