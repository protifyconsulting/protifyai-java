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

package ai.protify.core.internal.provider.deepseek;

import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.provider.chatcompletions.ChatCompletionsRequest;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class DeepSeekRequest extends ChatCompletionsRequest {

    private static final Set<AIConfigProperty> REASONER_UNSUPPORTED = Collections.unmodifiableSet(
            EnumSet.of(AIConfigProperty.TEMPERATURE, AIConfigProperty.TOP_P));

    @Override
    protected Set<AIConfigProperty> getUnsupportedParametersForModel(String model) {
        if ("deepseek-reasoner".equals(model)) {
            return REASONER_UNSUPPORTED;
        }
        return Collections.emptySet();
    }
}
