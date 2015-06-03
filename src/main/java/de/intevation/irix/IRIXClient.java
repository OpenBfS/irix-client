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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;

import org.iaea._2012.irix.format.ReportType;
import org.iaea._2012.irix.format.ObjectFactory;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import de.intevation.irixservice.UploadReportService;
import de.intevation.irixservice.UploadReportInterface;
import de.intevation.irixservice.UploadReportException_Exception;

public class IRIXClient extends HttpServlet
{
    private static Logger log = Logger.getLogger(IRIXClient.class);

    /** The name of the json array containing the print descriptions. */
    private static final String PRINT_JOB_LIST_KEY = "mapfish-print";

    private static final String REQUEST_TYPE_UPLOAD = "upload";
    private static final String REQUEST_TYPE_RESPOND = "respond";
    private static final String REQUEST_TYPE_UPLOAD_AND_RESPOND = "upload/respond";

    protected String mPDFUrl;
    protected String mMapUrl;
    protected String mLegendUrl;

    public void init() throws ServletException {
        String baseUrl = getInitParameter("print-url");

        if (baseUrl == null) {
            throw new ServletException("Missing 'print-url' as init-param in web.xml");
        }

        String pdfUrl = getInitParameter("pdf-service-url");
        if (pdfUrl == null) {
            throw new ServletException("Missing 'pdf-service-url' as init-param in web.xml");
        }
        mPDFUrl = baseUrl + pdfUrl;

        String mapUrl = getInitParameter("map-png-service-url");
        if (mapUrl == null) {
            throw new ServletException("Missing 'map-png-service-url' as init-param in web.xml");
        }
        mMapUrl = baseUrl + mapUrl;

        String legendUrl = getInitParameter("legend-png-service-url");
        if (legendUrl == null) {
            throw new ServletException("Missing 'legend-png-service-url' as init-param in web.xml");
        }
        mLegendUrl = baseUrl + legendUrl;
    }

    /** Parse the content of the request into a json object. */
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

    /** Obtain all the print specs in the JSON reques . */
    protected List<JSONObject> getPrintSpecs(JSONObject jsonObject) {
        List <JSONObject> retval = new ArrayList<JSONObject>();
        try {
            JSONArray mapfishPrintList = jsonObject.getJSONArray(PRINT_JOB_LIST_KEY);
            for (int i = 0; i < mapfishPrintList.length(); i++) {
                JSONObject jobDesc = mapfishPrintList.getJSONObject(i);
                retval.add(jobDesc);
            }
        } catch (JSONException e) {
            log.warn("Request did not contain valid json: " + e.getMessage());
        }
        return retval;
    }

    /** Wrapper to wirte an error message as response. */
    public void writeError(String msg, HttpServletResponse response)
        throws IOException {
        response.setContentType("text/html");
        response.setStatus(500);
        response.getOutputStream().print(msg);
        log.debug("Sending error: " + msg);
        response.getOutputStream().flush();
        return;
    }

    /** Helper method to print / attach the requested documents.
     *
     * @param specs A list of the json print specs.
     * @param report The report to attach the data to.
     */
    public void handlePrintSpecs(List<JSONObject> specs, ReportType report)
        throws JSONException, IOException {
        for (JSONObject spec: specs) {
            String title = null;
            title = spec.getJSONObject("attributes").getString("title");

            byte[] content = PrintClient.getReport(mPDFUrl, spec.toString());
            ReportUtils.attachFile(title, content, report, "application/pdf", title + ".pdf");

            // map without legend
            content = PrintClient.getReport(mMapUrl, spec.toString());
            ReportUtils.attachFile(title + " Map", content, report, "image/png", title + " Map.png");

            // legend without map
            content = PrintClient.getReport(mLegendUrl, spec.toString());
            ReportUtils.attachFile(title + " Legend", content, report, "image/png", title + " Legend.png");
        }
    }

    /** Sends a report to the UploadReport service configured during buildtime.
     *
     * @param report The report to send. */
    void sendReportToService(ReportType report)
        throws ServletException {
        UploadReportService service = new UploadReportService();
        UploadReportInterface irixservice = service.getUploadReportPort();
        log.debug("Sending report.");
        try {
            irixservice.uploadReport(report);
        } catch (UploadReportException_Exception e) {
            throw new ServletException("Failed to send report to IRIX service.", e);
        }
        log.debug("Report successfully sent.");
    }

    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
        throws ServletException, IOException  {

        JSONObject jsonObject = parseRequest(request);
        if (jsonObject == null) {
            writeError("Could not read jsonObject from request", response);
            return;
        }

        List<JSONObject> printSpecs = getPrintSpecs(jsonObject);
        if (printSpecs.isEmpty()) {
            writeError("Could not extract any print specs from request", response);
            return;
        }

        String requestType = null;
        try {
            requestType = jsonObject.getString("request-type");
        } catch (JSONException e) {
            writeError("Failed to parse request-type: "+ e.getMessage(), response);
            return;
        }
        requestType = requestType.toLowerCase();

        if (!requestType.equals(REQUEST_TYPE_UPLOAD) &&
            !requestType.equals(REQUEST_TYPE_RESPOND) &&
            !requestType.equals(REQUEST_TYPE_UPLOAD_AND_RESPOND)) {
            writeError("Unknown request-type: " + requestType, response);
            return;
        }

        ReportType report = null;
        try {
            report = ReportUtils.prepareReport(jsonObject);
            ReportUtils.addAnnotation(jsonObject, report);
            handlePrintSpecs(printSpecs, report);
        } catch (JSONException e) {
            writeError("Failed to parse IRIX information: "+ e.getMessage(), response);
            return;
        }

        if (requestType.equals(REQUEST_TYPE_UPLOAD) ||
            requestType.equals(REQUEST_TYPE_UPLOAD_AND_RESPOND)) {
            sendReportToService(report);
        }

        if (requestType.equals(REQUEST_TYPE_RESPOND) ||
            requestType.equals(REQUEST_TYPE_UPLOAD_AND_RESPOND)) {
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(ReportType.class);
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
                jaxbMarshaller.marshal(new ObjectFactory().createReport(report), response.getOutputStream());
                response.setContentType("application/xml");
            } catch (JAXBException e) {
                writeError("Failed to print requested spec." + e.toString(), response);
            }
            response.getOutputStream().flush();
        }
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws ServletException, IOException  {
    }

    public void destroy() {
    }
}
