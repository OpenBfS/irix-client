/* Copyright (C) 2015 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE.txt for details.
 */

package de.intevation.irix;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
//import java.nio.charset.Charset;

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

        RequestConfig config = RequestConfig.custom().
            setConnectTimeout(timeout).build();

        CloseableHttpClient client = HttpClients.custom().
            setDefaultRequestConfig(config).build();

        HttpGet get = new HttpGet(imageUrl);
        CloseableHttpResponse resp = client.execute(get);

        StatusLine status = resp.getStatusLine();

        byte[] retval = null;
        try {
            HttpEntity respEnt = resp.getEntity();
            InputStream in = respEnt.getContent();
            if (in != null) {
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[BYTE_ARRAY_SIZE];
                    int r;
                    while ((r = in.read(buf)) >= 0) {
                        out.write(buf, 0, r);
                    }
                    retval = out.toByteArray();
                } finally {
                    in.close();
                    EntityUtils.consume(respEnt);
                }
            }
        } finally {
            resp.close();
        }

        if (status.getStatusCode() < HttpURLConnection.HTTP_OK
            || status.getStatusCode() >= HttpURLConnection.HTTP_MULT_CHOICE) {
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
