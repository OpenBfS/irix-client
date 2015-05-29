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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.math.BigInteger;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.DatatypeConverter;
import javax.xml.transform.dom.DOMResult;

import org.apache.log4j.Logger;

import org.iaea._2012.irix.format.ObjectFactory;
import org.iaea._2012.irix.format.ReportType;
import org.iaea._2012.irix.format.identification.IdentificationType;
import org.iaea._2012.irix.format.identification.IdentificationsType;
import org.iaea._2012.irix.format.identification.ReportContextType;
import org.iaea._2012.irix.format.annexes.AnnexesType;
import org.iaea._2012.irix.format.annexes.AnnotationType;
import org.iaea._2012.irix.format.annexes.FileEnclosureType;
import org.iaea._2012.irix.format.annexes.FileHashType;
import org.iaea._2012.irix.format.base.FreeTextType;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import de.bfs.irix.extensions.dokpool.DokpoolMeta;

public class IRIXClient extends HttpServlet
{
    private static Logger log = Logger.getLogger(IRIXClient.class);

    /** The name of the json array containing the print descriptions. */
    private static final String PRINT_JOB_LIST_KEY = "mapfish-print";

    /** The name of the json object containing the irix information. */
    private static final String IRIX_DATA_KEY = "irix";

    /** The name of the json object containing the DokpoolMeta information. */
    private static final String DOKPOOL_DATA_KEY = "DokpoolMeta";

    /** The fields to set in the DokpoolMeta object.
     *
     * SamplingBegin and SamplingEnd are handled wether or not they
     * are part of this list.
     **/
    private static final String[] DOKPOOL_FIELDS = new String[] {
        "DokpoolContentType",
        "IsElan",
        "IsDoksys",
        "IsRodos",
        "IsRei",
        "NetworkOperator",
        "SampleTypeId",
        "SampleType",
        "Dom",
        "DataType",
        "LegalBase",
        "MeasuringProgram",
        "Status"
    };

    private ObjectFactory mObjFactory;

    protected String mPDFUrl;
    protected String mMapUrl;
    protected String mLegendUrl;

    public void init() throws ServletException {
        mObjFactory = new ObjectFactory();
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

    /** Helper method to obtain the current datetime as XMLGregorianCalendar.*/
    protected XMLGregorianCalendar getXMLGregorianNow() {
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date(System.currentTimeMillis()));
        try {
            XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
            return date;
        } catch (DatatypeConfigurationException e) {
            log.error("Exception converting to XMLGregorianCalendar");
            return null;
        }
    }

    /** Prepare the IRIX report to take the PDF attachments.
     *
     * The irix information is taken from the Object specified
     * by the IRIX_DATA_KEY name.
     *
     * @param jsonObject The full jsonObject of the request.
     */
    protected ReportType prepareReport(JSONObject jsonObject)
        throws JSONException {
        ReportType report = mObjFactory.createReportType();
        JSONObject idObj = jsonObject.getJSONObject(IRIX_DATA_KEY).getJSONObject("Identification");

        // Setup identification
        IdentificationType identification = new IdentificationType();
        identification.setOrganisationReporting(idObj.getString("OrganisationReporting"));
        identification.setDateAndTimeOfCreation(getXMLGregorianNow());
        if (idObj.has("SequenceNumber")) {
            identification.setSequenceNumber(new BigInteger(idObj.getString("SequenceNumber")));
        }
        identification.setReportUUID(UUID.randomUUID().toString());

        // Setup identifications in identification
        IdentificationsType identifications = new IdentificationsType();
        identification.setIdentifications(identifications);

        // setPersonContactInfo and organizationcontactinfo ?
        identification.setReportContext(ReportContextType.fromValue(
                idObj.getString("ReportContext")));
        report.setIdentification(identification);
        AnnexesType annex = new AnnexesType();
        report.setAnnexes(annex);

        return report;
    }

    /** Parses an XML Calendar string into an XMLGregorianCalendar object.
     *
     * @param str An  ISO 8601 DateTime like: 2015-05-28T15:35:54.168+02:00
     */
    protected XMLGregorianCalendar xmlCalendarFromString(String str) {
        try {
            Calendar cal = DatatypeConverter.parseDateTime(str);
            GregorianCalendar c = new GregorianCalendar();
            c.setTime(cal.getTime());
            XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
            return date;
        } catch (DatatypeConfigurationException e) {
            log.debug("Failed parse Date value '" + str + "' :" + e.getMessage());
        }
        return null;
    }


    /** Add an annotation to the Report containting the DokpoolMeta data fields.
     *
     * @param report The report to add the annoation to.
     * @param jsonObject The full jsonObject of the request.
     **/
    protected void addAnnotation(JSONObject jsonObject, ReportType report)
        throws JSONException {
        AnnotationType annotation = new AnnotationType();
        FreeTextType freeText = new FreeTextType();
        // freeText should probably get some content.
        annotation.setText(freeText);
        JSONObject irixObj = jsonObject.getJSONObject(IRIX_DATA_KEY);
        annotation.setTitle(irixObj.getString("Title"));

        JSONObject metaObj = irixObj.getJSONObject(DOKPOOL_DATA_KEY);

        DokpoolMeta meta = new DokpoolMeta();
        for (String field: DOKPOOL_FIELDS) {
            java.lang.reflect.Method method;
            String methodName = "set" + field;
            String value = metaObj.getString(field);
            try {
                if (field.startsWith("Is")) {
                    method = meta.getClass().getMethod(methodName, boolean.class);
                    method.invoke(meta, value.toLowerCase().equals("true"));
                } else {
                    method = meta.getClass().getMethod(methodName, String.class);
                    method.invoke(meta, value);
                }
            } catch (Exception e) {
                log.error(e.getClass().getName() +
                        " exception while trying to access " + methodName +
                        " on DokpoolMeta object.");
            }
        }

        // Handle the datetime values
        meta.setSamplingBegin(xmlCalendarFromString(metaObj.getString("SamplingBegin")));
        meta.setSamplingEnd(xmlCalendarFromString(metaObj.getString("SamplingEnd")));

        DOMResult res = new DOMResult();
        Element ele = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(DokpoolMeta.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            jaxbMarshaller.marshal(meta, res);
            ele = ((Document)res.getNode()).getDocumentElement();
        } catch (JAXBException e) {
            log.error("Failed marshall DokpoolMeta object.: "+ e.toString());
        }

        annotation.getAny().add(ele);
        report.getAnnexes().getAnnotation().add(annotation);
    }

    /** Helper method to print all request parameters into the trace log for debugging. */
    private void dumpParams(HttpServletRequest request) {
        Enumeration<String> parameterNames = request.getParameterNames();
        log.trace("Request parameters Begin");
        while (parameterNames.hasMoreElements()) {
            String param = parameterNames.nextElement();
            log.trace("Parameter: " + param);
            String[] values = request.getParameterMap().get(param);
            if (values == null) {
                continue;
            }
            for (String value: values) {
                log.trace("Value: " + value);
            }
        }
        log.trace("Request parameters End");
    }

    /** Parse the content of the request into a json object. */
    protected JSONObject parseRequest(HttpServletRequest request) {
        dumpParams(request);
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

    /** Attach a file as Annex on a ReportType object.
     *
     * @param title The title of the FileEnclosure.
     * @param data Binary content of the file.
     * @param report Report to attach the file to.
     * @param mimeType The mime type of that file.
     * @param fileName The filename to set.
     */

    public void attachFile(String title, byte[] data, ReportType report,
                           String mimeType, String fileName) {

        // Hashsum, algo should probably be configurable.
        FileHashType hash = new FileHashType();
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] sha1sum = sha1.digest(data);
            hash.setValue(sha1sum);
            hash.setAlgorithm("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 unavailable. Can't happen.");
        }

        // Add the acutal file
        FileEnclosureType file = new FileEnclosureType();
        file.setTitle(title);
        file.setMimeType(mimeType);
        file.setFileSize(data.length);
        file.setFileHash(hash);
        file.setFileName(fileName);
        file.setEnclosedObject(data);
        report.getAnnexes().getFileEnclosure().add(file);
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
            attachFile(title, content, report, "application/pdf", title + ".pdf");

            // map without legend
            content = PrintClient.getReport(mMapUrl, spec.toString());
            attachFile(title + " Map", content, report, "image/png", title + " Map.png");

            // legend without map
            content = PrintClient.getReport(mLegendUrl, spec.toString());
            attachFile(title + " Legend", content, report, "image/png", title + " Legend.png");
        }
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

        ReportType report = null;
        try {
            report = prepareReport(jsonObject);
            addAnnotation(jsonObject, report);
            handlePrintSpecs(printSpecs, report);
        } catch (JSONException e) {
            writeError("Failed to parse IRIX information: "+ e.getMessage(), response);
            return;
        }

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ReportType.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            jaxbMarshaller.marshal(mObjFactory.createReport(report), response.getOutputStream());
            response.setContentType("application/xml");
        } catch (JAXBException e) {
            writeError("Failed to print requested spec." + e.toString(), response);
        }
        response.getOutputStream().flush();
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws ServletException, IOException  {
    }

    public void destroy() {
    }
}
