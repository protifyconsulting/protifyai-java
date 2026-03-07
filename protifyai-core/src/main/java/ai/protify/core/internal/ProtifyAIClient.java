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

import ai.protify.core.AIClient;
import ai.protify.core.conversation.AIConversation;
import ai.protify.core.conversation.AIConversationBuilder;
import ai.protify.core.conversation.AIConversationState;
import ai.protify.core.conversation.AIConversationStore;
import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.config.Configuration;
import ai.protify.core.internal.conversation.ProtifyAIConversation;
import ai.protify.core.internal.provider.ProviderClientFactory;
import ai.protify.core.message.AIMessage;
import ai.protify.core.provider.AIProvider;
import ai.protify.core.provider.AIProviderClient;
import ai.protify.core.request.AIRequestBuilder;
import ai.protify.core.internal.util.Logger;
import ai.protify.core.internal.util.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ProtifyAIClient implements AIClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtifyAIClient.class);

    private final Configuration configuration;
    private final AIProvider provider;
    private final String modelName;

    private final AIProviderClient<?> providerClient;

    public ProtifyAIClient(Map<AIConfigProperty, Object> properties,
                           AIProvider provider,
                           String modelName) {
        this.configuration = new Configuration(properties);
        this.modelName = modelName;
        this.provider = provider;

        this.providerClient = ProviderClientFactory.createProviderClient(this.configuration, this.provider, this.modelName);

        LOGGER.info("Protify AI client created for model {}", this.modelName);
        LOGGER.debug("Provider is {}", this.getProvider().getName());
    }

    @Override
    public AIProvider getProvider() {
        return this.provider;
    }

    @Override
    public AIProviderClient getProviderClient() {
        return this.providerClient;
    }

    @Override
    public String getModelName() {
        return this.modelName;
    }

    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }

    @Override
    public AIRequestBuilder newRequest() {
        return new AIRequestBuilder(this);
    }

    @Override
    public AIConversationBuilder newConversation() {
        return new AIConversationBuilder(this);
    }

    @Override
    public AIConversation loadConversation(String conversationId, AIConversationStore store) {
        AIConversationState state = store.load(conversationId);
        List<AIMessage> messages = state != null ? new ArrayList<>(state.getMessages()) : new ArrayList<>();

        return new ProtifyAIConversation(
                this,
                conversationId,
                messages,
                store,
                Collections.emptyMap(),
                Collections.emptyList(),
                Collections.emptyMap(),
                10
        );
    }

    @Override
    public String toString() {
        return "ProtifyAIClient{" +
                ", modelName=" + modelName +
                ", provider=" + provider +
                ", configuration=" + configuration.getConfiguration() +
                '}';
    }
}
