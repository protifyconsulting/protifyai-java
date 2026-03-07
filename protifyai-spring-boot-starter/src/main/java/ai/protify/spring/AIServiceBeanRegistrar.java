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

import ai.protify.core.service.AIService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class AIServiceBeanRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private static final Logger LOGGER = Logger.getLogger(AIServiceBeanRegistrar.class.getName());

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String basePackage = environment.getProperty("protify.ai.service-scan-package");
        if (basePackage == null || basePackage.isEmpty()) {
            basePackage = detectBasePackage(registry);
        }
        if (basePackage == null || basePackage.isEmpty()) {
            LOGGER.info("No base package configured for @AIService scanning. " +
                    "Set 'protify.ai.service-scan-package' or ensure a Spring Boot application class is present.");
            return;
        }

        List<Class<?>> serviceInterfaces = scanForAIServices(basePackage);
        for (Class<?> serviceInterface : serviceInterfaces) {
            registerAIServiceBean(registry, serviceInterface);
        }

        if (!serviceInterfaces.isEmpty()) {
            LOGGER.info("Registered " + serviceInterfaces.size() + " @AIService bean(s): " + serviceInterfaces);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // No-op
    }

    private void registerAIServiceBean(BeanDefinitionRegistry registry, Class<?> serviceInterface) {
        String clientName = "";
        AIService annotation = serviceInterface.getAnnotation(AIService.class);
        if (annotation != null) {
            clientName = annotation.client();
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(AIServiceFactoryBean.class)
                .addConstructorArgValue(serviceInterface)
                .addConstructorArgValue(clientName);

        String beanName = ClassUtils.getShortNameAsProperty(serviceInterface);
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
        LOGGER.fine("Registered @AIService bean '" + beanName + "' for " + serviceInterface.getName() +
                (clientName.isEmpty() ? " (default client)" : " (client: " + clientName + ")"));
    }

    private List<Class<?>> scanForAIServices(String basePackage) {
        List<Class<?>> result = new ArrayList<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resolver);

        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                ClassUtils.convertClassNameToResourcePath(basePackage) + "/**/*.class";

        try {
            Resource[] resources = resolver.getResources(packageSearchPath);
            for (Resource resource : resources) {
                if (!resource.isReadable()) {
                    continue;
                }
                MetadataReader reader = readerFactory.getMetadataReader(resource);
                String className = reader.getClassMetadata().getClassName();
                if (!reader.getClassMetadata().isInterface()) {
                    continue;
                }
                try {
                    Class<?> clazz = Class.forName(className, false, getClass().getClassLoader());
                    if (clazz.isAnnotationPresent(AIService.class)) {
                        result.add(clazz);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    LOGGER.log(Level.FINE, "Could not load class: " + className, e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error scanning for @AIService interfaces in package: " + basePackage, e);
        }

        return result;
    }

    private String detectBasePackage(BeanDefinitionRegistry registry) {
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition bd = registry.getBeanDefinition(beanName);
            String beanClassName = bd.getBeanClassName();
            if (beanClassName == null) {
                continue;
            }
            try {
                Class<?> beanClass = Class.forName(beanClassName, false, getClass().getClassLoader());
                if (beanClass.isAnnotationPresent(SpringBootApplication.class)) {
                    return beanClass.getPackage().getName();
                }
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.FINE, "Could not load bean class: " + beanClassName, e);
            }
        }
        return null;
    }
}
