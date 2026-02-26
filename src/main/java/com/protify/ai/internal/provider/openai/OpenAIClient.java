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

package com.protify.ai.internal.provider.openai;

import com.protify.ai.internal.config.CredentialHelper;
import com.protify.ai.internal.provider.openai.model.OpenAIResponseBody;
import com.protify.ai.internal.response.ProtifyAIStreamResponse;
import com.protify.ai.internal.util.http.ProtifyHttpClient;
import com.protify.ai.internal.util.http.ProtifyHttpResponse;
import com.protify.ai.internal.util.json.ProtifyJson;
import com.protify.ai.provider.ProtifyAIProviderClient;
import com.protify.ai.response.AIResponse;
import com.protify.ai.response.AIStreamResponse;

public class OpenAIClient extends ProtifyAIProviderClient<OpenAIRequest> {

    private static final String RESPONSES_URI = "https://api.openai.com/v1/responses";

    @Override
    public AIResponse execute(OpenAIRequest request) {
        ProtifyHttpResponse response = ProtifyHttpClient.getInstance().post(request, RESPONSES_URI);
        String rawJson = response.getResponseBody();
        OpenAIResponseBody body = ProtifyJson.fromJson(rawJson, OpenAIResponseBody.class);
        return new OpenAIResponse(response.isCachedResponse(), null, null, super.getModelName(), rawJson, body);
    }

    @Override
    public AIStreamResponse executeStream(OpenAIRequest request) {
        request.setStream(true);

        ProtifyAIStreamResponse streamResponse = new ProtifyAIStreamResponse();
        String[] completedResponseJson = {null};

        ProtifyHttpClient.getInstance().postStream(request, RESPONSES_URI,
                data -> {
                    String type = CredentialHelper.extractJsonString(data, "type");
                    if ("response.output_text.delta".equals(type)) {
                        String delta = CredentialHelper.extractJsonString(data, "delta");
                        if (delta != null) {
                            streamResponse.pushToken(delta);
                        }
                    } else if ("response.completed".equals(type)) {
                        int responseStart = data.indexOf("\"response\":");
                        if (responseStart >= 0) {
                            completedResponseJson[0] = extractNestedObject(data, responseStart + "\"response\":".length());
                        }
                    }
                },
                () -> {
                    if (completedResponseJson[0] != null) {
                        OpenAIResponseBody body = ProtifyJson.fromJson(completedResponseJson[0], OpenAIResponseBody.class);
                        streamResponse.complete(new OpenAIResponse(false, null, null,
                                getModelName(), completedResponseJson[0], body));
                    } else {
                        streamResponse.completeWithAccumulatedText();
                    }
                }
        ).exceptionally(ex -> {
            streamResponse.completeExceptionally(ex);
            return null;
        });

        return streamResponse;
    }

    private static String extractNestedObject(String json, int startIndex) {
        int braceStart = json.indexOf('{', startIndex);
        if (braceStart < 0) return null;

        int depth = 0;
        for (int i = braceStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(braceStart, i + 1);
                }
            }
        }
        return null;
    }
}
