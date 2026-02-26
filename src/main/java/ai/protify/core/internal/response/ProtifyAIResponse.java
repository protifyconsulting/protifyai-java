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

package ai.protify.core.internal.response;

import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.internal.util.json.ProtifyJsonObject;
import ai.protify.core.response.AIResponse;

import java.util.UUID;

public abstract class ProtifyAIResponse implements AIResponse {

    private final boolean cachedResponse;
    private final String responseId = UUID.randomUUID().toString();
    private final String pipelineId;
    private final String correlationId;
    private final String rawResponse;
    private final ProtifyJsonObject jsonObject;
    private final String modelName;

    protected ProtifyAIResponse(
            boolean cachedResponse,
            String pipelineId,
            String correlationId,
            String modelName,
            String rawResponse) {
        this.cachedResponse = cachedResponse;
        if (correlationId == null) {
            this.correlationId = this.responseId;
        } else {
            this.correlationId = correlationId;
        }
        this.pipelineId = pipelineId;
        this.modelName = modelName;
        this.rawResponse = rawResponse;
        this.jsonObject = ProtifyJson.parse(rawResponse);
    }

    @Override
    public String getCorrelationId() {
        if (correlationId != null) {
            return correlationId;
        }
        return getResponseId();
    }

    @Override
    public String getResponseId() {
        return this.responseId;
    }

    @Override
    public String getPipelineId() {
        return this.pipelineId;
    }

    @Override
    public String getModelName() {
        return this.modelName;
    }

    @Override
    public boolean isCachedResponse() {
        return this.cachedResponse;
    }

    @Override
    public String getProviderResponse() {
        return this.rawResponse;
    }

    @Override
    public String text() {
        return "";
    }

    protected ProtifyJsonObject getJsonObject() {
        return this.jsonObject;
    }
}
