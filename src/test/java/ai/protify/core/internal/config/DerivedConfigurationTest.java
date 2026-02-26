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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DerivedConfigurationTest {

    Map<AIConfigProperty, Object> baseProps;
    Map<AIConfigProperty, Object> pipelineProps;
    Map<AIConfigProperty, Object> clientProps;
    Map<AIConfigProperty, Object> requestProps;

    @BeforeEach
    void setUp() {
        baseProps = new EnumMap<>(AIConfigProperty.class);
        pipelineProps = new EnumMap<>(AIConfigProperty.class);
        clientProps = new EnumMap<>(AIConfigProperty.class);
        requestProps = new EnumMap<>(AIConfigProperty.class);

        // Setup a common property to test overrides
        baseProps.put(AIConfigProperty.TEMPERATURE, 0.1);
        pipelineProps.put(AIConfigProperty.TEMPERATURE, 0.5);
        clientProps.put(AIConfigProperty.TEMPERATURE, 0.7);
        requestProps.put(AIConfigProperty.TEMPERATURE, 0.9);
    }

    @Test
    @DisplayName("Should respect standard precedence")
    void testStandardPrecedence() {
        Map<AIConfigProperty, Object> config = DerivedProperties.generate(
                new Configuration(baseProps), new Configuration(pipelineProps),
                new Configuration(clientProps), new Configuration(requestProps),
                false,
                false
        );

        // Should be the pipeline config
        assertEquals(0.5, config.get(AIConfigProperty.TEMPERATURE));
    }

    @Test
    @DisplayName("Should allow Client to override Pipeline Configuration")
    void testClientOverridesPipeline() {
        Map<AIConfigProperty, Object> config = DerivedProperties.generate(
                new Configuration(baseProps), new Configuration(pipelineProps),
                new Configuration(clientProps), new Configuration(requestProps),
                true,
                false
        );

        // Should be the request value, since client overrides
        assertEquals(0.9, config.get(AIConfigProperty.TEMPERATURE));

        // To verify client > pipeline specifically, we remove the request property
        requestProps.remove(AIConfigProperty.TEMPERATURE);
        config = DerivedProperties.generate(
                new Configuration(baseProps), new Configuration(pipelineProps),
                new Configuration(clientProps), new Configuration(requestProps), true, false);

        assertEquals(0.7, config.get(AIConfigProperty.TEMPERATURE), "Client should override Pipeline");
    }

    @Test
    @DisplayName("Should allow Request to override Pipeline Configuration")
    void testPipelineOverridesRequest() {
        Map<AIConfigProperty, Object> config = DerivedProperties.generate(
                new Configuration(baseProps), new Configuration(pipelineProps),
                new Configuration(clientProps), new Configuration(requestProps),
                false,
                true
        );

        assertEquals(0.9, config.get(AIConfigProperty.TEMPERATURE));
    }

    @Test
    @DisplayName("Should handle null configurations gracefully")
    void testNullConfigs() {
        Map<AIConfigProperty, Object> config = DerivedProperties.generate(
                new Configuration(baseProps), null, null, null,
                false, false
        );

        assertEquals(0.1, config.get(AIConfigProperty.TEMPERATURE));
    }

    @Test
    @DisplayName("Should merge unique properties from all sources")
    void testMergingUniqueProperties() {
        baseProps.put(AIConfigProperty.MAX_OUTPUT_TOKENS, 100);
        clientProps.put(AIConfigProperty.INSTRUCTIONS, "test");
        requestProps.put(AIConfigProperty.PRETTY_PRINT_JSON, true);

        Map<AIConfigProperty, Object> config = DerivedProperties.generate(
                new Configuration(baseProps), new Configuration(pipelineProps),
                new Configuration(clientProps), new Configuration(requestProps),
                false, false
        );

        assertEquals("test", config.get(AIConfigProperty.INSTRUCTIONS));
        assertEquals(100, (Integer) config.get(AIConfigProperty.MAX_OUTPUT_TOKENS));
        assertEquals(0.5, config.get(AIConfigProperty.TEMPERATURE));
        assertEquals(true, config.get(AIConfigProperty.PRETTY_PRINT_JSON));
    }
}
