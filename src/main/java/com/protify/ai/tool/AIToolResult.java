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

package com.protify.ai.tool;

public class AIToolResult {

    private final String toolCallId;
    private final String content;
    private final boolean error;

    public AIToolResult(String toolCallId, String content) {
        this(toolCallId, content, false);
    }

    public AIToolResult(String toolCallId, String content, boolean error) {
        this.toolCallId = toolCallId;
        this.content = content;
        this.error = error;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public String getContent() {
        return content;
    }

    public boolean isError() {
        return error;
    }
}
