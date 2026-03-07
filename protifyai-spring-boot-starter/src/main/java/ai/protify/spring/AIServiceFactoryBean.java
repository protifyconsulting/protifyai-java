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

package ai.protify.spring;

import ai.protify.core.AIClient;
import ai.protify.core.ProtifyAI;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

public class AIServiceFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> serviceInterface;
    private final String clientName;

    @Autowired
    private AIClientRegistry clientRegistry;

    public AIServiceFactoryBean(Class<T> serviceInterface, String clientName) {
        this.serviceInterface = serviceInterface;
        this.clientName = clientName;
    }

    @Override
    public T getObject() {
        AIClient client = clientRegistry.getClient(clientName);
        return ProtifyAI.create(serviceInterface, client);
    }

    @Override
    public Class<?> getObjectType() {
        return serviceInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
