/* Copyright (C) 2015 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE.txt for details.
 */

package de.intevation.test.irix;

import org.iaea._2012.irix.format.ReportType;
import org.iaea._2012.irix.format.identification.ReportContextType;

import org.json.JSONObject;
import org.json.JSONException;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import de.intevation.irix.ReportUtils;

import org.xml.sax.SAXException;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;

import javax.xml.bind.JAXBException;
public class ReportUtilsTest {

    private static final String REQUEST =
        "{"
        + "    \"request-type\": \"upload/respond\","
        + "    \"irix\": {"
        + "        \"Title\": \"IRIX Test request\","
        + "        \"User\": \"Testuser\","
        + "        \"Identification\": {"
        + "            \"OrganisationReporting\": \"irix.test.de\","
        + "            \"ReportContext\": \"Test\","
        + "            \"SequenceNumber\": \"42\","
        + "            \"OrganisationContact\": {"
        + "                \"Name\": \"TestOrg\","
        + "                \"OrganisationID\": \"irix.test.de\","
        + "                \"Country\": \"DE\""
        + "            }"
        + "        },"
        + "        \"DokpoolMeta\": {"
        + "            \"Purpose\": \"Standard-Info\","
        + "            \"DokpoolContentType\": \"eventinformation\","
        + "            \"IsElan\": \"true\","
        + "            \"IsDoksys\": \"false\","
        + "            \"IsRodos\": \"false\","
        + "            \"IsRei\": \"false\","
        + "            \"NetworkOperator\": \"U - BFS (ABI)\","
        + "            \"SampleTypeId\": \"L5\","
        + "            \"SampleType\": \"L5 - Niederschlag\","
        + "            \"Dom\": \"Gamma-Spektrometrie\","
        + "            \"DataType\": \"LaDa\","
        + "            \"LegalBase\": \"§2\","
        + "            \"MeasuringProgram\": \"Intensivmessprogramm\","
        + "            \"Status\": \"3 - nicht plausibel\","
        + "            \"SamplingBegin\": \"2015-05-28T15:35:54.168+02:00\","
        + "            \"SamplingEnd\":\"2015-05-28T15:52:52.128+02:00\""
        + "        }"
        + "    }"
        + "}";

    private static final String DOKPOOL_MINIMAL =
        "{"
        + "    \"request-type\": \"upload/respond\","
        + "    \"irix\": {"
        + "        \"Title\": \"IRIX Test request\","
        + "        \"User\": \"Testuser\","
        + "        \"Identification\": {"
        + "            \"OrganisationReporting\": \"irix.test.de\","
        + "            \"ReportContext\": \"Test\","
        + "            \"SequenceNumber\": \"42\","
        + "            \"OrganisationContact\": {"
        + "                \"Name\": \"TestOrg\","
        + "                \"OrganisationID\": \"irix.test.de\","
        + "                \"Country\": \"DE\""
        + "            }"
        + "        },"
        + "        \"DokpoolMeta\": {"
        + "            \"DokpoolContentType\": \"eventinformation\","
        + "            \"IsRei\": \"true\","
        + "            \"SampleTypeId\": \"L5\","
        + "            \"SampleType\": \"L5 - Niederschlag\","
        + "            \"Dom\": \"Gamma-Spektrometrie\","
        + "            \"SamplingBegin\": \"2015-05-28T15:35:54.168+02:00\","
        + "            \"SamplingEnd\":\"2015-05-28T15:52:52.128+02:00\""
        + "        }"
        + "    }"
        + "}";

    @Before
    public void setupLogging() {
        ConsoleAppender console = new ConsoleAppender(); //create appender
        String pattern = "[%p|%C{1}] %m%n";
        console.setLayout(new PatternLayout(pattern));
        console.setThreshold(Level.ERROR); // Change here for testing ;-)
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
    }

    @Test
    public void testOrganisationReporting() throws JSONException {
        JSONObject json = new JSONObject(REQUEST);
        ReportType report = ReportUtils.prepareReport(json);
        Assert.assertEquals(
            report.getIdentification().getOrganisationReporting(),
            json.getJSONObject("irix").getJSONObject("Identification").
                getString("OrganisationReporting"));
    }

    @Test
    public void testReportContext() throws JSONException {
        JSONObject json = new JSONObject(REQUEST);
        ReportType report = ReportUtils.prepareReport(json);
        Assert.assertEquals(
            report.getIdentification().getReportContext(),
            ReportContextType.fromValue(
                json.getJSONObject("irix").getJSONObject("Identification").
                    getString("ReportContext")));
    }

    @Test
    public void testNoDokpool()
        throws JAXBException, JSONException, SAXException {
        File schemaFile = new File(
            "src/main/webapp/WEB-INF/irix-schema/Dokpool-3.xsd");
        JSONObject json = new JSONObject(REQUEST);
        json.getJSONObject("irix").remove("DokpoolMeta");
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.addAnnotation(json, report, schemaFile);
    }

    @Test
    public void testDokpoolValidationOk()
        throws JAXBException, JSONException, SAXException {
        File schemaFile = new File(
            "src/main/webapp/WEB-INF/irix-schema/Dokpool-3.xsd");
        JSONObject json = new JSONObject(REQUEST);
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.addAnnotation(json, report, schemaFile);
    }

    @Test(expected = JAXBException.class)
    public void testDokpoolValidationFail()
        throws JAXBException, JSONException, SAXException {
        File schemaFile = new File(
            "src/main/webapp/WEB-INF/irix-schema/Dokpool-3.xsd");
        JSONObject json = new JSONObject(REQUEST);
        json.getJSONObject("irix").getJSONObject("DokpoolMeta")
            .put("DokpoolContentType", "foo bar");
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.addAnnotation(json, report, schemaFile);
    }

    @Test(expected = JAXBException.class)
    public void testDokpoolValidationFailType()
        throws JAXBException, JSONException, SAXException {
        File schemaFile = new File(
            "src/main/webapp/WEB-INF/irix-schema/Dokpool-3.xsd");
        JSONObject json = new JSONObject(DOKPOOL_MINIMAL);
        json.getJSONObject("irix").getJSONObject("DokpoolMeta")
            .put("IsRei", "false");
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.addAnnotation(json, report, schemaFile);
    }

    @Test(expected = JAXBException.class)
    public void testDokpoolValidationFailNetworkOp()
        throws JAXBException, JSONException, SAXException {
        File schemaFile = new File(
            "src/main/webapp/WEB-INF/irix-schema/Dokpool-3.xsd");
        JSONObject json = new JSONObject(DOKPOOL_MINIMAL);
        json.getJSONObject("irix").getJSONObject("DokpoolMeta")
            .put("IsRei", "false");
        json.getJSONObject("irix").getJSONObject("DokpoolMeta")
            .put("IsDoksys", "true");
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.addAnnotation(json, report, schemaFile);
    }

    @Test
    public void testSuggestedValues()
        throws JAXBException, JSONException, SAXException {
        File schemaFile = new File(
            "src/main/webapp/WEB-INF/irix-schema/Dokpool-3.xsd");
        JSONObject json = new JSONObject(REQUEST);
        json.getJSONObject("irix").getJSONObject("DokpoolMeta")
            .put("Dom", "divination");
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.addAnnotation(json, report, schemaFile);
    }

    @Test
    public void testDokpoolMinimal()
        throws JAXBException, JSONException, SAXException {
        File schemaFile = new File(
            "src/main/webapp/WEB-INF/irix-schema/Dokpool-3.xsd");
        JSONObject json = new JSONObject(DOKPOOL_MINIMAL);
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.addAnnotation(json, report, schemaFile);
    }

    @Test
    public void testValidationOk()
        throws JSONException, SAXException, JAXBException {
        File schemaFile = new File(
            "src/main/webapp/WEB-INF/irix-schema/IRIX.xsd");
        JSONObject json = new JSONObject(REQUEST);
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.marshallReport(
            report, new java.io.ByteArrayOutputStream(), schemaFile);
    }

    @Test(expected = JAXBException.class)
    public void testValidationFail()
        throws JSONException, SAXException, JAXBException {
        File schemaFile = new File(
            "src/main/webapp/WEB-INF/irix-schema/IRIX.xsd");
        JSONObject json = new JSONObject(REQUEST);
        json.getJSONObject("irix").getJSONObject("Identification")
            .put("OrganisationReporting", "in valid");
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.marshallReport(
            report, new java.io.ByteArrayOutputStream(), schemaFile);
    }

    @Test
    public void testUserName() {
        JSONObject json = new JSONObject(REQUEST);
        ReportType report = ReportUtils.prepareReport(json);
        Assert.assertEquals(
            report.getIdentification().getIdentifications()
                .getPersonContactInfo().get(0).getName(),
            json.getJSONObject("irix").getString("User"));
    }

    @Test(expected = JSONException.class)
    public void testUserNameMandatory() {
        JSONObject json = new JSONObject(REQUEST);
        json.getJSONObject("irix").remove("User");
        ReportType report = ReportUtils.prepareReport(json);
    }

    @Test
    public void testAnnotation() throws SAXException, JAXBException {
        JSONObject json = new JSONObject(REQUEST);
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.addAnnotation(json, report, null);
        Assert.assertEquals(
            report.getAnnexes().getAnnotation().get(0).getTitle(),
            json.getJSONObject("irix").getString("Title"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDateFormat() throws SAXException, JAXBException {
        JSONObject json = new JSONObject(REQUEST);
        ReportType report = ReportUtils.prepareReport(json);
        json.getJSONObject("irix").getJSONObject("DokpoolMeta")
            .put("SamplingBegin", "2015-15-28T15:35:54.168+02:00");
        ReportUtils.addAnnotation(json, report, null);
    }

    @Test
    public void testAttachFile() {
        JSONObject json = new JSONObject(REQUEST);
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.attachFile("foo", "testdata".getBytes(), report,
                "bar", "foo.bin");
        Assert.assertEquals(
            "foo", report.getAnnexes().getFileEnclosure().get(0).getTitle());
        byte [] expected  = "testdata".getBytes();
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(expected[i],
                report.getAnnexes().getFileEnclosure().get(0)
                    .getEnclosedObject()[i]);
        }
    }

    @Test
    public void testFileHash() {
        JSONObject json = new JSONObject(REQUEST);
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.attachFile("foo", "testdata".getBytes(), report,
                "bar", "foo.bin");
        byte [] expected = null;
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            expected = sha1.digest("testdata".getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 unavailable. Can't happen.");
        }

        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(expected[i],
                report.getAnnexes().getFileEnclosure().get(0)
                    .getFileHash().getValue()[i]);
        }
    }

    @Test
    public void testMultipleFiles() {
        JSONObject json = new JSONObject(REQUEST);
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.attachFile("foo", "testdata".getBytes(), report,
                "bar", "foo.bin");
        ReportUtils.attachFile("foo2", "testdata".getBytes(), report,
                "bar", "foo.bin");
        ReportUtils.attachFile("foo3", "testdata".getBytes(), report,
                "bar", "foo.bin");

        Assert.assertEquals(
            "foo", report.getAnnexes().getFileEnclosure().get(0).getTitle());
        Assert.assertEquals(
            "foo2", report.getAnnexes().getFileEnclosure().get(1).getTitle());
        Assert.assertEquals(
            "foo3", report.getAnnexes().getFileEnclosure().get(2).getTitle());
    }

};
