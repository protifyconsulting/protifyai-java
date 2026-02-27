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

package ai.protify.core;

import ai.protify.core.internal.SupportedModel;
import ai.protify.core.provider.AIProvider;

import java.util.Objects;

public interface AIModel {

    String getName();

    AIProvider getProvider();

    // Anthropic
    AIModel CLAUDE_OPUS_4_6 = SupportedModel.CLAUDE_OPUS_4_6;
    AIModel CLAUDE_SONNET_4_6 = SupportedModel.CLAUDE_SONNET_4_6;
    AIModel CLAUDE_HAIKU_4_5 = SupportedModel.CLAUDE_HAIKU_4_5;

    // Gemini
    AIModel GEMINI_3_1_PRO_PREVIEW = SupportedModel.GEMINI_3_1_PRO_PREVIEW;
    AIModel GEMINI_3_FLASH_PREVIEW = SupportedModel.GEMINI_3_FLASH_PREVIEW;
    AIModel GEMINI_2_5_PRO = SupportedModel.GEMINI_2_5_PRO;
    AIModel GEMINI_2_5_FLASH = SupportedModel.GEMINI_2_5_FLASH;
    AIModel GEMINI_2_5_FLASH_LITE = SupportedModel.GEMINI_2_5_FLASH_LITE;

    // Open AI
    AIModel GPT_5_2 = SupportedModel.GPT_5_2;
    AIModel GPT_5_2_PRO = SupportedModel.GPT_5_2_PRO;
    AIModel GPT_5_2_CODEX = SupportedModel.GPT_5_2_CODEX;
    AIModel GPT_5_1 = SupportedModel.GPT_5_1;
    AIModel GPT_5_1_CODEX = SupportedModel.GPT_5_1_CODEX;
    AIModel GPT_5_1_CODEX_MAX = SupportedModel.GPT_5_1_CODEX_MAX;
    AIModel GPT_5_MINI = SupportedModel.GPT_5_MINI;
    AIModel GPT_5_NANO = SupportedModel.GPT_5_NANO;

    // Mistral
    AIModel MISTRAL_LARGE = SupportedModel.MISTRAL_LARGE;
    AIModel MISTRAL_MEDIUM = SupportedModel.MISTRAL_MEDIUM;
    AIModel MISTRAL_SMALL = SupportedModel.MISTRAL_SMALL;
    AIModel CODESTRAL = SupportedModel.CODESTRAL;
    AIModel DEVSTRAL = SupportedModel.DEVSTRAL;
    AIModel MAGISTRAL_MEDIUM = SupportedModel.MAGISTRAL_MEDIUM;
    AIModel MAGISTRAL_SMALL = SupportedModel.MAGISTRAL_SMALL;

    // Groq
    AIModel LLAMA_4_SCOUT = SupportedModel.LLAMA_4_SCOUT;
    AIModel LLAMA_3_3_70B = SupportedModel.LLAMA_3_3_70B;
    AIModel LLAMA_3_1_8B = SupportedModel.LLAMA_3_1_8B;
    AIModel GPT_OSS_120B = SupportedModel.GPT_OSS_120B;
    AIModel QWEN_3_32B_GROQ = SupportedModel.QWEN_3_32B_GROQ;

    // DeepSeek
    AIModel DEEPSEEK_CHAT = SupportedModel.DEEPSEEK_CHAT;
    AIModel DEEPSEEK_REASONER = SupportedModel.DEEPSEEK_REASONER;

    // Together
    AIModel LLAMA_4_MAVERICK_TOGETHER = SupportedModel.LLAMA_4_MAVERICK_TOGETHER;
    AIModel LLAMA_4_SCOUT_TOGETHER = SupportedModel.LLAMA_4_SCOUT_TOGETHER;
    AIModel QWEN_3_5_397B_TOGETHER = SupportedModel.QWEN_3_5_397B_TOGETHER;

    // Fireworks
    AIModel LLAMA_4_MAVERICK_FIREWORKS = SupportedModel.LLAMA_4_MAVERICK_FIREWORKS;
    AIModel LLAMA_4_SCOUT_FIREWORKS = SupportedModel.LLAMA_4_SCOUT_FIREWORKS;
    AIModel QWEN_3_8B_FIREWORKS = SupportedModel.QWEN_3_8B_FIREWORKS;

    // xAI
    AIModel GROK_4_1_FAST = SupportedModel.GROK_4_1_FAST;
    AIModel GROK_4_1_FAST_NON_REASONING = SupportedModel.GROK_4_1_FAST_NON_REASONING;
    AIModel GROK_4 = SupportedModel.GROK_4;
    AIModel GROK_CODE_FAST = SupportedModel.GROK_CODE_FAST;

    // Vertex AI
    AIModel GEMINI_2_5_PRO_VERTEX = SupportedModel.GEMINI_2_5_PRO_VERTEX;
    AIModel GEMINI_2_5_FLASH_VERTEX = SupportedModel.GEMINI_2_5_FLASH_VERTEX;

    // AWS Bedrock
    AIModel CLAUDE_OPUS_4_6_BEDROCK = SupportedModel.CLAUDE_OPUS_4_6_BEDROCK;
    AIModel CLAUDE_SONNET_4_6_BEDROCK = SupportedModel.CLAUDE_SONNET_4_6_BEDROCK;
    AIModel CLAUDE_HAIKU_4_5_BEDROCK = SupportedModel.CLAUDE_HAIKU_4_5_BEDROCK;

    static AIModel custom(String modelName, AIProvider provider) {
        Objects.requireNonNull(modelName, "Model name cannot be null");
        Objects.requireNonNull(provider, "Provider cannot be null");
        return new AIModel() {
            @Override public String getName() { return modelName; }
            @Override public AIProvider getProvider() { return provider; }
            @Override public String toString() {
                return "AIModel{name='" + modelName + "', provider=" + provider.getName() + "}";
            }
        };
    }
}