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

package com.protify.ai.internal.util.http;

public class ProtifyHttpResponse {

    private final boolean cachedResponse;
    private final String responseBody;
    private final int statusCode;
    private final long elapsedTimeMillis;

    public ProtifyHttpResponse(boolean cachedResponse, String responseBody, int statusCode, long elapsedTimeMillis) {
        this.cachedResponse = cachedResponse;
        this.responseBody = responseBody;
        this.statusCode = statusCode;
        this.elapsedTimeMillis = elapsedTimeMillis;
    }

    public boolean isCachedResponse() {
        return cachedResponse;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getElapsedTimeMillis() {
        return elapsedTimeMillis;
    }
}
