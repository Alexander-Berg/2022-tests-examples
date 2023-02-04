package ru.yandex.solomon.alert.cluster;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.grpc.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.solomon.alert.cluster.balancer.AbstractAlertingNode;
import ru.yandex.solomon.alert.cluster.balancer.AlertingBalancerClientStub;
import ru.yandex.solomon.alert.cluster.balancer.AlertingLocalShards;
import ru.yandex.solomon.alert.cluster.balancer.AlertingLocalShardsStub;
import ru.yandex.solomon.alert.cluster.broker.evaluation.EvaluationAssignmentServiceStub;
import ru.yandex.solomon.alert.protobuf.THeartbeatRequest;
import ru.yandex.solomon.alert.protobuf.THeartbeatResponse;
import ru.yandex.solomon.locks.LockDetail;
import ru.yandex.solomon.locks.ReadOnlyDistributedLockStub;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladimir Gordiychuk
 */
public class AlertingHeartbeatProcessTest {
    private static final Logger logger = LoggerFactory.getLogger(AlertingHeartbeatProcessTest.class);

    private ManualClock clock;
    private ReadOnlyDistributedLockStub balancerLock;
    private AlertingBalancerClientStub client;
    private ScheduledExecutorService executorService;
    private AlertingLocalShards shards;

    private AlertingHeartbeatProcess heartbeatProcess;

    private AlertingNode alice;
    private AlertingNode bob;
    private AlertingNode eva;

    @Rule
    public Timeout timeoutRule = Timeout.builder()
        .withLookingForStuckThread(true)
        .withTimeout(15, TimeUnit.SECONDS)
        .build();

    @Before
    public void setUp() {
        alice = new AlertingNode("alice");
        bob = new AlertingNode("bob");
        eva = new AlertingNode("eva");

        clock = new ManualClock();
        balancerLock = new ReadOnlyDistributedLockStub(clock);
        client = new AlertingBalancerClientStub();
        client.register(alice, bob);
        shards = new AlertingLocalShardsStub();

        executorService = new ManualScheduledExecutorService(1, clock);
        heartbeatProcess = new AlertingHeartbeatProcess(
            clock,
            shards,
            new EvaluationAssignmentServiceStub(clock, executorService),
            balancerLock,
            client,
            executorService,
            executorService);

        alice.setUp();
        bob.setUp();
        eva.setUp();
    }

    @After
    public void tearDown() {
        heartbeatProcess.stop();
        executorService.shutdownNow();
    }

    @Test
    public void periodicallySendHeartbeats() throws InterruptedException {
        CountDownLatch sync = alice.heartbeatSync;
        alice.becomeLeader();
        heartbeatProcess.start();

        clock.passedTime(1, TimeUnit.SECONDS);
        sync.await();

        assertEquals(1, alice.heartbeatsCount.get());

        clock.passedTime(alice.heartbeatExpireTimeMillis / 2, TimeUnit.SECONDS);
        CountDownLatch syncTwo = alice.heartbeatSync;
        do {
            clock.passedTime(5, TimeUnit.SECONDS);
        } while (!syncTwo.await(10, TimeUnit.MILLISECONDS));
        assertTrue(alice.heartbeatsCount.get() >= 2);
    }

    @Test
    public void sendHeartbeatAsOnlyLeaderAppear() throws InterruptedException {
        CountDownLatch sync = alice.heartbeatSync;
        heartbeatProcess.start();

        assertFalse(sync.await(10, TimeUnit.MILLISECONDS));
        alice.becomeLeader();
        do {
            clock.passedTime(1, TimeUnit.SECONDS);
        } while (!sync.await(10, TimeUnit.MILLISECONDS));
        assertEquals(1, alice.heartbeatsCount.get());
    }

    @Test
    public void sendToActualLeader() throws InterruptedException {
        CountDownLatch aliceSync = alice.heartbeatSync;
        alice.becomeLeader();
        heartbeatProcess.start();
        aliceSync.await();
        assertEquals(1, alice.heartbeatsCount.get());

        CountDownLatch bobSync = bob.heartbeatSync;
        bob.becomeLeader();
        do {
            clock.passedTime(5, TimeUnit.SECONDS);
        } while (!bobSync.await(10, TimeUnit.MILLISECONDS));

        assertEquals(1, bob.heartbeatsCount.get());
        assertEquals(1, alice.heartbeatsCount.get());
    }

    @Test
    public void retryHeartbeatIfFailed() throws InterruptedException {
        CountDownLatch sync = alice.heartbeatSync;

        // alice not ready, but it's still leader
        alice.becomeLeader();
        alice.tearDown();

        heartbeatProcess.start();

        clock.passedTime(1, TimeUnit.SECONDS);
        assertFalse(sync.await(10, TimeUnit.MILLISECONDS));

        alice.setUp();
        do {
            clock.passedTime(5, TimeUnit.SECONDS);
        } while (!sync.await(10, TimeUnit.MILLISECONDS));
        assertEquals(1, alice.heartbeatsCount.get());
    }

    @Test
    public void sendHeartbeatsBeforeExpirationTime() throws InterruptedException {
        CountDownLatch sync = alice.heartbeatSync;
        alice.becomeLeader();
        heartbeatProcess.start();
        sync.await();
        assertEquals(1, alice.heartbeatsCount.get());

        CountDownLatch syncTwo = alice.heartbeatSync;
        long expiredAt = clock.millis() + alice.heartbeatExpireTimeMillis - TimeUnit.SECONDS.toMillis(1);
        do {
            clock.passedTime(5, TimeUnit.SECONDS);
        } while (!syncTwo.await(10, TimeUnit.MILLISECONDS));

        assertEquals(2, alice.heartbeatsCount.get());
        assertTrue(clock.millis() < expiredAt);
    }

    @Test
    public void heartbeatAsFastAsPossible() throws InterruptedException {
        CountDownLatch sync = alice.heartbeatSync;
        alice.becomeLeader();
        // we can lost time in gc, or on network, and should consider it during schedule next heartbeat
        alice.heartbeatExpireTimeMillis = TimeUnit.MILLISECONDS.toMillis(100);
        heartbeatProcess.start();
        sync.await();

        for (int index = 0; index < 3; index++) {
            CountDownLatch syncTwo = alice.heartbeatSync;
            do {
                clock.passedTime(100, TimeUnit.MILLISECONDS);
            } while (!syncTwo.await(10, TimeUnit.MILLISECONDS));
        }

        assertEquals(4, alice.heartbeatsCount.get());
    }

    private class AlertingNode extends AbstractAlertingNode {
        final AtomicInteger heartbeatsCount = new AtomicInteger();
        volatile boolean ready;
        volatile long leaderSeqNo;
        volatile long heartbeatExpireTimeMillis = TimeUnit.SECONDS.toMillis(30);
        volatile CountDownLatch heartbeatSync = new CountDownLatch(1);

        public AlertingNode(String name) {
            super(name);
        }

        public void setUp() {
            ready = true;
            logger.info("{} started", name);
        }

        @Override
        public CompletableFuture<THeartbeatResponse> heartbeat(THeartbeatRequest request) {
            return CompletableFuture.supplyAsync(() -> {
                if (!ready) {
                    throw Status.UNAVAILABLE
                        .withDescription("not ready yet to serve requests")
                        .asRuntimeException();
                }

                heartbeatsCount.incrementAndGet();
                CountDownLatch syncCopy = heartbeatSync;
                heartbeatSync = new CountDownLatch(1);
                try {
                    Objects.requireNonNull(request.getNode());

                    if (!isLeader()) {
                        syncCopy.countDown();
                        throw Status.ABORTED
                            .withDescription("I'm not a leader")
                            .asRuntimeException();
                    }

                    return THeartbeatResponse.newBuilder()
                        .setExpiredAt(clock.millis() + heartbeatExpireTimeMillis)
                        .setLeaderSeqNo(leaderSeqNo)
                        .setGlobalProjectSeqNo(42)
                        .build();
                } finally {
                    syncCopy.countDown();
                }
            });
        }

        public void tearDown() {
            logger.info("{} shutdown", name);
            ready = false;
        }

        public void becomeLeader() {
            logger.info("{} become leader", name);
            leaderSeqNo = balancerLock.setOwner(name);
        }

        public boolean isLeader() {
            String owner = balancerLock.lockDetail()
                .map(LockDetail::owner)
                .orElse(null);

            return Objects.equals(name, owner);
        }
    }
}
