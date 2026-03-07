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

import java.util.Map;

public final class MessageTemplateResolver {

    private MessageTemplateResolver() {}

    public static String resolve(String template, Map<String, Object> variables) {
        StringBuilder result = new StringBuilder(template.length());
        int i = 0;
        while (i < template.length()) {
            if (i + 1 < template.length() && template.charAt(i) == '{' && template.charAt(i + 1) == '{') {
                int closeIndex = template.indexOf("}}", i + 2);
                if (closeIndex == -1) {
                    throw new IllegalArgumentException(
                            "Unclosed placeholder starting at index " + i + " in template: " + template);
                }
                String varName = template.substring(i + 2, closeIndex).trim();
                if (varName.isEmpty()) {
                    throw new IllegalArgumentException("Empty placeholder name in template: " + template);
                }
                if (!variables.containsKey(varName)) {
                    throw new IllegalArgumentException(
                            "No value provided for template variable '{{" + varName + "}}'. "
                                    + "Available variables: " + variables.keySet());
                }
                result.append(String.valueOf(variables.get(varName)));
                i = closeIndex + 2;
            } else {
                result.append(template.charAt(i));
                i++;
            }
        }
        return result.toString();
    }
}
