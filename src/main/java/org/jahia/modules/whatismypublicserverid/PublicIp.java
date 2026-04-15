package org.jahia.modules.whatismypublicserverid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
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
    private static final String CHECK_IP_URL = "http://checkip.amazonaws.com";

    public String get() {
        try (InputStream inputStream = URI.create(CHECK_IP_URL).toURL().openStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            return bufferedReader.readLine();
        } catch (IOException ex) {
            LOGGER.error("Impossible to get IP from URL {}", CHECK_IP_URL, ex);
            return "unknown";
        }
    }
}
