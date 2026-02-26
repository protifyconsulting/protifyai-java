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

package com.protify;


import com.protify.ai.AIClient;
import com.protify.ai.AIModel;
import com.protify.ai.ProtifyAI;
import com.protify.ai.internal.pipeline.PipelineAIResponse;
import com.protify.ai.pipeline.AIPipeline;
import com.protify.ai.pipeline.AIPipelineContext;
import com.protify.ai.request.AIFileInput;
import com.protify.ai.request.AIRequest;
import com.protify.ai.resiliency.RetryBackoffStrategy;
import com.protify.ai.resiliency.RetryPolicy;
import com.protify.ai.response.AIResponse;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {

        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
        Logger.getLogger("").setLevel(java.util.logging.Level.ALL);

        ProtifyAI.initialize();

        RetryPolicy policy = RetryPolicy.builder()
                .maxRetries(3)
                .backoffStrategy(RetryBackoffStrategy.EXPONENTIAL)
                .delayMillis(500L)
                .maxDelayMillis(10000L)
                .jitterMillis(200L)
                .maxElapsedTimeMillis(30000L)
                .retryOnHttpStatusCodes(Set.of(429, 500, 502, 503, 504))
                .retryOnExceptions(Set.of(RuntimeException.class))
                .respectRetryAfter(true)
                .build();

        AIClient openAIClient = AIClient.builder()
                .instructions("Talk like a pirate.")
                .model(AIModel.GPT_5_2)
                .retryPolicy(policy)
                .maxOutputTokens(1000)
                .build();

        AIClient anthropicAIClient = AIClient.builder()
                .instructions("Talk like a pirate.")
                .model(AIModel.CLAUDE_SONNET_4_5)
                .retryPolicy(policy)
                .maxOutputTokens(1000)
                .build();

        List<MovieReview> reviews = openAIClient.newRequest()
                .addInput("Review these 3 movies as a JSON array: Inception, Interstellar, Tenet. "
                        + "Each with fields: title, rating (1-10), summary.")
                .build()
                .execute()
                .asList(MovieReview.class);

        reviews.forEach(r -> System.out.println(r.getTitle() + ": " + r.getRating()));

/*
        AIPipeline pipeline = AIPipeline.builder()
                .withInitialStep(() -> openAIClient.newRequest()
                        .addInput("generate a 200 word essay about the history of the Chicago Bears")
                        .build())
                .addStep(ctx -> {
                    String essay = ctx.text();
                    String newEssay = essay.replace("Chicago Bears", "CHICAGO BEARS");
                    return PipelineAIResponse.of(newEssay);
                })
                .addParallelStep(List.of(
                        AIPipelineContext::response,  // Forward the English
                        ctx -> openAIClient.newRequest().addInput("Translate to French: " + ctx.text()).build().execute(),
                        ctx -> anthropicAIClient.newRequest().addInput("Translate to Spanish: " + ctx.text()).build().execute(),
                        ctx -> openAIClient.newRequest().addInput("Translate to German: " + ctx.text()).build().execute()
                ))
                .addRequestStep(ctx -> anthropicAIClient.newRequest()
                        .addInput("Provide one single table consisting of the number of words by each language (English, French, Spanish and German): ")
                        .addInput(ctx.response())
                        .build())
                .onStepComplete(output -> LOGGER.info("Step completed: " + output))
                        .build();

        pipeline.execute();
    }
*/
    }
}