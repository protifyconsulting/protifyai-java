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
import com.protify.ai.resiliency.RetryBackoffStrategy;
import com.protify.ai.resiliency.RetryPolicy;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


public final class BaseConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseConfiguration.class);

    private static final Set<String> TRUE_VALUES = Set.of("true", "yes", "on", "1", "enabled");
    private static final Set<String> FALSE_VALUES = Set.of("false", "no", "off", "0", "disabled");

    static final String PROFILE_ENV_VAR = "PROTIFY_CFG_PROFILE";
    static final String PROPERTY_FILE_PATH_ENV_VAR = "PROTIFY_CFG_FILE_PATH";

    static final Set<AIConfigProperty> BASE_PROPERTY_SET = EnumSet.of(
            AIConfigProperty.PROTIFY_API_KEY,
            AIConfigProperty.API_KEY_URL,
            AIConfigProperty.API_KEY_URL_TIMEOUT_MS,
            AIConfigProperty.API_KEY_CACHE_TTL_SECS,
            AIConfigProperty.MAX_OUTPUT_TOKENS,
            AIConfigProperty.TEMPERATURE,
            AIConfigProperty.INSTRUCTIONS,
            AIConfigProperty.PRETTY_PRINT_JSON,
            AIConfigProperty.LOG_REQUESTS,
            AIConfigProperty.LOG_TRUNCATE_LARGE_INPUT,
            AIConfigProperty.LOG_RESPONSES,

            AIConfigProperty.RETRY_POLICY,
            AIConfigProperty.RETRY_MAX_RETRIES,
            AIConfigProperty.RETRY_BACKOFF_STRATEGY,
            AIConfigProperty.RETRY_DELAY_MS,
            AIConfigProperty.RETRY_JITTER_MS,
            AIConfigProperty.RETRY_MAX_DELAY_MS,
            AIConfigProperty.RETRY_MAX_ELAPSED_TIME_MS,
            AIConfigProperty.RETRY_ON_EXCEPTIONS,
            AIConfigProperty.RETRY_ON_HTTP_STATUS,
            AIConfigProperty.RETRY_RESPECT_RETRY_AFTER,

            AIConfigProperty.RESPONSE_CACHE_MAX_ENTRIES,
            AIConfigProperty.RESPONSE_CACHE_TTL_SECS,
            AIConfigProperty.REQUEST_TIMEOUT_MS
    );

    private static final BaseConfiguration INSTANCE = new BaseConfiguration();

    private volatile String PROPERTY_FILE_PREFIX = "protifyai";

    // Suppress warning because ConcurrentHashMap is completely swapped out with unmodifiableMap
    // when loaded or reloaded.  This should only ever come into play during unit testing
    // as this class is a singleton and the properties should effectively be immutable.
    @SuppressWarnings({"java:S3077"})
    private volatile Map<AIConfigProperty, Object> properties = new EnumMap<>(AIConfigProperty.class);
    private volatile Configuration configuration = new Configuration(properties);
    private volatile String logMessage = "";

    private BaseConfiguration() {
        loadProperties();
    }

    public static BaseConfiguration getInstance() {
        return INSTANCE;
    }

    public <T> T getProperty(AIConfigProperty property) {
        return getProperty(property, properties);
    }

    private Map<AIConfigProperty, Object> loadDefaults() {
        Map<AIConfigProperty, Object> propertiesToSet = new EnumMap<>(AIConfigProperty.class);

        // Initialize with library defaults
        for (AIConfigProperty property : AIConfigProperty.values()) {
            Object defaultValue = property.getDefaultValue(property.getType());
            if (defaultValue != null) {
                propertiesToSet.put(property, defaultValue);
            }
        }
        LOGGER.debug("Default configuration loaded; additional configuration may override these values.");
        return propertiesToSet;
    }

    private void loadProperties() {
        LOGGER.info("Loading base configuration properties");

        Map<AIConfigProperty, Object> propertiesToSet = loadDefaults();

        String profile = getEnv(PROFILE_ENV_VAR);
        String filePath = getEnv(PROPERTY_FILE_PATH_ENV_VAR);

        LOGGER.debug("Checking environment variables");
        LOGGER.debug("PROFILE_ENV_VAR: {}, PROPERTY_FILE_PATH_ENV_VAR: {}", profile, filePath);

        Map<AIConfigProperty, Object> newProperties = null;

        // 1. Check Filesystem + Profile
        if (filePath != null && profile != null) {
            newProperties = tryLoadFromFilesystem(
                    filePath, String.format(getProfilePropertyFileName(profile), profile));
        }

        // 2. Check Filesystem + Base
        if (filePath != null && newProperties == null) {
            newProperties = tryLoadFromFilesystem(filePath, getPropertyFileName());
        }

        // 3. Check Classpath + Profile
        if (profile != null && newProperties == null) {
            newProperties = tryLoadFromClasspath(String.format(getProfilePropertyFileName(profile), profile), true);
        }

        // 4. Check Classpath + Base
        if (newProperties == null) {
            newProperties = tryLoadFromClasspath(getPropertyFileName(), false);
        }

        if (newProperties == null) {
            LOGGER.info("No configuration file found. Using library defaults.");
            newProperties = Collections.emptyMap();
        }

        propertiesToSet.putAll(newProperties);
        propertiesToSet.put(AIConfigProperty.RETRY_POLICY, createRetryPolicy(newProperties));
        this.properties = Collections.unmodifiableMap(propertiesToSet);
        this.configuration = new Configuration(properties);
        this.logMessage = constructLogMessage();
        logConfigurationInternal();
    }

    private Map<AIConfigProperty, Object> tryLoadFromFilesystem(String path, String filename) {
        var file = new File(path, filename);
        LOGGER.debug("Loading configuration from file system: {}", file.getAbsolutePath());
        if (file.exists() && file.isFile()) {
            try (var fis = new FileInputStream(file)) {
                Map<AIConfigProperty, Object> newProperties = parseAndLoad(fis);
                LOGGER.info("Configuration loaded from filesystem: {}", file.getAbsolutePath());
                return newProperties;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            throw new IllegalStateException(String.format(
                    "Configuration path was specified via %s, but file was not found: %s",
                    PROPERTY_FILE_PATH_ENV_VAR, file.getAbsolutePath()
            ));
        }
    }

    @SuppressWarnings({"java:S1168"})
    private Map<AIConfigProperty, Object> tryLoadFromClasspath(String filename, boolean isProfile) {
        LOGGER.info("Loading configuration from classpath: {}", filename);
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)) {
            if (is != null) {
                Map<AIConfigProperty, Object> newProperties = parseAndLoad(is);
                LOGGER.info("Configuration loaded from classpath: {}", filename);
                return newProperties;
            } else {
                if (isProfile) {
                    throw new IllegalStateException(String.format(
                            "Configuration file was not found: %s",
                            filename
                    ));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    private Map<AIConfigProperty, Object> parseAndLoad(InputStream inputStream) throws IOException {
        Map<AIConfigProperty, Object> newProperties = new EnumMap<>(AIConfigProperty.class);

        var props = new Properties();
        props.load(inputStream);

        for (AIConfigProperty property : BASE_PROPERTY_SET) {
            String value = props.getProperty(property.getName());
            Object convertedValue = convertValue(value, property.getType());
            if (convertedValue != null) {
                newProperties.put(property, convertedValue);
            }
        }
        return newProperties;
    }

    @SuppressWarnings("java:S3776")
    private static Object convertValue(String value, Class<?> type) {
        if (value == null) return null;
        try {
            if (type == String.class) return value;
            if (type == Integer.class || type == int.class) return Integer.parseInt(value);
            if (type == Long.class || type == long.class) return Long.parseLong(value);
            if (type == Boolean.class || type == boolean.class) {
                String v = value.trim().toLowerCase(Locale.ROOT);
                if (TRUE_VALUES.contains(v)) return true;
                if (FALSE_VALUES.contains(v)) return false;
                return null;
            }
            if (type == Double.class || type == double.class) return Double.parseDouble(value);
            if (type == Float.class || type == float.class) return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("Failed to convert value '{}' to type '{}': {}", value, type.getName(), e.getMessage());
            return null;
        }
        return value;
    }

    public void logConfigurationInternal() {
        LOGGER.debug(this.logMessage);
    }

    public void logConfiguration() {
        if (!LOGGER.isInfoEnabled()) {
            LOGGER.warn("Cannot log configuration info; check the log level (INFO) to enable this feature.");
        }
        LOGGER.info(this.logMessage);
    }

    public String constructLogMessage() {
        String nl = System.lineSeparator();
        var sb = new StringBuilder();
        sb.append(nl).append("--- Protify AI Base Configuration ---").append(nl);

        for (AIConfigProperty property : AIConfigProperty.values()) {
            Object value = properties.get(property);
            String displayValue;
            if (value == null) { displayValue = "Not Set"; }
            else {
                if (property.isSecret()) { displayValue = "*****"; }
                else { displayValue = value.toString(); }
            }

            sb.append(String.format("  %-30s : %s", property.getName(), displayValue)).append(nl);
        }
        sb.append("--------------------------------------");
        return sb.toString();
    }

    // This should only every come into play in unit tests
    @SuppressWarnings({"java:S3077"})
    private volatile Map<String, String> envOverrides = null;

    private String getPropertyFileName() {
        return PROPERTY_FILE_PREFIX + ".properties";
    }

    private String getProfilePropertyFileName(String profile) {
        return String.format("%s-%s.properties", PROPERTY_FILE_PREFIX, profile);
    }

    private String getEnv(String varName) {
        if (envOverrides != null) {
            return envOverrides.get(varName);
        }
        return System.getenv(varName);
    }

    synchronized void resetForTesting(String filenamePrefix, Map<String, String> envOverrides) {
        LOGGER.info("Resetting configuration for testing");
        LOGGER.info("Using filename prefix: {}", filenamePrefix);
        LOGGER.info("Using environment overrides: {}", envOverrides);
        this.PROPERTY_FILE_PREFIX = filenamePrefix;
        this.envOverrides = envOverrides != null ? new HashMap<>(envOverrides) : new HashMap<>();
        loadProperties();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void initialize() {
        getInstance();
    }

    public static Configuration getBaseConfiguration() {
        BaseConfiguration config = getInstance();
        return config.configuration;
    }

    private RetryPolicy createRetryPolicy(Map<AIConfigProperty, Object> newProperties) {

        Set<Integer> retryHttpCodeSet = null;
        String retryHttpCodes = getProperty(AIConfigProperty.RETRY_ON_HTTP_STATUS, newProperties);
        if (retryHttpCodes != null) {
            retryHttpCodeSet = Arrays.stream(retryHttpCodes.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
        }

        Set<Class<? extends Exception>> retryExceptionSet = null;
        String retryExceptions = getProperty(AIConfigProperty.RETRY_ON_EXCEPTIONS, newProperties);
        if (retryExceptions != null) {
            retryExceptionSet = Arrays.stream(retryExceptions.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(BaseConfiguration::toExceptionClass)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        return RetryPolicy.builder()
                .maxRetries(getProperty(AIConfigProperty.RETRY_MAX_RETRIES, newProperties))
                .backoffStrategy(RetryBackoffStrategy
                        .getFromValue(getProperty(AIConfigProperty.RETRY_BACKOFF_STRATEGY, newProperties)))
                .delayMillis(getProperty(AIConfigProperty.RETRY_DELAY_MS, newProperties))
                .jitterMillis(getProperty(AIConfigProperty.RETRY_JITTER_MS, newProperties))
                .maxDelayMillis(getProperty(AIConfigProperty.RETRY_MAX_DELAY_MS, newProperties))
                .maxElapsedTimeMillis(getProperty(AIConfigProperty.RETRY_MAX_ELAPSED_TIME_MS, newProperties))
                .retryOnHttpStatusCodes(retryHttpCodeSet)
                .retryOnExceptions(retryExceptionSet)
                .respectRetryAfter(getProperty(AIConfigProperty.RETRY_RESPECT_RETRY_AFTER, newProperties))
                .build();
    }

    @SuppressWarnings("unchecked")
    private <T> T getProperty(AIConfigProperty property, Map<AIConfigProperty, Object> props) {
        if (!BASE_PROPERTY_SET.contains(property)) {
            LOGGER.warn("Property {} is not a valid base property.", property.getName());
            return null;
        }
        return (T) property.getType().cast(props.get(property));
    }

    private static Class<? extends Exception> toExceptionClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!Exception.class.isAssignableFrom(clazz)) {
                LOGGER.warn("Configured retry exception '{}' is not an Exception subtype; ignoring.", className);
                return null;
            }
            @SuppressWarnings("unchecked")
            Class<? extends Exception> exClass = (Class<? extends Exception>) clazz;
            return exClass;
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Configured retry exception class '{}' was not found; ignoring.", className);
            return null;
        } catch (LinkageError e) {
            LOGGER.warn("Configured retry exception class '{}' could not be loaded ({}); ignoring.",
                    className, e.getMessage());
            return null;
        }
    }
}
