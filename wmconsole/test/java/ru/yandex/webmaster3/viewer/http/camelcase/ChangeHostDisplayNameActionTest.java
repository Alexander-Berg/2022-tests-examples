package ru.yandex.webmaster3.viewer.http.camelcase;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.storage.host.HostDataState;
import ru.yandex.webmaster3.storage.host.moderation.camelcase.DisplayNameRequest;
import ru.yandex.webmaster3.viewer.W3CheckHostNameService;

import java.util.UUID;

/**
 * User: azakharov
 * Date: 01.08.14
 * Time: 11:56
 */
@Ignore
public class ChangeHostDisplayNameActionTest {
    @Test
    public void testIDNHostChange() throws Exception {
        ChangeHostDisplayNameAction action = new ChangeHostDisplayNameAction();

        HostDisplayNameTestUtil.Context context = HostDisplayNameTestUtil.createMockContext();
        action.setHostDisplayNameService(context.hostDisplayNameService);

        W3CheckHostNameService checkHostNameService = new W3CheckHostNameService();
        action.setW3checkHostNameService(checkHostNameService);

        ChangeHostDisplayNameRequest request = new ChangeHostDisplayNameRequest();

        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "xn--e1afmkfd.xn--p1ai", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setUserId(777);

        action.process(request);
    }

    @Test
    public void testInvalidDisplayName() throws Exception {
        ChangeHostDisplayNameAction action = new ChangeHostDisplayNameAction();

        HostDisplayNameTestUtil.Context context = HostDisplayNameTestUtil.createMockContext();

        action.setHostDisplayNameService(context.hostDisplayNameService);

        W3CheckHostNameService checkHostNameService = new W3CheckHostNameService();
        action.setW3checkHostNameService(checkHostNameService);

        ChangeHostDisplayNameRequest request = new ChangeHostDisplayNameRequest();

        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "tutu.ru", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setUserId(777);
        request.setDisplayName("http://yyy.zzz");

        action.process(request);
    }

    @Test
    public void testCamelCaseHostNameDoesntMatchHostName() throws Exception {
        ChangeHostDisplayNameAction action = new ChangeHostDisplayNameAction();

        HostDisplayNameTestUtil.Context context = HostDisplayNameTestUtil.createMockContext();
        action.setHostDisplayNameService(context.hostDisplayNameService);

        W3CheckHostNameService checkHostNameService = new W3CheckHostNameService();
        action.setW3checkHostNameService(checkHostNameService);

        ChangeHostDisplayNameRequest request = new ChangeHostDisplayNameRequest();
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "tutu.ru", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setUserId(777);
        request.setDisplayName("TyTy.ru");

        action.process(request);
    }

    @Test
    public void testSameDisplayName() throws Exception {
        ChangeHostDisplayNameAction action = new ChangeHostDisplayNameAction();

        HostDisplayNameTestUtil.Context context = HostDisplayNameTestUtil.createMockContext();
        action.setHostDisplayNameService(context.hostDisplayNameService);

        W3CheckHostNameService checkHostNameService = new W3CheckHostNameService();
        action.setW3checkHostNameService(checkHostNameService);

        UUID uuid = new UUID(1, 2);
        WebmasterHostId webmasterHostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "tutu.ru", 80);
        HostDataState hostDataState = new HostDataState(webmasterHostId, uuid, webmasterHostId, null, null, DateTime.now(), "TuTu.ru", null, null, null, null, null);
        //EasyMock.expect(context.mockHostService.getCurrentHostDataState(webmasterHostId)).andReturn(hostDataState);

        EasyMock.expect(context.mockHostDisplayNameModerationRequestsYDao.getDisplayNameRequest(webmasterHostId)).andReturn(null);

        EasyMock.expect(context.mockHostModeratedDisplayNameYDao.getLastRequest(webmasterHostId)).andReturn(null);

        EasyMock.replay(context.mockHostService, context.mockHostDisplayNameModerationRequestsYDao);

        ChangeHostDisplayNameRequest request = new ChangeHostDisplayNameRequest();

        request.setHostId(webmasterHostId.toStringId());
        request.setConvertedHostId(webmasterHostId);
        request.setUserId(777);
        request.setDisplayName("TuTu.ru");

        ChangeHostDisplayNameResponse response = action.process(request);

        Assert.assertTrue(response instanceof ChangeHostDisplayNameResponse.DisplayNameIsTheSameResponse);

        EasyMock.verify(context.mockHostService);
    }

    @Test
    public void testBadDisplayName() throws Exception {
        ChangeHostDisplayNameAction action = new ChangeHostDisplayNameAction();

        HostDisplayNameTestUtil.Context context = HostDisplayNameTestUtil.createMockContext();
        action.setHostDisplayNameService(context.hostDisplayNameService);

        W3CheckHostNameService checkHostNameService = new W3CheckHostNameService();
        checkHostNameService.setGeoListResource(new ClassPathResource("/webmaster-common-checkhostname-geolist.txt"));
        checkHostNameService.init();
        action.setW3checkHostNameService(checkHostNameService);

        UUID uuid = new UUID(1, 2);

        WebmasterHostId webmasterHostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "test.hotmail.ru", 80);
        HostDataState hostDataState = new HostDataState(webmasterHostId, uuid, webmasterHostId, null, null, DateTime.now(), "test.hotmail.ru", null, null, null, null, null);
        //EasyMock.expect(context.mockHostService.getCurrentHostDataState(webmasterHostId)).andReturn(hostDataState);

        EasyMock.expect(context.mockHostDisplayNameModerationRequestsYDao.getDisplayNameRequest(webmasterHostId)).andReturn(null).times(1);

        EasyMock.expect(context.mockHostModeratedDisplayNameYDao.getLastRequest(webmasterHostId)).andReturn(null);

        EasyMock.replay(context.mockHostService, context.mockHostDisplayNameModerationRequestsYDao, context.mockHostModeratedDisplayNameYDao);

        ChangeHostDisplayNameRequest request = new ChangeHostDisplayNameRequest();

        request.setHostId(webmasterHostId.toStringId());
        request.setConvertedHostId(webmasterHostId);
        request.setUserId(777);
        request.setDisplayName("test.Hotmail.ru");

        ChangeHostDisplayNameResponse response = action.process(request);

        Assert.assertTrue(response instanceof ChangeHostDisplayNameResponse.InvalidDisplayNameResponse);

        EasyMock.verify(context.mockHostService, context.mockHostDisplayNameModerationRequestsYDao, context.mockHostModeratedDisplayNameYDao);
    }

    @Test
    public void testNeedModeration() throws Exception {
        ChangeHostDisplayNameAction action = new ChangeHostDisplayNameAction();

        HostDisplayNameTestUtil.Context context = HostDisplayNameTestUtil.createMockContext();
        action.setHostDisplayNameService(context.hostDisplayNameService);

        WebmasterHostId webmasterHostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "born.inussr.ru", 80);

        EasyMock.expect(context.mockHostDisplayNameModerationRequestsYDao.getDisplayNameRequest(webmasterHostId)).andReturn(null).times(1);

        EasyMock.expect(context.mockHostModeratedDisplayNameYDao.getLastRequest(webmasterHostId)).andReturn(null);

        context.mockHostDisplayNameModerationRequestsYDao.saveDisplayNameRequest(EasyMock.anyObject(DisplayNameRequest.class));
        EasyMock.expectLastCall();

        W3CheckHostNameService checkHostNameService = new W3CheckHostNameService();
        checkHostNameService.setGeoListResource(new ClassPathResource("/webmaster-common-checkhostname-geolist.txt"));
        checkHostNameService.init();
        action.setW3checkHostNameService(checkHostNameService);

        UUID uuid = new UUID(1, 2);

        HostDataState hostDataState = new HostDataState(webmasterHostId, uuid, webmasterHostId, null, null, DateTime.now(), "born.inussr.ru", null, null, null, null, null);
        //EasyMock.expect(context.mockHostService.getCurrentHostDataState(webmasterHostId)).andReturn(hostDataState);

        EasyMock.replay(context.mockHostService, context.mockHostDisplayNameModerationRequestsYDao, context.mockHostModeratedDisplayNameYDao);

        ChangeHostDisplayNameRequest request = new ChangeHostDisplayNameRequest();

        request.setHostId(webmasterHostId.toStringId());
        request.setConvertedHostId(webmasterHostId);
        request.setUserId(777);
        request.setDisplayName("born.inUSSR.ru");

        ChangeHostDisplayNameResponse response = action.process(request);

        Assert.assertTrue(response instanceof ChangeHostDisplayNameResponse.NormalResponse);

        EasyMock.verify(context.mockHostService, context.mockHostDisplayNameModerationRequestsYDao, context.mockHostModeratedDisplayNameYDao);
    }

    @Test
    public void testGoodDisplayName() throws Exception {
        ChangeHostDisplayNameAction action = new ChangeHostDisplayNameAction();

        HostDisplayNameTestUtil.Context context = HostDisplayNameTestUtil.createMockContext();
        action.setHostDisplayNameService(context.hostDisplayNameService);

        W3CheckHostNameService checkHostNameService = new W3CheckHostNameService();
        checkHostNameService.setGeoListResource(new ClassPathResource("/webmaster-common-checkhostname-geolist.txt"));
        checkHostNameService.init();
        action.setW3checkHostNameService(checkHostNameService);

        UUID uuid = new UUID(1, 2);

        WebmasterHostId webmasterHostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "www.altavista.com", 80);
        HostDataState hostDataState = new HostDataState(webmasterHostId, uuid, webmasterHostId, null, null, DateTime.now(), "www.altavista.com", null, null, null, null, null);

        //EasyMock.expect(context.mockHostService.getCurrentHostDataState(webmasterHostId)).andReturn(hostDataState);
        EasyMock.expect(context.mockHostDisplayNameModerationRequestsYDao.getDisplayNameRequest(webmasterHostId)).andReturn(null).times(1);
        EasyMock.expect(context.mockHostModeratedDisplayNameYDao.getLastRequest(webmasterHostId)).andReturn(null);
        context.mockHostModeratedDisplayNameYDao.saveRequest(EasyMock.anyObject(DisplayNameRequest.class));
        EasyMock.expectLastCall();

        context.mockHostDisplayNameModerationRequestsYDao.saveDisplayNameRequest(EasyMock.anyObject(DisplayNameRequest.class));
        EasyMock.expectLastCall();

        EasyMock.replay(context.mockHostService, context.mockHostDisplayNameModerationRequestsYDao, context.mockHostModeratedDisplayNameYDao);

        ChangeHostDisplayNameRequest request = new ChangeHostDisplayNameRequest();

        request.setHostId(webmasterHostId.toStringId());
        request.setConvertedHostId(webmasterHostId);
        request.setUserId(777);
        request.setDisplayName("AltaVista");

        ChangeHostDisplayNameResponse response = action.process(request);

        Assert.assertTrue(response instanceof ChangeHostDisplayNameResponse.NormalResponse);

        EasyMock.verify(context.mockHostService, context.mockHostDisplayNameModerationRequestsYDao, context.mockHostModeratedDisplayNameYDao);
    }
}
