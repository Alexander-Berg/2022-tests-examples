package ru.yandex.wmtools.common.sita;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.util.collections.CollectionFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * User: azakharov
 * Date: 18.03.14
 * Time: 16:54
 */
public class SitaMirroringJsonRequestTest {

    private static final ObjectMapper OM = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    public void testRerank() throws URISyntaxException, IOException {
        String result = SitaMirroringJsonRequest.toJson(SitaMirroringRequest.createRerankRequest(new URI("http://yandex.ru"), new URI("http://www.yandex.ru")), "webmaster");
        SitaJson.TRequest request = OM.readValue(new StringReader(result), SitaJson.TRequest.class);
        Assert.assertNotNull("request is null", request);

        Assert.assertNotNull("request.AuthInfo is null", request.AuthInfo);
        Assert.assertNotNull("request.Data is null", request.Data);
        Assert.assertNotNull("request.Actions", request.Actions);
        Assert.assertNotNull("request.Settings", request.Settings);

        Assert.assertEquals("bad sita user", "webmaster", request.AuthInfo.User);

        Assert.assertEquals("bad url", "http://yandex.ru", request.Data[0].Url);

        Assert.assertEquals("bad action type", SitaJson.EActionType.AT_MIRRORING, request.Actions[0].Type);
        Assert.assertNotNull("request.Actions[0].MirroringData is null", request.Actions[0].MirroringData);
        Assert.assertEquals("bad mirroring action type", SitaJson.EMirroringAction.RERANK, request.Actions[0].MirroringData.Action);
        Assert.assertEquals("bad mirroring action newhost", "http://www.yandex.ru", request.Actions[0].MirroringData.NewMainHost);

        Assert.assertEquals("bad url validator", SitaJson.EUrlValidator.ROBOT_URL_VALIDATOR, request.Settings.UrlValidator);
    }

    @Test
    public void testStick() throws URISyntaxException, IOException {
        String result = SitaMirroringJsonRequest.toJson(SitaMirroringRequest.createStickRequest(
                new URI("http://www.yandex.ru"),
                CollectionFactory.list(new URI("http://www.yandex.ru"), new URI("http://yandex.ru"))),
                "webmaster");
        SitaJson.TRequest request = OM.readValue(new StringReader(result), SitaJson.TRequest.class);
        Assert.assertNotNull("request is null", request);

        Assert.assertNotNull("request.AuthInfo is null", request.AuthInfo);
        Assert.assertNotNull("request.Data is null", request.Data);
        Assert.assertNotNull("request.Actions", request.Actions);
        Assert.assertNotNull("request.Settings", request.Settings);

        Assert.assertEquals("bad sita user", "webmaster", request.AuthInfo.User);

        Assert.assertEquals("bad url", "http://www.yandex.ru", request.Data[0].Url);

        Assert.assertEquals("bad action type", SitaJson.EActionType.AT_MIRRORING, request.Actions[0].Type);
        Assert.assertNotNull("request.Actions[0].MirroringData is null", request.Actions[0].MirroringData);
        Assert.assertEquals("bad mirroring action type", SitaJson.EMirroringAction.STICK, request.Actions[0].MirroringData.Action);
        Assert.assertNotNull("request.Actions[0].MirroringData.Hosts is null", request.Actions[0].MirroringData.Hosts);
        Assert.assertEquals("bad mirroring hosts[0]", "http://www.yandex.ru", request.Actions[0].MirroringData.Hosts[0]);
        Assert.assertEquals("bad mirroring hosts[1]", "http://yandex.ru", request.Actions[0].MirroringData.Hosts[1]);

        Assert.assertEquals("bad url validator", SitaJson.EUrlValidator.ROBOT_URL_VALIDATOR, request.Settings.UrlValidator);
    }

    @Test
    public void testUnstick() throws URISyntaxException, IOException {
        String result = SitaMirroringJsonRequest.toJson(SitaMirroringRequest.createUnstickRequest(
                new URI("http://www.yandex.ru"),
                CollectionFactory.list(new URI("http://www.yandex.ru"), new URI("http://yandex.ru"))),
                "webmaster");
        SitaJson.TRequest request = OM.readValue(new StringReader(result), SitaJson.TRequest.class);
        Assert.assertNotNull("request is null", request);

        Assert.assertNotNull("request.AuthInfo is null", request.AuthInfo);
        Assert.assertNotNull("request.Data is null", request.Data);
        Assert.assertNotNull("request.Actions", request.Actions);
        Assert.assertNotNull("request.Settings", request.Settings);

        Assert.assertEquals("bad sita user", "webmaster", request.AuthInfo.User);

        Assert.assertEquals("bad url", "http://www.yandex.ru", request.Data[0].Url);

        Assert.assertEquals("bad action type", SitaJson.EActionType.AT_MIRRORING, request.Actions[0].Type);
        Assert.assertNotNull("request.Actions[0].MirroringData is null", request.Actions[0].MirroringData);
        Assert.assertEquals("bad mirroring action type", SitaJson.EMirroringAction.UNSTICK, request.Actions[0].MirroringData.Action);
        Assert.assertNotNull("request.Actions[0].MirroringData.Hosts is null", request.Actions[0].MirroringData.Hosts);
        Assert.assertEquals("bad mirroring hosts[0]", "http://www.yandex.ru", request.Actions[0].MirroringData.Hosts[0]);
        Assert.assertEquals("bad mirroring hosts[1]", "http://yandex.ru", request.Actions[0].MirroringData.Hosts[1]);

        Assert.assertEquals("bad url validator", SitaJson.EUrlValidator.ROBOT_URL_VALIDATOR, request.Settings.UrlValidator);
    }
}
