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

package ai.protify.core.internal;

import ai.protify.core.AIModel;
import ai.protify.core.internal.provider.ProtifyAIProvider;
import ai.protify.core.provider.AIProvider;


public enum SupportedModel implements AIModel {

    // Anthropic
    CLAUDE_OPUS_4_6("claude-opus-4-6", ProtifyAIProvider.ANTHROPIC),
    CLAUDE_SONNET_4_6("claude-sonnet-4-6", ProtifyAIProvider.ANTHROPIC),
    CLAUDE_HAIKU_4_5("claude-haiku-4-5", ProtifyAIProvider.ANTHROPIC),

    // Gemini
    GEMINI_3_1_PRO_PREVIEW("gemini-3.1-pro-preview", ProtifyAIProvider.GOOGLE),
    GEMINI_3_FLASH_PREVIEW("gemini-3-flash-preview", ProtifyAIProvider.GOOGLE),
    GEMINI_2_5_PRO("gemini-2.5-pro", ProtifyAIProvider.GOOGLE),
    GEMINI_2_5_FLASH("gemini-2.5-flash", ProtifyAIProvider.GOOGLE),
    GEMINI_2_5_FLASH_LITE("gemini-2.5-flash-lite", ProtifyAIProvider.GOOGLE),

    // Open AI
    GPT_5_4("gpt-5.4", ProtifyAIProvider.OPEN_AI),
    GPT_5_4_PRO("gpt-5.4-pro", ProtifyAIProvider.OPEN_AI),
    GPT_5_2("gpt-5.2", ProtifyAIProvider.OPEN_AI),
    GPT_5_2_PRO("gpt-5.2-pro", ProtifyAIProvider.OPEN_AI),
    GPT_5_2_CODEX("gpt-5.2-codex", ProtifyAIProvider.OPEN_AI),
    GPT_5_1("gpt-5.1", ProtifyAIProvider.OPEN_AI),
    GPT_5_1_CODEX("gpt-5.1-codex", ProtifyAIProvider.OPEN_AI),
    GPT_5_1_CODEX_MAX("gpt-5.1-codex-max", ProtifyAIProvider.OPEN_AI),
    GPT_5_MINI("gpt-5-mini", ProtifyAIProvider.OPEN_AI),
    GPT_5_NANO("gpt-5-nano", ProtifyAIProvider.OPEN_AI),
    GPT_4_1("gpt-4.1", ProtifyAIProvider.OPEN_AI),

    // Mistral
    MISTRAL_LARGE("mistral-large-latest", ProtifyAIProvider.MISTRAL),
    MISTRAL_MEDIUM("mistral-medium-latest", ProtifyAIProvider.MISTRAL),
    MISTRAL_SMALL("mistral-small-latest", ProtifyAIProvider.MISTRAL),
    CODESTRAL("codestral-latest", ProtifyAIProvider.MISTRAL),
    DEVSTRAL("devstral-latest", ProtifyAIProvider.MISTRAL),
    MAGISTRAL_MEDIUM("magistral-medium-latest", ProtifyAIProvider.MISTRAL),
    MAGISTRAL_SMALL("magistral-small-latest", ProtifyAIProvider.MISTRAL),

    // Groq
    LLAMA_4_SCOUT("meta-llama/llama-4-scout-17b-16e-instruct", ProtifyAIProvider.GROQ),
    LLAMA_3_3_70B("llama-3.3-70b-versatile", ProtifyAIProvider.GROQ),
    LLAMA_3_1_8B("llama-3.1-8b-instant", ProtifyAIProvider.GROQ),
    GPT_OSS_120B("openai/gpt-oss-120b", ProtifyAIProvider.GROQ),
    QWEN_3_32B_GROQ("qwen/qwen3-32b", ProtifyAIProvider.GROQ),

    // DeepSeek
    DEEPSEEK_CHAT("deepseek-chat", ProtifyAIProvider.DEEP_SEEK),
    DEEPSEEK_REASONER("deepseek-reasoner", ProtifyAIProvider.DEEP_SEEK),

    // Together
    LLAMA_4_MAVERICK_TOGETHER("meta-llama/Llama-4-Maverick-17B-128E-Instruct-FP8", ProtifyAIProvider.TOGETHER),
    LLAMA_3_3_70B_TOGETHER("meta-llama/Llama-3.3-70B-Instruct-Turbo", ProtifyAIProvider.TOGETHER),
    DEEPSEEK_V3_1_TOGETHER("deepseek-ai/DeepSeek-V3.1", ProtifyAIProvider.TOGETHER),

    // Fireworks
    LLAMA_3_3_70B_FIREWORKS("accounts/fireworks/models/llama-v3p3-70b-instruct", ProtifyAIProvider.FIREWORKS),
    DEEPSEEK_V3_FIREWORKS("accounts/fireworks/models/deepseek-v3p1", ProtifyAIProvider.FIREWORKS),
    QWEN_3_8B_FIREWORKS("accounts/fireworks/models/qwen3-8b", ProtifyAIProvider.FIREWORKS),

    // xAI
    GROK_4_1_FAST("grok-4-1-fast-reasoning", ProtifyAIProvider.X_AI),
    GROK_4_1_FAST_NON_REASONING("grok-4-1-fast-non-reasoning", ProtifyAIProvider.X_AI),
    GROK_4("grok-4", ProtifyAIProvider.X_AI),
    GROK_3_MINI("grok-3-mini", ProtifyAIProvider.X_AI),
    GROK_CODE_FAST("grok-code-fast", ProtifyAIProvider.X_AI),

    // Vertex AI
    GEMINI_2_5_PRO_VERTEX("gemini-2.5-pro", ProtifyAIProvider.VERTEX_AI),
    GEMINI_2_5_FLASH_VERTEX("gemini-2.5-flash", ProtifyAIProvider.VERTEX_AI),

    // Azure OpenAI
    GPT_5_2_AZURE("gpt-5.2", ProtifyAIProvider.AZURE_OPEN_AI),
    GPT_5_1_AZURE("gpt-5.1", ProtifyAIProvider.AZURE_OPEN_AI),
    GPT_5_MINI_AZURE("gpt-5-mini", ProtifyAIProvider.AZURE_OPEN_AI),
    GPT_5_NANO_AZURE("gpt-5-nano", ProtifyAIProvider.AZURE_OPEN_AI),
    GPT_4_1_AZURE("gpt-4.1", ProtifyAIProvider.AZURE_OPEN_AI),
    GPT_4O_AZURE("gpt-4o", ProtifyAIProvider.AZURE_OPEN_AI),
    GPT_4O_MINI_AZURE("gpt-4o-mini", ProtifyAIProvider.AZURE_OPEN_AI),
    O3_AZURE("o3", ProtifyAIProvider.AZURE_OPEN_AI),
    O3_MINI_AZURE("o3-mini", ProtifyAIProvider.AZURE_OPEN_AI),
    O4_MINI_AZURE("o4-mini", ProtifyAIProvider.AZURE_OPEN_AI),

    // Azure AI Foundry
    CLAUDE_SONNET_4_6_FOUNDRY("claude-sonnet-4-6", ProtifyAIProvider.AZURE_AI_FOUNDRY),
    CLAUDE_HAIKU_4_5_FOUNDRY("claude-haiku-4-5", ProtifyAIProvider.AZURE_AI_FOUNDRY),
    GPT_5_2_FOUNDRY("gpt-5.2", ProtifyAIProvider.AZURE_AI_FOUNDRY),
    GPT_5_1_FOUNDRY("gpt-5.1", ProtifyAIProvider.AZURE_AI_FOUNDRY),
    GPT_5_MINI_FOUNDRY("gpt-5-mini", ProtifyAIProvider.AZURE_AI_FOUNDRY),
    GPT_4O_FOUNDRY("gpt-4o", ProtifyAIProvider.AZURE_AI_FOUNDRY),
    GPT_4O_MINI_FOUNDRY("gpt-4o-mini", ProtifyAIProvider.AZURE_AI_FOUNDRY),
    MISTRAL_LARGE_FOUNDRY("mistral-large-latest", ProtifyAIProvider.AZURE_AI_FOUNDRY),
    MISTRAL_SMALL_FOUNDRY("mistral-small-latest", ProtifyAIProvider.AZURE_AI_FOUNDRY),
    LLAMA_3_3_70B_FOUNDRY("Meta-Llama-3.3-70B-Instruct", ProtifyAIProvider.AZURE_AI_FOUNDRY),
    LLAMA_4_SCOUT_FOUNDRY("Meta-Llama-4-Scout-17B-16E-Instruct", ProtifyAIProvider.AZURE_AI_FOUNDRY),
    LLAMA_4_MAVERICK_FOUNDRY("Meta-Llama-4-Maverick-17B-128E-Instruct-FP8", ProtifyAIProvider.AZURE_AI_FOUNDRY),

    // AWS Bedrock
    CLAUDE_OPUS_4_6_BEDROCK("anthropic.claude-opus-4-6-v1", ProtifyAIProvider.AWS_BEDROCK),
    CLAUDE_SONNET_4_6_BEDROCK("anthropic.claude-sonnet-4-6", ProtifyAIProvider.AWS_BEDROCK),
    CLAUDE_HAIKU_4_5_BEDROCK("anthropic.claude-haiku-4-5-20251001-v1:0", ProtifyAIProvider.AWS_BEDROCK),
    LLAMA_4_MAVERICK_BEDROCK("meta.llama4-maverick-17b-instruct-v1:0", ProtifyAIProvider.AWS_BEDROCK),
    LLAMA_4_SCOUT_BEDROCK("meta.llama4-scout-17b-instruct-v1:0", ProtifyAIProvider.AWS_BEDROCK),
    LLAMA_3_3_70B_BEDROCK("meta.llama3-3-70b-instruct-v1:0", ProtifyAIProvider.AWS_BEDROCK),
    MISTRAL_LARGE_BEDROCK("mistral.mistral-large-2411-v1:0", ProtifyAIProvider.AWS_BEDROCK),
    MISTRAL_SMALL_BEDROCK("mistral.mistral-small-2503-v1:0", ProtifyAIProvider.AWS_BEDROCK),
    AMAZON_NOVA_PRO_BEDROCK("amazon.nova-pro-v1:0", ProtifyAIProvider.AWS_BEDROCK),
    AMAZON_NOVA_LITE_BEDROCK("amazon.nova-lite-v1:0", ProtifyAIProvider.AWS_BEDROCK),
    AMAZON_NOVA_MICRO_BEDROCK("amazon.nova-micro-v1:0", ProtifyAIProvider.AWS_BEDROCK),
    COHERE_COMMAND_R_PLUS_BEDROCK("cohere.command-r-plus-v1:0", ProtifyAIProvider.AWS_BEDROCK),
    COHERE_COMMAND_R_BEDROCK("cohere.command-r-v1:0", ProtifyAIProvider.AWS_BEDROCK);

    private final String name;
    private final AIProvider provider;

    SupportedModel(String name, AIProvider provider) {
        this.name = name;
        this.provider = provider;
    }

    public String getName() {
        return name;
    }

    public AIProvider getProvider() {
        return provider;
    }

    @Override
    public String toString() {
        return "SupportedModel{" +
                "name='" + name + '\'' +
                ", provider=" + provider +
                '}';
    }
}
