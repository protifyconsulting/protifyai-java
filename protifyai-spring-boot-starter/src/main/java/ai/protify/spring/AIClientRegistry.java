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

package ai.protify.spring;

import ai.protify.core.AIClient;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AIClientRegistry {

    private final AIClient defaultClient;
    private final Map<String, AIClient> namedClients = new LinkedHashMap<>();

    AIClientRegistry(AIClient defaultClient) {
        this.defaultClient = defaultClient;
    }

    void registerClient(String name, AIClient client) {
        namedClients.put(name, client);
    }

    public AIClient getDefaultClient() {
        return defaultClient;
    }

    public AIClient getClient(String name) {
        if (name == null || name.isEmpty()) {
            return defaultClient;
        }
        AIClient client = namedClients.get(name);
        if (client == null) {
            throw new IllegalArgumentException(
                    "No AIClient configured with name '" + name + "'. " +
                    "Available named clients: " + namedClients.keySet() +
                    ". Configure it under 'protify.ai.clients." + name + "'.");
        }
        return client;
    }

    public Set<String> getClientNames() {
        return Collections.unmodifiableSet(namedClients.keySet());
    }
}
