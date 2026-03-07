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
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class CustomAIProviderBuilder {

    private final String name;
    private String apiKeyVarName;
    private Class<? extends AIProviderClient<?>> providerClientType;
    private Function<String, Map<String, String>> headersFunction;
    private boolean allMimeTypesSupported;
    private Set<MimeType> supportedMimeTypes = Set.of();

    CustomAIProviderBuilder(String name) {
        Objects.requireNonNull(name, "Provider name cannot be null");
        this.name = name;
    }

    public CustomAIProviderBuilder apiKeyVarName(String apiKeyVarName) {
        this.apiKeyVarName = apiKeyVarName;
        return this;
    }

    public CustomAIProviderBuilder clientType(Class<? extends AIProviderClient<?>> providerClientType) {
        this.providerClientType = providerClientType;
        return this;
    }

    public CustomAIProviderBuilder headers(Function<String, Map<String, String>> headersFunction) {
        this.headersFunction = headersFunction;
        return this;
    }

    public CustomAIProviderBuilder supportedMimeTypes(Set<MimeType> supportedMimeTypes) {
        this.supportedMimeTypes = supportedMimeTypes;
        this.allMimeTypesSupported = false;
        return this;
    }

    public CustomAIProviderBuilder allMimeTypesSupported() {
        this.allMimeTypesSupported = true;
        return this;
    }

    public CustomAIProvider build() {
        Objects.requireNonNull(apiKeyVarName, "apiKeyVarName is required");
        Objects.requireNonNull(providerClientType, "clientType is required");

        Function<String, Map<String, String>> headers = this.headersFunction;
        if (headers == null) {
            headers = credential -> Map.of(
                    "Authorization", "Bearer " + credential,
                    "Content-Type", "application/json"
            );
        }

        return new CustomAIProvider(name, apiKeyVarName, providerClientType, headers,
                allMimeTypesSupported, supportedMimeTypes);
    }
}
