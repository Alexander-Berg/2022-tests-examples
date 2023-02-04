package ru.yandex.solomon.alert.cluster.server.grpc;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import io.grpc.Status;
import io.grpc.Status.Code;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.solomon.alert.api.converters.AlertConverter;
import ru.yandex.solomon.alert.cluster.project.AssignmentConverter;
import ru.yandex.solomon.alert.evaluation.EvaluationServiceStub;
import ru.yandex.solomon.alert.protobuf.EvaluationAssignmentKey;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamClientMessage;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamClientMessage.AssignEvaluation;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamClientMessage.UnassignEvaluation;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamServerMessage;
import ru.yandex.solomon.alert.protobuf.TAlert;
import ru.yandex.solomon.alert.protobuf.TAssignmentSeqNo;
import ru.yandex.solomon.alert.protobuf.TEvaluationState;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlert;

/**
 * @author Vladimir Gordiychuk
 */
public class GrpcEvaluationStreamTest {

    private ManualClock clock;
    private ScheduledExecutorService timer;
    private EvaluationServiceStub evaluationService;
    private AssignmentTracker tracker;
    private StreamObserverStub<EvaluationStreamServerMessage> outbound;
    private GrpcEvaluationStream inbound;

    @Rule
    public Timeout timeoutRule = Timeout.builder()
            .withLookingForStuckThread(true)
            .withTimeout(30, TimeUnit.SECONDS)
            .build();

    @Before
    public void setUp() throws Exception {
        clock = new ManualClock();
        timer = new ManualScheduledExecutorService(2, clock);
        evaluationService = new EvaluationServiceStub(clock, timer);
        tracker = new AssignmentTracker();
        outbound = new StreamObserverStub<>();
        inbound = new GrpcEvaluationStream(outbound, evaluationService, tracker, ForkJoinPool.commonPool());

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            Runtime.getRuntime().halt(13);
        });
    }

    @After
    public void tearDown() throws Exception {
        inbound.close();
        timer.shutdownNow();
    }

    @Test
    public void closeOnServerSideNoMessage() {
        awaitAct(() -> inbound.close());

        var status = outbound.done.join();
        assertEquals(status.toString(), Code.CANCELLED, status.getCode());
    }

    @Test
    public void assignNotSupported() {
        var key = nextAssignmentKey();
        var req = EvaluationStreamClientMessage.newBuilder()
                .addAssignEvaluations(AssignEvaluation.newBuilder()
                        .setAssignmentKey(key)
                        .build())
                .build();

        awaitAct(() -> inbound.onNext(req));

        var resp = outbound.takeEvent();
        assertEquals(1, resp.getErrorsCount());
        assertEquals(Code.UNIMPLEMENTED, Status.fromCodeValue(resp.getErrors(0).getStatus().getCode()).getCode());
    }

    @Test
    public void emptyClientRequest() {
        awaitAct(() -> inbound.onNext(EvaluationStreamClientMessage.getDefaultInstance()));
        var resp = outbound.poolEvent(1, TimeUnit.MILLISECONDS);

        assertNull(resp);
        assertFalse(outbound.done.isDone());
    }

    @Test
    public void assignedEvalPeriodically() {
        var key = nextAssignmentKey();
        var alert = nextAlert();

        var req = EvaluationStreamClientMessage.newBuilder()
                .addAssignEvaluations(AssignEvaluation.newBuilder()
                        .setAssignmentKey(key)
                        .setAlert(alert)
                        .build())
                .build();

        awaitAct(() -> inbound.onNext(req));

        final long prevEval;
        {
            clock.passedTime(1, TimeUnit.MINUTES);
            var resp = outbound.takeEvent();
            assertEquals(1, resp.getEvaluationsCount());
            var evaluation = resp.getEvaluations(0);
            assertEquals(key, evaluation.getAssignmentKey());
            assertNotEquals(TEvaluationState.getDefaultInstance(), evaluation.getState());
            prevEval =evaluation.getState().getLatestEvalMillis();
        }

        {
            clock.passedTime(1, TimeUnit.MINUTES);
            var resp = outbound.takeEvent();
            assertEquals(1, resp.getEvaluationsCount());
            var evaluation = resp.getEvaluations(0);
            assertEquals(key, evaluation.getAssignmentKey());
            assertNotEquals(TEvaluationState.getDefaultInstance(), evaluation.getState());
            assertThat(evaluation.getState().getLatestEvalMillis(), greaterThan(prevEval));
        }
    }

    @Test
    public void unassignEvaluation() {
        var key = nextAssignmentKey();
        var alert = nextAlert();
        var alertKey = AlertConverter.protoToAlert(alert).getKey();


        var assign = EvaluationStreamClientMessage.newBuilder()
                .addAssignEvaluations(AssignEvaluation.newBuilder()
                        .setAssignmentKey(key)
                        .setAlert(alert)
                        .build())
                .build();

        awaitAct(() -> inbound.onNext(assign));
        assertTrue(evaluationService.hasTask(alertKey));

        {
            clock.passedTime(1, TimeUnit.MINUTES);
            var resp = outbound.takeEvent();
            assertEquals(1, resp.getEvaluationsCount());
            var evaluation = resp.getEvaluations(0);
            assertEquals(key, evaluation.getAssignmentKey());
            assertNotEquals(TEvaluationState.getDefaultInstance(), evaluation.getState());
        }

        var unassign = EvaluationStreamClientMessage.newBuilder()
                .addUnassignEvaluations(UnassignEvaluation.newBuilder()
                        .setAssignKey(key)
                        .build())
                .build();

        awaitAct(() -> inbound.onNext(unassign));
        assertFalse(evaluationService.hasTask(alertKey));
        assertFalse(outbound.done.isDone());
    }

    @Test
    public void unassignEvaluationGroup() {
        var group = nextSeqNo();

        var key1 = nextAssignmentKey(group);
        var key2 = nextAssignmentKey(group);
        var alert1 = nextAlert();
        var alert2 = nextAlert();
        var alertKey1 = AlertConverter.protoToAlert(alert1).getKey();
        var alertKey2 = AlertConverter.protoToAlert(alert2).getKey();

        var assign = EvaluationStreamClientMessage.newBuilder()
                .addAssignEvaluations(AssignEvaluation.newBuilder()
                        .setAssignmentKey(key1)
                        .setAlert(alert1))
                .addAssignEvaluations(AssignEvaluation.newBuilder()
                        .setAssignmentKey(key2)
                        .setAlert(alert2))
                .build();


        awaitAct(() -> inbound.onNext(assign));
        assertTrue(evaluationService.hasTask(alertKey1));
        assertTrue(evaluationService.hasTask(alertKey2));

        var unassign = EvaluationStreamClientMessage.newBuilder()
                .addUnassignEvaluations(UnassignEvaluation.newBuilder()
                        .setAssignKey(EvaluationAssignmentKey.newBuilder().setAssignGroupId(group)))
                .build();

        awaitAct(() -> inbound.onNext(unassign));
        assertFalse(evaluationService.hasTask(alertKey1));
        assertFalse(evaluationService.hasTask(alertKey2));
        assertFalse(outbound.done.isDone());
    }

    @Test
    public void cancelTaskOnServerSide() {
        var key = nextAssignmentKey();
        var alert = nextAlert();

        var req = EvaluationStreamClientMessage.newBuilder()
                .addAssignEvaluations(AssignEvaluation.newBuilder()
                        .setAssignmentKey(key)
                        .setAlert(alert)
                        .build())
                .build();


        awaitAct(() -> inbound.onNext(req));

        evaluationService.unassign(AlertConverter.protoToAlert(alert).getKey());
        var resp = outbound.takeEvent();
        assertEquals(1, resp.getErrorsCount());
        assertEquals(Code.CANCELLED, Status.fromCodeValue(resp.getErrors(0).getStatus().getCode()).getCode());
    }

    @Test
    public void unassignTaskOnStreamComplete() {
        var key = nextAssignmentKey();
        var alert = nextAlert();
        var alertKey = AlertConverter.protoToAlert(alert).getKey();

        var req = EvaluationStreamClientMessage.newBuilder()
                .addAssignEvaluations(AssignEvaluation.newBuilder()
                        .setAssignmentKey(key)
                        .setAlert(alert)
                        .build())
                .build();

        awaitAct(() -> inbound.onNext(req));
        assertTrue(evaluationService.hasTask(alertKey));

        awaitAct(() -> inbound.onError(Status.CANCELLED.asRuntimeException()));
        assertFalse(evaluationService.hasTask(alertKey));
        assertTrue(outbound.done.isDone());
    }

    @Test
    public void unassignTaskOnStreamClose() {
        var key = nextAssignmentKey();
        var alert = nextAlert();
        var alertKey = AlertConverter.protoToAlert(alert).getKey();

        var req = EvaluationStreamClientMessage.newBuilder()
                .addAssignEvaluations(AssignEvaluation.newBuilder()
                        .setAssignmentKey(key)
                        .setAlert(alert)
                        .build())
                .build();

        awaitAct(() -> inbound.onNext(req));
        assertTrue(evaluationService.hasTask(alertKey));
        awaitAct(() -> inbound.close());

        assertFalse(evaluationService.hasTask(alertKey));
        assertTrue(outbound.done.isDone());
    }

    @Test
    public void unassignWhenAssignmentMismatch() {
        var key = nextAssignmentKey();
        var alert = nextAlert();
        var alertKey = AlertConverter.protoToAlert(alert).getKey();

        var req = EvaluationStreamClientMessage.newBuilder()
                .addAssignEvaluations(AssignEvaluation.newBuilder()
                        .setAssignmentKey(key)
                        .setAlert(alert)
                        .build())
                .build();

        awaitAct(() -> inbound.onNext(req));
        assertTrue(evaluationService.hasTask(alertKey));

        awaitAct(() -> {
            var groupId = AssignmentConverter.fromProto(key.getAssignGroupId());
            assertTrue(tracker.isValid(alert.getProjectId(), new AssignmentSeqNo(groupId.getLeaderSeqNo(), groupId.getAssignSeqNo() + 1)));
            assertFalse(tracker.isValid(alert.getProjectId(), groupId));
        });

        assertFalse(evaluationService.hasTask(alertKey));

        var resp = outbound.takeEvent();
        assertEquals(1, resp.getErrorsCount());

        var error = resp.getErrors(0);
        assertEquals(key.toBuilder().clearAssignId().build(), error.getAssignmentKey());
        assertEquals(Code.ABORTED, Status.fromCodeValue(error.getStatus().getCode()).getCode());
    }

    @Test
    public void avoidCloseOutputStreamTwice() {
        var key = nextAssignmentKey();
        var alert = nextAlert();
        var alertKey = AlertConverter.protoToAlert(alert).getKey();

        var req = EvaluationStreamClientMessage.newBuilder()
                .addAssignEvaluations(AssignEvaluation.newBuilder()
                        .setAssignmentKey(key)
                        .setAlert(alert)
                        .build())
                .build();

        awaitAct(() -> inbound.onNext(req));
        assertTrue(evaluationService.hasTask(alertKey));
        awaitAct(() -> inbound.onError(Status.CANCELLED.asRuntimeException()));
        awaitAct(() -> inbound.onNext(EvaluationStreamClientMessage.getDefaultInstance()));
    }

    private void awaitAct(Runnable runnable) {
        var future = inbound.awaitAct();
        runnable.run();
        future.join();
    }

    private TAlert nextAlert() {
        return AlertConverter.alertToProto(randomAlert());
    }

    private EvaluationAssignmentKey nextAssignmentKey() {
        return nextAssignmentKey(nextSeqNo());
    }

    private EvaluationAssignmentKey nextAssignmentKey(TAssignmentSeqNo seqNo) {
        return EvaluationAssignmentKey.newBuilder()
                .setAssignId(nextAssignId())
                .setAssignGroupId(seqNo)
                .build();
    }

    private long nextAssignId() {
        while (true) {
            var id = ThreadLocalRandom.current().nextLong();
            if (id != 0) {
                return id;
            }
        }
    }

    private TAssignmentSeqNo nextSeqNo() {
        return TAssignmentSeqNo.newBuilder()
                .setLeaderSeqNo(ThreadLocalRandom.current().nextLong())
                .setProjectSeqNo(ThreadLocalRandom.current().nextLong())
                .build();
    }

}
