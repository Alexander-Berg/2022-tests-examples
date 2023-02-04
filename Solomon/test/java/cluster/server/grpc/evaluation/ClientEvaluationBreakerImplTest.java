package ru.yandex.solomon.alert.cluster.server.grpc.evaluation;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.protobuf.EvaluationAssignmentKey;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamServerMessage.Evaluation;
import ru.yandex.solomon.alert.protobuf.TAssignmentSeqNo;
import ru.yandex.solomon.alert.protobuf.TEvaluationState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladimir Gordiychuk
 */
public class ClientEvaluationBreakerImplTest {

    private ClientEvaluationBreakerImpl breaker;

    @Before
    public void setUp() throws Exception {
        breaker = new ClientEvaluationBreakerImpl();
    }

    @Test
    public void unassignWhenNotActive() {
        breaker.setActive(false);
        assertFalse(breaker.continueEvaluation(nextEvaluation()));

        breaker.setActive(true);
        assertTrue(breaker.continueEvaluation(nextEvaluation()));
    }

    @Test
    public void unassignWhenRequested() {
        breaker.unassign(1);
        assertFalse(breaker.continueEvaluation(nextEvaluation()));
        assertTrue(breaker.continueEvaluation(nextEvaluation()));
    }

    @Test
    public void byDefaultEnableContinue() {
        for (int index = 0; index < 3; index++) {
            assertTrue(breaker.continueEvaluation(nextEvaluation()));
        }
    }

    @Test
    public void parallelUnassign() {
        var evaluations = IntStream.range(0, 1000)
                .mapToObj(value -> nextEvaluation())
                .collect(Collectors.toList());

        breaker.unassign(250);
        var result = evaluations.stream().parallel()
                .map(evaluation -> breaker.continueEvaluation(evaluation))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        assertEquals(750L, (long) result.get(Boolean.TRUE));
        assertEquals(250L, (long) result.get(Boolean.FALSE));
    }

    @Test
    public void cancelUnassignByRequest() {
        breaker.unassign(100);
        assertFalse(breaker.continueEvaluation(nextEvaluation()));

        breaker.cancelUnassign();
        assertTrue(breaker.continueEvaluation(nextEvaluation()));
    }

    private Evaluation nextEvaluation() {
        var key = nextKey();
        return Evaluation.newBuilder()
                .setAssignmentKey(key)
                .setState(TEvaluationState.newBuilder()
                        .setAlertId("alertId_" + key.getAssignId())
                        .setProjectId("project_id_" + key.getAssignGroupId().getProjectSeqNo() + "/" + key.getAssignGroupId().getLeaderSeqNo())
                        .setLatestEvalMillis(System.currentTimeMillis())
                        .setSinceMillis(System.currentTimeMillis() - ThreadLocalRandom.current().nextInt(10000))
                        .build())
                .build();
    }

    private EvaluationAssignmentKey nextKey() {
        return EvaluationAssignmentKey.newBuilder()
                .setAssignGroupId(nextSeqNo())
                .setAssignId(ThreadLocalRandom.current().nextLong())
                .build();
    }

    private TAssignmentSeqNo nextSeqNo() {
        return TAssignmentSeqNo.newBuilder()
                .setLeaderSeqNo(ThreadLocalRandom.current().nextLong())
                .setProjectSeqNo(ThreadLocalRandom.current().nextLong())
                .build();
    }
}
