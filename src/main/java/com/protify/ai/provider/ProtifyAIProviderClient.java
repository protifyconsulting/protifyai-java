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

package com.protify.ai.provider;

import com.protify.ai.internal.config.Configuration;
import com.protify.ai.internal.exception.ProtifyApiException;
import com.protify.ai.request.AIRequest;

import java.lang.reflect.ParameterizedType;


public abstract class ProtifyAIProviderClient<T extends AIProviderRequest> implements AIProviderClient<T> {

    private Configuration configuration;
    private AIProvider provider;
    private String modelName;

    public void initialize(Configuration configuration, AIProvider provider, String modelName) {
        this.configuration = configuration;
        this.provider = provider;
        this.modelName = modelName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T transformRequest(AIRequest request, Configuration derivedConfiguration) {
        try {
            Class<T> clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            T instance = clazz.getDeclaredConstructor().newInstance();
            instance.initialize(request, derivedConfiguration);
            return instance;
        } catch (Exception e) {
            throw new ProtifyApiException("Failed to instantiate and initialize request of type T", e);
        }
    }

    public AIProvider getProvider() {
        return this.provider;
    }

    public String getModelName() {
        return this.modelName;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
