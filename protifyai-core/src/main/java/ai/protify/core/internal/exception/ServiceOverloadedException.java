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

public class ServiceOverloadedException extends ProtifyApiException {

    public ServiceOverloadedException() {
        super();
    }

    public ServiceOverloadedException(String message) {
        super(message);
    }

    public ServiceOverloadedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceOverloadedException(Throwable cause) {
        super(cause);
    }

    protected ServiceOverloadedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ServiceOverloadedException(String message, int statusCode, String providerMessage,
                                       String errorType, String rawResponseBody) {
        super(message, statusCode, providerMessage, errorType, rawResponseBody);
    }
}
