package ru.yandex.wmtools.common.sita;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class SitaUrlInfoReadingJsonResponseTest {
    private static final String RESPONSE_WMC_39 = "{\n"
            + "  \"Results\":\n"
            + "    [\n"
            + "      {\n"
            + "        \"Type\":\"AT_URL_INFO_READING\",\n"
            + "        \"Data\":\n"
            + "          {\n"
            + "            \"Url\":\"http://www.wear-baby.ru\"\n"
            + "          },\n"
            + "        \"Errors\":\n"
            + "          [\n"
            + "            {\n"
            + "              \"Source\":\"SITA\",\n"
            + "              \"SitaError\":\n"
            + "                {\n"
            + "                  \"Code\":\"INCOMPLETE_RESPONSE\"\n"
            + "                }\n"
            + "            }\n"
            + "          ],\n"
            + "        \"UrlInfoReadingResult\":\n"
            + "          {\n"
            + "            \"KiwiObject\":\n"
            + "              {\n"
            + "                \"Key\":\"http://www.wear-baby.ru/\",\n"
            + "                \"Keytype\":10,\n"
            + "                \"Tuples\":\n"
            + "                  [\n"
            + "                    {\n"
            + "                      \"AttrId\":167,\n"
            + "                      \"AttrName\":\"MirrorMain\",\n"
            + "                      \"BranchId\":0,\n"
            + "                      \"BranchName\":\"TRUNK\",\n"
            + "                      \"TimeStamp\":1385455960,\n"
            + "                      \"Type\":\"AT_STRING\",\n"
            + "                      \"RawData\":\"\",\n"
            + "                      \"DataHash\":0\n"
            + "                    },\n"
            + "                    {\n"
            + "                      \"AttrId\":46,\n"
            + "                      \"AttrName\":\"IsMultiLang\",\n"
            + "                      \"BranchId\":0,\n"
            + "                      \"BranchName\":\"TRUNK\",\n"
            + "                      \"TimeStamp\":4294967295,\n"
            + "                      \"Type\":\"AT_UI8\",\n"
            + "                      \"DataHash\":4294967295,\n"
            + "                      \"Info\":\n"
            + "                        {\n"
            + "                          \"Status\":\n"
            + "                            {\n"
            + "                              \"ExecStatus\":\"DATA_NOT_FOUND\",\n"
            + "                              \"Info\":\"DATA_NOT_FOUND\"\n"
            + "                            }\n"
            + "                        }\n"
            + "                    },\n"
            + "                    {\n"
            + "                      \"AttrId\":167,\n"
            + "                      \"AttrName\":\"MirrorMain\",\n"
            + "                      \"BranchId\":1,\n"
            + "                      \"BranchName\":\"RUS\",\n"
            + "                      \"TimeStamp\":4294967295,\n"
            + "                      \"Type\":\"AT_STRING\",\n"
            + "                      \"DataHash\":4294967295,\n"
            + "                      \"Info\":\n"
            + "                        {\n"
            + "                          \"Status\":\n"
            + "                            {\n"
            + "                              \"ExecStatus\":\"DATA_NOT_FOUND\",\n"
            + "                              \"Info\":\"DATA_NOT_FOUND\"\n"
            + "                            }\n"
            + "                        }\n"
            + "                    }\n"
            + "                  ]\n"
            + "              }\n"
            + "          }\n"
            + "      }\n"
            + "    ]\n"
            + "}";

    @Test
    public void testWMC_39() throws IOException {
        SitaUrlInfoReadingResponse sitaUrlInfoReadingResponse = SitaUrlInfoReadingJsonResponse.parse(new StringReader(RESPONSE_WMC_39));
        Assert.assertEquals(3, sitaUrlInfoReadingResponse.getTuples().size());
    }
}
