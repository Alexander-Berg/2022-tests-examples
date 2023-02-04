package ru.yandex.solomon.alert.dao.ydb;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import ru.yandex.solomon.alert.dao.AlertTemplateLastVersionDao;
import ru.yandex.solomon.alert.dao.AlertTemplateLastVersionDaoTest;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;

/**
 * @author Alexey Trushkin
 */
public class YdbAlertTemplateLastVersionDaoTest extends AlertTemplateLastVersionDaoTest {
    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();
    private AlertTemplateLastVersionDao ydbDao;

    @Before
    public void setUp() throws Exception {
        var ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        var root = ydb.getRootPath();

        ydbDao = new YdbAlertTemplateLastVersionDao(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.CURRENT);
        ydbDao.createSchemaForTests().join();
    }

    @Override
    public AlertTemplateLastVersionDao getDao() {
        return ydbDao;
    }
}
