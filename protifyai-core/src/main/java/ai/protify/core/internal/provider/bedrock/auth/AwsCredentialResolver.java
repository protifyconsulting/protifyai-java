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

package ai.protify.core.internal.provider.bedrock.auth;

import ai.protify.core.internal.config.AIConfigProperty;
import ai.protify.core.internal.config.Configuration;

public final class AwsCredentialResolver {

    private AwsCredentialResolver() {
    }

    public static AwsCredentials resolve(Configuration configuration) {
        String accessKeyId = configuration.getProperty(AIConfigProperty.AWS_ACCESS_KEY_ID);
        String secretAccessKey = configuration.getProperty(AIConfigProperty.AWS_SECRET_ACCESS_KEY);
        String sessionToken = configuration.getProperty(AIConfigProperty.AWS_SESSION_TOKEN);

        if (accessKeyId == null || accessKeyId.isEmpty()) {
            accessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
        }
        if (secretAccessKey == null || secretAccessKey.isEmpty()) {
            secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        }
        if (sessionToken == null || sessionToken.isEmpty()) {
            sessionToken = System.getenv("AWS_SESSION_TOKEN");
        }

        if (accessKeyId == null || accessKeyId.isEmpty()) {
            throw new IllegalStateException(
                    "AWS access key ID is required. Set it via .awsAccessKeyId() on the builder "
                    + "or the AWS_ACCESS_KEY_ID environment variable.");
        }
        if (secretAccessKey == null || secretAccessKey.isEmpty()) {
            throw new IllegalStateException(
                    "AWS secret access key is required. Set it via .awsSecretAccessKey() on the builder "
                    + "or the AWS_SECRET_ACCESS_KEY environment variable.");
        }

        return new AwsCredentials(accessKeyId, secretAccessKey, sessionToken);
    }
}
