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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
//import java.math.BigInteger;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.ArrayList;
import java.math.BigDecimal;
//import java.math.BigInteger;
import java.math.BigInteger;
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
        "DokpoolDocumentOwner",
        "Subjects",
        "IsElan",
        "IsDoksys",
        "IsRodos",
        "IsRei"
    };

    private static final String[] DOKSYS_FIELDS = new String[] {
        "Purpose",
        "NetworkOperator",
        "SampleTypeId",
        "SampleType",
        "Dom",
        "DataType",
        "LegalBase",
        "MeasuringProgram",
        "Status",
        "SamplingBegin",
        "SamplingEnd",
        "TrajectoryStartLocation",
        "TrajectoryEndLocation",
        "TrajectoryStartTime",
        "TrajectoryEndTime"
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
        "Type",
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

    private static final String[] REI_FIELDS = new String[] {
        "Revision", // numeric
        "Year", // (Mitte Sammelzeitraum): z.B. „2009“
        "Period", // e.g. Q3 for third Quarter
        "NuclearInstallation", // z.B. „KKW Grafenrheinfeld“
        "Medium", //"Abwasser", "Fortluft" oder "Abwasser/Fortluft",
        "ReiLegalBases", // REI-E, REI-I oder REI-E/REI-I
        "Origin", // "Genehmigungsinhaber"
        "MStIDs", // "1234", "ABCD"
        "Authority", // z.B. „Bayern“
        "PDFVersion",  // PDF/A-1b
        "SigningDate",
        "SigningComment",
        "Signed" // Bool
};

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
        JSONObject userJsonObject = new JSONObject();
        addAnnotation(jsonObject, report, schemaFile, userJsonObject);
    }

    /**
     * Add an annotation to the Report containing the DokpoolMeta data fields.
     *
     * @param report The report to add the annoation to.
     * @param jsonObject The full jsonObject of the request.
     * @param schemaFile Optional. Schema to validate against.
     * @param userJsonObject The userJsonObject created from headers.
     * @throws JSONException If the JSONObject does not match
     * expectations.
     * @throws SAXException In case there is a problem with
     * the schema.
     * @throws JAXBException if validation failed or something
     * else went wrong.
     */
    public static void addAnnotation(JSONObject jsonObject, ReportType report,
            File schemaFile, JSONObject userJsonObject)
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
            if (field.equals("DokpoolDocumentOwner") && userJsonObject
                    .has("uid")) {
                String uid = userJsonObject.getString("uid");
                if (uid.length() > 0) {
                    value = uid;
                    log.info("Using DokpoolDocumentOwner from Header "
                            + "instead of request");
                }
            }
            try {
                if (field.startsWith("Is")) {
                    Method method = meta.getClass().getMethod(methodName,
                            Boolean.class);
                    boolean bValue = value.toLowerCase().equals("true");
                    method.invoke(meta, bValue);
                    hasType = bValue || hasType;
                //FIXME add Subjects handling
                } else if (field.equals("Subjects")) {
                    if (metaObj.has("Subjects")) {
                        JSONArray dpSubjectsJson
                                = metaObj.getJSONArray("Subjects");
                        DokpoolMeta.Subjects subjects
                                = new DokpoolMeta.Subjects();
                        List<String> subjectsList = subjects.getSubject();
                        for (int i = 0; i < dpSubjectsJson.length(); i++) {
                            subjectsList.add(dpSubjectsJson.getString(i));
                        }
                        meta.setSubjects(subjects);
                    }
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
        if (metaObj.has("Doksys") && meta.isIsDoksys()) {
            addDoksysMeta(metaObj, meta);
        }
        if (metaObj.has("Elan") && meta.isIsElan()) {
            addElanMeta(metaObj, meta);
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
        JSONObject doksysMetaObj = metaObj.getJSONObject("Doksys");
        List<String> dateParams = Arrays.asList(
                "SamplingBegin",
                "SamplingEnd",
                "TrajectoryStartTime",
                "TrajectoryEndTime"
        );
        for (String field: DOKSYS_FIELDS) {
            if (!doksysMetaObj.has(field)) {
                continue;
            }
            String methodName = "set" + field;
            try {
                if (dateParams.contains(field)) {
                    String value = doksysMetaObj
                            .get(field).toString();
                    XMLGregorianCalendar calval;
                    calval = ReportUtils.xmlCalendarFromString(value);
                    Method method = doksys.getClass().getMethod(
                            methodName,
                            XMLGregorianCalendar.class
                    );
                    method.invoke(doksys, calval);
                } else {
                    String value = doksysMetaObj.get(field).toString();
                    Method method = doksys.getClass().getMethod(
                            methodName,
                            String.class
                    );
                    method.invoke(doksys, value);
                }
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
        if (metaObj.getJSONObject("Elan").has("Scenarios")) {
            //ELAN.Scenarios elanscenarios = elan.getScenarios();
            ELAN.Scenarios elanscenarios = new ELAN.Scenarios();
            JSONArray elanScenariosMetaJson = metaObj.getJSONObject("Elan")
                    .getJSONArray("Scenarios");
            List<String> elanscenarioList = elanscenarios.getScenario();
            for (int i = 0; i < elanScenariosMetaJson.length(); i++) {
                elanscenarioList.add(elanScenariosMetaJson.getString(i));
            }
            elan.setScenarios(elanscenarios);
            meta.setELAN(elan);
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
                } else if (field.equals("Sourceterms")
                        || field.equals("Sourceterm")) {
                    addRodosSourceterms(rodosMetaObj, rodos, field);
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
        List<String> dateParams = Arrays.asList("SigningDate");
        List<String> boolParams = Arrays.asList("Signed");
        List<String> numParams = Arrays.asList("Year", "Revision");
        List<String> listParams = Arrays.asList("ReiLegalBases", "MStIDs");
        for (String field: REI_FIELDS) {
            if (!reiMetaObj.has(field)) {
                continue;
            }
            String methodName = "set" + field;
            try {

                if (dateParams.contains(field)) {
                    String value = reiMetaObj
                            .get(field).toString();
                    XMLGregorianCalendar calval;
                    calval = ReportUtils.xmlCalendarFromString(value);
                    Method method = rei.getClass().getMethod(
                            methodName,
                            XMLGregorianCalendar.class
                    );
                    method.invoke(rei, calval);
                } else if (boolParams.contains(field)) {
                    Boolean bval = reiMetaObj.getBoolean(field);
                    Method bMethod = REI.class.getMethod(
                            methodName,
                            Boolean.class
                    );
                    bMethod.invoke(rei, bval);
                } else if (numParams.contains(field)) {
                    BigInteger numval = BigInteger.valueOf(
                            reiMetaObj.getInt(field)
                    );
                    Method numMethod = REI.class.getMethod(
                            methodName,
                            BigInteger.class
                    );
                    numMethod.invoke(rei, numval);
                } else if (listParams.contains(field)) {
                    addReiListParam(reiMetaObj, rei, field);
                } else {
                    String value = reiMetaObj
                            .getString(field);
                    Method aMethod = REI.class
                            .getMethod(
                                    methodName,
                                    String.class
                            );
                    aMethod.invoke(rei, value);
                }
            } catch (Exception e) {
                log.error(e.getClass().getName()
                        + " exception while trying to access " + methodName
                        + " on DokpoolReiMeta object.");
            }
        }
        meta.setREI(rei);
    }

    /**
     * Add an list params to the Report containing the
     * ReiDokpoolMeta data fields.
     *
     * @param rei The rei part of the report to add the listParam meta to.
     * @param reiMetaObj The REI metaJsonObject of the request.
     * @param field The key holding the list
     */
    public static void addReiListParam(
            JSONObject reiMetaObj,
            REI rei,
            String field) {
        if (field.equals("ReiLegalBases")) {
            if (reiMetaObj.has("ReiLegalBases")) {
                REI.ReiLegalBases reilegalbases = new REI.ReiLegalBases();
                JSONArray reiLegalBasesMetaJson = reiMetaObj
                        .getJSONArray(field);
                List<String> reilegalbaseList = reilegalbases
                        .getReiLegalBase();
                for (int i = 0; i < reiLegalBasesMetaJson.length(); i++) {
                    reilegalbaseList.add(reiLegalBasesMetaJson.getString(i));
                }
                rei.setReiLegalBases(reilegalbases);
            }
        } else if (field.equals("MStIDs")) {
            if (reiMetaObj.has("MStIDs")) {
                REI.MStIDs reimstids = new REI.MStIDs();
                List<REI.MStIDs.MSt> reimstList = reimstids.getMSt();
                JSONArray reiMstidsMetaJson = reiMetaObj.getJSONArray(field);
                for (int i = 0; i < reiMstidsMetaJson.length(); i++) {
                    REI.MStIDs.MSt reimst = new REI.MStIDs.MSt();
                    JSONObject reiMStJson = reiMstidsMetaJson.getJSONObject(i);
                    if (reiMStJson.has("MStID")) {
                        reimst.setMStID(reiMStJson.getString("MStID"));
                    } else {
                        continue;
                    }
                    if (reiMStJson.has("MStName")) {
                        reimst.setMStName(reiMStJson.getString("MStName"));
                    } else {
                        reimst.setMStName(null);
                    }
                    reimstList.add(reimst);
                }
                rei.setMStIDs(reimstids);
            }
        }
    }

    /**
     * Add an annotation to the Report containing the
     * RodosDokpoolMeta data fields.
     *
     * @param rodos The rodos part of the report to add the Sourceterm meta to.
     * @param rodosMetaObj The Rodos metaJsonObject of the request.
     * @param field The key holding the Sourceterm(s)
     */
    public static void addRodosSourceterms(
            JSONObject rodosMetaObj,
            RODOS rodos,
            String field) {
        RODOS.Sourceterms sourceterms = new RODOS.Sourceterms();
        List<RODOS.Sourceterms.Sourceterm> sourcetermList =
                sourceterms.getSourceterm();
        JSONArray rodosSourcetermsMetaObj = rodosMetaObj
                .getJSONArray(field);
        List<String> dateParams = Arrays.asList("StartRelease", "EndRelease");
        List<String> complexParams = Arrays.asList("Block", "Activity");
        for (int i = 0; i < rodosSourcetermsMetaObj.length(); i++) {
            JSONObject rodosSourcetermMetaObj =
                    rodosSourcetermsMetaObj.getJSONObject(i);
            RODOS.Sourceterms.Sourceterm sourceterm
                    = new RODOS.Sourceterms.Sourceterm();
            for (String rsfield: RODOS_SOURCETERM_FIELDS) {
                if (!rodosSourcetermMetaObj.has(rsfield)) {
                    continue;
                }
                String methodName = "set" + rsfield;
                try {
                    if (dateParams.contains(rsfield)) {
                        String value = rodosSourcetermMetaObj
                                .get(rsfield).toString();
                        XMLGregorianCalendar calval;
                        calval = ReportUtils.xmlCalendarFromString(value);
                        Method method = sourceterm.getClass().getMethod(
                                methodName,
                                XMLGregorianCalendar.class
                        );
                        method.invoke(sourceterm, calval);
                    } else if (complexParams.contains(rsfield)) {
                        if (rsfield.equals("Activity")) {
                            RODOS.Sourceterms.Sourceterm.Activity activity
                                    = new RODOS.Sourceterms.Sourceterm
                                    .Activity();
                            JSONObject rodosSourcetermActivityMetaObj
                                    = rodosSourcetermMetaObj
                                    .getJSONObject(rsfield);
                            for (Field afield: RODOS.Sourceterms
                                    .Sourceterm.Activity.class
                                    .getDeclaredFields()) {
                                String aFieldName = afield.getName()
                                        .substring(0, 1).toUpperCase()
                                        + afield.getName().substring(1);
                                String aMethodName
                                        = "set" + aFieldName;
                                Float numval = BigDecimal.valueOf(
                                        rodosSourcetermActivityMetaObj
                                        .getDouble(aFieldName))
                                        .floatValue();
                                Method aMethod = RODOS.Sourceterms
                                        .Sourceterm.Activity.class
                                        .getMethod(
                                                aMethodName,
                                        float.class
                                );
                                aMethod.invoke(activity, numval);
                            }
                            sourceterm.setActivity(activity);
                        }
                        if (rsfield.equals("Block")) {
                            RODOS.Sourceterms.Sourceterm.Block block
                                    = new RODOS.Sourceterms.Sourceterm.Block();
                            JSONObject rodosSourcetermBlockMetaObj
                                    = rodosSourcetermMetaObj
                                    .getJSONObject(rsfield);
                            for (Field afield: RODOS.Sourceterms
                                    .Sourceterm.Block.class
                                    .getDeclaredFields()) {
                                String aFieldName = afield.getName()
                                        .substring(0, 1).toUpperCase()
                                        + afield.getName().substring(1);
                                String aMethodName
                                        = "set" + aFieldName;
                                if (afield.getType() == String.class) {
                                    String value = rodosSourcetermBlockMetaObj
                                                    .getString(aFieldName);
                                    Method aMethod = RODOS.Sourceterms
                                            .Sourceterm.Block.class
                                            .getMethod(
                                                    aMethodName,
                                                    String.class
                                            );
                                    aMethod.invoke(block, value);
                                } else {
                                    Float numval = BigDecimal.valueOf(
                                            rodosSourcetermBlockMetaObj
                                                    .getDouble(aFieldName))
                                            .floatValue();
                                    Method aMethod = RODOS.Sourceterms
                                            .Sourceterm.Block.class
                                            .getMethod(
                                                    aMethodName,
                                                    float.class
                                            );
                                    aMethod.invoke(block, numval);
                                }
                            }
                            sourceterm.setBlock(block);
                        }
                    } else {
                        String value =
                                rodosSourcetermMetaObj.get(rsfield).toString();
                        Method method = sourceterm.getClass().getMethod(
                                methodName,
                                String.class
                                );
                        method.invoke(sourceterm, value);
                    }
                } catch (Exception e) {
                    log.error(e.getClass().getName()
                            + " exception while trying to access " + methodName
                            + " on DokpoolRodosSourcetermMeta object.");
                }
            }
            sourcetermList.add(sourceterm);
        }
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
