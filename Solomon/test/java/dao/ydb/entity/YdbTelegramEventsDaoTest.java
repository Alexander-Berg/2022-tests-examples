package ru.yandex.solomon.alert.dao.ydb.entity;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import ru.yandex.solomon.alert.dao.TelegramEventsDao;
import ru.yandex.solomon.alert.dao.TelegramEventsDaoTest;
import ru.yandex.solomon.alert.dao.ydb.YdbSchemaVersion;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;

/**
 * @author Vladimir Gordiychuk
 */
public class YdbTelegramEventsDaoTest extends TelegramEventsDaoTest {
    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();
    private YdbHelper ydb;
    private TelegramEventsDao ydbDao;

    @Before
    public void setUp() throws Exception {
        ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        var root = ydb.getRootPath();

        ydbDao = new YdbTelegramEventsDao(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.CURRENT);
    }

    @After
    public void tearDown() throws Exception {
        ydb.close();
    }

    @Override
    protected TelegramEventsDao getDao() {
        return ydbDao;
    }
}
