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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import de.intevation.irix.ReportUtils;

public class ReportUtilsTest
{

    private static final String request =
        "{" +
        "    \"request-type\": \"upload/respond\"," +
        "    \"irix\": {" +
        "        \"Title\": \"IRIX Test request\"," +
        "        \"User\": \"Testuser\"," +
        "        \"Identification\": {" +
        "            \"OrganisationReporting\": \"irix.test.de\"," +
        "            \"ReportContext\": \"Test\"," +
        "            \"SequenceNumber\": \"42\"," +
        "            \"OrganisationContact\": {" +
        "                \"Name\": \"TestOrg\"," +
        "                \"OrganisationID\": \"irix.test.de\"," +
        "                \"Country\": \"DE\"" +
        "            }" +
        "        }," +
        "        \"DokpoolMeta\": {" +
        "            \"Purpose\": \"Standard-Info\"," +
        "            \"DokpoolContentType\": \"eventinformation\"," +
        "            \"IsElan\": \"true\"," +
        "            \"IsDoksys\": \"false\"," +
        "            \"IsRodos\": \"false\"," +
        "            \"IsRei\": \"false\"," +
        "            \"NetworkOperator\": \"U - BFS (ABI)\"," +
        "            \"SampleTypeId\": \"L5\"," +
        "            \"SampleType\": \"L5 - Niederschlag\"," +
        "            \"Dom\": \"Gamma-Spektrometrie\"," +
        "            \"DataType\": \"LaDa\"," +
        "            \"LegalBase\": \"§2\"," +
        "            \"MeasuringProgram\": \"Intensivmessprogramm\"," +
        "            \"Status\": \"3 - nicht plausibel\"," +
        "            \"SamplingBegin\": \"2015-05-28T15:35:54.168+02:00\"," +
        "            \"SamplingEnd\":\"2015-05-28T15:52:52.128+02:00\"" +
        "        }" +
        "    }" +
        "}";

    @Test
    public void testOrganisationReporting() throws JSONException {
        JSONObject json = new JSONObject(request);
        ReportType report = ReportUtils.prepareReport(json);
        Assert.assertEquals(report.getIdentification().getOrganisationReporting(),
                json.getJSONObject("irix").getJSONObject("Identification").
                getString("OrganisationReporting"));
    }

    @Test
    public void testReportContext() throws JSONException {
        JSONObject json = new JSONObject(request);
        ReportType report = ReportUtils.prepareReport(json);
        Assert.assertEquals(report.getIdentification().getReportContext(),
                ReportContextType.fromValue(json.getJSONObject("irix").getJSONObject("Identification").
                getString("ReportContext")));
    }

    @Test
    public void testUserName() {
        JSONObject json = new JSONObject(request);
        ReportType report = ReportUtils.prepareReport(json);
        Assert.assertEquals(report.getIdentification().getIdentifications().getPersonContactInfo().get(0).
                getName(),
                json.getJSONObject("irix").getString("User"));
    }

    @Test(expected=JSONException.class)
    public void testUserNameMandatory() {
        JSONObject json = new JSONObject(request);
        json.getJSONObject("irix").remove("User");
        ReportType report = ReportUtils.prepareReport(json);
    }

    @Test
    public void testAnnotation() {
        JSONObject json = new JSONObject(request);
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.addAnnotation(json, report);
        Assert.assertEquals(report.getAnnexes().getAnnotation().get(0).getTitle(),
        json.getJSONObject("irix").getString("Title"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testDateFormat() {
        JSONObject json = new JSONObject(request);
        ReportType report = ReportUtils.prepareReport(json);
        json.getJSONObject("irix").getJSONObject("DokpoolMeta").put("SamplingBegin",
               "2015-15-28T15:35:54.168+02:00");
        ReportUtils.addAnnotation(json, report);
    }

    @Test
    public void testAttachFile() {
        JSONObject json = new JSONObject(request);
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.attachFile("foo", "testdata".getBytes(), report,
                "bar", "foo.bin");
        Assert.assertEquals("foo", report.getAnnexes().getFileEnclosure().get(0).getTitle());
        byte [] expected  = "testdata".getBytes();
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(expected[i],
                    report.getAnnexes().getFileEnclosure().get(0).getEnclosedObject()[i]);
        }
    }

    @Test
    public void testFileHash() {
        JSONObject json = new JSONObject(request);
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
                    report.getAnnexes().getFileEnclosure().get(0).getFileHash().getValue()[i]);
        }
    }

    @Test
    public void testMultipleFiles() {
        JSONObject json = new JSONObject(request);
        ReportType report = ReportUtils.prepareReport(json);
        ReportUtils.attachFile("foo", "testdata".getBytes(), report,
                "bar", "foo.bin");
        ReportUtils.attachFile("foo2", "testdata".getBytes(), report,
                "bar", "foo.bin");
        ReportUtils.attachFile("foo3", "testdata".getBytes(), report,
                "bar", "foo.bin");

        Assert.assertEquals("foo", report.getAnnexes().getFileEnclosure().get(0).getTitle());
        Assert.assertEquals("foo2", report.getAnnexes().getFileEnclosure().get(1).getTitle());
        Assert.assertEquals("foo3", report.getAnnexes().getFileEnclosure().get(2).getTitle());
    }

};
