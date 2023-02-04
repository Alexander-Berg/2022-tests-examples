package ru.yandex.solomon.alert.evaluation;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.consume.AlertRuleResultConsumerStub;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertKey;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.SubAlert;
import ru.yandex.solomon.alert.domain.threshold.Compare;
import ru.yandex.solomon.alert.domain.threshold.PredicateRule;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.executor.local.LocalAlertExecutorOptions;
import ru.yandex.solomon.alert.rule.AlertRule;
import ru.yandex.solomon.alert.rule.AlertRuleFactoryStub;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.alert.unroll.MultiAlertUnrollFactory;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.alert.rule.stubs.AlertRuleStubs.constantRule;
import static ru.yandex.solomon.alert.rule.stubs.AlertRuleStubs.countDownWhenReadyRule;
import static ru.yandex.solomon.alert.rule.stubs.AlertRuleStubs.countingRule;
import static ru.yandex.solomon.alert.rule.stubs.AlertRuleStubs.neverCompletedRule;
import static ru.yandex.solomon.alert.rule.stubs.AlertRuleStubs.randomDelayRule;

/**
 * @author Vladimir Gordiychuk
 */
public class EvaluationServiceImplTest {
    @Rule
    public Timeout globalTimeout = Timeout.builder()
            .withLookingForStuckThread(true)
            .withTimeout(30, TimeUnit.SECONDS)
            .build();

    private ManualClock clock;
    private ScheduledExecutorService executor;
    private TaskExecutorDispatcher taskExecutor;
    private LocalAlertExecutorOptions options;
    private LocalAlertExecutorOptions yasmOptions;
    private AlertRuleFactoryStub ruleFactory;
    private AlertRuleResultConsumerStub consumer;
    private EvaluationService evaluationService;

    @Before
    public void setUp() throws Exception {
        clock = new ManualClock();
        executor = new ManualScheduledExecutorService(2, clock);
        ruleFactory = new AlertRuleFactoryStub(executor);
        options = LocalAlertExecutorOptions.newBuilder()
                .setAlertRuleTimeout(30L, TimeUnit.SECONDS)
                .setEvalInterval(50L, TimeUnit.MILLISECONDS)
                .setMaxConcurrentAlertRules(2)
                .setMaxConcurrentWarmupAlertRules(2)
                .setMaxEvaluationJitter(5, TimeUnit.MILLISECONDS)
                .build();

        yasmOptions = options.toBuilder()
                .setMaxConcurrentAlertRules(100)
                .setMaxConcurrentWarmupAlertRules(100)
                .build();

        consumer = new AlertRuleResultConsumerStub();
        AlertExecutorOptionsProvider optionsProvider = task -> switch (task) {
            case DEFAULT -> options;
            case DATAPROXY -> yasmOptions;
        };
        taskExecutor = new TaskExecutorDispatcher(clock, executor, executor, optionsProvider, MetricRegistry.root());
        evaluationService = new EvaluationServiceImpl(
                Clock.systemDefaultZone(),
                ruleFactory,
                taskExecutor);
    }

    @After
    public void tearDown() throws Exception {
        taskExecutor.close();
        executor.shutdownNow();
    }

    @Test
    public void countActiveRuleLimited() throws Exception {
        AtomicInteger countActive = new AtomicInteger(0);
        for (int index = 0; index < options.getMaxConcurrentAlertRules() * 3; index++) {
            Alert alert = randomAlert();
            ruleFactory.setAlertRule(countingRule(neverCompletedRule(alert), countActive));
            assign(alert);
        }

        TimeUnit.MILLISECONDS.sleep(options.getEvalIntervalMillis() * 3);
        assertThat(countActive.get(), Matchers.lessThanOrEqualTo(options.getMaxConcurrentAlertRules()));
    }

    @Test
    public void consumerEvalResult() throws Exception {
        Alert alert = randomAlert();

        CountDownLatch sync = new CountDownLatch(2);
        ruleFactory.setAlertRule(countDownWhenReadyRule(constantRule(alert, EvaluationStatus.ALARM), sync));
        assign(alert);
        await(sync);

        EvaluationStatus latestStatus = consumer.getLatestStatus(alert.getId());
        assertThat(latestStatus, equalTo(EvaluationStatus.ALARM));
    }

    @Test
    public void consumeMultipleEvalResults() throws Exception {
        Alert alert = randomAlert();

        EvaluationStatus[] expectStatuses = new EvaluationStatus[]{
                EvaluationStatus.ALARM,
                EvaluationStatus.ALARM,
                EvaluationStatus.OK,
                EvaluationStatus.ERROR
        };

        CountDownLatch sync = new CountDownLatch(expectStatuses.length + 1);
        AlertRule rule = countDownWhenReadyRule(
                randomDelayRule(
                        constantRule(alert, expectStatuses),
                    executor, options.getEvalIntervalMillis()),
                sync
        );
        ruleFactory.setAlertRule(rule);
        assign(alert);
        await(sync);

        List<EvaluationStatus> result = consumer.getLatestResults(alert.getId())
                .stream()
                .map(EvaluationState::getStatus)
                .limit(expectStatuses.length)
                .collect(Collectors.toList());

        assertThat(result, equalTo(Arrays.asList(expectStatuses)));
    }

    @Test
    public void alertCanBeChanged() throws Exception {
        ThresholdAlert original = randomThresholdAlert();

        CountDownLatch v1Sync = new CountDownLatch(2);
        ruleFactory.setAlertRule(countDownWhenReadyRule(constantRule(original, EvaluationStatus.ALARM), v1Sync));
        assign(original);
        await(v1Sync);

        CountDownLatch v2Sync = new CountDownLatch(2);
        ruleFactory.setAlertRule(countDownWhenReadyRule(constantRule(original, EvaluationStatus.OK), v2Sync));
        assign(original.toBuilder()
                .setPredicateRule(PredicateRule.onThreshold(100500))
                .setVersion(1001)
                .build());
        await(v2Sync);

        EvaluationStatus latestStatus = consumer.getLatestStatus(original.getId());
        assertThat(latestStatus, equalTo(EvaluationStatus.OK));
    }

    @Test
    public void evalStepAlwaysTheSameForTheSameAlert() throws Exception {
        for (int index = 0; index < options.getMaxConcurrentAlertRules() * 2; index++) {
            Alert noise = randomAlert();
            assign(noise);
        }

        Alert alert = randomAlert();
        CountDownLatch sync = new CountDownLatch(10);
        AlertRule rule = countDownWhenReadyRule(
                randomDelayRule(
                        constantRule(alert, EvaluationStatus.OK),
                    executor, options.getEvalIntervalMillis() * 2),
                sync
        );
        ruleFactory.setAlertRule(rule);
        assign(alert);
        await(sync);

        Instant prev = null;
        for (EvaluationState result : consumer.getLatestResults(alert.getId())) {
            if (prev == null) {
                prev = result.getLatestEval();
                continue;
            }

            long interval = result.getLatestEval().toEpochMilli() - prev.toEpochMilli();
            prev = result.getLatestEval();
            assertThat("Interval evaluate alert rule should be always the same, " +
                            "because skip some time will affect alert rule that base on period and as " +
                            "a result important notification will not be delivery to use - " +
                            "for example nightly failure",

                    interval, equalTo(options.getEvalIntervalMillis())
            );
        }
    }

    /**
     * This test can be flapping, because reproduced not always, but flapping means that the same task
     * executes more then one times
     */
    @Test
    public void assignUnassignAssignNotLeadToDuplicateCalculation() throws Exception {
        for (int index = 0; index < options.getMaxConcurrentAlertRules() * 2; index++) {
            Alert noise = randomAlert();
            assign(noise);
        }

        Alert alert = randomAlert();

        // Assign
        ruleFactory.setAlertRule(randomDelayRule(
                constantRule(alert, EvaluationStatus.OK),
            executor, options.getEvalIntervalMillis() * 2));
        assign(alert);

        // Unassign
        TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(1, options.getEvalIntervalMillis()));
        unassign(alert.getKey());

        // Assign
        TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(1, options.getEvalIntervalMillis()));
        CountDownLatch sync = new CountDownLatch(10);
        AlertRule rule = countDownWhenReadyRule(
                randomDelayRule(
                        constantRule(alert, EvaluationStatus.OK),
                    executor, options.getEvalIntervalMillis() * 2),
                sync
        );
        ruleFactory.setAlertRule(rule);
        assign(alert);
        await(sync);

        List<EvaluationState> results = consumer.getLatestResults(alert.getId());
        Instant prev = null;
        // Check only latest 5 results because at least one time task can be evaluated by previous rule with previous time
        for (EvaluationState result : results.subList(results.size() - 5, results.size())) {
            if (prev == null) {
                prev = result.getLatestEval();
                continue;
            }

            long interval = result.getLatestEval().toEpochMilli() - prev.toEpochMilli();
            prev = result.getLatestEval();
            assertThat("Interval evaluate alert rule should be always the same, " +
                            "because skip some time will affect alert rule that base on period and as " +
                            "a result important notification will not be delivery to use - " +
                            "for example nightly failure",

                    interval, equalTo(options.getEvalIntervalMillis())
            );
        }
    }

    @Test
    public void evalMultiAlert() throws Exception {
        ThresholdAlert parent = randomThresholdAlert()
                .toBuilder()
                .setGroupByLabel("host")
                .build();

        SubAlert childOne = SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("host", "solomon-1"))
                .build();

        CountDownLatch childOneSync = new CountDownLatch(2);
        ruleFactory.setAlertRule(countDownWhenReadyRule(constantRule(childOne, EvaluationStatus.ALARM), childOneSync));
        assign(childOne);

        SubAlert childTwo = SubAlert.newBuilder()
                .setParent(parent)
                .setGroupKey(Labels.of("host", "solomon-2"))
                .build();

        CountDownLatch childTwoSync = new CountDownLatch(2);
        ruleFactory.setAlertRule(countDownWhenReadyRule(constantRule(childTwo, EvaluationStatus.OK), childTwoSync));
        assign(childTwo);

        await(childOneSync);
        await(childTwoSync);

        assertThat(consumer.getLatestStatus(childOne.getId()), equalTo(EvaluationStatus.ALARM));
        assertThat(consumer.getLatestStatus(childTwo.getId()), equalTo(EvaluationStatus.OK));
    }

    @Test
    public void restartFromLatestEval() throws Exception {
        Alert alert = randomAlert();
        CountDownLatch sync = new CountDownLatch(2);
        ruleFactory.setAlertRule(
                countDownWhenReadyRule(
                        randomDelayRule(
                                constantRule(alert, EvaluationStatus.ALARM),
                            executor, options.getEvalIntervalMillis()),
                        sync
                )
        );
        assign(alert);
        await(sync);
        unassign(alert.getKey());

        CountDownLatch restartSync = new CountDownLatch(2);
        ruleFactory.setAlertRule(
                countDownWhenReadyRule(
                        randomDelayRule(
                                constantRule(alert, EvaluationStatus.OK),
                            executor, options.getEvalIntervalMillis()),
                        restartSync
                )
        );
        assign(alert);
        await(restartSync);

        Instant prev = null;
        for (EvaluationState result : consumer.getLatestResults(alert.getId())) {
            if (prev == null) {
                prev = result.getLatestEval();
                continue;
            }

            long interval = result.getLatestEval().toEpochMilli() - prev.toEpochMilli();
            prev = result.getLatestEval();
            assertThat("Evaluation interval for alert should be the same, after assign alert, " +
                            "previous eval time should be restore",
                    interval, anyOf(equalTo(options.getEvalIntervalMillis()), equalTo(0L))
            );
        }
    }

    @Test
    public void forgotPreviousEvalStateIfAlertChanged() throws Exception {
        ThresholdAlert alert = randomThresholdAlert();
        CountDownLatch sync = new CountDownLatch(2);
        ruleFactory.setAlertRule(
                countDownWhenReadyRule(
                        randomDelayRule(
                                constantRule(alert, EvaluationStatus.ALARM),
                            executor, options.getEvalIntervalMillis()),
                        sync
                )
        );
        assign(alert);
        await(sync);
        unassign(alert.getKey());

        ThresholdAlert updatedAlert = alert.toBuilder()
                .setPredicateRule(PredicateRule.onThreshold(100500).withComparison(Compare.GTE))
                .setPeriod(alert.getPeriod().plusMinutes(1))
                .setVersion(alert.getVersion() + 1)
                .build();

        CountDownLatch restartSync = new CountDownLatch(3);
        ruleFactory.setAlertRule(
                countDownWhenReadyRule(
                        randomDelayRule(
                                constantRule(updatedAlert, EvaluationStatus.ALARM),
                            executor, options.getEvalIntervalMillis()),
                        restartSync
                )
        );
        assign(updatedAlert);
        await(restartSync);

        List<EvaluationState> results = consumer.getLatestResults(updatedAlert.getId());
        EvaluationState first = results.get(0);
        EvaluationState last = results.get(results.size() - 1);

        assertThat(last.getStatus(), equalTo(first.getStatus()));
        assertThat(first.getAlertVersion(), equalTo(alert.getVersion()));
        assertThat(last.getAlertVersion(), equalTo(updatedAlert.getVersion()));
        assertThat(last.getSince(), not(equalTo(first.getSince())));
        assertThat(last.getSince(), not(equalTo(last.getLatestEval())));
    }

    private void assign(Alert alert) {
        EvaluationState prevStatus = consumer.getLatestState(alert.getId());
        evaluationService.assign(alert, prevStatus, consumer);
    }

    private void await(CountDownLatch sync) throws InterruptedException {
        while (!sync.await(10, TimeUnit.MILLISECONDS)) {
            changeTime(20, TimeUnit.MILLISECONDS);
        }
    }

    private void changeTime(long value, TimeUnit unit) {
        clock.passedTime(value, unit);
        taskExecutor.scheduleAct();
    }

    private void unassign(AlertKey key) {
        evaluationService.unassign(key);
    }

    private Alert randomAlert() {
        Alert alert;
        do {
            alert = AlertTestSupport.randomAlert(ThreadLocalRandom.current(), false);
        } while (MultiAlertUnrollFactory.isSupportUnrolling(alert));
        return alert;
    }

    private ThresholdAlert randomThresholdAlert() {
        return AlertTestSupport.randomThresholdAlert(ThreadLocalRandom.current())
                .toBuilder()
                .setGroupByLabels(Collections.emptyList())
                .build();
    }
}
