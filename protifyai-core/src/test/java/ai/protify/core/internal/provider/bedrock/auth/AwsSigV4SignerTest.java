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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AwsSigV4SignerTest {

    @Test
    @DisplayName("SHA-256 hex encoding produces correct hash for empty string")
    void sha256HexEmptyString() {
        // AWS SigV4 spec: SHA256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        String hash = AwsSigV4Signer.sha256Hex("");
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
    }

    @Test
    @DisplayName("SHA-256 hex encoding produces correct hash for known input")
    void sha256HexKnownInput() {
        // echo -n "hello" | sha256sum
        String hash = AwsSigV4Signer.sha256Hex("hello");
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hash);
    }

    @Test
    @DisplayName("HMAC-SHA256 produces correct output for known test vector")
    void hmacSha256KnownVector() {
        // HMAC-SHA256("key", "The quick brown fox jumps over the lazy dog")
        // = f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8
        byte[] result = AwsSigV4Signer.hmacSha256("key".getBytes(), "The quick brown fox jumps over the lazy dog");
        String hex = AwsSigV4Signer.hexEncode(result);
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8", hex);
    }

    @Test
    @DisplayName("Signature key derivation produces deterministic output")
    void signatureKeyDerivation() {
        byte[] key1 = AwsSigV4Signer.getSignatureKey("wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
                "20150830", "us-east-1", "iam");
        byte[] key2 = AwsSigV4Signer.getSignatureKey("wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
                "20150830", "us-east-1", "iam");
        assertArrayEquals(key1, key2);
        assertEquals(32, key1.length); // HMAC-SHA256 always produces 32 bytes
    }

    @Test
    @DisplayName("Sign produces Authorization header with correct format")
    void signProducesAuthorizationHeader() {
        AwsCredentials credentials = new AwsCredentials(
                "AKIAIOSFODNN7EXAMPLE",
                "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
                null
        );

        URI uri = URI.create("https://bedrock-runtime.us-east-1.amazonaws.com/model/test-model/converse");

        Map<String, String> result = AwsSigV4Signer.sign(
                "POST", uri, Map.of("content-type", "application/json"),
                "{\"messages\":[]}", credentials, "us-east-1", "bedrock"
        );

        // Verify required headers are present
        assertTrue(result.containsKey("Authorization"));
        assertTrue(result.containsKey("x-amz-date"));
        assertTrue(result.containsKey("x-amz-content-sha256"));

        // Verify Authorization format
        String auth = result.get("Authorization");
        assertTrue(auth.startsWith("AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/"));
        assertTrue(auth.contains("SignedHeaders="));
        assertTrue(auth.contains("Signature="));
        assertTrue(auth.contains("/us-east-1/bedrock/aws4_request"));

        // Verify no security token header when not provided
        assertFalse(result.containsKey("x-amz-security-token"));
    }

    @Test
    @DisplayName("Sign includes security token header when session token is present")
    void signIncludesSecurityToken() {
        AwsCredentials credentials = new AwsCredentials(
                "AKIAIOSFODNN7EXAMPLE",
                "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
                "FwoGZXIvYXdzE..."
        );

        URI uri = URI.create("https://bedrock-runtime.us-east-1.amazonaws.com/model/test-model/converse");

        Map<String, String> result = AwsSigV4Signer.sign(
                "POST", uri, Map.of("content-type", "application/json"),
                "{}", credentials, "us-east-1", "bedrock"
        );

        assertTrue(result.containsKey("x-amz-security-token"));
        assertEquals("FwoGZXIvYXdzE...", result.get("x-amz-security-token"));
    }

    @Test
    @DisplayName("Content SHA-256 is correctly computed for request body")
    void contentSha256IsCorrect() {
        AwsCredentials credentials = new AwsCredentials(
                "AKIAIOSFODNN7EXAMPLE",
                "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
                null
        );

        String body = "{\"messages\":[{\"role\":\"user\",\"content\":[{\"text\":\"Hello\"}]}]}";
        URI uri = URI.create("https://bedrock-runtime.us-east-1.amazonaws.com/model/test/converse");

        Map<String, String> result = AwsSigV4Signer.sign(
                "POST", uri, null, body, credentials, "us-east-1", "bedrock"
        );

        String expectedHash = AwsSigV4Signer.sha256Hex(body);
        assertEquals(expectedHash, result.get("x-amz-content-sha256"));
    }

    @Test
    @DisplayName("Hex encoding produces correct output")
    void hexEncoding() {
        byte[] input = {0x00, (byte) 0xff, 0x10, (byte) 0xab};
        assertEquals("00ff10ab", AwsSigV4Signer.hexEncode(input));
    }
}
