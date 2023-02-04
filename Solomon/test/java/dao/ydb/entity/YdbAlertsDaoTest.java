package ru.yandex.solomon.alert.dao.ydb.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import ru.yandex.solomon.alert.dao.AlertsDaoTest;
import ru.yandex.solomon.alert.dao.EntitiesDao;
import ru.yandex.solomon.alert.dao.ydb.YdbSchemaVersion;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.inject.spring.AlertingIdempotentOperationContext;
import ru.yandex.solomon.idempotency.dao.IdempotentOperationDao;
import ru.yandex.solomon.idempotency.dao.ydb.YdbIdempotentOperationDao;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;

/**
 * @author Vladimir Gordiychuk
 */
public class YdbAlertsDaoTest extends AlertsDaoTest {
    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();
    private EntitiesDao<Alert> ydbDao;
    private YdbIdempotentOperationDao operationsDao;

    @Before
    public void setUp() throws Exception {
        var mapper = new ObjectMapper();
        var ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        var root = ydb.getRootPath();

        ydbDao = new YdbAlertsDao(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.CURRENT, mapper);
        operationsDao = new YdbIdempotentOperationDao(root + AlertingIdempotentOperationContext.PATH, ydb.getTableClient(), ydb.getSchemeClient());
        operationsDao.createSchemaForTests().join();
    }

    @Override
    public EntitiesDao<Alert> getDao() {
        return ydbDao;
    }

    @Override
    protected IdempotentOperationDao getOperationsDao() {
        return operationsDao;
    }
}
