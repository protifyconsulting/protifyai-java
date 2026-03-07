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

package ai.protify.spring;

import ai.protify.core.AIClient;
import ai.protify.core.AIClientBuilder;
import ai.protify.core.ReasoningEffort;
import ai.protify.core.internal.provider.ProtifyAIProvider;
import ai.protify.core.resiliency.RetryBackoffStrategy;
import ai.protify.core.resiliency.RetryPolicy;
import ai.protify.core.resiliency.RetryPolicyBuilder;

class AIClientFactory {

    private AIClientFactory() {}

    static AIClient createClient(ProtifyAIProperties.ClientProperties props) {
        if (props.getProvider() == null) {
            throw new IllegalArgumentException(
                    "Protify AI client requires a provider to be configured. " +
                    "Set 'protify.ai.defaults.provider' or 'protify.ai.clients.<name>.provider'.");
        }

        ProtifyAIProvider provider = resolveProvider(props.getProvider());
        AIClientBuilder builder = AIClient.builder().provider(provider);

        if (props.getModel() != null) {
            builder.explicitModelVersion(props.getModel());
        } else if (props.getModelVersion() != null) {
            builder.explicitModelVersion(props.getModelVersion());
        } else {
            throw new IllegalArgumentException(
                    "Protify AI client requires a model to be configured. " +
                    "Set 'protify.ai.defaults.model' or 'protify.ai.clients.<name>.model'.");
        }

        if (props.getApiKey() != null) {
            builder.apiKey(props.getApiKey());
        }
        if (props.getMaxOutputTokens() != null) {
            builder.maxOutputTokens(props.getMaxOutputTokens());
        }
        if (props.getTemperature() != null) {
            builder.temperature(props.getTemperature());
        }
        if (props.getTopP() != null) {
            builder.topP(props.getTopP());
        }
        if (props.getTopK() != null) {
            builder.topK(props.getTopK());
        }
        if (props.getReasoningEffort() != null) {
            builder.reasoningEffort(ReasoningEffort.valueOf(props.getReasoningEffort().toUpperCase()));
        }
        if (props.getInstructions() != null) {
            builder.instructions(props.getInstructions());
        }
        if (props.getLogRequests() != null) {
            builder.logRequests(props.getLogRequests());
        }
        if (props.getLogResponses() != null) {
            builder.logResponses(props.getLogResponses());
        }
        if (props.getPrettyPrint() != null) {
            builder.prettyPrint(props.getPrettyPrint());
        }
        if (props.getTruncateLongRequestInputs() != null) {
            builder.truncateLongRequestInputs(props.getTruncateLongRequestInputs());
        }
        if (props.getRegion() != null) {
            builder.region(props.getRegion());
        }
        if (props.getProjectId() != null) {
            builder.projectId(props.getProjectId());
        }
        if (props.getResourceName() != null) {
            builder.resourceName(props.getResourceName());
        }
        if (props.getDeploymentName() != null) {
            builder.deploymentName(props.getDeploymentName());
        }
        if (props.getApiVersion() != null) {
            builder.apiVersion(props.getApiVersion());
        }
        if (props.getAwsAccessKeyId() != null) {
            builder.awsAccessKeyId(props.getAwsAccessKeyId());
        }
        if (props.getAwsSecretAccessKey() != null) {
            builder.awsSecretAccessKey(props.getAwsSecretAccessKey());
        }
        if (props.getAwsSessionToken() != null) {
            builder.awsSessionToken(props.getAwsSessionToken());
        }

        if (props.getRetry() != null) {
            builder.retryPolicy(buildRetryPolicy(props.getRetry()));
        }

        return builder.build();
    }

    private static RetryPolicy buildRetryPolicy(ProtifyAIProperties.RetryProperties retry) {
        RetryPolicyBuilder builder = RetryPolicy.builder();
        if (retry.getMaxRetries() != null) {
            builder.maxRetries(retry.getMaxRetries());
        }
        if (retry.getBackoffStrategy() != null) {
            builder.backoffStrategy(RetryBackoffStrategy.valueOf(retry.getBackoffStrategy().toUpperCase()));
        }
        if (retry.getDelayMillis() != null) {
            builder.delayMillis(retry.getDelayMillis());
        }
        if (retry.getJitterMillis() != null) {
            builder.jitterMillis(retry.getJitterMillis());
        }
        if (retry.getMaxDelayMillis() != null) {
            builder.maxDelayMillis(retry.getMaxDelayMillis());
        }
        if (retry.getMaxElapsedTimeMillis() != null) {
            builder.maxElapsedTimeMillis(retry.getMaxElapsedTimeMillis());
        }
        if (retry.getRespectRetryAfter() != null) {
            builder.respectRetryAfter(retry.getRespectRetryAfter());
        }
        return builder.build();
    }

    private static ProtifyAIProvider resolveProvider(String providerName) {
        String normalized = providerName.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        for (ProtifyAIProvider p : ProtifyAIProvider.values()) {
            if (p.name().equals(normalized) || p.getName().equalsIgnoreCase(providerName.trim())) {
                return p;
            }
        }
        throw new IllegalArgumentException(
                "Unknown provider: '" + providerName + "'. Supported providers: " +
                java.util.Arrays.toString(ProtifyAIProvider.values()));
    }
}
