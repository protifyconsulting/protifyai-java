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

package com.protify.ai.internal.config;

import com.protify.ai.provider.AIProvider;

public interface CredentialHelper {

    String getCredential(AIProvider provider, Configuration config);

    static String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex < 0) return null;

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex < 0) return null;

        int quoteStart = json.indexOf('"', colonIndex + 1);
        if (quoteStart < 0) return null;

        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) return null;

        return json.substring(quoteStart + 1, quoteEnd);
    }
}
