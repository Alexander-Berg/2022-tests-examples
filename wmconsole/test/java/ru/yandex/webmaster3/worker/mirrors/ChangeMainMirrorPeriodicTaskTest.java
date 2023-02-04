package ru.yandex.webmaster3.worker.mirrors;

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.utils.UUIDs;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Test;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.mirrors.data.MirrorActionEnum;
import ru.yandex.webmaster3.storage.mirrors.data.MirrorRequest;
import ru.yandex.webmaster3.storage.mirrors.data.MirrorRequestStateEnum;

/**
 * @author aherman
 */
public class ChangeMainMirrorPeriodicTaskTest {


    @Test
    public void testRecheckStalledRequests() throws Exception {
        ChangeMainMirrorPeriodicTask task = new ChangeMainMirrorPeriodicTask();
        MirrorRequestStateService mirrorRequestStateService = EasyMock.createMock(MirrorRequestStateService.class);
        task.setMirrorRequestStateService(mirrorRequestStateService);

        DateTime now = DateTime.now();

        WebmasterHostId hostId1 = IdUtils.urlToHostId("http://example1.com");
        WebmasterHostId hostId2 = IdUtils.urlToHostId("http://example2.com");
        WebmasterHostId hostId3 = IdUtils.urlToHostId("http://example3.com");

        List<MirrorRequest> mirrorRequests = new ArrayList<>();
        mirrorRequests.add(new MirrorRequest(hostId1, UUIDs.startOf(1000), MirrorRequestStateEnum.NEW, hostId1, hostId1, now.minusMinutes(10), now.minusMinutes(10), 0, null, MirrorActionEnum.MOVE, false));

        MirrorRequest mrNewStale =
                new MirrorRequest(hostId2, UUIDs.startOf(1002), MirrorRequestStateEnum.NEW, hostId2, hostId2,
                        now.minusHours(26), now.minusHours(25), 0, null, MirrorActionEnum.MOVE, false);
        mirrorRequests.add(mrNewStale);
        mirrorRequests.add(new MirrorRequest(hostId2, UUIDs.startOf(1001), MirrorRequestStateEnum.WAITING, hostId2, hostId2, now.minusHours(30), now.minusHours(30), 0, null, MirrorActionEnum.MOVE, false));

        mirrorRequests.add(new MirrorRequest(hostId3, UUIDs.startOf(1004), MirrorRequestStateEnum.WAITING, hostId3, hostId3, now.minusHours(26), now.minusHours(25), 0, null, MirrorActionEnum.MOVE, false));
        MirrorRequest mrNewInvalid =
                new MirrorRequest(hostId3, UUIDs.startOf(1003), MirrorRequestStateEnum.NEW, hostId3, hostId3,
                        now.minusHours(30), now.minusHours(30), 0, null, MirrorActionEnum.MOVE, false);
        mirrorRequests.add(mrNewInvalid);

        EasyMock.expect(mirrorRequestStateService.getAllMirrorRequests()).andReturn(mirrorRequests);

        EasyMock.expect(mirrorRequestStateService.executeMirrorRequest(EasyMock.eq(mrNewStale))).andReturn(true);

        mirrorRequestStateService.markRequestAsFailed(mrNewInvalid);
        EasyMock.expectLastCall();

        EasyMock.replay(mirrorRequestStateService);
        task.checkMirrorRequests();
        EasyMock.verify(mirrorRequestStateService);
    }
}
