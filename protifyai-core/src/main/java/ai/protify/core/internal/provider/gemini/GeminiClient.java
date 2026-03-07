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

package ai.protify.core.internal.provider.gemini;

import ai.protify.core.internal.config.CredentialHelper;
import ai.protify.core.internal.provider.gemini.model.GeminiResponseBody;
import ai.protify.core.internal.response.ProtifyAIStreamResponse;
import ai.protify.core.internal.util.http.ProtifyHttpClient;
import ai.protify.core.internal.util.http.ProtifyHttpResponse;
import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.provider.ProtifyAIProviderClient;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;

public class GeminiClient extends ProtifyAIProviderClient<GeminiRequest> {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private String getGenerateContentUrl() {
        return BASE_URL + super.getModelName() + ":generateContent";
    }

    private String getStreamGenerateContentUrl() {
        return BASE_URL + super.getModelName() + ":streamGenerateContent?alt=sse";
    }

    @Override
    public AIResponse execute(GeminiRequest request) {
        ProtifyHttpResponse response = ProtifyHttpClient.getInstance().post(request, getGenerateContentUrl());
        String rawJson = response.getResponseBody();
        GeminiResponseBody body = ProtifyJson.fromJson(rawJson, GeminiResponseBody.class);
        return new GeminiResponse(response.isCachedResponse(), null, null, super.getModelName(), rawJson, body);
    }

    @Override
    public AIStreamResponse executeStream(GeminiRequest request) {
        ProtifyAIStreamResponse streamResponse = new ProtifyAIStreamResponse();

        ProtifyHttpClient.getInstance().postStream(request, getStreamGenerateContentUrl(),
                data -> {
                    // Gemini streaming returns JSON chunks with candidates[0].content.parts[0].text
                    String text = CredentialHelper.extractJsonString(data, "text");
                    if (text != null) {
                        streamResponse.pushToken(text);
                    }
                },
                () -> streamResponse.completeWithAccumulatedText()
        ).exceptionally(ex -> {
            streamResponse.completeExceptionally(ex);
            return null;
        });

        return streamResponse;
    }
}
