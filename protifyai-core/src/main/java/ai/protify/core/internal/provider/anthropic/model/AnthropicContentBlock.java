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

package ai.protify.core.internal.provider.anthropic.model;

import ai.protify.core.internal.util.json.ProtifyJsonProperty;

import java.util.Map;

public final class AnthropicContentBlock {

    private String type;
    private String text;
    private AnthropicSource source;

    // tool_use fields
    private String id;
    private String name;
    private Map<String, Object> input;

    // tool_result fields
    @ProtifyJsonProperty("tool_use_id")
    private String toolUseId;
    private String content;

    @ProtifyJsonProperty("is_error")
    private Boolean isError;

    private AnthropicContentBlock() {
    }

    public static AnthropicContentBlock text(String text) {
        AnthropicContentBlock block = new AnthropicContentBlock();
        block.type = "text";
        block.text = text;
        return block;
    }

    public static AnthropicContentBlock image(String mediaType, String base64Data) {
        AnthropicContentBlock block = new AnthropicContentBlock();
        block.type = "image";
        block.source = AnthropicSource.base64(mediaType, base64Data);
        return block;
    }

    public static AnthropicContentBlock document(String base64Data) {
        AnthropicContentBlock block = new AnthropicContentBlock();
        block.type = "document";
        block.source = AnthropicSource.base64("application/pdf", base64Data);
        return block;
    }

    public static AnthropicContentBlock toolUse(String id, String name, Map<String, Object> input) {
        AnthropicContentBlock block = new AnthropicContentBlock();
        block.type = "tool_use";
        block.id = id;
        block.name = name;
        block.input = input;
        return block;
    }

    public static AnthropicContentBlock toolResult(String toolUseId, String content) {
        return toolResult(toolUseId, content, false);
    }

    public static AnthropicContentBlock toolResult(String toolUseId, String content, boolean isError) {
        AnthropicContentBlock block = new AnthropicContentBlock();
        block.type = "tool_result";
        block.toolUseId = toolUseId;
        block.content = content;
        if (isError) {
            block.isError = true;
        }
        return block;
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public AnthropicSource getSource() {
        return source;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    @ProtifyJsonProperty("tool_use_id")
    public String getToolUseId() {
        return toolUseId;
    }

    public String getContent() {
        return content;
    }

    @ProtifyJsonProperty("is_error")
    public Boolean getIsError() {
        return isError;
    }
}
