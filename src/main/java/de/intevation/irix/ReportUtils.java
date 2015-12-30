/* Copyright (C) 2015 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE.txt for details.
 */

package de.intevation.irix;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.TimeZone;
import java.math.BigInteger;

import java.io.OutputStream;

import java.io.File;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.DatatypeConverter;
import javax.xml.transform.dom.DOMResult;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.XMLConstants;

import org.xml.sax.SAXException;

import org.apache.log4j.Logger;

import org.iaea._2012.irix.format.ReportType;
import org.iaea._2012.irix.format.ObjectFactory;
import org.iaea._2012.irix.format.base.OrganisationContactType;
import org.iaea._2012.irix.format.base.PersonContactType;
import org.iaea._2012.irix.format.identification.IdentificationType;
import org.iaea._2012.irix.format.identification.IdentificationsType;
import org.iaea._2012.irix.format.identification.ReportContextType;
import org.iaea._2012.irix.format.annexes.AnnexesType;
import org.iaea._2012.irix.format.annexes.AnnotationType;
import org.iaea._2012.irix.format.annexes.FileEnclosureType;
import org.iaea._2012.irix.format.annexes.FileHashType;
import org.iaea._2012.irix.format.base.FreeTextType;

import org.json.JSONObject;
import org.json.JSONException;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import de.bfs.irix.extensions.dokpool.DokpoolMeta;

/**
 * Static helper methods to work with an IRIX Report.
 *
 * This class provides helper methods to work with the
 * IRIX document scheme and the Dokpool extension.
 * The helper methods are directly tied to the IRIXClient JSON input format.
 *
 */
public final class ReportUtils {
    private static Logger log = Logger.getLogger(ReportUtils.class);

    /** The name of the json object containing the irix information. */
    private static final String IRIX_DATA_KEY = "irix";

    /** The name of the json object containing the DokpoolMeta information. */
    private static final String DOKPOOL_DATA_KEY = "DokpoolMeta";

    /** This is according to current documentation the fixed value.*/
    private static final String SCHEMA_VERSION = "1.0";

    /** Helper class to describe modifications
     *  to the IRIX DokpoolMeta element. */
    private abstract static class DokpoolMetaModifier {

        protected String fieldname;

        public DokpoolMetaModifier(String fname) {
            fieldname = fname;
        }

        public boolean modify(DokpoolMeta dpm, JSONObject meta) {
            if (meta.has(fieldname)) {
                return change(dpm, meta);
            }
            return false;
        }
        protected abstract boolean change(DokpoolMeta dpm, JSONObject meta);
    }

    /** The fields to set in the DokpoolMeta object.
     *
     * SamplingBegin and SamplingEnd are handled wether or not they
     * are part of this list.
     **/
    private static final DokpoolMetaModifier[] DOKPOOL_META_MODIFIERS =
        new DokpoolMetaModifier[] {

        new DokpoolMetaModifier("DokpoolMetaModifier") {
            @Override
            protected boolean change(DokpoolMeta dpm, JSONObject meta) {
                dpm.setDokpoolContentType(meta.getString(fieldname));
                return false;
            }
        },
        new DokpoolMetaModifier("IsElan") {
            @Override
            protected boolean change(DokpoolMeta dpm, JSONObject meta) {
                boolean value = meta.getBoolean(fieldname);
                dpm.setIsElan(value);
                return value;
            }
        },
        new DokpoolMetaModifier("IsDoksys") {
            @Override
            protected boolean change(DokpoolMeta dpm, JSONObject meta) {
                boolean value = meta.getBoolean(fieldname);
                dpm.setIsDoksys(value);
                return value;
            }
        },
        new DokpoolMetaModifier("IsRodos") {
            @Override
            protected boolean change(DokpoolMeta dpm, JSONObject meta) {
                boolean value = meta.getBoolean(fieldname);
                dpm.setIsRodos(value);
                return value;
            }
        },
        new DokpoolMetaModifier("IsRei") {
            @Override
            protected boolean change(DokpoolMeta dpm, JSONObject meta) {
                boolean value = meta.getBoolean(fieldname);
                dpm.setIsRei(value);
                return value;
            }
        },
        new DokpoolMetaModifier("NetworkOperator") {
            @Override
            protected boolean change(DokpoolMeta dpm, JSONObject meta) {
                dpm.setNetworkOperator(meta.getString(fieldname));
                return false;
            }
        },
        new DokpoolMetaModifier("SampleTypeId") {
            @Override
            protected boolean change(DokpoolMeta dpm, JSONObject meta) {
                dpm.setSampleTypeId(meta.getString(fieldname));
                return false;
            }
        },
        new DokpoolMetaModifier("SampleType") {
            @Override
            protected boolean change(DokpoolMeta dpm, JSONObject meta) {
                dpm.setSampleType(meta.getString(fieldname));
                return false;
            }
        },
        new DokpoolMetaModifier("Dom") {
            @Override
            protected boolean change(DokpoolMeta dpm, JSONObject meta) {
                dpm.setDom(meta.getString(fieldname));
                return false;
            }
        },
        new DokpoolMetaModifier("DataType") {
            @Override
            protected boolean change(DokpoolMeta dpm, JSONObject meta) {
                dpm.setDataType(meta.getString(fieldname));
                return false;
            }
        },
        new DokpoolMetaModifier("LegalBase") {
            @Override
            protected boolean change(DokpoolMeta dpm, JSONObject meta) {
                dpm.setLegalBase(meta.getString(fieldname));
                return false;
            }
        },
        new DokpoolMetaModifier("MeasuringProgram") {
            @Override
            protected boolean change(DokpoolMeta dpm, JSONObject meta) {
                dpm.setMeasuringProgram(meta.getString(fieldname));
                return false;
            }
        },
        new DokpoolMetaModifier("Status") {
            @Override
            protected boolean change(DokpoolMeta dpm, JSONObject meta) {
                dpm.setStatus(meta.getString(fieldname));
                return false;
            }
        },
        new DokpoolMetaModifier("Purpose") {
            @Override
            protected boolean change(DokpoolMeta dpm, JSONObject meta) {
                dpm.setPurpose(meta.getString(fieldname));
                return false;
            }
        }
    };

    private ReportUtils() {
        // hidden constructor to avoid instantiation.
    }

    /**
     * Create a XMLGregorianCalendar from a GregorianCalendar object.
     *
     * This method is necessary to fulfil the date spec of the
     * IRIX Schema.
     *
     * @param cal The Gregorian calendar.
     * @return An XMLGregorianCalendar with msces set to undefined.
     */
    protected static XMLGregorianCalendar createXMLCalFromGregCal(
        GregorianCalendar cal) {
        cal.setTimeZone(TimeZone.getTimeZone("utc"));
        try {
            XMLGregorianCalendar date = DatatypeFactory.newInstance().
                newXMLGregorianCalendar(cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH),
                                        cal.get(Calendar.HOUR),
                                        cal.get(Calendar.MINUTE),
                                        cal.get(Calendar.SECOND),
                                        DatatypeConstants.FIELD_UNDEFINED,
                                        0);
            return date;
        } catch (DatatypeConfigurationException e) {
            log.error("Exception converting to XMLGregorianCalendar");
            return null;
        }
    }

    /**
     * Helper method to obtain the current datetime as XMLGregorianCalendar.
     *
     * @return a {@link javax.xml.datatype.XMLGregorianCalendar} object.
     */
    public static XMLGregorianCalendar getXMLGregorianNow() {
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date(System.currentTimeMillis()));
        return createXMLCalFromGregCal(c);
    }

    /**
     * Handle the contents of the identifications element.
     *
     * This is currently a basic version that adds a single
     * OrganisationContact element.
     *
     * @param identifications element to which the identifications should
     * be added.
     * @param obj the json object to take the information from.
     *
     * @throws org.json.JSONException if values are missing in {@code obj}.
     */
    public static void addOrganization(IdentificationsType identifications,
        JSONObject obj) throws JSONException {
        OrganisationContactType orgContact = new OrganisationContactType();
        orgContact.setName(obj.getString("Name"));
        orgContact.setOrganisationID(obj.getString("OrganisationID"));
        orgContact.setCountry(obj.getString("Country"));
        identifications.getOrganisationContactInfo().add(orgContact);
    }

    /**
     * Add a username to a report as PersonContactInfo.
     *
     * @param identifications IdentificationsType object to modifiy.
     * @param user username to be added.
     */
    public static void addUser(IdentificationsType identifications,
        String user) {
        PersonContactType person = new PersonContactType();
        person.setName(user);
        identifications.getPersonContactInfo().add(person);
    }

    /**
     * Prepare the IRIX report to take the PDF attachments.
     *
     * The irix information is taken from the Object specified
     * by the IRIX_DATA_KEY name.
     *
     * @param jsonObject The full jsonObject of the request.
     * @return the IRIX report as
     * {@link org.iaea._2012.irix.format.ReportType}.
     * @throws org.json.JSONException in case a key is missing or invalid.
     */
    public static ReportType prepareReport(JSONObject jsonObject)
        throws JSONException {
        ReportType report = new ObjectFactory().createReportType();
        report.setVersion(SCHEMA_VERSION);
        JSONObject irixObj = jsonObject.getJSONObject(IRIX_DATA_KEY);
        JSONObject idObj = irixObj.getJSONObject("Identification");

        // Setup identification
        IdentificationType identification = new IdentificationType();
        identification.setOrganisationReporting(
            idObj.getString("OrganisationReporting"));
        identification.setDateAndTimeOfCreation(getXMLGregorianNow());
        if (idObj.has("SequenceNumber")) {
            identification.setSequenceNumber(
                new BigInteger(idObj.getString("SequenceNumber")));
        }
        identification.setReportUUID(UUID.randomUUID().toString());

        // Setup identifications in identification
        IdentificationsType identifications = new IdentificationsType();
        identification.setIdentifications(identifications);
        addOrganization(identifications,
            idObj.getJSONObject("OrganisationContact"));

        addUser(identifications, irixObj.getString("User"));

        // setPersonContactInfo and organizationcontactinfo ?
        identification.setReportContext(ReportContextType.fromValue(
                idObj.getString("ReportContext")));
        report.setIdentification(identification);
        AnnexesType annex = new AnnexesType();
        report.setAnnexes(annex);

        return report;
    }

    /**
     * Parses an XML Calendar string into an XMLGregorianCalendar object.
     *
     * @param str An  ISO 8601 DateTime like: 2015-05-28T15:35:54.168+02:00
     * @return a {@link javax.xml.datatype.XMLGregorianCalendar} object.
     */
    public static XMLGregorianCalendar xmlCalendarFromString(String str) {
        Calendar cal = DatatypeConverter.parseDateTime(str);
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(cal.getTime());
        return createXMLCalFromGregCal(c);
    }

    /**
     * Add an annotation to the Report containing the DokpoolMeta data fields.
     *
     * @param report The report to add the annoation to.
     * @param jsonObject The full jsonObject of the request.
     * @param schemaFile Optional. Schema to validate against.
     * @throws org.json.JSONException If the JSONObject does not match
     * expectations.
     * @throws org.xml.sax.SAXException In case there is a problem with
     * the schema.
     * @throws javax.xml.bind.JAXBException if validation failed or something
     * else went wrong.
     */
    public static void addAnnotation(JSONObject jsonObject, ReportType report,
            File schemaFile)
        throws JSONException, SAXException, JAXBException {
        JSONObject irixObj = jsonObject.getJSONObject(IRIX_DATA_KEY);

        if (!irixObj.has(DOKPOOL_DATA_KEY)) {
            return;
        }

        // prepare annoation
        AnnotationType annotation = new AnnotationType();
        FreeTextType freeText = new FreeTextType();
        // freeText should probably get some content.
        annotation.setText(freeText);
        annotation.setTitle(irixObj.getString("Title"));

        JSONObject metaObj = irixObj.getJSONObject(DOKPOOL_DATA_KEY);
        DokpoolMeta meta = new DokpoolMeta();

        boolean hasType = false;

        for (DokpoolMetaModifier dpm : DOKPOOL_META_MODIFIERS) {
            hasType |= dpm.modify(meta, metaObj);
        }

        if (!hasType) {
            // Faked JAXBException as we can't write these restrictions in
            // Schema 1.0
            throw new JAXBException("At least one of the fields, IsElan, "
                + "IsDoksys, IsRodos, IsRei needs to be true");
        }
        if (meta.isIsDoksys() != null && meta.isIsDoksys().booleanValue()
            && (meta.getNetworkOperator() == null
                || meta.getNetworkOperator().isEmpty())) {
            throw new JAXBException(
                "Doksys documents need to have a Network Operator set.");
        }

        // Handle the datetime values
        meta.setSamplingBegin(
            xmlCalendarFromString(metaObj.getString("SamplingBegin")));
        meta.setSamplingEnd(
            xmlCalendarFromString(metaObj.getString("SamplingEnd")));

        DOMResult res = new DOMResult();
        Element ele = null;
        JAXBContext jaxbContext = JAXBContext.newInstance(DokpoolMeta.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        if (schemaFile != null) {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(
                    XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaFile);
            jaxbMarshaller.setSchema(schema);
        }
        jaxbMarshaller.marshal(meta, res);
        ele = ((Document) res.getNode()).getDocumentElement();

        annotation.getAny().add(ele);
        report.getAnnexes().getAnnotation().add(annotation);
    }

    /**
     * Attach a file as Annex on a ReportType object.
     *
     * @param title The title of the FileEnclosure.
     * @param data Binary content of the file.
     * @param report Report to attach the file to.
     * @param mimeType The mime type of that file.
     * @param fileName The filename to set.
     */
    public static void attachFile(
        String title,
        byte[] data,
        ReportType report,
        String mimeType,
        String fileName
    ) {
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

    /**
     * Validate and Marshall a report object for an output stream.
     *
     * @param report The report to marshall.
     * @param out The output stream.
     * @param irixSchema The schema to validate against. Or null.
     * @throws javax.xml.bind.JAXBException if an error was
     * encountered while creating the JAXBContext
     * @throws org.xml.sax.SAXException in case of errors during
     * parsing of the schema.
     */
    public static void marshallReport(ReportType report, OutputStream out,
        File irixSchema)
        throws JAXBException, SAXException {
        JAXBContext jaxbContext = JAXBContext.newInstance(
                ReportType.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

        if (irixSchema != null) {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(
                    XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(irixSchema);
            jaxbMarshaller.setSchema(schema);
        }

        jaxbMarshaller.marshal(new ObjectFactory().createReport(report), out);
    }
};
