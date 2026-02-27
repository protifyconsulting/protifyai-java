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

package ai.protify.core.internal.provider.vertexai;

import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.config.CredentialHelper;
import ai.protify.core.internal.provider.gemini.GeminiRequest;
import ai.protify.core.internal.provider.gemini.GeminiResponse;
import ai.protify.core.internal.provider.gemini.model.GeminiResponseBody;
import ai.protify.core.internal.response.ProtifyAIStreamResponse;
import ai.protify.core.internal.util.http.ProtifyHttpClient;
import ai.protify.core.internal.util.http.ProtifyHttpResponse;
import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.provider.ProtifyAIProviderClient;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;

public class VertexAIClient extends ProtifyAIProviderClient<GeminiRequest> {

    private String getGenerateContentUrl() {
        String region = super.getConfiguration().getProperty(AIConfigProperty.REGION);
        String projectId = super.getConfiguration().getProperty(AIConfigProperty.PROJECT_ID);

        if (region == null || region.isEmpty()) {
            throw new IllegalStateException(
                    "Vertex AI requires a region. Set it via .region() on the builder.");
        }
        if (projectId == null || projectId.isEmpty()) {
            throw new IllegalStateException(
                    "Vertex AI requires a project ID. Set it via .projectId() on the builder.");
        }

        return "https://" + region + "-aiplatform.googleapis.com/v1/projects/" + projectId
                + "/locations/" + region + "/publishers/google/models/"
                + super.getModelName() + ":generateContent";
    }

    private String getStreamGenerateContentUrl() {
        String region = super.getConfiguration().getProperty(AIConfigProperty.REGION);
        String projectId = super.getConfiguration().getProperty(AIConfigProperty.PROJECT_ID);

        if (region == null || region.isEmpty()) {
            throw new IllegalStateException(
                    "Vertex AI requires a region. Set it via .region() on the builder.");
        }
        if (projectId == null || projectId.isEmpty()) {
            throw new IllegalStateException(
                    "Vertex AI requires a project ID. Set it via .projectId() on the builder.");
        }

        return "https://" + region + "-aiplatform.googleapis.com/v1/projects/" + projectId
                + "/locations/" + region + "/publishers/google/models/"
                + super.getModelName() + ":streamGenerateContent?alt=sse";
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
