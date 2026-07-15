package org.jahia.community.whatismypublicserverid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the upstream endpoint contract (U2/D2): the URL is HTTPS and hardcoded, guarding against a
 * silent downgrade to http. Read reflectively so the production API surface stays unchanged.
 */
@DisplayName("PublicIp endpoint constant")
class PublicIpEndpointTest {

    private static String readCheckIpUrl() {
        try {
            final Field field = PublicIp.class.getDeclaredField("CHECK_IP_URL");
            field.setAccessible(true);
            return (String) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to read CHECK_IP_URL", e);
        }
    }

    @Test
    @DisplayName("S16: endpoint is HTTPS and hardcoded to checkip.amazonaws.com")
    void endpointIsHttpsAndHardcoded() {
        // Arrange
        final String url = readCheckIpUrl();

        // Assert
        assertTrue(url.startsWith("https://"), "endpoint must use HTTPS, not HTTP: " + url);
        assertEquals("https://checkip.amazonaws.com", url);
    }
}
