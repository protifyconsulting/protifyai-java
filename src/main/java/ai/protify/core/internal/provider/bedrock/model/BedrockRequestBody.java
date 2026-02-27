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

import java.util.List;

public class BedrockRequestBody {

    private List<BedrockMessage> messages;
    private List<BedrockContentBlock> system;
    private BedrockInferenceConfig inferenceConfig;
    private BedrockToolConfig toolConfig;

    public List<BedrockMessage> getMessages() { return messages; }
    public void setMessages(List<BedrockMessage> messages) { this.messages = messages; }
    public List<BedrockContentBlock> getSystem() { return system; }
    public void setSystem(List<BedrockContentBlock> system) { this.system = system; }
    public BedrockInferenceConfig getInferenceConfig() { return inferenceConfig; }
    public void setInferenceConfig(BedrockInferenceConfig inferenceConfig) { this.inferenceConfig = inferenceConfig; }
    public BedrockToolConfig getToolConfig() { return toolConfig; }
    public void setToolConfig(BedrockToolConfig toolConfig) { this.toolConfig = toolConfig; }
}
