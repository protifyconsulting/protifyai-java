package ai.protify.core;

import ai.protify.core.internal.SupportedModel;
import ai.protify.core.response.AIResponse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@Tag("smoke")
public class AllModelsSmokeTest {

    private static final long API_CALL_DELAY_MS = 1000;

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

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SupportedModel.class, mode = EnumSource.Mode.EXCLUDE, names = {
            "GEMINI_2_5_PRO_VERTEX",
            "GEMINI_2_5_FLASH_VERTEX",
            "CLAUDE_OPUS_4_6_BEDROCK",
            "CLAUDE_SONNET_4_6_BEDROCK",
            "CLAUDE_HAIKU_4_5_BEDROCK"
    })
    void smokeTest(SupportedModel model) {
        AIClient client = AIClient.builder()
                .model(model)
                .build();

        AIResponse response = client.newRequest()
                .addInput("Say hello.")
                .maxOutputTokens(100)
                .build()
                .execute();

        assertNotNull(response.text(), model.getName() + ": response text is null");
        assertFalse(response.text().isEmpty(), model.getName() + ": response text is empty");
        assertNotNull(response.getModelName(), model.getName() + ": model name is null");
        assertFalse(response.getModelName().isEmpty(), model.getName() + ": model name is empty");
        assertTrue(response.getTotalTokens() > 0, model.getName() + ": total tokens should be > 0");
    }
}
