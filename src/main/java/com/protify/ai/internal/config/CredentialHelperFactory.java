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

public final class CredentialHelperFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialHelperFactory.class);
    private static final String SYSTEM_PROPERTY = "protify.credhelper.impl";

    private static volatile CredentialHelper instance;

    private CredentialHelperFactory() {
    }

    public static CredentialHelper getInstance() {
        if (instance == null) {
            synchronized (CredentialHelperFactory.class) {
                if (instance == null) {
                    instance = createInstance();
                }
            }
        }
        return instance;
    }

    private static CredentialHelper createInstance() {
        String implClass = System.getProperty(SYSTEM_PROPERTY);

        if (implClass == null || implClass.isBlank()) {
            LOGGER.debug("No {} system property set. Using DefaultCredentialHelper.", SYSTEM_PROPERTY);
            return new DefaultCredentialHelper();
        }

        try {
            Class<?> clazz = Class.forName(implClass);
            Object obj = clazz.getDeclaredConstructor().newInstance();
            if (!(obj instanceof CredentialHelper)) {
                throw new IllegalArgumentException(implClass + " does not implement CredentialHelper");
            }
            LOGGER.info("Using custom CredentialHelper: {}", implClass);
            return (CredentialHelper) obj;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate CredentialHelper: " + implClass, e);
        }
    }

    // Visible for testing
    static void reset() {
        synchronized (CredentialHelperFactory.class) {
            instance = null;
        }
    }
}
