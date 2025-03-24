/* Copyright (C) 2015-2025 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE for details.
 */

package de.intevation.test.irix;


import org.iaea._2012.irix.format.ReportType;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.intevation.irix.DokpoolUtils;
import de.intevation.irix.ReportUtils;

import jakarta.xml.bind.JAXBException;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DokpoolUtilsTest {

    // TODO: add Rodos and Rei in REQUEST string
    private static final String REQUEST =
      "{"
        + "  \"request-type\": \"upload/respond\","
        + "  \"irix\": {"
        + "    \"Title\": \"IRIX Test request\","
        + "    \"User\": \"Testuser\","
        + "    \"Identification\": {"
        + "      \"OrganisationReporting\": \"irix.test.de\","
        + "      \"Confidentiality\": \"Free for Public Use\","
        + "      \"ReportingBases\": {"
        + "        \"ReportingBasis\": ["
        + "          \"EU Council Decision 87/600/EURATOM\", "
        + "          \"Second entry for Testing\", "
        + "          \"ESD\""
        + "        ]"
        + "      },"
        + "      \"ReportContext\": \"Test\","
        + "      \"SequenceNumber\": \"42\","
        + "      \"OrganisationContact\": {"
        + "        \"Name\": \"TestOrg\","
        + "        \"OrganisationID\": \"irix.test.de\","
        + "        \"Country\": \"DE\""
        + "      }"
        + "    },"
        + "    \"DokpoolMeta\": {"
        + "      \"DokpoolContentType\": \"eventinformation\","
        + "      \"IsElan\": \"true\","
        + "      \"IsDoksys\": \"true\","
        + "      \"IsRodos\": \"true\","
        + "      \"IsRei\": \"false\","
        + "      \"ELAN\": {"
        + "        \"Scenario\": [\"routinemode\", \"event-abc\"]"
        + "      },"
        + "      \"DOKSYS\": {"
        + "        \"Purpose\": [\"Standard-Info\"],"
        + "        \"NetworkOperator\": [\"U\"],"
        + "        \"SampleType\": [\"L5\"],"
        + "        \"Dom\": [\"105\"],"
        + "        \"DataSource\": [\"IMIS\"],"
        + "        \"LegalBase\": [\"§162\"],"
        + "        \"SamplingBegin\": \"2015-05-28T15:35:54.168+02:00\","
        + "        \"SamplingEnd\":\"2015-05-28T15:52:52.128+02:00\","
        + "        \"Duration\":\"1d\","
        + "        \"OperationMode\": \"Übung\","
        + "        \"TrajectoryEndLocation\": \"\","
        + "        \"TrajectoryEndTime\": \"2024-12-03T08:59:47\","
        + "        \"TrajectoryStartLocation\": \"\","
        + "        \"TrajectoryStartTime\": \"2024-12-03T08:59:47\","
        + "        \"MeasuringProgram\": [\"Intensivmessprogramm\"],"
        + "        \"Status\": \"nicht plausibel\""
        + "      },"
        + "      \"RODOS\": {"
        + "        \"PrognosisType\": \"foo\","
        + "        \"PrognosisForm\": \"BARAKW\","
        + "      }"
        + "    }"
        + "  }"
        + "}";

    private static final String DOKPOOL_MINIMAL =
      "{"
        + "  \"request-type\": \"upload/respond\","
        + "  \"irix\": {"
        + "    \"Title\": \"IRIX Test request\","
        + "    \"User\": \"Testuser\","
        + "    \"Identification\": {"
        + "      \"OrganisationReporting\": \"irix.test.de\","
        + "      \"ReportContext\": \"Test\","
        + "      \"SequenceNumber\": \"42\","
        + "      \"OrganisationContact\": {"
        + "        \"Name\": \"TestOrg\","
        + "        \"OrganisationID\": \"irix.test.de\","
        + "        \"Country\": \"DE\""
        + "      }"
        + "    },"
        + "    \"DokpoolMeta\": {"
        + "      \"DokpoolContentType\": \"eventinformation\","
        + "      \"IsDoksys\": \"true\","
        + "      \"DOKSYS\": {"
        + "        \"SampleType\": [\"L5\"],"
        + "        \"Dom\": [\"105\"],"
        + "        \"SamplingBegin\": \"2015-05-28T15:35:54.168+02:00\","
        + "        \"SamplingEnd\":\"2015-05-28T15:52:52.128+02:00\""
        + "      }"
        + "    }"
        + "  }"
        + "}";

    private static final String DOKPOOL_FAIL =
      "{"
        + "  \"request-type\": \"upload/respond\","
        + "  \"irix\": {"
        + "    \"Title\": \"IRIX Test request\","
        + "    \"User\": \"Testuser\","
        + "    \"Identification\": {"
        + "      \"OrganisationReporting\": \"irix.test.de\","
        + "      \"Confidentiality\": \"FAIL for Testing\","
        + "      \"ReportingBases\": {"
        + "        \"ReportingBasis\": ["
        + "          \"EU Council Decision 87/600/EURATOM\", "
        + "          \"Second entry for Testing\", "
        + "          \"ESD\""
        + "        ]"
        + "      },"
        + "      \"ReportContext\": \"Test\","
        + "      \"SequenceNumber\": \"42\","
        + "      \"OrganisationContact\": {"
        + "        \"Name\": \"TestOrg\","
        + "        \"OrganisationID\": \"irix.test.de\","
        + "        \"Country\": \"DE\""
        + "      }"
        + "    },"
        + "    \"DokpoolMeta\": {"
        + "      \"DokpoolContentType\": \"eventinformation\","
        + "      \"IsRei\": \"true\","
        + "      \"SampleType\": [\"L5\"],"
        + "      \"Dom\": [\"105\"],"
        + "      \"SamplingBegin\": \"2015-05-28T15:35:54.168+02:00\","
        + "      \"SamplingEnd\":\"2015-05-28T15:52:52.128+02:00\""
        + "    }"
        + "  }"
        + "}";

    @Before
    public void setupLogging() {
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFail() throws IllegalArgumentException {
        JSONObject json = new JSONObject(DOKPOOL_FAIL);
        ReportType report = ReportUtils.prepareReport(json);
    }

    @Test
    public void testNoDokpool()
            throws JAXBException, JSONException, SAXException {
        File schemaFile = new File(
                "src/main/webapp/WEB-INF/irix-schema/Dokpool-3.xsd");
        JSONObject json = new JSONObject(REQUEST);
        json.getJSONObject("irix").remove("DokpoolMeta");
        ReportType report = ReportUtils.prepareReport(json);
        DokpoolUtils.addAnnotation(json, report, schemaFile);
    }

    @Test
    public void testDokpoolValidationOk()
            throws JAXBException, JSONException, SAXException {
        File schemaFile = new File(
                "src/main/webapp/WEB-INF/irix-schema/Dokpool-3.xsd");
        JSONObject json = new JSONObject(REQUEST);
        ReportType report = ReportUtils.prepareReport(json);
        DokpoolUtils.addAnnotation(json, report, schemaFile);
    }

// Does not fail any more, because LegalBase is on another level.
//     @Test(expected = JAXBException.class)
//     public void testDokpoolValidationFail()
//             throws JAXBException, JSONException, SAXException {
//         File schemaFile = new File(
//                 "src/main/webapp/WEB-INF/irix-schema/Dokpool-3.xsd");
//         JSONObject json = new JSONObject(REQUEST);
//         json.getJSONObject("irix").getJSONObject("DokpoolMeta")
//                 .put("LegalBase", "foo bar");
//         ReportType report = ReportUtils.prepareReport(json);
//         DokpoolUtils.addAnnotation(json, report, schemaFile);
//     }

    @Test(expected = JAXBException.class)
    public void testDokpoolValidationFailType()
            throws JAXBException, JSONException, SAXException {
        File schemaFile = new File(
                "src/main/webapp/WEB-INF/irix-schema/Dokpool-3.xsd");
        JSONObject json = new JSONObject(DOKPOOL_MINIMAL);
        json.getJSONObject("irix").getJSONObject("DokpoolMeta")
                .put("IsRei", "false");
        ReportType report = ReportUtils.prepareReport(json);
        DokpoolUtils.addAnnotation(json, report, schemaFile);
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
        DokpoolUtils.addAnnotation(json, report, schemaFile);
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
        DokpoolUtils.addAnnotation(json, report, schemaFile);
    }

    @Test
    public void testAnnotation() throws SAXException, JAXBException {
        JSONObject json = new JSONObject(REQUEST);
        ReportType report = ReportUtils.prepareReport(json);
        DokpoolUtils.addAnnotation(json, report, null);
        Assert.assertEquals(
                report.getAnnexes().getAnnotation().get(0).getTitle(),
                json.getJSONObject("irix").getString("Title"));
    }

    //TODO: passes now, Java changes?
//     @Test(expected = IllegalArgumentException.class)
//     public void testDateFormat() throws SAXException, JAXBException {
//         JSONObject json = new JSONObject(REQUEST);
//         ReportType report = ReportUtils.prepareReport(json);
//         json.getJSONObject("irix").getJSONObject("DokpoolMeta")
//                 .put("SamplingBegin", "2015-15-28T15:35:54.168+02:00");
//         DokpoolUtils.addAnnotation(json, report, null);
//     }

    @Test
    public void testAttachFile() {
        JSONObject json = new JSONObject(REQUEST);
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.attachFile("foo", "testdata".getBytes(), report,
                "bar", "foo.bin");
        Assert.assertEquals("foo", report.getAnnexes().getFileEnclosure()
                .get(0).getTitle());
        byte[] expected = "testdata".getBytes();
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
        byte[] expected = null;
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

        Assert.assertEquals("foo", report.getAnnexes().getFileEnclosure()
                .get(0).getTitle());
        Assert.assertEquals("foo2", report.getAnnexes().getFileEnclosure()
                .get(1).getTitle());
        Assert.assertEquals("foo3", report.getAnnexes().getFileEnclosure()
                .get(2).getTitle());
    }

};
