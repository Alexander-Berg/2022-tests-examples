package ru.yandex.webmaster3.storage.robotstxt;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.events.service.WMCEventsService;
import ru.yandex.webmaster3.storage.util.ydb.exception.WebmasterYdbException;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author akhazhoyan 11/2018
 */
@RunWith(MockitoJUnitRunner.class)
public class RobotsTxtServiceTest {

    private static final WebmasterHostId HOST_ID = IdUtils.urlToHostId("https://ya.ru");
    private static final long MILLIS = 1543408000000L;
    private static final Instant INSTANT = new Instant(MILLIS);
    private static final String CONTENT = "content";
    private static final List<RobotTxtInfo> INFOS = List.of(new RobotTxtInfo(HOST_ID, MILLIS, CONTENT));


    @Mock private RobotsTxtHistoryYDao robotsTxtHistoryYDao;
    @Mock private RobotsTxtSentNotificationsYDao robotsTxtSentNotificationsYDao;
    @Mock private WMCEventsService wmcEventsService;

    @InjectMocks private RobotsTxtService robotsTxtService;

    @Test
    public void testTryAddRobotsTxtWrongTimestamp() {
        {
            try {
                new RobotTxtInfo(HOST_ID, 0, CONTENT);
                fail("Exception expected");
            } catch (Exception e) {
                // okay
            }

            verifyNoMoreInteractions(robotsTxtHistoryYDao);
        }
        {
            try {
                new RobotTxtInfo(HOST_ID, 99999999999999L, CONTENT);
                fail("Exception expected");
            } catch (Exception e) {
                // okay
            }

            verifyNoMoreInteractions(robotsTxtHistoryYDao);
        }
    }

    @Test
    public void testTryAddRobotsTxtContentSuccess() {
        robotsTxtService.addRobotsTxt(INFOS);

        verify(robotsTxtHistoryYDao).batchInsert(INFOS);
        verifyNoMoreInteractions(robotsTxtHistoryYDao);
    }

    @Test
    public void testTryAddRobotsTxtFailure() {
        doThrow(new WebmasterYdbException("")).when(robotsTxtHistoryYDao).batchInsert(INFOS);

        try {
            robotsTxtService.addRobotsTxt(INFOS);
            fail("Exception expected");
        } catch (Exception e) {
            // okay
        }

        verify(robotsTxtHistoryYDao).batchInsert(INFOS);
        verifyNoMoreInteractions(robotsTxtHistoryYDao);
    }

    @Test
    public void testSendRobotsTxtNotificationAlreadySent() {
        long userId = 123L;
        String table = "2018-12-01";
        DateTime date = DateTime.parse(table);
        when(robotsTxtSentNotificationsYDao.getSentNotificationByTableId(date))
                .thenReturn(Set.of(Pair.of(HOST_ID, userId)));

        robotsTxtService.sendRobotsTxtNotification(List.of(Pair.of(HOST_ID, userId)), table);

        verify(robotsTxtSentNotificationsYDao).getSentNotificationByTableId(date);
        verifyNoMoreInteractions(robotsTxtSentNotificationsYDao, wmcEventsService, robotsTxtHistoryYDao);
    }

    @Test
    public void testSendRobotsTxtNotificationNotSentYet() {
        long userId = 123L;
        String table = "2018-12-01";
        DateTime date = DateTime.parse(table);

        when(robotsTxtSentNotificationsYDao.getSentNotificationByTableId(date))
                .thenReturn(Set.of());

        robotsTxtService.sendRobotsTxtNotification(List.of(Pair.of(HOST_ID, userId)), table);

        verify(robotsTxtSentNotificationsYDao).getSentNotificationByTableId(date);
        verify(robotsTxtSentNotificationsYDao).batchInsert(List.of(Triple.of(HOST_ID, userId, date)));
        verify(wmcEventsService).addEventContents(anyList());
        verifyNoMoreInteractions(robotsTxtSentNotificationsYDao, wmcEventsService, robotsTxtHistoryYDao);
    }
}
