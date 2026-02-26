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

package com.protify.ai.internal.provider.anthropic.model;

import com.protify.ai.internal.util.json.ProtifyJsonProperty;

public class AnthropicUsage {

    @ProtifyJsonProperty("input_tokens")
    private long inputTokens;

    @ProtifyJsonProperty("output_tokens")
    private long outputTokens;

    @ProtifyJsonProperty("input_tokens")
    public long getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(long inputTokens) {
        this.inputTokens = inputTokens;
    }

    @ProtifyJsonProperty("output_tokens")
    public long getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(long outputTokens) {
        this.outputTokens = outputTokens;
    }
}
