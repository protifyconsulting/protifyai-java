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

package ai.protify.core;

import ai.protify.core.internal.ProtifyAIClient;
import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.provider.AIProvider;
import ai.protify.core.internal.util.Logger;
import ai.protify.core.internal.util.LoggerFactory;
import ai.protify.core.resiliency.RetryPolicy;

import java.util.EnumMap;
import java.util.Map;


public class AIClientBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIClientBuilder.class);

    private final Map<AIConfigProperty, Object> clientProperties = new EnumMap<>(AIConfigProperty.class);
    private AIModel model;
    private AIProvider provider;
    private String explicitModelVersion;

    public AIClientBuilder apiKey(String apiKey) {
        LOGGER.debug("API key set");
        clientProperties.put(AIConfigProperty.PROVIDER_API_KEY, apiKey);
        return this;
    }

    public AIClientBuilder model(AIModel model) {
        LOGGER.debug("Model set to {}", model.getName());
        this.model = model;
        return this;
    }

    public AIClientBuilder provider(AIProvider provider) {
        LOGGER.debug("Provider set to {}", provider.getName());
        this.provider = provider;
        return this;
    }

    public AIClientBuilder explicitModelVersion(String modelVersion) {
        LOGGER.debug("Explicit model version set to {}", modelVersion);
        this.explicitModelVersion = modelVersion;
        return this;
    }

    public AIClientBuilder instructions(String instructions) {
        LOGGER.debug("Instructions set to {}", instructions);
        clientProperties.put(AIConfigProperty.INSTRUCTIONS, instructions);
        return this;
    }

    public AIClientBuilder temperature(double temperature) {
        LOGGER.debug("Temperature set to {}", temperature);
        clientProperties.put(AIConfigProperty.TEMPERATURE, temperature);
        return this;
    }

    public AIClientBuilder topP(double topP) {
        LOGGER.debug("Top-p set to {}", topP);
        clientProperties.put(AIConfigProperty.TOP_P, topP);
        return this;
    }

    public AIClientBuilder topK(int topK) {
        LOGGER.debug("Top-k set to {}", topK);
        clientProperties.put(AIConfigProperty.TOP_K, topK);
        return this;
    }

    public AIClientBuilder maxOutputTokens(int maxTokens) {
        LOGGER.debug("Max output tokens set to {}", maxTokens);
        clientProperties.put(AIConfigProperty.MAX_OUTPUT_TOKENS, maxTokens);
        return this;
    }

    public AIClientBuilder logRequests(boolean logRequests) {
        LOGGER.debug("Log requests set to {}", logRequests);
        clientProperties.put(AIConfigProperty.LOG_REQUESTS, logRequests);
        return this;
    }

    public AIClientBuilder logResponses(boolean logResponses) {
        LOGGER.debug("Log responses set to {}", logResponses);
        clientProperties.put(AIConfigProperty.LOG_RESPONSES, logResponses);
        return this;
    }

    public AIClientBuilder prettyPrint(boolean prettyPrint) {
        LOGGER.debug("JSON pretty print set to {}", prettyPrint);
        clientProperties.put(AIConfigProperty.PRETTY_PRINT_JSON, prettyPrint);
        return this;
    }

    public AIClientBuilder truncateLongRequestInputs(boolean truncate) {
        LOGGER.debug("Truncate large inputs set to {}", truncate);
        clientProperties.put(AIConfigProperty.LOG_TRUNCATE_LARGE_INPUT, truncate);
        return this;
    }

    public AIClientBuilder overridePipelineConfig(boolean override) {
        clientProperties.put(AIConfigProperty.OVERRIDE_PIPELINE_CONFIG, override);
        return this;
    }

    public AIClientBuilder retryPolicy(RetryPolicy retryPolicy) {
        clientProperties.put(AIConfigProperty.RETRY_POLICY, retryPolicy);
        return this;
    }

    public AIClientBuilder region(String region) {
        LOGGER.debug("Region set to {}", region);
        clientProperties.put(AIConfigProperty.REGION, region);
        return this;
    }

    public AIClientBuilder projectId(String projectId) {
        LOGGER.debug("Project ID set");
        clientProperties.put(AIConfigProperty.PROJECT_ID, projectId);
        return this;
    }

    public AIClientBuilder resourceName(String resourceName) {
        LOGGER.debug("Resource name set to {}", resourceName);
        clientProperties.put(AIConfigProperty.RESOURCE_NAME, resourceName);
        return this;
    }

    public AIClientBuilder deploymentName(String deploymentName) {
        LOGGER.debug("Deployment name set to {}", deploymentName);
        clientProperties.put(AIConfigProperty.DEPLOYMENT_NAME, deploymentName);
        return this;
    }

    public AIClientBuilder apiVersion(String apiVersion) {
        LOGGER.debug("API version set to {}", apiVersion);
        clientProperties.put(AIConfigProperty.API_VERSION, apiVersion);
        return this;
    }

    public AIClientBuilder awsAccessKeyId(String accessKeyId) {
        LOGGER.debug("AWS access key ID set");
        clientProperties.put(AIConfigProperty.AWS_ACCESS_KEY_ID, accessKeyId);
        return this;
    }

    public AIClientBuilder awsSecretAccessKey(String secretAccessKey) {
        LOGGER.debug("AWS secret access key set");
        clientProperties.put(AIConfigProperty.AWS_SECRET_ACCESS_KEY, secretAccessKey);
        return this;
    }

    public AIClientBuilder awsSessionToken(String sessionToken) {
        LOGGER.debug("AWS session token set");
        clientProperties.put(AIConfigProperty.AWS_SESSION_TOKEN, sessionToken);
        return this;
    }

    public AIClient build() {
        LOGGER.debug("Building client");

        String modelName;
        if (model != null) {
            modelName = model.getName();
            provider = model.getProvider();
            if (explicitModelVersion != null) {
                LOGGER.warn("Explicit model version is ignored when an AIModel instance is provided.");
            }
            if (provider != null && model.getProvider() != provider) {
                LOGGER.warn("Provided AIProvider does not match the AIModel provider.  AIModel provider will be used.");
            }
        } else if (explicitModelVersion == null || provider == null) {
            throw new IllegalArgumentException("An AIModel instance, or an AIProvider instance and explicitModelVersion must be provided.");
        } else {
            modelName = explicitModelVersion;
        }
        return new ProtifyAIClient(clientProperties, provider, modelName);
    }

    @Override
    public String toString() {
        return "AIClientBuilder{" +
                ", model=" + model +
                ", provider=" + provider +
                ", explicitModelVersion='" + explicitModelVersion + '\'' +
                '}';
    }
}
