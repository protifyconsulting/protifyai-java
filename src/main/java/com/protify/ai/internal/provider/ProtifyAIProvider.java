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

package com.protify.ai.internal.provider;

import com.protify.ai.internal.provider.anthropic.AnthropicClient;
import com.protify.ai.internal.provider.openai.OpenAIClient;
import com.protify.ai.provider.AIProvider;
import com.protify.ai.provider.AIProviderClient;
import com.protify.ai.response.MimeType;

import java.util.Map;
import java.util.Set;

public enum ProtifyAIProvider implements AIProvider {
    OPEN_AI("OpenAI", OpenAIClient.class, "OPENAI_API_KEY", false, Set.of(MimeType.PDF, MimeType.PNG, MimeType.JPG, MimeType.JPEG, MimeType.WEBP, MimeType.GIF)),
    ANTHROPIC("Anthropic", AnthropicClient.class, "ANTHROPIC_API_KEY", false, Set.of(MimeType.PDF, MimeType.PNG, MimeType.JPG, MimeType.JPEG, MimeType.WEBP, MimeType.GIF)),
    GOOGLE("Gemini", OpenAIClient.class, "GEMINI_API_KEY", false, Set.of(MimeType.PDF, MimeType.PNG, MimeType.JPG, MimeType.JPEG, MimeType.WEBP, MimeType.GIF, MimeType.HEIC, MimeType.HEIF)),
    MISTRAL("Mistral", OpenAIClient.class, "MISTRAL_API_KEY", false, Set.of(MimeType.PDF, MimeType.PNG, MimeType.JPG, MimeType.JPEG)),
    GROQ("Groq", OpenAIClient.class, "GROQ_API_KEY", false, Set.of(MimeType.PDF, MimeType.PNG, MimeType.JPG, MimeType.JPEG)),
    DEEP_SEEK("DeepSeek", OpenAIClient.class, "DEEPSEEK_API_KEY", false, Set.of(MimeType.PDF, MimeType.PNG, MimeType.JPG, MimeType.JPEG)),
    X_AI("xAI", OpenAIClient.class, "XAI_API_KEY", false, Set.of(MimeType.PDF, MimeType.PNG, MimeType.JPG, MimeType.JPEG));

    private final String name;
    private final Class<? extends AIProviderClient<?>> providerClientType;
    private final String apiKeyVarName;
    private final boolean allMimeTypesSupported;
    private final Set<MimeType> supportedMimeTypes;

    ProtifyAIProvider(String name,
                      Class<? extends AIProviderClient<?>> providerClientType,
                      String apiKeyVarName, boolean allMimeTypesSupported, Set<MimeType> supportedMimeTypes) {
        this.name = name;
        this.providerClientType = providerClientType;
        this.apiKeyVarName = apiKeyVarName;
        this.allMimeTypesSupported = allMimeTypesSupported;
        this.supportedMimeTypes = supportedMimeTypes;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getApiKeyVarName() {
        return apiKeyVarName;
    }

    @Override
    public boolean isMimeTypeSupported(MimeType mimeType) {
        if (allMimeTypesSupported) return true;
        return supportedMimeTypes.contains(mimeType);
    }

    @Override
    public Class<? extends AIProviderClient<?>> getProviderClientType() {
        return providerClientType;
    }

    @Override
    public Map<String, String> getHeaders(String credential) {
        if (this == ANTHROPIC) {
            return Map.of(
                    "x-api-key", credential,
                    "anthropic-version", "2023-06-01",
                    "Content-Type", "application/json"
            );
        }
        return Map.of(
                "Authorization", "Bearer " + credential,
                "Content-Type", "application/json"
        );
    }

    @Override
    public String toString() {
        return "AIProvider{" +
                "name='" + name + '\'' +
                '}';
    }
}
