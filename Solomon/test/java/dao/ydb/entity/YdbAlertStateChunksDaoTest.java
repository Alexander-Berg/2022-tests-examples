package ru.yandex.solomon.alert.dao.ydb.entity;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;

/**
 * @author Vladimir Gordiychuk
 */
@YaIgnore
public class YdbAlertStateChunksDaoTest extends AlertStatesChunksDaoTest {

    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();
    private YdbHelper ydb;
    private AlertStatesChunksDao ydbDao;

    @Before
    public void setUp() throws Exception {
        ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        var root = ydb.getRootPath();
        ydbDao = new YdbAlertStateChunksDao(root, ydb.getTableClient());
    }

    @After
    public void tearDown() throws Exception {
        ydb.close();
    }

    @Override
    protected AlertStatesChunksDao getDao() {
        return ydbDao;
    }
}
