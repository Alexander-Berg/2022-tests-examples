package ru.yandex.wmtools.common.sita;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.wmtools.common.error.InternalException;
import ru.yandex.wmtools.common.util.http.YandexHttpStatus;

/**
 * @author azakharov
 */
public class SitaUrlFetchJsonResponseTest {
    private static final String RESPONSE =
            "{\n" +
                    "    \"Results\": [\n" +
                    "        {\n" +
                    "            \"Type\": \"AT_URL_FETCHING\",\n" +
                    "            \"Data\": {\n" +
                    "                \"Url\": \"http://m-senin.narod.ru/\"\n" +
                    "            },\n" +
                    "            \"UrlFetchingResult\": {\n" +
                    "                \"IsUrlAllowed\": true,\n" +
                    "                \"Document\": \"HTTP/1.1 200 OK\\r\\nServer: uServ/3.2.2\\r\\nDate: Fri, 24 May 2013 15:23:12 GMT\\r\\nContent-Type: text/html; charset=UTF-8\\r\\n\\r\\n\",\n" +
                    "                \"RobotsTxt\": \"User-agent: yandex \\r\\nDisallow: /info \\r\\nDisallow: /sitemap.xml\\r\\n \\r\\nSitemap: http://m-senin.narod.ru/sitemapindex.xml\\r\\nHost\",\n" +
                    "                \"Ip4\": 3245209573,\n" +
                    "                \"HttpCode\": 200,\n" +
                    "                \"Times\": {\n" +
                    "                    \"ZoraInfo\": {\n" +
                    "                        \"GotRequest\": 1369408990697072,\n" +
                    "                        \"SentRequest\": 1369408990697194,\n" +
                    "                        \"GotReply\": 1369408990717354,\n" +
                    "                        \"SentReply\": 1369408990717386,\n" +
                    "                        \"HostName\": \"zora4-04.yandex.ru\"\n" +
                    "                    },\n" +
                    "                    \"SpiderInfo\": {\n" +
                    "                        \"GotRequest\": 1369408990697463,\n" +
                    "                        \"StartSend\": 1369408990703044,\n" +
                    "                        \"FinishSend\": 1369408990705799,\n" +
                    "                        \"StartReceive\": 1369408990708778,\n" +
                    "                        \"FinishReceive\": 1369408990708963,\n" +
                    "                        \"StartCalc\": 1369408990709222,\n" +
                    "                        \"FinishCalc\": 1369408990715036,\n" +
                    "                        \"SentReply\": 1369408990715147,\n" +
                    "                        \"HostName\": \"178-154-243-104.yandex.com\"\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            }\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}";

    private static final String ERROR_RESPONSE =
            "{\n" +
                    "    \"Results\": [\n" +
                    "        {\n" +
                    "            \"Type\": \"AT_URL_FETCHING\",\n" +
                    "            \"Data\": {\n" +
                    "                \"Url\": \"http://m-senin.narod.ru/404.html\"\n" +
                    "            },\n" +
                    "            \"Errors\": [\n" +
                    "                {\n" +
                    "                    \"Source\": \"SITA\",\n" +
                    "                    \"SitaError\": {\n" +
                    "                        \"Code\": \"QUOTA_ERROR\"\n" +
                    "                    }\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"Source\": \"QUOTA\",\n" +
                    "                    \"Description\": \"no source in the config\",\n" +
                    "                    \"QuotaError\": {\n" +
                    "                        \"Code\": 1\n" +
                    "                    }\n" +
                    "                }\n" +
                    "            ]\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}";

    private static final String ROBOTS_TXT =
            "User-agent: *\n" +
            "Disallow: /тест/\n";

    private static final String TIMEOUT_RESPONSE =
            "{" +
                "\"Results\":[{" +
                    "\"Type\":\"AT_URL_FETCHING\"," +
                    "\"Data\":{" +
                        "\"Url\":\"http://wmtest.people.yandex.net\"" +
                    "}," +
                    "\"Errors\":[{" +
                        "\"Source\":\"MSGBUS\"," +
                        "\"Description\":\"[document] OnlineZora read failed\"," +
                        "\"SitaError\":null," +
                        "\"MsgBusError\":{" +
                            "\"MessageStatus\":2" +
                        "}" +
                    "}]," +
                    "\"UrlFetchingResult\":null" +
                "}]," +
                "\"Errors\":[{" +
                    "\"Source\":\"SITA\"," +
                    "\"SitaError\":{" +
                        "\"Code\":\"TIMEOUT_EXCEEDED\"" +
                    "}" +
                "}]" +
            "}";

    private static final String UTF8_REDIRECT=
            "{\n"
            + "  \"Results\":\n"
            + "    [\n"
            + "      {\n"
            + "        \"Type\":\n"
            + "          \"AT_URL_FETCHING\",\n"
            + "        \"Data\":\n"
            + "          {\n"
            + "            \"Url\":\n"
            + "              \"http://wmtest.people.yandex.net/\"\n"
            + "          },\n"
            + "        \"Errors\":\n"
            + "          [\n"
            + "            {\n"
            + "              \"Source\":\n"
            + "                \"ZORA\",\n"
            + "              \"Description\":\n"
            + "                \"[document]\",\n"
            + "              \"ZoraError\":\n"
            + "                {\n"
            + "                  \"Type\":\n"
            + "                    \"FETCH_ERROR\",\n"
            + "                  \"FetchStatus\":\n"
            + "                    \"UPDATE\",\n"
            + "                  \"CrawlDelay\":\n"
            + "                    200,\n"
            + "                  \"Ip\":\n"
            + "                    621363575,\n"
            + "                  \"HttpCode\":\n"
            + "                    301\n"
            + "                }\n"
            + "            }\n"
            + "          ],\n"
            + "        \"UrlFetchingResult\":\n"
            + "          {\n"
            + "            \"IsUrlAllowed\":\n"
            + "              true,"
            + "            \"Document\":\n"
            + "              \"HTTP/1.1 301 Moved Permanently\\r\\nServer: nginx/1.4.4\\r\\nDate: Tue, 14 Jan 2014 16:00:52 GMT\\r\\nContent-Type: text/html\\r\\nContent-Length: 184\\r\\nLocation: http://wmtest.people.yandex.net/фоо-бар-баз-index.html\\r\\nConnection: keep-alive\\r\\n\\r\\n<html>\\r\\n<head><title>301 Moved Permanently</title></head>\\r\\n<body bgcolor=\\\"white\\\">\\r\\n<center><h1>301 Moved Permanently</h1></center>\\r\\n<hr><center>nginx/1.4.4</center>\\r\\n</body>\\r\\n</html>\\r\\n\",\n"
            + "            \"Ip4\":\n"
            + "              621363575,\n"
            + "            \"HttpCode\":\n"
            + "              301,\n"
            + "            \"MimeType\":\n"
            + "              2,\n"
            + "            \"Encoding\":\n"
            + "              -1,\n"
            + "            \"Language\":\n"
            + "              0,\n"
            + "            \"Times\":\n"
            + "              {\n"
            + "                \"ZoraInfo\":\n"
            + "                  {\n"
            + "                    \"GotRequest\":\n"
            + "                      1389715252066072,\n"
            + "                    \"SentRequest\":\n"
            + "                      1389715252066164,\n"
            + "                    \"GotReply\":\n"
            + "                      1389715252072695,\n"
            + "                    \"SentReply\":\n"
            + "                      1389715252072724,\n"
            + "                    \"HostName\":\n"
            + "                      \"zora4-02.yandex.ru\"\n"
            + "                  },"
            + "                \"SpiderInfo\":\n"
            + "                  {\n"
            + "                    \"GotRequest\":\n"
            + "                      1389715252066493,\n"
            + "                    \"StartSend\":\n"
            + "                      1389715252070015,\n"
            + "                    \"FinishSend\":\n"
            + "                      1389715252071204,\n"
            + "                    \"StartReceive\":\n"
            + "                      1389715252072500,\n"
            + "                    \"FinishReceive\":\n"
            + "                      1389715252072504,\n"
            + "                    \"SentReply\":\n"
            + "                      1389715252072676,\n"
            + "                    \"HostName\":\n"
            + "                      \"178-154-243-106.yandex.com\"\n"
            + "                  }\n"
            + "              }\n"
            + "          }\n"
            + "      }\n"
            + "    ]\n"
            + "}";
    @Test
    public void testSitaParser() throws Exception {
        SitaUrlFetchResponse urlFetchResponse = SitaUrlFetchJsonResponse.parse(toReader(RESPONSE), false);
        Assert.assertEquals(YandexHttpStatus.HTTP_200_OK, urlFetchResponse.getSitaHttpStatus());
        Assert.assertEquals(200, urlFetchResponse.getParsedHttpHeaders().getStatusLine().getStatusCode());
    }

    @Test
    public void testSitaErrorPage() throws IOException, InternalException {
        try {
            SitaUrlFetchResponse urlFetchResponse = SitaUrlFetchJsonResponse.parse(new StringReader(ERROR_RESPONSE), false);
            Assert.fail("SitaService should throw exception on empty response");
        } catch (SitaException e) {
        }
    }

    @Test
    public void testSitaTimeoutResponse() throws Exception {
        try {
            SitaUrlFetchJsonResponse.parse(new StringReader(TIMEOUT_RESPONSE), false);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(SitaIncompleteResponseException.class, e.getClass());
        }
    }

    @Test
    public void testRobotsTxtParse() throws Exception {
        InputStream sitaResponse = this.getClass().getClassLoader().getResourceAsStream("sita_robots_txt_bom.json");
        try {
            SitaUrlFetchResponse sitaUrlFetchResponse = SitaUrlFetchJsonResponse.parse(
                    new InputStreamReader(sitaResponse, SitaService.ISO8859_1), false);
            Assert.assertEquals(ROBOTS_TXT, sitaUrlFetchResponse.getRobotsTxtContent());
        } finally {
            IOUtils.closeQuietly(sitaResponse);
        }
    }

    @Test
    public void testUtf8Redirect() throws Exception {
        SitaUrlFetchResponse response = SitaUrlFetchJsonResponse.parse(toReader(UTF8_REDIRECT), false);
        Header location = response.getParsedHttpHeaders().getFirstHeader("location");
        Assert.assertNotNull(location);
        Assert.assertEquals("http://wmtest.people.yandex.net/фоо-бар-баз-index.html", location.getValue());
    }

    static Reader toReader(String s) throws UnsupportedEncodingException {
        byte[] bytes = s.getBytes(SitaService.UTF_8);
        return new InputStreamReader(new ByteArrayInputStream(bytes), SitaService.ISO8859_1);
    }

    static SitaUrlFetchResponse toResponse(String s) throws IOException {
        return SitaUrlFetchJsonResponse.parse(toReader(s), false);
    }
}
