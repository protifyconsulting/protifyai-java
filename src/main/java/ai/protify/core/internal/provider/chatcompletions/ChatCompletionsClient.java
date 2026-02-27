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

package ai.protify.core.internal.provider.chatcompletions;

import ai.protify.core.internal.config.CredentialHelper;
import ai.protify.core.internal.provider.chatcompletions.model.ChatResponseBody;
import ai.protify.core.internal.response.ProtifyAIStreamResponse;
import ai.protify.core.internal.util.http.ProtifyHttpClient;
import ai.protify.core.internal.util.http.ProtifyHttpResponse;
import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.provider.ProtifyAIProviderClient;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;

public abstract class ChatCompletionsClient<T extends ChatCompletionsRequest> extends ProtifyAIProviderClient<T> {

    protected abstract String getEndpointUrl();

    @Override
    public AIResponse execute(T request) {
        ProtifyHttpResponse response = ProtifyHttpClient.getInstance().post(request, getEndpointUrl());
        String rawJson = response.getResponseBody();
        ChatResponseBody body = ProtifyJson.fromJson(rawJson, ChatResponseBody.class);
        return new ChatCompletionsResponse(response.isCachedResponse(), null, null,
                super.getModelName(), rawJson, body);
    }

    @Override
    public AIStreamResponse executeStream(T request) {
        request.setStream(true);

        ProtifyAIStreamResponse streamResponse = new ProtifyAIStreamResponse();

        ProtifyHttpClient.getInstance().postStream(request, getEndpointUrl(),
                data -> {
                    if ("[DONE]".equals(data.trim())) {
                        return;
                    }
                    String content = CredentialHelper.extractJsonString(data, "content");
                    if (content != null) {
                        streamResponse.pushToken(content);
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
