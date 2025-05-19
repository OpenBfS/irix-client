/* Copyright (C) 2015-2025 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE for details.
 */

package de.intevation.irix;

import java.io.IOException;
import java.io.File;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;

import jakarta.xml.bind.JAXBException;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.MessageContext;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;
import static java.lang.System.Logger.Level.INFO;

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
    private static System.Logger log = System.getLogger(IRIXClient.class.getName());
    private static final int BUF_SIZE = 8192;

    /**
     * Default timeout in milliseconds.
     * Currently only used for WSDL file download.
     */
    protected static final int CONNECTION_TIMEOUT = 5000;

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
     * AUTH_TYPE_NONE = "none".
     */
    protected static final String AUTH_TYPE_NONE = "none";
    /**
     * AUTH_TYPE_BASIC = "basic-auth".
     */
    protected static final String AUTH_TYPE_BASIC = "basic-auth";

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
    protected String defaultBaseUrl;
    /**
     * Timeout of http requests to the mapfish-print service in milliseconds.
     */
    protected int printTimeout;
    /**
     * URL of irix-webservice upload service.
     */
    protected URL irixServiceUrl;
    /**
     * Location of the local temporary compy of the WSDL file.
     */
    protected String irixServiceWsdlTmp;
     /**
      * Authentication type of irix-webservice upload service.
      * Currently: "none" or "basic-auth".
      */
    protected String irixServiceAuthType;
    /**
     * Authentication credential of irix-webservice upload service.
     * For "basic-auth" this is the base64 encoded string passend after
     * "Basic " in the HTTP Authentication header.
     */
    protected String irixServiceAuthCred;
    /**
     * String of Header username - e.g. set by Shibboleth
     */
    protected String userHeaderString;
    /**
     * String of Header roles - e.g. set by Shibboleth
     */
    protected String rolesHeaderString;
    /**
     * String of Header user displayname - e.g. set by Shibboleth
     */
    protected String displaynameHeaderString;
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
     * @throws jakarta.servlet.ServletException if a configuration
     *                                        parameter is missing.
     */
    @Override
    public void init() throws ServletException {
        defaultBaseUrl = getInitParameter("print-url");

        if (defaultBaseUrl == null) {
            throw new ServletException("Missing 'print-url' parameter");
        }

        try {
            printTimeout = Integer.parseUnsignedInt(getInitParameter("print-timeout-ms"));
        } catch (NumberFormatException nfe) { //also handles null
            printTimeout = PrintClient.CONNECTION_TIMEOUT;
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

        irixServiceWsdlTmp = getInitParameter("irix-webservice-wsdl-tmpfile");
        if (irixServiceWsdlTmp == null) {
            irixServiceWsdlTmp = "/tmp/upload-report.wsdl";
        }

        irixServiceAuthType = getInitParameter("irix-webservice-auth-type");
        if (irixServiceAuthType == null) {
            irixServiceAuthType = AUTH_TYPE_NONE;
        }
        switch (irixServiceAuthType) {
            case AUTH_TYPE_NONE:
            case AUTH_TYPE_BASIC:
                break;

            default:
                throw new ServletException(
                    "Unknown 'irix-webservice-auth-type' parameter.");
        }

        irixServiceAuthCred = getInitParameter("irix-webservice-auth-cred");
        if (irixServiceAuthCred == null && !irixServiceAuthType.equals(AUTH_TYPE_NONE)) {
            throw new ServletException(
                    "All authentication types except 'none' require the 'irix-webservice-auth-cred' parameter.");
        }

        userHeaderString = getInitParameter("user-header");
        if (userHeaderString == null) {
            log.log(DEBUG, "No user-header set.");
        }

        displaynameHeaderString = getInitParameter("user-displayname-header");
        if (displaynameHeaderString == null) {
            log.log(DEBUG, "No user-displayname-header set.");
        }

        rolesHeaderString = getInitParameter("roles-header");
        if (rolesHeaderString == null) {
            log.log(DEBUG, "No roles-header set.");
        }

        String rolesPermissionParam = getInitParameter("roles-permission");
        if (rolesPermissionParam != null) {
            rolesPermission = Arrays.asList(rolesPermissionParam);
            if (rolesPermission.isEmpty()) {
                log.log(DEBUG, "No roles-permission set.");
                // parseHeaders(request) will check for valid role in Header
            }
        } else {
            rolesPermission = null;
            log.log(DEBUG, "No init-param roles-permission found.");
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
            log.log(WARNING, "Request did not contain valid json: " + e.getMessage());
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
            if (uid != null) {
                uidHeaders.put("uid", uid);
            }
        }
        if (displaynameHeaderString != null) {
            String displayname = request.getHeader(displaynameHeaderString);
            if (displayname != null) {
                uidHeaders.put("displayname", displayname);
            }
        }
        if (rolesHeaderString != null) {
            if (request.getHeader(rolesHeaderString) != null) {
                List<String> roles = Arrays.asList(request
                        .getHeader(rolesHeaderString).split("[\\s,;]+"));
                if (rolesPermission != null) {
                    List<String> validRolesList = rolesPermission.stream()
                            .filter(roles::contains)
                            .collect(Collectors.toList());
                    if (validRolesList.isEmpty()) {
                        log.log(DEBUG, "No valid roles found for user "
                                + uidHeaders.get("uid").toString());
                    } else {
                        uidHeaders.put("roles", validRolesList);
                    }
                } else {
                    uidHeaders.put("roles", roles);
                }

            }
        }
        return uidHeaders;
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
                log.log(WARNING, "Request did not contain valid JOB_LIST_KEY: "
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
            log.log(WARNING, "Request did not contain valid json: " + e.getMessage());
        }
        return retval;
    }

    /**
     * Wrapper to forward a print error response.
     *
     * @param err      the PrintException.
     * @param response the jakarta.servlet.http.HttpServletResponse
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
     * @param response the jakarta.servlet.http.HttpServletResponse
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
     * @param specs          A list of the json print specs.
     * @param report         The report to attach the data to.
     * @param printApp       The printApp to use
     * @param title          The title for the Annex
     * @param commonBaseUrl  The baseurl to use as mapfish print endpoint for all specs.
     *                       Print spec "baseurl" takes precedence if it exists.
     * @throws IOException    if a requested document could not be printed
     *                        because of Connection problems.
     * @throws PrintException it the print service returned an error.
     */
    protected void handlePrintSpecs(
            List<JSONObject> specs,
            ReportType report,
            String printApp,
            String title,
            String commonBaseUrl
    ) throws IOException, PrintException {
        int i = 1;
        String suffix = "";
        // baseurl precedence: print spec > common (report) url > default url
        String printUrl = defaultBaseUrl + "/" + printApp + "/buildreport";
        String printCapaUrl = defaultBaseUrl + "/" + printApp + "/capabilities.json";
        if (commonBaseUrl != "") {
            printUrl = commonBaseUrl + "/" + printApp + "/buildreport";
            printCapaUrl = commonBaseUrl + "/" + printApp + "/capabilities.json";
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

            JSONObject printLayouts = PrintClient.getLayouts(printCapaUrl, printTimeout);
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
                        spec.toString(), printTimeout);
                ReportUtils.attachFile(title + suffix, content, report,
                        "application/pdf", title + suffix + ".pdf");
            } else {
                log.log(INFO, "Layout " + spec.get("layout") + " not found at "
                        +  printCapaUrl);
            }

            // try to fetch additional attachments for map and legend (as png)
            // try map without legend
            if (printLayoutsList.contains(baseLayout + mapSuffix)) {
                spec.put("layout", baseLayout + mapSuffix);
                byte[] content = PrintClient.getReport(printUrl + ".png",
                        spec.toString(), printTimeout);
                ReportUtils.attachFile(title + mapSuffix + suffix, content,
                        report, "image/png", title + mapSuffix + suffix
                                + ".png");
            } else {
                log.log(INFO, "Layout " + spec.get("layout") + " not found at "
                        +  printCapaUrl);
            }

            // legend without map
            if (printLayoutsList.contains(baseLayout + legendSuffix)) {
                spec.put("layout", baseLayout + legendSuffix);
                byte[] content = PrintClient.getReport(printUrl + ".png",
                        spec.toString(), printTimeout);
                ReportUtils.attachFile(title + legendSuffix + suffix, content,
                        report, "image/png",
                        title + legendSuffix + suffix + ".png"
                );
            } else {
                log.log(INFO, "Layout " + spec.get("layout") + " not found at "
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

    private void fetchWSDL() throws ServletException {
        HttpClient client = java.net.http.HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .header("Authorization", "Basic " + irixServiceAuthCred)
            .GET()
            .uri(URI.create(irixServiceUrl.toString() + "?wsdl"))
            .timeout(Duration.ofMillis(CONNECTION_TIMEOUT))
            .build();

        int statusCode = 0;
        try {
            HttpResponse<Path> response = client.send(request,
                HttpResponse.BodyHandlers.ofFile(Path.of(irixServiceWsdlTmp)));
            statusCode = response.statusCode();
        } catch (InterruptedException | IOException e) {
            throw new ServletException("WSDL could be downloaded.");
        }

        if (statusCode < HttpURLConnection.HTTP_OK
            || statusCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
            throw new ServletException("WSDL could be downloaded.");
        }

    }

    /**
     * Sends a report to the configured UploadReport service.
     *
     * @param report The report to send.
     * @throws jakarta.servlet.ServletException if uploading failed.
     */
    protected void sendReportToService(ReportType report)
            throws ServletException {
        //FIXME how pass on authentication headers from original request??

        fetchWSDL();
        URL wsdlUrl = null;
        try {
            wsdlUrl = new URL("file://" + irixServiceWsdlTmp);
        } catch (MalformedURLException e) {
        }

        UploadReportService service = new UploadReportService(wsdlUrl);
        UploadReportInterface irixservice = service.getUploadReportPort();

        // TODO Add further HTTP headers to the web service request?

        switch (irixServiceAuthType) {
            case AUTH_TYPE_BASIC:
                Map<String, Object> reqCon = ((BindingProvider) irixservice).getRequestContext();
                //TODO: This line seems to override the endpoint (e.g. host, port) found in the wsdl file.
                //This is usually good, because bad proxy configurations cannot break the
                //communication, but it also obstructs intentional changes to urls via wsdl file.
                reqCon.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, irixServiceUrl.toString());
                reqCon.put(MessageContext.HTTP_REQUEST_HEADERS, Map.of("Authorization",
                    List.of("Basic " + irixServiceAuthCred)));
            default:
                break;
        }

        log.log(DEBUG, "Sending report.");
        try {
            irixservice.uploadReport(report);
        } catch (UploadReportException_Exception e) {
            throw new ServletException(
                    "Failed to send report to IRIX service.", e);
        }
        log.log(DEBUG, "Report successfully sent.");
    }
    /**
     * Handle GET request.
     * <p>
     * So far return only Version information
     *
     * @param request  object that contains the request the client has made
     *                 of the servlet
     * @param response object that contains the response the servlet sends
     *                 to the client
     * @throws ServletException in case of errors with schema.
     * @throws IOException      if the request is invalid.
     */
    @Override
    public void doGet(HttpServletRequest request,
                       HttpServletResponse response)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        //getServletConfig().getServletName();
        return;
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

        if (rolesPermission != null) {
            if (request.getHeader(rolesHeaderString) != null) {
                List<String> roles = Arrays.asList(request
                        .getHeader(rolesHeaderString).split("[\\s,;]+"));
                List<String> validRolesList = rolesPermission.stream()
                        .filter(roles::contains)
                        .collect(Collectors.toList());
                if (validRolesList.isEmpty()) {
                    log.log(DEBUG, "No valid roles found for user");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        JSONObject jsonObject = parseRequest(request);
        if (jsonObject == null) {
            throw new ServletException(
                    "Could not read jsonObject from request.");
        }
        // FIXME may be this test has obsolete conditions?
        JSONObject userJsonObject = parseHeader(request);
        if (userJsonObject.length() == 0
                && (request.getHeader(userHeaderString) != null
                || request.getHeader(rolesHeaderString) != null)
        ) {
            throw new ServletException(
                    "Could not parse Header from request. Empty JSON returned");
        }

        List<JSONObject> printSpecs = getPrintSpecs(jsonObject);
        // FIXME allow empty printSpecs (IRIX without attachements)
        if (printSpecs.isEmpty()) {
            log.log(WARNING, "Could not extract any print specs from request.");
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
        String reportBaseUrl = "";
        if (jsonObject.has("baseurl")) {
            reportBaseUrl = jsonObject.getString("baseurl");
        }
        try {
            // FIXME do we have to send userJsonObject as well?
            // better to use a Java Object?
            report = ReportUtils.prepareReport(jsonObject, userJsonObject);
            DokpoolUtils.addAnnotation(jsonObject, report, dokpoolSchemaFile,
                    userJsonObject);
            if (!printSpecs.isEmpty()) {
                if (printSpecs.get(0).has("jobKey")
                        && printSpecs.get(0).get("jobKey")
                        .hashCode() == EVENT_JOB_LIST_KEY.hashCode()) {
                    log.log(DEBUG, "Found key for eventinformation.");
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
                            reportBaseUrl);
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
                log.log(ERROR, se);
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
                log.log(ERROR, se);
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
