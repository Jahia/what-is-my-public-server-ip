package org.jahia.community.whatismypublicserverid;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Exercises the real {@link PublicIp#fetchFromUpstream()} wiring against a loopback
 * {@code com.sun.net.httpserver.HttpServer} (JDK built-in — no new dependency). The
 * {@code checkIpUrl()} seam points the connection at {@code http://127.0.0.1:<port>},
 * which the production protocol allow-list permits, so no HTTPS/cert setup is needed.
 *
 * <p>Covers the graceful {@code "unknown"} fallback (F3) and the happy path (F1/S17).
 */
@DisplayName("PublicIp upstream fetch")
class PublicIpFetchTest {

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

    /** Starts a loopback server with the given handler and returns its base URL. */
    private String startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.start();
        servers.add(server);
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    /** A {@link PublicIp} whose upstream URL is fixed to a loopback server. */
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
    @DisplayName("S17: a valid upstream IP is returned trimmed")
    void validUpstreamIpIsReturnedTrimmed() throws IOException {
        // Arrange
        final String url = startServer(ex -> respond(ex, 200, "203.0.113.42\n"));
        final PublicIp subject = subjectFor(url);

        // Act
        final String result = subject.get();

        // Assert
        assertEquals("203.0.113.42", result);
    }

    @Test
    @DisplayName("S8: connection refused (no listener) → unknown, never throws")
    void connectionRefusedYieldsUnknown() {
        // Arrange — port 1 on loopback has no listener; the connection is refused
        final PublicIp subject = subjectFor("http://127.0.0.1:1/");

        // Act
        final String result = subject.get();

        // Assert
        assertEquals("unknown", result);
    }

    @Test
    @DisplayName("S9: non-200 upstream status → unknown")
    void non200StatusYieldsUnknown() throws IOException {
        // Arrange
        final String url = startServer(ex -> respond(ex, 500, ""));
        final PublicIp subject = subjectFor(url);

        // Act
        final String result = subject.get();

        // Assert
        assertEquals("unknown", result);
    }

    @Test
    @DisplayName("S10: empty upstream body → unknown")
    void emptyBodyYieldsUnknown() throws IOException {
        // Arrange
        final String url = startServer(ex -> respond(ex, 200, ""));
        final PublicIp subject = subjectFor(url);

        // Act
        final String result = subject.get();

        // Assert
        assertEquals("unknown", result);
    }

    @Test
    @DisplayName("S11: a hung endpoint is bounded by the read timeout → unknown (does not hang)")
    void hungEndpointBoundedByReadTimeout() throws IOException {
        // Arrange — handler sleeps well past the (test-shortened) read timeout before replying
        final String url = startServer(ex -> {
            try {
                Thread.sleep(3_000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            respond(ex, 200, "203.0.113.99\n");
        });
        final PublicIp subject = new PublicIp() {
            @Override
            String checkIpUrl() {
                return url;
            }

            @Override
            int readTimeoutMs() {
                return 300; // sub-second so the read timeout fires long before the handler replies
            }
        };

        // Act + Assert — outer guard proves the call returns (does not hang indefinitely)
        assertTimeoutPreemptively(Duration.ofSeconds(8), () ->
                assertEquals("unknown", subject.get()));
    }
}
