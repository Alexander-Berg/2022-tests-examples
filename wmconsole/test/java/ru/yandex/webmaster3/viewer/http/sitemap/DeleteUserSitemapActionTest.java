package ru.yandex.webmaster3.viewer.http.sitemap;

import java.util.UUID;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Assert;
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
public class DeleteUserSitemapActionTest {
    private static final String HTTP_LENTA_RU_SITEMAP_XML = "http://lenta.ru/sitemap.xml";

    @Test
    public void testDelete() throws Exception {
        DeleteUserSitemapRequest request = new DeleteUserSitemapRequest();
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");

        UUID sitemapId = SitemapUtils.createSitemapId(HTTP_LENTA_RU_SITEMAP_XML);
        request.setSitemapId(sitemapId);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setUserId(12345);
        WebmasterUser user = new WebmasterUser(12345);
        request.setWebmasterUser(user);
        request.setUserId(user.getUserId());
        request.setBalancerRequestId(new RequestId.BalancerRequestId("fuuuuu"));

        UserSitemap expectedUserSitemap = new UserSitemap(hostId, HTTP_LENTA_RU_SITEMAP_XML, sitemapId, DateTime
                .now(), false, null, UserSitemapStatus.NEW);

        HostEventLogClient hostEventLogClient = EasyMock.createMock(HostEventLogClient.class);
        UserSitemapService userSitemapService = EasyMock.createMock(UserSitemapService.class);

        EasyMock.expect(userSitemapService.getUserSitemap(hostId, sitemapId)).andReturn(expectedUserSitemap);
        hostEventLogClient.log(EasyMock.anyObject());
        EasyMock.expectLastCall();

        userSitemapService.deleteUserSitemap(hostId, sitemapId);
        EasyMock.replay(userSitemapService);

        DeleteUserSitemapAction action = new DeleteUserSitemapAction();
        action.setUserSitemapService(userSitemapService);
        action.setHostEventLogClient(hostEventLogClient);
        DeleteUserSitemapResponse response = action.process(request);

        Assert.assertTrue(response instanceof DeleteUserSitemapResponse.NormalResponse);

        EasyMock.verify(userSitemapService);
    }

    @Test
    public void testSitemapNotFound() throws Exception {
        DeleteUserSitemapRequest request = new DeleteUserSitemapRequest();
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");

        UUID sitemapId = SitemapUtils.createSitemapId(HTTP_LENTA_RU_SITEMAP_XML);
        request.setSitemapId(sitemapId);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setUserId(12345);
        WebmasterUser user = new WebmasterUser(12345);
        request.setWebmasterUser(user);

        UserSitemapService userSitemapService = EasyMock.createMock(UserSitemapService.class);
        EasyMock.expect(userSitemapService.getUserSitemap(hostId, sitemapId)).andReturn(null);
        EasyMock.replay(userSitemapService);

        DeleteUserSitemapAction action = new DeleteUserSitemapAction();
        action.setUserSitemapService(userSitemapService);
        DeleteUserSitemapResponse response = action.process(request);

        Assert.assertTrue(response instanceof DeleteUserSitemapResponse.NormalResponse);

        EasyMock.verify(userSitemapService);
    }
}
