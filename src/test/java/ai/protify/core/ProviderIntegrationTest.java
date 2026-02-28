package ai.protify.core;

import ai.protify.core.conversation.AIConversation;
import ai.protify.core.conversation.AIConversationState;
import ai.protify.core.conversation.AIConversationStore;
import ai.protify.core.internal.pipeline.PipelineAIResponse;
import ai.protify.core.pipeline.AIPipeline;
import ai.protify.core.pipeline.AIPipelineResponse;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;
import ai.protify.core.service.AIService;
import ai.protify.core.service.MaxTokens;
import ai.protify.core.service.UserMessage;
import ai.protify.core.service.V;
import ai.protify.core.tool.AITool;
import ai.protify.core.tool.AIToolCall;
import ai.protify.core.tool.AIToolHandler;
import ai.protify.core.tool.AIToolParameter;
import ai.protify.core.tool.AIToolResult;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ProviderIntegrationTest {

    // Delay in milliseconds between API calls to avoid provider rate limits.
    // Set to 0 to disable delays.
    private static final long API_CALL_DELAY_MS = 500;

    @BeforeEach
    void rateLimitPause() throws InterruptedException {
        if (API_CALL_DELAY_MS > 0) {
            Thread.sleep(API_CALL_DELAY_MS);
        }
    }

    private static void delayBetweenCalls() {
        if (API_CALL_DELAY_MS > 0) {
            try { Thread.sleep(API_CALL_DELAY_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    // One model per provider — cheapest/fastest option chosen
    static Stream<AIModel> allModels() {
        return Stream.of(
                AIModel.GPT_5_NANO,                    // OpenAI
                AIModel.MISTRAL_SMALL,                  // Mistral
                AIModel.CLAUDE_HAIKU_4_5,               // Anthropic
                AIModel.GEMINI_2_5_FLASH,             // Gemini
                AIModel.DEEPSEEK_CHAT,                  // DeepSeek
                AIModel.LLAMA_4_MAVERICK_TOGETHER,      // Together
                AIModel.QWEN_3_8B_FIREWORKS,            // Fireworks
                AIModel.GROK_3_MINI                      // xAI
        );
    }

    // Models that reliably support tool use
    static Stream<AIModel> toolModels() {
        return Stream.of(
                AIModel.GPT_5_NANO,
                AIModel.MISTRAL_SMALL,
                AIModel.CLAUDE_HAIKU_4_5,
                AIModel.GEMINI_2_5_FLASH,
                AIModel.GROK_3_MINI
        );
    }

    // Models that support streaming
    static Stream<AIModel> streamModels() {
        return Stream.of(
                AIModel.GPT_5_NANO,
                AIModel.MISTRAL_SMALL,
                AIModel.CLAUDE_HAIKU_4_5,
                AIModel.GEMINI_2_5_FLASH,
                AIModel.DEEPSEEK_CHAT,
                AIModel.GROK_3_MINI
        );
    }

    @BeforeAll
    static void initialize() {
        // Reset BaseConfiguration to defaults in case other test classes
        // (e.g., BaseConfigurationTest) contaminated the singleton with
        // properties like temperature=0.75 that GPT-5 models reject.
        try {
            Object baseConfig = Class.forName("ai.protify.core.internal.config.BaseConfiguration")
                    .getMethod("getInstance").invoke(null);
            java.lang.reflect.Method reset = baseConfig.getClass()
                    .getDeclaredMethod("resetForTesting", String.class, java.util.Map.class);
            reset.setAccessible(true);
            reset.invoke(baseConfig, "protifyai", java.util.Map.of());
        } catch (Exception e) {
            System.err.println("Warning: Could not reset BaseConfiguration: " + e.getMessage());
        }
        ProtifyAI.initialize();
    }

    // ---------------------------------------------------------------
    // Static inner types
    // ---------------------------------------------------------------

    public static class CityInfo {
        private String name;
        private String country;
        private int population;

        public CityInfo() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public int getPopulation() { return population; }
        public void setPopulation(int population) { this.population = population; }
    }

    public static class ColorInfo {
        private String name;
        private String hexCode;

        public ColorInfo() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getHexCode() { return hexCode; }
        public void setHexCode(String hexCode) { this.hexCode = hexCode; }
    }

    public static class InMemoryConversationStore implements AIConversationStore {
        private final HashMap<String, AIConversationState> store = new HashMap<>();

        @Override
        public void save(AIConversationState state) { store.put(state.getId(), state); }
        @Override
        public AIConversationState load(String conversationId) { return store.get(conversationId); }
        @Override
        public void delete(String conversationId) { store.remove(conversationId); }
    }

    @AIService
    public interface AssistantService {

        @UserMessage("Briefly answer: {{question}}")
        @MaxTokens(4096)
        String answer(@V("question") String question);

        @UserMessage("Return a JSON object with fields name, country, and population for the city: {{city}}. Only return the JSON, no markdown.")
        @MaxTokens(4096)
        CityInfo getCityInfo(@V("city") String city);

        @UserMessage("Say hello to {{name}} in one sentence.")
        @MaxTokens(4096)
        AIStreamResponse streamGreeting(@V("name") String name);

        @UserMessage("Briefly answer: {{question}}")
        @MaxTokens(4096)
        CompletableFuture<String> answerAsync(@V("question") String question);
    }

    // ---------------------------------------------------------------
    // 1. BasicRequests
    // ---------------------------------------------------------------

    @Nested
    class BasicRequests {

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Send text prompt and receive response")
        void sendTextPromptAndReceiveResponse(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AIResponse response = client.newRequest()
                    .addInput("What is 2 + 2? Reply with just the number.")
                    .maxOutputTokens(4096)
                    .build()
                    .execute();

            assertNotNull(response.text(), model.getName());
            assertFalse(response.text().isEmpty(), model.getName());
            assertNotNull(response.getModelName(), model.getName());
            assertFalse(response.getModelName().isEmpty(), model.getName());
            assertTrue(response.getInputTokens() > 0, model.getName());
            assertTrue(response.getOutputTokens() > 0, model.getName());
            assertTrue(response.getTotalTokens() > 0, model.getName());
            assertTrue(response.getProcessingTimeMillis() >= 0, model.getName());
            assertNotNull(response.getResponseId(), model.getName());
            assertFalse(response.getResponseId().isEmpty(), model.getName());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Structured output — single object")
        void structuredOutputSingleObject(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AIResponse response = client.newRequest()
                    .addInput("Return a JSON object with fields name, country, and population (integer) for Paris, France. Only return the JSON, no markdown.")
                    .maxOutputTokens(4096)
                    .build()
                    .execute();

            CityInfo city = response.as(CityInfo.class);
            assertNotNull(city, model.getName());
            assertNotNull(city.getName(), model.getName());
            assertTrue(city.getName().toLowerCase().contains("paris"), model.getName() + ": " + city.getName());
            assertNotNull(city.getCountry(), model.getName());
            assertTrue(city.getCountry().toLowerCase().contains("france"), model.getName() + ": " + city.getCountry());
            assertTrue(city.getPopulation() > 0, model.getName());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Structured output — list")
        void structuredOutputList(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AIResponse response = client.newRequest()
                    .addInput("Return a JSON array of 3 objects, each with fields name and hexCode, for the colors red, green, and blue. Only return the JSON array, no markdown.")
                    .maxOutputTokens(4096)
                    .build()
                    .execute();

            List<ColorInfo> colors = response.asList(ColorInfo.class);
            assertNotNull(colors, model.getName());
            assertEquals(3, colors.size(), model.getName());
            for (ColorInfo color : colors) {
                assertNotNull(color.getName(), model.getName());
                assertFalse(color.getName().isEmpty(), model.getName());
                assertNotNull(color.getHexCode(), model.getName());
                assertFalse(color.getHexCode().isEmpty(), model.getName());
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Multiple inputs")
        void multipleInputs(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AIResponse response = client.newRequest()
                    .addInput("Remember: the secret word is 'banana'.")
                    .addInput("What is the secret word? Reply with just the word.")
                    .maxOutputTokens(4096)
                    .build()
                    .execute();

            assertNotNull(response.text(), model.getName());
            assertTrue(response.text().toLowerCase().contains("banana"), model.getName() + ": " + response.text());
        }
    }

    // ---------------------------------------------------------------
    // 2. Streaming
    // ---------------------------------------------------------------

    @Nested
    class Streaming {

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#streamModels")
        @DisplayName("Stream tokens")
        void streamTokens(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AIStreamResponse stream = client.newRequest()
                    .addInput("Count from 1 to 5, each number on its own line.")
                    .maxOutputTokens(4096)
                    .build()
                    .executeStream();

            List<String> tokens = new ArrayList<>();
            stream.onToken(tokens::add);

            AIResponse response = stream.toResponse();

            assertFalse(tokens.isEmpty(), model.getName() + ": Expected at least one streamed token");

            StringBuilder joined = new StringBuilder();
            for (String token : tokens) {
                joined.append(token);
            }
            String joinedText = joined.toString();

            assertNotNull(response.text(), model.getName());
            assertFalse(response.text().isEmpty(), model.getName());
            assertTrue(joinedText.contains("1"), model.getName());
            assertTrue(joinedText.contains("5"), model.getName());
        }
    }

    // ---------------------------------------------------------------
    // 3. Async
    // ---------------------------------------------------------------

    @Nested
    class Async {

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Async execution")
        void asyncExecution(AIModel model) throws Exception {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            CompletableFuture<AIResponse> future = client.newRequest()
                    .addInput("What is the capital of Japan? Reply with just the city name.")
                    .maxOutputTokens(4096)
                    .build()
                    .executeAsync();

            AIResponse response = future.get(60, TimeUnit.SECONDS);

            assertNotNull(response, model.getName());
            assertNotNull(response.text(), model.getName());
            assertTrue(response.text().toLowerCase().contains("tokyo"), model.getName() + ": " + response.text());
        }
    }

    // ---------------------------------------------------------------
    // 4. Tools
    // ---------------------------------------------------------------

    @Nested
    class Tools {

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#toolModels")
        @DisplayName("Manual tool use")
        void manualToolUse(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AITool weatherTool = AITool.builder("get_weather")
                    .description("Get the current weather for a city")
                    .addRequiredParameter("city", AIToolParameter.string("The city name"))
                    .build();

            AIResponse firstResponse = client.newRequest()
                    .addInput("What is the weather in London?")
                    .addTool(weatherTool)
                    .maxOutputTokens(4096)
                    .build()
                    .execute();

            assertTrue(firstResponse.hasToolCalls(), model.getName() + ": Expected tool calls");
            assertFalse(firstResponse.getToolCalls().isEmpty(), model.getName());

            AIToolCall toolCall = firstResponse.getToolCalls().get(0);
            assertEquals("get_weather", toolCall.getName(), model.getName());
            assertNotNull(toolCall.getId(), model.getName());

            AIResponse finalResponse = client.newRequest()
                    .addInput("What is the weather in London?")
                    .addTool(weatherTool)
                    .previousResponse(firstResponse)
                    .addToolResult(new AIToolResult(toolCall.getId(), "{\"temperature\": \"15C\", \"condition\": \"cloudy\"}"))
                    .maxOutputTokens(4096)
                    .build()
                    .execute();

            assertNotNull(finalResponse.text(), model.getName());
            assertFalse(finalResponse.text().isEmpty(), model.getName());
            assertFalse(finalResponse.hasToolCalls(), model.getName());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#toolModels")
        @DisplayName("Automatic tool use")
        void automaticToolUse(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AITool calculatorTool = AITool.builder("calculate")
                    .description("Evaluate a math expression and return the result")
                    .addRequiredParameter("expression", AIToolParameter.string("The math expression to evaluate"))
                    .build();

            AIToolHandler handler = AIToolHandler.of(args -> {
                String expression = (String) args.get("expression");
                if (expression != null && expression.contains("7") && expression.contains("8")) {
                    return "56";
                }
                return "unknown";
            });

            AIResponse response = client.newRequest()
                    .addInput("Use the calculate tool to compute 7 * 8, then tell me the result.")
                    .addTool(calculatorTool, handler)
                    .maxOutputTokens(4096)
                    .build()
                    .execute();

            assertNotNull(response.text(), model.getName());
            assertTrue(response.text().contains("56"), model.getName() + ": " + response.text());
            assertFalse(response.hasToolCalls(), model.getName() + ": Expected no pending tool calls after auto-invocation");
        }
    }

    // ---------------------------------------------------------------
    // 5. Conversations
    // ---------------------------------------------------------------

    @Nested
    class Conversations {

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Conversation context")
        void conversationContext(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AIConversation conversation = client.newConversation()
                    .instructions("You are a helpful assistant. Keep responses brief.")
                    .maxOutputTokens(4096)
                    .build();

            conversation.send("My name is Alice.");

            AIResponse response = conversation.send("What is my name? Reply with just the name.");

            assertNotNull(response.text(), model.getName());
            assertTrue(response.text().toLowerCase().contains("alice"), model.getName() + ": " + response.text());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Conversation persistence")
        void conversationPersistence(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            InMemoryConversationStore memoryStore = new InMemoryConversationStore();

            AIConversation conversation = client.newConversation()
                    .store(memoryStore)
                    .instructions("You are a helpful assistant. Keep responses brief.")
                    .maxOutputTokens(4096)
                    .build();

            String conversationId = conversation.getId();

            conversation.send("My favorite color is blue.");
            conversation.send("Remember that.");

            AIConversation reloaded = client.newConversation()
                    .store(memoryStore)
                    .id(conversationId)
                    .instructions("You are a helpful assistant. Keep responses brief.")
                    .maxOutputTokens(4096)
                    .build();

            AIResponse response = reloaded.send("What is my favorite color? Reply with just the color.");

            assertNotNull(response.text(), model.getName());
            assertTrue(response.text().toLowerCase().contains("blue"), model.getName() + ": " + response.text());
        }
    }

    // ---------------------------------------------------------------
    // 6. Pipelines
    // ---------------------------------------------------------------

    @Nested
    class Pipelines {

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Sequential pipeline")
        void sequentialPipeline(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AIPipeline pipeline = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("Name one European country. Reply with just the country name.")
                            .maxOutputTokens(4096)
                            .build())
                    .addStep(ctx -> { delayBetweenCalls(); return PipelineAIResponse.of(ctx.text()); })
                    .addRequestStep(ctx -> client.newRequest()
                            .addInput("What is the capital of " + ctx.text() + "? Reply with just the city name.")
                            .maxOutputTokens(4096)
                            .build())
                    .build();

            AIPipelineResponse result = pipeline.execute();

            assertNotNull(result.text(), model.getName());
            assertFalse(result.text().isEmpty(), model.getName());
            assertEquals(3, result.getStepCount(), model.getName());
            assertNotNull(result.getStepResponses(), model.getName());
            assertEquals(3, result.getStepResponses().size(), model.getName());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Parallel pipeline")
        void parallelPipeline(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AIPipeline pipeline = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("Say 'start'. Reply with just that word.")
                            .maxOutputTokens(4096)
                            .build())
                    .addStep(ctx -> { delayBetweenCalls(); return PipelineAIResponse.of(ctx.text()); })
                    .addParallelStep(Arrays.asList(
                            ctx -> client.newRequest()
                                    .addInput("Say 'branch-A'. Reply with just that word.")
                                    .maxOutputTokens(4096)
                                    .build()
                                    .execute(),
                            ctx -> client.newRequest()
                                    .addInput("Say 'branch-B'. Reply with just that word.")
                                    .maxOutputTokens(4096)
                                    .build()
                                    .execute()
                    ))
                    .build();

            AIPipelineResponse result = pipeline.execute();

            assertNotNull(result.text(), model.getName());
            assertTrue(result.text().contains("---"), model.getName() + ": Expected parallel results joined with --- separator");
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Conditional pipeline")
        void conditionalPipeline(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AIPipeline pipeline = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("Is the sky blue? Reply with just 'yes' or 'no'.")
                            .maxOutputTokens(4096)
                            .build())
                    .addStep(ctx -> { delayBetweenCalls(); return PipelineAIResponse.of(ctx.text()); })
                    .addConditionalStep(cond -> cond
                            .when(
                                    ctx -> ctx.text() != null && ctx.text().toLowerCase().contains("yes"),
                                    ctx -> client.newRequest()
                                            .addInput("What color is grass? Reply with just the color.")
                                            .maxOutputTokens(4096)
                                            .build()
                                            .execute()
                            )
                            .otherwise(ctx -> client.newRequest()
                                    .addInput("What color is snow? Reply with just the color.")
                                    .maxOutputTokens(4096)
                                    .build()
                                    .execute())
                    )
                    .build();

            AIPipelineResponse result = pipeline.execute();

            assertNotNull(result.text(), model.getName());
            assertTrue(result.text().toLowerCase().contains("green")
                    || result.text().toLowerCase().contains("white"), model.getName() + ": " + result.text());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Transform pipeline")
        void transformPipeline(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AIPipeline pipeline = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("Say 'hello'. Reply with just that word.")
                            .maxOutputTokens(4096)
                            .build())
                    .addStep(ctx -> {
                        ctx.addCustomProperty("greeting", ctx.text());
                        delayBetweenCalls();
                        return PipelineAIResponse.of(ctx.text());
                    })
                    .addStep(ctx -> {
                        Object greeting = ctx.getCustomProperty("greeting");
                        assertNotNull(greeting, model.getName() + ": Custom property 'greeting' should be set");
                        return client.newRequest()
                                .addInput("The previous greeting was: " + greeting + ". Now say 'goodbye'. Reply with just that word.")
                                .maxOutputTokens(4096)
                                .build()
                                .execute();
                    })
                    .build();

            AIPipelineResponse result = pipeline.execute();

            assertNotNull(result.text(), model.getName());
            assertFalse(result.text().isEmpty(), model.getName());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Loop pipeline")
        void loopPipeline(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AIPipeline pipeline = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("Say 'loop-start'. Reply with just that word.")
                            .maxOutputTokens(4096)
                            .build())
                    .addStep(ctx -> { delayBetweenCalls(); return PipelineAIResponse.of(ctx.text()); })
                    .addLoopStep(loop -> loop
                            .step(ctx -> {
                                ctx.addCustomProperty("loopRan", true);
                                return client.newRequest()
                                        .addInput("Say 'done'. Reply with just that word.")
                                        .maxOutputTokens(4096)
                                        .build()
                                        .execute();
                            })
                            .until(ctx -> ctx.text() != null && ctx.text().toLowerCase().contains("done"))
                            .maxIterations(3)
                    )
                    .build();

            AIPipelineResponse result = pipeline.execute();

            assertNotNull(result.text(), model.getName());
            assertTrue(result.text().toLowerCase().contains("done"), model.getName() + ": " + result.text());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Safe pipeline")
        void safePipeline(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AIPipeline pipeline = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("Say 'start'. Reply with just that word.")
                            .maxOutputTokens(4096)
                            .build())
                    .addSafeStep(safe -> safe
                            .step(ctx -> {
                                throw new RuntimeException("Intentional failure for testing");
                            })
                            .onError((ctx, ex) -> PipelineAIResponse.of("recovered from: " + ex.getMessage()))
                    )
                    .build();

            AIPipelineResponse result = pipeline.execute();

            assertNotNull(result.text(), model.getName());
            assertTrue(result.text().contains("recovered from"), model.getName());
            assertTrue(result.text().contains("Intentional failure"), model.getName());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#streamModels")
        @DisplayName("Streaming pipeline")
        void streamingPipeline(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AIPipeline pipeline = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("Count from 1 to 3, each number on its own line.")
                            .maxOutputTokens(4096)
                            .build())
                    .build();

            AIStreamResponse stream = pipeline.executeStream();

            List<String> tokens = new ArrayList<>();
            stream.onToken(tokens::add);

            AIResponse response = stream.toResponse();

            assertNotNull(response.text(), model.getName());
            assertFalse(response.text().isEmpty(), model.getName());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Async pipeline")
        void asyncPipeline(AIModel model) throws Exception {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AIPipeline pipeline = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("What is 3 + 4? Reply with just the number.")
                            .maxOutputTokens(4096)
                            .build())
                    .build();

            CompletableFuture<AIPipelineResponse> future = pipeline.executeAsync();

            AIPipelineResponse result = future.get(60, TimeUnit.SECONDS);

            assertNotNull(result, model.getName());
            assertNotNull(result.text(), model.getName());
            assertFalse(result.text().isEmpty(), model.getName());
        }
    }

    // ---------------------------------------------------------------
    // 7. DeclarativeServices
    // ---------------------------------------------------------------

    @Nested
    class DeclarativeServices {

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Declarative string return")
        void declarativeStringReturn(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AssistantService service = ProtifyAI.create(AssistantService.class, client);

            String answer = service.answer("What is the capital of France? Reply with just the city name.");

            assertNotNull(answer, model.getName());
            assertFalse(answer.isEmpty(), model.getName());
            assertTrue(answer.toLowerCase().contains("paris"),
                    model.getName() + ": Expected 'paris' in answer but got: " + answer);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Declarative structured output")
        void declarativeStructuredOutput(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AssistantService service = ProtifyAI.create(AssistantService.class, client);

            CityInfo city = service.getCityInfo("Tokyo");

            assertNotNull(city, model.getName());
            assertNotNull(city.getName(), model.getName());
            assertTrue(city.getName().toLowerCase().contains("tokyo"), model.getName() + ": " + city.getName());
            assertNotNull(city.getCountry(), model.getName());
            assertTrue(city.getCountry().toLowerCase().contains("japan"), model.getName() + ": " + city.getCountry());
            assertTrue(city.getPopulation() > 0, model.getName());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#streamModels")
        @DisplayName("Declarative streaming")
        void declarativeStreaming(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AssistantService service = ProtifyAI.create(AssistantService.class, client);

            AIStreamResponse stream = service.streamGreeting("World");

            List<String> tokens = new ArrayList<>();
            stream.onToken(tokens::add);

            AIResponse response = stream.toResponse();

            assertNotNull(response.text(), model.getName());
            assertFalse(response.text().isEmpty(), model.getName());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Declarative async")
        void declarativeAsync(AIModel model) throws Exception {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AssistantService service = ProtifyAI.create(AssistantService.class, client);

            CompletableFuture<String> future = service.answerAsync("What is 10 + 5?");

            String answer = future.get(60, TimeUnit.SECONDS);

            assertNotNull(answer, model.getName());
            assertFalse(answer.isEmpty(), model.getName());
        }
    }

    // ---------------------------------------------------------------
    // 8. Configuration
    // ---------------------------------------------------------------

    @Nested
    class Configuration {

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Request-level overrides")
        void requestLevelOverrides(AIModel model) {
            AIClient client = AIClient.builder()
                    .model(model)
                    .build();

            AIResponse response = client.newRequest()
                    .addInput("Say 'hello'. Reply with just that word.")
                    .maxOutputTokens(500)
                    .build()
                    .execute();

            assertNotNull(response.text(), model.getName());
            assertFalse(response.text().isEmpty(), model.getName());
            assertTrue(response.getOutputTokens() > 0, model.getName());
            assertTrue(response.getOutputTokens() <= 500,
                    model.getName() + ": Expected output tokens within limit, but got: " + response.getOutputTokens());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ai.protify.core.ProviderIntegrationTest#allModels")
        @DisplayName("Custom model")
        void customModel(AIModel model) {
            AIModel custom = AIModel.custom(model.getName(), model.getProvider());

            AIClient client = AIClient.builder()
                    .model(custom)
                    .build();

            AIResponse response = client.newRequest()
                    .addInput("Say 'hello'. Reply with just that one word.")
                    .maxOutputTokens(4096)
                    .build()
                    .execute();

            assertNotNull(response.text(), model.getName());
            assertFalse(response.text().isEmpty(), model.getName());
            assertNotNull(response.getModelName(), model.getName());
        }
    }
}
