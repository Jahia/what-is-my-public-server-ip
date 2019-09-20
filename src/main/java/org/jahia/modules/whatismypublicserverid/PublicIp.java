package org.jahia.modules.whatismypublicserverid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
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
        String ip = "unknown";
        try {
            final URL whatismyip = new URL(CHECK_IP_URL);
            try ( InputStream inputStream = whatismyip.openStream()) {
                try ( InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    try ( BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                        ip = bufferedReader.readLine();
                    }
                }
            } catch (IOException ex) {
                LOGGER.error("Impossible to open URL {}", CHECK_IP_URL, ex);
            }
        } catch (MalformedURLException ex) {
            LOGGER.error("Malformed URL {}", CHECK_IP_URL, ex);
        }
        return ip;
    }
}
