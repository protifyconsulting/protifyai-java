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
    GPT_5_2("gpt-5.2", ProtifyAIProvider.OPEN_AI),
    GPT_5_2_PRO("gpt-5.2-pro", ProtifyAIProvider.OPEN_AI),
    GPT_5_2_CODEX("gpt-5.2-codex", ProtifyAIProvider.OPEN_AI),
    GPT_5_1("gpt-5.1", ProtifyAIProvider.OPEN_AI),
    GPT_5_1_CODEX("gpt-5.1-codex", ProtifyAIProvider.OPEN_AI),
    GPT_5_1_CODEX_MAX("gpt-5.1-codex-max", ProtifyAIProvider.OPEN_AI),
    GPT_5_MINI("gpt-5-mini", ProtifyAIProvider.OPEN_AI),
    GPT_5_NANO("gpt-5-nano", ProtifyAIProvider.OPEN_AI),

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
    LLAMA_4_SCOUT_TOGETHER("meta-llama/Llama-4-Scout-17B-16E-Instruct", ProtifyAIProvider.TOGETHER),
    QWEN_3_5_397B_TOGETHER("Qwen/Qwen3.5-397B-A17B", ProtifyAIProvider.TOGETHER),

    // Fireworks
    LLAMA_4_MAVERICK_FIREWORKS("accounts/fireworks/models/llama4-maverick-instruct-basic", ProtifyAIProvider.FIREWORKS),
    LLAMA_4_SCOUT_FIREWORKS("accounts/fireworks/models/llama4-scout-instruct-basic", ProtifyAIProvider.FIREWORKS),
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

    // AWS Bedrock
    CLAUDE_OPUS_4_6_BEDROCK("anthropic.claude-opus-4-6-v1", ProtifyAIProvider.AWS_BEDROCK),
    CLAUDE_SONNET_4_6_BEDROCK("anthropic.claude-sonnet-4-6", ProtifyAIProvider.AWS_BEDROCK),
    CLAUDE_HAIKU_4_5_BEDROCK("anthropic.claude-haiku-4-5-20251001-v1:0", ProtifyAIProvider.AWS_BEDROCK);

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
