package ru.yandex.webmaster3.viewer.http.camelcase;

import com.datastax.driver.core.utils.UUIDs;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.storage.host.HostDataState;
import ru.yandex.webmaster3.storage.host.moderation.camelcase.DisplayNameRequest;
import ru.yandex.webmaster3.storage.host.moderation.camelcase.HostDisplayNameModerationRequestState;
import ru.yandex.webmaster3.storage.host.moderation.camelcase.dao.HostDisplayNameModerationRequestsYDao;
import ru.yandex.webmaster3.storage.host.moderation.camelcase.dao.HostModeratedDisplayNameYDao;
import ru.yandex.webmaster3.storage.host.moderation.camelcase.service.DisplayNameService2;
import ru.yandex.webmaster3.viewer.W3CheckHostNameService;
import ru.yandex.webmaster3.viewer.http.camelcase.data.BeautyHostName;

import java.util.UUID;


/**
 * User: azakharov
 * Date: 01.08.14
 * Time: 11:15
 */
public class HostDisplayNameInfoActionTest {
    @Test
    public void testNoDisplayNameRequest() throws Exception {

        W3CheckHostNameService mockCheckHostNameService = EasyMock.createMock(W3CheckHostNameService.class);

        HostDisplayNameModerationRequestsYDao mockHostDisplayNameModerationRequestsYDao = EasyMock.createMock(HostDisplayNameModerationRequestsYDao.class);

        HostModeratedDisplayNameYDao mockHostModeratedDisplayNameYDao = EasyMock.createMock(HostModeratedDisplayNameYDao.class);

        DisplayNameService2 displayNameService2 = EasyMock.createMock(DisplayNameService2.class);
        HostDisplayNameInfoAction action = new HostDisplayNameInfoAction(
                mockCheckHostNameService,
                mockHostDisplayNameModerationRequestsYDao,
                mockHostModeratedDisplayNameYDao,
                displayNameService2);


        UUID uuid = new UUID(1, 2);

        WebmasterHostId webmasterHostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "actiontest.ru", 80);
        EasyMock.expect(displayNameService2.getDisplayName(webmasterHostId)).andReturn("ActiOnTest.ru");
        HostDataState hostDataState =
                new HostDataState(webmasterHostId, uuid, webmasterHostId, null, null, DateTime.now(), "ActiOnTest.ru", null, null, null, null, null);

        EasyMock.expect(mockHostDisplayNameModerationRequestsYDao.getDisplayNameRequest(EasyMock.anyObject(WebmasterHostId.class))).andReturn(null);

        EasyMock.expect(mockHostModeratedDisplayNameYDao.getLastRequest(EasyMock.anyObject(WebmasterHostId.class))).andReturn(null);

        BeautyHostName current = new BeautyHostName(null, "ActiOnTest", "ru");
        EasyMock.expect(mockCheckHostNameService.getDisplayName("ActiOnTest.ru")).andReturn(current);

        EasyMock.replay(mockHostDisplayNameModerationRequestsYDao, mockHostModeratedDisplayNameYDao,
                mockCheckHostNameService, displayNameService2);

        HostDisplayNameInfoRequest request = new HostDisplayNameInfoRequest();
        request.setHostId(webmasterHostId.toStringId());
        request.setConvertedHostId(webmasterHostId);
        request.setUserId(777);
        request.setHostDataState(hostDataState);

        HostDisplayNameInfoResponse resp = action.process(request);
        Assert.assertTrue(resp instanceof HostDisplayNameInfoResponse.NormalResponse);

        HostDisplayNameInfoResponse.NormalResponse response = (HostDisplayNameInfoResponse.NormalResponse) resp;
        Assert.assertNull(response.getDisplayNameRequest());
        Assert.assertNotNull(response.getCurrentHostState());
        Assert.assertEquals("ActiOnTest.ru", response.getCurrentHostState().getBeautyHostName().toFullDisplayName());

        EasyMock.verify(mockHostDisplayNameModerationRequestsYDao, mockHostModeratedDisplayNameYDao,
                mockCheckHostNameService);
    }

    @Test
    public void testHasDisplayNameRequest() throws Exception {
        W3CheckHostNameService mockCheckHostNameService = EasyMock.createMock(W3CheckHostNameService.class);

        HostDisplayNameModerationRequestsYDao mockHostDisplayNameModerationRequestsYDao = EasyMock.createMock(HostDisplayNameModerationRequestsYDao.class);

        HostModeratedDisplayNameYDao mockHostModeratedDisplayNameYDao = EasyMock.createMock(HostModeratedDisplayNameYDao.class);

        DisplayNameService2 displayNameService2 = EasyMock.createMock(DisplayNameService2.class);
        HostDisplayNameInfoAction action = new HostDisplayNameInfoAction(
                mockCheckHostNameService,
                mockHostDisplayNameModerationRequestsYDao,
                mockHostModeratedDisplayNameYDao,
                displayNameService2);

        UUID uuid = new UUID(1, 2);

        WebmasterHostId webmasterHostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "actiontest.ru", 80);
        EasyMock.expect(displayNameService2.getDisplayName(webmasterHostId)).andReturn("ActiOnTest.ru");

        HostDataState hostDataState =
                new HostDataState(webmasterHostId, uuid, webmasterHostId, null, null, DateTime.now(), "ActiOnTest.ru", null, null, null, null, null);

        DisplayNameRequest displayNameRequest = DisplayNameRequest.builder()
                .hostId(webmasterHostId)
                .requestId(UUIDs.timeBased())
                .displayName("ActionTest.ru")
                .state(HostDisplayNameModerationRequestState.IN_PROGRESS)
                .creationDate(new DateTime())
                .modificationDate(new DateTime())
                .isUserClosedInfoBanner(false)
                .userId(0L)
                .assessorId(null)
                .build();
        EasyMock.expect(mockHostDisplayNameModerationRequestsYDao.getDisplayNameRequest(EasyMock.anyObject(WebmasterHostId.class)))
                .andReturn(displayNameRequest);

        BeautyHostName current = new BeautyHostName(null, "ActiOnTest", "ru");
        EasyMock.expect(mockCheckHostNameService.getDisplayName("ActiOnTest.ru")).andReturn(current);

        BeautyHostName displayName = new BeautyHostName(null, "ActionTest", "ru");
        EasyMock.expect(mockCheckHostNameService.getDisplayName(displayNameRequest.getDisplayName())).andReturn(displayName);

        EasyMock.replay(mockHostDisplayNameModerationRequestsYDao, mockCheckHostNameService, displayNameService2);

        HostDisplayNameInfoRequest request = new HostDisplayNameInfoRequest();
        request.setHostId(webmasterHostId.toStringId());
        request.setConvertedHostId(webmasterHostId);
        request.setUserId(777);
        request.setHostDataState(hostDataState);

        HostDisplayNameInfoResponse resp = action.process(request);
        Assert.assertTrue(resp instanceof HostDisplayNameInfoResponse.NormalResponse);

        HostDisplayNameInfoResponse.NormalResponse response = (HostDisplayNameInfoResponse.NormalResponse) resp;

        Assert.assertEquals(displayName, response.getDisplayNameRequest().getBeautyHostName());
        Assert.assertEquals(displayNameRequest.getState(), response.getDisplayNameRequest().getState());
        Assert.assertEquals(displayNameRequest.getCreationDate(), response.getDisplayNameRequest().getCreationDate());
        Assert.assertEquals(displayNameRequest.getModificationDate(), response.getDisplayNameRequest().getModificationDate());
        Assert.assertNotNull(response.getCurrentHostState());
        Assert.assertEquals("ActiOnTest.ru", response.getCurrentHostState().getBeautyHostName().toFullDisplayName());

        EasyMock.verify(mockHostDisplayNameModerationRequestsYDao, mockCheckHostNameService);
    }
}
