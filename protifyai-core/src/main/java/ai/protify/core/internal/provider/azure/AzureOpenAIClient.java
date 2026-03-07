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

package ai.protify.core.internal.provider.azure;

import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.provider.chatcompletions.ChatCompletionsClient;

public class AzureOpenAIClient extends ChatCompletionsClient<AzureOpenAIRequest> {

    private static final String DEFAULT_API_VERSION = "2024-06-01";

    @Override
    protected String getEndpointUrl() {
        String resourceName = super.getConfiguration().getProperty(AIConfigProperty.RESOURCE_NAME);
        String deploymentName = super.getConfiguration().getProperty(AIConfigProperty.DEPLOYMENT_NAME);
        String apiVersion = super.getConfiguration().getProperty(AIConfigProperty.API_VERSION);

        if (resourceName == null || resourceName.isEmpty()) {
            throw new IllegalStateException(
                    "Azure OpenAI requires a resource name. Set it via .resourceName() on the builder.");
        }
        if (deploymentName == null || deploymentName.isEmpty()) {
            throw new IllegalStateException(
                    "Azure OpenAI requires a deployment name. Set it via .deploymentName() on the builder.");
        }
        if (apiVersion == null || apiVersion.isEmpty()) {
            apiVersion = DEFAULT_API_VERSION;
        }

        return "https://" + resourceName + ".openai.azure.com/openai/deployments/"
                + deploymentName + "/chat/completions?api-version=" + apiVersion;
    }
}
