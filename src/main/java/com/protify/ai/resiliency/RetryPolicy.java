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

package com.protify.ai.resiliency;

import java.util.Set;

public class RetryPolicy {

    private final int maxRetries;
    private final RetryBackoffStrategy backoffStrategy;
    private final long delayMillis;
    private final long jitterMillis;
    private final long maxDelayMillis;
    private final long maxElapsedTimeMillis;
    private final Set<Integer> retryOnHttpStatusCodes;
    private final Set<Class<? extends Exception>> retryOnExceptions;
    private final boolean respectRetryAfter;

    public static final RetryPolicy DEFAULT = builder()
            .maxRetries(0)
            .backoffStrategy(RetryBackoffStrategy.EXPONENTIAL)
            .delayMillis(500L)
            .maxDelayMillis(10000L)
            .jitterMillis(200L)
            .maxElapsedTimeMillis(20000L)
            .retryOnHttpStatusCodes(Set.of(429, 500, 502, 503, 504, 408))
            .retryOnExceptions(Set.of(RuntimeException.class))
            .respectRetryAfter(true)
            .build();

    @SuppressWarnings({"java:S107"})
    protected RetryPolicy(int maxRetries,
                       RetryBackoffStrategy backoffStrategy,
                       long delayMillis,
                       long jitterMillis,
                       long maxDelayMillis,
                       long maxElapsedTimeMillis,
                       Set<Integer> retryOnHttpStatusCodes,
                       Set<Class<? extends Exception>> retryOnExceptions,
                       boolean respectRetryAfter) {
        this.maxRetries = maxRetries;
        this.backoffStrategy = backoffStrategy;
        this.delayMillis = delayMillis;
        this.jitterMillis = jitterMillis;
        this.maxDelayMillis = maxDelayMillis;
        this.maxElapsedTimeMillis = maxElapsedTimeMillis;
        this.retryOnHttpStatusCodes = retryOnHttpStatusCodes;
        this.retryOnExceptions = retryOnExceptions;
        this.respectRetryAfter = respectRetryAfter;
    }

    public static RetryPolicyBuilder builder() {
        return new RetryPolicyBuilder();
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public RetryBackoffStrategy getBackoffStrategy() {
        return backoffStrategy;
    }

    public long getDelayMillis() {
        return delayMillis;
    }

    public long getJitterMillis() {
        return jitterMillis;
    }

    public long getMaxDelayMillis() {
        return maxDelayMillis;
    }

    public long getMaxElapsedTimeMillis() {
        return maxElapsedTimeMillis;
    }

    public Set<Integer> getRetryOnHttpStatusCodes() {
        return retryOnHttpStatusCodes;
    }

    public Set<Class<? extends Exception>> getRetryOnExceptions() {
        return retryOnExceptions;
    }

    public boolean isRespectRetryAfter() {
        return respectRetryAfter;
    }

    @Override
    public String toString() {
        return "RetryPolicy{" +
                "maxRetries=" + maxRetries +
                ", backoffStrategy=" + backoffStrategy +
                ", delayMillis=" + delayMillis +
                ", jitterMillis=" + jitterMillis +
                ", maxDelayMillis=" + maxDelayMillis +
                ", maxElapsedTimeMillis=" + maxElapsedTimeMillis +
                ", retryOnHttpStatusCodes=" + retryOnHttpStatusCodes +
                ", retryOnExceptions=" + retryOnExceptions +
                ", respectRetryAfter=" + respectRetryAfter +
                '}';
    }
}
