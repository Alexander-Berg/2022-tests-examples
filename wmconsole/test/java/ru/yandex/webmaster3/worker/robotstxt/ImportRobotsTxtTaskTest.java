package ru.yandex.webmaster3.worker.robotstxt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.user.UserVerifiedHost;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.host.AllVerifiedHostsCacheService;
import ru.yandex.webmaster3.storage.host.CommonDataState;
import ru.yandex.webmaster3.storage.robotstxt.RobotTxtInfo;
import ru.yandex.webmaster3.storage.robotstxt.RobotsTxtService;
import ru.yandex.webmaster3.storage.settings.SettingsService;
import ru.yandex.webmaster3.storage.user.service.UserHostsService;
import ru.yandex.webmaster3.storage.util.yt.YtCypressService;
import ru.yandex.webmaster3.storage.util.yt.YtPath;
import ru.yandex.webmaster3.storage.util.yt.YtService;
import ru.yandex.webmaster3.worker.robotstxt.ImportRobotsTxtTask.LocalState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.webmaster3.storage.host.CommonDataType.ROBOTS_TXT_LAST_TABLE;

/**
 * @author akhazhoyan 12/2018
 */
@RunWith(MockitoJUnitRunner.class)
public class ImportRobotsTxtTaskTest {
    private static final String HOST = "http://abc.example.com";
    private static final WebmasterHostId HOST_ID = IdUtils.urlToHostId(HOST);
    private static final Map<Long, UserVerifiedHost> USER_IDS_MAP = ImmutableMap.of(
            123L, new UserVerifiedHost(null, null, null, 0L, null),
            234L, new UserVerifiedHost(null, null, null, 0L, null)
    );
    private static final List<Long> USER_IDS = ImmutableList.copyOf(USER_IDS_MAP.keySet());

    private static final String HOST_2 = "http://cde.example.com";
    private static final WebmasterHostId HOST_ID_2 = IdUtils.urlToHostId(HOST_2);
    private static final List<Long> USER_IDS_2 = ImmutableList.of(345L);

    private static final ImportRobotsTxtTask.YtRow YT_ROW = new ImportRobotsTxtTask.YtRow(
            HOST, 10L, "abc"
    );

    private static final ImportRobotsTxtTask.YtRow CORRECT_YT_ROW = new ImportRobotsTxtTask.YtRow(
            HOST, 1543408000L, "abc"
    );

    private static final YtPath YT_DIR_PATH = YtPath.fromString("hahn://foo/bar");
    private static final String TABLE_NAME = "2018-12-01";


    @Mock
    private RobotsTxtService robotsTxtService;
    @Mock
    private SettingsService settingsService;
    @Mock
    private UserHostsService userHostsService;
    @Mock
    private YtService ytService;
    @Mock
    private YtCypressService cypressService;

    @Mock
    private AllVerifiedHostsCacheService allVerifiedHostsCacheService;

    @InjectMocks
    private ImportRobotsTxtTask importRobotsTxtTask;

    @Before
    public void setUp() throws Exception {
        when(allVerifiedHostsCacheService.contains(HOST_ID)).thenReturn(true);

        importRobotsTxtTask = new ImportRobotsTxtTask(
                allVerifiedHostsCacheService,
                robotsTxtService,
                settingsService,
                userHostsService,
                YT_DIR_PATH,
                ytService);
    }

    @Test
    public void processRowRetrievesUsersAndPutsInMap() {
        long notificationThresholdMillis = 9_000L; // smaller than lastAccessSeconds
        when(userHostsService.listUsersVerifiedHost(HOST_ID)).thenReturn(USER_IDS_MAP);
        LocalState state = new LocalState();
        state.notificationThresholdMillis = notificationThresholdMillis;

        importRobotsTxtTask.processRows(state, List.of(CORRECT_YT_ROW));

        assertEquals(state.hostToUsers.size(), 1);
        assertEquals(state.hostToUsers.get(HOST_ID), USER_IDS);

        verify(userHostsService).listUsersVerifiedHost(HOST_ID);
        verifyNoMoreInteractions(userHostsService);
    }

    @Test
    public void processRowTimestampOutOfBounds() {
        long notificationThresholdMillis = 1543408000001L;  // bigger than lastAccessSeconds
        LocalState state = new LocalState();
        state.notificationThresholdMillis = notificationThresholdMillis;

        importRobotsTxtTask.processRows(state, List.of(CORRECT_YT_ROW));

        assertTrue(state.hostToUsers.isEmpty());

        verifyNoMoreInteractions(userHostsService);
    }

    @Test
    public void processRowMapAlreadyContainsHost() {
        long notificationThresholdMillis = 11_000L;  // bigger than lastAccessSeconds
        LocalState state = new LocalState();
        state.notificationThresholdMillis = notificationThresholdMillis;
        state.hostToUsers.put(HOST_ID, USER_IDS);

        importRobotsTxtTask.processRows(state, List.of(CORRECT_YT_ROW));

        assertEquals(state.hostToUsers.size(), 1);
        assertEquals(state.hostToUsers.get(HOST_ID), USER_IDS);

        verifyNoMoreInteractions(userHostsService);
    }

    @Test
    public void processRowAddsRobotsTxt() {
        LocalState state = new LocalState();

        importRobotsTxtTask.processRows(state, List.of(CORRECT_YT_ROW));

        verify(robotsTxtService).addRobotsTxt(List.of(new RobotTxtInfo(HOST_ID, CORRECT_YT_ROW.getTimestampMilliseconds(), CORRECT_YT_ROW.content)));
        verifyNoMoreInteractions(robotsTxtService);
    }

    @Test
    public void processRowIncrementsCounter() {
        LocalState state = new LocalState();

        importRobotsTxtTask.processRows(state, List.of(CORRECT_YT_ROW));

        assertEquals(state.rowsTotal.get(), 1L);
        assertEquals(state.rowsFailed.get(), 0L);
    }

    @Test
    public void processStateSendsNotification() throws Exception {
        LocalState state = new LocalState();
        state.hostToUsers.put(HOST_ID, USER_IDS);
        state.hostToUsers.put(HOST_ID_2, USER_IDS_2);
        state.tableName = TABLE_NAME;


        importRobotsTxtTask.processState(state);

        List<Pair<WebmasterHostId, Long>> items = new ArrayList<>();
        USER_IDS.forEach(u -> items.add(Pair.of(HOST_ID, u)));
        USER_IDS_2.forEach(u -> items.add(Pair.of(HOST_ID_2, u)));
        verify(robotsTxtService).sendRobotsTxtNotification(items, TABLE_NAME);
        verifyNoMoreInteractions(robotsTxtService);
    }

    @Test
    public void processStateUpdatesLastTableSetting() throws Exception {
        LocalState state = new LocalState();
        state.tableName = TABLE_NAME;

        importRobotsTxtTask.processState(state);

        verify(settingsService).update(ROBOTS_TXT_LAST_TABLE, TABLE_NAME);

        verifyNoMoreInteractions(settingsService);
    }

    @Test
    public void processStateChecksFailureRatio() throws Exception {
        {
            LocalState state = new LocalState();
            state.tableName = TABLE_NAME;
            state.rowsFailed.set(11L);
            state.rowsTotal.set(100L);

            try {
                importRobotsTxtTask.processState(state);
                fail("Exception expected, failure ration > 10%");
            } catch (IllegalStateException e) {
                // expected
            }
        }
        {
            LocalState state = new LocalState();
            state.tableName = TABLE_NAME;
            state.rowsFailed.set(9L);
            state.rowsTotal.set(100L);

            try {
                importRobotsTxtTask.processState(state);
            } catch (IllegalStateException e) {
                fail("Exception is not expected, failure ration < 10%");
            }
        }
    }

    @Test
    public void initStateAndCheckFindsTable() {
        LocalState state = new LocalState();
        when(settingsService.getSettingUncached(ROBOTS_TXT_LAST_TABLE))
                .thenReturn(new CommonDataState(ROBOTS_TXT_LAST_TABLE, TABLE_NAME, DateTime.now()));
        YtPath oldTable = YtPath.path(YT_DIR_PATH, "2018-11-10");
        YtPath newTable = YtPath.path(YT_DIR_PATH, "2018-12-10");
        when(cypressService.list(YT_DIR_PATH))
                .thenReturn(Arrays.asList(oldTable, newTable));

        boolean tableFound = importRobotsTxtTask.initStateAndCheck(cypressService, state);

        assertTrue(tableFound);
        assertEquals(state.tableToProcess, newTable);
        assertEquals(state.tableName, newTable.getName());
    }

    @Test
    public void initStateAndCheckDoesNotFindTable() {
        LocalState state = new LocalState();
        when(settingsService.getSettingUncached(ROBOTS_TXT_LAST_TABLE))
                .thenReturn(new CommonDataState(ROBOTS_TXT_LAST_TABLE, TABLE_NAME, DateTime.now()));
        YtPath oldTable = YtPath.path(YT_DIR_PATH, "2018-11-10");
        when(cypressService.list(YT_DIR_PATH))
                .thenReturn(Collections.singletonList(oldTable));

        boolean tableFound = importRobotsTxtTask.initStateAndCheck(cypressService, state);

        assertFalse("There's no unprocessed table", tableFound);
    }

    @Test
    public void initStateAndCheckCalculatesNotificationThreshold() {
        LocalState state = new LocalState();
        when(settingsService.getSettingUncached(ROBOTS_TXT_LAST_TABLE))
                .thenReturn(new CommonDataState(ROBOTS_TXT_LAST_TABLE, TABLE_NAME, DateTime.now()));
        YtPath newTable = YtPath.path(YT_DIR_PATH, "2018-12-10");
        when(cypressService.list(YT_DIR_PATH)).thenReturn(Collections.singletonList(newTable));

        boolean tableFound = importRobotsTxtTask.initStateAndCheck(cypressService, state);

        assertTrue(tableFound);
        assertEquals(state.notificationThresholdMillis, DateTime.parse("2018-12-09").getMillis());
    }

    @Test
    public void initStateAndCheckInitsState() {
        LocalState state = new LocalState();
        when(settingsService.getSettingUncached(ROBOTS_TXT_LAST_TABLE))
                .thenReturn(new CommonDataState(ROBOTS_TXT_LAST_TABLE, TABLE_NAME, DateTime.now()));
        YtPath newTable = YtPath.path(YT_DIR_PATH, "2018-12-10");
        when(cypressService.list(YT_DIR_PATH)).thenReturn(Collections.singletonList(newTable));

        importRobotsTxtTask.initStateAndCheck(cypressService, state);

        assertNotNull(state.hostToUsers);
        assertEquals(state.rowsFailed.get(), 0L);
        assertEquals(state.rowsTotal.get(), 0L);
    }
}
