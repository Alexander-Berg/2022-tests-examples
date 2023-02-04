package ru.yandex.solomon.alert.dao.ydb.entity;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;

/**
 * @author Vladimir Gordiychuk
 */
@YaIgnore
public class YdbAlertsStateFilesDaoTest extends AlertStatesFilesDaoTest {
    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();
    private YdbHelper ydb;
    private AlertStatesFilesDao ydbDao;

    @Before
    public void setUp() throws Exception {
        ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        var root = ydb.getRootPath();

        ydbDao = new YdbAlertsStateFilesDao(root, ydb.getTableClient(), new MetricRegistry());
    }

    @After
    public void tearDown() throws Exception {
        ydb.close();
    }

    @Override
    protected AlertStatesFilesDao getDao() {
        return ydbDao;
    }

}
