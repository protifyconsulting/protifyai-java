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

package com.protify.ai.internal.provider.anthropic;

import com.protify.ai.internal.config.CredentialHelper;
import com.protify.ai.internal.provider.anthropic.model.AnthropicResponseBody;
import com.protify.ai.internal.response.ProtifyAIStreamResponse;
import com.protify.ai.internal.util.http.ProtifyHttpClient;
import com.protify.ai.internal.util.http.ProtifyHttpResponse;
import com.protify.ai.internal.util.json.ProtifyJson;
import com.protify.ai.provider.ProtifyAIProviderClient;
import com.protify.ai.response.AIResponse;
import com.protify.ai.response.AIStreamResponse;

public class AnthropicClient extends ProtifyAIProviderClient<AnthropicRequest> {

    private static final String MESSAGES_URI = "https://api.anthropic.com/v1/messages";

    @Override
    public AIResponse execute(AnthropicRequest request) {
        ProtifyHttpResponse response = ProtifyHttpClient.getInstance().post(request, MESSAGES_URI);
        String rawJson = response.getResponseBody();
        AnthropicResponseBody body = ProtifyJson.fromJson(rawJson, AnthropicResponseBody.class);
        return new AnthropicResponse(response.isCachedResponse(), null, null, super.getModelName(), rawJson, body);
    }

    @Override
    public AIStreamResponse executeStream(AnthropicRequest request) {
        request.setStream(true);

        ProtifyAIStreamResponse streamResponse = new ProtifyAIStreamResponse();
        String[] messageJson = {null};

        ProtifyHttpClient.getInstance().postStream(request, MESSAGES_URI,
                data -> {
                    String type = CredentialHelper.extractJsonString(data, "type");
                    if ("content_block_delta".equals(type)) {
                        String text = CredentialHelper.extractJsonString(data, "text");
                        if (text != null) {
                            streamResponse.pushToken(text);
                        }
                    } else if ("message_start".equals(type)) {
                        messageJson[0] = data;
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
