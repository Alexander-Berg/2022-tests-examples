package ru.yandex.solomon.alert.dao.ydb.entity;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import ru.yandex.solomon.alert.dao.ShardsDao;
import ru.yandex.solomon.alert.dao.ShardsDaoTest;
import ru.yandex.solomon.alert.dao.ydb.YdbSchemaVersion;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;

/**
 * @author Vladimir Gordiychuk
 */
public class YdbShardsDaoTest extends ShardsDaoTest {
    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();
    private ShardsDao ydbDao;

    @Before
    public void setUp() throws Exception {
        var ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        var root = ydb.getRootPath();

        ydbDao = new YdbShardsDao(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.CURRENT);
        ydbDao.createSchemaForTests().join();
    }

    @Override
    public ShardsDao getDao() {
        return ydbDao;
    }
}
