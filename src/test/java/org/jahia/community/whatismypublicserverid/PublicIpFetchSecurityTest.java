package org.jahia.community.whatismypublicserverid;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Security regression guards on the real {@link PublicIp#fetchFromUpstream()} path. These pin the
 * SSRF / echo / validation hardening (currently correct) so a future edit cannot silently remove it:
 *
 * <ul>
 *   <li>S12 — oversized body is capped at 64 chars and never echoed.</li>
 *   <li>S13 — non-IP / injection body is rejected and never echoed.</li>
 *   <li>S14 — 3xx redirects are NOT followed (attacker server gets zero requests).</li>
 *   <li>S15 — non-http(s) protocol is refused before any connection is opened.</li>
 * </ul>
 *
 * Uses the JDK-builtin loopback {@code HttpServer} (no new dependency); http loopback satisfies
 * the production protocol allow-list, so no HTTPS/cert setup is required.
 */
@DisplayName("PublicIp fetch-path security")
class PublicIpFetchSecurityTest {

    private final List<HttpServer> servers = new ArrayList<>();

    @BeforeEach
    void resetCache() {
        PublicIp.resetCacheForTesting();
    }

    @AfterEach
    void stopServers() {
        for (final HttpServer server : servers) {
            server.stop(0);
        }
        servers.clear();
    }

    private String startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.start();
        servers.add(server);
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    private static PublicIp subjectFor(String url) {
        return new PublicIp() {
            @Override
            String checkIpUrl() {
                return url;
            }
        };
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
            throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
        exchange.close();
    }

    @Test
    @DisplayName("S12: oversized body is capped and not echoed → unknown")
    void oversizedBodyIsCappedAndNotEchoed() throws IOException {
        // Arrange — 5000 'A's; truncated-to-64 is still not a valid IP literal
        final String payload = "A".repeat(5000);
        final String url = startServer(ex -> respond(ex, 200, payload));
        final PublicIp subject = subjectFor(url);

        // Act
        final String result = subject.get();

        // Assert — no reflected content whatsoever
        assertEquals("unknown", result);
        assertFalse(result.contains("A"), "payload must not be echoed back");
    }

    @ParameterizedTest(name = "malicious body: {0}")
    @ValueSource(strings = {
            "<script>alert(1)</script>",
            "1.2.3.4; rm -rf /",
            "not-an-ip"
    })
    @DisplayName("S13: non-IP / injection body is rejected and not echoed → unknown")
    void maliciousBodyIsRejectedAndNotEchoed(String malicious) throws IOException {
        // Arrange
        final String url = startServer(ex -> respond(ex, 200, malicious));
        final PublicIp subject = subjectFor(url);

        // Act
        final String result = subject.get();

        // Assert — exactly "unknown", none of the payload's dangerous chars leak
        assertEquals("unknown", result);
        assertFalse(result.contains("<"), "no markup may leak");
        assertFalse(result.contains(";"), "no shell metacharacters may leak");
        assertFalse(result.contains("rm"), "no command fragment may leak");
    }

    @Test
    @DisplayName("S14: a 302 redirect is NOT followed (attacker server gets zero requests) → unknown")
    void redirectIsNotFollowed() throws IOException {
        // Arrange — attacker server would serve a plausible-looking IP if reached
        final AtomicInteger attackerHits = new AtomicInteger(0);
        final String attackerUrl = startServer(ex -> {
            attackerHits.incrementAndGet();
            respond(ex, 200, "8.8.8.8\n");
        });
        final String primaryUrl = startServer(ex -> {
            ex.getResponseHeaders().add("Location", attackerUrl);
            respond(ex, 302, "");
        });
        final PublicIp subject = subjectFor(primaryUrl);

        // Act
        final String result = subject.get();

        // Assert — did not pivot to the redirect target
        assertEquals("unknown", result);
        assertEquals(0, attackerHits.get(), "redirect target must never be contacted");
    }

    @ParameterizedTest(name = "refused protocol: {0}")
    @ValueSource(strings = {
            "ftp://127.0.0.1/",
            "file:///etc/passwd"
    })
    @DisplayName("S15: non-http(s) protocol is refused by the allow-list → unknown")
    void nonHttpProtocolIsRefused(String url) {
        // Arrange
        final PublicIp subject = subjectFor(url);

        // Act
        final String result = subject.get();

        // Assert — guard returns before openConnection
        assertEquals("unknown", result);
    }
}
