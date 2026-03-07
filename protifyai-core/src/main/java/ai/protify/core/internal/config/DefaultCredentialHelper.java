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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultCredentialHelper implements CredentialHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCredentialHelper.class);
    private static final String CREDS_FILE = ".creds";
    private static volatile Map<String, String> credsFileCache;

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

        // 3. .creds file on the classpath
        if (provider != null) {
            String apiKeyFromFile = loadCredsFile().get(provider.getApiKeyVarName());
            if (apiKeyFromFile != null && !apiKeyFromFile.isEmpty()) {
                return apiKeyFromFile;
            }
        }

        throw new IllegalArgumentException("No API key found for provider " +
                (provider != null ? provider.getName() : "unknown") +
                ". Set the " + (provider != null ? provider.getApiKeyVarName() : "") +
                " environment variable, configure providers.apiKey, or add it to a .creds file on the classpath.");
    }

    private static Map<String, String> loadCredsFile() {
        if (credsFileCache != null) {
            return credsFileCache;
        }
        synchronized (DefaultCredentialHelper.class) {
            if (credsFileCache != null) {
                return credsFileCache;
            }
            Map<String, String> entries = new ConcurrentHashMap<>();
            InputStream is = DefaultCredentialHelper.class.getClassLoader().getResourceAsStream(CREDS_FILE);
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String key = line.substring(0, eq).trim();
                            String value = line.substring(eq + 1).trim();
                            entries.put(key, value);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to read .creds file from classpath: {}", e.getMessage());
                }
            }
            credsFileCache = entries;
            return credsFileCache;
        }
    }
}
