package ru.yandex.solomon.alert.dao.ydb;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.api.converters.NotificationConverter;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.search.AlertPersistStateSupport;
import ru.yandex.solomon.alert.dao.AlertStatesDao;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.protobuf.TPersistAlertState;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

/**
 * @author Vladimir Gordiychuk
 */
@YaIgnore
public class YdbAlertStatesDaoVersionTest {
    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();

    private AlertStatesDao min;
    private AlertStatesDao max;
    private AlertPersistStateSupport stateSupport;

    @Before
    public void setUp() throws Exception {
        assumeThat(YdbSchemaVersion.MIN, not(CoreMatchers.equalTo(YdbSchemaVersion.MAX)));

        var ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        var root = ydb.getRootPath();
        MetricRegistry registry = new MetricRegistry();
        min = YdbAlertStatesDaoFactory.create(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.MIN, registry);
        max = YdbAlertStatesDaoFactory.create(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.MAX, registry);
        stateSupport = new AlertPersistStateSupport(new NotificationConverter(new ChatIdResolverStub()));
    }

    @Test
    public void writeAlwaysDuplicatesIntoMinVersion() {
        min.createSchemaForTests().join();
        min.createSchema("junk").join();
        max.createSchemaForTests().join();
        max.createSchema("junk").join();

        TPersistAlertState expected = stateSupport.randomState();
        max.save("junk", Instant.now(), randomSeqNo(), Collections.singletonList(expected)).join();

        List<TPersistAlertState> resultActual = list(max, "junk");
        assertThat(resultActual, iterableWithSize(1));
        assertThat(resultActual.get(0), equalTo(expected));

        List<TPersistAlertState> resultOld = list(max,"junk");
        assertThat(resultOld, iterableWithSize(1));
        assertThat(resultOld.get(0), equalTo(expected));
    }

    @Test
    public void readFromOldStateFirst() {
        min.createSchemaForTests();
        min.createSchema("junk").join();
        max.createSchemaForTests();
        max.createSchema("junk").join();

        List<TPersistAlertState> v1 = Collections.singletonList(stateSupport.randomState());
        AssignmentSeqNo seqNoV1 = randomSeqNo();
        max.save("junk", Instant.now(), seqNoV1, v1).join();

        assertThat(list(max, "junk"), equalTo(v1));

        List<TPersistAlertState> v2 = Collections.singletonList(stateSupport.randomState());
        AssignmentSeqNo seqNoV2 = new AssignmentSeqNo(seqNoV1.getLeaderSeqNo(), seqNoV1.getAssignSeqNo() + 10);
        min.save("junk", Instant.now(), seqNoV2, v2).join();

        assertThat(list(min, "junk"), equalTo(v2));
        assertThat(list(max, "junk"), equalTo(v2));
    }

    @Test
    public void prevVersionCanBeAbsent() {
        max.createSchemaForTests().join();
        max.createSchema("junk").join();

        List<TPersistAlertState> v1 = Collections.singletonList(stateSupport.randomState());
        max.save("junk", Instant.now(), randomSeqNo(), v1).join();
        assertThat(list(max, "junk"), equalTo(v1));
    }

    private List<TPersistAlertState> list(AlertStatesDao dao, String projectId) {
        return dao.findAll(projectId).join();
    }

    private AssignmentSeqNo randomSeqNo() {
        return new AssignmentSeqNo(ThreadLocalRandom.current().nextInt(), ThreadLocalRandom.current().nextInt());
    }
}
