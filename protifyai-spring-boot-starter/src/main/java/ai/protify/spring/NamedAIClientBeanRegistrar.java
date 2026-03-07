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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.logging.Logger;

class NamedAIClientBeanRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private static final Logger LOGGER = Logger.getLogger(NamedAIClientBeanRegistrar.class.getName());

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ProtifyAIProperties properties = bindProperties();
        if (properties == null) {
            return;
        }

        for (Map.Entry<String, ProtifyAIProperties.ClientProperties> entry : properties.getClients().entrySet()) {
            String clientName = entry.getKey();
            ProtifyAIProperties.ClientProperties clientProps = entry.getValue();
            registerNamedClientBean(registry, clientName, clientProps);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // No-op
    }

    private void registerNamedClientBean(BeanDefinitionRegistry registry, String clientName,
                                          ProtifyAIProperties.ClientProperties clientProps) {
        String beanName = clientName + "AIClient";

        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(AIClient.class, () -> AIClientFactory.createClient(clientProps))
                .getBeanDefinition();

        beanDefinition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, clientName));

        registry.registerBeanDefinition(beanName, beanDefinition);
        LOGGER.info("Registered named AIClient bean '" + beanName + "' with @Qualifier(\"" + clientName + "\")");
    }

    private ProtifyAIProperties bindProperties() {
        try {
            return Binder.get(environment)
                    .bind("protify.ai", Bindable.of(ProtifyAIProperties.class))
                    .orElse(null);
        } catch (Exception e) {
            LOGGER.warning("Could not bind protify.ai properties: " + e.getMessage());
            return null;
        }
    }
}
