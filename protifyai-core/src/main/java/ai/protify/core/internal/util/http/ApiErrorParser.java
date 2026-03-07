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

package ai.protify.core.internal.util.http;

import ai.protify.core.internal.util.json.ProtifyJson;
import ai.protify.core.internal.util.json.ProtifyJsonObject;

import java.util.Locale;
import java.util.Map;

final class ApiErrorParser {

    private static final String[] CONTENT_FILTER_KEYWORDS = {
            "content filter", "safety", "safety_check", "content policy",
            "violates", "usage guidelines", "moderation", "responsible ai"
    };

    private ApiErrorParser() {}

    static ParsedError parse(int statusCode, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return new ParsedError(null, null, false, responseBody);
        }

        String message = null;
        String errorType = null;

        try {
            ProtifyJsonObject json = ProtifyJson.parse(responseBody);
            Object errorField = json.get("error");

            if (errorField instanceof Map) {
                // Strategy 1: OpenAI-compatible / Anthropic / Gemini nested error object
                // OpenAI/Anthropic: error.message + error.type
                // Gemini: error.message + error.status
                message = json.getString("error.message");
                errorType = json.getString("error.type");
                if (errorType == null) {
                    errorType = json.getString("error.status");
                }
            } else if (errorField instanceof String) {
                // Strategy 2: xAI non-standard — error is a plain string, code is a plain string
                message = (String) errorField;
                Object codeField = json.get("code");
                if (codeField instanceof String) {
                    errorType = (String) codeField;
                }
            } else {
                // Strategy 3: Bedrock — top-level message field
                message = json.getString("message");
                if (message == null) {
                    // No recognized fields — fall through to raw body
                    return new ParsedError(null, null, false, responseBody);
                }
            }
        } catch (Exception e) {
            // Non-JSON body or parse failure — graceful fallback
            return new ParsedError(null, null, false, responseBody);
        }

        boolean contentFiltered = isContentFiltered(message, errorType);
        return new ParsedError(message, errorType, contentFiltered, responseBody);
    }

    private static boolean isContentFiltered(String message, String errorType) {
        String combined = ((message != null ? message : "") + " " + (errorType != null ? errorType : ""))
                .toLowerCase(Locale.ROOT);
        for (String keyword : CONTENT_FILTER_KEYWORDS) {
            if (combined.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    static final class ParsedError {
        final String message;
        final String errorType;
        final boolean contentFiltered;
        final String rawBody;

        ParsedError(String message, String errorType, boolean contentFiltered, String rawBody) {
            this.message = message;
            this.errorType = errorType;
            this.contentFiltered = contentFiltered;
            this.rawBody = rawBody;
        }
    }
}
