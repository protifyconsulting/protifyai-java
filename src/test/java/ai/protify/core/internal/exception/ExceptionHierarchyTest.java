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

package ai.protify.core.internal.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionHierarchyTest {

    @Nested
    @DisplayName("All subclasses extend ProtifyApiException")
    class Hierarchy {

        @Test
        @DisplayName("AccessDeniedException extends ProtifyApiException")
        void accessDenied() {
            assertInstanceOf(ProtifyApiException.class, new AccessDeniedException("test"));
        }

        @Test
        @DisplayName("BadRequestException extends ProtifyApiException")
        void badRequest() {
            assertInstanceOf(ProtifyApiException.class, new BadRequestException("test"));
        }

        @Test
        @DisplayName("RateLimitExceededException extends ProtifyApiException")
        void rateLimit() {
            assertInstanceOf(ProtifyApiException.class, new RateLimitExceededException("test"));
        }

        @Test
        @DisplayName("TimeoutException extends ProtifyApiException")
        void timeout() {
            assertInstanceOf(ProtifyApiException.class, new TimeoutException("test"));
        }

        @Test
        @DisplayName("NotFoundException extends ProtifyApiException")
        void notFound() {
            assertInstanceOf(ProtifyApiException.class, new NotFoundException("test"));
        }

        @Test
        @DisplayName("ServiceException extends ProtifyApiException")
        void service() {
            assertInstanceOf(ProtifyApiException.class, new ServiceException("test"));
        }

        @Test
        @DisplayName("ServiceUnavailableException extends ProtifyApiException")
        void serviceUnavailable() {
            assertInstanceOf(ProtifyApiException.class, new ServiceUnavailableException("test"));
        }

        @Test
        @DisplayName("ServiceOverloadedException extends ProtifyApiException")
        void serviceOverloaded() {
            assertInstanceOf(ProtifyApiException.class, new ServiceOverloadedException("test"));
        }

        @Test
        @DisplayName("ProviderBusyException extends ProtifyApiException")
        void providerBusy() {
            assertInstanceOf(ProtifyApiException.class, new ProviderBusyException("test"));
        }

        @Test
        @DisplayName("ContentFilteredException extends ProtifyApiException")
        void contentFiltered() {
            assertInstanceOf(ProtifyApiException.class, new ContentFilteredException("test"));
        }
    }

    @Nested
    @DisplayName("Structured fields accessible via getters")
    class StructuredFields {

        @Test
        @DisplayName("structured constructor populates all fields")
        void structuredConstructorPopulatesFields() {
            AccessDeniedException ex = new AccessDeniedException(
                    "Access denied (HTTP 401): Invalid key", 401,
                    "Invalid key", "invalid_request_error",
                    "{\"error\":{\"message\":\"Invalid key\"}}");

            assertEquals("Access denied (HTTP 401): Invalid key", ex.getMessage());
            assertEquals(401, ex.getStatusCode());
            assertEquals("Invalid key", ex.getProviderMessage());
            assertEquals("invalid_request_error", ex.getErrorType());
            assertEquals("{\"error\":{\"message\":\"Invalid key\"}}", ex.getRawResponseBody());
        }

        @Test
        @DisplayName("ContentFilteredException structured fields")
        void contentFilteredStructuredFields() {
            ContentFilteredException ex = new ContentFilteredException(
                    "Content safety violation (HTTP 403): Content violates guidelines", 403,
                    "Content violates guidelines", "safety_error",
                    "{\"error\":\"Content violates guidelines\"}");

            assertEquals(403, ex.getStatusCode());
            assertEquals("Content violates guidelines", ex.getProviderMessage());
            assertEquals("safety_error", ex.getErrorType());
        }
    }

    @Nested
    @DisplayName("Backward compatibility")
    class BackwardCompat {

        @Test
        @DisplayName("old string constructor still works with default field values")
        void oldConstructorDefaults() {
            AccessDeniedException ex = new AccessDeniedException("Access denied: 403");

            assertEquals("Access denied: 403", ex.getMessage());
            assertEquals(0, ex.getStatusCode());
            assertNull(ex.getProviderMessage());
            assertNull(ex.getErrorType());
            assertNull(ex.getRawResponseBody());
        }

        @Test
        @DisplayName("no-arg constructor still works")
        void noArgConstructor() {
            BadRequestException ex = new BadRequestException();

            assertNull(ex.getMessage());
            assertEquals(0, ex.getStatusCode());
        }

        @Test
        @DisplayName("all subclasses are catchable as RuntimeException")
        void catchableAsRuntimeException() {
            assertInstanceOf(RuntimeException.class, new AccessDeniedException("test"));
            assertInstanceOf(RuntimeException.class, new ContentFilteredException("test"));
            assertInstanceOf(RuntimeException.class, new ServiceException("test"));
        }

        @Test
        @DisplayName("ContentFilteredException catchable as ProtifyApiException")
        void contentFilteredCatchableAsProtifyApi() {
            try {
                throw new ContentFilteredException("content blocked", 403,
                        "blocked", "safety", "{}");
            } catch (ProtifyApiException e) {
                assertEquals(403, e.getStatusCode());
                assertEquals("blocked", e.getProviderMessage());
            }
        }
    }
}
