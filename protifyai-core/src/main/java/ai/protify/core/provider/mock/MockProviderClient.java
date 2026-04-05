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

package ai.protify.core.provider.mock;

import ai.protify.core.internal.response.ProtifyAIStreamResponse;
import ai.protify.core.provider.ProtifyAIProviderClient;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;

public class MockProviderClient extends ProtifyAIProviderClient<MockProviderRequest> {

    public MockProviderClient() { }

    @Override
    public AIResponse execute(MockProviderRequest request) {
        MockProvider mockProvider = (MockProvider) getProvider();
        mockProvider.recordRequest(request);
        return mockProvider.nextResponse(request);
    }

    @Override
    public AIStreamResponse executeStream(MockProviderRequest request) {
        MockProvider mockProvider = (MockProvider) getProvider();
        mockProvider.recordRequest(request);
        AIResponse response = mockProvider.nextResponse(request);

        ProtifyAIStreamResponse stream = new ProtifyAIStreamResponse();

        Thread tokenThread = new Thread(() -> {
            String text = response.text();
            if (text != null && !text.isEmpty()) {
                long delay = mockProvider.getStreamTokenDelayMillis();
                for (int i = 0; i < text.length(); i++) {
                    try {
                        Thread.sleep(Math.max(delay, 1));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    stream.pushToken(String.valueOf(text.charAt(i)));
                }
            }
            stream.complete(response);
        });
        tokenThread.setDaemon(true);
        tokenThread.start();

        return stream;
    }
}
