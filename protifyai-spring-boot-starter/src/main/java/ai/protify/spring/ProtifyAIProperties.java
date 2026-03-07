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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "protify.ai")
public class ProtifyAIProperties {

    private boolean enabled = true;

    private String serviceScanPackage;

    private ClientProperties defaults = new ClientProperties();

    private Map<String, ClientProperties> clients = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceScanPackage() {
        return serviceScanPackage;
    }

    public void setServiceScanPackage(String serviceScanPackage) {
        this.serviceScanPackage = serviceScanPackage;
    }

    public ClientProperties getDefaults() {
        return defaults;
    }

    public void setDefaults(ClientProperties defaults) {
        this.defaults = defaults;
    }

    public Map<String, ClientProperties> getClients() {
        return clients;
    }

    public void setClients(Map<String, ClientProperties> clients) {
        this.clients = clients;
    }

    public static class ClientProperties {

        private String provider;
        private String apiKey;
        private String model;
        private String modelVersion;

        // Client defaults
        private Integer maxOutputTokens;
        private Double temperature;
        private Double topP;
        private Integer topK;
        private String reasoningEffort;
        private String instructions;

        // Logging
        private Boolean logRequests;
        private Boolean logResponses;
        private Boolean prettyPrint;
        private Boolean truncateLongRequestInputs;

        // Request
        private Integer requestTimeoutMs;

        // Retry
        private RetryProperties retry;

        // Cloud provider
        private String region;
        private String projectId;
        private String resourceName;
        private String deploymentName;
        private String apiVersion;

        // AWS
        private String awsAccessKeyId;
        private String awsSecretAccessKey;
        private String awsSessionToken;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getModelVersion() {
            return modelVersion;
        }

        public void setModelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
        }

        public Integer getMaxOutputTokens() {
            return maxOutputTokens;
        }

        public void setMaxOutputTokens(Integer maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Double getTopP() {
            return topP;
        }

        public void setTopP(Double topP) {
            this.topP = topP;
        }

        public Integer getTopK() {
            return topK;
        }

        public void setTopK(Integer topK) {
            this.topK = topK;
        }

        public String getReasoningEffort() {
            return reasoningEffort;
        }

        public void setReasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
        }

        public String getInstructions() {
            return instructions;
        }

        public void setInstructions(String instructions) {
            this.instructions = instructions;
        }

        public Boolean getLogRequests() {
            return logRequests;
        }

        public void setLogRequests(Boolean logRequests) {
            this.logRequests = logRequests;
        }

        public Boolean getLogResponses() {
            return logResponses;
        }

        public void setLogResponses(Boolean logResponses) {
            this.logResponses = logResponses;
        }

        public Boolean getPrettyPrint() {
            return prettyPrint;
        }

        public void setPrettyPrint(Boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
        }

        public Boolean getTruncateLongRequestInputs() {
            return truncateLongRequestInputs;
        }

        public void setTruncateLongRequestInputs(Boolean truncateLongRequestInputs) {
            this.truncateLongRequestInputs = truncateLongRequestInputs;
        }

        public Integer getRequestTimeoutMs() {
            return requestTimeoutMs;
        }

        public void setRequestTimeoutMs(Integer requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
        }

        public RetryProperties getRetry() {
            return retry;
        }

        public void setRetry(RetryProperties retry) {
            this.retry = retry;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getResourceName() {
            return resourceName;
        }

        public void setResourceName(String resourceName) {
            this.resourceName = resourceName;
        }

        public String getDeploymentName() {
            return deploymentName;
        }

        public void setDeploymentName(String deploymentName) {
            this.deploymentName = deploymentName;
        }

        public String getApiVersion() {
            return apiVersion;
        }

        public void setApiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
        }

        public String getAwsAccessKeyId() {
            return awsAccessKeyId;
        }

        public void setAwsAccessKeyId(String awsAccessKeyId) {
            this.awsAccessKeyId = awsAccessKeyId;
        }

        public String getAwsSecretAccessKey() {
            return awsSecretAccessKey;
        }

        public void setAwsSecretAccessKey(String awsSecretAccessKey) {
            this.awsSecretAccessKey = awsSecretAccessKey;
        }

        public String getAwsSessionToken() {
            return awsSessionToken;
        }

        public void setAwsSessionToken(String awsSessionToken) {
            this.awsSessionToken = awsSessionToken;
        }
    }

    public static class RetryProperties {
        private Integer maxRetries;
        private String backoffStrategy;
        private Long delayMillis;
        private Long jitterMillis;
        private Long maxDelayMillis;
        private Long maxElapsedTimeMillis;
        private Boolean respectRetryAfter;

        public Integer getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
        }

        public String getBackoffStrategy() {
            return backoffStrategy;
        }

        public void setBackoffStrategy(String backoffStrategy) {
            this.backoffStrategy = backoffStrategy;
        }

        public Long getDelayMillis() {
            return delayMillis;
        }

        public void setDelayMillis(Long delayMillis) {
            this.delayMillis = delayMillis;
        }

        public Long getJitterMillis() {
            return jitterMillis;
        }

        public void setJitterMillis(Long jitterMillis) {
            this.jitterMillis = jitterMillis;
        }

        public Long getMaxDelayMillis() {
            return maxDelayMillis;
        }

        public void setMaxDelayMillis(Long maxDelayMillis) {
            this.maxDelayMillis = maxDelayMillis;
        }

        public Long getMaxElapsedTimeMillis() {
            return maxElapsedTimeMillis;
        }

        public void setMaxElapsedTimeMillis(Long maxElapsedTimeMillis) {
            this.maxElapsedTimeMillis = maxElapsedTimeMillis;
        }

        public Boolean getRespectRetryAfter() {
            return respectRetryAfter;
        }

        public void setRespectRetryAfter(Boolean respectRetryAfter) {
            this.respectRetryAfter = respectRetryAfter;
        }
    }
}
