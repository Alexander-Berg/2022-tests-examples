package ru.yandex.webmaster3.viewer.http.sitemap;

import java.net.URL;
import java.util.UUID;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.data.WebmasterUser;
import ru.yandex.webmaster3.core.events2.client.HostEventLogClient;
import ru.yandex.webmaster3.core.http.request.RequestId;
import ru.yandex.webmaster3.core.sitemap.UserSitemap;
import ru.yandex.webmaster3.core.sitemap.UserSitemapStatus;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.sitemap.SitemapUtils;
import ru.yandex.webmaster3.storage.sitemap.UserSitemapService;

/**
 * @author aherman
 */
public class AddUserSitemapActionTest {
    private URL HTTP_LENTA_RU_SITEMAP_XML;
    private String HTTP_LENTA_RU_SITEMAP_XML_STRING;

    @Before
    public void setUp() throws Exception {
        HTTP_LENTA_RU_SITEMAP_XML = new URL("http://lenta.ru/sitemap.xml");
        HTTP_LENTA_RU_SITEMAP_XML_STRING = HTTP_LENTA_RU_SITEMAP_XML.toExternalForm();
    }

    @Test
    public void testAdd() throws Exception {
        AddUserSitemapRequest request = new AddUserSitemapRequest();
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");

        UUID sitemapId = SitemapUtils.createSitemapId(HTTP_LENTA_RU_SITEMAP_XML_STRING);
        request.setSitemapUrl(HTTP_LENTA_RU_SITEMAP_XML);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setUserId(12345);
        WebmasterUser user = new WebmasterUser(12345);
        request.setWebmasterUser(user);
        request.setUserId(user.getUserId());
        request.setBalancerRequestId(new RequestId.BalancerRequestId("fuuuuu"));

        UserSitemap expectedUserSitemap =
                new UserSitemap(hostId, HTTP_LENTA_RU_SITEMAP_XML_STRING, sitemapId, DateTime.now(), false, null,
                        UserSitemapStatus.NEW);

        HostEventLogClient hostEventLogClient = EasyMock.createMock(HostEventLogClient.class);
        UserSitemapService userSitemapService = EasyMock.createMock(UserSitemapService.class);
        EasyMock.expect(userSitemapService.getUserSitemap(hostId, HTTP_LENTA_RU_SITEMAP_XML_STRING)).andReturn(null);
        EasyMock.expect(userSitemapService.addUserSitemap(hostId, HTTP_LENTA_RU_SITEMAP_XML_STRING, user))
                .andReturn(expectedUserSitemap);
        hostEventLogClient.log(EasyMock.anyObject());
        EasyMock.expectLastCall();

        EasyMock.replay(userSitemapService, hostEventLogClient);

        AddUserSitemapAction action = new AddUserSitemapAction();
        action.setUserSitemapService(userSitemapService);
        action.setHostEventLogClient(hostEventLogClient);
        AddUserSitemapResponse response = action.process(request);

        Assert.assertTrue(response instanceof AddUserSitemapResponse.NormalResponse);
        AddUserSitemapResponse.NormalResponse normalResponse = (AddUserSitemapResponse.NormalResponse) response;

        UserSitemap userSitemap = normalResponse.getUserSitemap();
        Assert.assertEquals(HTTP_LENTA_RU_SITEMAP_XML_STRING, userSitemap.getSitemapUrl());
        Assert.assertEquals(sitemapId, userSitemap.getSitemapId());
        EasyMock.verify(userSitemapService, hostEventLogClient);
    }

    @Test
    public void testAlreadyAdded() throws Exception {
        AddUserSitemapRequest request = new AddUserSitemapRequest();
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");

        UUID sitemapId = SitemapUtils.createSitemapId(HTTP_LENTA_RU_SITEMAP_XML_STRING);
        request.setSitemapUrl(HTTP_LENTA_RU_SITEMAP_XML);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setUserId(12345);
        WebmasterUser user = new WebmasterUser(12345);
        request.setWebmasterUser(user);

        UserSitemap expectedUserSitemap =
                new UserSitemap(hostId, HTTP_LENTA_RU_SITEMAP_XML_STRING, sitemapId, DateTime.now(), false, null,
                        UserSitemapStatus.NEW);

        UserSitemapService userSitemapService = EasyMock.createMock(UserSitemapService.class);
        EasyMock.expect(userSitemapService.getUserSitemap(hostId, HTTP_LENTA_RU_SITEMAP_XML_STRING)).andReturn(expectedUserSitemap);
        EasyMock.replay(userSitemapService);

        AddUserSitemapAction action = new AddUserSitemapAction();
        action.setUserSitemapService(userSitemapService);
        AddUserSitemapResponse response = action.process(request);

        Assert.assertTrue(response instanceof AddUserSitemapResponse.NormalResponse);
        EasyMock.verify(userSitemapService);
    }
}
