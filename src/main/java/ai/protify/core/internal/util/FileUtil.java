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

package ai.protify.core.internal.util;

import ai.protify.core.internal.exception.ProtifyApiException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class FileUtil {

    private FileUtil() {
    }

    public static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1).toLowerCase();
    }

    public static String extractExtensionFromDataUrl(String dataUrl) {
        try {
            // Format: data:image/png;base64,xxxx
            int colonIndex = dataUrl.indexOf(':');
            int semicolonIndex = dataUrl.indexOf(';');
            if (colonIndex != -1 && semicolonIndex != -1 && semicolonIndex > colonIndex) {
                String mimeType = dataUrl.substring(colonIndex + 1, semicolonIndex);
                return mimeType.substring(mimeType.lastIndexOf('/') + 1).toLowerCase();
            } else {
                throw new IllegalArgumentException("Invalid data URL: " + dataUrl);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid data URL: " + dataUrl);
        }
    }

    public static String computeSHA256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new ProtifyApiException("SHA-256 algorithm not found", e);
        }
    }
}
