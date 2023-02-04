package ru.yandex.solomon.alert.cluster.broker.alert;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.solomon.alert.api.converters.NotificationConverter;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.search.AlertPersistStateSupport;
import ru.yandex.solomon.alert.cluster.project.ProjectAssignment;
import ru.yandex.solomon.alert.dao.AlertStatesDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertStatesDao;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.protobuf.TPersistAlertState;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class SnapshotProcessTest {
    private ManualClock clock;
    private ManualScheduledExecutorService timer;
    private ProjectAssignment assignment;
    private AlertStatesDao dao;
    private SnapshotProcess flushProcess;

    private ConcurrentMap<String, TPersistAlertState> stateByAlertId = new ConcurrentHashMap<>();
    private AlertPersistStateSupport stateSupport;
    @Rule
    public Timeout timeoutRule = Timeout.builder()
        .withLookingForStuckThread(true)
        .withTimeout(15, TimeUnit.SECONDS)
        .build();

    @Before
    public void setUp() throws Exception {
        clock = new ManualClock();
        timer = new ManualScheduledExecutorService(2, clock);
        assignment = new ProjectAssignment("junk", "localhost", new AssignmentSeqNo(10, 42));
        dao = new InMemoryAlertStatesDao();
        flushProcess = new SnapshotProcess(
                clock,
                ForkJoinPool.commonPool(),
                timer,
                assignment,
                dao,
                () -> ImmutableList.copyOf(stateByAlertId.values()),
                Duration.ofMinutes(2));
        flushProcess.run();
        stateSupport = new AlertPersistStateSupport(new NotificationConverter(new ChatIdResolverStub()));
    }

    @After
    public void tearDown() throws Exception {
        flushProcess.cancel();
        timer.shutdownNow();
    }

    @Test
    public void stateFlushDuringEachInterval() throws InterruptedException {
        addState(stateSupport.randomState());
        awaitScheduledFlush();

        Map<String, TPersistAlertState> v1 = restoreStates();
        assertThat(v1, equalTo(stateByAlertId));

        addState(stateSupport.randomState());
        awaitScheduledFlush();

        Map<String, TPersistAlertState> v2 = restoreStates();
        assertThat(v2, not(equalTo(v1)));
        assertThat(v2, equalTo(stateByAlertId));
    }

    @Test
    public void forceFlushNotAffectScheduled() throws InterruptedException {
        addState(stateSupport.randomState());
        awaitScheduledFlush();

        Map<String, TPersistAlertState> v1 = restoreStates();
        assertThat(v1, equalTo(stateByAlertId));

        clock.passedTime(1, TimeUnit.MINUTES);
        addState(stateSupport.randomState());
        flushProcess.flush().join();

        Map<String, TPersistAlertState> v2 = restoreStates();
        assertThat(v2, equalTo(stateByAlertId));

        addState(stateSupport.randomState());
        awaitScheduledFlush();

        Map<String, TPersistAlertState> v3 = restoreStates();
        assertThat(v3, equalTo(stateByAlertId));
    }

    @Test(expected = CancellationException.class)
    public void cancelProcess() {
        flushProcess.cancel();
        flushProcess.awaitFlush().join();
    }

    @Test
    public void cancelNotStartedProcess() {
        flushProcess.cancel();

        flushProcess = new SnapshotProcess(clock,
                ForkJoinPool.commonPool(),
                timer,
                assignment,
                dao,
                () -> ImmutableList.copyOf(stateByAlertId.values()),
                Duration.ofMinutes(2));

        flushProcess.cancel();
    }

    @Test
    public void exceptionFromSupplierNotAffectScheduling() throws InterruptedException {
        flushProcess.cancel();

        AtomicBoolean failGet = new AtomicBoolean(true);

        flushProcess = new SnapshotProcess(clock,
                ForkJoinPool.commonPool(),
                timer,
                assignment,
                dao,
                () -> {
                    if (failGet.get()) {
                        throw new IllegalStateException("Not ready yet to flush");
                    }
                    return ImmutableList.copyOf(stateByAlertId.values());
                },
                Duration.ofMinutes(2));
        flushProcess.run();

        addState(stateSupport.randomState());
        CompletableFuture<?> flushSync = flushProcess.awaitFlush();
        clock.passedTime(5, TimeUnit.MINUTES);
        boolean failed = flushSync.thenApply(ignore -> false)
                .exceptionally(e -> true)
                .join();
        assertThat(failed, equalTo(true));

        Map<String, TPersistAlertState> v1 = restoreStates();
        assertThat(v1, not(equalTo(stateByAlertId)));

        addState(stateSupport.randomState());
        failGet.set(false);
        awaitScheduledFlush();

        Map<String, TPersistAlertState> v2 = restoreStates();
        assertThat(v2, equalTo(stateByAlertId));
    }

    private void addState(TPersistAlertState state) {
        stateByAlertId.put(state.getId(), state);
    }

    private void awaitScheduledFlush() throws InterruptedException {
        CompletableFuture<?> flushSync = flushProcess.awaitFlush();
        clock.passedTime(5, TimeUnit.MINUTES);
        do {
            clock.passedTime(1, TimeUnit.MINUTES);
            TimeUnit.MILLISECONDS.sleep(2);
        } while (!flushSync.isDone());
        flushSync.join();
    }

    private Map<String, TPersistAlertState> restoreStates() {
        return dao.findAll(assignment.getProjectId()).join()
                .stream()
                .collect(Collectors.toMap(TPersistAlertState::getId, Function.identity()));
    }
}
