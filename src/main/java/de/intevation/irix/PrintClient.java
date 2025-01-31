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
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpResponse.BodyHandlers;

import org.json.JSONObject;

import java.time.Duration;

/**
 * Utility class to handle interaction with the mapfish-print service.
 *
 */
public class PrintClient {
    private static System.Logger log = System.getLogger(PrintClient.class.getName());

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
    public static JSONObject getLayouts(String printUrl)
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

        HttpClient client = java.net.http.HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .header("Content-Type", "application/json; charset=UTF-8")
            .POST(BodyPublishers.ofString(json))
            .uri(URI.create(printUrl))
            .timeout(Duration.ofMillis(timeout))
            .build();

        int statusCode = 0;
        byte[] retval = null;
        try {
            HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());
            statusCode = response.statusCode();
            retval = response.body();
        } catch (InterruptedException e) {
            throw new PrintException("Communication with print service '"
                                    + printUrl + "' was interrupted.");
        }

        if (statusCode < HttpURLConnection.HTTP_OK
            || statusCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
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
     * @return JSONObject with the layouts.
     *
     * @throws IOException if communication with print service failed.
     * @throws PrintException if the print job failed.
     */
    public static JSONObject getLayouts(String printUrl, int timeout)
            throws IOException, PrintException {

        HttpClient client = java.net.http.HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .GET()//for clarity, actually GET is the default
            .uri(URI.create(printUrl))
            .timeout(Duration.ofMillis(timeout))
            .build();

        int statusCode = 0;
        JSONObject retval = null;
        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            statusCode = response.statusCode();
            retval = new JSONObject(response.body());
        } catch (InterruptedException e) {
            throw new PrintException("Communication with print service '"
                                     + printUrl + "' was interrupted.");
        } catch (IOException e) {
            throw new PrintException("Response from print service could not be parsed as valid JSON: "
                    + e);
        }

        if (statusCode < HttpURLConnection.HTTP_OK
            || statusCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
            if (retval != null) {
                throw new PrintException(new String(retval.toString()));
            } else {
                throw new PrintException("Communication with print service '"
                        + printUrl + "' failed."
                        + "\nNo response from print service.");
            }
        }
        return retval;
    }
}
