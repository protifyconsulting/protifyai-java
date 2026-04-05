package ai.protify.core.internal.provider.mock;

import ai.protify.core.AIClient;
import ai.protify.core.AIModel;
import ai.protify.core.conversation.AIConversation;
import ai.protify.core.conversation.AIConversationState;
import ai.protify.core.conversation.AIConversationStore;
import ai.protify.core.internal.pipeline.PipelineAIResponse;
import ai.protify.core.pipeline.AIPipeline;
import ai.protify.core.pipeline.AIPipelineResponse;
import ai.protify.core.provider.mock.MockProvider;
import ai.protify.core.provider.mock.MockProviderRequest;
import ai.protify.core.provider.mock.MockResponse;
import ai.protify.core.provider.mock.MockToolCall;
import ai.protify.core.request.AITextInput;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;
import ai.protify.core.response.MimeType;
import ai.protify.core.tool.AIToolCall;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MockProviderTest {

    private static AIClient clientFor(MockProvider mock) {
        return AIClient.builder()
                .model(AIModel.custom("mock-model", mock))
                .apiKey("mock-key")
                .build();
    }

    // ---------------------------------------------------------------
    // 1. MockProvider — AIProvider contract
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("AIProvider contract")
    class AIProviderContract {

        @Test
        @DisplayName("getName returns Mock")
        void getName() {
            MockProvider mock = MockProvider.withResponse("ok");
            assertEquals("Mock", mock.getName());
        }

        @Test
        @DisplayName("getApiKeyVarName returns MOCK_API_KEY")
        void getApiKeyVarName() {
            MockProvider mock = MockProvider.withResponse("ok");
            assertEquals("MOCK_API_KEY", mock.getApiKeyVarName());
        }

        @Test
        @DisplayName("getHeaders returns Content-Type")
        void getHeaders() {
            MockProvider mock = MockProvider.withResponse("ok");
            Map<String, String> headers = mock.getHeaders("any-credential");
            assertEquals("application/json", headers.get("Content-Type"));
        }

        @Test
        @DisplayName("isMimeTypeSupported returns true for all types")
        void allMimeTypesSupported() {
            MockProvider mock = MockProvider.withResponse("ok");
            for (MimeType type : MimeType.values()) {
                assertTrue(mock.isMimeTypeSupported(type), "Should support " + type);
            }
        }

        @Test
        @DisplayName("getProviderClientType returns MockProviderClient")
        void providerClientType() {
            MockProvider mock = MockProvider.withResponse("ok");
            assertNotNull(mock.getProviderClientType());
            assertEquals("MockProviderClient", mock.getProviderClientType().getSimpleName());
        }

        @Test
        @DisplayName("toString returns expected format")
        void toStringFormat() {
            MockProvider mock = MockProvider.withResponse("ok");
            assertEquals("AIProvider{name='Mock'}", mock.toString());
        }
    }

    // ---------------------------------------------------------------
    // 2. Static factory methods
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Static factory methods")
    class StaticFactories {

        @Test
        @DisplayName("withResponse(String) returns text")
        void withResponseString() {
            MockProvider mock = MockProvider.withResponse("Hello");
            AIClient client = clientFor(mock);

            AIResponse response = client.newRequest()
                    .addInput("test")
                    .maxOutputTokens(100)
                    .build()
                    .execute();

            assertEquals("Hello", response.text());
        }

        @Test
        @DisplayName("withResponse(AIResponse) returns provided response")
        void withResponseObject() {
            MockResponse customResponse = MockResponse.builder()
                    .text("custom")
                    .modelName("my-model")
                    .inputTokens(50)
                    .build();

            MockProvider mock = MockProvider.withResponse(customResponse);
            AIClient client = clientFor(mock);

            AIResponse response = client.newRequest()
                    .addInput("test")
                    .maxOutputTokens(100)
                    .build()
                    .execute();

            assertEquals("custom", response.text());
            assertEquals("my-model", response.getModelName());
            assertEquals(50, response.getInputTokens());
        }

        @Test
        @DisplayName("withResponseFunction receives request and returns dynamic response")
        void withResponseFunction() {
            MockProvider mock = MockProvider.withResponseFunction(request -> {
                String input = ((AITextInput) request.getInputs().get(0)).getText();
                return MockResponse.of("Echo: " + input);
            });
            AIClient client = clientFor(mock);

            AIResponse response = client.newRequest()
                    .addInput("hello world")
                    .maxOutputTokens(100)
                    .build()
                    .execute();

            assertEquals("Echo: hello world", response.text());
        }
    }

    // ---------------------------------------------------------------
    // 3. Builder
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builder with no configuration returns empty response")
        void emptyBuilder() {
            MockProvider mock = MockProvider.builder().build();
            AIClient client = clientFor(mock);

            AIResponse response = client.newRequest()
                    .addInput("test")
                    .maxOutputTokens(100)
                    .build()
                    .execute();

            assertEquals("", response.text());
        }

        @Test
        @DisplayName("builder with default response only")
        void defaultResponseOnly() {
            MockProvider mock = MockProvider.builder()
                    .defaultResponse("fallback")
                    .build();
            AIClient client = clientFor(mock);

            assertEquals("fallback", client.newRequest().addInput("1").maxOutputTokens(100).build().execute().text());
            assertEquals("fallback", client.newRequest().addInput("2").maxOutputTokens(100).build().execute().text());
        }

        @Test
        @DisplayName("builder with queued responses drains in order")
        void queuedResponses() {
            MockProvider mock = MockProvider.builder()
                    .response("First")
                    .response("Second")
                    .response("Third")
                    .build();
            AIClient client = clientFor(mock);

            assertEquals("First", client.newRequest().addInput("1").maxOutputTokens(100).build().execute().text());
            assertEquals("Second", client.newRequest().addInput("2").maxOutputTokens(100).build().execute().text());
            assertEquals("Third", client.newRequest().addInput("3").maxOutputTokens(100).build().execute().text());
            // Queue exhausted — falls back to empty
            assertEquals("", client.newRequest().addInput("4").maxOutputTokens(100).build().execute().text());
        }

        @Test
        @DisplayName("queued responses take priority over default and response function")
        void queuePriority() {
            MockProvider mock = MockProvider.builder()
                    .response("queued")
                    .defaultResponse("default")
                    .responseFunction(req -> MockResponse.of("function"))
                    .build();
            AIClient client = clientFor(mock);

            assertEquals("queued", client.newRequest().addInput("1").maxOutputTokens(100).build().execute().text());
            // Queue exhausted — falls to response function
            assertEquals("function", client.newRequest().addInput("2").maxOutputTokens(100).build().execute().text());
        }

        @Test
        @DisplayName("response function takes priority over default")
        void functionOverDefault() {
            MockProvider mock = MockProvider.builder()
                    .responseFunction(req -> MockResponse.of("function"))
                    .defaultResponse("default")
                    .build();
            AIClient client = clientFor(mock);

            assertEquals("function", client.newRequest().addInput("1").maxOutputTokens(100).build().execute().text());
        }

        @Test
        @DisplayName("builder with MockResponse objects in queue")
        void queuedMockResponses() {
            MockProvider mock = MockProvider.builder()
                    .response(MockResponse.builder().text("rich").inputTokens(10).outputTokens(5).build())
                    .build();
            AIClient client = clientFor(mock);

            AIResponse response = client.newRequest().addInput("test").maxOutputTokens(100).build().execute();
            assertEquals("rich", response.text());
            assertEquals(10, response.getInputTokens());
            assertEquals(5, response.getOutputTokens());
        }
    }

    // ---------------------------------------------------------------
    // 4. MockResponse
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("MockResponse")
    class MockResponseTests {

        @Test
        @DisplayName("of(String) creates response with defaults")
        void ofString() {
            MockResponse response = MockResponse.of("hello");

            assertEquals("hello", response.text());
            assertEquals("mock-model", response.getModelName());
            assertNotNull(response.getResponseId());
            assertFalse(response.getResponseId().isEmpty());
            assertEquals(0, response.getInputTokens());
            assertEquals(0, response.getOutputTokens());
            assertEquals(0, response.getTotalTokens());
            assertEquals(0, response.getProcessingTimeMillis());
            assertFalse(response.isCachedResponse());
            assertEquals("", response.getCorrelationId());
            assertEquals("", response.getPipelineId());
            assertEquals("", response.getProviderResponse());
            assertNull(response.getReasoningContent());
            assertNull(response.getStopReason());
            assertFalse(response.hasToolCalls());
            assertTrue(response.getToolCalls().isEmpty());
        }

        @Test
        @DisplayName("builder sets all fields")
        void builderAllFields() {
            MockResponse response = MockResponse.builder()
                    .text("answer")
                    .modelName("gpt-test")
                    .responseId("resp-123")
                    .inputTokens(100)
                    .outputTokens(50)
                    .processingTimeMillis(250)
                    .providerResponse("{\"raw\": true}")
                    .reasoningContent("thinking...")
                    .stopReason("end_turn")
                    .toolCalls(List.of(new MockToolCall("fn", Map.of())))
                    .build();

            assertEquals("answer", response.text());
            assertEquals("gpt-test", response.getModelName());
            assertEquals("resp-123", response.getResponseId());
            assertEquals(100, response.getInputTokens());
            assertEquals(50, response.getOutputTokens());
            assertEquals(150, response.getTotalTokens());
            assertEquals(250, response.getProcessingTimeMillis());
            assertEquals("{\"raw\": true}", response.getProviderResponse());
            assertEquals("thinking...", response.getReasoningContent());
            assertEquals("end_turn", response.getStopReason());
            assertTrue(response.hasToolCalls());
            assertEquals(1, response.getToolCalls().size());
        }

        @Test
        @DisplayName("as() deserializes JSON text to object")
        void asDeserializes() {
            MockProvider mock = MockProvider.withResponse("{\"name\":\"Tokyo\",\"country\":\"Japan\"}");
            AIClient client = clientFor(mock);

            CityInfo city = client.newRequest()
                    .addInput("test")
                    .maxOutputTokens(100)
                    .build()
                    .execute()
                    .as(CityInfo.class);

            assertEquals("Tokyo", city.name);
            assertEquals("Japan", city.country);
        }

        @Test
        @DisplayName("asList() deserializes JSON array to list")
        void asListDeserializes() {
            MockProvider mock = MockProvider.withResponse("[{\"name\":\"Red\"},{\"name\":\"Blue\"}]");
            AIClient client = clientFor(mock);

            List<ColorInfo> colors = client.newRequest()
                    .addInput("test")
                    .maxOutputTokens(100)
                    .build()
                    .execute()
                    .asList(ColorInfo.class);

            assertEquals(2, colors.size());
            assertEquals("Red", colors.get(0).name);
            assertEquals("Blue", colors.get(1).name);
        }
    }

    // ---------------------------------------------------------------
    // 5. MockToolCall
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("MockToolCall")
    class MockToolCallTests {

        @Test
        @DisplayName("auto-generates ID when not provided")
        void autoGeneratesId() {
            MockToolCall call = new MockToolCall("my_tool", Map.of("key", "value"));
            assertNotNull(call.getId());
            assertFalse(call.getId().isEmpty());
            assertEquals("my_tool", call.getName());
            assertEquals("value", call.getArguments().get("key"));
        }

        @Test
        @DisplayName("uses provided ID")
        void providedId() {
            MockToolCall call = new MockToolCall("call-42", "my_tool", Map.of("a", "b"));
            assertEquals("call-42", call.getId());
        }

        @Test
        @DisplayName("handles null arguments as empty map")
        void nullArguments() {
            MockToolCall call = new MockToolCall("my_tool", null);
            assertNotNull(call.getArguments());
            assertTrue(call.getArguments().isEmpty());
        }

        @Test
        @DisplayName("getArgumentsJson returns non-null string")
        void argumentsJson() {
            MockToolCall call = new MockToolCall("my_tool", Map.of("city", "London"));
            String json = call.getArgumentsJson();
            assertNotNull(json);
            assertFalse(json.isEmpty());
        }

        @Test
        @DisplayName("tool calls returned in response")
        void toolCallsInResponse() {
            List<AIToolCall> toolCalls = List.of(
                    new MockToolCall("get_weather", Map.of("city", "London")),
                    new MockToolCall("get_time", Map.of("timezone", "UTC"))
            );

            MockProvider mock = MockProvider.withResponse(
                    MockResponse.builder().toolCalls(toolCalls).build()
            );
            AIClient client = clientFor(mock);

            AIResponse response = client.newRequest()
                    .addInput("test")
                    .maxOutputTokens(100)
                    .build()
                    .execute();

            assertTrue(response.hasToolCalls());
            assertEquals(2, response.getToolCalls().size());
            assertEquals("get_weather", response.getToolCalls().get(0).getName());
            assertEquals("get_time", response.getToolCalls().get(1).getName());
        }
    }

    // ---------------------------------------------------------------
    // 6. Request recording
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Request recording")
    class RequestRecording {

        @Test
        @DisplayName("records each request")
        void recordsRequests() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIClient client = clientFor(mock);

            client.newRequest().addInput("first").maxOutputTokens(100).build().execute();
            client.newRequest().addInput("second").maxOutputTokens(100).build().execute();
            client.newRequest().addInput("third").maxOutputTokens(100).build().execute();

            assertEquals(3, mock.getRequestCount());
            assertEquals(3, mock.getRecordedRequests().size());
        }

        @Test
        @DisplayName("getLastRequest returns most recent")
        void lastRequest() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIClient client = clientFor(mock);

            client.newRequest().addInput("first").maxOutputTokens(100).build().execute();
            client.newRequest().addInput("second").maxOutputTokens(100).build().execute();

            MockProviderRequest last = mock.getLastRequest();
            assertNotNull(last);
            assertEquals(1, last.getInputs().size());
            String lastText = ((AITextInput) last.getInputs().get(0)).getText();
            assertEquals("second", lastText);
        }

        @Test
        @DisplayName("getLastRequest returns null when no requests")
        void lastRequestEmpty() {
            MockProvider mock = MockProvider.withResponse("ok");
            assertNull(mock.getLastRequest());
        }

        @Test
        @DisplayName("getRecordedRequests returns unmodifiable list")
        void unmodifiable() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIClient client = clientFor(mock);
            client.newRequest().addInput("test").maxOutputTokens(100).build().execute();

            assertThrows(UnsupportedOperationException.class, () ->
                    mock.getRecordedRequests().clear()
            );
        }

        @Test
        @DisplayName("clearRecordedRequests clears only requests, not responses")
        void clearRecordedRequests() {
            MockProvider mock = MockProvider.builder()
                    .defaultResponse("still works")
                    .build();
            AIClient client = clientFor(mock);

            client.newRequest().addInput("test").maxOutputTokens(100).build().execute();
            assertEquals(1, mock.getRequestCount());

            mock.clearRecordedRequests();
            assertEquals(0, mock.getRequestCount());

            // Responses still work
            assertEquals("still works",
                    client.newRequest().addInput("test").maxOutputTokens(100).build().execute().text());
        }

        @Test
        @DisplayName("recorded request contains input text")
        void requestContainsInputs() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIClient client = clientFor(mock);

            client.newRequest()
                    .addInput("input one")
                    .addInput("input two")
                    .maxOutputTokens(100)
                    .build()
                    .execute();

            MockProviderRequest request = mock.getLastRequest();
            assertEquals(2, request.getInputs().size());
            assertEquals("input one", ((AITextInput) request.getInputs().get(0)).getText());
            assertEquals("input two", ((AITextInput) request.getInputs().get(1)).getText());
        }
    }

    // ---------------------------------------------------------------
    // 7. Runtime mutation
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Runtime mutation")
    class RuntimeMutation {

        @Test
        @DisplayName("enqueueResponse after construction")
        void enqueueResponse() {
            MockProvider mock = MockProvider.withResponse("default");
            AIClient client = clientFor(mock);

            mock.enqueueResponse("queued-1");
            mock.enqueueResponse("queued-2");

            assertEquals("queued-1", client.newRequest().addInput("1").maxOutputTokens(100).build().execute().text());
            assertEquals("queued-2", client.newRequest().addInput("2").maxOutputTokens(100).build().execute().text());
            assertEquals("default", client.newRequest().addInput("3").maxOutputTokens(100).build().execute().text());
        }

        @Test
        @DisplayName("enqueueResponse with AIResponse object")
        void enqueueResponseObject() {
            MockProvider mock = MockProvider.withResponse("default");
            AIClient client = clientFor(mock);

            mock.enqueueResponse(MockResponse.builder().text("rich").inputTokens(99).build());

            AIResponse response = client.newRequest().addInput("1").maxOutputTokens(100).build().execute();
            assertEquals("rich", response.text());
            assertEquals(99, response.getInputTokens());
        }

        @Test
        @DisplayName("setDefaultResponse changes default")
        void setDefaultResponse() {
            MockProvider mock = MockProvider.withResponse("original");
            AIClient client = clientFor(mock);

            assertEquals("original", client.newRequest().addInput("1").maxOutputTokens(100).build().execute().text());

            mock.setDefaultResponse("updated");
            assertEquals("updated", client.newRequest().addInput("2").maxOutputTokens(100).build().execute().text());
        }

        @Test
        @DisplayName("setDefaultResponse with AIResponse object")
        void setDefaultResponseObject() {
            MockProvider mock = MockProvider.withResponse("original");
            AIClient client = clientFor(mock);

            mock.setDefaultResponse(MockResponse.builder().text("updated").outputTokens(77).build());

            AIResponse response = client.newRequest().addInput("1").maxOutputTokens(100).build().execute();
            assertEquals("updated", response.text());
            assertEquals(77, response.getOutputTokens());
        }

        @Test
        @DisplayName("setResponseFunction changes dynamic behavior")
        void setResponseFunction() {
            MockProvider mock = MockProvider.withResponse("static");
            AIClient client = clientFor(mock);

            mock.setResponseFunction(req -> MockResponse.of("dynamic"));

            // Default still takes priority since it's set — but response function
            // only kicks in when queue is empty and default is null.
            // Let's reset first:
            mock.reset();
            mock.setResponseFunction(req -> MockResponse.of("dynamic"));

            assertEquals("dynamic", client.newRequest().addInput("1").maxOutputTokens(100).build().execute().text());
        }

        @Test
        @DisplayName("reset clears all state")
        void reset() {
            MockProvider mock = MockProvider.builder()
                    .response("queued")
                    .defaultResponse("default")
                    .responseFunction(req -> MockResponse.of("function"))
                    .build();
            AIClient client = clientFor(mock);

            client.newRequest().addInput("1").maxOutputTokens(100).build().execute();
            assertEquals(1, mock.getRequestCount());

            mock.reset();
            assertEquals(0, mock.getRequestCount());

            // After reset, no queue, no default, no function — returns empty
            assertEquals("", client.newRequest().addInput("2").maxOutputTokens(100).build().execute().text());
        }
    }

    // ---------------------------------------------------------------
    // 8. Streaming
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Streaming")
    class StreamingTests {

        @Test
        @DisplayName("streams tokens character by character")
        void streamTokens() {
            MockProvider mock = MockProvider.withResponse("Hello");
            AIClient client = clientFor(mock);

            AIStreamResponse stream = client.newRequest()
                    .addInput("test")
                    .maxOutputTokens(100)
                    .build()
                    .executeStream();

            List<String> tokens = new ArrayList<>();
            stream.onToken(tokens::add);

            AIResponse response = stream.toResponse();

            assertEquals("Hello", response.text());
            assertEquals(5, tokens.size());
            assertEquals("H", tokens.get(0));
            assertEquals("o", tokens.get(4));
        }

        @Test
        @DisplayName("streaming with empty response completes without tokens")
        void streamEmpty() {
            MockProvider mock = MockProvider.withResponse("");
            AIClient client = clientFor(mock);

            AIStreamResponse stream = client.newRequest()
                    .addInput("test")
                    .maxOutputTokens(100)
                    .build()
                    .executeStream();

            List<String> tokens = new ArrayList<>();
            stream.onToken(tokens::add);

            AIResponse response = stream.toResponse();

            assertEquals("", response.text());
            assertTrue(tokens.isEmpty());
        }

        @Test
        @DisplayName("streaming records the request")
        void streamRecordsRequest() {
            MockProvider mock = MockProvider.withResponse("streamed");
            AIClient client = clientFor(mock);

            AIStreamResponse stream = client.newRequest()
                    .addInput("stream-input")
                    .maxOutputTokens(100)
                    .build()
                    .executeStream();

            stream.toResponse(); // wait for completion

            assertEquals(1, mock.getRequestCount());
        }
    }

    // ---------------------------------------------------------------
    // 9. Async
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Async execution")
    class AsyncTests {

        @Test
        @DisplayName("async execute returns correct response")
        void asyncExecute() throws Exception {
            MockProvider mock = MockProvider.withResponse("async result");
            AIClient client = clientFor(mock);

            CompletableFuture<AIResponse> future = client.newRequest()
                    .addInput("async test")
                    .maxOutputTokens(100)
                    .build()
                    .executeAsync();

            AIResponse response = future.get(5, TimeUnit.SECONDS);
            assertEquals("async result", response.text());
        }

        @Test
        @DisplayName("multiple concurrent async requests")
        void concurrentAsync() throws Exception {
            MockProvider mock = MockProvider.builder()
                    .response("first")
                    .response("second")
                    .response("third")
                    .build();
            AIClient client = clientFor(mock);

            CompletableFuture<AIResponse> f1 = client.newRequest().addInput("1").maxOutputTokens(100).build().executeAsync();
            CompletableFuture<AIResponse> f2 = client.newRequest().addInput("2").maxOutputTokens(100).build().executeAsync();
            CompletableFuture<AIResponse> f3 = client.newRequest().addInput("3").maxOutputTokens(100).build().executeAsync();

            CompletableFuture.allOf(f1, f2, f3).get(5, TimeUnit.SECONDS);

            // All three should have been served (order may vary due to concurrency)
            assertEquals(3, mock.getRequestCount());
        }
    }

    // ---------------------------------------------------------------
    // 10. Conversations
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Conversations")
    class ConversationTests {

        @Test
        @DisplayName("conversation sends and receives messages")
        void conversationBasic() {
            MockProvider mock = MockProvider.builder()
                    .response("Hi there!")
                    .response("I remember you said hello.")
                    .build();
            AIClient client = clientFor(mock);

            AIConversation conversation = client.newConversation()
                    .instructions("You are a test assistant.")
                    .maxOutputTokens(100)
                    .build();

            AIResponse r1 = conversation.send("Hello");
            assertEquals("Hi there!", r1.text());

            AIResponse r2 = conversation.send("Do you remember?");
            assertEquals("I remember you said hello.", r2.text());

            assertEquals(2, mock.getRequestCount());
        }

        @Test
        @DisplayName("conversation with persistence")
        void conversationWithStore() {
            InMemoryConversationStore store = new InMemoryConversationStore();

            MockProvider mock = MockProvider.builder()
                    .response("Noted.")
                    .response("Your favorite color is blue.")
                    .build();
            AIClient client = clientFor(mock);

            // First session
            AIConversation conversation = client.newConversation()
                    .store(store)
                    .instructions("Remember things.")
                    .maxOutputTokens(100)
                    .build();

            String conversationId = conversation.getId();
            conversation.send("My favorite color is blue.");

            // Second session — reload
            AIConversation reloaded = client.newConversation()
                    .store(store)
                    .id(conversationId)
                    .instructions("Remember things.")
                    .maxOutputTokens(100)
                    .build();

            AIResponse response = reloaded.send("What is my favorite color?");
            assertEquals("Your favorite color is blue.", response.text());
        }
    }

    // ---------------------------------------------------------------
    // 11. Pipelines
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Pipelines")
    class PipelineTests {

        @Test
        @DisplayName("sequential pipeline with mock")
        void sequentialPipeline() {
            MockProvider mock = MockProvider.builder()
                    .response("France")
                    .response("Paris")
                    .build();
            AIClient client = clientFor(mock);

            AIPipeline pipeline = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("Name a European country.")
                            .maxOutputTokens(100)
                            .build())
                    .addRequestStep(ctx -> client.newRequest()
                            .addInput("What is the capital of " + ctx.text() + "?")
                            .maxOutputTokens(100)
                            .build())
                    .build();

            AIPipelineResponse result = pipeline.execute();
            assertEquals("Paris", result.text());
            assertEquals(2, mock.getRequestCount());
        }

        @Test
        @DisplayName("pipeline with transformation step")
        void transformPipeline() {
            MockProvider mock = MockProvider.withResponse("hello world");
            AIClient client = clientFor(mock);

            AIPipeline pipeline = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("test")
                            .maxOutputTokens(100)
                            .build())
                    .addStep(ctx -> PipelineAIResponse.of(ctx.text().toUpperCase()))
                    .build();

            AIPipelineResponse result = pipeline.execute();
            assertEquals("HELLO WORLD", result.text());
        }

        @Test
        @DisplayName("pipeline streaming with mock")
        void pipelineStreaming() {
            MockProvider mock = MockProvider.withResponse("streamed pipeline");
            AIClient client = clientFor(mock);

            AIPipeline pipeline = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("test")
                            .maxOutputTokens(100)
                            .build())
                    .build();

            AIStreamResponse stream = pipeline.executeStream();

            List<String> tokens = new ArrayList<>();
            stream.onToken(tokens::add);

            AIResponse response = stream.toResponse();

            assertEquals("streamed pipeline", response.text());
            assertFalse(tokens.isEmpty());
        }

        @Test
        @DisplayName("async pipeline with mock")
        void asyncPipeline() throws Exception {
            MockProvider mock = MockProvider.withResponse("async pipeline result");
            AIClient client = clientFor(mock);

            AIPipeline pipeline = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("test")
                            .maxOutputTokens(100)
                            .build())
                    .build();

            AIPipelineResponse result = pipeline.executeAsync().get(5, TimeUnit.SECONDS);
            assertEquals("async pipeline result", result.text());
        }
    }

    // ---------------------------------------------------------------
    // 12. MockProviderRequest
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("MockProviderRequest")
    class MockProviderRequestTests {

        @Test
        @DisplayName("toJson returns mock JSON")
        void toJson() {
            MockProviderRequest request = new MockProviderRequest();
            assertEquals("{\"mock\": true}", request.toJson());
        }

        @Test
        @DisplayName("toLoggableJson returns same as toJson")
        void toLoggableJson() {
            MockProviderRequest request = new MockProviderRequest();
            assertEquals(request.toJson(), request.toLoggableJson());
        }
    }

    // ---------------------------------------------------------------
    // 13. Integration — MockProvider with AIModel.custom
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Integration with AIModel.custom")
    class IntegrationTests {

        @Test
        @DisplayName("works with custom model name")
        void customModelName() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIClient client = AIClient.builder()
                    .model(AIModel.custom("my-custom-model", mock))
                    .apiKey("test-key")
                    .build();

            assertEquals("my-custom-model", client.getModelName());
            assertEquals("Mock", client.getProvider().getName());
        }

        @Test
        @DisplayName("multiple clients share same MockProvider")
        void sharedMockProvider() {
            MockProvider mock = MockProvider.builder()
                    .response("from-client-1")
                    .response("from-client-2")
                    .build();

            AIClient client1 = AIClient.builder()
                    .model(AIModel.custom("model-1", mock))
                    .apiKey("key-1")
                    .build();

            AIClient client2 = AIClient.builder()
                    .model(AIModel.custom("model-2", mock))
                    .apiKey("key-2")
                    .build();

            assertEquals("from-client-1",
                    client1.newRequest().addInput("test").maxOutputTokens(100).build().execute().text());
            assertEquals("from-client-2",
                    client2.newRequest().addInput("test").maxOutputTokens(100).build().execute().text());
            assertEquals(2, mock.getRequestCount());
        }
    }

    // ---------------------------------------------------------------
    // Helper types
    // ---------------------------------------------------------------

    static class CityInfo {
        public String name;
        public String country;
    }

    static class ColorInfo {
        public String name;
    }

    static class InMemoryConversationStore implements AIConversationStore {
        private final HashMap<String, AIConversationState> store = new HashMap<>();

        @Override
        public void save(AIConversationState state) { store.put(state.getId(), state); }
        @Override
        public AIConversationState load(String conversationId) { return store.get(conversationId); }
        @Override
        public void delete(String conversationId) { store.remove(conversationId); }
    }
}
