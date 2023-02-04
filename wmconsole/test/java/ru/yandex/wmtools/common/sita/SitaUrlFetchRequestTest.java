package ru.yandex.wmtools.common.sita;

import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class SitaUrlFetchRequestTest {
    private static final String EXPECTED_JSON = "{\"AuthInfo\":{\"Type\":\"BASIC\",\"User\":\"proxy-user\"},\"Data\":[{\"Url\":\"http://cikv.ru/%D0%90%D0%BD%D0%B0%D0%BB%D0%B8%D0%B7_%D0%B2%D0%BE%D0%B4%D1%8B_%D0%9F%D0%B5%D1%82%D0%B5%D1%80%D0%B1%D1%83%D1%80%D0%B3\"}],\"Actions\":[{\"Type\":\"AT_URL_FETCHING\",\"UrlFetchingData\":{\"CheckIfAllowed\":true,\"DocumentFormat\":\"DF_HTTP_RESPONSE\",\"RobotsTxtFormat\":\"RF_NO_ROBOTS_TXT\"}}],\"Settings\":{\"UrlValidator\":\"ROBOT_URL_VALIDATOR\",\"OnlineZora\":{\"Timeout\":8,\"Freshness\":0,\"UserAgent\":\"Mozilla/5.0 (compatible; YandexWebmaster/2.0; +http://yandex.com/bots)\",\"User\":\"user\"}}}";

    @Test
    public void testWMCON_5781() throws Exception {
        SitaUrlFetchRequest sitaUrlFetchRequest = new SitaUrlFetchRequestBuilder(
                new URI("http://cikv.ru/Анализ_воды_Петербург"))
                .setDocumentFormat(DocumentFormatEnum.DF_HTTP_RESPONSE)
                .createSitaUrlFetchRequest();
        String json = SitaUrlFetchJsonRequest.toJson(sitaUrlFetchRequest, "user", "proxy-user");
        Assert.assertEquals(EXPECTED_JSON, json);
    }
}
