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

package ai.protify.core.internal.provider.bedrock.model;

public class BedrockResponseBody {

    private BedrockOutputContent output;
    private String stopReason;
    private BedrockUsage usage;

    public BedrockOutputContent getOutput() { return output; }
    public void setOutput(BedrockOutputContent output) { this.output = output; }
    public String getStopReason() { return stopReason; }
    public void setStopReason(String stopReason) { this.stopReason = stopReason; }
    public BedrockUsage getUsage() { return usage; }
    public void setUsage(BedrockUsage usage) { this.usage = usage; }

    public static class BedrockOutputContent {
        private BedrockMessage message;

        public BedrockMessage getMessage() { return message; }
        public void setMessage(BedrockMessage message) { this.message = message; }
    }

    public static class BedrockUsage {
        private int inputTokens;
        private int outputTokens;
        private int totalTokens;

        public int getInputTokens() { return inputTokens; }
        public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }
        public int getOutputTokens() { return outputTokens; }
        public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }
        public int getTotalTokens() { return totalTokens; }
        public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
    }
}
