package ai.protify.core;

import ai.protify.core.pipeline.AIPipeline;
import ai.protify.core.pipeline.AIPipelineResponse;
import ai.protify.core.response.AIResponse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
public class PipelineTokenReportingIntegrationTest {

    private static final long API_CALL_DELAY_MS = 500;

    @BeforeAll
    static void initialize() {
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

    @BeforeEach
    void rateLimitPause() throws InterruptedException {
        if (API_CALL_DELAY_MS > 0) {
            Thread.sleep(API_CALL_DELAY_MS);
        }
    }

    @Test
    @DisplayName("Two-step pipeline (Anthropic + OpenAI) reports output tokens per step and aggregated")
    void multiProviderPipelineTokenReporting() {
        AIClient anthropicClient = AIClient.builder()
                .model(AIModel.CLAUDE_HAIKU_4_5)
                .build();

        AIClient openaiClient = AIClient.builder()
                .model(AIModel.GPT_5_4_MINI)
                .build();

        AIPipeline pipeline = AIPipeline.builder()
                .withInitialStep(() -> anthropicClient.newRequest()
                        .addInput("Name one European country. Reply with just the country name.")
                        .maxOutputTokens(1024)
                        .build())
                .addRequestStep(ctx -> {
                    try { Thread.sleep(API_CALL_DELAY_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return openaiClient.newRequest()
                            .addInput("What is the capital of " + ctx.text() + "? Reply with just the city name.")
                            .maxOutputTokens(1024)
                            .build();
                })
                .build();

        AIPipelineResponse result = pipeline.execute();

        // --- Basic response validation ---
        assertNotNull(result.text(), "Final pipeline text should not be null");
        assertFalse(result.text().isEmpty(), "Final pipeline text should not be empty");

        // --- Step count ---
        assertEquals(2, result.getStepCount(), "Pipeline should have exactly 2 steps");

        // --- Step 1 (Anthropic) token validation ---
        AIResponse step1 = result.getStepResponse(0);
        assertNotNull(step1, "Step 1 response should not be null");
        assertTrue(step1.getOutputTokens() > 0,
                "Step 1 (Anthropic) output tokens should be > 0, got: " + step1.getOutputTokens());
        assertTrue(step1.getInputTokens() > 0,
                "Step 1 (Anthropic) input tokens should be > 0, got: " + step1.getInputTokens());
        assertTrue(step1.getTotalTokens() > 0,
                "Step 1 (Anthropic) total tokens should be > 0, got: " + step1.getTotalTokens());

        // --- Step 2 (OpenAI) token validation ---
        AIResponse step2 = result.getStepResponse(1);
        assertNotNull(step2, "Step 2 response should not be null");
        assertTrue(step2.getOutputTokens() > 0,
                "Step 2 (OpenAI) output tokens should be > 0, got: " + step2.getOutputTokens());
        assertTrue(step2.getInputTokens() > 0,
                "Step 2 (OpenAI) input tokens should be > 0, got: " + step2.getInputTokens());
        assertTrue(step2.getTotalTokens() > 0,
                "Step 2 (OpenAI) total tokens should be > 0, got: " + step2.getTotalTokens());

        // --- Pipeline-level aggregated token validation ---
        long expectedTotalOutput = step1.getOutputTokens() + step2.getOutputTokens();
        long expectedTotalInput = step1.getInputTokens() + step2.getInputTokens();
        long expectedTotalTokens = step1.getTotalTokens() + step2.getTotalTokens();

        assertEquals(expectedTotalOutput, result.getTotalOutputTokens(),
                "Pipeline totalOutputTokens should equal sum of step output tokens");
        assertEquals(expectedTotalInput, result.getTotalInputTokens(),
                "Pipeline totalInputTokens should equal sum of step input tokens");
        assertEquals(expectedTotalTokens, result.getTotalTokensUsed(),
                "Pipeline totalTokensUsed should equal sum of step total tokens");

        // --- Pipeline-level totals must be strictly greater than any single step ---
        assertTrue(result.getTotalOutputTokens() > step1.getOutputTokens(),
                "Pipeline output tokens should exceed any single step");
        assertTrue(result.getTotalOutputTokens() > step2.getOutputTokens(),
                "Pipeline output tokens should exceed any single step");

        // --- The final response delegates to the last step ---
        assertEquals(step2.getOutputTokens(), result.getOutputTokens(),
                "Pipeline getOutputTokens() should delegate to the final step response");
        assertEquals(step2.getModelName(), result.getModelName(),
                "Pipeline getModelName() should delegate to the final step response");

        // --- Processing time ---
        assertTrue(result.getTotalProcessingTimeMillis() >= 0,
                "Pipeline total processing time should be non-negative");
    }
}
