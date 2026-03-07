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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class AwsSigV4Signer {

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final DateTimeFormatter ISO8601_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private AwsSigV4Signer() {
    }

    public static Map<String, String> sign(
            String method,
            URI uri,
            Map<String, String> headers,
            String body,
            AwsCredentials credentials,
            String region,
            String service) {

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = now.format(ISO8601_FORMAT);
        String dateStamp = now.format(DATE_FORMAT);

        // Build the headers map with required signing headers
        TreeMap<String, String> sortedHeaders = new TreeMap<>();
        sortedHeaders.put("host", uri.getHost());
        sortedHeaders.put("x-amz-date", amzDate);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                sortedHeaders.put(entry.getKey().toLowerCase(), entry.getValue().trim());
            }
        }
        if (credentials.hasSessionToken()) {
            sortedHeaders.put("x-amz-security-token", credentials.getSessionToken());
        }

        String bodyHash = sha256Hex(body != null ? body : "");
        sortedHeaders.put("x-amz-content-sha256", bodyHash);

        // Canonical headers and signed headers
        StringBuilder canonicalHeadersBuilder = new StringBuilder();
        StringBuilder signedHeadersBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedHeaders.entrySet()) {
            canonicalHeadersBuilder.append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
            if (signedHeadersBuilder.length() > 0) {
                signedHeadersBuilder.append(';');
            }
            signedHeadersBuilder.append(entry.getKey());
        }
        String canonicalHeaders = canonicalHeadersBuilder.toString();
        String signedHeaders = signedHeadersBuilder.toString();

        // Canonical request
        String canonicalUri = uri.getRawPath() != null && !uri.getRawPath().isEmpty()
                ? uri.getRawPath() : "/";
        String canonicalQueryString = uri.getRawQuery() != null ? uri.getRawQuery() : "";

        String canonicalRequest = method + "\n"
                + canonicalUri + "\n"
                + canonicalQueryString + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + bodyHash;

        // Credential scope
        String credentialScope = dateStamp + "/" + region + "/" + service + "/aws4_request";

        // String to sign
        String stringToSign = ALGORITHM + "\n"
                + amzDate + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest);

        // Signing key derivation
        byte[] signingKey = getSignatureKey(
                credentials.getSecretAccessKey(), dateStamp, region, service);

        // Signature
        String signature = hexEncode(hmacSha256(signingKey, stringToSign));

        // Authorization header
        String authorizationHeader = ALGORITHM + " "
                + "Credential=" + credentials.getAccessKeyId() + "/" + credentialScope + ", "
                + "SignedHeaders=" + signedHeaders + ", "
                + "Signature=" + signature;

        // Build result headers
        Map<String, String> result = new LinkedHashMap<>();
        result.put("Authorization", authorizationHeader);
        result.put("x-amz-date", amzDate);
        result.put("x-amz-content-sha256", bodyHash);
        if (credentials.hasSessionToken()) {
            result.put("x-amz-security-token", credentials.getSessionToken());
        }

        return result;
    }

    static byte[] getSignatureKey(String key, String dateStamp, String region, String service) {
        byte[] kDate = hmacSha256(("AWS4" + key).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, service);
        return hmacSha256(kService, "aws4_request");
    }

    static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key, HMAC_SHA256));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256", e);
        }
    }

    static String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return hexEncode(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute SHA-256", e);
        }
    }

    static String hexEncode(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
