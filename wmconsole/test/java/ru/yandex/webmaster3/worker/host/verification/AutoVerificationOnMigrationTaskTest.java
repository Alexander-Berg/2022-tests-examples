package ru.yandex.webmaster3.worker.host.verification;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.data.WebmasterUser;
import ru.yandex.webmaster3.core.host.verification.VerificationType;
import ru.yandex.webmaster3.core.user.UserVerifiedHost;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.events.service.WMCEventsService;
import ru.yandex.webmaster3.storage.host.CommonDataState;
import ru.yandex.webmaster3.storage.host.CommonDataType;
import ru.yandex.webmaster3.storage.mirrors.dao.MirrorsChangesCHDao;
import ru.yandex.webmaster3.storage.settings.SettingsService;
import ru.yandex.webmaster3.storage.user.dao.UserHostVerificationYDao;
import ru.yandex.webmaster3.storage.user.dao.UserNotificationHostSettingsYDao;
import ru.yandex.webmaster3.storage.user.service.UserHostsService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.webmaster3.worker.PeriodicTask.Result.SUCCESS;


/**
 * @author kravchenko99
 * @date 5/27/21
 */

public class AutoVerificationOnMigrationTaskTest {

    private AutoVerificationOnMigrationTask task;
    private SettingsService settingsService;
    private MirrorsChangesCHDao mirrorsChangesCHDao;
    private UserHostsService userHostsService;
    private UserHostVerificationYDao userHostVerificationYDao;
    private WMCEventsService wmcEventsService;
    private UserNotificationHostSettingsYDao userNotificationHostSettingsYDao;


    private static final WebmasterHostId hostId = IdUtils.urlToHostId("https://wmcon.ru");
    private static final WebmasterHostId hostId2 = IdUtils.urlToHostId("https://khaliullin.info");
    private static final WebmasterHostId hostId3 = IdUtils.urlToHostId("https://test.ru");

    private static final Optional<UserVerifiedHost> record = Optional.of(new UserVerifiedHost(hostId, DateTime.now(),
            DateTime.now(), 1234554321L, VerificationType.META_TAG));

    private static final WebmasterHostId newHostId1 = IdUtils.urlToHostId("wmcon.ru");
    private static final WebmasterHostId newHostId2 = IdUtils.urlToHostId("https://www.wmcon.ru");
    private static final WebmasterHostId newHostId3 = IdUtils.urlToHostId("http://www.wmcon.ru");

    private static final WebmasterHostId newHostId4 = IdUtils.urlToHostId("https://abc.wmcon.ru");
    private static final WebmasterHostId newHostId5 = IdUtils.urlToHostId("khaliullin.info");

    @Before
    public void setUp() throws Exception {
        settingsService = mock(SettingsService.class);
        mirrorsChangesCHDao = mock(MirrorsChangesCHDao.class);
        userHostsService = mock(UserHostsService.class);
        userHostVerificationYDao = mock(UserHostVerificationYDao.class);
        wmcEventsService = mock(WMCEventsService.class);
        task = new AutoVerificationOnMigrationTask(settingsService, mirrorsChangesCHDao, userHostsService,
                userHostVerificationYDao, userNotificationHostSettingsYDao, wmcEventsService);
    }

    @Test
    public void isSuitableHostSuccess() {
        AutoVerificationOnMigrationTask.TaskState taskState = new AutoVerificationOnMigrationTask.TaskState();
        List.of(newHostId1, newHostId2, newHostId3).forEach(newHostId -> {
            boolean isSuitableHosts = AutoVerificationOnMigrationTask.isSuitableHosts(taskState, hostId, newHostId,
                    record, Optional.empty());
            assertTrue(isSuitableHosts);
        });
        assertEquals(0, taskState.getDifferentDomainsCount());
        assertEquals(0, taskState.getUnexpectedSituationCount());
        assertEquals(0, taskState.getVerificationCantBeInherited());
        assertEquals(0, taskState.getMainMirrorAlreadyAddedCount());
    }

    @Test
    public void isSuitableHostDiffDomains() {
        AutoVerificationOnMigrationTask.TaskState taskState = new AutoVerificationOnMigrationTask.TaskState();
        List.of(newHostId4, newHostId5).forEach(newHostId -> {
            boolean isSuitableHosts = AutoVerificationOnMigrationTask.isSuitableHosts(taskState, hostId, newHostId,
                    record, Optional.empty());
            assertFalse(isSuitableHosts);
        });
        assertEquals(2, taskState.getDifferentDomainsCount());
        assertEquals(0, taskState.getUnexpectedSituationCount());
        assertEquals(0, taskState.getVerificationCantBeInherited());
        assertEquals(0, taskState.getMainMirrorAlreadyAddedCount());
    }

    @Test
    public void isSuitableHostAlreadyVerified() {
        AutoVerificationOnMigrationTask.TaskState taskState = new AutoVerificationOnMigrationTask.TaskState();

        boolean isSuitableHosts = AutoVerificationOnMigrationTask.isSuitableHosts(taskState, hostId, hostId,
                record, record);
        assertFalse(isSuitableHosts);
        assertEquals(0, taskState.getDifferentDomainsCount());
        assertEquals(0, taskState.getUnexpectedSituationCount());
        assertEquals(0, taskState.getVerificationCantBeInherited());
        assertEquals(1, taskState.getMainMirrorAlreadyAddedCount());
    }

    @Test
    public void isSuitableHostUnexpectedError() {
        AutoVerificationOnMigrationTask.TaskState taskState = new AutoVerificationOnMigrationTask.TaskState();

        boolean isSuitableHosts = AutoVerificationOnMigrationTask.isSuitableHosts(taskState, newHostId1, hostId,
                Optional.empty(), Optional.empty());
        assertFalse(isSuitableHosts);
        assertEquals(0, taskState.getDifferentDomainsCount());
        assertEquals(1, taskState.getUnexpectedSituationCount());
        assertEquals(0, taskState.getVerificationCantBeInherited());
        assertEquals(0, taskState.getMainMirrorAlreadyAddedCount());
    }

    @Test
    public void isSuitableHostNotinherited() {
        AutoVerificationOnMigrationTask.TaskState taskState = new AutoVerificationOnMigrationTask.TaskState();
        Optional<UserVerifiedHost> record = Optional.of(new UserVerifiedHost(hostId, DateTime.now(), DateTime.now(),
                1234554321L, VerificationType.SELF));

        boolean isSuitableHosts = AutoVerificationOnMigrationTask.isSuitableHosts(taskState, hostId, newHostId1,
                record, Optional.empty());
        assertFalse(isSuitableHosts);
        assertEquals(0, taskState.getDifferentDomainsCount());
        assertEquals(0, taskState.getUnexpectedSituationCount());
        assertEquals(1, taskState.getVerificationCantBeInherited());
        assertEquals(0, taskState.getMainMirrorAlreadyAddedCount());
    }


    @Test
    @Ignore // фиг поставишь мок на forEach
    public void runTest() {
        String tableId = "123";
        when(settingsService.getSettingUncached(CommonDataType.LAST_IMPORTED_MIRRORS_CHANGES))
                .thenReturn(new CommonDataState(CommonDataType.LAST_IMPORTED_MIRRORS_CHANGES, tableId, DateTime.now()));
        when(settingsService.getSettingUncached(CommonDataType.LAST_PROCESSED_MIRRORS_AUTO_VERIFICATION))
                .thenReturn(new CommonDataState(CommonDataType.LAST_PROCESSED_MIRRORS_AUTO_VERIFICATION, "122",
                        DateTime.now()));

        when(mirrorsChangesCHDao.getHostsForAutoVerification(tableId)).thenReturn(
                Map.of(
                        hostId, newHostId1,
                        newHostId4, newHostId1,
                        hostId2, newHostId5,
                        hostId3, newHostId2
                )
        );
        Map.of(
                // norm and diff
                1L, List.of(Pair.of(hostId, newHostId1), Pair.of(newHostId4, newHostId1)),
                //norm and unexpected
                2L, List.of(Pair.of(hostId2, newHostId5), Pair.of(hostId, newHostId1)),
                // cant inherited
                3L, List.of(Pair.of(hostId, newHostId1)),
                // already added and diff
                4L, List.of(Pair.of(hostId, newHostId1), Pair.of(hostId3, newHostId2))
        );

        when(userHostsService.getVerifiedHosts(new WebmasterUser(1L), List.of(hostId, newHostId1)))
                .thenReturn(List.of(record.get()));

        when(userHostsService.getVerifiedHosts(new WebmasterUser(1L), List.of(newHostId4, newHostId1)))
                .thenReturn(List.of(
                        new UserVerifiedHost(newHostId4, DateTime.now(),
                                DateTime.now(), 1234554321L, VerificationType.META_TAG)
                ));

        when(userHostsService.getVerifiedHosts(new WebmasterUser(2L), List.of(hostId2, newHostId5)))
                .thenReturn(List.of(
                        new UserVerifiedHost(hostId2, DateTime.now(),
                                DateTime.now(), 1234554321L, VerificationType.META_TAG)
                ));

        when(userHostsService.getVerifiedHosts(new WebmasterUser(2L), List.of(hostId, newHostId1)))
                .thenReturn(List.of());
        when(userHostsService.getVerifiedHosts(new WebmasterUser(3L), List.of(hostId, newHostId1)))
                .thenReturn(List.of(
                        new UserVerifiedHost(hostId, DateTime.now(),
                                DateTime.now(), 1234554321L, VerificationType.DELEGATED)
                ));
        when(userHostsService.getVerifiedHosts(new WebmasterUser(4L), List.of(hostId, newHostId1)))
                .thenReturn(List.of(record.get(),
                        new UserVerifiedHost(newHostId1, DateTime.now(),
                                DateTime.now(), 1234554321L, VerificationType.DELEGATED)
                ));
        when(userHostsService.getVerifiedHosts(new WebmasterUser(4L), List.of(hostId2, newHostId2)))
                .thenReturn(List.of(record.get(),
                        new UserVerifiedHost(hostId2, DateTime.now(),
                                DateTime.now(), 1234554321L, VerificationType.DELEGATED)
                ));

        try {
            assertEquals(SUCCESS, task.run(UUID.randomUUID()));
        } catch (Exception e) {
            Assert.fail();
        }
        verify(userHostsService, times(2)).addVerifiedHost(any(), any());


        AutoVerificationOnMigrationTask.TaskState taskState = task.getState();
        assertEquals(2, taskState.getSuccessCount());
        assertEquals(1, taskState.getUnexpectedSituationCount());
        assertEquals(1, taskState.getVerificationCantBeInherited());
        assertEquals(2, taskState.getDifferentDomainsCount());
        assertEquals(1, taskState.getMainMirrorAlreadyAddedCount());
        assertEquals(4, taskState.getUsersCount());
        assertEquals(7, taskState.getAllRowsCount());
    }


}
