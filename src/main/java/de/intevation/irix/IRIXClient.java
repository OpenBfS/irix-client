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
import java.util.Date;
import java.util.Enumeration;
import java.math.BigInteger;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

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

import org.json.JSONObject;
import org.json.JSONException;

public class IRIXClient extends HttpServlet
{
    private static Logger log = Logger.getLogger(IRIXClient.class);

    private ObjectFactory mObjFactory;

    protected String mPrintUrl;

    public void init() throws ServletException {
        mObjFactory = new ObjectFactory();
        mPrintUrl = getInitParameter("print-url");

        if (mPrintUrl == null) {
            throw new ServletException("Missing 'print-url' as init-param in web.xml");
        }
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

    protected ReportType prepareReport() {
        ReportType report = mObjFactory.createReportType();

        // Setup identification
        IdentificationType identification = new IdentificationType();
        identification.setOrganisationReporting("Dummy Value");
        identification.setDateAndTimeOfCreation(getXMLGregorianNow());
        identification.setSequenceNumber(new BigInteger("42"));
        identification.setReportUUID("Dummy Value");

        // Setup identifications in identification
        IdentificationsType identifications = new IdentificationsType();
        identification.setIdentifications(identifications);

        // TODO setPersonContactInfo and organizationcontactinfo
        identification.setReportContext(ReportContextType.fromValue("Test"));
        report.setIdentification(identification);
        return report;
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
    protected String getPrintSpec(JSONObject jsonObject) {
        // TODO this will be more complicated in reality.
        return jsonObject.toString();
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

    /** Attach a pdf as Annex on a ReportType object */
    public void attachPDF(byte[] pdf, ReportType report) {
        AnnexesType annex = new AnnexesType();

        // Add an annoation
        //AnnotationType annotation = new AnnotationType();
        //annotation.setText("Some text here.");
        //annotation.setTitle("The title of this annotation");
        //annex.getAnnotation().add(annotation);

        // Hashsum, algo should probably be configurable.
        FileHashType hash = new FileHashType();
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] sha1sum = sha1.digest(pdf);
            hash.setValue(sha1sum);
            hash.setAlgorithm("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 unavailable. Can't happen.");
        }

        // Add the acutal file
        FileEnclosureType file = new FileEnclosureType();
        file.setTitle("Dummy value");
        file.setMimeType("application/pdf");
        file.setFileSize(pdf.length);
        file.setFileHash(hash);
        file.setEnclosedObject(pdf);
        annex.getFileEnclosure().add(file);

        report.setAnnexes(annex);
    }

    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
        throws ServletException, IOException  {

        JSONObject jsonObject = parseRequest(request);
        if (jsonObject == null) {
            writeError("Could not read jsonObject from request", response);
            return;
        }

        String printSpec = getPrintSpec(jsonObject);
        if (printSpec == null) {
            writeError("Could not extract print spec from request", response);
            return;
        }

        byte[] pdfContent = PrintClient.getPDF(mPrintUrl, printSpec);

        if (pdfContent == null) {
            writeError("Failed to print requested spec.", response);
            return;
        }

        ReportType report = prepareReport();

        attachPDF(pdfContent, report);

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ReportType.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
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
