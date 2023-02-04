package ru.yandex.solomon.alert.cluster.broker.notification;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.solomon.alert.dao.ydb.YdbSchemaVersion;
import ru.yandex.solomon.alert.dao.ydb.entity.YdbNotificationsDao;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
@YaIgnore
public class YdbDesyncOnRetryTest extends DesyncOnRetryTest {
    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();

    @Before
    public void setUp() throws Exception {
        var ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        var root = ydb.getRootPath();
        var mapper = new ObjectMapper();

        var ydbDao = new YdbNotificationsDao(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.CURRENT, mapper);
        ydbDao.createSchemaForTests().join();

        super.setUp(ydbDao);
    }

    @After
    public void tearDown() {
        super.tearDown();
    }
}
