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

public final class GeminiInlineData {

    @ProtifyJsonProperty("mimeType")
    private String mimeType;

    private String data;

    public GeminiInlineData() {
    }

    GeminiInlineData(String mimeType, String data) {
        this.mimeType = mimeType;
        this.data = data;
    }

    @ProtifyJsonProperty("mimeType")
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
