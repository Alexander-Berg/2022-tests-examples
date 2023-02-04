package ru.yandex.wmtools.common.sita;

import java.io.IOException;
import java.net.URL;

import com.codahale.metrics.MetricRegistry;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.wmtools.common.error.InternalException;
import ru.yandex.wmtools.common.util.http.YandexHttpStatus;

/**
 * @author aherman
 */
public class SitaRedirectServiceTest {
    private static final String _302_REDIRECT =
            "{\n"
            + "  \"Results\":\n"
            + "    [\n"
            + "      {\n"
            + "        \"Type\":\n"
            + "          \"AT_URL_FETCHING\",\n"
            + "        \"Data\":\n"
            + "          {\n"
            + "            \"Url\":\n"
            + "              \"http://vsdaria.com/\"\n"
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
            + "                    \"REDIR\",\n"
            + "                  \"CrawlDelay\":\n"
            + "                    200,\n"
            + "                  \"Ip\":\n"
            + "                    89081094,\n"
            + "                  \"HttpCode\":\n"
            + "                    302\n"
            + "                }\n"
            + "            }\n"
            + "          ],\n"
            + "        \"UrlFetchingResult\":\n"
            + "          {\n"
            + "            \"IsUrlAllowed\":\n"
            + "              true,"
            + "            \"Document\":\n"
            + "              \"HTTP/1.1 302 Moved Temporarily\\r\\nDate: Wed, 15 Jan 2014 18:54:13 GMT\\r\\nServer: Apache/2.2.15 (CentOS)\\r\\nX-Powered-By: PHP/5.3.3\\r\\nLocation: dar/home.php\\r\\nContent-Length: 0\\r\\nConnection: close\\r\\nContent-Type: text/html; charset=UTF-8\\r\\n\\r\\n\",\n"
            + "            \"Ip4\":\n"
            + "              89081094,\n"
            + "            \"HttpCode\":\n"
            + "              302,\n"
            + "            \"RedirTarget\":\n"
            + "              \"http://vsdaria.com/dar/home.php\",\n"
            + "            \"Times\":\n"
            + "              {\n"
            + "                \"ZoraInfo\":\n"
            + "                  {\n"
            + "                    \"GotRequest\":\n"
            + "                      1389808323604142,\n"
            + "                    \"SentRequest\":\n"
            + "                      1389808323604240,\n"
            + "                    \"GotReply\":\n"
            + "                      1389808323702574,\n"
            + "                    \"SentReply\":\n"
            + "                      1389808323702610,\n"
            + "                    \"HostName\":\n"
            + "                      \"zora4-02.yandex.ru\"\n"
            + "                  },"
            + "                \"SpiderInfo\":\n"
            + "                  {\n"
            + "                    \"GotRequest\":\n"
            + "                      1389808323605507,\n"
            + "                    \"StartSend\":\n"
            + "                      1389808323610724,\n"
            + "                    \"FinishSend\":\n"
            + "                      1389808323655436,\n"
            + "                    \"StartReceive\":\n"
            + "                      1389808323703006,\n"
            + "                    \"FinishReceive\":\n"
            + "                      1389808323703012,\n"
            + "                    \"SentReply\":\n"
            + "                      1389808323703294,\n"
            + "                    \"HostName\":\n"
            + "                      \"93-158-150-21.yandex.com\"\n"
            + "                  }\n"
            + "              }\n"
            + "          }\n"
            + "      }\n"
            + "    ]\n"
            + "}";

    private static final String _200_OK =
            "{\n" +
            "    \"Results\": [\n" +
            "        {\n" +
            "            \"Type\": \"AT_URL_FETCHING\",\n" +
            "            \"Data\": {\n" +
            "                \"Url\": \"http://vsdaria.com/dar/home.php\"\n" +
            "            },\n" +
            "            \"UrlFetchingResult\": {\n" +
            "                \"IsUrlAllowed\": true,\n" +
            "                \"Document\": \"HTTP/1.1 200 OK\\r\\nServer: uServ/3.2.2\\r\\nDate: Fri, 24 May 2013 15:23:12 GMT\\r\\nContent-Type: text/html; charset=UTF-8\\r\\n\\r\\n\",\n" +
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

    private static final String WMCON_5781_1 = "{\n" +
            "  \"Results\": [\n" +
            "    {\n" +
            "      \"Type\": \"AT_URL_FETCHING\",\n" +
            "      \"Data\": {\"Url\": \"http://cikv.ru\"},\n" +
            "      \"Errors\": [\n" +
            "        {\n" +
            "          \"Source\": \"ZORA\",\n" +
            "          \"Description\": \"[document]\",\n" +
            "          \"SitaError\": null,\n" +
            "          \"MsgBusError\": null,\n" +
            "          \"KiwiError\": null,\n" +
            "          \"ZoraError\": {\n" +
            "            \"Type\": \"FETCH_ERROR\",\n" +
            "            \"FetchStatus\": \"REDIR\",\n" +
            "            \"DataError\": null\n" +
            "          },\n" +
            "          \"MirrorsDbError\": null,\n" +
            "          \"QuotaError\": null,\n" +
            "          \"GeminiError\": null,\n" +
            "          \"StickerError\": null\n" +
            "        }\n" +
            "      ],\n" +
            "      \"UrlFetchingResult\": {\n" +
            "        \"IsUrlAllowed\": null,\n" +
            "        \"Ip4\": 1839716380,\n" +
            "        \"HttpCode\": 302,\n" +
            "        \"MimeType\": null,\n" +
            "        \"Encoding\": null,\n" +
            "        \"Language\": null,\n" +
            "        \"Times\": {\n" +
            "          \"ZoraInfo\": null,\n" +
            "          \"SpiderInfo\": {\n" +
            "            \"GotRequest\": 1413810998491110,\n" +
            "            \"StartSend\": 1413810998493746,\n" +
            "            \"FinishSend\": 1413810998516679,\n" +
            "            \"StartReceive\": 1413810998542665,\n" +
            "            \"FinishReceive\": 1413810998542670,\n" +
            "            \"StartCalc\": null,\n" +
            "            \"FinishCalc\": null,\n" +
            "            \"SentReply\": 1413810998542987,\n" +
            "            \"HostName\": null\n" +
            "          },\n" +
            "          \"LogicdocInfo\": null\n" +
            "        },\n" +
            "        \"readableEncoding\": \"UNKNOWN\",\n" +
            "        \"readableLanguage\": \"UNKNOWN\",\n" +
            "        \"readableMimeType\": \"UNKNOWN\",\n" +
            "        \"Document\": \"HTTP/1.1 302 Moved Temporarily\\r\\nServer: Apache-Coyote/1.1\\r\\nsequence: 124889\\r\\nSet-Cookie: JSESSIONID=A60444138F3FDC53CA7A2D16DC077D60; Path=/\\r\\nLocation: /%D0%90%D0%BD%D0%B0%D0%BB%D0%B8%D0%B7_%D0%B2%D0%BE%D0%B4%D1%8B_%D0%9F%D0%B5%D1%82%D0%B5%D1%80%D0%B1%D1%83%D1%80%D0%B3\\r\\nConnection: close\\r\\nContent-Length: 0\\r\\nDate: Mon, 20 Oct 2014 13:16:38 GMT\"\n" +
            "      },\n" +
            "      \"UrlInfoReadingResult\": null,\n" +
            "      \"MirroringResult\": null\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Errors\": null\n" +
            "}";

    private static final String ZORA_IMPLICIT_REDIRECT = "{\n"
            + "  \"Results\":\n"
            + "    [\n"
            + "      {\n"
            + "        \"Type\":\"AT_URL_FETCHING\",\n"
            + "        \"Data\":\n"
            + "          {\n"
            + "            \"Url\":\"http://conf.cloudjcup.com\"\n"
            + "          },\n"
            + "        \"Errors\":\n"
            + "          [\n"
            + "            {\n"
            + "              \"Source\":\"ZORA\",\n"
            + "              \"Description\":\"[document]\",\n"
            + "              \"ZoraError\":\n"
            + "                {\n"
            + "                  \"Type\":\"FETCH_ERROR\",\n"
            + "                  \"FetchStatus\":\"REDIR\",\n"
            + "                  \"CrawlDelay\":40,\n"
            + "                  \"Ip\":3343859209,\n"
            + "                  \"HttpCode\":303\n"
            + "                }\n"
            + "            }\n"
            + "          ],\n"
            + "        \"UrlFetchingResult\":\n"
            + "          {\n"
            + "            \"IsUrlAllowed\":true,\n"
            + "            \"Document\":\"HTTP/1.1 200 OK\\r\\nDate: Tue, 21 Apr 2015 14:25:21 GMT\\r\\nServer: Apache Phusion_Passenger/4.0.10 mod_bwlimited/1.4 mod_fcgid/2.3.9\\r\\nX-Powered-By: PHP/5.3.28\\r\\nRefresh: 0; url=http://conf.cloudjcup.com/index.php/msid/index\\r\\nSet-Cookie: OCSSID=13adb9a4c433e5737220a3539c3b198a; path=/\\r\\nContent-Length: 0\\r\\nKeep-Alive: timeout=3, max=30\\r\\nConnection: Keep-Alive\\r\\nContent-Type: text/html\\r\\n\\r\\n\",\n"
            + "            \"RobotsTxt\":\"User-agent: *\\nDisallow: cache/\\n\",\n"
            + "            \"Ip4\":3343859209,\n"
            + "            \"HttpCode\":303,\n"
            + "            \"RedirTarget\":\"http://conf.cloudjcup.com/index.php/msid/index\",\n"
            + "            \"Times\":\n"
            + "              {\n"
            + "                \"SpiderInfo\":\n"
            + "                  {\n"
            + "                    \"GotRequest\":1429626321752305,\n"
            + "                    \"StartSend\":1429626321804918,\n"
            + "                    \"FinishSend\":1429626321879535,\n"
            + "                    \"StartReceive\":1429626321879535,\n"
            + "                    \"FinishReceive\":1429626321881711,\n"
            + "                    \"SentReply\":1429626321882291,\n"
            + "                    \"FetcherName\":\"100-43-90-6.yandex.com\"\n"
            + "                  }\n"
            + "              }\n"
            + "          }\n"
            + "      }\n"
            + "    ],\n"
            + "  \"StartedAt\":1429626319,\n"
            + "  \"FinishedAt\":1429626321\n"
            + "}";

    @Test
    public void testWrongRedirect() throws Exception {
        SitaService sitaService = new SitaService() {
            int questCount = 0;
            @Override
            public SitaUrlFetchResponse request(SitaUrlFetchRequest urlFetchRequest) throws InternalException {
                if (questCount == 0) {
                    Assert.assertEquals("http://vsdaria.com/", urlFetchRequest.getUri().toASCIIString());
                    questCount = 1;
                    try {
                        return SitaUrlFetchJsonResponseTest.toResponse(_302_REDIRECT);
                    } catch (IOException e) {
                        throw new SitaException("Exception", e);
                    }

                } else if (questCount == 1) {
                    Assert.assertEquals("http://vsdaria.com/dar/home.php", urlFetchRequest.getUri().toASCIIString());
                    questCount = 2;
                    try {
                        return SitaUrlFetchJsonResponseTest.toResponse(_200_OK);
                    } catch (IOException e) {
                        throw new SitaException("Exception", e);
                    }
                }
                throw new SitaException("Illegal state");
            }
        };

        SitaRedirectService sitaRedirectService;
        sitaRedirectService = new SitaRedirectService();

        MetricRegistry mr = new MetricRegistry();
        sitaRedirectService.setMetricRegistry(mr);

        sitaRedirectService.setNewSitaService(sitaService);

        SitaUrlFetchRequest request =
                new SitaUrlFetchRequestBuilder(new URL("http://vsdaria.com/"))
                        .setDocumentFormat(DocumentFormatEnum.DF_HTTP_RESPONSE)
                        .createSitaUrlFetchRequest();
        SitaUrlFetchResponse response = sitaRedirectService.followRedirects(request);

        Assert.assertEquals(YandexHttpStatus.HTTP_200_OK, response.getSitaHttpStatus());
    }

    @Test
    public void testWMCON5781() throws Exception {
        SitaService sitaService = new SitaService() {
            int questCount = 0;
            @Override
            public SitaUrlFetchResponse request(SitaUrlFetchRequest urlFetchRequest) throws InternalException {
                if (questCount == 0) {
                    Assert.assertEquals("http://cikv.ru/", urlFetchRequest.getUri().toASCIIString());
                    questCount = 1;
                    try {
                        return SitaUrlFetchJsonResponseTest.toResponse(WMCON_5781_1);
                    } catch (IOException e) {
                        throw new SitaException("Exception", e);
                    }

                } else if (questCount == 1) {
                    Assert.assertEquals(
                            "http://cikv.ru/%D0%90%D0%BD%D0%B0%D0%BB%D0%B8%D0%B7_%D0%B2%D0%BE%D0%B4%D1%8B_%D0%9F%D0%B5%D1%82%D0%B5%D1%80%D0%B1%D1%83%D1%80%D0%B3",
                            urlFetchRequest.getUri().toASCIIString()
                    );
                    questCount = 2;
                    try {
                        return SitaUrlFetchJsonResponseTest.toResponse(_200_OK);
                    } catch (IOException e) {
                        throw new SitaException("Exception", e);
                    }
                }
                throw new SitaException("Illegal state");
            }
        };

        SitaRedirectService sitaRedirectService;
        sitaRedirectService = new SitaRedirectService();

        MetricRegistry mr = new MetricRegistry();
        sitaRedirectService.setMetricRegistry(mr);

        sitaRedirectService.setNewSitaService(sitaService);

        SitaUrlFetchRequest request =
                new SitaUrlFetchRequestBuilder(new URL("http://cikv.ru/"))
                        .setDocumentFormat(DocumentFormatEnum.DF_HTTP_RESPONSE)
                        .createSitaUrlFetchRequest();
        SitaUrlFetchResponse response = sitaRedirectService.followRedirects(request);

        Assert.assertEquals(YandexHttpStatus.HTTP_200_OK, response.getSitaHttpStatus());
    }

    @Test
    public void testWMCON5928() throws Exception {
        SitaService sitaService = new SitaService() {
            int questCount = 0;
            @Override
            public SitaUrlFetchResponse request(SitaUrlFetchRequest urlFetchRequest) throws InternalException {
                if (questCount == 0) {
                    Assert.assertEquals("http://conf.cloudjcup.com", urlFetchRequest.getUri().toASCIIString());
                    questCount = 1;
                    try {
                        return SitaUrlFetchJsonResponseTest.toResponse(ZORA_IMPLICIT_REDIRECT);
                    } catch (IOException e) {
                        throw new SitaException("Exception", e);
                    }

                } else if (questCount == 1) {
                    Assert.assertEquals("http://conf.cloudjcup.com/index.php/msid/index", urlFetchRequest.getUri().toASCIIString());
                    questCount = 2;
                    try {
                        return SitaUrlFetchJsonResponseTest.toResponse(_200_OK);
                    } catch (IOException e) {
                        throw new SitaException("Exception", e);
                    }
                }
                throw new SitaException("Illegal state");
            }
        };

        SitaRedirectService sitaRedirectService;
        sitaRedirectService = new SitaRedirectService();

        MetricRegistry mr = new MetricRegistry();
        sitaRedirectService.setMetricRegistry(mr);

        sitaRedirectService.setNewSitaService(sitaService);

        SitaUrlFetchRequest request =
                new SitaUrlFetchRequestBuilder(new URL("http://conf.cloudjcup.com"))
                        .setDocumentFormat(DocumentFormatEnum.DF_HTTP_RESPONSE)
                        .createSitaUrlFetchRequest();
        SitaUrlFetchResponse response = sitaRedirectService.followRedirects(request);

        Assert.assertEquals(YandexHttpStatus.HTTP_200_OK, response.getSitaHttpStatus());

    }
}
