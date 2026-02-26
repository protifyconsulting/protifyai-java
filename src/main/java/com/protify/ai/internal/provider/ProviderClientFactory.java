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

package com.protify.ai.internal.provider;

import com.protify.ai.internal.config.AIConfigProperty;
import com.protify.ai.internal.config.Configuration;
import com.protify.ai.internal.exception.ProtifyApiException;
import com.protify.ai.provider.AIProvider;
import com.protify.ai.provider.AIProviderClient;
import com.protify.ai.internal.util.Logger;
import com.protify.ai.internal.util.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public final class ProviderClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderClientFactory.class);

    private ProviderClientFactory() { }

    public static AIProviderClient<?> createProviderClient(Configuration configuration,
                                                            AIProvider provider,
                                                            String modelName) {
        Objects.requireNonNull(provider, "AIProvider cannot be null");
        LOGGER.debug("Instantiating provider client for AI Provider: {}", provider.getName());

        Class<? extends AIProviderClient<?>> type = provider.getProviderClientType();
        if (type == null) {
            throw new IllegalStateException("Provider " + provider.getName() + " does not define a provider client type.");
        }

        try {
            AIProviderClient<?> client = type.getDeclaredConstructor().newInstance();
            client.initialize(configuration, provider, modelName);
            return client;
        } catch (NoSuchMethodException e) {
            throw new ProtifyApiException("Provider client " + type.getName() + " must have a public no-args constructor", e);
        } catch (InvocationTargetException e) {
            throw new ProtifyApiException("An error occurred during initialization of " + type.getName(), e.getCause());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ProtifyApiException("Could not instantiate provider client: " + type.getName(), e);
        }
    }
}
