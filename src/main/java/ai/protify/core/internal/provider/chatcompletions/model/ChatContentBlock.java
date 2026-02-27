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

package ai.protify.core.internal.provider.chatcompletions.model;

import ai.protify.core.internal.util.json.ProtifyJsonProperty;

public final class ChatContentBlock {

    private String type;
    private String text;

    @ProtifyJsonProperty("image_url")
    private ChatImageUrl imageUrl;

    private ChatContentBlock() {
    }

    public static ChatContentBlock text(String text) {
        ChatContentBlock block = new ChatContentBlock();
        block.type = "text";
        block.text = text;
        return block;
    }

    public static ChatContentBlock imageUrl(String url) {
        ChatContentBlock block = new ChatContentBlock();
        block.type = "image_url";
        block.imageUrl = new ChatImageUrl(url);
        return block;
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    @ProtifyJsonProperty("image_url")
    public ChatImageUrl getImageUrl() {
        return imageUrl;
    }
}
