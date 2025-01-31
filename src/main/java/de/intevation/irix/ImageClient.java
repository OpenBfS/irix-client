/* Copyright (C) 2015-2025 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE for details.
 */

package de.intevation.irix;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

/**
 * Utility class to handle interaction for external images.
 *
 */
public class ImageClient {
    private static System.Logger log = System.getLogger(ImageClient.class.getName());

    private static final int CONNECTION_TIMEOUT = 5000;

    private static final int BYTE_ARRAY_SIZE = 4096;

    private ImageClient() {
        // hidden constructor to avoid instantiation.
    }

    /**
     * Obtains a Report from mapfish-print service.
     *
     * @param imageUrl The url to send the request to.
     *
     * @return byte[] with the report.
     *
     * @throws IOException if communication with print service failed.
     * @throws ImageException if the print job failed.
     */
    public static byte[] getImage(String imageUrl)
        throws IOException, ImageException {
        return getImage(imageUrl, CONNECTION_TIMEOUT);
    }

    /** Obtains a Report from mapfish-print service.
     *
     * @param imageUrl The url to send the request to.
     * @param timeout the timeout for the httpconnection.
     *
     * @return byte[] with the report.
     *
     * @throws IOException if communication with print service failed.
     * @throws ImageException if the print job failed.
     */
    public static byte[] getImage(String imageUrl, int timeout)
        throws IOException, ImageException {

        HttpClient client = java.net.http.HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .GET()//for clarity, actually GET is the default
            .uri(URI.create(imageUrl))
            .timeout(Duration.ofMillis(timeout))
            .build();

        int statusCode = 0;
        byte[] retval = null;
        try {
            HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());
            statusCode = response.statusCode();
            retval = response.body();
        } catch (InterruptedException e) {
            throw new ImageException("Communication with print service '"
                                     + imageUrl + "' was interrupted.");
        }

        if (statusCode < HttpURLConnection.HTTP_OK
            || statusCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
            if (retval != null) {
                throw new ImageException(new String(retval));
            } else {
                throw new ImageException("Communication with print service '"
                                         + imageUrl + "' failed."
                                         + "\nNo response from print service.");
            }
        }
        return retval;
    }
}
