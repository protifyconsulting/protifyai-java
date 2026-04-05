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
    AIModel GEMINI_2_5_PRO = SupportedModel.GEMINI_2_5_PRO;
    AIModel GEMINI_2_5_FLASH = SupportedModel.GEMINI_2_5_FLASH;

    // Open AI
    AIModel GPT_5_4 = SupportedModel.GPT_5_4;
    AIModel GPT_5_4_MINI = SupportedModel.GPT_5_4_MINI;
    AIModel O3 = SupportedModel.O3;
    AIModel O4_MINI = SupportedModel.O4_MINI;

    // Mistral
    AIModel MISTRAL_LARGE = SupportedModel.MISTRAL_LARGE;
    AIModel MISTRAL_SMALL = SupportedModel.MISTRAL_SMALL;
    AIModel CODESTRAL = SupportedModel.CODESTRAL;

    // Groq
    AIModel LLAMA_4_SCOUT = SupportedModel.LLAMA_4_SCOUT;
    AIModel LLAMA_3_3_70B = SupportedModel.LLAMA_3_3_70B;

    // DeepSeek
    AIModel DEEPSEEK_CHAT = SupportedModel.DEEPSEEK_CHAT;
    AIModel DEEPSEEK_REASONER = SupportedModel.DEEPSEEK_REASONER;

    // Fireworks
    AIModel LLAMA_3_3_70B_FIREWORKS = SupportedModel.LLAMA_3_3_70B_FIREWORKS;

    // xAI
    AIModel GROK_4_20 = SupportedModel.GROK_4_20;
    AIModel GROK_4 = SupportedModel.GROK_4;

    // Azure OpenAI
    AIModel GPT_5_4_AZURE = SupportedModel.GPT_5_4_AZURE;
    AIModel O4_MINI_AZURE = SupportedModel.O4_MINI_AZURE;

    // Azure AI Foundry
    AIModel GPT_5_4_FOUNDRY = SupportedModel.GPT_5_4_FOUNDRY;
    AIModel CLAUDE_SONNET_4_6_FOUNDRY = SupportedModel.CLAUDE_SONNET_4_6_FOUNDRY;
    AIModel LLAMA_4_SCOUT_FOUNDRY = SupportedModel.LLAMA_4_SCOUT_FOUNDRY;

    // Vertex AI
    AIModel GEMINI_2_5_PRO_VERTEX = SupportedModel.GEMINI_2_5_PRO_VERTEX;
    AIModel GEMINI_2_5_FLASH_VERTEX = SupportedModel.GEMINI_2_5_FLASH_VERTEX;

    // AWS Bedrock
    AIModel CLAUDE_SONNET_4_6_BEDROCK = SupportedModel.CLAUDE_SONNET_4_6_BEDROCK;
    AIModel AMAZON_NOVA_PREMIER_BEDROCK = SupportedModel.AMAZON_NOVA_PREMIER_BEDROCK;
    AIModel LLAMA_4_MAVERICK_BEDROCK = SupportedModel.LLAMA_4_MAVERICK_BEDROCK;

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
