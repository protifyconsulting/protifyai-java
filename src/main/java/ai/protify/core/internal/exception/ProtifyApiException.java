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

public class ProtifyApiException extends RuntimeException {

    private final int statusCode;
    private final String providerMessage;
    private final String errorType;
    private final String rawResponseBody;

    public ProtifyApiException() {
        super();
        this.statusCode = 0;
        this.providerMessage = null;
        this.errorType = null;
        this.rawResponseBody = null;
    }

    public ProtifyApiException(String message) {
        super(message);
        this.statusCode = 0;
        this.providerMessage = null;
        this.errorType = null;
        this.rawResponseBody = null;
    }

    public ProtifyApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.providerMessage = null;
        this.errorType = null;
        this.rawResponseBody = null;
    }

    public ProtifyApiException(Throwable cause) {
        super(cause);
        this.statusCode = 0;
        this.providerMessage = null;
        this.errorType = null;
        this.rawResponseBody = null;
    }

    protected ProtifyApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.statusCode = 0;
        this.providerMessage = null;
        this.errorType = null;
        this.rawResponseBody = null;
    }

    public ProtifyApiException(String message, int statusCode, String providerMessage,
                                String errorType, String rawResponseBody) {
        super(message);
        this.statusCode = statusCode;
        this.providerMessage = providerMessage;
        this.errorType = errorType;
        this.rawResponseBody = rawResponseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getProviderMessage() {
        return providerMessage;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getRawResponseBody() {
        return rawResponseBody;
    }
}
