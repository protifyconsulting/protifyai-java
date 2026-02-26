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

package ai.protify.core.internal.util.json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProtifyJson {

    private static final Map<Class<?>, List<JsonBeanProperty>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, JsonBeanField>> DESERIALIZATION_CACHE = new ConcurrentHashMap<>();

    public static ProtifyJsonObject parse(String json) {
        return new ProtifyJsonObject(json);
    }

    public static String toJson(Object object) {
        if (object == null) {
            return "null";
        }

        List<String> pairs = new ArrayList<>();
        List<JsonBeanProperty> properties = CLASS_CACHE.computeIfAbsent(object.getClass(), ProtifyJson::inspectClass);

        for (JsonBeanProperty prop : properties) {
            try {
                Object value = prop.method.invoke(object);
                if (value != null) {
                    pairs.add("\"" + prop.jsonName + "\":" + formatValue(value));
                }
            } catch (Exception e) {
                // Ignore invocation errors
            }
        }

        return "{" + String.join(",", pairs) + "}";
    }

    private static List<JsonBeanProperty> inspectClass(Class<?> clazz) {
        List<JsonBeanProperty> properties = new ArrayList<>();
        for (Method method : clazz.getMethods()) {
            if (isGetter(method)) {
                properties.add(new JsonBeanProperty(getFieldName(method), method));
            }
        }
        return properties;
    }

    // Helper class to hold the cached reflection data
    private static class JsonBeanProperty {
        final String jsonName;
        final Method method;

        JsonBeanProperty(String jsonName, Method method) {
            this.jsonName = jsonName;
            this.method = method;
        }
    }

    private static boolean isGetter(Method method) {
        String name = method.getName();
        return (name.startsWith("get") && name.length() > 3 || name.startsWith("is") && name.length() > 2)
                && method.getParameterCount() == 0
                && !name.equals("getClass");
    }

    private static String getFieldName(Method method) {
        if (method.isAnnotationPresent(ProtifyJsonProperty.class)) {
            return method.getAnnotation(ProtifyJsonProperty.class).value();
        }
        String methodName = method.getName();
        int prefixLength = methodName.startsWith("is") ? 2 : 3;
        String nameWithoutPrefix = methodName.substring(prefixLength);
        return Character.toLowerCase(nameWithoutPrefix.charAt(0)) + nameWithoutPrefix.substring(1);
    }

    private static String formatValue(Object value) {
        if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Enum<?>) {
            return "\"" + value + "\"";
        }
        if (value instanceof List) {
            List<String> items = new ArrayList<>();
            for (Object item : (List<?>) value) {
                items.add(item == null ? "null" : formatValue(item));
            }
            return "[" + String.join(",", items) + "]";
        }
        if (value instanceof Map) {
            List<String> entries = new ArrayList<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (entry.getValue() != null) {
                    entries.add("\"" + escapeJson(entry.getKey().toString()) + "\":" + formatValue(entry.getValue()));
                }
            }
            return "{" + String.join(",", entries) + "}";
        }
        return toJson(value);
    }

    public static String toJsonMap(Map<String, Object> map) {
        if (map == null) {
            return "null";
        }
        return formatValue(map);
    }

    public static String escapeJson(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c <= 0x1F) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // --- Deserialization ---

    public static <T> T fromJson(String json, Class<T> type) {
        Object root = parse(extractJson(json)).getRoot();
        return mapToObject(root, type, type);
    }

    public static <T> List<T> fromJsonList(String json, Class<T> elementType) {
        Object root = parse(extractJson(json)).getRoot();
        if (!(root instanceof List)) {
            throw new IllegalArgumentException("JSON root is not an array");
        }
        List<T> result = new ArrayList<>();
        for (Object item : (List<?>) root) {
            result.add(mapToObject(item, elementType, elementType));
        }
        return result;
    }

    static String extractJson(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String trimmed = text.trim();

        // Already clean JSON
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed;
        }

        // Markdown fenced code block: ```json ... ``` or ``` ... ```
        int fenceStart = text.indexOf("```");
        if (fenceStart >= 0) {
            int contentStart = text.indexOf('\n', fenceStart);
            if (contentStart >= 0) {
                int fenceEnd = text.indexOf("```", contentStart);
                if (fenceEnd > contentStart) {
                    return text.substring(contentStart + 1, fenceEnd).trim();
                }
            }
        }

        // Surrounding prose: find the outermost { ... } or [ ... ]
        int objStart = trimmed.indexOf('{');
        int arrStart = trimmed.indexOf('[');

        int start;
        char openChar;
        char closeChar;

        if (objStart >= 0 && (arrStart < 0 || objStart < arrStart)) {
            start = objStart;
            openChar = '{';
            closeChar = '}';
        } else if (arrStart >= 0) {
            start = arrStart;
            openChar = '[';
            closeChar = ']';
        } else {
            return trimmed;
        }

        // Walk forward to find the matching close bracket, respecting nesting and strings
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == openChar) {
                depth++;
            } else if (c == closeChar) {
                depth--;
                if (depth == 0) {
                    return trimmed.substring(start, i + 1);
                }
            }
        }

        // No balanced match found — return trimmed and let the parser report the error
        return trimmed;
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapToObject(Object value, Class<T> targetType, Type genericType) {
        if (value == null) {
            return null;
        }

        // String
        if (targetType == String.class) {
            return targetType.cast(value.toString());
        }

        // Primitives and wrappers
        if (targetType == int.class || targetType == Integer.class) {
            return (T) Integer.valueOf(toInt(value));
        }
        if (targetType == long.class || targetType == Long.class) {
            return (T) Long.valueOf(toLong(value));
        }
        if (targetType == double.class || targetType == Double.class) {
            return (T) Double.valueOf(toDouble(value));
        }
        if (targetType == float.class || targetType == Float.class) {
            return (T) Float.valueOf(toFloat(value));
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return (T) toBoolean(value);
        }
        if (targetType == short.class || targetType == Short.class) {
            return (T) Short.valueOf((short) toInt(value));
        }
        if (targetType == byte.class || targetType == Byte.class) {
            return (T) Byte.valueOf((byte) toInt(value));
        }

        // BigDecimal / BigInteger
        if (targetType == BigDecimal.class) {
            if (value instanceof BigDecimal) return (T) value;
            return (T) new BigDecimal(value.toString());
        }
        if (targetType == BigInteger.class) {
            if (value instanceof BigInteger) return (T) value;
            if (value instanceof Number) return (T) BigInteger.valueOf(((Number) value).longValue());
            return (T) new BigInteger(value.toString());
        }

        // Enum (case-insensitive)
        if (targetType.isEnum()) {
            String raw = value.toString();
            Object[] constants = targetType.getEnumConstants();
            for (Object constant : constants) {
                if (((Enum<?>) constant).name().equalsIgnoreCase(raw)) {
                    return (T) constant;
                }
            }
            StringBuilder valid = new StringBuilder();
            for (int i = 0; i < constants.length; i++) {
                if (i > 0) valid.append(", ");
                valid.append(((Enum<?>) constants[i]).name());
            }
            String displayValue = raw.length() > 100
                    ? raw.substring(0, 100) + "... (truncated, length=" + raw.length() + ")"
                    : raw;
            throw new IllegalArgumentException(
                    "No enum constant " + targetType.getSimpleName() + " matching \"" + displayValue
                    + "\". Valid values: [" + valid + "]");
        }

        // Array
        if (targetType.isArray() && value instanceof List) {
            Class<?> componentType = targetType.getComponentType();
            List<?> items = (List<?>) value;
            Object array = Array.newInstance(componentType, items.size());
            for (int i = 0; i < items.size(); i++) {
                Array.set(array, i, mapToObject(items.get(i), componentType, componentType));
            }
            return (T) array;
        }

        // List
        if (List.class.isAssignableFrom(targetType) && value instanceof List) {
            Class<?> elementType = Object.class;
            Type elementGenericType = Object.class;
            if (genericType instanceof ParameterizedType) {
                Type typeArg = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                elementGenericType = typeArg;
                elementType = extractRawClass(typeArg);
            }
            List<Object> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(mapToObject(item, elementType, elementGenericType));
            }
            return (T) result;
        }

        // Map
        if (Map.class.isAssignableFrom(targetType) && value instanceof Map) {
            Class<?> valueType = Object.class;
            Type valueGenericType = Object.class;
            if (genericType instanceof ParameterizedType) {
                Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
                if (typeArgs.length >= 2) {
                    valueGenericType = typeArgs[1];
                    valueType = extractRawClass(typeArgs[1]);
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                result.put(entry.getKey().toString(),
                        mapToObject(entry.getValue(), valueType, valueGenericType));
            }
            return (T) result;
        }

        // Nested object
        if (value instanceof Map) {
            return constructObject((Map<String, Object>) value, targetType);
        }

        // Last resort — direct cast
        return targetType.cast(value);
    }

    private static <T> T constructObject(Map<String, Object> map, Class<T> type) {
        T instance;
        try {
            instance = type.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    type.getName() + " has no no-arg constructor. "
                    + "Classes used with fromJson must have a public no-arg constructor.", e);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to instantiate " + type.getName() + ": " + e.getMessage(), e);
        }

        Map<String, JsonBeanField> fields = DESERIALIZATION_CACHE.computeIfAbsent(
                type, ProtifyJson::inspectForDeserialization);

        for (Map.Entry<String, JsonBeanField> entry : fields.entrySet()) {
            if (!map.containsKey(entry.getKey())) {
                continue;
            }

            JsonBeanField beanField = entry.getValue();
            Object rawValue = map.get(entry.getKey());

            Object converted;
            try {
                converted = mapToObject(rawValue, beanField.field.getType(), beanField.field.getGenericType());
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Failed to deserialize field '" + entry.getKey() + "' on " + type.getName()
                        + ": expected " + beanField.field.getType().getSimpleName()
                        + " but JSON value was " + truncateValue(rawValue)
                        + ". Cause: " + e.getMessage(), e);
            }

            if (converted == null && beanField.field.getType().isPrimitive()) {
                continue;
            }

            if (!beanField.accessible) {
                String setterName = "set" + Character.toUpperCase(beanField.field.getName().charAt(0))
                        + beanField.field.getName().substring(1);
                String moduleName = beanField.field.getDeclaringClass().getModule().getName();
                String addOpensHint = moduleName != null
                        ? " If running on Java 16+, add '--add-opens " + moduleName
                          + "/" + beanField.field.getDeclaringClass().getPackageName()
                          + "=ALL-UNNAMED' to your JVM arguments."
                        : "";
                throw new IllegalArgumentException(
                        "Cannot set field '" + entry.getKey() + "' on " + type.getName()
                        + ": the field is not accessible and no public setter '"
                        + setterName + "(" + beanField.field.getType().getSimpleName()
                        + ")' was found. Add a public setter method." + addOpensHint);
            }

            try {
                if (beanField.setter != null) {
                    beanField.setter.invoke(instance, converted);
                } else {
                    beanField.field.set(instance, converted);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Failed to set field '" + entry.getKey() + "' on " + type.getName()
                        + ": cannot assign " + (converted == null ? "null" : converted.getClass().getSimpleName())
                        + " to " + beanField.field.getType().getSimpleName()
                        + ". Cause: " + e.getMessage(), e);
            }
        }

        return instance;
    }

    private static String truncateValue(Object value) {
        if (value == null) return "null";
        String str = value.toString();
        if (str.length() > 100) {
            return "\"" + str.substring(0, 100) + "...\" (truncated, length=" + str.length() + ")";
        }
        return "\"" + str + "\"";
    }

    private static Map<String, JsonBeanField> inspectForDeserialization(Class<?> type) {
        Map<String, JsonBeanField> fields = new LinkedHashMap<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                String jsonName;
                if (field.isAnnotationPresent(ProtifyJsonProperty.class)) {
                    jsonName = field.getAnnotation(ProtifyJsonProperty.class).value();
                } else {
                    jsonName = field.getName();
                }
                if (fields.containsKey(jsonName)) {
                    continue;
                }

                Method setter = findSetter(type, field);
                boolean accessible = setter != null;
                if (setter == null) {
                    try {
                        field.setAccessible(true);
                        accessible = true;
                    } catch (RuntimeException e) {
                        // InaccessibleObjectException on Java 16+ when module system blocks access.
                        // Field will be flagged as inaccessible; a clear error is thrown at deserialization time.
                    }
                }
                fields.put(jsonName, new JsonBeanField(jsonName, field, setter, accessible));
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static Method findSetter(Class<?> type, Field field) {
        String fieldName = field.getName();
        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            return type.getMethod(setterName, field.getType());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static class JsonBeanField {
        final String jsonName;
        final Field field;
        final Method setter;
        final boolean accessible;

        JsonBeanField(String jsonName, Field field, Method setter, boolean accessible) {
            this.jsonName = jsonName;
            this.field = field;
            this.setter = setter;
            this.accessible = accessible;
        }
    }

    private static Class<?> extractRawClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        return Object.class;
    }

    private static int toInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    private static long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    private static double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    private static float toFloat(Object value) {
        if (value instanceof Number) return ((Number) value).floatValue();
        return Float.parseFloat(value.toString());
    }

    private static Boolean toBoolean(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    public static Map<String, Object> mapOf(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid number of arguments");
        }
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }

    private ProtifyJson() {}
}