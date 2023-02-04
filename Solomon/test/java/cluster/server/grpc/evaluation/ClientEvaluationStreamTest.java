package ru.yandex.solomon.alert.cluster.server.grpc.evaluation;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.solomon.alert.cluster.server.grpc.StreamObserverStub;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.protobuf.EvaluationAssignmentKey;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamClientMessage;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamClientMessage.AssignEvaluation;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamClientMessage.UnassignEvaluation;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamServerMessage;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamServerMessage.Evaluation;
import ru.yandex.solomon.alert.protobuf.TAssignmentSeqNo;
import ru.yandex.solomon.alert.protobuf.TEvaluationState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.alert.cluster.server.grpc.GrpcEvaluationStreamConverter.toAlert;
import static ru.yandex.solomon.alert.cluster.server.grpc.GrpcEvaluationStreamConverter.toError;
import static ru.yandex.solomon.alert.cluster.server.grpc.GrpcEvaluationStreamConverter.toState;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlert;

/**
 * @author Vladimir Gordiychuk
 */
public class ClientEvaluationStreamTest {

    private StreamObserverStub<EvaluationStreamClientMessage> outbound;
    private ClientEvaluationStream inbound;
    private ClientEvaluationBreakerImpl evaluationBreaker;

    @Rule
    public Timeout timeoutRule = Timeout.builder()
            .withLookingForStuckThread(true)
            .withTimeout(30, TimeUnit.SECONDS)
            .build();

    @Before
    public void setUp() throws Exception {
        outbound = new StreamObserverStub<>();
        evaluationBreaker = new ClientEvaluationBreakerImpl();
        inbound = new ClientEvaluationStream(outbound, ForkJoinPool.commonPool(), evaluationBreaker);
    }

    @After
    public void tearDown() throws Exception {
        inbound.close();
    }

    @Test
    public void closeOnClientSideNoMessage() {
        awaitAct(() -> inbound.close());
        assertTrue(inbound.isDone());

        var status = outbound.done.join();
        assertEquals(status.toString(), Code.CANCELLED, status.getCode());
    }

    @Test
    public void emptyServerMessage() {
        awaitAct(() -> inbound.onNext(EvaluationStreamServerMessage.getDefaultInstance()));
        assertFalse(inbound.isDone());
        var resp = outbound.poolEvent(1, TimeUnit.MILLISECONDS);

        assertNull(resp);
        assertFalse(outbound.done.isDone());
    }

    @Test
    public void assignEvaluation() {
        var groupId = nextSeqNo();
        var alert = randomAlert();
        var subscriber = new SubscriberStub<Evaluation>();

        awaitAct(() -> assign(groupId, alert, subscriber));

        var assign = expectOneAssign();
        var key = assign.getAssignmentKey();
        assertEquals(groupId, key.getAssignGroupId());
        assertNotEquals(0L, key.getAssignId());
        assertEquals(alert, toAlert(assign));
        assertNull(toState(assign));

        subscriber.expectNoEvents();
        assertFalse(subscriber.doneFuture.isDone());
    }

    private void assign(TAssignmentSeqNo groupId, Alert alert, SubscriberStub<Evaluation> subscriber) {
        var req = ClientAssignReq.of(groupId, alert, null, subscriber);
        inbound.assign(req);
    }

    @Test
    public void cancelEvaluation() {
        var groupId = nextSeqNo();
        var alert = randomAlert();
        var subscriber = new SubscriberStub<Evaluation>();

        awaitAct(() -> assign(groupId, alert, subscriber));

        var assign = expectOneAssign();
        awaitAct(() -> subscriber.subscription.cancel());

        var unassign = expectOneUnassign();
        assertEquals(assign.getAssignmentKey(), unassign.getAssignKey());
    }

    @Test
    public void evaluationError() {
        var groupId = nextSeqNo();
        var alert = randomAlert();
        var subscriber = new SubscriberStub<Evaluation>();

        awaitAct(() -> assign(groupId, alert, subscriber));

        var assign = expectOneAssign();
        var expectStatus = Status.RESOURCE_EXHAUSTED.withDescription("hi");

        awaitAct(() -> inbound.onNext(EvaluationStreamServerMessage.newBuilder()
                .addErrors(toError(assign.getAssignmentKey(), expectStatus))
                .build()));

        var status = subscriber.doneFuture.join();
        assertEquals(expectStatus.getCode(), status.getCode());
        assertEquals(expectStatus.getDescription(), status.getDescription());
    }

    @Test
    public void evaluationGroupError() {
        var groupId = nextSeqNo();

        var oneAlert = randomAlert();
        var oneSubscriber = new SubscriberStub<Evaluation>();

        var twoAlert = randomAlert();
        var twoSubscriber = new SubscriberStub<Evaluation>();

        awaitAct(() -> assign(groupId, oneAlert, oneSubscriber));
        var oneAssign = expectOneAssign();

        awaitAct(() -> assign(groupId, twoAlert, twoSubscriber));
        var twoAssign = expectOneAssign();

        assertNotEquals(oneAssign.getAssignmentKey().getAssignId(), twoAssign.getAssignmentKey().getAssignId());

        var expectStatus = Status.RESOURCE_EXHAUSTED.withDescription("hi");
        awaitAct(() -> inbound.onNext(EvaluationStreamServerMessage.newBuilder()
                .addErrors(toError(groupId, expectStatus))
                .build()));

        var oneStatus = oneSubscriber.doneFuture.join();
        assertEquals(expectStatus.getCode(), oneStatus.getCode());
        assertEquals(expectStatus.getDescription(), oneStatus.getDescription());

        var twoStatus = oneSubscriber.doneFuture.join();
        assertEquals(expectStatus.getCode(), twoStatus.getCode());
        assertEquals(expectStatus.getDescription(), twoStatus.getDescription());
    }

    @Test
    public void evaluationOneByOne() {
        var groupId = nextSeqNo();
        var alert = randomAlert();
        var subscriber = new SubscriberStub<Evaluation>();

        awaitAct(() -> assign(groupId, alert, subscriber));

        var assign = expectOneAssign();

        for (int index = 0; index < 3; index++) {
            var expectEvaluation = nextEvaluation(assign.getAssignmentKey());

            awaitAct(() -> inbound.onNext(EvaluationStreamServerMessage.newBuilder()
                    .addEvaluations(expectEvaluation)
                    .build()));

            assertEquals(expectEvaluation, subscriber.takeEvent());
        }

        assertFalse(subscriber.doneFuture.isDone());
    }

    @Test
    public void evaluationBatch() {
        var groupId = nextSeqNo();
        var alert = randomAlert();
        var subscriber = new SubscriberStub<Evaluation>();

        awaitAct(() -> assign(groupId, alert, subscriber));

        var assign = expectOneAssign();
        var expectedList = IntStream.range(0, 3)
                .mapToObj(value -> nextEvaluation(assign.getAssignmentKey()))
                .collect(Collectors.toList());

        awaitAct(() -> inbound.onNext(EvaluationStreamServerMessage.newBuilder()
                .addAllEvaluations(expectedList)
                .build()));

        for (var expected : expectedList) {
            assertEquals(expected, subscriber.takeEvent());
        }

        subscriber.expectNoEvents();
        assertFalse(subscriber.doneFuture.isDone());
    }

    @Test
    public void evaluationByUnknownKey() {
        var unknownKey = EvaluationAssignmentKey.newBuilder()
                .setAssignId(42)
                .setAssignGroupId(nextSeqNo())
                .build();

        awaitAct(() -> inbound.onNext(EvaluationStreamServerMessage.newBuilder()
                .addEvaluations(nextEvaluation(unknownKey))
                .build()));

        var unassign = expectOneUnassign();
        assertEquals(unknownKey, unassign.getAssignKey());
    }

    @Test
    public void closeSubscriptionOnTransportError() {
        outbound = new StreamObserverStub<>() {
            @Override
            public void onNext(EvaluationStreamClientMessage value) {
                throw Status.UNAVAILABLE.asRuntimeException();
            }
        };

        inbound = newStream(outbound, 100);

        var subscribers = IntStream.range(0, 100)
                .mapToObj(ignore -> new SubscriberStub<Evaluation>())
                .collect(Collectors.toList());

        awaitAct(() -> {
            for (var subscriber : subscribers) {
                assign(nextSeqNo(), randomAlert(), subscriber);
            }
        });

        for (var subscriber : subscribers) {
            assertNotNull(subscriber.doneFuture.join());
        }
    }

    @Test
    public void closeSubscriptionWhenStreamAlreadyClosed() {
        outbound = new StreamObserverStub<>() {
            @Override
            public void onNext(EvaluationStreamClientMessage value) {
                throw Status.UNAVAILABLE.asRuntimeException();
            }
        };

        inbound = newStream(outbound, 100);

        var cause = new SubscriberStub<Evaluation>();
        awaitAct(() -> assign(nextSeqNo(), randomAlert(), cause));
        assertNotNull(cause.doneFuture.join());

        var target = new SubscriberStub<Evaluation>();
        awaitAct(() -> assign(nextSeqNo(), randomAlert(), target));
        assertNotNull(cause.doneFuture.join());
    }

    @Test
    public void noMessageTimeout() {
        outbound = new StreamObserverStub<>();
        inbound = newStream(outbound, 100, 10);

        var subscriber = new SubscriberStub<Evaluation>();
        awaitAct(() -> assign(nextSeqNo(), randomAlert(), subscriber));
        expectOneAssign();

        assertEquals(Status.OK.getCode(), subscriber.doneFuture.join().getCode());
        assertEquals(Status.DEADLINE_EXCEEDED.getCode(), outbound.done.join().getCode());
    }

    @Test
    public void subscriptionsSize() {
        for (int index = 1; index <= 10; index++) {
            var subscriber = new SubscriberStub<Evaluation>();
            awaitAct(() -> assign(nextSeqNo(), randomAlert(), subscriber));
            expectOneAssign();
            assertEquals(index, inbound.size());
        }
    }

    @Test
    public void sizeActualWithCancel() {
        var subscriber = new SubscriberStub<Evaluation>();
        awaitAct(() -> assign(nextSeqNo(), randomAlert(), subscriber));
        expectOneAssign();
        assertEquals(1, inbound.size());

        awaitAct(() -> subscriber.subscription.cancel());
        assertEquals(0, inbound.size());
    }

    @Test
    public void inactiveEmptyStream() {
        evaluationBreaker.setActive(false);
        assertFalse(inbound.isDone());
        assertFalse(outbound.done.isDone());
    }

    @Test
    public void gracefulUnassignEvaluationOnNext() {
        var subscriber = new SubscriberStub<Evaluation>();
        awaitAct(() -> assign(nextSeqNo(), randomAlert(), subscriber));
        var assign = expectOneAssign();
        assertEquals(1, inbound.size());

        evaluationBreaker.setActive(false);
        assertFalse(inbound.isDone());

        var expectEvaluation = nextEvaluation(assign.getAssignmentKey());
        awaitAct(() -> inbound.onNext(EvaluationStreamServerMessage.newBuilder()
                .addEvaluations(expectEvaluation)
                .build()));

        assertEquals(expectEvaluation, subscriber.takeEvent());
        assertEquals(Code.OK, subscriber.doneFuture.join().getCode());

        assertFalse(inbound.isDone());
        assertFalse(outbound.done.isDone());
    }

    @Test
    public void gracefulUnassignEvaluationOnByOne() {
        var alice = new SubscriberStub<Evaluation>();
        awaitAct(() -> assign(nextSeqNo(), randomAlert(), alice));
        var aliceAssign = expectOneAssign();

        var bob = new SubscriberStub<Evaluation>();
        awaitAct(() -> assign(nextSeqNo(), randomAlert(), bob));
        var bobAssign = expectOneAssign();

        evaluationBreaker.setActive(false);
        assertFalse(inbound.isDone());

        {
            var aliceEvaluation = nextEvaluation(aliceAssign.getAssignmentKey());
            awaitAct(() -> inbound.onNext(EvaluationStreamServerMessage.newBuilder()
                    .addEvaluations(aliceEvaluation)
                    .build()));

            assertEquals(aliceEvaluation, alice.takeEvent());
            assertEquals(Code.OK, alice.doneFuture.join().getCode());
        }
        assertFalse(inbound.isDone());

        {
            var bobEvaluation = nextEvaluation(bobAssign.getAssignmentKey());
            awaitAct(() -> inbound.onNext(EvaluationStreamServerMessage.newBuilder()
                    .addEvaluations(bobEvaluation)
                    .build()));

            assertEquals(bobEvaluation, bob.takeEvent());
            assertEquals(Code.OK, bob.doneFuture.join().getCode());
        }
        assertFalse(inbound.isDone());
    }

    private AssignEvaluation expectOneAssign() {
        var req = outbound.takeEvent();
        assertEquals(1, req.getAssignEvaluationsCount());
        assertEquals(0, req.getUnassignEvaluationsCount());

        return req.getAssignEvaluations(0);
    }

    private UnassignEvaluation expectOneUnassign() {
        var req = outbound.takeEvent();
        assertEquals(0, req.getAssignEvaluationsCount());
        assertEquals(1, req.getUnassignEvaluationsCount());

        return req.getUnassignEvaluations(0);
    }

    private Evaluation nextEvaluation(EvaluationAssignmentKey key) {
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

    private TAssignmentSeqNo nextSeqNo() {
        return TAssignmentSeqNo.newBuilder()
                .setLeaderSeqNo(ThreadLocalRandom.current().nextLong())
                .setProjectSeqNo(ThreadLocalRandom.current().nextLong())
                .build();
    }

    private void awaitAct(Runnable runnable) {
        var future = inbound.awaitAct();
        runnable.run();
        future.join();
    }

    private ClientEvaluationStream newStream(StreamObserver<EvaluationStreamClientMessage> output, int batchSize) {
        var writer = new EvaluationStreamClientMessageWriter(output, batchSize);
        return new ClientEvaluationStream(writer, ForkJoinPool.commonPool(), evaluationBreaker, ClientEvaluationStream.NO_MESSAGE_TIMEOUT_MS);
    }

    private ClientEvaluationStream newStream(StreamObserver<EvaluationStreamClientMessage> output, int batchSize, long timeoutMs) {
        var writer = new EvaluationStreamClientMessageWriter(output, 100);
        return new ClientEvaluationStream(writer, ForkJoinPool.commonPool(), evaluationBreaker, timeoutMs);
    }

}
