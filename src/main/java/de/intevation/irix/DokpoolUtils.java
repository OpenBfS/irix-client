/* Copyright (C) 2015 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE.txt for details.
 */

package de.intevation.irix;

import de.bfs.irix.extensions.dokpool.DokpoolMeta;
import de.bfs.irix.extensions.dokpool.DokpoolMeta.ELAN;
import de.bfs.irix.extensions.dokpool.DokpoolMeta.RODOS;
import de.bfs.irix.extensions.dokpool.DokpoolMeta.DOKSYS;
import de.bfs.irix.extensions.dokpool.DokpoolMeta.REI;
import org.apache.log4j.Logger;
//import org.iaea._2012.irix.format.ObjectFactory;
import org.iaea._2012.irix.format.ReportType;
//import org.iaea._2012.irix.format.annexes.AnnexesType;
import org.iaea._2012.irix.format.annexes.AnnotationType;
//import org.iaea._2012.irix.format.annexes.FileEnclosureType;
//import org.iaea._2012.irix.format.annexes.FileHashType;
import org.iaea._2012.irix.format.base.FreeTextType;
/*import org.iaea._2012.irix.format.base.OrganisationContactType;
import org.iaea._2012.irix.format.base.PersonContactType;
import org.iaea._2012.irix.format.base.YesNoType;
import org.iaea._2012.irix.format.eventinformation.DateAndTimeOfEventType;
import org.iaea._2012.irix.format.eventinformation.EventInformationType;
import org.iaea._2012.irix.format.eventinformation.TypeOfEventType;
import org.iaea._2012.irix.format.identification.*;
import org.iaea._2012.irix.format.locations.LocationOrLocationRefType;*/
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
//import javax.xml.datatype.DatatypeConfigurationException;
//import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
//import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMResult;
//import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
//import java.io.OutputStream;
import java.lang.reflect.Method;
//import java.math.BigInteger;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Static helper methods to work with an IRIX Report.
 *
 * This class provides helper methods to work with the
 * IRIX document scheme and the Dokpool extension.
 * The helper methods are directly tied to the IRIXClient JSON input format.
 *
 */
public final class DokpoolUtils {
    private static Logger log = Logger.getLogger(DokpoolUtils.class);

    /** The name of the json object containing the irix information. */
    private static final String IRIX_DATA_KEY = "irix";
    /** The name of the json object containing the DokpoolMeta information. */
    private static final String DOKPOOL_DATA_KEY = "DokpoolMeta";
    /** This is according to current documentation the fixed value.*/
    private static final String SCHEMA_VERSION = "1.0";
    /** The fields to set in the DokpoolMeta object.
     *
     * SamplingBegin and SamplingEnd are handled wether or not they
     * are part of this list.
     **/
    private static final String[] DOKPOOL_FIELDS = new String[] {
        "DokpoolContentType",
        "DokpoolName",
        "DokpoolGroupFolder",
        "DokpoolPrivateFolder",
        "DokpoolTransferFolder",
        "IsElan",
        "IsDoksys",
        "IsRodos",
        "IsRei"
    };

    private static final String[] DOKSYS_FIELDS = new String[] {
        "NetworkOperator",
        "SampleTypeId",
        "SampleType",
        "Dom",
        "DataType",
        "LegalBase",
        "MeasuringProgram",
        "Status",
        "Purpose"
    };

    private static final String[] RODOS_FIELDS = new String[] {
        "ProjectComment",
        "Site",
        "ReportRunID",
        "Model",
        "CalculationId",
        "CalculationDate",
        "CalculationUser",
        "Projectname",
        "ProjectChain",
        "WorkingMode",
        "Sourceterms",
        "Prognosis"
    };

    private static final String[] RODOS_PROGNOSIS_FIELDS = new String[]{
        "Provider",
        "Meteo",
        "End",
        "Date",
        "Begin",
        "Dates"
    };

    private static final String[] RODOS_SOURCETERM_FIELDS = new String[]{
        "StartRelease",
        "EndRelease",
        "Label",
        "Type",
        "Activity",
        "Block",
        "Comment"
    };

    private static final String[] REI_FIELDS = new String[] {};



    // removed Doksys specific fields
    /*
        "NetworkOperator",
        "SampleTypeId",
        "SampleType",
        "Dom",
        "DataType",
        "LegalBase",
        "MeasuringProgram",
        "Status",
        "Purpose"
     */

    private DokpoolUtils() {
        // hidden constructor to avoid instantiation.
    }

    /**
     * Add an annotation to the Report containing the DokpoolMeta data fields.
     *
     * @param report The report to add the annoation to.
     * @param jsonObject The full jsonObject of the request.
     * @param schemaFile Optional. Schema to validate against.
     * @throws JSONException If the JSONObject does not match
     * expectations.
     * @throws SAXException In case there is a problem with
     * the schema.
     * @throws JAXBException if validation failed or something
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
        if (irixObj.has("Text")) {
            freeText.getContent().add(irixObj.getString("Text"));
        }
        annotation.setText(freeText);
        annotation.setTitle(irixObj.getString("Title"));

        // FIXME DokpoolMeta in separate function!
        JSONObject metaObj = irixObj.getJSONObject(DOKPOOL_DATA_KEY);
        DokpoolMeta meta = new DokpoolMeta();
        boolean hasType = false;
        for (String field: DOKPOOL_FIELDS) {
            if (!metaObj.has(field)) {
                continue;
            }
            String methodName = "set" + field;
            String value = metaObj.get(field).toString();
            try {
                if (field.startsWith("Is")) {
                    Method method = meta.getClass().getMethod(methodName,
                        Boolean.class);
                    boolean bValue = value.toLowerCase().equals("true");
                    method.invoke(meta, bValue);
                    hasType = bValue || hasType;
                } else {
                    Method method = meta.getClass().getMethod(methodName,
                        String.class);
                    method.invoke(meta, value);
                }
            } catch (Exception e) {
                log.error(e.getClass().getName()
                    + " exception while trying to access " + methodName
                    + " on DokpoolMeta object.");
            }
        }
        if (!hasType) {
            // Faked JAXBException as we can't write these restrictions in
            // Schema 1.0
            throw new JAXBException("At least one of the fields, IsElan, "
                + "IsDoksys, IsRodos, IsRei needs to be true");
        }
        // FIXME process DOKSYS, ELAN, RODOS and REI sequentially
        if (meta.isIsDoksys() != null && meta.isIsDoksys().booleanValue()
            && (meta.getDOKSYS() == null
                || meta.getDOKSYS().getNetworkOperator().isEmpty())) {
            try {
                meta.getDOKSYS().setNetworkOperator(metaObj
                        .getJSONObject("Doksys").getString("NetworkOperator"));
            } catch (Exception e) {
                throw new JAXBException(
                        "Doksys documents need to have a Network Operator set."
                );
            }
        }
        if (meta.isIsDoksys() != null && meta.isIsDoksys().booleanValue()) {
            // Handle the datetime values
            meta.getDOKSYS().setSamplingBegin(
                    ReportUtils.xmlCalendarFromString(metaObj
                            .getString("SamplingBegin")));
            meta.getDOKSYS().setSamplingEnd(
                    ReportUtils.xmlCalendarFromString(metaObj
                            .getString("SamplingEnd")));
        }
        if (metaObj.has("Elan") && meta.isIsElan()) {
            addElanMeta(metaObj, meta);

/*
            ELAN elan = new ELAN();
            if (metaObj.getJSONObject("Elan").has("ElanScenarios")) {
                ELAN.ElanScenarios elanscenarios = new ELAN.ElanScenarios();
                JSONObject rbjson = metaObj.getJSONObject("Elan")
                    .getJSONObject("ElanScenarios");
                if (rbjson.has("ElanScenario")) {
                    List<String> elanscenario = elanscenarios.getElanScenario();
                    if (rbjson.get("ElanScenario") instanceof JSONArray) {
                        JSONArray rbsisjson = rbjson
                                .getJSONArray("ElanScenario");

                        //List<String> rblist = new ArrayList<String>();
                        for (int i = 0; i < rbsisjson.length(); i++) {
                            elanscenario.add(rbsisjson.getString(i));
                        }
                        //elanscenarios.getElanScenario().add();
                    } else if (rbjson.get("ElanScenario") instanceof String) {
                        String rbstring = rbjson.getString("ElanScenario");
                        elanscenario.add(rbstring);
                        //setElanScenarios(elanscenarios, rbstring);
                    }
                    elan.setElanScenarios(elanscenarios);
                    meta.setELAN(elan);
                }
            }
*/

        }
        if (metaObj.has("Rodos") && meta.isIsRodos()) {
            addRodosMeta(metaObj, meta);
        }
        if (metaObj.has("Rei") && meta.isIsRei()) {
            addReiMeta(metaObj, meta);
        }

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
     * Add an annotation to the Report containing the
     * DoksysDokpoolMeta data fields.
     *
     * @param meta The meta part of the report to add the Doksys meta to.
     * @param metaObj The full metaJsonObject of the request.
     */
    public static void addDoksysMeta(JSONObject metaObj, DokpoolMeta meta) {
        DOKSYS doksys = new DOKSYS();
        JSONObject rodosMetaObj = metaObj.getJSONObject("Doksys");
        for (String field: DOKSYS_FIELDS) {
            if (!rodosMetaObj.has(field)) {
                continue;
            }
            String methodName = "set" + field;
            try {
                String value = rodosMetaObj.get(field).toString();
                Method method = doksys.getClass().getMethod(
                        methodName,
                        String.class
                );
                method.invoke(doksys, value);
            } catch (Exception e) {
                log.error(e.getClass().getName()
                        + " exception while trying to access " + methodName
                        + " on DokpoolDoksysMeta object.");
            }
        }
        meta.setDOKSYS(doksys);
    }

    /**
     * Add an annotation to the Report containing the
     * ElanDokpoolMeta data fields.
     *
     * @param meta The meta part of the report to add the Elan meta to.
     * @param metaObj The full metaJsonObject of the request.
     */
    public static void addElanMeta(JSONObject metaObj, DokpoolMeta meta) {
        ELAN elan = new ELAN();
        if (metaObj.getJSONObject("Elan").has("ElanScenarios")) {
            ELAN.ElanScenarios elanscenarios = new ELAN.ElanScenarios();
            JSONObject rbjson = metaObj.getJSONObject("Elan")
                    .getJSONObject("ElanScenarios");
            if (rbjson.has("ElanScenario")) {
                List<String> elanscenario = elanscenarios.getElanScenario();
                if (rbjson.get("ElanScenario") instanceof JSONArray) {
                    JSONArray rbsisjson = rbjson
                            .getJSONArray("ElanScenario");

                    //List<String> rblist = new ArrayList<String>();
                    for (int i = 0; i < rbsisjson.length(); i++) {
                        elanscenario.add(rbsisjson.getString(i));
                    }
                    //elanscenarios.getElanScenario().add();
                } else if (rbjson.get("ElanScenario") instanceof String) {
                    String rbstring = rbjson.getString("ElanScenario");
                    elanscenario.add(rbstring);
                    //setElanScenarios(elanscenarios, rbstring);
                }
                elan.setElanScenarios(elanscenarios);
                meta.setELAN(elan);
            }
        }
    }

    /**
     * Add an annotation to the Report containing the
     * RodosDokpoolMeta data fields.
     * FIXME add sourceterm and prognosis
     * @param meta The meta part of the report to add the Rodos meta to.
     * @param metaObj The full metaJsonObject of the request.
     */
    public static void addRodosMeta(JSONObject metaObj, DokpoolMeta meta) {
        RODOS rodos = new RODOS();
        JSONObject rodosMetaObj = metaObj.getJSONObject("Rodos");
        for (String field: RODOS_FIELDS) {
            if (!rodosMetaObj.has(field)) {
                continue;
            }
            String methodName = "set" + field;
            try {
                if (field.equals("CalculationDate")) {
                    String value = rodosMetaObj.get(field).toString();
                    XMLGregorianCalendar calval;
                    calval = ReportUtils.xmlCalendarFromString(value);
                    Method method = rodos.getClass().getMethod(
                            methodName,
                            XMLGregorianCalendar.class
                    );
                    method.invoke(rodos, calval);
                } else if (field.equals("Sourceterms")) {
                    addRodosSourceterms(rodosMetaObj, rodos);
                } else if (field.equals("Prognosis")) {
                    addRodosPrognosis(rodosMetaObj, rodos);
                } else {
                    String value = rodosMetaObj.get(field).toString();
                    Method method = rodos.getClass().getMethod(
                            methodName,
                            String.class
                    );
                    method.invoke(rodos, value);
                }
            } catch (Exception e) {
                log.error(e.getClass().getName()
                        + " exception while trying to access " + methodName
                        + " on DokpoolRodosMeta object.");
            }
        }
        meta.setRODOS(rodos);
    }

    /**
     * Add an annotation to the Report containing the
     * ReiDokpoolMeta data fields.
     *
     * @param meta The meta part of the report to add the Rei meta to.
     * @param metaObj The full metaJsonObject of the request.
     */
    public static void addReiMeta(JSONObject metaObj, DokpoolMeta meta) {
        REI rei = new REI();
        JSONObject reiMetaObj = metaObj.getJSONObject("Rei");
        for (String field: REI_FIELDS) {
            if (!reiMetaObj.has(field)) {
                continue;
            }
            String methodName = "set" + field;
            try {
                String value = reiMetaObj.get(field).toString();
                Method method = rei.getClass().getMethod(
                        methodName,
                        String.class
                );
                method.invoke(rei, value);
            } catch (Exception e) {
                log.error(e.getClass().getName()
                        + " exception while trying to access " + methodName
                        + " on DokpoolReiMeta object.");
            }
        }
        meta.setREI(rei);
    }

    /**
     * Add an annotation to the Report containing the
     * RodosDokpoolMeta data fields.
     *
     * @param rodos The rodos part of the report to add the Sourceterm meta to.
     * @param rodosMetaObj The Rodos metaJsonObject of the request.
     */
    public static void addRodosSourceterms(
            JSONObject rodosMetaObj,
            RODOS rodos) {
        RODOS.Sourceterms sourceterms = new RODOS.Sourceterms();

        JSONArray rodosSourcetermsMetaObj = rodosMetaObj
                .getJSONArray("Sourceterms");

        for (int i = 0; i < rodosSourcetermsMetaObj.length(); i++) {
            RODOS.Sourceterms.Sourceterm sourceterm =
                    new RODOS.Sourceterms.Sourceterm();
            JSONArray rodosSourcetermMetaObj = rodosSourcetermsMetaObj;
            /*for (String field: RODOS_SOURCETERM_FIELDS) {
                if (!rodosSourcetermsMetaObj.has(field)) {
                    continue;
                }
                String methodName = "set" + field;
                try {
                    String value = rodosSourcetermMetaObj.get(field).toString();
                    Method method = rodosSourcetermMetaObj.getClass().getMethod(
                            methodName,
                            String.class
                    );
                    method.invoke(sourceterm, value);
                } catch (Exception e) {
                    log.error(e.getClass().getName()
                            + " exception while trying to access " + methodName
                            + " on DokpoolRodosSourcetermMeta object.");
                }
            }*/
        }

        RODOS.Sourceterms.Sourceterm sourceterm =
                new RODOS.Sourceterms.Sourceterm();

        rodos.setSourceterms(sourceterms);

    }

    /**
     * Add an annotation to the Report containing the
     * RodosDokpoolMeta data fields.
     *
     * @param rodos The rodos part of the report to add the Sourceterm meta to.
     * @param rodosMetaObj The Rodos metaJsonObject of the request.
     */
    public static void addRodosPrognosis(
            JSONObject rodosMetaObj,
            RODOS rodos) {
        RODOS.Prognosis prognosis = new RODOS.Prognosis();
        JSONObject rodosPrognosisMetaObj =
                rodosMetaObj.getJSONObject("Prognosis");
        List<String> dateParams = Arrays.asList("Begin", "End", "Date");
        for (String field: RODOS_PROGNOSIS_FIELDS) {
            if (!rodosPrognosisMetaObj.has(field)) {
                continue;
            }
            String methodName = "set" + field;
            try {
                if (dateParams.contains(field)) {
                    String value = rodosPrognosisMetaObj.get(field).toString();
                    XMLGregorianCalendar calval;
                    calval = ReportUtils.xmlCalendarFromString(value);
                    Method method = prognosis.getClass().getMethod(
                            methodName,
                            XMLGregorianCalendar.class
                    );
                    method.invoke(prognosis, calval);
                } else if (field.equals("Dates")) {
                    RODOS.Prognosis.Dates dates = new RODOS.Prognosis.Dates();
                    JSONArray datesMetaObj = rodosPrognosisMetaObj
                            .getJSONArray(field);
                    List<XMLGregorianCalendar> datesList = dates.getDate();
                    for (int i = 0; i < datesMetaObj.length(); i++) {
                        String value = datesMetaObj.get(i).toString();
                        XMLGregorianCalendar calval;
                        calval = ReportUtils.xmlCalendarFromString(value);
                        datesList.add(calval);
                    }
                    Method method = prognosis.getClass().getMethod(
                            methodName,
                            RODOS.Prognosis.Dates.class
                    );
                    method.invoke(prognosis, dates);
                } else {
                    String value = rodosPrognosisMetaObj.get(field).toString();
                    Method method = prognosis.getClass().getMethod(
                            methodName,
                            String.class
                    );
                    method.invoke(prognosis, value);
                }
            } catch (Exception e) {
                log.error(e.getClass().getName()
                        + " exception while trying to access " + methodName
                        + " on DokpoolRodosPrognosisMeta object.");
            }
        }
        rodos.setPrognosis(prognosis);
    }

}
