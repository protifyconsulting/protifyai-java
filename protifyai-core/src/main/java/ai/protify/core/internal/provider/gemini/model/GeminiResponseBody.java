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

package ai.protify.core.internal.provider.gemini.model;

import ai.protify.core.internal.util.json.ProtifyJsonProperty;

import java.util.List;

public class GeminiResponseBody {

    private List<GeminiCandidate> candidates;

    @ProtifyJsonProperty("usageMetadata")
    private GeminiUsage usageMetadata;

    @ProtifyJsonProperty("modelVersion")
    private String modelVersion;

    public List<GeminiCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<GeminiCandidate> candidates) {
        this.candidates = candidates;
    }

    @ProtifyJsonProperty("usageMetadata")
    public GeminiUsage getUsageMetadata() {
        return usageMetadata;
    }

    public void setUsageMetadata(GeminiUsage usageMetadata) {
        this.usageMetadata = usageMetadata;
    }

    @ProtifyJsonProperty("modelVersion")
    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }
}
