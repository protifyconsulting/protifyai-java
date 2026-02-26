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

package ai.protify.core.resiliency;

import java.util.Set;

public class RetryPolicyBuilder {

    private static final int MAX_MAX_RETRIES = 10;
    private static final long MAX_DELAY_MILLIS = 60_000L;
    private static final long MAX_JITTER_MILLIS = 60_000L;
    private static final long MAX_MAX_DELAY_MILLIS = 5 * 60_000L;
    private static final long MAX_ELAPSED_TIME_MILLIS = 15 * 60_000L;

    private Integer maxRetries;
    private RetryBackoffStrategy backoffStrategy;
    private Long delayMillis;
    private Long jitterMillis;
    private Long maxDelayMillis;
    private Long maxElapsedTimeMillis;
    private Set<Integer> retryOnHttpStatusCodes;
    private Set<Class<? extends Exception>> retryOnExceptions;
    private Boolean respectRetryAfter;

    public RetryPolicyBuilder maxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public RetryPolicyBuilder backoffStrategy(RetryBackoffStrategy backoffStrategy) {
        this.backoffStrategy = backoffStrategy;
        return this;
    }

    public RetryPolicyBuilder delayMillis(Long delayMillis) {
        this.delayMillis = delayMillis;
        return this;
    }

    public RetryPolicyBuilder jitterMillis(Long jitterMillis) {
        this.jitterMillis = jitterMillis;
        return this;
    }

    public RetryPolicyBuilder maxDelayMillis(Long maxDelayMillis) {
        this.maxDelayMillis = maxDelayMillis;
        return this;
    }

    public RetryPolicyBuilder maxElapsedTimeMillis(Long maxElapsedTimeMillis) {
        this.maxElapsedTimeMillis = maxElapsedTimeMillis;
        return this;
    }

    public RetryPolicyBuilder retryOnHttpStatusCodes(Set<Integer> retryOnHttpStatusCodes) {
        this.retryOnHttpStatusCodes = retryOnHttpStatusCodes;
        return this;
    }

    public RetryPolicyBuilder retryOnExceptions(Set<Class<? extends Exception>> retryOnExceptions) {
        this.retryOnExceptions = retryOnExceptions;
        return this;
    }

    public RetryPolicyBuilder respectRetryAfter(Boolean respectRetryAfter) {
        this.respectRetryAfter = respectRetryAfter;
        return this;
    }

    public RetryPolicy build() {

        RetryPolicy defaultPolicy = RetryPolicy.DEFAULT;

        RetryPolicy retryPolicy = new RetryPolicy(
                maxRetries == null ? defaultPolicy.getMaxRetries() : maxRetries,
                backoffStrategy == null ? defaultPolicy.getBackoffStrategy() : backoffStrategy,
                delayMillis == null ? defaultPolicy.getDelayMillis() : delayMillis,
                jitterMillis == null ? defaultPolicy.getJitterMillis() : jitterMillis,
                maxDelayMillis == null ? defaultPolicy.getMaxDelayMillis() : maxDelayMillis,
                maxElapsedTimeMillis == null ? defaultPolicy.getMaxElapsedTimeMillis() : maxElapsedTimeMillis,
                retryOnHttpStatusCodes == null ? defaultPolicy.getRetryOnHttpStatusCodes() : retryOnHttpStatusCodes,
                retryOnExceptions == null ? defaultPolicy.getRetryOnExceptions() : retryOnExceptions,
                respectRetryAfter == null ? defaultPolicy.isRespectRetryAfter() : respectRetryAfter);

        validate(retryPolicy);
        return retryPolicy;
    }

    @SuppressWarnings({"java:S3776","java:S6541"})
    private void validate(RetryPolicy retryPolicy) {

        if (retryPolicy == null) {
            throw new IllegalArgumentException("retryPolicy must not be null");
        }

        if (retryPolicy.getBackoffStrategy() == null) {
            throw new IllegalArgumentException("backoffStrategy must not be null");
        }

        if (retryPolicy.getMaxRetries() < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0");
        }
        if (retryPolicy.getMaxRetries() > MAX_MAX_RETRIES) {
            throw new IllegalArgumentException("maxRetries must be <= " + MAX_MAX_RETRIES);
        }

        if (retryPolicy.getDelayMillis() < 0) {
            throw new IllegalArgumentException("delayMillis must be >= 0");
        }
        if (retryPolicy.getDelayMillis() > MAX_DELAY_MILLIS) {
            throw new IllegalArgumentException("delayMillis must be <= " + MAX_DELAY_MILLIS);
        }

        if (retryPolicy.getJitterMillis() < 0) {
            throw new IllegalArgumentException("jitterMillis must be >= 0");
        }
        if (retryPolicy.getJitterMillis() > MAX_JITTER_MILLIS) {
            throw new IllegalArgumentException("jitterMillis must be <= " + MAX_JITTER_MILLIS);
        }

        if (retryPolicy.getMaxDelayMillis() < 0) {
            throw new IllegalArgumentException("maxDelayMillis must be >= 0");
        }
        if (retryPolicy.getMaxDelayMillis() > MAX_MAX_DELAY_MILLIS) {
            throw new IllegalArgumentException("maxDelayMillis must be <= " + MAX_MAX_DELAY_MILLIS);
        }

        if (retryPolicy.getMaxElapsedTimeMillis() < 0) {
            throw new IllegalArgumentException("maxElapsedTimeMillis must be >= 0");
        }
        if (retryPolicy.getMaxElapsedTimeMillis() > MAX_ELAPSED_TIME_MILLIS) {
            throw new IllegalArgumentException("maxElapsedTimeMillis must be <= " + MAX_ELAPSED_TIME_MILLIS);
        }

        // Relationship checks (common-sense constraints)
        if (retryPolicy.getMaxDelayMillis() > 0 && retryPolicy.getDelayMillis() > retryPolicy.getMaxDelayMillis()) {
            throw new IllegalArgumentException("delayMillis must be <= maxDelayMillis");
        }

        // If the user supplies jitter, it should not dwarf the delay cap (helps avoid surprising huge sleeps)
        if (retryPolicy.getMaxDelayMillis() > 0 && retryPolicy.getJitterMillis() > retryPolicy.getMaxDelayMillis()) {
            throw new IllegalArgumentException("jitterMillis must be <= maxDelayMillis");
        }

        // If a total retry window is configured, it should be able to accommodate at least one wait.
        // (This doesn’t need to be strict math; just catches obviously impossible configs.)
        if (retryPolicy.getMaxRetries() > 0 && retryPolicy.getMaxElapsedTimeMillis() > 0) {
            long minOneWait = retryPolicy.getDelayMillis();
            if (retryPolicy.getMaxElapsedTimeMillis() < minOneWait) {
                throw new IllegalArgumentException("maxElapsedTimeMillis must be >= delayMillis when maxRetries > 0");
            }
        }

        Set<Integer> statusCodes = retryPolicy.getRetryOnHttpStatusCodes();
        if (statusCodes != null) {
            for (Integer code : statusCodes) {
                if (code == null) {
                    throw new IllegalArgumentException("retryOnHttpStatusCodes must not contain null");
                }
                // HTTP status codes are defined in the 100-599 range
                if (code < 100 || code > 599) {
                    throw new IllegalArgumentException("Invalid HTTP status code in retryOnHttpStatusCodes: " + code);
                }
            }
        }

        Set<Class<? extends Exception>> exceptions = retryPolicy.getRetryOnExceptions();
        if (exceptions != null) {
            for (Class<? extends Exception> exClass : exceptions) {
                if (exClass == null) {
                    throw new IllegalArgumentException("retryOnExceptions must not contain null");
                }
                // Defensive: ensure it's actually an Exception type (should already be enforced by generics)
                if (!Exception.class.isAssignableFrom(exClass)) {
                    throw new IllegalArgumentException("retryOnExceptions contains non-Exception type: " + exClass.getName());
                }
            }
        }

        // If no retries are allowed, we don't need retry conditions; but if retries ARE allowed,
        // it's usually a misconfiguration to have nothing to retry on.
        if (retryPolicy.getMaxRetries() > 0) {
            boolean hasStatusCodes = statusCodes != null && !statusCodes.isEmpty();
            boolean hasExceptions = exceptions != null && !exceptions.isEmpty();
            if (!hasStatusCodes && !hasExceptions) {
                throw new IllegalArgumentException(
                        "maxRetries > 0 but no retry conditions configured (retryOnHttpStatusCodes/retryOnExceptions are empty)"
                );
            }
        }
    }
}
