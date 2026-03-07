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

public final class GeminiRequestBody {

    private List<GeminiContent> contents;

    @ProtifyJsonProperty("systemInstruction")
    private GeminiContent systemInstruction;

    @ProtifyJsonProperty("generationConfig")
    private GeminiGenerationConfig generationConfig;

    private List<GeminiTool> tools;

    public List<GeminiContent> getContents() {
        return contents;
    }

    public void setContents(List<GeminiContent> contents) {
        this.contents = contents;
    }

    @ProtifyJsonProperty("systemInstruction")
    public GeminiContent getSystemInstruction() {
        return systemInstruction;
    }

    public void setSystemInstruction(GeminiContent systemInstruction) {
        this.systemInstruction = systemInstruction;
    }

    @ProtifyJsonProperty("generationConfig")
    public GeminiGenerationConfig getGenerationConfig() {
        return generationConfig;
    }

    public void setGenerationConfig(GeminiGenerationConfig generationConfig) {
        this.generationConfig = generationConfig;
    }

    public List<GeminiTool> getTools() {
        return tools;
    }

    public void setTools(List<GeminiTool> tools) {
        this.tools = tools;
    }
}
