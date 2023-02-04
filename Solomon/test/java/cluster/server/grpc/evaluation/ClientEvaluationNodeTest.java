package ru.yandex.solomon.alert.cluster.server.grpc.evaluation;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.net.HostAndPort;
import io.grpc.Status;
import io.grpc.Status.Code;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.grpc.utils.DefaultClientOptions;
import ru.yandex.grpc.utils.GrpcTransport;
import ru.yandex.grpc.utils.InProcessChannelFactory;
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
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static ru.yandex.solomon.alert.cluster.server.grpc.GrpcEvaluationStreamConverter.toAlert;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlert;

/**
 * @author Vladimir Gordiychuk
 */
public class ClientEvaluationNodeTest {
    private EvaluationServerStub serverStub;
    private GrpcTransport transport;
    private ManualScheduledExecutorService timer;
    private ClientEvaluationNode node;

    @Before
    public void setUp() throws Throwable {
        serverStub = new EvaluationServerStub("srv");
        serverStub.setUp();

        var opts = DefaultClientOptions.newBuilder()
                .setChannelFactory(new InProcessChannelFactory())
                .build();
        timer = new ManualScheduledExecutorService(1, new ManualClock());
        transport = new GrpcTransport(HostAndPort.fromHost(serverStub.server.getServerName()), opts);
    }

    @After
    public void tearDown() throws Exception {
        if (node != null) {
            node.close();
        }

        if (timer != null) {
            timer.shutdownNow();
        }

        transport.close();
        serverStub.tearDown();
    }

    @Test
    public void assignOnClosedNode() {
        node = newNode(5);
        node.close();

        var subscriber = new SubscriberStub<Evaluation>();
        assign(nextSeqNo(), randomAlert(), subscriber);

        var status = subscriber.doneFuture.join();
        assertEquals(Status.OK, status);
    }

    @Test
    public void assignOnClosedTransport() {
        node = newNode(5);
        transport.close();

        var subscriber = new SubscriberStub<Evaluation>();
        assign(nextSeqNo(), randomAlert(), subscriber);

        var status = subscriber.doneFuture.join();
        assertEquals(Status.OK, status);
    }

    @Test
    public void errorOnServerSide() {
        node = newNode(3);
        serverStub.predefinedStatus = Status.INTERNAL.withDescription("hi");

        var subscriber = new SubscriberStub<Evaluation>();
        assign(nextSeqNo(), randomAlert(), subscriber);

        var status = subscriber.doneFuture.join();
        assertEquals(Status.OK, status);
    }

    @Test
    public void limitedStreamOne() throws InterruptedException {
        node = newNode(1);
        serverStub.sync = new CountDownLatch(1);
        for (int index = 0; index < 10; index++) {
            awaitAct(() -> assign(nextSeqNo(), randomAlert(), new SubscriberStub<>()));
        }

        serverStub.sync.await();
        assertEquals(1, serverStub.inboundStream.size());
    }

    @Test
    public void limitedStreamFive() throws InterruptedException {
        node = newNode(5);
        serverStub.sync = new CountDownLatch(5);
        for (int index = 0; index < 10; index++) {
            awaitAct(() -> assign(nextSeqNo(), randomAlert(), new SubscriberStub<>()));
        }

        serverStub.sync.await();
        assertEquals(5, serverStub.inboundStream.size());
    }

    @Test
    public void expectAssignedAlerts() throws InterruptedException {
        node = newNode(1);

        var groupId = nextSeqNo();
        var alert = randomAlert();
        var subscriber = new SubscriberStub<Evaluation>();

        awaitAct(() -> assign(groupId, alert, subscriber));

        var serverStream = serverStub.inboundStream.take();
        var assign = expectOneAssign(serverStream.inbound);

        assertEquals(alert, toAlert(assign));
        assertEquals(groupId, assign.getAssignmentKey().getAssignGroupId());
        assertFalse(subscriber.doneFuture.isDone());
    }

    @Test
    public void expectUnassignOnCancel() throws InterruptedException {
        node = newNode(1);

        var subscriber = new SubscriberStub<Evaluation>();

        awaitAct(() -> assign(nextSeqNo(), randomAlert(), subscriber));

        var serverStream = serverStub.inboundStream.take();
        var assign = expectOneAssign(serverStream.inbound);

        subscriber.subscription.cancel();
        var unassign = expectOneUnassign(serverStream.inbound);
        assertEquals(assign.getAssignmentKey(), unassign.getAssignKey());
    }

    @Test
    public void evaluationFromServerSide() throws InterruptedException {
        node = newNode(1);

        var subscriber = new SubscriberStub<Evaluation>();
        awaitAct(() -> assign(nextSeqNo(), randomAlert(), subscriber));

        var serverStream = serverStub.inboundStream.take();
        var assign = expectOneAssign(serverStream.inbound);

        List<Evaluation> evaluations = IntStream.range(0, 10)
                .mapToObj(value -> nextEvaluation(assign.getAssignmentKey()))
                .collect(Collectors.toList());

        for (var evaluation : evaluations) {
            serverStream.outbound.onNext(EvaluationStreamServerMessage.newBuilder()
                    .addEvaluations(evaluation)
                    .build());
        }

        for (var expected : evaluations) {
            assertEquals(expected, subscriber.takeEvent());
        }

        subscriber.expectNoEvents();
        assertFalse(subscriber.doneFuture.isDone());
    }

    @Test
    public void gracefulUnassignEvaluationByInactive() throws InterruptedException {
        node = newNode(1);

        var alice = new SubscriberStub<Evaluation>();
        awaitAct(() -> assign(nextSeqNo(), randomAlert(), alice));

        var serverStream = serverStub.inboundStream.take();
        var aliceAssign = expectOneAssign(serverStream.inbound);

        awaitAct(() -> node.setActive(false));

        var aliceEvaluation = nextEvaluation(aliceAssign.getAssignmentKey());
        serverStream.outbound.onNext(EvaluationStreamServerMessage.newBuilder()
                .addEvaluations(aliceEvaluation)
                .build());

        assertEquals(aliceEvaluation, alice.takeEvent());
        assertEquals(Code.OK, alice.doneFuture.join().getCode());
    }

    @Test
    public void unableAssignOnNotActiveNode() throws InterruptedException {
        node = newNode(1);

        awaitAct(() -> node.setActive(false));

        var alice = new SubscriberStub<Evaluation>();
        awaitAct(() -> assign(nextSeqNo(), randomAlert(), alice));
        assertEquals(Code.OK, alice.doneFuture.join().getCode());

        awaitAct(() -> node.setActive(true));

        var bob = new SubscriberStub<Evaluation>();
        var bobAlert = randomAlert();
        awaitAct(() -> assign(nextSeqNo(), bobAlert, bob));

        var serverStream = serverStub.inboundStream.take();
        var assign = expectOneAssign(serverStream.inbound);
        assertEquals(bobAlert, toAlert(assign));

        var nextEval = nextEvaluation(assign.getAssignmentKey());
        serverStream.outbound.onNext(EvaluationStreamServerMessage.newBuilder()
                .addEvaluations(nextEval)
                .build());

        assertEquals(nextEval, bob.takeEvent());
        assertFalse(bob.doneFuture.isDone());
    }

    private AssignEvaluation expectOneAssign(StreamObserverStub<EvaluationStreamClientMessage> stream) {
        var req = stream.takeEvent();
        assertEquals(1, req.getAssignEvaluationsCount());
        assertEquals(0, req.getUnassignEvaluationsCount());

        return req.getAssignEvaluations(0);
    }

    private UnassignEvaluation expectOneUnassign(StreamObserverStub<EvaluationStreamClientMessage> stream) {
        var req = stream.takeEvent();
        assertEquals(0, req.getAssignEvaluationsCount());
        assertEquals(1, req.getUnassignEvaluationsCount());
        return req.getUnassignEvaluations(0);
    }

    private void awaitAct(Runnable runnable) {
        var future = node.awaitAct();
        runnable.run();
        future.join();
    }

    private void assign(TAssignmentSeqNo groupId, Alert alert, SubscriberStub<Evaluation> subscriber) {
        var req = ClientAssignReq.of(groupId, alert, null, subscriber);
        node.assign(req);
    }

    private ClientEvaluationNode newNode(int maxStream) {
        return new ClientEvaluationNode(transport, maxStream, timer, ForkJoinPool.commonPool());
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
}
