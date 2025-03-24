/* Copyright (C) 2015-2025 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE for details.
 */

package de.intevation.irix;

import de.bfs.irix.extensions.dokpool.DokpoolMeta;
import de.bfs.irix.extensions.dokpool.DokpoolMeta.ELAN;
import de.bfs.irix.extensions.dokpool.DokpoolMeta.RODOS;
import de.bfs.irix.extensions.dokpool.DokpoolMeta.DOKSYS;
import de.bfs.irix.extensions.dokpool.DokpoolMeta.REI;
import org.iaea._2012.irix.format.ReportType;
import org.iaea._2012.irix.format.annexes.AnnotationType;
import org.iaea._2012.irix.format.base.FreeTextType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.dom.DOMResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;
import static java.lang.System.Logger.Level.INFO;

/**
 * Static helper methods to work with an IRIX Report.
 *
 * This class provides helper methods to work with the
 * IRIX document scheme and the Dokpool extension.
 * The helper methods are directly tied to the IRIXClient JSON input format.
 *
 */
public final class DokpoolUtils {
    private static System.Logger log = System.getLogger(DokpoolUtils.class.getName());

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
        "Subject",
        "IsElan",
        "IsDoksys",
        "IsRodos",
        "IsRei"
    };

    private static final String[] DOKSYS_FIELDS = new String[] {
        "Purpose",
        "NetworkOperator",
        "SampleType",
        "MeasurementCategory",
        "Dom",
        "DataSource",
        "LegalBase",
        "SamplingBegin",
        "SamplingEnd",
        "Duration",
        "OperationMode",
        "TrajectoryStartLocation",
        "TrajectoryEndLocation",
        "TrajectoryStartTime",
        "TrajectoryEndTime",
        "MeasuringProgram",
        "Status"
    };

    private static final String[] REI_FIELDS = new String[] {
            "Revision", // numeric
            "Year", // (Mitte Sammelzeitraum): z.B. „2009“
            "Period", // e.g. Q3 for third Quarter
            "NuclearInstallation", // z.B. „KKW Grafenrheinfeld“
            "Medium", //"Abwasser", "Fortluft" oder "Abwasser/Fortluft",
            "ReiLegalBase", // REI-E, REI-I oder REI-E/REI-I
            "Origin", // "Genehmigungsinhaber"
            "MSt", // "1234", "ABCD"
            "Authority", // z.B. „Bayern“
            "PDFVersion",  // PDF/A-1b
            "SigningDate",
            "SigningComment",
            "Signed" // Bool
};
    private DOKSYS methodClass;

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
                    log.log(INFO, "Using DokpoolDocumentOwner from Header "
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
                } else if (field.equals("Subject")) {
                    if (metaObj.has("Subject")) {
                        JSONArray dpSubjectsJson = metaObj.getJSONArray("Subject");
                        for (int i = 0; i < dpSubjectsJson.length(); i++) {
                            meta.getSubject().add(dpSubjectsJson.getString(i));
                        }
                    }
                } else {
                    Method method = meta.getClass().getMethod(methodName, String.class);
                    method.invoke(meta, value);
                }
            } catch (Exception e) {
                log.log(ERROR, e.getClass().getName()
                    + " exception while trying to access " + methodName
                    + " on DokpoolMeta object.");
            }
        }
        if ((metaObj.has("Doksys") || metaObj.has("DOKSYS"))
                && meta.isIsDoksys()) {
            addDoksysMeta(metaObj, meta);
        }
        if ((metaObj.has("Elan") || metaObj.has("ELAN"))
                && meta.isIsElan()) {
            addElanMeta(metaObj, meta);
        }
        if ((metaObj.has("Rodos") || metaObj.has("RODOS"))
                && meta.isIsRodos()) {
            addRodosMeta(metaObj, meta);
        }
        if ((metaObj.has("Rei") || metaObj.has("REI"))
                && meta.isIsRei()) {
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
        JSONObject doksysMetaObj;
        if (metaObj.has("Doksys")) {
            doksysMetaObj = metaObj.getJSONObject("Doksys");
        } else if (metaObj.has("DOKSYS")) {
            doksysMetaObj = metaObj.getJSONObject("DOKSYS");
        } else {
            return;
        }
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
            String setMethodName = "set" + field;
            String getMethodName = "get" + field;
            Method[] methods = doksys.getClass().getDeclaredMethods();
            try {
                if (dateParams.contains(field)) {
                    String value = doksysMetaObj
                            .get(field).toString();
                    XMLGregorianCalendar calval;
                    calval = ReportUtils.xmlCalendarFromString(value);
                    Method method = doksys.getClass().getMethod(
                            setMethodName,
                            XMLGregorianCalendar.class
                    );
                    method.invoke(doksys, calval);
                } else {
                    boolean hasGetMethod = false;
                    boolean hasSetMethod = false;
                    if (doksysMetaObj.get(field) instanceof String) {
                        String value = doksysMetaObj.get(field).toString();
                        for (Method m : methods) {
                            if (m.getName().equals(getMethodName)) {
                                hasGetMethod = true;
                            }
                            if (m.getName().equals(setMethodName)) {
                                hasSetMethod = true;
                            }
                        }
                        if (hasSetMethod) {
                            Method method = doksys.getClass().getMethod(
                                    setMethodName,
                                    String.class
                            );
                            method.invoke(doksys, value);
                        }
                        if (hasGetMethod) {
                            Method method = doksys.getClass().getMethod(getMethodName);
                            try {
                                ArrayList methodArray = (ArrayList) method.invoke(doksys);
                                methodArray.add(value);
                            } catch (Exception e) {
                                log.log(ERROR, e);
                            }
                        }
                    } else if (doksysMetaObj.get(field) instanceof JSONArray) {
                        JSONArray values = doksysMetaObj.getJSONArray(field);
                        for (Method m : methods) {
                            if (m.getName().equals(getMethodName)) {
                                hasGetMethod = true;
                            }
                            if (m.getName().equals(setMethodName)) {
                                hasSetMethod = true;
                            }
                        }
                        if (hasGetMethod) {
                            Method method = doksys.getClass().getMethod(getMethodName);
                            try {
                                ArrayList methodArray = (ArrayList) method.invoke(doksys);
                                for (int i = 0; i < values.length(); i++) {
                                    methodArray.add(values.get(i));
                                }
                            } catch (Exception e) {
                                log.log(ERROR, e);
                            }
                        }
                        if (hasSetMethod) {
                            log.log(DEBUG, setMethodName + " shouldn't exist here.");
                        }
                    }
                }
            } catch (Exception e) {
                log.log(ERROR, e.getClass().getName()
                        + " exception while trying to access methods for " + field
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
        JSONObject elanMetaObj;
        JSONArray elanScenarioMetaJson = new JSONArray();
        if (metaObj.has("Elan")) {
            elanMetaObj = metaObj.getJSONObject("Elan");
        } else if (metaObj.has("ELAN")) {
            elanMetaObj = metaObj.getJSONObject("ELAN");
        } else {
            return;
        }
        if (elanMetaObj.has("Scenario")) {
            if (elanMetaObj.get("Scenario") instanceof String || elanMetaObj.get("Scenario") instanceof Number) {
                elanScenarioMetaJson.put(elanMetaObj.get("Scenario").toString());
            } else if (elanMetaObj.get("Scenario") instanceof JSONArray) {
                elanScenarioMetaJson = elanMetaObj.getJSONArray("Scenario");
            }
        } else if (elanMetaObj.has("Scenarios")) {
            log.log(WARNING, "[deprecated] Key 'Scenarios' found in JSON. Please change to 'Scenario': []. "
                    + "Support for 'Scenarios' will be removed in a future release.");
            if (elanMetaObj.get("Scenarios") instanceof String || elanMetaObj.get("Scenarios") instanceof Number) {
                elanScenarioMetaJson.put(elanMetaObj.get("Scenarios").toString());
            } else if (elanMetaObj.get("Scenarios") instanceof JSONArray) {
                elanScenarioMetaJson = elanMetaObj.getJSONArray("Scenarios");
            }
        }
        if (elanScenarioMetaJson.length() > 0) {
            for (int i = 0; i < elanScenarioMetaJson.length(); i++) {
                elan.getScenario().add(elanScenarioMetaJson.get(i).toString());
            }
        }
        meta.setELAN(elan);
    }

    /**
     * Add an annotation to the Report containing the
     * RodosDokpoolMeta data fields.
     * @param meta The meta part of the report to add the Rodos meta to.
     * @param metaObj The full metaJsonObject of the request.
     */
    public static void addRodosMeta(JSONObject metaObj, DokpoolMeta meta) {
        RODOS rodos = new RODOS();
        JSONObject rodosMetaObj;
        if (metaObj.has("Rodos")) {
            rodosMetaObj = metaObj.getJSONObject("Rodos");
        } else if (metaObj.has("RODOS")) {
            rodosMetaObj = metaObj.getJSONObject("RODOS");
        } else {
            return;
        }
        for (String field: rodosMetaObj.keySet()) {
            String methodName = "set" + field;
            try {
                String value = rodosMetaObj.get(field).toString();
                Method method = rodos.getClass().getMethod(
                        methodName,
                        String.class
                );
                method.invoke(rodos, value);
            } catch (Exception e) {
                log.log(ERROR, e.getClass().getName()
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
        JSONObject reiMetaObj;
        if (metaObj.has("Rei")) {
            reiMetaObj = metaObj.getJSONObject("Rei");
        } else if (metaObj.has("REI")) {
            reiMetaObj = metaObj.getJSONObject("REI");
        } else {
            return;
        }
        List<String> dateParams = Arrays.asList("SigningDate");
        List<String> boolParams = Arrays.asList("Signed");
        List<String> numParams = Arrays.asList("Year", "Revision");
        List<String> listParams = Arrays.asList(
                "ReiLegalBase",
                "MSt",
                "Origin",
                "NuclearInstallation"
        );
        for (String field: REI_FIELDS) {
            if (!reiMetaObj.has(field)) {
                continue;
            }
            String methodName = "set" + field;
            // TODO add generic list handling as well!
            // TODO allow string if list has only one value for JSON
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
                log.log(ERROR, e.getClass().getName()
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
        if (field.equals("ReiLegalBases") || field.equals("ReiLegalBase")) {
            if (reiMetaObj.has(field)) {
                //REI.ReiLegalBase reilegalbase = new REI.ReiLegalBase();
                JSONArray fieldMetaJson = reiMetaObj
                        .getJSONArray(field);
                //List<String> reilegalbaseList = reilegalbases.getReiLegalBase();
                for (int i = 0; i < fieldMetaJson.length(); i++) {
                    rei.getReiLegalBase().add(fieldMetaJson.getString(i));
                    //reilegalbaseList.add(fieldMetaJson.getString(i));
                }
                //rei.setReiLegalBases(reilegalbases);
            }
        } else if (field.equals("Origins") || field.equals("Origin")) {
            if (reiMetaObj.has(field)) {
                //REI.Origins origins = new REI.Origins();
                JSONArray fieldMetaJson = reiMetaObj
                        .getJSONArray(field);
                //List<String> originList = origins.getOrigin();
                for (int i = 0; i < fieldMetaJson.length(); i++) {
                    //originList.add(fieldMetaJson.getString(i));
                    rei.getOrigin().add(fieldMetaJson.getString(i));
                }
                //rei.setOrigins(origins);
            }
        } else if (field.equals("NuclearInstallations")
                || field.equals("NuclearInstallation")) {
            if (reiMetaObj.has(field)) {
                //REI.NuclearInstallations nuclearinstallations
                //        = new REI.NuclearInstallations();
                JSONArray fieldMetaJson = reiMetaObj.getJSONArray(field);
                //List<String> nuclearinstallationList = nuclearinstallations
                //        .getNuclearInstallation();
                for (int i = 0; i < fieldMetaJson.length(); i++) {
                    //nuclearinstallationList.add(fieldMetaJson.getString(i));
                    rei.getNuclearInstallation().add(fieldMetaJson.getString(i));
                }
                //rei.setNuclearInstallations(nuclearinstallations);
            }
        } else if (field.equals("MSt")) {
            if (reiMetaObj.has("MSt")) {
                //REI.MStIDs reimstids = new REI.MStIDs();
                //List<REI.MStIDs.MSt> reimstList = reimstids.getMSt();
                JSONArray reiMstidsMetaJson = reiMetaObj.getJSONArray(field);
                for (int i = 0; i < reiMstidsMetaJson.length(); i++) {
                    //REI.MStIDs.MSt reimst = new REI.MStIDs.MSt();
                    JSONObject reiMStJson = reiMstidsMetaJson.getJSONObject(i);
                    REI.MSt reimst = new REI.MSt();
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
                    rei.getMSt().add(reimst);
                    //reimstList.add(reimst);
                }
                //rei.setMStIDs(reimstids);
            }
        }
    }

}
