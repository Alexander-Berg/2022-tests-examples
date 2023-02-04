package ru.yandex.webmaster3.viewer.http.camelcase;

import java.util.UUID;

import com.datastax.driver.core.utils.UUIDs;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.storage.host.HostDataState;
import ru.yandex.webmaster3.storage.host.moderation.camelcase.DisplayNameRequest;
import ru.yandex.webmaster3.storage.host.moderation.camelcase.HostDisplayNameModerationRequestState;
import ru.yandex.webmaster3.viewer.W3CheckHostNameService;
import ru.yandex.webmaster3.viewer.http.camelcase.data.BeautyHostName;

/**
 * User: azakharov
 * Date: 01.08.14
 * Time: 15:01
 */
public class CancelHostDisplayNameChangeActionTest {
    @Test
    public void testCancelInProgress() throws Exception {
        HostDisplayNameTestUtil.Context context = HostDisplayNameTestUtil.createMockContext();

        CancelHostDisplayNameChangeAction action = new CancelHostDisplayNameChangeAction();

        action.setHostDisplayNameService(context.hostDisplayNameService);

        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "testsite.ru", 443);
        UUID requestId = UUIDs.timeBased();
        DisplayNameRequest displayNameRequest = DisplayNameRequest.builder()
            .hostId(hostId)
            .requestId(requestId)
            .displayName("TestSite.ru")
            .state(HostDisplayNameModerationRequestState.IN_PROGRESS)
            .creationDate(new DateTime())
            .modificationDate(new DateTime())
            .isUserClosedInfoBanner(false)
            .userId(0L)
            .assessorId(null)
            .build();

        EasyMock.expect(context.mockHostDisplayNameModerationRequestsYDao.getDisplayNameRequest(hostId)).andReturn(displayNameRequest);
        Capture<DisplayNameRequest> capturedArgument = new Capture<>();
        context.mockHostDisplayNameModerationRequestsYDao.saveDisplayNameRequest(
            EasyMock.and(EasyMock.capture(capturedArgument),
                EasyMock.isA(DisplayNameRequest.class)));
        EasyMock.expectLastCall().once();
        EasyMock.expect(context.displayNameService2.getDisplayName(hostId)).andReturn("testsite.ru");

        UUID uuid = UUID.randomUUID();

        HostDataState hostDataState =
            new HostDataState(hostId, uuid, hostId, null, null, DateTime.now(), "testsite.ru", null, null, null, null, null);
        //EasyMock.expect(context.mockHostService.getCurrentHostDataState(hostId)).andReturn(hostDataState);

        W3CheckHostNameService mockCheckHostNameService = EasyMock.createMock(W3CheckHostNameService.class);
        action.setW3checkHostNameService(mockCheckHostNameService);
        EasyMock.expect(mockCheckHostNameService.getDisplayName("testsite.ru"))
            .andReturn(new BeautyHostName(null, "testsite", "ru"));

        EasyMock.replay(context.mockHostDisplayNameModerationRequestsYDao, context.mockHostService, mockCheckHostNameService, context.displayNameService2);

        CancelHostDisplayNameChangeRequest request = new CancelHostDisplayNameChangeRequest();
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setUserId(777);
        request.setRequestId(requestId);

        CancelHostDisplayNameChangeResponse response = action.process(request);

        Assert.assertTrue(response instanceof CancelHostDisplayNameChangeResponse.NormalResponse);
        Assert.assertEquals(HostDisplayNameModerationRequestState.CANCELLED, capturedArgument.getValue().getState());
        Assert.assertEquals(displayNameRequest.getDisplayName(), capturedArgument.getValue().getDisplayName());

        EasyMock.verify(context.mockHostDisplayNameModerationRequestsYDao, mockCheckHostNameService, context.displayNameService2);
    }

    @Test
    public void testTryCancelAccepted() throws Exception {
        CancelHostDisplayNameChangeAction action = new CancelHostDisplayNameChangeAction();

        HostDisplayNameTestUtil.Context context = HostDisplayNameTestUtil.createMockContext();
        action.setHostDisplayNameService(context.hostDisplayNameService);

        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "testsite.ru", 443);
        UUID requestId = UUIDs.timeBased();
        DisplayNameRequest displayNameRequest = DisplayNameRequest.builder()
            .hostId(hostId)
            .requestId(requestId)
            .displayName("TestSite.ru")
            .state(HostDisplayNameModerationRequestState.ACCEPTED)
            .creationDate(new DateTime())
            .modificationDate(new DateTime())
            .isUserClosedInfoBanner(false)
            .userId(null)
            .assessorId("test")
            .build();

        EasyMock.expect(context.mockHostDisplayNameModerationRequestsYDao.getDisplayNameRequest(hostId)).andReturn(displayNameRequest);

        UUID uuid = UUID.randomUUID();

        HostDataState hostDataState =
            new HostDataState(hostId, uuid, hostId, null, null, DateTime.now(), "testsite.ru", null, null, null, null, null);
        //EasyMock.expect(context.mockHostService.getCurrentHostDataState(hostId)).andReturn(hostDataState);

        EasyMock.replay(context.mockHostDisplayNameModerationRequestsYDao, context.mockHostService);

        CancelHostDisplayNameChangeRequest request = new CancelHostDisplayNameChangeRequest();
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setUserId(777);
        request.setRequestId(requestId);

        CancelHostDisplayNameChangeResponse response = action.process(request);

        Assert.assertTrue(response instanceof CancelHostDisplayNameChangeResponse.UnableToCancelResponse);

        EasyMock.verify(context.mockHostDisplayNameModerationRequestsYDao);
    }

    @Test
    public void testCancelNoRequest() throws Exception {
        CancelHostDisplayNameChangeAction action = new CancelHostDisplayNameChangeAction();

        HostDisplayNameTestUtil.Context context = HostDisplayNameTestUtil.createMockContext();

        action.setHostDisplayNameService(context.hostDisplayNameService);

        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "testsite.ru", 443);
        EasyMock.expect(context.mockHostDisplayNameModerationRequestsYDao.getDisplayNameRequest(hostId)).andReturn(null);

        UUID uuid = UUID.randomUUID();

        HostDataState hostDataState =
            new HostDataState(hostId, uuid, hostId, null, null, DateTime.now(), "testsite.ru", null, null, null, null, null);
        //EasyMock.expect(context.mockHostService.getCurrentHostDataState(hostId)).andReturn(hostDataState);

        EasyMock.replay(context.mockHostDisplayNameModerationRequestsYDao, context.mockHostService);

        CancelHostDisplayNameChangeRequest request = new CancelHostDisplayNameChangeRequest();
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setUserId(777);
        request.setRequestId(UUIDs.timeBased());

        CancelHostDisplayNameChangeResponse response = action.process(request);

        Assert.assertTrue(response instanceof CancelHostDisplayNameChangeResponse.RequestNotFoundResponse);

        EasyMock.verify(context.mockHostDisplayNameModerationRequestsYDao);
    }
}
