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

package ai.protify.core.internal.service;

import ai.protify.core.AIClient;
import ai.protify.core.request.AIRequestBuilder;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;
import ai.protify.core.service.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class AIServiceProxy implements InvocationHandler {

    private final AIClient client;
    private final Map<Method, MethodMetadata> methodCache;

    private AIServiceProxy(AIClient client, Map<Method, MethodMetadata> methodCache) {
        this.client = client;
        this.methodCache = methodCache;
    }

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> serviceInterface, AIClient client) {
        validate(serviceInterface);
        Map<Method, MethodMetadata> methodCache = buildMethodCache(serviceInterface);
        AIServiceProxy handler = new AIServiceProxy(client, methodCache);
        return (T) Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                handler
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        MethodMetadata metadata = methodCache.get(method);
        if (metadata == null) {
            throw new IllegalStateException("No metadata found for method: " + method.getName());
        }

        Map<String, Object> variables = buildVariableMap(metadata, args);
        String resolvedMessage = MessageTemplateResolver.resolve(metadata.template, variables);

        String instructions = metadata.instructions;
        String formatGuidance = ReturnTypeMapper.getResponseFormatGuidance(metadata.returnTypeInfo);
        if (formatGuidance != null) {
            instructions = instructions != null
                    ? instructions + "\n\n" + formatGuidance
                    : formatGuidance;
        }

        if (metadata.returnTypeInfo.getCategory() == ReturnTypeMapper.ReturnCategory.AI_STREAM_RESPONSE) {
            return executeStream(resolvedMessage, instructions, metadata);
        }

        if (metadata.returnTypeInfo.getCategory() == ReturnTypeMapper.ReturnCategory.COMPLETABLE_FUTURE) {
            String finalInstructions = instructions;
            return CompletableFuture.supplyAsync(() -> {
                AIResponse response = executeSync(resolvedMessage, finalInstructions, metadata);
                ReturnTypeMapper.ReturnTypeInfo innerType = metadata.returnTypeInfo.getInnerTypeInfo();
                if (innerType == null) {
                    return response;
                }
                return ReturnTypeMapper.mapResponse(response, innerType);
            });
        }

        AIResponse response = executeSync(resolvedMessage, instructions, metadata);
        return ReturnTypeMapper.mapResponse(response, metadata.returnTypeInfo);
    }

    private AIResponse executeSync(String message, String instructions, MethodMetadata metadata) {
        AIRequestBuilder builder = client.newRequest().addInput(message);
        if (instructions != null) {
            builder.instructions(instructions);
        }
        if (metadata.temperature != null) {
            builder.temperature(metadata.temperature);
        }
        if (metadata.maxTokens != null) {
            builder.maxOutputTokens(metadata.maxTokens);
        }
        return builder.build().execute();
    }

    private AIStreamResponse executeStream(String message, String instructions, MethodMetadata metadata) {
        AIRequestBuilder builder = client.newRequest().addInput(message);
        if (instructions != null) {
            builder.instructions(instructions);
        }
        if (metadata.temperature != null) {
            builder.temperature(metadata.temperature);
        }
        if (metadata.maxTokens != null) {
            builder.maxOutputTokens(metadata.maxTokens);
        }
        return builder.build().executeStream();
    }

    private static Map<String, Object> buildVariableMap(MethodMetadata metadata, Object[] args) {
        Map<String, Object> variables = new LinkedHashMap<>();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                variables.put(metadata.paramNames[i], args[i]);
            }
        }
        return variables;
    }

    private static <T> void validate(Class<T> serviceInterface) {
        if (!serviceInterface.isInterface()) {
            throw new IllegalArgumentException(
                    serviceInterface.getName() + " is not an interface. @AIService can only be applied to interfaces.");
        }
        if (!serviceInterface.isAnnotationPresent(AIService.class)) {
            throw new IllegalArgumentException(
                    serviceInterface.getName() + " is not annotated with @AIService.");
        }
    }

    private static Map<Method, MethodMetadata> buildMethodCache(Class<?> serviceInterface) {
        Map<Method, MethodMetadata> cache = new HashMap<>();
        Instructions typeInstructions = serviceInterface.getAnnotation(Instructions.class);
        String typeInstructionsValue = typeInstructions != null ? typeInstructions.value() : null;

        for (Method method : serviceInterface.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            if (method.isDefault()) {
                continue;
            }
            cache.put(method, buildMethodMetadata(method, typeInstructionsValue));
        }
        return cache;
    }

    private static MethodMetadata buildMethodMetadata(Method method, String typeInstructions) {
        UserMessage userMessage = method.getAnnotation(UserMessage.class);
        if (userMessage == null) {
            throw new IllegalArgumentException(
                    "Method '" + method.getName() + "' is missing @UserMessage annotation.");
        }

        String template = userMessage.value();

        Instructions methodInstructions = method.getAnnotation(Instructions.class);
        String instructions = methodInstructions != null ? methodInstructions.value() : typeInstructions;

        Temperature temp = method.getAnnotation(Temperature.class);
        Double temperature = temp != null ? temp.value() : null;

        MaxTokens mt = method.getAnnotation(MaxTokens.class);
        Integer maxTokens = mt != null ? mt.value() : null;

        String[] paramNames = resolveParamNames(method);
        ReturnTypeMapper.ReturnTypeInfo returnTypeInfo = ReturnTypeMapper.analyze(method);

        return new MethodMetadata(template, instructions, paramNames, returnTypeInfo, temperature, maxTokens);
    }

    private static String[] resolveParamNames(Method method) {
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        String[] names = new String[paramAnnotations.length];
        for (int i = 0; i < paramAnnotations.length; i++) {
            V v = findAnnotation(paramAnnotations[i], V.class);
            if (v == null) {
                throw new IllegalArgumentException(
                        "Parameter " + i + " of method '" + method.getName()
                                + "' is missing @V annotation. All parameters must be annotated with @V.");
            }
            names[i] = v.value();
        }
        return names;
    }

    @SuppressWarnings("unchecked")
    private static <A extends Annotation> A findAnnotation(Annotation[] annotations, Class<A> type) {
        for (Annotation annotation : annotations) {
            if (type.isInstance(annotation)) {
                return (A) annotation;
            }
        }
        return null;
    }

    private static final class MethodMetadata {
        final String template;
        final String instructions;
        final String[] paramNames;
        final ReturnTypeMapper.ReturnTypeInfo returnTypeInfo;
        final Double temperature;
        final Integer maxTokens;

        MethodMetadata(String template, String instructions, String[] paramNames,
                       ReturnTypeMapper.ReturnTypeInfo returnTypeInfo, Double temperature, Integer maxTokens) {
            this.template = template;
            this.instructions = instructions;
            this.paramNames = paramNames;
            this.returnTypeInfo = returnTypeInfo;
            this.temperature = temperature;
            this.maxTokens = maxTokens;
        }
    }
}
