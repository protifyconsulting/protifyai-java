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

package ai.protify.core.internal.provider.bedrock;

import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.exception.ProtifyApiException;
import ai.protify.core.internal.util.http.ProtifyHttpClient;
import ai.protify.core.internal.provider.bedrock.auth.AwsCredentialResolver;
import ai.protify.core.internal.provider.bedrock.auth.AwsCredentials;
import ai.protify.core.internal.provider.bedrock.auth.AwsSigV4Signer;
import ai.protify.core.internal.provider.bedrock.model.BedrockResponseBody;
import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.provider.ProtifyAIProviderClient;
import ai.protify.core.response.AIResponse;
import ai.protify.core.response.AIStreamResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class BedrockClient extends ProtifyAIProviderClient<BedrockRequest> {

    private static final String SERVICE = "bedrock";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String getEndpointUrl() {
        String region = super.getConfiguration().getProperty(AIConfigProperty.REGION);
        if (region == null || region.isEmpty()) {
            throw new IllegalStateException(
                    "AWS Bedrock requires a region. Set it via .region() on the builder.");
        }
        return "https://bedrock-runtime." + region + ".amazonaws.com/model/"
                + super.getModelName() + "/converse";
    }

    @Override
    public AIResponse execute(BedrockRequest request) {
        String region = super.getConfiguration().getProperty(AIConfigProperty.REGION);
        if (region == null || region.isEmpty()) {
            throw new IllegalStateException(
                    "AWS Bedrock requires a region. Set it via .region() on the builder.");
        }

        AwsCredentials credentials = AwsCredentialResolver.resolve(super.getConfiguration());
        String endpointUrl = getEndpointUrl();
        URI uri = URI.create(endpointUrl);
        String jsonBody = request.toJson();

        int timeoutMillis = super.getConfiguration().getProperty(AIConfigProperty.REQUEST_TIMEOUT_MS);

        // Sign the request with SigV4
        Map<String, String> sigV4Headers = AwsSigV4Signer.sign(
                "POST", uri, Map.of("content-type", "application/json"),
                jsonBody, credentials, region, SERVICE);

        // Build the HTTP request
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(timeoutMillis))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        sigV4Headers.forEach(reqBuilder::header);

        try {
            HttpResponse<String> response = httpClient.send(
                    reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw ProtifyHttpClient.createApiException(response.statusCode(), response.body());
            }

            String rawJson = response.body();
            BedrockResponseBody body = ProtifyJson.fromJson(rawJson, BedrockResponseBody.class);
            return new BedrockResponse(false, null, null, super.getModelName(), rawJson, body);
        } catch (ProtifyApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ProtifyApiException("Failed to execute Bedrock request", e);
        }
    }

    @Override
    public AIStreamResponse executeStream(BedrockRequest request) {
        throw new UnsupportedOperationException(
                "Bedrock streaming is not yet supported. Bedrock uses a binary event stream format, "
                + "not SSE. Use execute() for non-streaming requests.");
    }
}
