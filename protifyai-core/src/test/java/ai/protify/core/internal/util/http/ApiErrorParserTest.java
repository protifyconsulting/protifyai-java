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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiErrorParserTest {

    @Nested
    @DisplayName("OpenAI-compatible format")
    class OpenAIFormat {

        @Test
        @DisplayName("extracts message and type from nested error object")
        void extractsMessageAndType() {
            String body = "{\"error\":{\"message\":\"Incorrect API key provided: sk-...\",\"type\":\"invalid_request_error\",\"code\":\"invalid_api_key\"}}";
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(401, body);

            assertEquals("Incorrect API key provided: sk-...", parsed.message);
            assertEquals("invalid_request_error", parsed.errorType);
            assertFalse(parsed.contentFiltered);
            assertEquals(body, parsed.rawBody);
        }

        @Test
        @DisplayName("extracts rate limit error")
        void extractsRateLimitError() {
            String body = "{\"error\":{\"message\":\"Rate limit reached for gpt-5-nano\",\"type\":\"rate_limit_error\"}}";
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(429, body);

            assertEquals("Rate limit reached for gpt-5-nano", parsed.message);
            assertEquals("rate_limit_error", parsed.errorType);
            assertFalse(parsed.contentFiltered);
        }
    }

    @Nested
    @DisplayName("xAI non-standard format")
    class XAIFormat {

        @Test
        @DisplayName("extracts plain-string error and code")
        void extractsPlainStringErrorAndCode() {
            String body = "{\"code\":\"The caller does not have permission\",\"error\":\"Content violates usage guidelines. Failed check: SAFETY_CHECK_TYPE_BIO\"}";
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(403, body);

            assertEquals("Content violates usage guidelines. Failed check: SAFETY_CHECK_TYPE_BIO", parsed.message);
            assertEquals("The caller does not have permission", parsed.errorType);
            assertTrue(parsed.contentFiltered);
        }

        @Test
        @DisplayName("disambiguates from OpenAI format when error is a string")
        void disambiguatesFromOpenAI() {
            String body = "{\"error\":\"simple error string\",\"code\":\"some_code\"}";
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(400, body);

            assertEquals("simple error string", parsed.message);
            assertEquals("some_code", parsed.errorType);
        }
    }

    @Nested
    @DisplayName("Gemini/Vertex format")
    class GeminiFormat {

        @Test
        @DisplayName("extracts message and status from nested error object")
        void extractsMessageAndStatus() {
            String body = "{\"error\":{\"message\":\"Resource has been exhausted\",\"status\":\"RESOURCE_EXHAUSTED\",\"code\":429}}";
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(429, body);

            assertEquals("Resource has been exhausted", parsed.message);
            assertEquals("RESOURCE_EXHAUSTED", parsed.errorType);
            assertFalse(parsed.contentFiltered);
        }
    }

    @Nested
    @DisplayName("Bedrock format")
    class BedrockFormat {

        @Test
        @DisplayName("extracts top-level message field")
        void extractsTopLevelMessage() {
            String body = "{\"message\":\"The model returned an error: throttling\"}";
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(429, body);

            assertEquals("The model returned an error: throttling", parsed.message);
            assertNull(parsed.errorType);
            assertFalse(parsed.contentFiltered);
        }
    }

    @Nested
    @DisplayName("Content safety detection")
    class ContentSafetyDetection {

        @Test
        @DisplayName("detects 'content filter' keyword")
        void detectsContentFilter() {
            String body = "{\"error\":{\"message\":\"Request blocked by content filter\",\"type\":\"content_filter\"}}";
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(400, body);
            assertTrue(parsed.contentFiltered);
        }

        @Test
        @DisplayName("detects 'safety' keyword")
        void detectsSafety() {
            String body = "{\"error\":{\"message\":\"Content blocked for safety reasons\",\"type\":\"safety_error\"}}";
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(403, body);
            assertTrue(parsed.contentFiltered);
        }

        @Test
        @DisplayName("detects 'SAFETY_CHECK' keyword")
        void detectsSafetyCheck() {
            String body = "{\"error\":\"Failed check: SAFETY_CHECK_TYPE_BIO\",\"code\":\"permission_denied\"}";
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(403, body);
            assertTrue(parsed.contentFiltered);
        }

        @Test
        @DisplayName("detects 'moderation' keyword")
        void detectsModeration() {
            String body = "{\"error\":{\"message\":\"Content flagged by moderation system\",\"type\":\"moderation_error\"}}";
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(400, body);
            assertTrue(parsed.contentFiltered);
        }

        @Test
        @DisplayName("detects 'responsible ai' keyword")
        void detectsResponsibleAi() {
            String body = "{\"error\":{\"message\":\"Blocked by responsible ai policy\",\"type\":\"blocked\"}}";
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(400, body);
            assertTrue(parsed.contentFiltered);
        }

        @Test
        @DisplayName("detects 'violates' keyword")
        void detectsViolates() {
            String body = "{\"error\":{\"message\":\"Content violates our terms of service\",\"type\":\"policy_violation\"}}";
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(403, body);
            assertTrue(parsed.contentFiltered);
        }

        @Test
        @DisplayName("does not flag normal auth errors as content filtered")
        void doesNotFlagNormalAuthErrors() {
            String body = "{\"error\":{\"message\":\"Invalid API key\",\"type\":\"authentication_error\"}}";
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(401, body);
            assertFalse(parsed.contentFiltered);
        }
    }

    @Nested
    @DisplayName("Fallback and edge cases")
    class FallbackCases {

        @Test
        @DisplayName("handles null body gracefully")
        void handlesNullBody() {
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(500, null);
            assertNull(parsed.message);
            assertNull(parsed.errorType);
            assertFalse(parsed.contentFiltered);
            assertNull(parsed.rawBody);
        }

        @Test
        @DisplayName("handles empty body gracefully")
        void handlesEmptyBody() {
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(500, "");
            assertNull(parsed.message);
            assertNull(parsed.errorType);
            assertFalse(parsed.contentFiltered);
        }

        @Test
        @DisplayName("handles whitespace-only body gracefully")
        void handlesWhitespaceBody() {
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(500, "   ");
            assertNull(parsed.message);
            assertNull(parsed.errorType);
            assertFalse(parsed.contentFiltered);
        }

        @Test
        @DisplayName("handles non-JSON body gracefully without throwing")
        void handlesNonJsonBody() {
            String body = "<html><body>Internal Server Error</body></html>";
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(500, body);
            assertNull(parsed.message);
            assertNull(parsed.errorType);
            assertFalse(parsed.contentFiltered);
            assertEquals(body, parsed.rawBody);
        }

        @Test
        @DisplayName("handles JSON with no recognized fields")
        void handlesUnrecognizedJson() {
            String body = "{\"foo\":\"bar\",\"baz\":123}";
            ApiErrorParser.ParsedError parsed = ApiErrorParser.parse(500, body);
            assertNull(parsed.message);
            assertNull(parsed.errorType);
            assertFalse(parsed.contentFiltered);
            assertEquals(body, parsed.rawBody);
        }
    }
}
