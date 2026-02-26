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
    AIModel ANTHROPIC_V1 = SupportedModel.ANTHROPIC_V1;
    AIModel CLAUDE_SONNET_4_5 = SupportedModel.CLAUDE_SONNET_4_5;
    AIModel CLAUDE_HAIKU_4_5 = SupportedModel.CLAUDE_HAIKU_4_5;
    AIModel CLAUDE_OPUS_4_5 = SupportedModel.CLAUDE_OPUS_4_5;

    // Open AI
    AIModel GPT_5_1 = SupportedModel.GPT_5_1;
    AIModel GPT_5_2 = SupportedModel.GPT_5_2;
    AIModel GPT_5_2_PRO = SupportedModel.GPT_5_2_PRO;
    AIModel GPT_5_1_CODEX_MAX = SupportedModel.GPT_5_1_CODEX_MAX;
    AIModel GPT_5_MINI = SupportedModel.GPT_5_MINI;
    AIModel GPT_5_NANO = SupportedModel.GPT_5_NANO;

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