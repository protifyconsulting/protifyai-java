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

package ai.protify.core.internal.provider;

import ai.protify.core.internal.provider.anthropic.AnthropicClient;
import ai.protify.core.internal.provider.azure.AzureOpenAIClient;
import ai.protify.core.internal.provider.bedrock.BedrockClient;
import ai.protify.core.internal.provider.deepseek.DeepSeekClient;
import ai.protify.core.internal.provider.fireworks.FireworksClient;
import ai.protify.core.internal.provider.gemini.GeminiClient;
import ai.protify.core.internal.provider.groq.GroqClient;
import ai.protify.core.internal.provider.mistral.MistralClient;
import ai.protify.core.internal.provider.openai.OpenAIClient;
import ai.protify.core.internal.provider.together.TogetherClient;
import ai.protify.core.internal.provider.vertexai.VertexAIClient;
import ai.protify.core.internal.provider.xai.XAIClient;
import ai.protify.core.provider.AIProvider;
import ai.protify.core.provider.AIProviderClient;
import ai.protify.core.response.MimeType;

import java.util.Map;
import java.util.Set;

public enum ProtifyAIProvider implements AIProvider {
    OPEN_AI("OpenAI", OpenAIClient.class, "OPENAI_API_KEY", false, Set.of(MimeType.PDF, MimeType.PNG, MimeType.JPG, MimeType.JPEG, MimeType.WEBP, MimeType.GIF)),
    ANTHROPIC("Anthropic", AnthropicClient.class, "ANTHROPIC_API_KEY", false, Set.of(MimeType.PDF, MimeType.PNG, MimeType.JPG, MimeType.JPEG, MimeType.WEBP, MimeType.GIF)),
    GOOGLE("Gemini", GeminiClient.class, "GEMINI_API_KEY", false, Set.of(MimeType.PDF, MimeType.PNG, MimeType.JPG, MimeType.JPEG, MimeType.WEBP, MimeType.GIF, MimeType.HEIC, MimeType.HEIF)),
    MISTRAL("Mistral", MistralClient.class, "MISTRAL_API_KEY", false, Set.of(MimeType.PNG, MimeType.JPG, MimeType.JPEG, MimeType.WEBP, MimeType.GIF)),
    GROQ("Groq", GroqClient.class, "GROQ_API_KEY", false, Set.of(MimeType.PNG, MimeType.JPG, MimeType.JPEG, MimeType.WEBP)),
    DEEP_SEEK("DeepSeek", DeepSeekClient.class, "DEEPSEEK_API_KEY", false, Set.of()),
    TOGETHER("Together", TogetherClient.class, "TOGETHER_API_KEY", false, Set.of(MimeType.PNG, MimeType.JPG, MimeType.JPEG, MimeType.WEBP)),
    FIREWORKS("Fireworks", FireworksClient.class, "FIREWORKS_API_KEY", false, Set.of(MimeType.PNG, MimeType.JPG, MimeType.JPEG, MimeType.WEBP)),
    X_AI("xAI", XAIClient.class, "XAI_API_KEY", false, Set.of(MimeType.PNG, MimeType.JPG, MimeType.JPEG, MimeType.WEBP)),
    AZURE_OPEN_AI("Azure OpenAI", AzureOpenAIClient.class, "AZURE_OPENAI_API_KEY", false, Set.of(MimeType.PNG, MimeType.JPG, MimeType.JPEG, MimeType.WEBP, MimeType.GIF)),
    VERTEX_AI("Vertex AI", VertexAIClient.class, "VERTEX_AI_ACCESS_TOKEN", false, Set.of(MimeType.PDF, MimeType.PNG, MimeType.JPG, MimeType.JPEG, MimeType.WEBP, MimeType.GIF, MimeType.HEIC, MimeType.HEIF)),
    AWS_BEDROCK("AWS Bedrock", BedrockClient.class, "AWS_BEDROCK_API_KEY", false, Set.of(MimeType.PNG, MimeType.JPG, MimeType.JPEG, MimeType.WEBP, MimeType.GIF));

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
        if (this == GOOGLE) {
            return Map.of(
                    "x-goog-api-key", credential,
                    "Content-Type", "application/json"
            );
        }
        if (this == AZURE_OPEN_AI) {
            return Map.of(
                    "api-key", credential,
                    "Content-Type", "application/json"
            );
        }
        if (this == VERTEX_AI) {
            return Map.of(
                    "Authorization", "Bearer " + credential,
                    "Content-Type", "application/json"
            );
        }
        if (this == AWS_BEDROCK) {
            // Bedrock uses SigV4 signing, not a simple header-based auth.
            // Headers are set directly by BedrockClient. Return content-type only.
            return Map.of(
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
