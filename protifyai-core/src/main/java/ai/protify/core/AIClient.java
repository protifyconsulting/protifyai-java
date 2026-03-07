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

import ai.protify.core.conversation.AIConversation;
import ai.protify.core.conversation.AIConversationBuilder;
import ai.protify.core.conversation.AIConversationStore;
import ai.protify.core.internal.config.Configuration;
import ai.protify.core.provider.AIProvider;
import ai.protify.core.provider.AIProviderClient;
import ai.protify.core.request.AIRequestBuilder;

public interface AIClient {

    static AIClientBuilder builder() {
        return new AIClientBuilder();
    }

    String getModelName();

    AIProvider getProvider();

    @SuppressWarnings({"java:S1452"})
    AIProviderClient<?> getProviderClient();

    Configuration getConfiguration();

    AIRequestBuilder newRequest();

    AIConversationBuilder newConversation();

    AIConversation loadConversation(String conversationId, AIConversationStore store);
}
