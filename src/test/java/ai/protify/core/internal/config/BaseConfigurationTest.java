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

import ai.protify.core.resiliency.RetryBackoffStrategy;
import ai.protify.core.resiliency.RetryPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Isolated
class BaseConfigurationTest {

    @Test
    @DisplayName("Should return the same instance (Singleton)")
    void testSingletonInstance() {
        BaseConfiguration instance1 = BaseConfiguration.getInstance();
        BaseConfiguration instance2 = BaseConfiguration.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2, "BaseConfiguration should be a singleton");
    }

    @Test
    @DisplayName("Should return base default values for non-existent properties")
    void testDefaultValues() {
        BaseConfiguration config = BaseConfiguration.getInstance();
        config.resetForTesting("non-existent-file", Map.of());

        assertNull(config.getProperty(AIConfigProperty.PROVIDER_API_KEY));
        assertEquals((Integer) 4096, config.getProperty(AIConfigProperty.MAX_OUTPUT_TOKENS));
        assertNull(config.getProperty(AIConfigProperty.TEMPERATURE));
        assertFalse(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.PRETTY_PRINT_JSON)));
        assertTrue(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_TRUNCATE_LARGE_INPUT)));
        assertFalse(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_RESPONSES)));
        assertFalse(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_REQUESTS)));
    }

    @Test
    @DisplayName("Should return valid values from specified configuration")
    void testValidFileValues() {
        BaseConfiguration config = BaseConfiguration.getInstance();
        config.resetForTesting("protifyai-valid", Map.of());

        assertEquals((Integer)1000, config.getProperty(AIConfigProperty.MAX_OUTPUT_TOKENS));
        assertEquals(0.75, config.getProperty(AIConfigProperty.TEMPERATURE));
        assertEquals("I am a pirate", config.getProperty(AIConfigProperty.INSTRUCTIONS));
        assertTrue(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.PRETTY_PRINT_JSON)));
        assertFalse(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_TRUNCATE_LARGE_INPUT)));
        assertTrue(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_RESPONSES)));
        assertTrue(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_REQUESTS)));

        assertNull(config.getProperty(AIConfigProperty.MODEL));
        assertNull(config.getProperty(AIConfigProperty.MODEL_EXPLICIT_VERSION));
    }

    @Test
    @DisplayName("Should ignore invalid configuration properties, and fall back to base defaults")
    void testInvalidValues() {
        BaseConfiguration config = BaseConfiguration.getInstance();
        config.resetForTesting("protifyai-invalid", Map.of());

        assertNull(config.getProperty(AIConfigProperty.INSTRUCTIONS));
        assertEquals((Integer) 4096, config.getProperty(AIConfigProperty.MAX_OUTPUT_TOKENS));
        assertNull(config.getProperty(AIConfigProperty.TEMPERATURE));
        assertTrue(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_TRUNCATE_LARGE_INPUT)));
        assertFalse(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_REQUESTS)));
    }

    @Test
    @DisplayName("Should get property values from profile configuration")
    void testValidProfileValues() {
        BaseConfiguration config = BaseConfiguration.getInstance();
        config.resetForTesting("protifyai-valid", Map.of(
                BaseConfiguration.PROFILE_ENV_VAR, "testing"));

        assertEquals((Integer)500, config.getProperty(AIConfigProperty.MAX_OUTPUT_TOKENS));
        assertEquals(0.99, config.getProperty(AIConfigProperty.TEMPERATURE));
        assertEquals("I am an astronaut", config.getProperty(AIConfigProperty.INSTRUCTIONS));
        assertFalse(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.PRETTY_PRINT_JSON)));
        assertFalse(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_TRUNCATE_LARGE_INPUT)));
        assertFalse(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_RESPONSES)));
        assertTrue(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_REQUESTS)));

        assertNull(config.getProperty(AIConfigProperty.MODEL));
        assertNull(config.getProperty(AIConfigProperty.MODEL_EXPLICIT_VERSION));
    }


    @Test
    @DisplayName("Should handle various boolean string representations (on, 1, enabled, off)")
    void testBooleanTypeAliases() {
        BaseConfiguration config = BaseConfiguration.getInstance();
        config.resetForTesting("protifyai-booleans", Map.of());

        assertTrue(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.PRETTY_PRINT_JSON)));
        assertFalse(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_TRUNCATE_LARGE_INPUT)));
        assertTrue(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_RESPONSES)));
        assertTrue(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_REQUESTS)));
    }

    @Test
    @DisplayName("Should return null for defined properties that aren't base properties")
    void testGlobalPropertyFiltering() {
        BaseConfiguration config = BaseConfiguration.getInstance();
        config.resetForTesting("protifyai-booleans", Map.of());
        assertNull(config.getProperty(AIConfigProperty.MODEL));
    }

    @Test
    @DisplayName("Should load configuration from the file system")
    void testFilesystemLoading(@TempDir Path tempDir) throws IOException {

        String resourceName = "protifyai-valid.properties";
        String fileName = "protifyai-fs-valid.properties";
        Path targetFile = tempDir.resolve(fileName);

        try (var is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertNotNull(is, "Resource " + resourceName + " not found on classpath");
            Files.copy(is, targetFile);
        }

        BaseConfiguration config = BaseConfiguration.getInstance();
        config.resetForTesting("protifyai-fs-valid", Map.of(
                "PROTIFY_CFG_FILE_PATH", tempDir.toAbsolutePath().toString()
        ));

        assertEquals((Integer)1000, config.getProperty(AIConfigProperty.MAX_OUTPUT_TOKENS));
        assertEquals(0.75, config.getProperty(AIConfigProperty.TEMPERATURE));
        assertEquals("I am a pirate", config.getProperty(AIConfigProperty.INSTRUCTIONS));
        assertTrue(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.PRETTY_PRINT_JSON)));
        assertFalse(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_TRUNCATE_LARGE_INPUT)));
        assertTrue(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_RESPONSES)));
        assertTrue(() -> Boolean.TRUE.equals(config.getProperty(AIConfigProperty.LOG_REQUESTS)));

        assertNull(config.getProperty(AIConfigProperty.MODEL));
        assertNull(config.getProperty(AIConfigProperty.MODEL_EXPLICIT_VERSION));
    }

    @Test
    @DisplayName("Should throw exception if filesystem path and profile is provided but file does not exist")
    void testFilesystemPathProfileNotFound() {
        BaseConfiguration config = BaseConfiguration.getInstance();
        Map<String, String> env = Map.of(
                "PROTIFY_CFG_FILE_PATH", "/non/existent/path",
                "PROTIFY_CFG_PROFILE", "testing"
        );

        assertThrows(IllegalStateException.class, () -> config.resetForTesting("protifyai-fs", env));
    }

    @Test
    @DisplayName("Should throw exception if filesystem path is provided but file does not exist")
    void testFilesystemPathNotFound() {
        BaseConfiguration config = BaseConfiguration.getInstance();
        Map<String, String> env = Map.of("PROTIFY_CFG_FILE_PATH", "/non/existent/path");

        assertThrows(IllegalStateException.class, () -> config.resetForTesting("protifyai-fs", env));
    }

    @Test
    @DisplayName("Should throw exception if profile set but configuration not found")
    void testClasspathPathProfileNotFound() {
        BaseConfiguration config = BaseConfiguration.getInstance();
        Map<String, String> env = Map.of(
                "PROTIFY_CFG_PROFILE", "cp-profile"
        );

        assertThrows(IllegalStateException.class, () -> config.resetForTesting("protifyai-cp", env));
    }

    @Test
    @DisplayName("Check default retry policy configuration")
    void testDefaultRetryPolicy() {
        BaseConfiguration config = BaseConfiguration.getInstance();
        config.resetForTesting("protifyai-valid", Map.of());

        RetryPolicy defaultPolicy = RetryPolicy.DEFAULT;
        RetryPolicy policy = config.getProperty(AIConfigProperty.RETRY_POLICY);
        assertNotNull(policy);
        assertEquals(defaultPolicy.getBackoffStrategy(), policy.getBackoffStrategy());
        assertEquals(defaultPolicy.getDelayMillis(), policy.getDelayMillis());
        assertEquals(defaultPolicy.getMaxRetries(), policy.getMaxRetries());
        assertEquals(defaultPolicy.getMaxElapsedTimeMillis(), policy.getMaxElapsedTimeMillis());
        assertEquals(defaultPolicy.getJitterMillis(), policy.getJitterMillis());
        assertEquals(defaultPolicy.getMaxDelayMillis(), policy.getMaxDelayMillis());
        assertEquals(defaultPolicy.getRetryOnHttpStatusCodes(), policy.getRetryOnHttpStatusCodes());
        assertEquals(defaultPolicy.getRetryOnExceptions(), policy.getRetryOnExceptions());
        assertEquals(defaultPolicy.isRespectRetryAfter(), policy.isRespectRetryAfter());
    }


    @Test
    @DisplayName("Should build retry policy from configured retry policy settings")
    void testConfiguredRetryPolicySettings(@TempDir Path tempDir) throws IOException {
        String filenamePrefix = "protifyai-retry-settings";
        Path targetFile = tempDir.resolve(filenamePrefix + ".properties");

        Files.writeString(targetFile, String.join(System.lineSeparator(),
                "request.retryPolicy.maxRetries=3",
                "request.retryPolicy.backoffStrategy=fixed",
                "request.retryPolicy.delayMillis=1234",
                "request.retryPolicy.jitterMillis=55",
                "request.retryPolicy.maxDelayMillis=9999",
                "request.retryPolicy.maxElapsedTimeMillis=88888",
                "request.retryPolicy.retryOnHttpStatusCodes=429, 500, 500",
                "request.retryPolicy.retryOnExceptions=java.lang.IllegalArgumentException, java.lang.String, not.a.Class",
                "request.retryPolicy.respectRetryAfter=off"
        ));

        BaseConfiguration config = BaseConfiguration.getInstance();
        config.resetForTesting(filenamePrefix, Map.of(
                "PROTIFY_CFG_FILE_PATH", tempDir.toAbsolutePath().toString()
        ));

        RetryPolicy policy = config.getProperty(AIConfigProperty.RETRY_POLICY);
        assertNotNull(policy);

        assertEquals(3, policy.getMaxRetries());
        assertEquals(RetryBackoffStrategy.FIXED, policy.getBackoffStrategy());
        assertEquals(1234L, policy.getDelayMillis());
        assertEquals(55L, policy.getJitterMillis());
        assertEquals(9999L, policy.getMaxDelayMillis());
        assertEquals(88888L, policy.getMaxElapsedTimeMillis());

        assertEquals(Set.of(429, 500), policy.getRetryOnHttpStatusCodes());
        assertEquals(Set.of(IllegalArgumentException.class), policy.getRetryOnExceptions());
        assertFalse(policy.isRespectRetryAfter());
    }

    @SuppressWarnings({"java:S5778"})
    @Test
    @DisplayName("Should throw if configured retry backoff strategy is invalid")
    void testInvalidRetryBackoffStrategyThrows(@TempDir Path tempDir) throws IOException {
        String filenamePrefix = "protifyai-retry-invalid-backoff";
        Path targetFile = tempDir.resolve(filenamePrefix + ".properties");

        Files.writeString(targetFile, String.join(System.lineSeparator(),
                "request.retryPolicy.maxRetries=1",
                "request.retryPolicy.backoffStrategy=not-a-real-strategy"
        ));

        BaseConfiguration config = BaseConfiguration.getInstance();

        assertThrows(IllegalArgumentException.class, () -> config.resetForTesting(filenamePrefix, Map.of(
                "PROTIFY_CFG_FILE_PATH", tempDir.toAbsolutePath().toString()
        )));
    }

    @SuppressWarnings({"java:S5778"})
    @Test
    @DisplayName("Should throw if configured retry numeric value is negative")
    void testInvalidRetryNegativeValue(@TempDir Path tempDir) throws IOException {
        String filenamePrefix = "protifyai-retry-invalid-backoff";
        Path targetFile = tempDir.resolve(filenamePrefix + ".properties");

        Files.writeString(targetFile, String.join(System.lineSeparator(),
                "request.retryPolicy.maxRetries=-1"
        ));

        BaseConfiguration config = BaseConfiguration.getInstance();
        assertThrows(IllegalArgumentException.class, () -> config.resetForTesting(filenamePrefix, Map.of(
                "PROTIFY_CFG_FILE_PATH", tempDir.toAbsolutePath().toString()
        )));
    }

    @SuppressWarnings({"java:S5778"})
    @Test
    @DisplayName("Should throw if configured retry numeric value is over limit")
    void testInvalidRetryOverMaxValue(@TempDir Path tempDir) throws IOException {
        String filenamePrefix = "protifyai-retry-invalid-backoff";
        Path targetFile = tempDir.resolve(filenamePrefix + ".properties");

        Files.writeString(targetFile, String.join(System.lineSeparator(),
                "request.retryPolicy.maxRetries=50000000"
        ));

        BaseConfiguration config = BaseConfiguration.getInstance();
        assertThrows(IllegalArgumentException.class, () -> config.resetForTesting(filenamePrefix, Map.of(
                "PROTIFY_CFG_FILE_PATH", tempDir.toAbsolutePath().toString()
        )));
    }
}
