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
import java.util.Set;
import java.util.function.Function;

public class CustomAIProvider implements AIProvider {

    private final String name;
    private final String apiKeyVarName;
    private final Class<? extends AIProviderClient<?>> providerClientType;
    private final Function<String, Map<String, String>> headersFunction;
    private final boolean allMimeTypesSupported;
    private final Set<MimeType> supportedMimeTypes;

    CustomAIProvider(String name,
                     String apiKeyVarName,
                     Class<? extends AIProviderClient<?>> providerClientType,
                     Function<String, Map<String, String>> headersFunction,
                     boolean allMimeTypesSupported,
                     Set<MimeType> supportedMimeTypes) {
        this.name = name;
        this.apiKeyVarName = apiKeyVarName;
        this.providerClientType = providerClientType;
        this.headersFunction = headersFunction;
        this.allMimeTypesSupported = allMimeTypesSupported;
        this.supportedMimeTypes = supportedMimeTypes;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getApiKeyVarName() {
        return apiKeyVarName;
    }

    @Override
    @SuppressWarnings("java:S1452")
    public Class<? extends AIProviderClient<?>> getProviderClientType() {
        return providerClientType;
    }

    @Override
    public Map<String, String> getHeaders(String credential) {
        return headersFunction.apply(credential);
    }

    @Override
    public boolean isMimeTypeSupported(MimeType mimeType) {
        if (allMimeTypesSupported) return true;
        return supportedMimeTypes.contains(mimeType);
    }

    @Override
    public String toString() {
        return "AIProvider{name='" + name + "'}";
    }
}
