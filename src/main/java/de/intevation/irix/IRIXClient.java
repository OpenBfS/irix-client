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
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;
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
    private static final String DOC_JOB_LIST_KEY = "doc-print";
    private static final String EVENT_JOB_LIST_KEY = "event";

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
     * String of Header username - e.g. set by Shibboleth
     */
    protected String userHeaderString;
    /**
     * String of Header roles - e.g. set by Shibboleth
     */
    protected String rolesHeaderString;
    /**
     * List of permitted roles.
     */
    protected List<String> rolesPermission;
    /**
     * forward Headers (incl. auth Headers).
     */
    protected Boolean keepRequestHeaders;
    /**
     * store request Header fo reuse.
     */
    protected HttpServletRequest keptRequest;


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

        userHeaderString = getInitParameter("user-header");
        if (userHeaderString == null) {
            log.debug("No user-header set.");
        }

        rolesHeaderString = getInitParameter("roles-header");
        if (rolesHeaderString == null) {
            log.debug("No roles-header set.");
        }

        String rolesPermissionParam = getInitParameter("roles-permission");
        if (rolesPermissionParam != null) {
            rolesPermission = Arrays.asList(rolesPermissionParam);
            if (rolesPermission.isEmpty()) {
                log.debug("No roles-permission set.");
                // parseHeaders(request) will check for valid role in Header
            }
        } else {
            rolesPermission = null;
            log.debug("No init-param roles-permission found.");
        }

        ServletContext sc = getServletContext();

        irixSchemaFile = new File(sc.getRealPath(IRIX_SCHEMA_LOC));
        dokpoolSchemaFile = new File(sc.getRealPath(DOKPOOL_SCHEMA_LOC));

        keepRequestHeaders = Boolean.parseBoolean(
                getInitParameter("keep-request-headers")
        );
        //FIXME this if should never be true according to getInitParameter
        if (keepRequestHeaders == null) {
            throw new ServletException(
                    "Missing 'keep-request-headers' parameter.");
        }
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
     * Parse the header of the request into a json object.
     *
     * @param request the request.
     * @return a {@link org.json.JSONObject} object with parts of
     * the header of the request.
     */
    protected JSONObject parseHeader(HttpServletRequest request) {
        //FIXME make sure that this doesn't crash if properties are not set
        if (keepRequestHeaders) {
            this.keptRequest = request;
        }

        JSONObject uidHeaders = new JSONObject();
        if (userHeaderString != null) {
            String uid = request.getHeader(userHeaderString);
            uidHeaders.put("uid", uid);
            log.debug("Found User " + uidHeaders.get("uid"));
        }
        if (rolesHeaderString != null) {
            List<String> roles = Arrays.asList(request
                    .getHeader(rolesHeaderString).split("[\\s,]+"));
            if (rolesPermission != null) {
                List<String> validRolesList = rolesPermission.stream()
                        .filter(roles::contains)
                        .collect(Collectors.toList());
                if (validRolesList.isEmpty()) {
                    log.debug("No valid roles found for user "
                            + uidHeaders.get("uid"));
                    return null;
                } else {
                    //FIXME should we use the whole roles list instead?
                    uidHeaders.put("roles", validRolesList);
                }
            }
            return uidHeaders;
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
            } else if (jsonObject.has(DOC_JOB_LIST_KEY)) {
                jobListKey = DOC_JOB_LIST_KEY;
            } else if (jsonObject.has(EVENT_JOB_LIST_KEY)) {
                jobListKey = EVENT_JOB_LIST_KEY;
            } else {
                log.warn("Request did not contain valid JOB_LIST_KEY: "
                        + PRINT_JOB_LIST_KEY + ", " + IMAGE_JOB_LIST_KEY
                        + ", " + DOC_JOB_LIST_KEY);
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
     * @param baseurl  The baseurl to use as mapfish print endpoint
     * @throws IOException    if a requested document could not be printed
     *                        because of Connection problems.
     * @throws PrintException it the print service returned an error.
     */
    protected void handlePrintSpecs(
            List<JSONObject> specs,
            ReportType report,
            String printApp,
            String title,
            String baseurl
    ) throws IOException, PrintException {
        int i = 1;
        String suffix = "";
        String printUrl = baseUrl + "/" + printApp + "/buildreport";
        String printCapaUrl = baseUrl + "/" + printApp + "/capabilities.json";
        if (baseurl != "") {
            printUrl = baseurl + "/" + printApp + "/buildreport";
            printCapaUrl = baseurl + "/" + printApp + "/capabilities.json";
        }
        for (JSONObject spec : specs) {
            if (specs.size() > 1) {
                suffix = " " + Integer.toString(i++);
            }
            if (spec.has("baseurl") && spec.get("baseurl") != "") {
                String specBaseUrl = spec.get("baseurl").toString();
                if (spec.has("printapp") && spec.get("printapp") != "") {
                    String specPrintApp = spec.get("printapp").toString();
                    printUrl = specBaseUrl + "/" + specPrintApp
                            + "/buildreport";
                    printCapaUrl = specBaseUrl + "/" + specPrintApp
                            + "/capabilities.json";
                } else {
                    printUrl = specBaseUrl + "/" + printApp + "/buildreport";
                    printCapaUrl = specBaseUrl + "/" + printApp
                            + "/capabilities.json";
                }
            }
            try {
                URL url = new URL(printUrl);
                URI uri = new URI(
                        url.getProtocol(), url.getUserInfo(),
                        url.getHost(), url.getPort(), url.getPath(),
                        url.getQuery(), url.getRef()
                );
                printUrl = uri.toString();
            } catch (URISyntaxException e) {
                throw new PrintException("URL encoding for printUrl failed.");
            }

            try {
                URL capaurl = new URL(printCapaUrl);
                URI capauri = new URI(
                        capaurl.getProtocol(), capaurl.getUserInfo(),
                        capaurl.getHost(), capaurl.getPort(), capaurl.getPath(),
                        capaurl.getQuery(), capaurl.getRef()
                );
                printCapaUrl = capauri.toString();
            } catch (URISyntaxException e) {
                throw new PrintException("URL encoding for "
                        + "printCapaUrl failed.");
            }

            String baseLayout = spec.getString("layout");

            JSONObject printLayouts = PrintClient.getLayouts(printCapaUrl);
            List<String> printLayoutsList = new ArrayList<String>();
            if (printLayouts.has("layouts")) {
                JSONArray printLayoutsArray = printLayouts
                        .getJSONArray("layouts");
                for (int j = 0; j < printLayoutsArray.length(); j++) {
                     printLayoutsList.add(printLayoutsArray.getJSONObject(j)
                             .get("name").toString());
                }
            }

            if (printLayoutsList.contains(baseLayout)) {
                byte[] content = PrintClient.getReport(printUrl + ".pdf",
                        spec.toString());
                ReportUtils.attachFile(title + suffix, content, report,
                        "application/pdf", title + suffix + ".pdf");
            } else {
                log.info("Layout " + spec.get("layout") + " not found at "
                        +  printCapaUrl);
            }

            // try to fetch additional attachments for map and legend (as png)
            // try map without legend
            if (printLayoutsList.contains(baseLayout + mapSuffix)) {
                spec.put("layout", baseLayout + mapSuffix);
                byte[] content = PrintClient.getReport(printUrl + ".png",
                        spec.toString());
                ReportUtils.attachFile(title + mapSuffix + suffix, content,
                        report, "image/png", title + mapSuffix + suffix
                                + ".png");
            } else {
                log.info("Layout " + spec.get("layout") + " not found at "
                        +  printCapaUrl);
            }

            // legend without map
            if (printLayoutsList.contains(baseLayout + mapSuffix)) {
                spec.put("layout", baseLayout + legendSuffix);
                byte[] content = PrintClient.getReport(printUrl + ".png",
                        spec.toString());
                ReportUtils.attachFile(title + legendSuffix + suffix, content,
                        report, "image/png",
                        title + legendSuffix + suffix + ".png"
                );
            } else {
                log.info("Layout " + spec.get("layout") + " not found at "
                        +  printCapaUrl);
            }
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
            String attachLinkname = "";
            if (spec.has("linkName")) {
                attachLinkname = spec.get("linkName").toString();
                int index = attachLinkname.lastIndexOf("." + outputFormat);
                if (index > 0) {
                    attachLinkname = attachLinkname.substring(0, index);
                }
            }
            if (content.getClass().equals(byte[].class)) {
                if (attachLinkname.length() > 0) {
                    ReportUtils.attachFile(attachLinkname, content, report,
                            mimeType, attachLinkname + "." + outputFormat);
                } else {
                    ReportUtils.attachFile(title + suffix, content, report,
                            mimeType, title + suffix + "." + outputFormat);
                }
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
        //FIXME how to handle authentication headers from original request??
        UploadReportService service = new UploadReportService(irixServiceUrl);
        UploadReportInterface irixservice = service.getUploadReportPort();

        // TODO Add HTTP headers to the web service request

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
        JSONObject userJsonObject = parseHeader(request);
        if (userJsonObject == null) {
            throw new ServletException(
                    "Could not parse Header from request.");
        }

        List<JSONObject> printSpecs = getPrintSpecs(jsonObject);
        // FIXME allow empty printSpecs (IRIX without attachements)
        if (printSpecs.isEmpty()) {
            log.warn("Could not extract any print specs from request.");
            //throw new ServletException(
            //        "Could not extract any print specs from request.");
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
        String baseurl = "";
        if (jsonObject.has("baseurl")) {
            baseurl = jsonObject.getString("baseurl");
        }
        try {
            // FIXME do we have to send userJsonObject as well?
            // better to use a Java Object?
            report = ReportUtils.prepareReport(jsonObject);
            DokpoolUtils.addAnnotation(jsonObject, report, dokpoolSchemaFile);
            if (!printSpecs.isEmpty()) {
                if (printSpecs.get(0).has("jobKey")
                        && printSpecs.get(0).get("jobKey")
                        .hashCode() == EVENT_JOB_LIST_KEY.hashCode()) {
                    log.debug("Found key for eventinformation.");
                } else if (printSpecs.get(0).has("jobKey")
                        && printSpecs.get(0).get("jobKey")
                        .hashCode() == IMAGE_JOB_LIST_KEY.hashCode()) {
                    handleImageSpecs(printSpecs, report,
                            jsonObject.getJSONObject("irix")
                                    .getString("Title"));
                } else if (printSpecs.get(0).has("jobKey")
                        && printSpecs.get(0).get("jobKey")
                        .hashCode() == DOC_JOB_LIST_KEY.hashCode()) {
                    handleImageSpecs(printSpecs, report,
                            jsonObject.getJSONObject("irix")
                                    .getString("Title"));
                } else {
                    handlePrintSpecs(printSpecs, report,
                            jsonObject.getString("printapp"),
                            jsonObject.getJSONObject("irix")
                                    .getString("Title"),
                            baseurl);
                }
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
