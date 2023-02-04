package ru.yandex.solomon.alert.cluster.balancer;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import io.grpc.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.jns.client.JnsClientStub;
import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.api.converters.MuteConverter;
import ru.yandex.solomon.alert.api.converters.NotificationConverter;
import ru.yandex.solomon.alert.cluster.broker.AlertingProjectShard;
import ru.yandex.solomon.alert.cluster.broker.AlertingProjectShardFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.TemplateAlertFactory;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.search.ActivityFilters;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.search.ActivitySearch;
import ru.yandex.solomon.alert.cluster.broker.evaluation.EvaluationAssignmentServiceStub;
import ru.yandex.solomon.alert.cluster.broker.mute.search.MuteSearch;
import ru.yandex.solomon.alert.cluster.broker.notification.search.NotificationSearch;
import ru.yandex.solomon.alert.cluster.project.AssignmentConverter;
import ru.yandex.solomon.alert.cluster.project.ProjectAssignment;
import ru.yandex.solomon.alert.dao.DaoFactoryImpl;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertStatesDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertTemplateDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertTemplateLastVersionDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertsDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryMutesDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryNotificationsDao;
import ru.yandex.solomon.alert.notification.RetryOptions;
import ru.yandex.solomon.alert.notification.channel.NotificationChannelStubFactory;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.notification.state.StatefulNotificationChannelFactoryImpl;
import ru.yandex.solomon.alert.protobuf.TAssignProjectRequest;
import ru.yandex.solomon.alert.protobuf.TAssignProjectResponse;
import ru.yandex.solomon.alert.protobuf.TProjectAssignmentRequest;
import ru.yandex.solomon.alert.protobuf.TProjectAssignmentResponse;
import ru.yandex.solomon.alert.protobuf.TUnassignProjectRequest;
import ru.yandex.solomon.alert.protobuf.TUnassignProjectResponse;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.unroll.UnrollExecutorStub;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.core.container.ContainerType;
import ru.yandex.solomon.core.db.dao.memory.InMemoryQuotasDao;
import ru.yandex.solomon.flags.FeatureFlagHolderStub;
import ru.yandex.solomon.idempotency.IdempotentOperation;
import ru.yandex.solomon.idempotency.IdempotentOperationService;
import ru.yandex.solomon.locks.ReadOnlyDistributedLockStub;
import ru.yandex.solomon.quotas.watcher.QuotaWatcher;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Vladimir Gordiychuk
 */
public class AlertingLocalShardsStateTest {
    private static final Logger logger = LoggerFactory.getLogger(AlertingLocalShardsStateTest.class);

    private ManualClock clock;
    private ScheduledExecutorService timer;
    private ReadOnlyDistributedLockStub leaderLock;
    private AlertingBalancerClientStub client;
    private AlertingProjectShardFactory shardFactory;

    private AlertingTestNode alice;
    private AlertingTestNode bob;
    private AlertingTestNode eva;

    @Rule
    public Timeout timeoutRule = Timeout.builder()
        .withLookingForStuckThread(true)
        .withTimeout(15, TimeUnit.SECONDS)
        .build();

    @Before
    public void setUp() throws Exception {
        clock = new ManualClock();
        var notificationChannelFactory = new NotificationChannelStubFactory();
        var notificationConverter = new NotificationConverter(new ChatIdResolverStub());
        var statefulChannelFactory = new StatefulNotificationChannelFactoryImpl(timer, RetryOptions.empty(), notificationConverter);
        timer = new ManualScheduledExecutorService(2, clock);
        var assignmentService = new EvaluationAssignmentServiceStub(clock, timer);
        var unrollExecutor = new UnrollExecutorStub(timer);
        var dao = new DaoFactoryImpl(new InMemoryAlertsDao(),
                new InMemoryNotificationsDao(),
                new InMemoryMutesDao(),
                new InMemoryAlertStatesDao());
        this.shardFactory = new AlertingProjectShardFactory(
                clock,
                dao,
                notificationChannelFactory,
                statefulChannelFactory,
                unrollExecutor,
                assignmentService,
                ForkJoinPool.commonPool(),
                timer,
                Duration.ofMinutes(5),
                notificationConverter,
                new NotificationSearch(notificationConverter),
                new ActivitySearch(new ActivityFilters(notificationConverter)),
                MuteConverter.INSTANCE,
                new MuteSearch(MuteConverter.INSTANCE),
                new QuotaWatcher(null, new InMemoryQuotasDao(), "alerting", timer),
                new InMemoryAlertTemplateDao(true),
                new InMemoryAlertTemplateLastVersionDao(),
                new TemplateAlertFactory(new MustacheTemplateFactory()),
                new MetricRegistry(),
                new IdempotentOperationService() {
                    @Override
                    public CompletableFuture<Optional<IdempotentOperation>> get(String idempotentOperationId, String containerId, ContainerType containerType, String type) {
                        return CompletableFuture.completedFuture(Optional.empty());
                    }

                    @Override
                    public CompletableFuture<Boolean> delete(String idempotentOperationId, String containerId, ContainerType containerType, String type) {
                        return CompletableFuture.completedFuture(false);
                    }
                },
                new FeatureFlagHolderStub(),
                ForkJoinPool.commonPool(),
                new JnsClientStub());

        client = new AlertingBalancerClientStub();
        leaderLock = new ReadOnlyDistributedLockStub(clock);

        alice = new AlertingTestNode("alice");
        bob = new AlertingTestNode("bob");
        eva = new AlertingTestNode("eva");

        client.register(alice, bob, eva);
    }

    @After
    public void tearDown() throws Exception {
        alice.localShards.close();
        bob.localShards.close();
        eva.localShards.close();

        if (timer != null) {
            timer.shutdownNow();
        }
    }

    @Test
    public void getNotAssignedShard() {
        assertNull(alice.localShards.getShardById("notExists"));
    }

    @Test
    public void assignProject() {
        alice.becomeLeader();
        alice.addAssignment("junk", "bob").join();

        assertNull(alice.localShards.getShardById("junk"));
        assertNull(eva.localShards.getShardById("junk"));

        var junk = bob.localShards.getShardById("junk");
        assertNotNull(junk);

        alice.addAssignment("solomon", "bob").join();
        var solomon = bob.localShards.getShardById("solomon");
        assertNotNull(solomon);

        assertEquals("bob", junk.getAssignment().getAddress());
        assertEquals("bob", solomon.getAssignment().getAddress());

        assertEquals(1, solomon.getAssignment().compareTo(junk.getAssignment()));
    }

    @Test
    public void multipleAssignAtTheSameTime() {
        alice.becomeLeader();

        IntStream.range(0, 1000)
            .parallel()
            .mapToObj(index -> alice.addAssignment("project-" + index, "bob"))
            .collect(collectingAndThen(toList(), CompletableFutures::allOfUnit))
            .join();

        for (int index = 0; index < 1000; index++) {
            AlertingProjectShard shard = bob.localShards.getShardById("project-" + index);
            assertNotNull(shard);
        }
    }

    @Test
    public void actualizeAssignmentListOnRestart() throws InterruptedException {
        alice.becomeLeader();

        alice.addAssignment("junk", "bob").join();
        alice.addAssignment("solomon", "bob").join();

        assertNotNull(bob.localShards.getShardById("junk"));
        assertNotNull(bob.localShards.getShardById("solomon"));

        alice.addAssignment("solomon", "eva").join();

        bob.localShards.close();
        bob = new AlertingTestNode("bob");
        client.register(bob);
        while (!bob.localShards.isAssignmentActual(alice.getSeqNo())) {
            alice.awaitMessage(100);
        }

        assertNotNull(bob.localShards.getShardById("junk"));
        assertNull(bob.localShards.getShardById("solomon"));
        assertNotNull(eva.localShards.getShardById("solomon"));
    }

    @Test
    public void actualizeAssignmentListOnHeartbeat() throws InterruptedException {
        alice.becomeLeader();

        alice.addAssignment("junk", "bob").join();
        alice.addAssignment("solomon", "bob").join();

        assertNotNull(bob.localShards.getShardById("junk"));
        assertNotNull(bob.localShards.getShardById("solomon"));

        while (!bob.localShards.isAssignmentActual(alice.getSeqNo())) {
            alice.awaitMessage(100);
        }

        // unassign not delivered by any reason to bob
        alice.addAssignment("solomon", "eva").join();
        logger.info("bob assignment not actual anymore, actual {}", alice.getSeqNo());

        while (!bob.localShards.isAssignmentActual(alice.getSeqNo())) {
            alice.awaitMessage(100);
        }

        assertNotNull(bob.localShards.getShardById("junk"));
        assertNull(bob.localShards.getShardById("solomon"));
        assertNotNull(eva.localShards.getShardById("solomon"));
    }

    @Test
    public void assignUnassign() {
        alice.becomeLeader();

        alice.addAssignment("junk", "bob").join();
        assertNotNull(bob.localShards.getShardById("junk"));
        alice.removeAssignment("junk").join();
        assertNull(bob.localShards.getShardById("junk"));
    }

    @Test
    public void abortAssignFromPrevLeader() {
        alice.becomeLeader();

        alice.addAssignment("junk", "bob").join();
        assertNotNull(bob.localShards.getShardById("junk"));

        eva.assignments.putAll(alice.assignments);
        eva.becomeLeader();

        Status status = alice.addAssignment("test", "bob")
            .thenApply(ignore -> Status.OK)
            .exceptionally(Status::fromThrowable)
            .join();

        assertEquals(Status.Code.ABORTED, status.getCode());
        assertNull(bob.localShards.getShardById("test"));

        eva.addAssignment("test", "bob").join();
        assertNotNull(bob.localShards.getShardById("test"));
    }

    @Test
    public void abortUnassignFromPrevLeader() {
        alice.becomeLeader();

        alice.addAssignment("junk", "bob").join();
        assertNotNull(bob.localShards.getShardById("junk"));

        eva.assignments.putAll(alice.assignments);
        eva.becomeLeader();

        Status status = alice.removeAssignment("junk")
            .thenApply(ignore -> Status.OK)
            .exceptionally(Status::fromThrowable)
            .join();

        assertEquals(Status.Code.ABORTED, status.getCode());
        assertNotNull(bob.localShards.getShardById("junk"));

        eva.removeAssignment("junk").join();
        assertNull(bob.localShards.getShardById("junk"));
    }

    private class AlertingTestNode extends AbstractAlertingNode {
        private Map<String, ProjectAssignment> assignments = new ConcurrentHashMap<>();
        private long leaderSeqNo;
        private AtomicLong projectSeqNo = new AtomicLong();
        private AlertingLocalShardsImpl localShards;

        AlertingTestNode(String name) {
            super(name);
            localShards = new AlertingLocalShardsImpl(name, shardFactory, client, leaderLock, timer, timer);
        }

        private CompletableFuture<TAssignProjectResponse> addAssignment(String projectId, String node) {
            var seqNo = new AssignmentSeqNo(leaderSeqNo, projectSeqNo.incrementAndGet());
            var assignment = new ProjectAssignment(projectId, node, seqNo);
            assignments.put(projectId, assignment);
            return client.assignShard(node, TAssignProjectRequest.newBuilder()
                .setProjectId(projectId)
                .setSeqNo(AssignmentConverter.toProto(seqNo))
                .build());
        }

        private CompletableFuture<TUnassignProjectResponse> removeAssignment(String projectId) {
            projectSeqNo.incrementAndGet();
            var seqNo = new AssignmentSeqNo(leaderSeqNo, projectSeqNo.incrementAndGet());
            var assignment = assignments.remove(projectId);
            if (assignment == null) {
                return CompletableFuture.completedFuture(TUnassignProjectResponse.getDefaultInstance());
            }

            return client.unassignShard(assignment.getAddress(), TUnassignProjectRequest.newBuilder()
                .setSeqNo(AssignmentConverter.toProto(seqNo))
                .setProjectId(projectId)
                .build());
        }

        private void becomeLeader() {
            leaderSeqNo = leaderLock.setOwner(name);
        }

        public AssignmentSeqNo getSeqNo() {
            return new AssignmentSeqNo(leaderSeqNo, projectSeqNo.get());
        }

        @Override
        public CompletableFuture<TProjectAssignmentResponse> listAssignments(TProjectAssignmentRequest request) {
            return CompletableFuture.supplyAsync(() -> {
                var response = TProjectAssignmentResponse.newBuilder()
                    .setSeqNo(AssignmentConverter.toProto(getSeqNo()));

                for (var assignment : assignments.values()) {
                    response.addAssignEntries(TProjectAssignmentResponse.Entry.newBuilder()
                        .setSeqNo(AssignmentConverter.toProto(assignment.getSeqNo()))
                        .setAddress(assignment.getAddress())
                        .setProjectId(assignment.getProjectId())
                        .build());
                }

                return response.build();
            }, timer);
        }

        @Override
        public CompletableFuture<TAssignProjectResponse> assignShard(TAssignProjectRequest request) {
            return localShards.assignShard(request);
        }

        @Override
        public CompletableFuture<TUnassignProjectResponse> unassignShard(TUnassignProjectRequest request) {
            return localShards.unassignShard(request);
        }
    }
}
