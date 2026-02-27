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

import java.util.Map;

public class BedrockContentBlock {

    private String text;
    private BedrockImageBlock image;
    private BedrockToolUseBlock toolUse;
    private BedrockToolResultBlock toolResult;

    public static BedrockContentBlock text(String text) {
        BedrockContentBlock block = new BedrockContentBlock();
        block.text = text;
        return block;
    }

    public static BedrockContentBlock image(String format, String base64Data) {
        BedrockContentBlock block = new BedrockContentBlock();
        BedrockImageBlock img = new BedrockImageBlock();
        img.setFormat(format);
        BedrockImageSource source = new BedrockImageSource();
        source.setBytes(base64Data);
        img.setSource(source);
        block.image = img;
        return block;
    }

    public static BedrockContentBlock toolUse(String toolUseId, String name, Map<String, Object> input) {
        BedrockContentBlock block = new BedrockContentBlock();
        BedrockToolUseBlock tu = new BedrockToolUseBlock();
        tu.setToolUseId(toolUseId);
        tu.setName(name);
        tu.setInput(input);
        block.toolUse = tu;
        return block;
    }

    public static BedrockContentBlock toolResult(String toolUseId, String content) {
        BedrockContentBlock block = new BedrockContentBlock();
        BedrockToolResultBlock tr = new BedrockToolResultBlock();
        tr.setToolUseId(toolUseId);
        BedrockContentBlock textBlock = BedrockContentBlock.text(content);
        tr.setContent(java.util.Collections.singletonList(textBlock));
        block.toolResult = tr;
        return block;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public BedrockImageBlock getImage() { return image; }
    public void setImage(BedrockImageBlock image) { this.image = image; }
    public BedrockToolUseBlock getToolUse() { return toolUse; }
    public void setToolUse(BedrockToolUseBlock toolUse) { this.toolUse = toolUse; }
    public BedrockToolResultBlock getToolResult() { return toolResult; }
    public void setToolResult(BedrockToolResultBlock toolResult) { this.toolResult = toolResult; }

    public static class BedrockImageBlock {
        private String format;
        private BedrockImageSource source;

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public BedrockImageSource getSource() { return source; }
        public void setSource(BedrockImageSource source) { this.source = source; }
    }

    public static class BedrockImageSource {
        private String bytes;

        public String getBytes() { return bytes; }
        public void setBytes(String bytes) { this.bytes = bytes; }
    }

    public static class BedrockToolUseBlock {
        private String toolUseId;
        private String name;
        private Map<String, Object> input;

        public String getToolUseId() { return toolUseId; }
        public void setToolUseId(String toolUseId) { this.toolUseId = toolUseId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Map<String, Object> getInput() { return input; }
        public void setInput(Map<String, Object> input) { this.input = input; }
    }

    public static class BedrockToolResultBlock {
        private String toolUseId;
        private java.util.List<BedrockContentBlock> content;

        public String getToolUseId() { return toolUseId; }
        public void setToolUseId(String toolUseId) { this.toolUseId = toolUseId; }
        public java.util.List<BedrockContentBlock> getContent() { return content; }
        public void setContent(java.util.List<BedrockContentBlock> content) { this.content = content; }
    }
}
