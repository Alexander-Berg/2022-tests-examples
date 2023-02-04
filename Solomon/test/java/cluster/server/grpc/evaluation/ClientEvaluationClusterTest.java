package ru.yandex.solomon.alert.cluster.server.grpc.evaluation;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.net.HostAndPort;
import io.grpc.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.cluster.discovery.ClusterDiscoveryStub;
import ru.yandex.grpc.utils.DefaultClientOptions;
import ru.yandex.grpc.utils.GrpcTransport;
import ru.yandex.grpc.utils.InProcessChannelFactory;
import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.cluster.server.grpc.StreamObserverStub;
import ru.yandex.solomon.alert.cluster.server.grpc.evaluation.EvaluationServerStub.ServerStream;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.protobuf.EvaluationAssignmentKey;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamClientMessage;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamClientMessage.AssignEvaluation;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamServerMessage;
import ru.yandex.solomon.alert.protobuf.EvaluationStreamServerMessage.Evaluation;
import ru.yandex.solomon.alert.protobuf.TAssignmentSeqNo;
import ru.yandex.solomon.alert.protobuf.TEvaluationState;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.solomon.alert.cluster.server.grpc.GrpcEvaluationStreamConverter.toAlert;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlert;

/**
 * @author Vladimir Gordiychuk
 */
public class ClientEvaluationClusterTest {

    @Rule
    public Timeout timeout = Timeout.builder()
            .withTimeout(30, TimeUnit.SECONDS)
            .withLookingForStuckThread(true)
            .build();

    private ClusterDiscoveryStub<GrpcTransport> discovery;
    private ClientEvaluationCluster cluster;
    private ManualScheduledExecutorService timer;

    private EvaluationServerStub alice;
    private EvaluationServerStub bob;

    @Before
    public void setUp() throws Throwable {
        discovery = new ClusterDiscoveryStub<>();
        var clock = new ManualClock();
        timer = new ManualScheduledExecutorService(1, clock);
        cluster = new ClientEvaluationCluster(discovery, 1, timer, ForkJoinPool.commonPool(), new MetricRegistry());

        alice = new EvaluationServerStub("alice");
        alice.setUp();

        bob = new EvaluationServerStub("bob");
        bob.setUp();
    }

    @After
    public void tearDown() {
        if (timer != null) {
            timer.shutdownNow();
        }

        cluster.close();
        discovery.close();
        alice.tearDown();
        bob.tearDown();
    }

    @Test
    public void cancelNotAssignedByClose() {
        var subscription = new SubscriberStub<Evaluation>();
        awaitAct(() -> assign(nextSeqNo(), randomAlert(), subscription));

        assertFalse(subscription.doneFuture.isDone());

        cluster.close();
        assertEquals(Status.OK, subscription.doneFuture.join());
    }

    @Test
    public void delayedAssign() throws InterruptedException {
        var subscription = new SubscriberStub<Evaluation>();
        var groupId = nextSeqNo();
        var alert = randomAlert();
        awaitAct(() -> assign(groupId, alert, subscription));

        assertFalse(subscription.doneFuture.isDone());

        awaitAct(() -> addServer(alice));
        var stream = awaitInboundStream(alice);
        var assign = expectOneAssign(stream.inbound);
        assertEquals(groupId, assign.getAssignmentKey().getAssignGroupId());
        assertEquals(alert, toAlert(assign));
    }

    @Test
    public void evaluationFromServerSide() throws InterruptedException {
        awaitAct(() -> addServer(alice));

        var subscriber = new SubscriberStub<Evaluation>();
        awaitAct(() -> assign(nextSeqNo(), randomAlert(), subscriber));

        var serverStream =awaitInboundStream(alice);
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
    public void spreadAlertsByNodes() throws InterruptedException {
        awaitAct(() -> addServer(alice));
        awaitAct(() -> addServer(bob));

        for (int index = 0; index < 50; index++) {
            awaitAct(() -> assign(nextSeqNo(), randomAlert(), new SubscriberStub<>()));
        }

        {
            var serverStream = awaitInboundStream(alice);
            assertNotNull(serverStream.inbound.takeEvent());
        }
        {
            var serverStream = awaitInboundStream(bob);
            assertNotNull(serverStream.inbound.takeEvent());
        }
    }

    @Test
    public void removeNode() throws InterruptedException {
        awaitAct(() -> addServer(alice));
        awaitAct(() -> addServer(bob));

        awaitAct(() -> removeServer(bob));

        awaitAct(() -> assign(nextSeqNo(), randomAlert(), new SubscriberStub<>()));

        var serverStream = awaitInboundStream(alice);
        expectOneAssign(serverStream.inbound);

        for (int index = 0; index < 3; index++) {
            awaitAct(() -> assign(nextSeqNo(), randomAlert(), new SubscriberStub<>()));
            expectOneAssign(serverStream.inbound);
        }
    }

    @Test
    public void completeFromRemovedNode() throws InterruptedException {
        var subscription = new SubscriberStub<Evaluation>();
        awaitAct(() -> addServer(alice));
        awaitAct(() -> assign(nextSeqNo(), randomAlert(), subscription));

        var serverStream = awaitInboundStream(alice);
        expectOneAssign(serverStream.inbound);
        assertFalse(subscription.doneFuture.isDone());

        awaitAct(() -> removeServer(alice));
        assertEquals(Status.Code.CANCELLED, serverStream.inbound.done.join().getCode());
        assertEquals(Status.OK, subscription.doneFuture.join());
    }

    @Test
    public void assignOnlyOnActiveNode() throws InterruptedException {
        awaitAct(() -> addServer(alice));
        awaitAct(() -> addServer(bob));

        setActive(bob, false);

        awaitAct(() -> assign(nextSeqNo(), randomAlert(), new SubscriberStub<>()));

        var serverStream = awaitInboundStream(alice);
        expectOneAssign(serverStream.inbound);

        for (int index = 0; index < 10; index++) {
            awaitAct(() -> assign(nextSeqNo(), randomAlert(), new SubscriberStub<>()));
            expectOneAssign(serverStream.inbound);
        }
    }

    private AssignEvaluation expectOneAssign(StreamObserverStub<EvaluationStreamClientMessage> stream) {
        var req = stream.takeEvent();
        assertEquals(1, req.getAssignEvaluationsCount());
        assertEquals(0, req.getUnassignEvaluationsCount());

        return req.getAssignEvaluations(0);
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

    private void addServer(EvaluationServerStub stub) {
        discovery.add(stub.server.getServerName(), newTransport(stub));
    }

    private void setActive(EvaluationServerStub stub, boolean active) {
        cluster.setActive(stub.server.getServerName(), active);
    }

    private void removeServer(EvaluationServerStub stub) {
        discovery.remove(stub.server.getServerName());
    }

    private GrpcTransport newTransport(EvaluationServerStub stub) {
        var opts = DefaultClientOptions.newBuilder()
                .setChannelFactory(new InProcessChannelFactory())
                .build();
        return new GrpcTransport(HostAndPort.fromHost(stub.server.getServerName()), opts);
    }

    private void awaitAct(Runnable runnable) {
        var future = cluster.awaitAct();
        runnable.run();
        future.join();
    }

    private ServerStream awaitInboundStream(EvaluationServerStub node) throws InterruptedException {
        ServerStream stream;
        while ((stream = node.inboundStream.poll(1, TimeUnit.MILLISECONDS)) == null) {
            awaitAct(() -> cluster.scheduleAct());
        }
        return stream;
    }

    private void assign(TAssignmentSeqNo groupId, Alert alert, SubscriberStub<Evaluation> subscriber) {
        var req = ClientAssignReq.of(groupId, alert, null, subscriber);
        cluster.assign(req);
    }

    private TAssignmentSeqNo nextSeqNo() {
        return TAssignmentSeqNo.newBuilder()
                .setLeaderSeqNo(ThreadLocalRandom.current().nextLong())
                .setProjectSeqNo(ThreadLocalRandom.current().nextLong())
                .build();
    }
}
