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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Map;

@Configuration
@ConditionalOnClass(AIClient.class)
@ConditionalOnProperty(prefix = "protify.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ProtifyAIProperties.class)
@Import({AIServiceBeanRegistrar.class, NamedAIClientBeanRegistrar.class})
public class ProtifyAIAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "protifyAIInitializer")
    public ProtifyAIInitializer protifyAIInitializer() {
        ProtifyAI.initialize();
        return new ProtifyAIInitializer();
    }

    @Bean
    @ConditionalOnMissingBean(AIClient.class)
    public AIClient protifyAIClient(ProtifyAIProperties properties) {
        return AIClientFactory.createClient(properties.getDefaults());
    }

    @Bean
    @ConditionalOnMissingBean(AIClientRegistry.class)
    public AIClientRegistry protifyAIClientRegistry(ProtifyAIProperties properties, AIClient defaultClient) {
        AIClientRegistry registry = new AIClientRegistry(defaultClient);
        for (Map.Entry<String, ProtifyAIProperties.ClientProperties> entry : properties.getClients().entrySet()) {
            registry.registerClient(entry.getKey(), AIClientFactory.createClient(entry.getValue()));
        }
        return registry;
    }

    static class ProtifyAIInitializer {
        // Marker bean to track initialization
    }
}
