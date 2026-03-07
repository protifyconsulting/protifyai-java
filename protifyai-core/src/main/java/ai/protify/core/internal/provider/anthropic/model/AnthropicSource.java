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

public final class AnthropicSource {

    private String type;

    @ProtifyJsonProperty("media_type")
    private String mediaType;

    private String data;

    private AnthropicSource() {
    }

    static AnthropicSource base64(String mediaType, String data) {
        AnthropicSource source = new AnthropicSource();
        source.type = "base64";
        source.mediaType = mediaType;
        source.data = data;
        return source;
    }

    public String getType() {
        return type;
    }

    @ProtifyJsonProperty("media_type")
    public String getMediaType() {
        return mediaType;
    }

    public String getData() {
        return data;
    }
}
