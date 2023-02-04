package ru.yandex.solomon.alert.dao.ydb.entity;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.solomon.alert.dao.TelegramDao;
import ru.yandex.solomon.alert.dao.TelegramDaoTest;
import ru.yandex.solomon.alert.dao.ydb.YdbSchemaVersion;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;

/**
 * @author Vladimir Gordiychuk
 */
@YaIgnore
public class YdbTelegramDaoTest extends TelegramDaoTest {
    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();
    private TelegramDao ydbDao;

    @Before
    public void setUp() throws Exception {
        var ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        var root = ydb.getRootPath();

        ydbDao = new YdbTelegramDao(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.CURRENT);
    }

    @Override
    protected TelegramDao getDao() {
        return ydbDao;
    }
}
