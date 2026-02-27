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

package ai.protify.core.internal.provider.gemini;

import ai.protify.core.internal.provider.gemini.model.GeminiCandidate;
import ai.protify.core.internal.provider.gemini.model.GeminiContent;
import ai.protify.core.internal.provider.gemini.model.GeminiFunctionCall;
import ai.protify.core.internal.provider.gemini.model.GeminiPart;
import ai.protify.core.internal.provider.gemini.model.GeminiResponseBody;
import ai.protify.core.internal.response.ProtifyAIResponse;
import ai.protify.core.internal.tool.ProtifyAIToolCall;
import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.tool.AIToolCall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GeminiResponse extends ProtifyAIResponse {

    private final GeminiResponseBody body;

    public GeminiResponse(boolean cachedResponse, String pipelineId, String correlationId,
                          String modelName, String rawResponse, GeminiResponseBody body) {
        super(cachedResponse, pipelineId, correlationId, modelName, rawResponse);
        this.body = body;
    }

    @Override
    public String getResponseId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public String getModelName() {
        return body.getModelVersion() != null ? body.getModelVersion() : "";
    }

    @Override
    public long getInputTokens() {
        return body.getUsageMetadata() != null ? body.getUsageMetadata().getPromptTokenCount() : 0;
    }

    @Override
    public long getOutputTokens() {
        return body.getUsageMetadata() != null ? body.getUsageMetadata().getCandidatesTokenCount() : 0;
    }

    @Override
    public long getTotalTokens() {
        return body.getUsageMetadata() != null ? body.getUsageMetadata().getTotalTokenCount() : 0;
    }

    @Override
    public long getProcessingTimeMillis() {
        return 0;
    }

    @Override
    public String text() {
        GeminiContent content = getFirstCandidateContent();
        if (content != null && content.getParts() != null) {
            StringBuilder sb = new StringBuilder();
            for (GeminiPart part : content.getParts()) {
                if (part.getText() != null) {
                    sb.append(part.getText());
                }
            }
            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        return "";
    }

    @Override
    public String getStopReason() {
        if (body.getCandidates() != null && !body.getCandidates().isEmpty()) {
            return body.getCandidates().get(0).getFinishReason();
        }
        return null;
    }

    @Override
    public boolean hasToolCalls() {
        GeminiContent content = getFirstCandidateContent();
        if (content != null && content.getParts() != null) {
            for (GeminiPart part : content.getParts()) {
                if (part.getFunctionCall() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<AIToolCall> getToolCalls() {
        GeminiContent content = getFirstCandidateContent();
        if (content == null || content.getParts() == null) {
            return Collections.emptyList();
        }

        List<AIToolCall> toolCalls = new ArrayList<>();
        for (GeminiPart part : content.getParts()) {
            GeminiFunctionCall fc = part.getFunctionCall();
            if (fc != null) {
                String name = fc.getName() != null ? fc.getName() : "";
                Map<String, Object> args = fc.getArgs() != null ? fc.getArgs() : Collections.emptyMap();
                String argumentsJson = ProtifyJson.toJsonMap(args);
                String id = UUID.randomUUID().toString();
                toolCalls.add(new ProtifyAIToolCall(id, name, args, argumentsJson));
            }
        }
        return toolCalls;
    }

    private GeminiContent getFirstCandidateContent() {
        if (body.getCandidates() != null && !body.getCandidates().isEmpty()) {
            GeminiCandidate candidate = body.getCandidates().get(0);
            return candidate.getContent();
        }
        return null;
    }
}
