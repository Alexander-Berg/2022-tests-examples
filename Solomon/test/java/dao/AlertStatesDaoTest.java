package ru.yandex.solomon.alert.dao;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.api.converters.NotificationConverter;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.search.AlertPersistStateSupport;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.protobuf.TPersistAlertState;
import ru.yandex.solomon.balancer.AssignmentSeqNo;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public abstract class AlertStatesDaoTest {

    private AlertPersistStateSupport stateSupport;

    protected abstract AlertStatesDao getDao();

    @Before
    public void init() {
        stateSupport = new AlertPersistStateSupport(new NotificationConverter(new ChatIdResolverStub()));
    }

    private AlertStatesDao getDao(String projectId) {
        var dao = getDao();
        dao.createSchemaForTests().join();
        dao.createSchema(projectId).join();
        return dao;
    }

    @Test
    public void loadEmpty() {
        var result = list("junk");
        assertArrayEquals(new TPersistAlertState[0], result);
    }

    @Test
    public void saveOneReadOne() {
        TPersistAlertState expected = stateSupport.randomState();
        AlertStatesDao dao = getDao("solomon");
        dao.save("solomon", Instant.now(), randomSeqNo(), Collections.singletonList(expected)).join();
        TPersistAlertState[] result = list("solomon");

        assertArrayEquals(new TPersistAlertState[]{expected}, result);
    }

    @Test
    public void saveManyReadMany() {
        TPersistAlertState[] source = IntStream.range(0, 10)
                .mapToObj(ignore -> stateSupport.randomState())
                .toArray(TPersistAlertState[]::new);

        AlertStatesDao dao = getDao("junk");
        dao.save("junk", Instant.now(), randomSeqNo(), Arrays.asList(source)).join();
        TPersistAlertState[] result = list("junk");
        assertArrayEquals(source, result);
    }

    @Test
    public void save20MegabyteState() {
        final int expectedSize = 20 << 20;
        ArrayList<TPersistAlertState> source = new ArrayList<>();
        int size = 0;
        while (size < expectedSize) {
            TPersistAlertState state = stateSupport.randomState();
            size += state.getSerializedSize();
            source.add(state);
        }

        AlertStatesDao dao = getDao("junk");
        dao.save("junk", Instant.now(), randomSeqNo(), source).join();
        TPersistAlertState[] result = list("junk");
        assertArrayEquals(source.toArray(new TPersistAlertState[0]), result);
    }

    @Test
    public void updateState() {
        AssignmentSeqNo seqNo = randomSeqNo();

        TPersistAlertState v1 = stateSupport.randomState();
        AlertStatesDao dao = getDao("solomon");
        dao.save("solomon", Instant.now(), seqNo, Collections.singletonList(v1)).join();

        TPersistAlertState v2 = stateSupport.randomState();
        dao.save("solomon", Instant.now().plusSeconds(10), seqNo, Collections.singletonList(v2)).join();

        TPersistAlertState[] result = list("solomon");
        assertArrayEquals(new TPersistAlertState[]{v2}, result);
    }

    @Test
    public void updateManyState() {
        AssignmentSeqNo seqNo = randomSeqNo();

        TPersistAlertState[] v1 = IntStream.range(0, 10)
                .mapToObj(ignore -> stateSupport.randomState())
                .toArray(TPersistAlertState[]::new);
        AlertStatesDao dao = getDao("junk");
        dao.save("junk", Instant.now(), seqNo, Arrays.asList(v1)).join();

        TPersistAlertState[] v2 = IntStream.range(0, 10)
                .mapToObj(ignore -> stateSupport.randomState())
                .toArray(TPersistAlertState[]::new);
        dao.save("junk", Instant.now().plusSeconds(60), seqNo, Arrays.asList(v2)).join();

        TPersistAlertState[] result = list("junk");
        assertArrayEquals(v2, result);
    }

    @Test
    public void stateOfDifferentProjectIndependent() {
        TPersistAlertState alice = stateSupport.randomState();
        getDao("alice")
                .save("alice", Instant.now(), randomSeqNo(), Collections.singletonList(alice))
                .join();

        TPersistAlertState bob = stateSupport.randomState();
        getDao("bob")
                .save("bob", Instant.now(), randomSeqNo(), Collections.singletonList(bob))
                .join();

        assertArrayEquals(new TPersistAlertState[]{alice}, list("alice"));
        assertArrayEquals(new TPersistAlertState[]{bob}, list("bob"));
    }

    @Test
    public void deleteProject() {
        TPersistAlertState alice = stateSupport.randomState();
        getDao("alice")
            .save("alice", Instant.now(), randomSeqNo(), Collections.singletonList(alice))
            .join();

        TPersistAlertState bob = stateSupport.randomState();
        getDao("bob")
            .save("bob", Instant.now(), randomSeqNo(), Collections.singletonList(bob))
            .join();

        getDao().deleteProject("alice").join();
        assertArrayEquals(new TPersistAlertState[]{}, list("alice"));
        assertArrayEquals(new TPersistAlertState[]{bob}, list("bob"));
    }

    @Test
    public void casOnUpdateCompareSeqNo() {
        AssignmentSeqNo seqNo1 = randomSeqNo();

        TPersistAlertState[] v1 = randomStates();
        AlertStatesDao dao = getDao("solomon");
        dao.save("solomon", Instant.now(), seqNo1, Arrays.asList(v1)).join();

        TPersistAlertState[] v1_1 = randomStates();
        TPersistAlertState[] v2 = randomStates();

        AssignmentSeqNo seqNo2 = new AssignmentSeqNo(seqNo1.getLeaderSeqNo(), seqNo1.getAssignSeqNo() + 1);

        CompletableFuture.allOf(
                dao.save("solomon", Instant.now(), seqNo1, Arrays.asList(v1_1)),
                dao.save("solomon", Instant.now(), seqNo2, Arrays.asList(v2)))
                .join();

        TPersistAlertState[] result = list("solomon");
        assertArrayEquals(v2, result);
    }

    @Test
    public void skipUpdateWhenSeqNoObsoleted() {
        AssignmentSeqNo seqNo1 = randomSeqNo();

        TPersistAlertState[] v1 = randomStates();
        AlertStatesDao dao = getDao("solomon");
        dao.save("solomon", Instant.now(), seqNo1, Arrays.asList(v1)).join();

        AssignmentSeqNo seqNo2 = new AssignmentSeqNo(seqNo1.getLeaderSeqNo(), seqNo1.getAssignSeqNo() + 1);
        TPersistAlertState[] v2 = randomStates();
        dao.save("solomon", Instant.now(), seqNo2, Arrays.asList(v2)).join();

        TPersistAlertState[] v1_1 = randomStates();
        dao.save("solomon", Instant.now().plusSeconds(60), seqNo1, Arrays.asList(v1_1)).join();

        TPersistAlertState[] result = list("solomon");
        assertArrayEquals(v2, result);
    }

    @Test
    public void multipleUpdateIteration() {
        AssignmentSeqNo seqNo = randomSeqNo();

        AlertStatesDao dao = getDao("solomon");
        Instant now = Instant.now();
        for (int index = 0; index < 10; index++) {
            TPersistAlertState[] states = randomStates();
            dao.save("solomon", now, seqNo, Arrays.asList(states)).join();

            TPersistAlertState[] result = list("solomon");
            assertArrayEquals(states, result);
            now = now.plus(5, ChronoUnit.MINUTES);
        }
    }

    @Test
    public void avoidSafeObsoleteState() {
        var oldSeqNo = new AssignmentSeqNo(1581497927624L, 1594721714128L);
        var newSeqNo = new AssignmentSeqNo(1594722001858L, 2378L);

        assertThat(oldSeqNo, lessThan(newSeqNo));

        AlertStatesDao dao = getDao("solomon");

        Instant now = Instant.now();
        // save by oldSeqNo
        {
            now = now.plus(1, ChronoUnit.MINUTES);
            var states = randomStates();
            dao.save("solomon", now, oldSeqNo, List.of(states)).join();

            var result = list("solomon");
            assertArrayEquals(states, result);
        }

        // save by newSeqNo
        {
            now = now.plus(1, ChronoUnit.MINUTES);
            var states = randomStates();
            dao.save("solomon", now, newSeqNo, List.of(states)).join();

            var result = list("solomon");
            assertArrayEquals(states, result);
        }

        // unable save by old
        {
            now = now.plus(1, ChronoUnit.MINUTES);
            var prevState = list("solomon");

            now = now.plus(1, ChronoUnit.MINUTES);
            var states = randomStates();
            dao.save("solomon", now, oldSeqNo, List.of(states)).join();

            var result = list("solomon");
            assertArrayEquals(prevState, result);
        }
    }

    private TPersistAlertState[] list(String projectId) {
        var dao = getDao(projectId);
        return list(dao, projectId);
    }

    private TPersistAlertState[] randomStates() {
        return IntStream.range(0, 10)
                .mapToObj(ignore -> stateSupport.randomState())
                .toArray(TPersistAlertState[]::new);
    }

    protected AssignmentSeqNo randomSeqNo() {
        return new AssignmentSeqNo(ThreadLocalRandom.current().nextInt(), ThreadLocalRandom.current().nextInt());
    }

    protected TPersistAlertState randomState() {
        return stateSupport.randomState();
    }

    protected TPersistAlertState[] list(AlertStatesDao dao, String projectId) {
        return dao.findAll(projectId).join()
            .toArray(new TPersistAlertState[0]);
    }
}
