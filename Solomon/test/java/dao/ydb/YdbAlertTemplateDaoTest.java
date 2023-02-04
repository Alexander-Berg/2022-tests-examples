package ru.yandex.solomon.alert.dao.ydb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import ru.yandex.solomon.alert.dao.AlertTemplateDao;
import ru.yandex.solomon.alert.dao.AlertTemplateDaoTest;
import ru.yandex.solomon.alert.dao.AlertTemplateLastVersionDao;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;

/**
 * @author Alexey Trushkin
 */
public class YdbAlertTemplateDaoTest extends AlertTemplateDaoTest {
    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();
    private AlertTemplateDao ydbDao;
    private AlertTemplateLastVersionDao versionsDao;

    @Before
    public void setUp() throws Exception {
        var mapper = new ObjectMapper();
        var ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        var root = ydb.getRootPath();

        ydbDao = new YdbAlertTemplateDao(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.CURRENT, mapper);
        ydbDao.createSchemaForTests().join();

        versionsDao = new YdbAlertTemplateLastVersionDao(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.CURRENT);
        versionsDao.createSchemaForTests().join();
    }

    @Override
    public AlertTemplateDao getDao() {
        return ydbDao;
    }

    @Override
    public AlertTemplateLastVersionDao getVersionsDao() {
        return versionsDao;
    }
}
