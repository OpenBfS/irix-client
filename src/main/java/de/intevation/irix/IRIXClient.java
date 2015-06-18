/* Copyright (C) 2015 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE.txt for details.
 */

package de.intevation.irix;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.ArrayList;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;

import org.iaea._2012.irix.format.ReportType;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import de.intevation.irixservice.UploadReportService;
import de.intevation.irixservice.UploadReportInterface;
import de.intevation.irixservice.UploadReportException_Exception;

import org.xml.sax.SAXException;

/**
 * Servlet implementation for the IRIX-Client.
 *
 * Process client requests and handle resulting IRIX-reports.
 *
 */
public class IRIXClient extends HttpServlet {
    private static Logger log = Logger.getLogger(IRIXClient.class);

    /** The name of the json array containing the print descriptions. */
    private static final String PRINT_JOB_LIST_KEY = "mapfish-print";

    private static final String REQUEST_TYPE_UPLOAD = "upload";
    private static final String REQUEST_TYPE_RESPOND = "respond";
    private static final String REQUEST_TYPE_UPLOAD_RESPOND = "upload/respond";

    /** Path to the irixSchema xsd file. */
    private static final String IRIX_SCHEMA_LOC =
        "/WEB-INF/irix-schema/IRIX.xsd";

    /** Path to the Dokpool extension xsd file. */
    private static final String DOKPOOL_SCHEMA_LOC =
        "/WEB-INF/irix-schema/Dokpool-3.xsd";

    /** The IRIX XSD-schema file. */
    protected File irixSchemaFile;
    /** The Dokpool XSD-schema file. */
    protected File dokpoolSchemaFile;

    /** Append to mapfish-print layout name to identify map layout. */
    protected String mapSuffix;
    /** Append to mapfish-print layout name to identify legend layout. */
    protected String legendSuffix;
    /** Base URL of mapfish-print service. */
    protected String baseUrl;

    /**
     * Get configuration parameters from web.xml and initialize servlet
     * context.
     *
     * @throws javax.servlet.ServletException if a configuration
     * parameter is missing.
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
        StringBuffer buffer = new StringBuffer();
        String line = null;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            log.warn("Failed to read response: " + e.getMessage());
            return null;
        }

        String json = buffer.toString();
        JSONObject retval = null;
        try {
            retval = new JSONObject(json);
        } catch (JSONException e) {
            log.warn("Request did not contain valid json: " + e.getMessage());
            return null;
        }
        return retval;
    }

    /**
     * Obtain all the print specs in the JSON request.
     *
     * @param jsonObject a {@link org.json.JSONObject} object with the content
     * of the request.
     * @return a {@link java.util.List} of JSON objects, each containing one
     * print spec.
     */
    protected List<JSONObject> getPrintSpecs(JSONObject jsonObject) {
        List <JSONObject> retval = new ArrayList<JSONObject>();
        try {
            JSONArray mapfishPrintList =
                jsonObject.getJSONArray(PRINT_JOB_LIST_KEY);
            for (int i = 0; i < mapfishPrintList.length(); i++) {
                JSONObject jobDesc = mapfishPrintList.getJSONObject(i);
                retval.add(jobDesc);
            }
        } catch (JSONException e) {
            log.warn("Request did not contain valid json: " + e.getMessage());
        }
        return retval;
    }

    /**
     * Wrapper to write an error message as response.
     *
     * @param msg the error message.
     * @param response the javax.servlet.http.HttpServletResponse
     * to be used for returning the error.
     *
     * @throws IOException if such occured on the output stream.
     */
    public void writeError(String msg, HttpServletResponse response)
        throws IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getOutputStream().print(msg);
        log.debug("Sending error: " + msg);
        response.getOutputStream().flush();
        return;
    }

    /**
     * Helper method to print / attach the requested documents.
     *
     * @param specs A list of the json print specs.
     * @param report The report to attach the data to.
     * @param printApp The printApp to use
     * @param title The title for the Annex
     *
     * @throws IOException if a requested document could not be printed.
     */
    protected void handlePrintSpecs(List<JSONObject> specs,
        ReportType report, String printApp, String title)
        throws IOException {
        String printUrl = baseUrl + "/" + printApp + "/buildreport";
        int i = 1;
        String suffix = "";
        for (JSONObject spec: specs) {
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
            ReportUtils.attachFile(title + " Map" + suffix, content, report,
                "image/png", title + " Map" + suffix + ".png");

            // legend without map
            spec.put("layout", baseLayout + legendSuffix);
            content = PrintClient.getReport(printUrl + ".png",
                spec.toString());
            ReportUtils.attachFile(title + " Legend" + suffix, content, report,
                "image/png", title + " Legend" + suffix + ".png");
        }
    }

    /**
     * Sends a report to the UploadReport service configured during buildtime.
     *
     * @param report The report to send.
     * @throws javax.servlet.ServletException if uploading failed.
     */
    protected void sendReportToService(ReportType report)
        throws ServletException {
        UploadReportService service = new UploadReportService();
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
     *
     * Parse request and generate according IRIX report for response.
     *
     * @param request object that contains the request the client has made
     * of the servlet
     * @param response object that contains the response the servlet sends
     * to the client
     *
     * @throws ServletException in case of errors with schema.
     * @throws IOException if the request is invalid.
     *
     */
    @Override
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
        throws ServletException, IOException {

        JSONObject jsonObject = parseRequest(request);
        if (jsonObject == null) {
            writeError("Could not read jsonObject from request", response);
            return;
        }

        List<JSONObject> printSpecs = getPrintSpecs(jsonObject);
        if (printSpecs.isEmpty()) {
            writeError("Could not extract any print specs from request",
                response);
            return;
        }

        String requestType = null;
        try {
            requestType = jsonObject.getString("request-type");
        } catch (JSONException e) {
            writeError("Failed to parse request-type: " + e.getMessage(),
                response);
            return;
        }
        requestType = requestType.toLowerCase();

        if (!requestType.equals(REQUEST_TYPE_UPLOAD)
            && !requestType.equals(REQUEST_TYPE_RESPOND)
            && !requestType.equals(REQUEST_TYPE_UPLOAD_RESPOND)) {
            writeError("Unknown request-type: " + requestType, response);
            return;
        }

        ReportType report = null;
        try {
            report = ReportUtils.prepareReport(jsonObject);
            ReportUtils.addAnnotation(jsonObject, report, dokpoolSchemaFile);
            handlePrintSpecs(printSpecs, report,
                jsonObject.getString("printapp"),
                jsonObject.getJSONObject("irix").getString("Title"));
        } catch (JSONException e) {
            writeError("Failed to parse IRIX information: " + e.getMessage(),
                response);
            return;
        } catch (SAXException e) {
            throw new ServletException("Failed to parse schema.", e);
        } catch (JAXBException e) {
            throw new ServletException("Invalid request.", e);
        }

        if (requestType.equals(REQUEST_TYPE_UPLOAD)
            || requestType.equals(REQUEST_TYPE_UPLOAD_RESPOND)) {
            sendReportToService(report);
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

    /** {@inheritDoc} */
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws ServletException, IOException  {
    }
}
