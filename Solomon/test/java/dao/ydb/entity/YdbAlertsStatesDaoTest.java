package ru.yandex.solomon.alert.dao.ydb.entity;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.dao.AlertStatesDao;
import ru.yandex.solomon.alert.dao.AlertStatesDaoTest;
import ru.yandex.solomon.alert.dao.ydb.YdbSchemaVersion;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;

/**
 * @author Vladimir Gordiychuk
 */
@YaIgnore
public class YdbAlertsStatesDaoTest extends AlertStatesDaoTest {
    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();
    private YdbHelper ydb;
    private AlertStatesDao ydbDao;

    @Before
    public void setUp() throws Exception {
        ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        var root = ydb.getRootPath();

        ydbDao = new YdbAlertsStatesDao(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.CURRENT, new MetricRegistry());
    }

    @After
    public void tearDown() throws Exception {
        ydb.close();
    }

    @Override
    protected AlertStatesDao getDao() {
        return ydbDao;
    }
}
