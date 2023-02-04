package ru.yandex.solomon.alert.evaluation;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.executor.local.LocalAlertExecutorOptions;
import ru.yandex.solomon.alert.rule.AlertRule;
import ru.yandex.solomon.alert.rule.AlertRuleFactoryStub;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.alert.unroll.MultiAlertUnrollFactory;
import ru.yandex.solomon.main.logger.LoggerConfigurationUtils;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.alert.rule.stubs.AlertRuleStubs.constantRule;
import static ru.yandex.solomon.alert.rule.stubs.AlertRuleStubs.countDownWhenReadyRule;
import static ru.yandex.solomon.alert.util.CommonMatchers.reflectionEqualTo;

/**
 * @author Vladimir Gordiychuk
 */
public class TaskExecutorImplTest {
    @Rule
    public Timeout globalTimeout = Timeout.builder()
            .withLookingForStuckThread(true)
            .withTimeout(30, TimeUnit.SECONDS)
            .build();

    private ManualClock clock;
    private ScheduledExecutorService executorService;
    private TaskExecutorImpl executor;
    private LocalAlertExecutorOptions options;
    private AlertRuleFactoryStub ruleFactory;

    @BeforeClass
    public static void beforeClass() {
        LoggerConfigurationUtils.simpleLogger(Level.INFO);
    }

    @Before
    public void setUp() throws Exception {
        clock = new ManualClock();
        executorService = new ManualScheduledExecutorService(2, clock);
        ruleFactory = new AlertRuleFactoryStub(executorService);
        options = LocalAlertExecutorOptions.newBuilder()
                .setAlertRuleTimeout(5L, TimeUnit.MILLISECONDS)
                .setEvalInterval(50L, TimeUnit.MILLISECONDS)
                .setMaxConcurrentAlertRules(10)
                .setMaxConcurrentWarmupAlertRules(3)
                .setMaxAlertRules(0)
                .setMaxEvaluationLag(1, TimeUnit.HOURS)
                .setMaxEvaluationJitter(2, TimeUnit.MILLISECONDS)
                .build();

        var evaluationMetrics = new TaskEvaluationMetrics(MetricRegistry.root());
        executor = new TaskExecutorImpl(clock, executorService, executorService, options,
                MetricRegistry.root().subRegistry("executor", "DEFAULT"), evaluationMetrics);
    }

    @After
    public void tearDown() throws Exception {
        executor.close();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    public void continueEvaluating() throws InterruptedException {
        CountDownLatch sync = new CountDownLatch(4);
        AlertRule rule = countDownWhenReadyRule(constantRule(randomAlert(), EvaluationStatus.OK), sync);

        Queue<EvaluationState> queue = new ConcurrentLinkedQueue<>();
        Instant now = clock.instant().minusMillis(options.getEvalIntervalMillis() * 4);
        executor.scheduleNewTask(new Task(now.toEpochMilli(), rule, null, new EvaluationService.Consumer() {
            @Override
            public boolean isCanceled() {
                return false;
            }

            @Override
            public void consume(@Nonnull EvaluationState state) {
                queue.add(state);
            }

            @Override
            public void onComplete() {
            }
        }));

        sync.await();
        assertThat(queue.remove().getLatestEval(), equalTo(now));
        assertThat(queue.remove().getLatestEval(), equalTo(now.plusMillis(options.getEvalIntervalMillis())));
        assertThat(queue.remove().getLatestEval(), equalTo(now.plusMillis(options.getEvalIntervalMillis() * 2)));
    }

    @Test
    public void evaluationContainsActualStatus() throws InterruptedException {
        EvaluationStatus[] expectedStatuses = {
                EvaluationStatus.OK.withAnnotations(ImmutableMap.of("value", "42")),
                EvaluationStatus.OK.withAnnotations(ImmutableMap.of("value", "43")),
                EvaluationStatus.OK.withAnnotations(ImmutableMap.of("value", "44")),
                EvaluationStatus.OK.withAnnotations(ImmutableMap.of("value", "45"))
        };

        CountDownLatch sync = new CountDownLatch(expectedStatuses.length + 1);
        AlertRule rule = countDownWhenReadyRule(constantRule(randomAlert(), expectedStatuses), sync);

        Queue<EvaluationState> queue = new ConcurrentLinkedQueue<>();
        Instant now = clock.instant().minusMillis(options.getEvalIntervalMillis() * 4);
        executor.scheduleNewTask(new Task(now.toEpochMilli(), rule, null, new EvaluationService.Consumer() {
            @Override
            public boolean isCanceled() {
                return false;
            }

            @Override
            public void consume(@Nonnull EvaluationState state) {
                queue.add(state);
            }

            @Override
            public void onComplete() {
            }
        }));

        sync.await();
        for (EvaluationStatus expectedStatus : expectedStatuses) {
            assertThat(queue.remove().getStatus(), reflectionEqualTo(expectedStatus));
        }
    }

    @Test
    public void laggingTaskCanSkipInterval() throws InterruptedException {
        CountDownLatch sync = new CountDownLatch(2);
        AlertRule rule = constantRule(randomAlert(), EvaluationStatus.OK);

        Queue<EvaluationState> queue = new ConcurrentLinkedQueue<>();
        Instant now = clock.instant().minus(9, ChronoUnit.HOURS);
        executor.scheduleNewTask(new Task(now.toEpochMilli(), rule, null, new EvaluationService.Consumer() {
            @Override
            public boolean isCanceled() {
                return false;
            }

            @Override
            public void consume(@Nonnull EvaluationState state) {
                queue.add(state);
                sync.countDown();
            }

            @Override
            public void onComplete() {
            }
        }));

        sync.await();
        assertThat(queue.remove().getLatestEval(), equalTo(now));
        assertThat(queue.remove().getLatestEval(), greaterThan(now.plus(7, ChronoUnit.HOURS)));
    }

    @Test
    public void retryEvaluation() throws InterruptedException {
        CountDownLatch sync = new CountDownLatch(3);
        var deadline = EvaluationStatus.ERROR.withErrorCode(EvaluationStatus.ErrorCode.DEADLINE);
        AlertRule rule = constantRule(randomAlert(), deadline, deadline, EvaluationStatus.OK);

        Queue<EvaluationState> queue = new ConcurrentLinkedQueue<>();
        Instant now = clock.instant().minusMillis(options.getEvalIntervalMillis() * 6);
        executor.scheduleNewTask(new Task(now.toEpochMilli(), rule, null, new EvaluationService.Consumer() {
            @Override
            public boolean isCanceled() {
                return false;
            }

            @Override
            public void consume(@Nonnull EvaluationState state) {
                queue.add(state);
                clock.passedTime(options.getEvalIntervalMillis(), TimeUnit.MILLISECONDS);
                sync.countDown();
            }

            @Override
            public void onComplete() {
            }
        }));

        sync.await();
        assertNotNull(queue.peek());
        Instant evalAt = queue.peek().getLatestEval();
        {
            var state = queue.remove();
            assertEquals(evalAt, state.getLatestEval());
            assertEquals(deadline, state.getStatus());
        }
        {
            var state = queue.remove();
            assertEquals(evalAt, state.getLatestEval());
            assertEquals(deadline, state.getStatus());
        }
        {
            var state = queue.remove();
            assertEquals(evalAt, state.getLatestEval());
            assertEquals(EvaluationStatus.OK, state.getStatus());
        }
    }

    @Test
    public void unableScheduleTaskMoreThenLimit() {
        executor.setMaxAlertRule(3);
        var tasks = IntStream.range(0, 10)
                .mapToObj(value -> task(clock.instant(), constantRule(randomAlert(), EvaluationStatus.OK)))
                .collect(Collectors.toList());

        for (var task : tasks) {
            executor.scheduleNewTask(task);
        }

        for (int index = 0; index < 3; index++) {
            assertTrue(tasks.get(index).isActive());
        }

        for (int index = 3; index < tasks.size(); index++) {
            assertFalse(tasks.get(index).isActive());
        }
    }

    @Test
    public void completeTasksWhenLimitReduced() {
        var tasks = IntStream.range(0, 10)
                .mapToObj(value -> task(clock.instant(), constantRule(randomAlert(), EvaluationStatus.OK)))
                .collect(Collectors.toList());

        for (var task : tasks) {
            executor.scheduleNewTask(task);
        }

        for (var task : tasks) {
            assertTrue(task.isActive());
        }

        executor.setMaxAlertRule(3);
        var countActive = tasks.stream().filter(Task::isActive).count();
        assertEquals(3, countActive);
    }

    private Task task(Instant time, AlertRule rule) {
        return new Task(time.toEpochMilli(), rule, null, new EvaluationService.Consumer() {
            @Override
            public boolean isCanceled() {
                return false;
            }

            @Override
            public void consume(@Nonnull EvaluationState state) {
            }

            @Override
            public void onComplete() {
            }
        });
    }

    private Instant randomEvalTime() {
        long delay = ThreadLocalRandom.current().nextLong(0, options.getEvalIntervalMillis());
        return Instant.now().plus(Duration.ofMillis(delay));
    }

    private Alert randomAlert() {
        Alert alert;
        do {
            alert = AlertTestSupport.randomAlert(ThreadLocalRandom.current(), false);
        } while (MultiAlertUnrollFactory.isSupportUnrolling(alert));
        return alert;
    }
}
