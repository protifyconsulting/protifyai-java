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

package ai.protify.core.internal.config;

import ai.protify.core.provider.AIProvider;
import ai.protify.core.internal.util.Logger;
import ai.protify.core.internal.util.LoggerFactory;

public class DefaultCredentialHelper implements CredentialHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCredentialHelper.class);

    @Override
    public String getCredential(AIProvider provider, Configuration config) {
        // 1. Environment variable
        String apiKeyFromEnv = (provider != null) ? System.getenv(provider.getApiKeyVarName()) : null;
        String apiKeyFromConfig = config.getProperty(AIConfigProperty.PROVIDER_API_KEY);

        if (apiKeyFromEnv != null) {
            if (apiKeyFromConfig != null) {
                LOGGER.warn("Provider {} has both environment variable and configuration API key set. " +
                        "Using environment variable.", provider.getName());
            }
            return apiKeyFromEnv;
        }

        // 2. Direct config property
        if (apiKeyFromConfig != null) {
            return apiKeyFromConfig;
        }

        throw new IllegalArgumentException("No API key found for provider " +
                (provider != null ? provider.getName() : "unknown") +
                ". Set the " + (provider != null ? provider.getApiKeyVarName() : "") +
                " environment variable or configure providers.apiKey.");
    }
}
