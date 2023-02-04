package ru.yandex.webmaster3.worker.notifications;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.webmaster3.core.util.Either;
import ru.yandex.webmaster3.storage.notifications.dao.EuEmailYDao;
import ru.yandex.webmaster3.storage.util.ydb.YdbYqlService;
import ru.yandex.webmaster3.worker.notifications.ImportEuEmailsTask.State;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author akhazhoyan 02/2019
 */
@RunWith(MockitoJUnitRunner.class)
public class ImportEuEmailsTaskTest {

    private final LocalDate TODAY = LocalDate.now();
    private final LocalDate YESTERDAY = TODAY.minusDays(1);
    private final LocalDate TOMORROW = TODAY.plusDays(1);

    private ImportEuEmailsTask importEuEmailsTask;

    @Mock
    private EuEmailYDao euEmailYDao;
    @Mock
    private EuEmailService euEmailService;
    @Mock
    private YdbYqlService ydbYqlService;

    @Before
    public void setUp() {
        importEuEmailsTask = new ImportEuEmailsTask(
                ydbYqlService,
                euEmailYDao,
                euEmailService
        );
    }

    @Test
    public void initStateAndCheck_succeeds() {
        // initStateAndCheck succeeds if lastImportDate < today
        when(euEmailService.lastImportDate()).thenReturn(YESTERDAY);
        System.out.println(euEmailService.lastImportDate());
        Either<String, State> stateEither = importEuEmailsTask.initStateAndCheck();
        assertFalse(stateEither.isLeft());
        State state = stateEither.getRightUnsafe();
        assertEquals(YESTERDAY, state.lastUpdateDate);
        assertEquals(TODAY, state.today);
    }

    @Test
    public void initStateAndCheck_fails() {
        // initStateAndCheck fails if lastImportDate >= today
        {
            when(euEmailService.lastImportDate()).thenReturn(TODAY);
            assertTrue(importEuEmailsTask.initStateAndCheck().isLeft());
        }
        reset(euEmailService);
        {
            when(euEmailService.lastImportDate()).thenReturn(TOMORROW);
            assertTrue(importEuEmailsTask.initStateAndCheck().isLeft());
        }
    }
}
