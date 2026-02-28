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

package ai.protify.core.internal.provider;

import ai.protify.core.AIClient;
import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.config.Configuration;
import ai.protify.core.internal.provider.anthropic.AnthropicRequest;
import ai.protify.core.internal.provider.bedrock.BedrockRequest;
import ai.protify.core.internal.provider.chatcompletions.ChatCompletionsRequest;
import ai.protify.core.internal.provider.openai.OpenAIRequest;
import ai.protify.core.message.AIMessage;
import ai.protify.core.provider.AIProvider;
import ai.protify.core.provider.AIProviderClient;
import ai.protify.core.request.AIInput;
import ai.protify.core.request.AIRequest;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;
import ai.protify.core.pipeline.AIPipelineContext;
import ai.protify.core.tool.AITool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class UnsupportedParameterWarningTest {

    @Test
    @DisplayName("OpenAI + topK set -> topK excluded from JSON, warning logged")
    void openAI_topK_excluded() {
        Map<AIConfigProperty, Object> props = createBaseProps();
        props.put(AIConfigProperty.TOP_K, 40);

        List<LogRecord> logs = new ArrayList<>();
        Handler handler = captureWarnings(logs);

        try {
            OpenAIRequest request = new OpenAIRequest();
            request.initialize(
                    stubRequest("gpt-5-nano", ProtifyAIProvider.OPEN_AI),
                    new Configuration(props));

            assertNull(request.getConfiguration().getProperty(AIConfigProperty.TOP_K),
                    "topK should be suppressed for OpenAI");
            assertTrue(logs.stream().anyMatch(r -> r.getMessage().contains("topK")),
                    "Warning should mention topK");
        } finally {
            removeHandler(handler);
        }
    }

    @Test
    @DisplayName("OpenAI + GPT-5 model + temperature set -> temperature excluded, warning logged")
    void openAI_gpt5_temperature_excluded() {
        Map<AIConfigProperty, Object> props = createBaseProps();
        props.put(AIConfigProperty.TEMPERATURE, 0.7);

        List<LogRecord> logs = new ArrayList<>();
        Handler handler = captureWarnings(logs);

        try {
            OpenAIRequest request = new OpenAIRequest();
            request.initialize(
                    stubRequest("gpt-5-nano", ProtifyAIProvider.OPEN_AI),
                    new Configuration(props));

            assertNull(request.getConfiguration().getProperty(AIConfigProperty.TEMPERATURE),
                    "temperature should be suppressed for GPT-5 models");
            assertTrue(logs.stream().anyMatch(r -> r.getMessage().contains("temperature")),
                    "Warning should mention temperature");
        } finally {
            removeHandler(handler);
        }
    }

    @Test
    @DisplayName("OpenAI + non-GPT-5 model + temperature set -> temperature included, no warning")
    void openAI_nonGpt5_temperature_included() {
        Map<AIConfigProperty, Object> props = createBaseProps();
        props.put(AIConfigProperty.TEMPERATURE, 0.7);

        List<LogRecord> logs = new ArrayList<>();
        Handler handler = captureWarnings(logs);

        try {
            OpenAIRequest request = new OpenAIRequest();
            request.initialize(
                    stubRequest("o3-mini", ProtifyAIProvider.OPEN_AI),
                    new Configuration(props));

            assertEquals(0.7, (Double) request.getConfiguration().getProperty(AIConfigProperty.TEMPERATURE),
                    "temperature should be present for non-GPT-5 models");
            assertTrue(logs.stream().noneMatch(r -> r.getMessage().contains("temperature")),
                    "No warning should be logged for temperature");
        } finally {
            removeHandler(handler);
        }
    }

    @Test
    @DisplayName("Anthropic + all params set -> all included, no warnings")
    void anthropic_allParams_included() {
        Map<AIConfigProperty, Object> props = createBaseProps();
        props.put(AIConfigProperty.TEMPERATURE, 0.7);
        props.put(AIConfigProperty.TOP_P, 0.9);
        props.put(AIConfigProperty.TOP_K, 40);
        props.put(AIConfigProperty.MAX_OUTPUT_TOKENS, 1024);
        props.put(AIConfigProperty.INSTRUCTIONS, "Be helpful");

        List<LogRecord> logs = new ArrayList<>();
        Handler handler = captureWarnings(logs);

        try {
            AnthropicRequest request = new AnthropicRequest();
            request.initialize(
                    stubRequest("claude-sonnet-4-6", ProtifyAIProvider.ANTHROPIC),
                    new Configuration(props));

            assertNotNull(request.getConfiguration().getProperty(AIConfigProperty.TEMPERATURE));
            assertNotNull(request.getConfiguration().getProperty(AIConfigProperty.TOP_P));
            assertNotNull(request.getConfiguration().getProperty(AIConfigProperty.TOP_K));
            assertNotNull(request.getConfiguration().getProperty(AIConfigProperty.MAX_OUTPUT_TOKENS));
            assertNotNull(request.getConfiguration().getProperty(AIConfigProperty.INSTRUCTIONS));
            assertTrue(logs.isEmpty(), "No warnings should be logged for Anthropic");
        } finally {
            removeHandler(handler);
        }
    }

    @Test
    @DisplayName("ChatCompletions + topK set -> topK excluded, warning logged")
    void chatCompletions_topK_excluded() {
        Map<AIConfigProperty, Object> props = createBaseProps();
        props.put(AIConfigProperty.TOP_K, 40);

        List<LogRecord> logs = new ArrayList<>();
        Handler handler = captureWarnings(logs);

        try {
            ChatCompletionsRequest request = new ChatCompletionsRequest();
            request.initialize(
                    stubRequest("mistral-large-latest", ProtifyAIProvider.MISTRAL),
                    new Configuration(props));

            assertNull(request.getConfiguration().getProperty(AIConfigProperty.TOP_K),
                    "topK should be suppressed for ChatCompletions");
            assertTrue(logs.stream().anyMatch(r -> r.getMessage().contains("topK")),
                    "Warning should mention topK");
        } finally {
            removeHandler(handler);
        }
    }

    @Test
    @DisplayName("Bedrock + topK set -> topK excluded, warning logged")
    void bedrock_topK_excluded() {
        Map<AIConfigProperty, Object> props = createBaseProps();
        props.put(AIConfigProperty.TOP_K, 40);

        List<LogRecord> logs = new ArrayList<>();
        Handler handler = captureWarnings(logs);

        try {
            BedrockRequest request = new BedrockRequest();
            request.initialize(
                    stubRequest("anthropic.claude-sonnet-4-6", ProtifyAIProvider.AWS_BEDROCK),
                    new Configuration(props));

            assertNull(request.getConfiguration().getProperty(AIConfigProperty.TOP_K),
                    "topK should be suppressed for Bedrock");
            assertTrue(logs.stream().anyMatch(r -> r.getMessage().contains("topK")),
                    "Warning should mention topK");
        } finally {
            removeHandler(handler);
        }
    }

    // --- helpers ---

    private static Map<AIConfigProperty, Object> createBaseProps() {
        Map<AIConfigProperty, Object> props = new EnumMap<>(AIConfigProperty.class);
        props.put(AIConfigProperty.PRETTY_PRINT_JSON, false);
        props.put(AIConfigProperty.LOG_REQUESTS, false);
        props.put(AIConfigProperty.LOG_RESPONSES, false);
        return props;
    }

    private static Handler captureWarnings(List<LogRecord> sink) {
        Logger logger = Logger.getLogger("ai.protify.core.provider.ProtifyAIProviderRequest");
        logger.setLevel(Level.ALL);
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    sink.add(record);
                }
            }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(handler);
        return handler;
    }

    private static void removeHandler(Handler handler) {
        Logger logger = Logger.getLogger("ai.protify.core.provider.ProtifyAIProviderRequest");
        logger.removeHandler(handler);
    }

    private static AIRequest stubRequest(String modelName, AIProvider provider) {
        return new AIRequest() {
            private final AIClient client = new AIClient() {
                @Override public String getModelName() { return modelName; }
                @Override public AIProvider getProvider() { return provider; }
                @Override public AIProviderClient<?> getProviderClient() { return null; }
                @Override public Configuration getConfiguration() { return null; }
                @Override public ai.protify.core.request.AIRequestBuilder newRequest() { return null; }
                @Override public ai.protify.core.conversation.AIConversationBuilder newConversation() { return null; }
                @Override public ai.protify.core.conversation.AIConversation loadConversation(String id, ai.protify.core.conversation.AIConversationStore store) { return null; }
            };

            @Override public AIResponse execute() { return null; }
            @Override public AIResponse execute(AIPipelineContext ctx) { return null; }
            @Override public CompletableFuture<AIResponse> executeAsync() { return null; }
            @Override public CompletableFuture<AIResponse> executeAsync(AIPipelineContext ctx) { return null; }
            @Override public AIStreamResponse executeStream() { return null; }
            @Override public AIStreamResponse executeStream(AIPipelineContext ctx) { return null; }
            @Override public String toJson() { return null; }
            @Override public String toLoggableJson() { return null; }
            @Override public List<AIInput> getInputs() { return Collections.emptyList(); }
            @Override public AIClient getClient() { return client; }
            @Override public AIProviderClient<?> getProviderClient() { return null; }
            @Override public Configuration getConfiguration() { return null; }
            @Override public List<AITool> getTools() { return Collections.emptyList(); }
            @Override public List<AIMessage> getMessages() { return Collections.emptyList(); }
        };
    }
}
