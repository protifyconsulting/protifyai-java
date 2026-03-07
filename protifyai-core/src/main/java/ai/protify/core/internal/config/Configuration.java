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

package ai.protify.core.internal.config;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Configuration {

    private final Map<AIConfigProperty, Object> properties;
    private Set<AIConfigProperty> suppressedProperties;

    public Configuration(Map<AIConfigProperty, Object> properties) {
        this.properties = properties;
    }

    public void suppressProperty(AIConfigProperty property) {
        if (suppressedProperties == null) {
            suppressedProperties = EnumSet.noneOf(AIConfigProperty.class);
        }
        suppressedProperties.add(property);
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(AIConfigProperty property) {
        if (suppressedProperties != null && suppressedProperties.contains(property)) {
            return null;
        }
        return (T) property.getType().cast(properties.get(property));
    }

    @SuppressWarnings({"java:S3358"})
    public String getConfiguration() {
        return properties.entrySet().stream()
            .map(entry -> {
                AIConfigProperty property = entry.getKey();
                Object value = entry.getValue();

                String displayValue = (value == null) ? "null" :
                        (property.isSecret()) ? "*****" : value.toString();

                return property.getName() + ": " + displayValue;
            })
            .collect(Collectors.joining("\n", "", "\n"));
    }

    public Map<AIConfigProperty, Object> getProperties() {
        return properties;
    }
}
