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

import java.util.function.BiConsumer;

/**
 * Stateful line-by-line SSE parser. Receives raw lines from an HTTP response body,
 * assembles them into events, and emits complete (event, data) pairs via a callback.
 */
public class SSELineParser {

    private final BiConsumer<String, String> onEvent;

    private String currentEvent = "";
    private final StringBuilder currentData = new StringBuilder();
    private boolean hasData = false;

    public SSELineParser(BiConsumer<String, String> onEvent) {
        this.onEvent = onEvent;
    }

    public void feedLine(String line) {
        if (line == null) {
            return;
        }

        // Blank line dispatches the current event
        if (line.isEmpty()) {
            dispatch();
            return;
        }

        // Comment lines (starting with ':') are ignored per SSE spec
        if (line.startsWith(":")) {
            return;
        }

        if (line.startsWith("event:")) {
            currentEvent = line.substring("event:".length()).trim();
        } else if (line.startsWith("data:")) {
            String data = line.substring("data:".length()).trim();
            if (hasData) {
                currentData.append('\n');
            }
            currentData.append(data);
            hasData = true;
        }
    }

    private void dispatch() {
        if (hasData) {
            String data = currentData.toString();
            if (!"[DONE]".equals(data)) {
                onEvent.accept(currentEvent, data);
            }
            currentEvent = "";
            currentData.setLength(0);
            hasData = false;
        }
    }

    public void finish() {
        dispatch();
    }
}
