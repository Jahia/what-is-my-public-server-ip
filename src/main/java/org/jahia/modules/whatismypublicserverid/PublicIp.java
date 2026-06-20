package org.jahia.modules.whatismypublicserverid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author fbourasse
 */
public final class PublicIp implements Serializable {

    private static final long serialVersionUID = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicIp.class);
    private static final String CHECK_IP_URL = "https://checkip.amazonaws.com";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    private static final int MAX_RESPONSE_LENGTH = 64;
    private static final String UNKNOWN = "unknown";
    private static final long CACHE_TTL_MS = 60_000L;

    /** Cached IP value; {@code null} means the cache is empty. */
    private static volatile String cachedIp = null;
    /** Timestamp (ms since epoch) at which {@link #cachedIp} was stored. */
    private static volatile long cacheTimestamp = 0L;

    public String get() {
        final long now = System.currentTimeMillis();
        final String cached = cachedIp;
        if (cached != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            return cached;
        }
        final String fresh = fetchFromUpstream();
        storeInCache(fresh, now);
        return fresh;
    }

    /** Updates the process-wide cache. Static so the shared-state mutation is explicit (Sonar S2696). */
    private static void storeInCache(String ip, long timestamp) {
        cachedIp = ip;
        cacheTimestamp = timestamp;
    }

    private String fetchFromUpstream() {
        HttpURLConnection connection = null;
        try {
            final URL url = URI.create(CHECK_IP_URL).toURL();
            final String protocol = url.getProtocol();
            if (!"https".equalsIgnoreCase(protocol) && !"http".equalsIgnoreCase(protocol)) {
                LOGGER.error("Refusing to fetch public IP from non-http(s) URL");
                return UNKNOWN;
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            try (InputStream inputStream = connection.getInputStream();
                 InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                final char[] buffer = new char[MAX_RESPONSE_LENGTH];
                final int read = bufferedReader.read(buffer, 0, MAX_RESPONSE_LENGTH);
                if (read <= 0) {
                    return UNKNOWN;
                }
                final String candidate = new String(buffer, 0, read).trim();
                if (!isValidIpLiteral(candidate)) {
                    LOGGER.warn("Upstream IP service returned an unexpected payload");
                    return UNKNOWN;
                }
                return candidate;
            }
        } catch (IOException ex) {
            LOGGER.error("Impossible to get IP from URL {}", CHECK_IP_URL, ex);
            return UNKNOWN;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static boolean isValidIpLiteral(String candidate) {
        if (candidate == null || candidate.isEmpty() || candidate.length() > 45) {
            return false;
        }
        for (int i = 0; i < candidate.length(); i++) {
            final char c = candidate.charAt(i);
            final boolean allowed = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F')
                    || c == '.' || c == ':';
            if (!allowed) {
                return false;
            }
        }
        try {
            InetAddress.getByName(candidate);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
