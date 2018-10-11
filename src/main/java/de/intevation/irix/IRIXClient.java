/* Copyright (C) 2015 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE.txt for details.
 */

package de.intevation.irix;

import java.io.IOException;
import java.io.File;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;
import java.util.Base64;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;

import org.iaea._2012.irix.format.ReportType;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONArray;
import org.json.JSONException;

import de.intevation.irixservice.UploadReportService;
import de.intevation.irixservice.UploadReportInterface;
import de.intevation.irixservice.UploadReportException_Exception;

import org.xml.sax.SAXException;

/**
 * Servlet implementation for the IRIX-Client.
 * <p>
 * Process client requests and handle resulting IRIX-reports.
 */
public class IRIXClient extends HttpServlet {
    private static Logger log = Logger.getLogger(IRIXClient.class);
    private static final int BUF_SIZE = 8192;

    /**
     * The name of the json array containing the print descriptions.
     */
    private static final String PRINT_JOB_LIST_KEY = "mapfish-print";
    private static final String IMAGE_JOB_LIST_KEY = "img-print";

    private static final String REQUEST_TYPE_UPLOAD = "upload";
    private static final String REQUEST_TYPE_RESPOND = "respond";
    private static final String REQUEST_TYPE_UPLOAD_RESPOND = "upload/respond";

    /**
     * Path to the irixSchema xsd file.
     */
    private static final String IRIX_SCHEMA_LOC =
            "/WEB-INF/irix-schema/IRIX.xsd";

    /**
     * Path to the Dokpool extension xsd file.
     */
    private static final String DOKPOOL_SCHEMA_LOC =
            "/WEB-INF/irix-schema/Dokpool-3.xsd";

    /**
     * The IRIX XSD-schema file.
     */
    protected File irixSchemaFile;
    /**
     * The Dokpool XSD-schema file.
     */
    protected File dokpoolSchemaFile;

    /**
     * Append to mapfish-print layout name to identify map layout.
     */
    protected String mapSuffix;
    /**
     * Append to mapfish-print layout name to identify legend layout.
     */
    protected String legendSuffix;
    /**
     * Base URL of mapfish-print service.
     */
    protected String baseUrl;
    /**
     * URL of irix-webservice upload service.
     */
    protected URL irixServiceUrl;

    /**
     * Get configuration parameters from web.xml and initialize servlet
     * context.
     *
     * @throws javax.servlet.ServletException if a configuration
     *                                        parameter is missing.
     */
    @Override
    public void init() throws ServletException {
        baseUrl = getInitParameter("print-url");

        if (baseUrl == null) {
            throw new ServletException("Missing 'print-url' parameter");
        }

        legendSuffix = getInitParameter("legend-layout-suffix");
        if (legendSuffix == null) {
            throw new ServletException(
                    "Missing 'legend-layout-suffix' parameter.");
        }

        mapSuffix = getInitParameter("map-layout-suffix");
        if (mapSuffix == null) {
            throw new ServletException(
                    "Missing 'map-layout-suffix' parameter.");
        }

        String irixServiceStr = getInitParameter("irix-webservice-url");
        if (irixServiceStr == null) {
            throw new ServletException(
                    "Missing 'irix-webservice-url' parameter.");
        }
        try {
            irixServiceUrl = new URL(irixServiceStr);
        } catch (MalformedURLException e) {
            throw new ServletException(
                    "Bad configuration value for: irix-webservice-url", e);
        }

        ServletContext sc = getServletContext();

        irixSchemaFile = new File(sc.getRealPath(IRIX_SCHEMA_LOC));
        dokpoolSchemaFile = new File(sc.getRealPath(DOKPOOL_SCHEMA_LOC));
    }

    /**
     * Parse the content of the request into a json object.
     *
     * @param request the request.
     * @return a {@link org.json.JSONObject} object with the content of the
     * request.
     */
    protected JSONObject parseRequest(HttpServletRequest request) {
        try {
            return new JSONObject(new JSONTokener(request.getReader()));
        } catch (IOException e) {
            log.warn("Request did not contain valid json: " + e.getMessage());
        }
        return null;
    }

    /**
     * Obtain all the print specs in the JSON request.
     *
     * @param jsonObject a {@link org.json.JSONObject} object with the content
     *                   of the request.
     * @return a {@link java.util.List} of JSON objects, each containing one
     * print spec.
     */
    protected List<JSONObject> getPrintSpecs(JSONObject jsonObject) {
        List<JSONObject> retval = new ArrayList<JSONObject>();
        try {
            String jobListKey = "";
            if (jsonObject.has(PRINT_JOB_LIST_KEY)) {
                jobListKey = PRINT_JOB_LIST_KEY;
            } else if (jsonObject.has(IMAGE_JOB_LIST_KEY)) {
                jobListKey = IMAGE_JOB_LIST_KEY;
            } else {
                log.warn("Request did not contain valid JOB_LIST_KEY: "
                        + PRINT_JOB_LIST_KEY + ", " + IMAGE_JOB_LIST_KEY);
            }
            JSONArray printList =
                    jsonObject.getJSONArray(jobListKey);
            for (int i = 0; i < printList.length(); i++) {
                JSONObject jobDesc = printList.getJSONObject(i);
                jobDesc.put("jobKey", jobListKey);
                retval.add(jobDesc);
            }
        } catch (JSONException e) {
            log.warn("Request did not contain valid json: " + e.getMessage());
        }
        return retval;
    }

    /**
     * Wrapper to forward a print error response.
     *
     * @param err      the PrintException.
     * @param response the javax.servlet.http.HttpServletResponse
     *                 to be used for returning the error.
     * @throws IOException if such occured on the output stream.
     */
    public void writePrintError(PrintException err,
                                HttpServletResponse response)
            throws IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getOutputStream().print(err.getMessage());
        response.getOutputStream().flush();
        return;
    }

    /**
     * Wrapper to forward a image error response.
     *
     * @param err      the ImageException.
     * @param response the javax.servlet.http.HttpServletResponse
     *                 to be used for returning the error.
     * @throws IOException if such occured on the output stream.
     */
    public void writeImageError(ImageException err,
                                HttpServletResponse response)
            throws IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getOutputStream().print(err.getMessage());
        response.getOutputStream().flush();
        return;
    }

    /**
     * Helper method to print / attach the requested documents.
     *
     * @param specs    A list of the json print specs.
     * @param report   The report to attach the data to.
     * @param printApp The printApp to use
     * @param title    The title for the Annex
     * @throws IOException    if a requested document could not be printed
     *                        because of Connection problems.
     * @throws PrintException it the print service returned an error.
     */
    protected void handlePrintSpecs(
            List<JSONObject> specs,
            ReportType report,
            String printApp,
            String title
    ) throws IOException, PrintException {
        String printUrl = baseUrl + "/" + printApp + "/buildreport";
        try {
            URL url = new URL(printUrl);
            URI uri = new URI(
                    url.getProtocol(), url.getUserInfo(),
                    url.getHost(), url.getPort(), url.getPath(),
                    url.getQuery(), url.getRef()
            );
            printUrl = uri.toString();
        } catch (URISyntaxException e) {
            throw new PrintException("URL encoding failed.");
        }
        int i = 1;
        String suffix = "";
        for (JSONObject spec : specs) {
            if (specs.size() > 1) {
                suffix = " " + Integer.toString(i++);
            }

            byte[] content = PrintClient.getReport(printUrl + ".pdf",
                    spec.toString());
            ReportUtils.attachFile(title + suffix, content, report,
                    "application/pdf", title + suffix + ".pdf");

            String baseLayout = spec.getString("layout");

            // map without legend
            spec.put("layout", baseLayout + mapSuffix);
            content = PrintClient.getReport(printUrl + ".png",
                    spec.toString());
            ReportUtils.attachFile(title + mapSuffix + suffix, content, report,
                    "image/png", title + mapSuffix + suffix + ".png");

            // legend without map
            spec.put("layout", baseLayout + legendSuffix);
            content = PrintClient.getReport(printUrl + ".png",
                    spec.toString());
            ReportUtils.attachFile(title + legendSuffix
                    + suffix, content, report, "image/png", title
                    + legendSuffix + suffix + ".png");
        }
    }

    /**
     * Helper method to print / attach the requested documents.
     *
     * @param specs  A list of the json print specs.
     * @param report The report to attach the data to.
     * @param title  The title for the Annex
     * @throws IOException    if a requested document could not be printed
     *                        because of Connection problems.
     * @throws ImageException it the print service returned an error.
     */
    protected void handleImageSpecs(List<JSONObject> specs,
                                    ReportType report, String title)
            throws IOException, ImageException {
        int i = 1;
        String suffix = "";
        for (JSONObject spec : specs) {
            if (specs.size() > 1) {
                suffix = " " + Integer.toString(i++);
            }

            String outputFormat = spec.getString("outputFormat");
            String mimeType = spec.getString("mimetype");
            if (spec.has("outputSuffix")) {
                suffix += spec.getString("outputSuffix");
            }
            byte[] content = null;
            if (spec.has("value")
                    && spec.get("value").toString().length() > 0) {
                // content is embedded as base64 string (incl. data:... part)
                String base64value = spec.getString("value");
                // removing everything before including an eventually
                // existing comma to separate base64 content
                String base64content = base64value.
                        split(",")[base64value.split(",").length - 1];
                content = Base64.getDecoder().decode(base64content);
                //content = decoder.decode(base64content);
            } else if (spec.has("url")) {
                // content has to be fetched from external URL
                String imageUrl = spec.get("url").toString();
                try {
                    URL url = new URL(imageUrl);
                    URI uri = new URI(
                            url.getProtocol(), url.getUserInfo(),
                            url.getHost(), url.getPort(), url.getPath(),
                            url.getQuery(), url.getRef()
                    );
                    imageUrl = uri.toString();
                } catch (URISyntaxException e) {
                    throw new ImageException("URL encoding failed.");
                }
                content = ImageClient.getImage(imageUrl);
            }
            if (content.getClass().equals(byte[].class)) {
                ReportUtils.attachFile(title + suffix, content, report,
                        mimeType, title + suffix + "." + outputFormat);
            }
        }
    }

    /**
     * Sends a report to the configured UploadReport service.
     *
     * @param report The report to send.
     * @throws javax.servlet.ServletException if uploading failed.
     */
    protected void sendReportToService(ReportType report)
            throws ServletException {
        UploadReportService service = new UploadReportService(irixServiceUrl);
        UploadReportInterface irixservice = service.getUploadReportPort();
        log.debug("Sending report.");
        try {
            irixservice.uploadReport(report);
        } catch (UploadReportException_Exception e) {
            throw new ServletException(
                    "Failed to send report to IRIX service.", e);
        }
        log.debug("Report successfully sent.");
    }

    /**
     * Handle POST request.
     * <p>
     * Parse request and generate according IRIX report for response.
     *
     * @param request  object that contains the request the client has made
     *                 of the servlet
     * @param response object that contains the response the servlet sends
     *                 to the client
     * @throws ServletException in case of errors with schema.
     * @throws IOException      if the request is invalid.
     */
    @Override
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
            throws ServletException, IOException {

        JSONObject jsonObject = parseRequest(request);
        if (jsonObject == null) {
            throw new ServletException(
                    "Could not read jsonObject from request.");
        }

        List<JSONObject> printSpecs = getPrintSpecs(jsonObject);
        if (printSpecs.isEmpty()) {
            throw new ServletException(
                    "Could not extract any print specs from request.");
        }

        String requestType = null;
        try {
            requestType = jsonObject.getString("request-type");
        } catch (JSONException e) {
            throw new ServletException("Failed to parse request-type: ", e);
        }
        requestType = requestType.toLowerCase();

        if (!requestType.equals(REQUEST_TYPE_UPLOAD)
                && !requestType.equals(REQUEST_TYPE_RESPOND)
                && !requestType.equals(REQUEST_TYPE_UPLOAD_RESPOND)) {
            throw new ServletException("Unknown request-type: " + requestType);
        }

        ReportType report = null;
        try {
            report = ReportUtils.prepareReport(jsonObject);
            ReportUtils.addAnnotation(jsonObject, report, dokpoolSchemaFile);
            if (printSpecs.get(0).has("jobKey")
                    && printSpecs.get(0).get("jobKey")
                    .hashCode() == IMAGE_JOB_LIST_KEY.hashCode()) {
                handleImageSpecs(printSpecs, report,
                        jsonObject.getJSONObject("irix").getString("Title"));
            } else {
                handlePrintSpecs(printSpecs, report,
                        jsonObject.getString("printapp"),
                        jsonObject.getJSONObject("irix").getString("Title"));
            }
        } catch (JSONException e) {
            throw new ServletException("Failed to parse IRIX information: ", e);
        } catch (SAXException e) {
            throw new ServletException("Failed to parse schema.", e);
        } catch (JAXBException e) {
            throw new ServletException("Invalid request.", e);
        } catch (ImageException e) {
            writeImageError(e, response);
            return;
        } catch (PrintException e) {
            writePrintError(e, response);
            return;
        }

        if (requestType.equals(REQUEST_TYPE_UPLOAD)) {
            try {
                sendReportToService(report);
                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_OK);
            } catch (ServletException se) {
                response.setContentType("text/plain");
                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                );
                response.getWriter().write(se.toString());
                response.getWriter().flush();
                response.getWriter().close();
                log.error(se);
            }
        }

        if (requestType.equals(REQUEST_TYPE_UPLOAD_RESPOND)) {
            try {
                sendReportToService(report);
            } catch (ServletException se) {
                response.setContentType("text/plain");
                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                );
                response.getWriter().write(se.toString());
                response.getWriter().flush();
                response.getWriter().close();
                log.error(se);
            }
        }

        if (requestType.equals(REQUEST_TYPE_RESPOND)
                || requestType.equals(REQUEST_TYPE_UPLOAD_RESPOND)) {
            try {
                ReportUtils.marshallReport(report, response.getOutputStream(),
                        irixSchemaFile);
            } catch (JAXBException e) {
                throw new ServletException("Invalid request.", e);
            } catch (SAXException e) {
                throw new ServletException("Failed to parse schema.", e);
            }
            response.setContentType("application/xml");
            response.getOutputStream().flush();
        }
    }


}
