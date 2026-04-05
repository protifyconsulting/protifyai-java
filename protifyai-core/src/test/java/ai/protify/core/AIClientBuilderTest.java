package ai.protify.core;

import ai.protify.core.internal.provider.ProtifyAIProvider;
import ai.protify.core.provider.mock.MockProvider;
import ai.protify.core.resiliency.RetryPolicy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AIClientBuilderTest {

    @Nested
    @DisplayName("Model and provider resolution")
    class ModelResolution {

        @Test
        @DisplayName("Build with AIModel constant")
        void buildWithModel() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIClient client = AIClient.builder()
                    .model(AIModel.custom("test-model", mock))
                    .apiKey("key")
                    .build();
            assertNotNull(client);
        }

        @Test
        @DisplayName("Build with explicitModelVersion and provider")
        void buildWithExplicitVersion() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIClient client = AIClient.builder()
                    .provider(mock)
                    .explicitModelVersion("my-custom-model")
                    .apiKey("key")
                    .build();
            assertNotNull(client);
        }

        @Test
        @DisplayName("Fails when no model and no provider")
        void failsWithNoModelNoProvider() {
            assertThrows(IllegalArgumentException.class, () ->
                    AIClient.builder().apiKey("key").build()
            );
        }

        @Test
        @DisplayName("Fails when explicitModelVersion set but no provider")
        void failsWithExplicitVersionNoProvider() {
            assertThrows(IllegalArgumentException.class, () ->
                    AIClient.builder()
                            .explicitModelVersion("gpt-5.4")
                            .apiKey("key")
                            .build()
            );
        }

        @Test
        @DisplayName("Fails when provider set but no model version")
        void failsWithProviderNoVersion() {
            MockProvider mock = MockProvider.withResponse("ok");
            assertThrows(IllegalArgumentException.class, () ->
                    AIClient.builder()
                            .provider(mock)
                            .apiKey("key")
                            .build()
            );
        }

        @Test
        @DisplayName("AIModel takes precedence over explicitModelVersion")
        void modelTakesPrecedence() {
            MockProvider mock = MockProvider.withResponse("hello");
            AIClient client = AIClient.builder()
                    .model(AIModel.custom("model-from-aimodel", mock))
                    .explicitModelVersion("should-be-ignored")
                    .apiKey("key")
                    .build();
            // Should build without error — explicitModelVersion is ignored with a warning
            assertNotNull(client);
        }
    }

    @Nested
    @DisplayName("Configuration methods")
    class Configuration {

        private AIClient buildClient() {
            MockProvider mock = MockProvider.withResponse("ok");
            return AIClient.builder()
                    .model(AIModel.custom("test", mock))
                    .apiKey("key")
                    .instructions("Be helpful")
                    .temperature(0.7)
                    .topP(0.9)
                    .topK(40)
                    .maxOutputTokens(500)
                    .reasoningEffort(ReasoningEffort.MEDIUM)
                    .logRequests(true)
                    .logResponses(true)
                    .prettyPrint(true)
                    .truncateLongRequestInputs(true)
                    .overridePipelineConfig(false)
                    .retryPolicy(RetryPolicy.builder().maxRetries(3).build())
                    .build();
        }

        @Test
        @DisplayName("All configuration methods chain correctly")
        void allConfigMethodsChain() {
            AIClient client = buildClient();
            assertNotNull(client);
        }

        @Test
        @DisplayName("Null reasoning effort throws NullPointerException")
        void nullReasoningEffortThrows() {
            MockProvider mock = MockProvider.withResponse("ok");
            assertThrows(NullPointerException.class, () ->
                    AIClient.builder()
                            .model(AIModel.custom("test", mock))
                            .reasoningEffort(null)
            );
        }

        @Test
        @DisplayName("Cloud provider configuration methods chain correctly")
        void cloudProviderConfigMethods() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIClient client = AIClient.builder()
                    .model(AIModel.custom("test", mock))
                    .apiKey("key")
                    .region("us-east-1")
                    .projectId("my-project")
                    .resourceName("my-resource")
                    .deploymentName("my-deployment")
                    .apiVersion("2024-01-01")
                    .awsAccessKeyId("AKIA...")
                    .awsSecretAccessKey("secret")
                    .awsSessionToken("session")
                    .build();
            assertNotNull(client);
        }
    }

    @Nested
    @DisplayName("AIModel.custom()")
    class CustomModel {

        @Test
        @DisplayName("Creates custom model with name and provider")
        void customModelCreation() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIModel model = AIModel.custom("my-model-v2", mock);
            assertEquals("my-model-v2", model.getName());
            assertSame(mock, model.getProvider());
        }

        @Test
        @DisplayName("Custom model toString includes name and provider")
        void customModelToString() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIModel model = AIModel.custom("my-model", mock);
            String str = model.toString();
            assertTrue(str.contains("my-model"));
            assertTrue(str.contains("Mock"));
        }

        @Test
        @DisplayName("Null model name throws NullPointerException")
        void nullModelNameThrows() {
            MockProvider mock = MockProvider.withResponse("ok");
            assertThrows(NullPointerException.class, () ->
                    AIModel.custom(null, mock)
            );
        }

        @Test
        @DisplayName("Null provider throws NullPointerException")
        void nullProviderThrows() {
            assertThrows(NullPointerException.class, () ->
                    AIModel.custom("model", null)
            );
        }
    }
}
