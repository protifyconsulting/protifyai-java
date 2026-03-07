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

package ai.protify.core.internal.provider.chatcompletions.model;

public class ChatToolCall {

    private String id;
    private String type;
    private ChatFunctionRef function;

    public ChatToolCall() {
    }

    private ChatToolCall(String id, String name, String arguments) {
        this.id = id;
        this.type = "function";
        this.function = new ChatFunctionRef(name, arguments);
    }

    public static ChatToolCall of(String id, String name, String arguments) {
        return new ChatToolCall(id, name, arguments);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ChatFunctionRef getFunction() {
        return function;
    }

    public void setFunction(ChatFunctionRef function) {
        this.function = function;
    }

    public static class ChatFunctionRef {

        private String name;
        private String arguments;

        public ChatFunctionRef() {
        }

        ChatFunctionRef(String name, String arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArguments() {
            return arguments;
        }

        public void setArguments(String arguments) {
            this.arguments = arguments;
        }
    }
}
