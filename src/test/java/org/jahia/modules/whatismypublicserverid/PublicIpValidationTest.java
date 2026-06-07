package org.jahia.modules.whatismypublicserverid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PublicIp#isValidIpLiteral(String)}.
 *
 * <p>The method is private; we invoke it via reflection to keep the production API surface
 * unchanged while still verifying the validation logic in isolation.
 */
@DisplayName("PublicIp IP-literal validation")
class PublicIpValidationTest {

    // ---------------------------------------------------------------------------
    // Reflection helper
    // ---------------------------------------------------------------------------

    private static boolean invokeIsValidIpLiteral(String candidate) {
        try {
            final Method method = PublicIp.class.getDeclaredMethod("isValidIpLiteral", String.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(null, candidate);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke isValidIpLiteral", e);
        }
    }

    // ---------------------------------------------------------------------------
    // Valid IPv4 addresses
    // ---------------------------------------------------------------------------

    @ParameterizedTest(name = "valid IPv4: {0}")
    @ValueSource(strings = {
            "1.2.3.4",
            "0.0.0.0",
            "255.255.255.255",
            "192.168.0.1",
            "10.0.0.1",
            "172.16.254.1"
    })
    @DisplayName("should accept well-formed IPv4 addresses")
    void shouldAcceptValidIpv4(String ip) {
        assertTrue(invokeIsValidIpLiteral(ip), "Expected valid IPv4: " + ip);
    }

    // ---------------------------------------------------------------------------
    // Valid IPv6 addresses
    // ---------------------------------------------------------------------------

    @ParameterizedTest(name = "valid IPv6: {0}")
    @ValueSource(strings = {
            "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
            "::1",
            "::",
            "fe80::1",
            "2001:db8::1"
    })
    @DisplayName("should accept well-formed IPv6 addresses")
    void shouldAcceptValidIpv6(String ip) {
        assertTrue(invokeIsValidIpLiteral(ip), "Expected valid IPv6: " + ip);
    }

    // ---------------------------------------------------------------------------
    // Injection / malicious inputs
    // ---------------------------------------------------------------------------

    @ParameterizedTest(name = "injection attempt: {0}")
    @ValueSource(strings = {
            "1.2.3.4; rm -rf /",
            "1.2.3.4\n",
            "1.2.3.4\r\n",
            "<script>alert(1)</script>",
            "'; DROP TABLE users; --",
            "$(hostname)",
            "1.2.3.4 OR 1=1",
            "http://evil.com/",
            "../../etc/passwd",
            "1.2.3.4%0aSet-Cookie:foo=bar"
    })
    @DisplayName("should reject injection-attempt inputs")
    void shouldRejectInjectionAttempts(String malicious) {
        assertFalse(invokeIsValidIpLiteral(malicious), "Expected rejection of: " + malicious);
    }

    // ---------------------------------------------------------------------------
    // Empty / null / oversized inputs
    // ---------------------------------------------------------------------------

    @ParameterizedTest(name = "empty/null/oversized: [{0}]")
    @NullSource
    @DisplayName("should reject null")
    void shouldRejectNull(String candidate) {
        assertFalse(invokeIsValidIpLiteral(candidate));
    }

    @ParameterizedTest(name = "invalid: {0}")
    @ValueSource(strings = {
            "",
            "   ",
            "not-an-ip",
            "256.0.0.1",
            "1.2.3.4.5",
            "::gggg",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" // > 45 chars
    })
    @DisplayName("should reject invalid or malformed addresses")
    void shouldRejectInvalidAddresses(String candidate) {
        assertFalse(invokeIsValidIpLiteral(candidate), "Expected rejection of: " + candidate);
    }
}
