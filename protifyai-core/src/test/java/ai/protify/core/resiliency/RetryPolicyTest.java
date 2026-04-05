package ai.protify.core.resiliency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

    // ---------------------------------------------------------------
    // 1. Default policy
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Default policy")
    class DefaultPolicy {

        @Test
        @DisplayName("Has expected default values")
        void defaultValues() {
            RetryPolicy policy = RetryPolicy.DEFAULT;

            assertEquals(0, policy.getMaxRetries());
            assertEquals(RetryBackoffStrategy.EXPONENTIAL, policy.getBackoffStrategy());
            assertEquals(500L, policy.getDelayMillis());
            assertEquals(200L, policy.getJitterMillis());
            assertEquals(10_000L, policy.getMaxDelayMillis());
            assertEquals(20_000L, policy.getMaxElapsedTimeMillis());
            assertTrue(policy.isRespectRetryAfter());
            assertTrue(policy.getRetryOnHttpStatusCodes().contains(429));
            assertTrue(policy.getRetryOnHttpStatusCodes().contains(500));
            assertTrue(policy.getRetryOnHttpStatusCodes().contains(502));
            assertTrue(policy.getRetryOnHttpStatusCodes().contains(503));
            assertTrue(policy.getRetryOnHttpStatusCodes().contains(504));
            assertTrue(policy.getRetryOnHttpStatusCodes().contains(408));
            assertTrue(policy.getRetryOnExceptions().contains(RuntimeException.class));
        }
    }

    // ---------------------------------------------------------------
    // 2. Builder with valid values
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Builder — valid configurations")
    class ValidBuilder {

        @Test
        @DisplayName("Builds with all fields specified")
        void allFieldsSpecified() {
            RetryPolicy policy = RetryPolicy.builder()
                    .maxRetries(3)
                    .backoffStrategy(RetryBackoffStrategy.FIXED)
                    .delayMillis(1000L)
                    .jitterMillis(100L)
                    .maxDelayMillis(5000L)
                    .maxElapsedTimeMillis(30_000L)
                    .retryOnHttpStatusCodes(Set.of(429, 503))
                    .retryOnExceptions(Set.of(RuntimeException.class))
                    .respectRetryAfter(false)
                    .build();

            assertEquals(3, policy.getMaxRetries());
            assertEquals(RetryBackoffStrategy.FIXED, policy.getBackoffStrategy());
            assertEquals(1000L, policy.getDelayMillis());
            assertEquals(100L, policy.getJitterMillis());
            assertEquals(5000L, policy.getMaxDelayMillis());
            assertEquals(30_000L, policy.getMaxElapsedTimeMillis());
            assertFalse(policy.isRespectRetryAfter());
            assertEquals(Set.of(429, 503), policy.getRetryOnHttpStatusCodes());
        }

        @Test
        @DisplayName("Unset fields fall back to defaults")
        void fallsBackToDefaults() {
            RetryPolicy policy = RetryPolicy.builder()
                    .maxRetries(2)
                    .build();

            assertEquals(2, policy.getMaxRetries());
            // Everything else should be default
            assertEquals(RetryBackoffStrategy.EXPONENTIAL, policy.getBackoffStrategy());
            assertEquals(500L, policy.getDelayMillis());
            assertEquals(200L, policy.getJitterMillis());
            assertTrue(policy.isRespectRetryAfter());
        }

        @Test
        @DisplayName("Zero retries requires no retry conditions")
        void zeroRetriesNoConditionsNeeded() {
            RetryPolicy policy = RetryPolicy.builder()
                    .maxRetries(0)
                    .retryOnHttpStatusCodes(Set.of())
                    .retryOnExceptions(Set.of())
                    .build();

            assertEquals(0, policy.getMaxRetries());
        }

        @Test
        @DisplayName("Max retries at upper bound")
        void maxRetriesUpperBound() {
            RetryPolicy policy = RetryPolicy.builder()
                    .maxRetries(10)
                    .build();

            assertEquals(10, policy.getMaxRetries());
        }

        @Test
        @DisplayName("Delay at upper bound")
        void delayUpperBound() {
            RetryPolicy policy = RetryPolicy.builder()
                    .delayMillis(60_000L)
                    .maxDelayMillis(60_000L)
                    .build();

            assertEquals(60_000L, policy.getDelayMillis());
        }

        @Test
        @DisplayName("Zero delay is valid")
        void zeroDelay() {
            RetryPolicy policy = RetryPolicy.builder()
                    .delayMillis(0L)
                    .jitterMillis(0L)
                    .build();

            assertEquals(0L, policy.getDelayMillis());
            assertEquals(0L, policy.getJitterMillis());
        }

        @Test
        @DisplayName("FIXED backoff strategy")
        void fixedStrategy() {
            RetryPolicy policy = RetryPolicy.builder()
                    .backoffStrategy(RetryBackoffStrategy.FIXED)
                    .build();

            assertEquals(RetryBackoffStrategy.FIXED, policy.getBackoffStrategy());
        }

        @Test
        @DisplayName("EXPONENTIAL backoff strategy")
        void exponentialStrategy() {
            RetryPolicy policy = RetryPolicy.builder()
                    .backoffStrategy(RetryBackoffStrategy.EXPONENTIAL)
                    .build();

            assertEquals(RetryBackoffStrategy.EXPONENTIAL, policy.getBackoffStrategy());
        }
    }

    // ---------------------------------------------------------------
    // 3. Builder validation — maxRetries
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Builder validation — maxRetries")
    class MaxRetriesValidation {

        @Test
        @DisplayName("Negative maxRetries throws")
        void negativeThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder().maxRetries(-1).build()
            );
        }

        @Test
        @DisplayName("maxRetries above 10 throws")
        void aboveMaxThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder().maxRetries(11).build()
            );
        }

        @Test
        @DisplayName("maxRetries > 0 with no retry conditions throws")
        void retriesWithNoConditionsThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder()
                            .maxRetries(3)
                            .retryOnHttpStatusCodes(Set.of())
                            .retryOnExceptions(Set.of())
                            .build()
            );
        }
    }

    // ---------------------------------------------------------------
    // 4. Builder validation — delay constraints
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Builder validation — delay constraints")
    class DelayValidation {

        @Test
        @DisplayName("Negative delayMillis throws")
        void negativeDelayThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder().delayMillis(-1L).build()
            );
        }

        @Test
        @DisplayName("delayMillis above 60000 throws")
        void delayAboveMaxThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder().delayMillis(60_001L).build()
            );
        }

        @Test
        @DisplayName("Negative jitterMillis throws")
        void negativeJitterThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder().jitterMillis(-1L).build()
            );
        }

        @Test
        @DisplayName("jitterMillis above 60000 throws")
        void jitterAboveMaxThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder().jitterMillis(60_001L).build()
            );
        }

        @Test
        @DisplayName("Negative maxDelayMillis throws")
        void negativeMaxDelayThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder().maxDelayMillis(-1L).build()
            );
        }

        @Test
        @DisplayName("maxDelayMillis above 300000 throws")
        void maxDelayAboveMaxThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder().maxDelayMillis(300_001L).build()
            );
        }

        @Test
        @DisplayName("Negative maxElapsedTimeMillis throws")
        void negativeElapsedThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder().maxElapsedTimeMillis(-1L).build()
            );
        }

        @Test
        @DisplayName("maxElapsedTimeMillis above 900000 throws")
        void elapsedAboveMaxThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder().maxElapsedTimeMillis(900_001L).build()
            );
        }
    }

    // ---------------------------------------------------------------
    // 5. Builder validation — relationship constraints
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Builder validation — relationship constraints")
    class RelationshipValidation {

        @Test
        @DisplayName("delayMillis > maxDelayMillis throws")
        void delayExceedsMaxDelay() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder()
                            .delayMillis(5000L)
                            .maxDelayMillis(1000L)
                            .build()
            );
        }

        @Test
        @DisplayName("jitterMillis > maxDelayMillis throws")
        void jitterExceedsMaxDelay() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder()
                            .jitterMillis(5000L)
                            .maxDelayMillis(1000L)
                            .build()
            );
        }

        @Test
        @DisplayName("maxElapsedTimeMillis < delayMillis with retries throws")
        void elapsedLessThanDelay() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder()
                            .maxRetries(2)
                            .delayMillis(5000L)
                            .maxElapsedTimeMillis(1000L)
                            .build()
            );
        }
    }

    // ---------------------------------------------------------------
    // 6. Builder validation — HTTP status codes
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Builder validation — HTTP status codes")
    class StatusCodeValidation {

        @Test
        @DisplayName("Status code below 100 throws")
        void below100Throws() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder()
                            .retryOnHttpStatusCodes(Set.of(99))
                            .build()
            );
        }

        @Test
        @DisplayName("Status code above 599 throws")
        void above599Throws() {
            assertThrows(IllegalArgumentException.class, () ->
                    RetryPolicy.builder()
                            .retryOnHttpStatusCodes(Set.of(600))
                            .build()
            );
        }

        @Test
        @DisplayName("Valid custom status codes accepted")
        void customCodesAccepted() {
            RetryPolicy policy = RetryPolicy.builder()
                    .maxRetries(1)
                    .retryOnHttpStatusCodes(Set.of(429, 500, 503))
                    .build();

            assertEquals(3, policy.getRetryOnHttpStatusCodes().size());
        }
    }

    // ---------------------------------------------------------------
    // 7. toString
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test
        @DisplayName("Includes key fields")
        void includesKeyFields() {
            RetryPolicy policy = RetryPolicy.builder()
                    .maxRetries(3)
                    .backoffStrategy(RetryBackoffStrategy.FIXED)
                    .delayMillis(1000L)
                    .build();

            String str = policy.toString();
            assertTrue(str.contains("maxRetries=3"));
            assertTrue(str.contains("FIXED"));
            assertTrue(str.contains("delayMillis=1000"));
        }
    }

    // ---------------------------------------------------------------
    // 8. Retry execution with MockProvider
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Retry policy on AIClient")
    class RetryOnClient {

        @Test
        @DisplayName("Client accepts retry policy configuration")
        void clientAcceptsRetryPolicy() {
            ai.protify.core.provider.mock.MockProvider mock =
                    ai.protify.core.provider.mock.MockProvider.withResponse("ok");

            ai.protify.core.AIClient client = ai.protify.core.AIClient.builder()
                    .model(ai.protify.core.AIModel.custom("test", mock))
                    .apiKey("key")
                    .retryPolicy(RetryPolicy.builder()
                            .maxRetries(3)
                            .backoffStrategy(RetryBackoffStrategy.EXPONENTIAL)
                            .delayMillis(100L)
                            .jitterMillis(50L)
                            .maxDelayMillis(5000L)
                            .maxElapsedTimeMillis(30_000L)
                            .retryOnHttpStatusCodes(Set.of(429, 503))
                            .retryOnExceptions(Set.of(RuntimeException.class))
                            .respectRetryAfter(true)
                            .build())
                    .build();

            // Client builds successfully with retry policy and can make requests
            ai.protify.core.response.AIResponse response = client.newRequest()
                    .addInput("hello")
                    .build()
                    .execute();

            assertEquals("ok", response.text());
        }

        @Test
        @DisplayName("Request-level retry policy overrides client-level")
        void requestLevelOverride() {
            ai.protify.core.provider.mock.MockProvider mock =
                    ai.protify.core.provider.mock.MockProvider.withResponse("ok");

            ai.protify.core.AIClient client = ai.protify.core.AIClient.builder()
                    .model(ai.protify.core.AIModel.custom("test", mock))
                    .apiKey("key")
                    .retryPolicy(RetryPolicy.builder().maxRetries(1).build())
                    .build();

            ai.protify.core.response.AIResponse response = client.newRequest()
                    .addInput("hello")
                    .retryPolicy(RetryPolicy.builder().maxRetries(5).build())
                    .build()
                    .execute();

            assertEquals("ok", response.text());
        }
    }
}
