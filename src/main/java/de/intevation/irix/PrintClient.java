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

import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.HttpClient;

/** Utility class to handle interaction with the mapfish-print service. */

public class PrintClient
{
    private static Logger log = Logger.getLogger(PrintClient.class);
    /** Obtains a Report from mapfish-print service.
     *
     * @param print-url The url to send the request to.
     * @param json The json spec for the print request.
     *
     * @return: byte[] with the report. null on error.
     */
    public static byte[] getReport(String printUrl, String json)
        throws IOException {
        HttpClient client = new HttpClient(
                new MultiThreadedHttpConnectionManager());
        PostMethod post = new PostMethod(printUrl);
        post.setRequestBody(json);
        post.addRequestHeader("Content-Type", "application/json");
        int result = client.executeMethod(post);
        InputStream in = post.getResponseBodyAsStream();
        byte [] retval = null;
        if (in != null) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte [] buf = new byte[4096];
                int r;
                while ((r = in.read(buf)) >= 0) {
                    out.write(buf, 0, r);
                }
                retval = out.toByteArray();
            } finally {
                in.close();
            }
        }
        if (result < 200 || result >= 300) {
            log.debug("Error during print.");
            return null;
        }
        return retval;
    }
}
