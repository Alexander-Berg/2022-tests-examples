package ru.yandex.solomon.alert.dao.ydb.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import ru.yandex.solomon.alert.dao.EntitiesDao;
import ru.yandex.solomon.alert.dao.MutesDaoTest;
import ru.yandex.solomon.alert.dao.ydb.YdbSchemaVersion;
import ru.yandex.solomon.alert.mute.domain.Mute;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;

/**
 * @author Ivan Tsybulin
 */
public class YdbMutesDaoTest extends MutesDaoTest {
    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();
    private EntitiesDao<Mute> ydbDao;

    @Before
    public void setUp() throws Exception {
        var mapper = new ObjectMapper();
        var ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        var root = ydb.getRootPath();

        ydbDao = new YdbMutesDao(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.CURRENT, mapper);
    }

    @Override
    protected EntitiesDao<Mute> getDao() {
        return ydbDao;
    }
}
