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

package ai.protify.core.provider;

import ai.protify.core.response.MimeType;

import java.util.Map;


public interface AIProvider {

    String getName();

    Map<String, String> getHeaders(String credential);

    @SuppressWarnings({"java:S1452"})
    Class<? extends AIProviderClient<?>> getProviderClientType();

    boolean isMimeTypeSupported(MimeType mimeType);

    String getApiKeyVarName();

    static CustomAIProviderBuilder custom(String name) {
        return new CustomAIProviderBuilder(name);
    }
}
