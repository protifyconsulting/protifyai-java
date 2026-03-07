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

import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ReturnTypeMapper {

    private ReturnTypeMapper() {}

    public enum ReturnCategory {
        STRING,
        POJO,
        LIST,
        ENUM,
        PRIMITIVE,
        AI_RESPONSE,
        AI_STREAM_RESPONSE,
        COMPLETABLE_FUTURE,
        VOID
    }

    public static final class ReturnTypeInfo {
        private final ReturnCategory category;
        private final Class<?> targetClass;
        private final ReturnTypeInfo innerTypeInfo;

        private ReturnTypeInfo(ReturnCategory category, Class<?> targetClass, ReturnTypeInfo innerTypeInfo) {
            this.category = category;
            this.targetClass = targetClass;
            this.innerTypeInfo = innerTypeInfo;
        }

        public ReturnCategory getCategory() {
            return category;
        }

        public Class<?> getTargetClass() {
            return targetClass;
        }

        public ReturnTypeInfo getInnerTypeInfo() {
            return innerTypeInfo;
        }
    }

    public static ReturnTypeInfo analyze(Method method) {
        Type genericReturnType = method.getGenericReturnType();
        Class<?> rawReturnType = method.getReturnType();
        return analyzeType(genericReturnType, rawReturnType);
    }

    private static ReturnTypeInfo analyzeType(Type genericType, Class<?> rawType) {
        if (rawType == void.class || rawType == Void.class) {
            return new ReturnTypeInfo(ReturnCategory.VOID, void.class, null);
        }

        if (rawType == String.class) {
            return new ReturnTypeInfo(ReturnCategory.STRING, String.class, null);
        }

        if (AIResponse.class.isAssignableFrom(rawType)) {
            return new ReturnTypeInfo(ReturnCategory.AI_RESPONSE, AIResponse.class, null);
        }

        if (AIStreamResponse.class.isAssignableFrom(rawType)) {
            return new ReturnTypeInfo(ReturnCategory.AI_STREAM_RESPONSE, AIStreamResponse.class, null);
        }

        if (rawType.isEnum()) {
            return new ReturnTypeInfo(ReturnCategory.ENUM, rawType, null);
        }

        if (isPrimitive(rawType)) {
            return new ReturnTypeInfo(ReturnCategory.PRIMITIVE, rawType, null);
        }

        if (CompletableFuture.class.isAssignableFrom(rawType)) {
            ReturnTypeInfo inner = resolveGenericTypeArg(genericType);
            return new ReturnTypeInfo(ReturnCategory.COMPLETABLE_FUTURE, rawType, inner);
        }

        if (List.class.isAssignableFrom(rawType)) {
            Class<?> elementClass = resolveListElementClass(genericType);
            return new ReturnTypeInfo(ReturnCategory.LIST, elementClass, null);
        }

        return new ReturnTypeInfo(ReturnCategory.POJO, rawType, null);
    }

    public static Object mapResponse(AIResponse response, ReturnTypeInfo typeInfo) {
        switch (typeInfo.category) {
            case STRING:
                return response.text();
            case POJO:
                return response.as(typeInfo.targetClass);
            case LIST:
                return response.asList(typeInfo.targetClass);
            case ENUM:
                return parseEnum(response.text(), typeInfo.targetClass);
            case PRIMITIVE:
                return parsePrimitive(response.text(), typeInfo.targetClass);
            case AI_RESPONSE:
                return response;
            case VOID:
                return null;
            default:
                throw new IllegalStateException("Unsupported return category: " + typeInfo.category);
        }
    }

    public static String getResponseFormatGuidance(ReturnTypeInfo typeInfo) {
        switch (typeInfo.category) {
            case POJO:
                return "Respond with valid JSON only. Do not include any text outside the JSON object.";
            case LIST:
                return "Respond with a valid JSON array only. Do not include any text outside the JSON array.";
            case ENUM:
                return "Respond with exactly one of: " + getEnumValues(typeInfo.targetClass)
                        + ". Do not include any other text.";
            case PRIMITIVE:
                return getPrimitiveGuidance(typeInfo.targetClass);
            default:
                return null;
        }
    }

    private static boolean isPrimitive(Class<?> type) {
        return type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == double.class || type == Double.class
                || type == float.class || type == Float.class
                || type == boolean.class || type == Boolean.class
                || type == short.class || type == Short.class
                || type == byte.class || type == Byte.class;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parseEnum(String text, Class<?> enumClass) {
        String trimmed = text.trim();
        for (Object constant : enumClass.getEnumConstants()) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(trimmed)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(
                "Could not match '" + trimmed + "' to any value of " + enumClass.getSimpleName()
                        + ". Expected one of: " + getEnumValues(enumClass));
    }

    private static Object parsePrimitive(String text, Class<?> type) {
        String trimmed = text.trim();
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(trimmed);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(trimmed);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(trimmed);
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(trimmed);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(trimmed);
        } else if (type == short.class || type == Short.class) {
            return Short.parseShort(trimmed);
        } else if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(trimmed);
        }
        throw new IllegalArgumentException("Unsupported primitive type: " + type);
    }

    private static String getEnumValues(Class<?> enumClass) {
        StringBuilder sb = new StringBuilder();
        Object[] constants = enumClass.getEnumConstants();
        for (int i = 0; i < constants.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(((Enum<?>) constants[i]).name());
        }
        return sb.toString();
    }

    private static String getPrimitiveGuidance(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            return "Respond with exactly 'true' or 'false'. Do not include any other text.";
        } else if (type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == short.class || type == Short.class
                || type == byte.class || type == Byte.class) {
            return "Respond with exactly one integer number. Do not include any other text.";
        } else {
            return "Respond with exactly one number. Do not include any other text.";
        }
    }

    private static Class<?> resolveListElementClass(Type genericType) {
        if (genericType instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
            if (typeArgs.length == 1 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }
        return Object.class;
    }

    private static ReturnTypeInfo resolveGenericTypeArg(Type genericType) {
        if (genericType instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
            if (typeArgs.length == 1) {
                Type innerType = typeArgs[0];
                Class<?> innerRaw;
                if (innerType instanceof Class) {
                    innerRaw = (Class<?>) innerType;
                } else if (innerType instanceof ParameterizedType) {
                    innerRaw = (Class<?>) ((ParameterizedType) innerType).getRawType();
                } else {
                    innerRaw = Object.class;
                }
                return analyzeType(innerType, innerRaw);
            }
        }
        return new ReturnTypeInfo(ReturnCategory.STRING, String.class, null);
    }
}
