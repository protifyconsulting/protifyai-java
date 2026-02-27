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

package ai.protify.core.internal.provider.gemini.model;

import ai.protify.core.internal.util.json.ProtifyJsonProperty;

import java.util.Map;

public final class GeminiPart {

    private String text;

    @ProtifyJsonProperty("inlineData")
    private GeminiInlineData inlineData;

    @ProtifyJsonProperty("functionCall")
    private GeminiFunctionCall functionCall;

    @ProtifyJsonProperty("functionResponse")
    private GeminiFunctionResponse functionResponse;

    private GeminiPart() {
    }

    public static GeminiPart text(String text) {
        GeminiPart part = new GeminiPart();
        part.text = text;
        return part;
    }

    public static GeminiPart inlineData(String mimeType, String base64Data) {
        GeminiPart part = new GeminiPart();
        part.inlineData = new GeminiInlineData(mimeType, base64Data);
        return part;
    }

    public static GeminiPart functionCall(String name, Map<String, Object> args) {
        GeminiPart part = new GeminiPart();
        part.functionCall = new GeminiFunctionCall(name, args);
        return part;
    }

    public static GeminiPart functionResponse(String name, Map<String, Object> response) {
        GeminiPart part = new GeminiPart();
        part.functionResponse = new GeminiFunctionResponse(name, response);
        return part;
    }

    public String getText() {
        return text;
    }

    @ProtifyJsonProperty("inlineData")
    public GeminiInlineData getInlineData() {
        return inlineData;
    }

    @ProtifyJsonProperty("functionCall")
    public GeminiFunctionCall getFunctionCall() {
        return functionCall;
    }

    @ProtifyJsonProperty("functionResponse")
    public GeminiFunctionResponse getFunctionResponse() {
        return functionResponse;
    }
}
