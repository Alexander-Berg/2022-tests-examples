package ru.yandex.intranet.d.datasorce.coordination;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.Disposable;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.datasource.coordination.CoordinationClient;
import ru.yandex.intranet.d.datasource.coordination.Coordinator;
import ru.yandex.intranet.d.datasource.coordination.model.session.ChangesSubscription;
import ru.yandex.intranet.d.datasource.coordination.model.session.CoordinationSemaphore;
import ru.yandex.intranet.d.datasource.coordination.model.session.CoordinationSemaphoreDescription;
import ru.yandex.intranet.d.datasource.coordination.model.session.SessionState;
import ru.yandex.intranet.d.datasource.coordination.rpc.CoordinationRpc;
import ru.yandex.intranet.d.datasource.coordination.rpc.grpc.GrpcCoordinationRpc;
import ru.yandex.intranet.d.datasource.impl.YdbRpcTransport;
import ru.yandex.intranet.d.util.Barrier;

/**
 * Coordinator test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class CoordinatorTest {

    private static final Logger LOG = LoggerFactory.getLogger(CoordinatorTest.class);

    @Autowired
    private YdbRpcTransport ydbRpcTransport;
    @Value("${ydb.database}")
    private String database;

    @Test
    public void testStartStop() throws IOException, InterruptedException {
        try (CoordinationRpc rpc = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport())) {
            try (CoordinationClient client = CoordinationClient.newClient(rpc).build()) {
                LOG.info("Creating new coordinator...");
                Coordinator coordinator = Coordinator.newCoordinator(client, database + "/coordination-test").build();
                LOG.info("Coordinator created, starting coordinator...");
                coordinator.start();
                LOG.info("Сoordinator started");
                Barrier startBarrier = new Barrier();
                startBarrier.close();
                Barrier stopBarrier = new Barrier();
                stopBarrier.close();
                AtomicBoolean started = new AtomicBoolean(false);
                LOG.info("Subscribing to coordinator sessions...");
                coordinator.subscribeToSessionState(state -> {
                    LOG.info("Coordinator sessions state changed: {}", state);
                    if (state == SessionState.VALID) {
                        startBarrier.open();
                        started.set(true);
                    }
                    if (state == SessionState.INVALID && started.get()) {
                        stopBarrier.open();
                    }
                });
                LOG.info("Subscribed to coordinator sessions, waiting until session is created");
                startBarrier.passThrough();
                LOG.info("Coordinator session started");
                Assertions.assertEquals(SessionState.VALID, coordinator.getSessionState());
                Barrier barrier = new Barrier();
                barrier.close();
                LOG.info("Stopping coordinator...");
                coordinator.stop(barrier::open);
                LOG.info("Waiting until session is stopped...");
                stopBarrier.passThrough();
                LOG.info("Session is stopped");
                Assertions.assertEquals(SessionState.INVALID, coordinator.getSessionState());
                LOG.info("Waiting until coordinator is stopped...");
                barrier.passThrough();
                LOG.info("Coordinator is stopped");
            }
        }
        LOG.info("Coordinator lifecycle test is finished");
    }

    @Test
    public void testCreateDeleteSemaphore() throws IOException, InterruptedException {
        try (CoordinationRpc rpc = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport())) {
            try (CoordinationClient client = CoordinationClient.newClient(rpc).build()) {
                LOG.info("Creating new coordinator...");
                Coordinator coordinator = Coordinator.newCoordinator(client, database + "/coordination-test").build();
                LOG.info("Coordinator created, starting coordinator...");
                coordinator.start();
                LOG.info("Сoordinator started");
                Barrier startBarrier = new Barrier();
                startBarrier.close();
                Barrier stopBarrier = new Barrier();
                stopBarrier.close();
                AtomicBoolean started = new AtomicBoolean(false);
                LOG.info("Subscribing to coordinator sessions...");
                coordinator.subscribeToSessionState(state -> {
                    LOG.info("Coordinator sessions state changed: {}", state);
                    if (state == SessionState.VALID) {
                        startBarrier.open();
                        started.set(true);
                    }
                    if (state == SessionState.INVALID && started.get()) {
                        stopBarrier.open();
                    }
                });
                LOG.info("Subscribed to coordinator sessions, waiting until session is created");
                startBarrier.passThrough();
                LOG.info("Coordinator session started");
                Assertions.assertEquals(SessionState.VALID, coordinator.getSessionState());
                LOG.info("Creating semaphore...");
                CoordinationSemaphore semaphore = coordinator
                        .createSemaphore("test", 1L, new byte[] {0, 1, 2}).block();
                Assertions.assertNotNull(semaphore);
                LOG.info("Semaphore created");
                LOG.info("Deleting semaphore...");
                coordinator.deleteSemaphore(semaphore.getName(), true).block();
                LOG.info("Semaphore deleted");
                Barrier barrier = new Barrier();
                barrier.close();
                LOG.info("Stopping coordinator...");
                coordinator.stop(barrier::open);
                LOG.info("Waiting until session is stopped...");
                stopBarrier.passThrough();
                LOG.info("Session is stopped");
                Assertions.assertEquals(SessionState.INVALID, coordinator.getSessionState());
                LOG.info("Waiting until coordinator is stopped...");
                barrier.passThrough();
                LOG.info("Coordinator is stopped");
            }
        }
        LOG.info("Coordinator lifecycle test is finished");
    }

    @Test
    public void testCreateUpdateDeleteSemaphore() throws IOException, InterruptedException {
        try (CoordinationRpc rpc = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport())) {
            try (CoordinationClient client = CoordinationClient.newClient(rpc).build()) {
                LOG.info("Creating new coordinator...");
                Coordinator coordinator = Coordinator.newCoordinator(client, database + "/coordination-test").build();
                LOG.info("Coordinator created, starting coordinator...");
                coordinator.start();
                LOG.info("Сoordinator started");
                Barrier startBarrier = new Barrier();
                startBarrier.close();
                Barrier stopBarrier = new Barrier();
                stopBarrier.close();
                AtomicBoolean started = new AtomicBoolean(false);
                LOG.info("Subscribing to coordinator sessions...");
                coordinator.subscribeToSessionState(state -> {
                    LOG.info("Coordinator sessions state changed: {}", state);
                    if (state == SessionState.VALID) {
                        startBarrier.open();
                        started.set(true);
                    }
                    if (state == SessionState.INVALID && started.get()) {
                        stopBarrier.open();
                    }
                });
                LOG.info("Subscribed to coordinator sessions, waiting until session is created");
                startBarrier.passThrough();
                LOG.info("Coordinator session started");
                Assertions.assertEquals(SessionState.VALID, coordinator.getSessionState());
                LOG.info("Creating semaphore...");
                CoordinationSemaphore semaphore = coordinator
                        .createSemaphore("test", 1L, new byte[] {0, 1, 2}).block();
                Assertions.assertNotNull(semaphore);
                LOG.info("Semaphore created");
                LOG.info("Checking semaphore description before...");
                CoordinationSemaphoreDescription descriptionBefore = coordinator
                        .describeSemaphore(semaphore.getName(), false, false).block();
                Assertions.assertNotNull(descriptionBefore);
                Assertions.assertArrayEquals(new byte[] {0, 1, 2}, descriptionBefore.getData());
                LOG.info("Checked semaphore description before");
                LOG.info("Updating semaphore...");
                coordinator.updateSemaphore(semaphore.getName(), new byte[] {3, 4, 5}).block();
                LOG.info("Semaphore updated");
                LOG.info("Checking semaphore description after...");
                CoordinationSemaphoreDescription descriptionAfter = coordinator
                        .describeSemaphore(semaphore.getName(), false, false).block();
                Assertions.assertNotNull(descriptionAfter);
                Assertions.assertArrayEquals(new byte[] {3, 4, 5}, descriptionAfter.getData());
                LOG.info("Checked semaphore description after");
                LOG.info("Deleting semaphore...");
                coordinator.deleteSemaphore(semaphore.getName(), true).block();
                LOG.info("Semaphore deleted");
                Barrier barrier = new Barrier();
                barrier.close();
                LOG.info("Stopping coordinator...");
                coordinator.stop(barrier::open);
                LOG.info("Waiting until session is stopped...");
                stopBarrier.passThrough();
                LOG.info("Session is stopped");
                Assertions.assertEquals(SessionState.INVALID, coordinator.getSessionState());
                LOG.info("Waiting until coordinator is stopped...");
                barrier.passThrough();
                LOG.info("Coordinator is stopped");
            }
        }
        LOG.info("Coordinator lifecycle test is finished");
    }

    @Test
    public void testAcquireReleaseSemaphore() throws IOException, InterruptedException {
        try (CoordinationRpc rpc = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport())) {
            try (CoordinationClient client = CoordinationClient.newClient(rpc).build()) {
                LOG.info("Creating new coordinator...");
                Coordinator coordinator = Coordinator.newCoordinator(client, database + "/coordination-test").build();
                LOG.info("Coordinator created, starting coordinator...");
                coordinator.start();
                LOG.info("Сoordinator started");
                Barrier startBarrier = new Barrier();
                startBarrier.close();
                Barrier stopBarrier = new Barrier();
                stopBarrier.close();
                AtomicBoolean started = new AtomicBoolean(false);
                LOG.info("Subscribing to coordinator sessions...");
                coordinator.subscribeToSessionState(state -> {
                    LOG.info("Coordinator sessions state changed: {}", state);
                    if (state == SessionState.VALID) {
                        startBarrier.open();
                        started.set(true);
                    }
                    if (state == SessionState.INVALID && started.get()) {
                        stopBarrier.open();
                    }
                });
                LOG.info("Subscribed to coordinator sessions, waiting until session is created");
                startBarrier.passThrough();
                LOG.info("Coordinator session started");
                Assertions.assertEquals(SessionState.VALID, coordinator.getSessionState());
                LOG.info("Creating semaphore...");
                CoordinationSemaphore semaphore = coordinator
                        .createSemaphore("test", 1L, new byte[] {0, 1, 2}).block();
                Assertions.assertNotNull(semaphore);
                LOG.info("Semaphore created");
                LOG.info("Checking semaphore description before...");
                CoordinationSemaphoreDescription descriptionBefore = coordinator
                        .describeSemaphore(semaphore.getName(), false, false).block();
                Assertions.assertNotNull(descriptionBefore);
                Assertions.assertArrayEquals(new byte[] {0, 1, 2}, descriptionBefore.getData());
                Assertions.assertEquals(0L, descriptionBefore.getCount());
                Assertions.assertTrue(descriptionBefore.getOwners().isEmpty());
                LOG.info("Checked semaphore description before");
                LOG.info("Acquiring semaphore...");
                Boolean acquired = coordinator.acquireSemaphore(semaphore.getName(), Duration.ofSeconds(100), 1L,
                        new byte[] {6, 7, 8}, false).block();
                Assertions.assertNotNull(acquired);
                Assertions.assertTrue(acquired);
                LOG.info("Semaphore acquired");
                LOG.info("Checking semaphore description during...");
                CoordinationSemaphoreDescription descriptionDuring = coordinator
                        .describeSemaphore(semaphore.getName(), true, false).block();
                Assertions.assertNotNull(descriptionDuring);
                Assertions.assertArrayEquals(new byte[] {0, 1, 2}, descriptionDuring.getData());
                Assertions.assertEquals(1L, descriptionDuring.getCount());
                Assertions.assertFalse(descriptionDuring.getOwners().isEmpty());
                Assertions.assertArrayEquals(new byte[] {6, 7, 8}, descriptionDuring.getOwners().get(0).getData());
                LOG.info("Checked semaphore description during");
                LOG.info("Releasing semaphore...");
                Boolean released = coordinator.releaseSemaphore(semaphore.getName()).block();
                Assertions.assertNotNull(released);
                Assertions.assertTrue(released);
                LOG.info("Semaphore released");
                LOG.info("Checking semaphore description after...");
                CoordinationSemaphoreDescription descriptionAfter = coordinator
                        .describeSemaphore(semaphore.getName(), false, false).block();
                Assertions.assertNotNull(descriptionAfter);
                Assertions.assertArrayEquals(new byte[] {0, 1, 2}, descriptionAfter.getData());
                Assertions.assertEquals(0L, descriptionAfter.getCount());
                Assertions.assertTrue(descriptionAfter.getOwners().isEmpty());
                LOG.info("Checked semaphore description after");
                LOG.info("Deleting semaphore...");
                coordinator.deleteSemaphore(semaphore.getName(), true).block();
                LOG.info("Semaphore deleted");
                Barrier barrier = new Barrier();
                barrier.close();
                LOG.info("Stopping coordinator...");
                coordinator.stop(barrier::open);
                LOG.info("Waiting until session is stopped...");
                stopBarrier.passThrough();
                LOG.info("Session is stopped");
                Assertions.assertEquals(SessionState.INVALID, coordinator.getSessionState());
                LOG.info("Waiting until coordinator is stopped...");
                barrier.passThrough();
                LOG.info("Coordinator is stopped");
            }
        }
        LOG.info("Coordinator lifecycle test is finished");
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testAcquireReleaseSemaphoreTwoClients() throws IOException, InterruptedException {
        try (CoordinationRpc rpcOne = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport());
             CoordinationRpc rpcTwo = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport())) {
            try (CoordinationClient clientOne = CoordinationClient.newClient(rpcOne).build();
                 CoordinationClient clientTwo = CoordinationClient.newClient(rpcTwo).build()) {
                LOG.info("Creating first new coordinator...");
                Coordinator coordinatorOne = Coordinator.newCoordinator(clientOne, database + "/coordination-test")
                        .build();
                LOG.info("First coordinator created");
                LOG.info("Creating second new coordinator...");
                Coordinator coordinatorTwo = Coordinator.newCoordinator(clientTwo, database + "/coordination-test")
                        .build();
                LOG.info("Second coordinator created");
                LOG.info("Starting first coordinator...");
                coordinatorOne.start();
                LOG.info("First coordinator started");
                LOG.info("Starting second coordinator...");
                coordinatorTwo.start();
                LOG.info("Second coordinator started");
                Barrier startBarrierOne = new Barrier();
                startBarrierOne.close();
                Barrier startBarrierTwo = new Barrier();
                startBarrierTwo.close();
                Barrier stopBarrierOne = new Barrier();
                stopBarrierOne.close();
                Barrier stopBarrierTwo = new Barrier();
                stopBarrierTwo.close();
                AtomicBoolean startedOne = new AtomicBoolean(false);
                LOG.info("Subscribing to first coordinator sessions...");
                coordinatorOne.subscribeToSessionState(state -> {
                    LOG.info("First coordinator sessions state changed: {}", state);
                    if (state == SessionState.VALID) {
                        startBarrierOne.open();
                        startedOne.set(true);
                    }
                    if (state == SessionState.INVALID && startedOne.get()) {
                        stopBarrierOne.open();
                    }
                });
                LOG.info("Subscribed to first coordinator sessions");
                AtomicBoolean startedTwo = new AtomicBoolean(false);
                LOG.info("Subscribing to second coordinator sessions...");
                coordinatorTwo.subscribeToSessionState(state -> {
                    LOG.info("Second coordinator sessions state changed: {}", state);
                    if (state == SessionState.VALID) {
                        startBarrierTwo.open();
                        startedTwo.set(true);
                    }
                    if (state == SessionState.INVALID && startedTwo.get()) {
                        stopBarrierTwo.open();
                    }
                });
                LOG.info("Subscribed to second coordinator sessions");
                LOG.info("Waiting until first coordinator session is created...");
                startBarrierOne.passThrough();
                LOG.info("First coordinator session started");
                Assertions.assertEquals(SessionState.VALID, coordinatorOne.getSessionState());
                LOG.info("Waiting until second coordinator session is created...");
                startBarrierTwo.passThrough();
                LOG.info("Second coordinator session started");
                Assertions.assertEquals(SessionState.VALID, coordinatorTwo.getSessionState());
                LOG.info("Creating semaphore in first session...");
                CoordinationSemaphore semaphore = coordinatorOne
                        .createSemaphore("test", 1L, new byte[] {0, 1, 2}).block();
                Assertions.assertNotNull(semaphore);
                LOG.info("Semaphore created in first session");
                LOG.info("Checking semaphore description before...");
                CoordinationSemaphoreDescription descriptionBefore = coordinatorOne
                        .describeSemaphore(semaphore.getName(), false, false).block();
                Assertions.assertNotNull(descriptionBefore);
                Assertions.assertArrayEquals(new byte[] {0, 1, 2}, descriptionBefore.getData());
                Assertions.assertEquals(0L, descriptionBefore.getCount());
                Assertions.assertTrue(descriptionBefore.getOwners().isEmpty());
                LOG.info("Checked semaphore description before");
                LOG.info("Acquiring semaphore in first session...");
                Boolean acquired = coordinatorOne.acquireSemaphore(semaphore.getName(), Duration.ofSeconds(100), 1L,
                        new byte[] {6, 7, 8}, false).block();
                Assertions.assertNotNull(acquired);
                Assertions.assertTrue(acquired);
                LOG.info("Semaphore acquired in first session");
                LOG.info("Checking semaphore description one owner...");
                CoordinationSemaphoreDescription descriptionOneOwner = coordinatorOne
                        .describeSemaphore(semaphore.getName(), true, false).block();
                Assertions.assertNotNull(descriptionOneOwner);
                Assertions.assertArrayEquals(new byte[] {0, 1, 2}, descriptionOneOwner.getData());
                Assertions.assertEquals(1L, descriptionOneOwner.getCount());
                Assertions.assertFalse(descriptionOneOwner.getOwners().isEmpty());
                Assertions.assertArrayEquals(new byte[] {6, 7, 8}, descriptionOneOwner.getOwners().get(0).getData());
                LOG.info("Checked semaphore description one owner");
                AtomicBoolean acquiredTwo = new AtomicBoolean(false);
                Barrier acquiredBarrier = new Barrier();
                acquiredBarrier.close();
                LOG.info("Subscribing to acquire subscription in second session...");
                Disposable acquireSubscription = coordinatorTwo.acquireSemaphore(semaphore.getName(),
                        Duration.ofSeconds(100), 1L, new byte[] {7, 8, 9}, false).subscribe(acq -> {
                            acquiredTwo.set(acq);
                            acquiredBarrier.open();
                });
                LOG.info("Subscribed to acquire subscription in second session");
                LOG.info("Sleeping...");
                Thread.sleep(300);
                LOG.info("Checking semaphore description one waiter...");
                CoordinationSemaphoreDescription descriptionOnePending = coordinatorOne
                        .describeSemaphore(semaphore.getName(), true, true).block();
                Assertions.assertNotNull(descriptionOnePending);
                Assertions.assertArrayEquals(new byte[] {0, 1, 2}, descriptionOnePending.getData());
                Assertions.assertEquals(1L, descriptionOnePending.getCount());
                Assertions.assertFalse(descriptionOnePending.getOwners().isEmpty());
                Assertions.assertArrayEquals(new byte[] {6, 7, 8},
                        descriptionOnePending.getOwners().get(0).getData());
                Assertions.assertFalse(descriptionOnePending.getWaiters().isEmpty());
                Assertions.assertArrayEquals(new byte[] {7, 8, 9},
                        descriptionOnePending.getWaiters().get(0).getData());
                LOG.info("Checked semaphore description one waiter");
                LOG.info("Releasing semaphore in first session...");
                Boolean released = coordinatorOne.releaseSemaphore(semaphore.getName()).block();
                Assertions.assertNotNull(released);
                Assertions.assertTrue(released);
                LOG.info("Semaphore released in first session");
                LOG.info("Waiting for semaphore acquire in second session...");
                acquiredBarrier.passThrough();
                LOG.info("Semaphore acquired in second session");
                Assertions.assertTrue(acquiredTwo.get());
                LOG.info("Checking semaphore description one owner second time...");
                CoordinationSemaphoreDescription descriptionOneOwnerTwo = coordinatorTwo
                        .describeSemaphore(semaphore.getName(), true, false).block();
                Assertions.assertNotNull(descriptionOneOwnerTwo);
                Assertions.assertArrayEquals(new byte[] {0, 1, 2}, descriptionOneOwnerTwo.getData());
                Assertions.assertEquals(1L, descriptionOneOwnerTwo.getCount());
                Assertions.assertFalse(descriptionOneOwnerTwo.getOwners().isEmpty());
                Assertions.assertArrayEquals(new byte[] {7, 8, 9},
                        descriptionOneOwnerTwo.getOwners().get(0).getData());
                LOG.info("Checked semaphore description one owner second time");
                LOG.info("Releasing semaphore in second session...");
                Boolean releasedTwo = coordinatorTwo.releaseSemaphore(semaphore.getName()).block();
                Assertions.assertNotNull(releasedTwo);
                Assertions.assertTrue(releasedTwo);
                LOG.info("Semaphore released in second session");
                LOG.info("Checking semaphore description after...");
                CoordinationSemaphoreDescription descriptionAfter = coordinatorTwo
                        .describeSemaphore(semaphore.getName(), false, false).block();
                Assertions.assertNotNull(descriptionAfter);
                Assertions.assertArrayEquals(new byte[] {0, 1, 2}, descriptionAfter.getData());
                Assertions.assertEquals(0L, descriptionAfter.getCount());
                Assertions.assertTrue(descriptionAfter.getOwners().isEmpty());
                LOG.info("Checked semaphore description after");
                LOG.info("Deleting semaphore in first session...");
                coordinatorOne.deleteSemaphore(semaphore.getName(), true).block();
                LOG.info("Semaphore deleted in first session");
                Barrier barrier = new Barrier();
                acquireSubscription.dispose();
                barrier.close();
                LOG.info("Stopping first coordinator...");
                coordinatorOne.stop(barrier::open);
                LOG.info("Waiting until first session is stopped...");
                stopBarrierOne.passThrough();
                LOG.info("First session is stopped");
                Assertions.assertEquals(SessionState.INVALID, coordinatorOne.getSessionState());
                LOG.info("Waiting until first coordinator is stopped...");
                barrier.passThrough();
                LOG.info("First coordinator is stopped");
                Barrier barrierTwo = new Barrier();
                barrierTwo.close();
                LOG.info("Stopping second coordinator...");
                coordinatorTwo.stop(barrierTwo::open);
                LOG.info("Waiting until second session is stopped...");
                stopBarrierTwo.passThrough();
                LOG.info("Second session is stopped");
                Assertions.assertEquals(SessionState.INVALID, coordinatorTwo.getSessionState());
                LOG.info("Waiting until second coordinator is stopped...");
                barrierTwo.passThrough();
                LOG.info("Second coordinator is stopped");
            }
        }
        LOG.info("Coordinator lifecycle test is finished");
    }

    @Test
    public void testSubscribeData() throws IOException, InterruptedException {
        try (CoordinationRpc rpc = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport())) {
            try (CoordinationClient client = CoordinationClient.newClient(rpc).build()) {
                LOG.info("Creating new coordinator...");
                Coordinator coordinator = Coordinator.newCoordinator(client, database + "/coordination-test").build();
                LOG.info("Coordinator created, starting coordinator...");
                coordinator.start();
                LOG.info("Сoordinator started");
                Barrier startBarrier = new Barrier();
                startBarrier.close();
                Barrier stopBarrier = new Barrier();
                stopBarrier.close();
                AtomicBoolean started = new AtomicBoolean(false);
                LOG.info("Subscribing to coordinator sessions...");
                coordinator.subscribeToSessionState(state -> {
                    LOG.info("Coordinator sessions state changed: {}", state);
                    if (state == SessionState.VALID) {
                        startBarrier.open();
                        started.set(true);
                    }
                    if (state == SessionState.INVALID && started.get()) {
                        stopBarrier.open();
                    }
                });
                LOG.info("Subscribed to coordinator sessions, waiting until session is created");
                startBarrier.passThrough();
                LOG.info("Coordinator session started");
                Assertions.assertEquals(SessionState.VALID, coordinator.getSessionState());
                LOG.info("Creating semaphore...");
                CoordinationSemaphore semaphore = coordinator
                        .createSemaphore("test", 1L, new byte[] {0, 1, 2}).block();
                Assertions.assertNotNull(semaphore);
                LOG.info("Semaphore created");
                LOG.info("Checking semaphore description before...");
                CoordinationSemaphoreDescription descriptionBefore = coordinator
                        .describeSemaphore(semaphore.getName(), false, false).block();
                Assertions.assertNotNull(descriptionBefore);
                Assertions.assertArrayEquals(new byte[] {0, 1, 2}, descriptionBefore.getData());
                LOG.info("Checked semaphore description before");
                LOG.info("Subscribing to events first time...");
                Barrier eventBarrierOne = new Barrier();
                eventBarrierOne.close();
                AtomicLong eventsReceivedOne = new AtomicLong(0L);
                ChangesSubscription changesSubscriptionOne = coordinator
                        .subscribeToChanges(semaphore.getName(), false, false, true, false).block();
                Assertions.assertNotNull(changesSubscriptionOne);
                Disposable eventSubscriptionOne = changesSubscriptionOne.getSubscription().subscribe(e -> {
                            LOG.info("Semaphore first event received: {}", e);
                            eventsReceivedOne.incrementAndGet();
                            eventBarrierOne.open();
                        });
                LOG.info("Subscribed to events first time");
                LOG.info("Updating semaphore first time...");
                coordinator.updateSemaphore(semaphore.getName(), new byte[] {3, 4, 5}).block();
                LOG.info("Semaphore updated first time");
                LOG.info("Waiting until first semaphore event...");
                eventBarrierOne.passThrough();
                LOG.info("First semaphore event is received");
                Assertions.assertEquals(1L, eventsReceivedOne.get());
                eventSubscriptionOne.dispose();
                LOG.info("Subscribing to events second time...");
                Barrier eventBarrierTwo = new Barrier();
                eventBarrierTwo.close();
                AtomicLong eventsReceivedTwo = new AtomicLong(0L);
                ChangesSubscription changesSubscriptionTwo = coordinator
                        .subscribeToChanges(semaphore.getName(), false, false, true, false).block();
                Assertions.assertNotNull(changesSubscriptionTwo);
                Disposable eventSubscriptionTwo = changesSubscriptionTwo.getSubscription().subscribe(e -> {
                    LOG.info("Semaphore second event received: {}", e);
                    eventsReceivedTwo.incrementAndGet();
                    eventBarrierTwo.open();
                });
                LOG.info("Subscribed to events second time");
                LOG.info("Updating semaphore second time...");
                coordinator.updateSemaphore(semaphore.getName(), new byte[] {5, 6, 7}).block();
                LOG.info("Semaphore updated second time");
                LOG.info("Waiting until second semaphore event...");
                eventBarrierTwo.passThrough();
                LOG.info("Second semaphore event is received");
                Assertions.assertEquals(1L, eventsReceivedTwo.get());
                eventSubscriptionTwo.dispose();
                LOG.info("Checking semaphore description after...");
                CoordinationSemaphoreDescription descriptionAfter = coordinator
                        .describeSemaphore(semaphore.getName(), false, false).block();
                Assertions.assertNotNull(descriptionAfter);
                Assertions.assertArrayEquals(new byte[] {5, 6, 7}, descriptionAfter.getData());
                LOG.info("Checked semaphore description after");
                LOG.info("Subscribing to events third time...");
                Barrier eventBarrierThree = new Barrier();
                eventBarrierThree.close();
                AtomicLong eventsReceivedThree = new AtomicLong(0L);
                ChangesSubscription changesSubscriptionThree = coordinator
                        .subscribeToChanges(semaphore.getName(), false, false, true, false).block();
                Assertions.assertNotNull(changesSubscriptionThree);
                Disposable eventSubscriptionThree = changesSubscriptionThree.getSubscription().subscribe(e -> {
                    LOG.info("Semaphore third event received: {}", e);
                    eventsReceivedThree.incrementAndGet();
                    eventBarrierThree.open();
                });
                LOG.info("Subscribed to events third time");
                LOG.info("Updating semaphore third time...");
                coordinator.updateSemaphore(semaphore.getName(), new byte[] {6, 7, 8}).block();
                LOG.info("Semaphore updated third time");
                LOG.info("Waiting until third semaphore event...");
                eventBarrierThree.passThrough();
                LOG.info("Third semaphore event is received");
                Assertions.assertEquals(1L, eventsReceivedThree.get());
                eventSubscriptionThree.dispose();
                LOG.info("Checking semaphore description after third time...");
                CoordinationSemaphoreDescription descriptionAfterSecond = coordinator
                        .describeSemaphore(semaphore.getName(), false, false).block();
                Assertions.assertNotNull(descriptionAfterSecond);
                Assertions.assertArrayEquals(new byte[] {6, 7, 8}, descriptionAfterSecond.getData());
                LOG.info("Checked semaphore description after third time");
                LOG.info("Deleting semaphore...");
                coordinator.deleteSemaphore(semaphore.getName(), true).block();
                LOG.info("Semaphore deleted");
                Barrier barrier = new Barrier();
                barrier.close();
                LOG.info("Stopping coordinator...");
                coordinator.stop(barrier::open);
                LOG.info("Waiting until session is stopped...");
                stopBarrier.passThrough();
                LOG.info("Session is stopped");
                Assertions.assertEquals(SessionState.INVALID, coordinator.getSessionState());
                LOG.info("Waiting until coordinator is stopped...");
                barrier.passThrough();
                LOG.info("Coordinator is stopped");
            }
        }
        LOG.info("Coordinator lifecycle test is finished");
    }

    @Test
    public void testSubscribeOwner() throws IOException, InterruptedException {
        try (CoordinationRpc rpc = GrpcCoordinationRpc.useTransport(ydbRpcTransport.getRpcTransport())) {
            try (CoordinationClient client = CoordinationClient.newClient(rpc).build()) {
                LOG.info("Creating new coordinator...");
                Coordinator coordinator = Coordinator.newCoordinator(client, database + "/coordination-test").build();
                LOG.info("Coordinator created, starting coordinator...");
                coordinator.start();
                LOG.info("Сoordinator started");
                Barrier startBarrier = new Barrier();
                startBarrier.close();
                Barrier stopBarrier = new Barrier();
                stopBarrier.close();
                AtomicBoolean started = new AtomicBoolean(false);
                LOG.info("Subscribing to coordinator sessions...");
                coordinator.subscribeToSessionState(state -> {
                    LOG.info("Coordinator sessions state changed: {}", state);
                    if (state == SessionState.VALID) {
                        startBarrier.open();
                        started.set(true);
                    }
                    if (state == SessionState.INVALID && started.get()) {
                        stopBarrier.open();
                    }
                });
                LOG.info("Subscribed to coordinator sessions, waiting until session is created");
                startBarrier.passThrough();
                LOG.info("Coordinator session started");
                Assertions.assertEquals(SessionState.VALID, coordinator.getSessionState());
                LOG.info("Creating semaphore...");
                CoordinationSemaphore semaphore = coordinator
                        .createSemaphore("test", 1L, new byte[] {0, 1, 2}).block();
                Assertions.assertNotNull(semaphore);
                LOG.info("Semaphore created");
                LOG.info("Checking semaphore description before...");
                CoordinationSemaphoreDescription descriptionBefore = coordinator
                        .describeSemaphore(semaphore.getName(), false, false).block();
                Assertions.assertNotNull(descriptionBefore);
                Assertions.assertArrayEquals(new byte[] {0, 1, 2}, descriptionBefore.getData());
                Assertions.assertEquals(0L, descriptionBefore.getCount());
                Assertions.assertTrue(descriptionBefore.getOwners().isEmpty());
                LOG.info("Checked semaphore description before");
                LOG.info("Subscribing to events first time...");
                Barrier eventBarrierOne = new Barrier();
                eventBarrierOne.close();
                AtomicLong eventsReceivedOne = new AtomicLong(0L);
                ChangesSubscription changesSubscriptionOne = coordinator
                        .subscribeToChanges(semaphore.getName(), false, false, false, true).block();
                Assertions.assertNotNull(changesSubscriptionOne);
                Disposable eventSubscriptionOne = changesSubscriptionOne.getSubscription().subscribe(e -> {
                    LOG.info("Semaphore first event received: {}", e);
                    eventsReceivedOne.incrementAndGet();
                    eventBarrierOne.open();
                });
                LOG.info("Subscribed to events first time");
                LOG.info("Acquiring semaphore...");
                Boolean acquired = coordinator.acquireSemaphore(semaphore.getName(), Duration.ofSeconds(100), 1L,
                        new byte[] {6, 7, 8}, false).block();
                Assertions.assertNotNull(acquired);
                Assertions.assertTrue(acquired);
                LOG.info("Semaphore acquired");
                LOG.info("Waiting until first semaphore event...");
                eventBarrierOne.passThrough();
                LOG.info("First semaphore event is received");
                Assertions.assertEquals(1L, eventsReceivedOne.get());
                eventSubscriptionOne.dispose();
                LOG.info("Checking semaphore description during...");
                CoordinationSemaphoreDescription descriptionDuring = coordinator
                        .describeSemaphore(semaphore.getName(), true, false).block();
                Assertions.assertNotNull(descriptionDuring);
                Assertions.assertArrayEquals(new byte[] {0, 1, 2}, descriptionDuring.getData());
                Assertions.assertEquals(1L, descriptionDuring.getCount());
                Assertions.assertFalse(descriptionDuring.getOwners().isEmpty());
                Assertions.assertArrayEquals(new byte[] {6, 7, 8}, descriptionDuring.getOwners().get(0).getData());
                LOG.info("Checked semaphore description during");
                LOG.info("Subscribing to events second time...");
                Barrier eventBarrierTwo = new Barrier();
                eventBarrierTwo.close();
                AtomicLong eventsReceivedTwo = new AtomicLong(0L);
                ChangesSubscription changesSubscriptionTwo = coordinator
                        .subscribeToChanges(semaphore.getName(), false, false, false, true).block();
                Assertions.assertNotNull(changesSubscriptionTwo);
                Disposable eventSubscriptionTwo = changesSubscriptionTwo.getSubscription().subscribe(e -> {
                    LOG.info("Semaphore second event received: {}", e);
                    eventsReceivedTwo.incrementAndGet();
                    eventBarrierTwo.open();
                });
                LOG.info("Subscribed to events second time");
                LOG.info("Releasing semaphore...");
                Boolean released = coordinator.releaseSemaphore(semaphore.getName()).block();
                Assertions.assertNotNull(released);
                Assertions.assertTrue(released);
                LOG.info("Semaphore released");
                LOG.info("Waiting until second semaphore event...");
                eventBarrierTwo.passThrough();
                LOG.info("Second semaphore event is received");
                Assertions.assertEquals(1L, eventsReceivedTwo.get());
                eventSubscriptionTwo.dispose();
                LOG.info("Checking semaphore description after...");
                CoordinationSemaphoreDescription descriptionAfter = coordinator
                        .describeSemaphore(semaphore.getName(), false, false).block();
                Assertions.assertNotNull(descriptionAfter);
                Assertions.assertArrayEquals(new byte[] {0, 1, 2}, descriptionAfter.getData());
                Assertions.assertEquals(0L, descriptionAfter.getCount());
                Assertions.assertTrue(descriptionAfter.getOwners().isEmpty());
                LOG.info("Checked semaphore description after");
                LOG.info("Deleting semaphore...");
                coordinator.deleteSemaphore(semaphore.getName(), true).block();
                LOG.info("Semaphore deleted");
                Barrier barrier = new Barrier();
                barrier.close();
                LOG.info("Stopping coordinator...");
                coordinator.stop(barrier::open);
                LOG.info("Waiting until session is stopped...");
                stopBarrier.passThrough();
                LOG.info("Session is stopped");
                Assertions.assertEquals(SessionState.INVALID, coordinator.getSessionState());
                LOG.info("Waiting until coordinator is stopped...");
                barrier.passThrough();
                LOG.info("Coordinator is stopped");
            }
        }
        LOG.info("Coordinator lifecycle test is finished");
    }

}
