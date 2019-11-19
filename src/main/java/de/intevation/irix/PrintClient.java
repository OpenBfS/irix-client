/* Copyright (C) 2015 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE.txt for details.
 */

package de.intevation.irix;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

import java.net.HttpURLConnection;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;

import java.nio.charset.Charset;

/**
 * Utility class to handle interaction with the mapfish-print service.
 *
 */
public class PrintClient {
    private static Logger log = Logger.getLogger(PrintClient.class);

    private static final int CONNECTION_TIMEOUT = 5000;

    private static final int BYTE_ARRAY_SIZE = 4096;

    private PrintClient() {
        // hidden constructor to avoid instantiation.
    }

    /**
     * Obtains a Report from mapfish-print service.
     *
     * @param printUrl The url to send the request to.
     * @param json The json spec for the print request.
     *
     * @return byte[] with the report.
     *
     * @throws IOException if communication with print service failed.
     * @throws PrintException if the print job failed.
     */
    public static byte[] getReport(String printUrl, String json)
        throws IOException, PrintException {
        return getReport(printUrl, json, CONNECTION_TIMEOUT);
    }

    /**
     * Obtains printLayouts from mapfish-print service.
     *
     * @param printUrl The url to send the request to.
     *
     * @return byte[] with the report.
     *
     * @throws IOException if communication with print service failed.
     * @throws PrintException if the print job failed.
     */
    public static byte[] getLayouts(String printUrl)
            throws IOException, PrintException {
        return getLayouts(printUrl, CONNECTION_TIMEOUT);
    }

    /** Obtains a Report from mapfish-print service.
     *
     * @param printUrl The url to send the request to.
     * @param json The json spec for the print request.
     * @param timeout the timeout for the httpconnection.
     *
     * @return byte[] with the report.
     *
     * @throws IOException if communication with print service failed.
     * @throws PrintException if the print job failed.
     */
    public static byte[] getReport(String printUrl, String json, int timeout)
        throws IOException, PrintException {

        RequestConfig config = RequestConfig.custom().
            setConnectTimeout(timeout).build();

        CloseableHttpClient client = HttpClients.custom().
            setDefaultRequestConfig(config).build();

        HttpEntity entity = new StringEntity(json,
            ContentType.create("application/json", Charset.forName("UTF-8")));

        HttpPost post = new HttpPost(printUrl);
        post.setEntity(entity);
        CloseableHttpResponse resp = client.execute(post);

        StatusLine status = resp.getStatusLine();

        byte [] retval = null;
        try {
            HttpEntity respEnt = resp.getEntity();
            InputStream in = respEnt.getContent();
            if (in != null) {
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte [] buf = new byte[BYTE_ARRAY_SIZE];
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
                throw new PrintException(new String(retval));
            } else {
                throw new PrintException("Communication with print service '"
                                         + printUrl + "' failed."
                                         + "\nNo response from print service.");
            }
        }
        return retval;
    }

    /** Obtains printLayouts from mapfish-print service.
     *
     * @param printUrl The url to send the request to..
     * @param timeout the timeout for the httpconnection.
     *
     * @return byte[] with the report.
     *
     * @throws IOException if communication with print service failed.
     * @throws PrintException if the print job failed.
     */
    public static byte[] getLayouts(String printUrl, int timeout)
            throws IOException, PrintException {

        RequestConfig config = RequestConfig.custom().
                setConnectTimeout(timeout).build();

        CloseableHttpClient client = HttpClients.custom().
                setDefaultRequestConfig(config).build();

        HttpGet get = new HttpGet(printUrl);
        CloseableHttpResponse resp = client.execute(get);

        StatusLine status = resp.getStatusLine();

        byte [] retval = null;
        try {
            HttpEntity respEnt = resp.getEntity();
            InputStream in = respEnt.getContent();
            if (in != null) {
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte [] buf = new byte[BYTE_ARRAY_SIZE];
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
                throw new PrintException(new String(retval));
            } else {
                throw new PrintException("Communication with print service '"
                        + printUrl + "' failed."
                        + "\nNo response from print service.");
            }
        }
        return retval;
    }

}
