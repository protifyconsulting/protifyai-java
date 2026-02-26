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

package com.protify.ai.internal.config;

import com.protify.ai.internal.util.Logger;
import com.protify.ai.internal.util.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public final class DerivedProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(DerivedProperties.class);

    public static Map<AIConfigProperty, Object> generate(
            Configuration baseConfiguration,
            Configuration pipelineConfig,
            Configuration clientConfig,
            Configuration requestConfig,
            boolean clientOverridesPipelineConfig,
            boolean requestOverridesPipelineConfig) {

        Map<AIConfigProperty, Object> baseProps = (baseConfiguration == null) ? null : baseConfiguration.getProperties();
        Map<AIConfigProperty, Object> pipelineProps = (pipelineConfig == null) ? null : pipelineConfig.getProperties();
        Map<AIConfigProperty, Object> clientProps = (clientConfig == null) ? null : clientConfig.getProperties();
        Map<AIConfigProperty, Object> requestProps = (requestConfig == null) ? null : requestConfig.getProperties();

        Map<AIConfigProperty, Object> merged = new EnumMap<>(AIConfigProperty.class);

        Stream.Builder<Map<AIConfigProperty, Object>> builder = Stream.builder();
        builder.add(baseProps);
        LOGGER.debug("Added base properties");

        if (requestOverridesPipelineConfig) {
            if (clientOverridesPipelineConfig) {
                LOGGER.debug("Client and Request properties override pipeline properties if running in a pipeline.");
                builder.add(pipelineProps).add(clientProps).add(requestProps);
            } else {
                LOGGER.debug("Request properties override pipeline properties if running in a pipeline.  Client does not.");
                builder.add(clientProps).add(pipelineProps).add(requestProps);
            }
        } else {
            if (clientOverridesPipelineConfig) {
                LOGGER.debug("Client properties override pipeline properties if running in a pipeline.  Request does not.  " +
                        "Client should take precedence over pipeline.");
                builder.add(pipelineProps).add(clientProps).add(requestProps);
            } else {
                LOGGER.debug("Pipeline properties override client and request properties if running in a pipeline.");
                builder.add(clientProps).add(requestProps).add(pipelineProps);
            }
        }

        builder.build()
                .filter(Objects::nonNull)
                .forEach(source -> source.forEach((key, value) -> {
                    if (value != null) {
                        merged.put(key, value);
                    }
                }));

        return Collections.unmodifiableMap(merged);
    }

    private DerivedProperties() { }
}
