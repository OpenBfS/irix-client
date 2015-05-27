/* Copyright (C) 2015 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE.txt for details.
 */

package de.intevation.irix;

import java.io.PrintWriter;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.util.GregorianCalendar;
import java.util.Date;
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

public class IRIXClient extends HttpServlet
{
    private static Logger log = Logger.getLogger(IRIXClient.class);

    private ObjectFactory mObjFactory;
    public void init() throws ServletException {
        mObjFactory = new ObjectFactory();
    }

    protected XMLGregorianCalendar getXMLGregorianNow() {
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date(System.currentTimeMillis()));
        try {
            XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
            return date;
        } catch (DatatypeConfigurationException e) {
            log.debug("Exception converting to XMLGregorianCalendar");
            return null;
        }
    }

    protected ReportType prepareReport() {
        ReportType report = mObjFactory.createReportType();

        // Setup identification
        IdentificationType identification = new IdentificationType();
        identification.setOrganisationReporting("TODO make dynamic");
        identification.setDateAndTimeOfCreation(getXMLGregorianNow());
        identification.setSequenceNumber(new BigInteger("42"));
        identification.setReportUUID("TODO generate UUID");

        // Setup identifications in identification
        IdentificationsType identifications = new IdentificationsType();
        identification.setIdentifications(identifications);

        // TODO setPersonContactInfo and organizationcontactinfo

        identification.setReportContext(ReportContextType.fromValue("Test"));
        report.setIdentification(identification);
        return report;
    }

    public void doGet(HttpServletRequest request,
            HttpServletResponse response)
        throws ServletException, IOException  {
        log.debug("Got a get");
        ReportType report = prepareReport();

        //response.setContentType("text/html");
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ReportType.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(mObjFactory.createReport(report), response.getOutputStream());
        } catch (JAXBException e) {
            response.setStatus(500);
            response.getOutputStream().print(e.toString());
            log.debug("Failed to marshall report: \n" + e);
        }
        response.getOutputStream().flush();
    }

    public void destroy() {
    }
}
