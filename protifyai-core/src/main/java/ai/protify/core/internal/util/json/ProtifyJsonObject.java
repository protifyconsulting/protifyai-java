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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProtifyJsonObject {

    private final Object root;

    ProtifyJsonObject(String json) {
        this.root = parseValue(new JsonScanner(json));
    }

    Object getRoot() {
        return root;
    }

    public Object get(String path) {
        if (path == null || path.isEmpty()) return root;
        String[] parts = path.split("\\.");
        Object current = root;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else if (current instanceof List) {
                if (part.matches("\\d+")) {
                    int index = Integer.parseInt(part);
                    List<?> list = (List<?>) current;
                    current = (index >= 0 && index < list.size()) ? list.get(index) : null;
                } else {
                    return null;
                }
            } else {
                return null;
            }
            if (current == null) return null;
        }
        return current;
    }

    public String getString(String path) {
        Object val = get(path);
        return val != null ? val.toString() : null;
    }

    private Object parseValue(JsonScanner scanner) {
        scanner.skipWhitespace();
        char c = scanner.peek();
        if (c == 0) return null;
        if (c == '{') return parseObject(scanner);
        if (c == '[') return parseArray(scanner);
        if (c == '"') return parseString(scanner);
        if (c == 't' || c == 'f') return parseBoolean(scanner);
        if (c == 'n') return parseNull(scanner);
        return parseNumber(scanner);
    }

    private Map<String, Object> parseObject(JsonScanner scanner) {
        Map<String, Object> map = new LinkedHashMap<>();
        scanner.consume('{');
        scanner.skipWhitespace();
        if (scanner.peek() == '}') {
            scanner.consume('}');
            return map;
        }
        while (true) {
            String key = parseString(scanner);
            scanner.skipWhitespace();
            scanner.consume(':');
            map.put(key, parseValue(scanner));
            scanner.skipWhitespace();
            char next = scanner.next();
            if (next == '}') break;
            if (next != ',') throw new RuntimeException("Expected ',' or '}'");
            scanner.skipWhitespace();
        }
        return map;
    }

    private List<Object> parseArray(JsonScanner scanner) {
        List<Object> list = new ArrayList<>();
        scanner.consume('[');
        scanner.skipWhitespace();
        if (scanner.peek() == ']') {
            scanner.consume(']');
            return list;
        }
        while (true) {
            list.add(parseValue(scanner));
            scanner.skipWhitespace();
            char next = scanner.next();
            if (next == ']') break;
            if (next != ',') throw new RuntimeException("Expected ',' or ']'");
            scanner.skipWhitespace();
        }
        return list;
    }

    private String parseString(JsonScanner scanner) {
        scanner.consume('"');
        StringBuilder sb = new StringBuilder();
        while (scanner.peek() != '"' && scanner.peek() != 0) {
            char c = scanner.next();
            if (c == '\\') {
                char escaped = scanner.next();
                switch (escaped) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        StringBuilder hex = new StringBuilder();
                        for (int i = 0; i < 4; i++) hex.append(scanner.next());
                        sb.append((char) Integer.parseInt(hex.toString(), 16));
                        break;
                    default:
                        sb.append(escaped);
                }
            } else {
                sb.append(c);
            }
        }
        scanner.consume('"');
        return sb.toString();
    }

    private Number parseNumber(JsonScanner scanner) {
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = scanner.peek();
            if (Character.isDigit(c) || "-+.eE".indexOf(c) != -1) {
                sb.append(scanner.next());
            } else if (c == '_') {
                // Skip underscores used as digit grouping (e.g. 2_161_000)
                scanner.next();
            } else if (c == ',' && isThousandsSeparator(scanner)) {
                // Skip commas used as digit grouping (e.g. 2,161,000)
                scanner.next();
            } else {
                break;
            }
        }
        String val = sb.toString();
        if (val.isEmpty()) {
            throw new NumberFormatException(
                    "Expected a number at position " + scanner.pos + " but found '" + scanner.peek() + "'");
        }
        try {
            if (val.contains(".") || val.contains("e") || val.contains("E")) {
                return Double.parseDouble(val);
            }
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return new BigDecimal(val);
        }
    }

    private boolean isThousandsSeparator(JsonScanner scanner) {
        // A comma is a thousands separator if the next 3 characters are digits
        // and the 4th is NOT a digit (e.g. ",000}" or ",161," but not ",1234")
        return Character.isDigit(scanner.peekAt(1))
                && Character.isDigit(scanner.peekAt(2))
                && Character.isDigit(scanner.peekAt(3))
                && !Character.isDigit(scanner.peekAt(4));
    }

    private Boolean parseBoolean(JsonScanner scanner) {
        String s = scanner.peek() == 't' ? "true" : "false";
        for (int i = 0; i < s.length(); i++) scanner.consume(s.charAt(i));
        return Boolean.parseBoolean(s);
    }

    private Object parseNull(JsonScanner scanner) {
        String s = "null";
        for (int i = 0; i < s.length(); i++) scanner.consume(s.charAt(i));
        return null;
    }

    private static class JsonScanner {
        private final String json;
        private int pos = 0;

        JsonScanner(String json) {
            this.json = json != null ? json : "";
            skipWhitespace();
        }

        char peek() {
            return pos < json.length() ? json.charAt(pos) : 0;
        }

        char peekAt(int offset) {
            int idx = pos + offset;
            return idx < json.length() ? json.charAt(idx) : 0;
        }

        char next() {
            if (pos >= json.length()) return 0;
            return json.charAt(pos++);
        }

        void consume(char expected) {
            skipWhitespace();
            char c = next();
            if (c != expected) {
                throw new RuntimeException(String.format(
                        "JSON Syntax Error: Expected '%c' but found '%c' at position %d around: ...%s...",
                        expected, c, pos, getSnippet()
                ));
            }
        }

        private String getSnippet() {
            int start = Math.max(0, pos - 10);
            int end = Math.min(json.length(), pos + 10);
            return json.substring(start, end);
        }

        void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
        }
    }
}
