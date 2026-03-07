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
import java.util.stream.Collectors;

public class BedrockToolConfig {

    private List<BedrockToolEntry> tools;

    public static BedrockToolConfig from(List<ai.protify.core.tool.AITool> aiTools) {
        BedrockToolConfig config = new BedrockToolConfig();
        config.tools = aiTools.stream()
                .map(tool -> {
                    BedrockToolEntry entry = new BedrockToolEntry();
                    entry.setToolSpec(BedrockToolSpec.from(tool));
                    return entry;
                })
                .collect(Collectors.toList());
        return config;
    }

    public List<BedrockToolEntry> getTools() { return tools; }
    public void setTools(List<BedrockToolEntry> tools) { this.tools = tools; }

    public static class BedrockToolEntry {
        private BedrockToolSpec toolSpec;

        public BedrockToolSpec getToolSpec() { return toolSpec; }
        public void setToolSpec(BedrockToolSpec toolSpec) { this.toolSpec = toolSpec; }
    }
}
