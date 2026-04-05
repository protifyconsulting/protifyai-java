package ai.protify.core.pipeline;

import ai.protify.core.AIClient;
import ai.protify.core.AIModel;
import ai.protify.core.internal.pipeline.PipelineAIResponse;
import ai.protify.core.provider.mock.MockProvider;
import ai.protify.core.response.AIResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AIPipelineTest {

    private static AIClient clientFor(MockProvider mock) {
        return AIClient.builder()
                .model(AIModel.custom("mock-model", mock))
                .apiKey("mock-key")
                .build();
    }

    private static MockProvider mockWithQueue(String... responses) {
        MockProvider mock = MockProvider.withResponse("unused-default");
        for (String response : responses) {
            mock.enqueueResponse(response);
        }
        return mock;
    }

    // ---------------------------------------------------------------
    // 1. Conditional steps
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Conditional steps")
    class ConditionalSteps {

        @Test
        @DisplayName("Executes matching branch")
        void executesMatchingBranch() {
            MockProvider mock = mockWithQueue("initial", "branch-a");
            AIClient client = clientFor(mock);

            AIPipelineResponse result = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("start")
                            .build())
                    .addConditionalStep(cond -> cond
                            .when(ctx -> ctx.text().contains("initial"),
                                    ctx -> client.newRequest()
                                            .addInput("go to A")
                                            .build().execute())
                            .when(ctx -> ctx.text().contains("other"),
                                    ctx -> client.newRequest()
                                            .addInput("go to B")
                                            .build().execute()))
                    .build()
                    .execute();

            assertEquals("branch-a", result.text());
        }

        @Test
        @DisplayName("Executes otherwise when no branch matches")
        void executesOtherwise() {
            MockProvider mock = mockWithQueue("initial", "fallback");
            AIClient client = clientFor(mock);

            AIPipelineResponse result = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("start")
                            .build())
                    .addConditionalStep(cond -> cond
                            .when(ctx -> ctx.text().contains("xyz"),
                                    ctx -> client.newRequest()
                                            .addInput("should not run")
                                            .build().execute())
                            .otherwise(ctx -> client.newRequest()
                                    .addInput("fallback path")
                                    .build().execute()))
                    .build()
                    .execute();

            assertEquals("fallback", result.text());
        }

        @Test
        @DisplayName("Returns previous response when no branch matches and no otherwise")
        void returnsPreviousWhenNoMatch() {
            MockProvider mock = MockProvider.withResponse("initial");
            AIClient client = clientFor(mock);

            AIPipelineResponse result = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("start")
                            .build())
                    .addConditionalStep(cond -> cond
                            .when(ctx -> false,
                                    ctx -> client.newRequest()
                                            .addInput("never")
                                            .build().execute()))
                    .build()
                    .execute();

            assertEquals("initial", result.text());
        }
    }

    // ---------------------------------------------------------------
    // 2. Loop steps
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Loop steps")
    class LoopSteps {

        @Test
        @DisplayName("Loops until condition is met")
        void loopsUntilCondition() {
            AtomicInteger counter = new AtomicInteger(0);
            MockProvider mock = mockWithQueue("start", "attempt-1", "attempt-2", "done");
            AIClient client = clientFor(mock);

            AIPipelineResponse result = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("start")
                            .build())
                    .addLoopStep(loop -> loop
                            .step(ctx -> {
                                counter.incrementAndGet();
                                return client.newRequest()
                                        .addInput("try again")
                                        .build().execute();
                            })
                            .until(ctx -> ctx.text().contains("done"))
                            .maxIterations(5))
                    .build()
                    .execute();

            assertEquals("done", result.text());
            assertEquals(3, counter.get());
        }

        @Test
        @DisplayName("Stops at max iterations and calls onMaxIterations")
        void stopsAtMaxIterations() {
            AtomicInteger counter = new AtomicInteger(0);
            MockProvider mock = mockWithQueue("start", "loop-1", "loop-2", "fallback");
            AIClient client = clientFor(mock);

            AIPipelineResponse result = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("start")
                            .build())
                    .addLoopStep(loop -> loop
                            .step(ctx -> {
                                counter.incrementAndGet();
                                return client.newRequest()
                                        .addInput("retry")
                                        .build().execute();
                            })
                            .until(ctx -> ctx.text().contains("never-matches"))
                            .maxIterations(2)
                            .onMaxIterations(ctx -> client.newRequest()
                                    .addInput("max reached")
                                    .build().execute()))
                    .build()
                    .execute();

            assertEquals("fallback", result.text());
            assertEquals(2, counter.get());
        }

        @Test
        @DisplayName("Fails to build without step")
        void failsWithoutStep() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIClient client = clientFor(mock);

            assertThrows(IllegalStateException.class, () ->
                    AIPipeline.builder()
                            .withInitialStep(() -> client.newRequest()
                                    .addInput("start")
                                    .build())
                            .addLoopStep(loop -> loop
                                    .until(ctx -> true)
                                    .maxIterations(3))
                            .build()
            );
        }

        @Test
        @DisplayName("Fails to build without until condition")
        void failsWithoutUntil() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIClient client = clientFor(mock);

            assertThrows(IllegalStateException.class, () ->
                    AIPipeline.builder()
                            .withInitialStep(() -> client.newRequest()
                                    .addInput("start")
                                    .build())
                            .addLoopStep(loop -> loop
                                    .step(ctx -> client.newRequest()
                                            .addInput("loop")
                                            .build().execute())
                                    .maxIterations(3))
                            .build()
            );
        }
    }

    // ---------------------------------------------------------------
    // 3. Safe steps (error handling)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Safe steps")
    class SafeSteps {

        @Test
        @DisplayName("Passes through on success")
        void passesThroughOnSuccess() {
            MockProvider mock = mockWithQueue("start", "safe-result");
            AIClient client = clientFor(mock);

            AIPipelineResponse result = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("start")
                            .build())
                    .addSafeStep(safe -> safe
                            .step(ctx -> client.newRequest()
                                    .addInput("do work")
                                    .build().execute())
                            .onError((ctx, ex) -> client.newRequest()
                                    .addInput("error fallback")
                                    .build().execute()))
                    .build()
                    .execute();

            assertEquals("safe-result", result.text());
        }

        @Test
        @DisplayName("Catches exception and calls onError")
        void catchesExceptionAndCallsOnError() {
            MockProvider mock = mockWithQueue("start", "recovered");
            AIClient client = clientFor(mock);

            AIPipelineResponse result = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("start")
                            .build())
                    .addSafeStep(safe -> safe
                            .step(ctx -> { throw new RuntimeException("step failed"); })
                            .onError((ctx, ex) -> client.newRequest()
                                    .addInput("recover")
                                    .build().execute()))
                    .build()
                    .execute();

            assertEquals("recovered", result.text());
        }

        @Test
        @DisplayName("Retries before calling onError")
        void retriesBeforeError() {
            AtomicInteger attempts = new AtomicInteger(0);
            MockProvider mock = mockWithQueue("start", "recovered");
            AIClient client = clientFor(mock);

            AIPipelineResponse result = AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("start")
                            .build())
                    .addSafeStep(safe -> safe
                            .step(ctx -> {
                                attempts.incrementAndGet();
                                throw new RuntimeException("always fails");
                            })
                            .maxRetries(2)
                            .onError((ctx, ex) -> client.newRequest()
                                    .addInput("final fallback")
                                    .build().execute()))
                    .build()
                    .execute();

            assertEquals("recovered", result.text());
            assertEquals(3, attempts.get()); // 1 initial + 2 retries
        }

        @Test
        @DisplayName("Throws when no onError and step fails")
        void throwsWhenNoOnError() {
            MockProvider mock = MockProvider.withResponse("start");
            AIClient client = clientFor(mock);

            assertThrows(RuntimeException.class, () ->
                    AIPipeline.builder()
                            .withInitialStep(() -> client.newRequest()
                                    .addInput("start")
                                    .build())
                            .addSafeStep(safe -> safe
                                    .step(ctx -> { throw new RuntimeException("no recovery"); }))
                            .build()
                            .execute()
            );
        }

        @Test
        @DisplayName("Fails to build without step")
        void failsWithoutStep() {
            MockProvider mock = MockProvider.withResponse("ok");
            AIClient client = clientFor(mock);

            assertThrows(IllegalStateException.class, () ->
                    AIPipeline.builder()
                            .withInitialStep(() -> client.newRequest()
                                    .addInput("start")
                                    .build())
                            .addSafeStep(safe -> safe
                                    .maxRetries(3))
                            .build()
            );
        }
    }

    // ---------------------------------------------------------------
    // 4. Parallel steps
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Parallel steps")
    class ParallelSteps {

        @Test
        @DisplayName("Executes steps in parallel and joins results")
        void executesInParallel() {
            MockProvider mock1 = MockProvider.withResponse("result-a");
            MockProvider mock2 = MockProvider.withResponse("result-b");
            AIClient client1 = clientFor(mock1);
            AIClient client2 = clientFor(mock2);

            MockProvider initialMock = MockProvider.withResponse("initial");
            AIClient initialClient = clientFor(initialMock);

            AIPipelineResponse result = AIPipeline.builder()
                    .withInitialStep(() -> initialClient.newRequest()
                            .addInput("start")
                            .build())
                    .addParallelStep(List.of(
                            ctx -> client1.newRequest()
                                    .addInput("parallel-a")
                                    .build().execute(),
                            ctx -> client2.newRequest()
                                    .addInput("parallel-b")
                                    .build().execute()
                    ))
                    .build()
                    .execute();

            String text = result.text();
            assertTrue(text.contains("result-a"));
            assertTrue(text.contains("result-b"));
        }
    }

    // ---------------------------------------------------------------
    // 5. onStepComplete callback
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Step callbacks")
    class StepCallbacks {

        @Test
        @DisplayName("onStepComplete fires for each step")
        void callbackFiresForEachStep() {
            MockProvider mock = mockWithQueue("step-1", "step-2", "step-3");
            AIClient client = clientFor(mock);
            List<String> callbacks = new ArrayList<>();

            AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("first")
                            .build())
                    .addStep(ctx -> client.newRequest()
                            .addInput("second")
                            .build().execute())
                    .addStep(ctx -> client.newRequest()
                            .addInput("third")
                            .build().execute())
                    .onStepComplete(text -> callbacks.add(text))
                    .build()
                    .execute();

            assertEquals(3, callbacks.size());
            assertEquals("step-1", callbacks.get(0));
            assertEquals("step-2", callbacks.get(1));
            assertEquals("step-3", callbacks.get(2));
        }
    }

    // ---------------------------------------------------------------
    // 6. Custom context properties
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Pipeline context")
    class PipelineContextTest {

        @Test
        @DisplayName("Custom properties are accessible across steps")
        void customPropertiesAcrossSteps() {
            MockProvider mock = mockWithQueue("start", "end");
            AIClient client = clientFor(mock);
            List<String> captured = new ArrayList<>();

            AIPipeline.builder()
                    .withInitialStep(() -> client.newRequest()
                            .addInput("init")
                            .build())
                    .addStep(ctx -> {
                        ctx.addCustomProperty("myKey", "myValue");
                        return client.newRequest()
                                .addInput("set property")
                                .build().execute();
                    })
                    .addStep(ctx -> {
                        captured.add((String) ctx.getCustomProperty("myKey"));
                        return ctx.response();
                    })
                    .build()
                    .execute();

            assertEquals(1, captured.size());
            assertEquals("myValue", captured.get(0));
        }
    }
}
