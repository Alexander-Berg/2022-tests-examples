package ru.yandex.infra.stage.deployunit;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.dto.Condition;
import ru.yandex.infra.stage.dto.DeployUnitStatus;

import static org.mockito.AdditionalAnswers.returnsElementsOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeployUnitTimelineManagerTest {
    private static final String STAGE_ID = "deploy_unit_id";
    private static final String DEPLOY_UNIT_ID = "deploy_unit_id";
    private static final String METRIC_PREFIX = "deploy_time_";

    private static final Clock DEFAULT_CLOCK = Clock.systemDefaultZone();
    private static final Instant ZERO_INSTANT = Instant.EPOCH;
    private static final Instant ONE_INSTANT = Instant.ofEpochSecond(1);
    private static final Instant TWO_INSTANT = Instant.ofEpochSecond(2);
    private static final Instant THREE_INSTANT = Instant.ofEpochSecond(3);
    private static final Instant FOUR_INSTANT = Instant.ofEpochSecond(4);
    private static final long ONE_SECOND = 1000L;
    private static final long TWO_SECONDS = 2000L;
    private static final int ZERO_REVISION = 0;
    private static final int ONE_REVISION = 1;
    private static final int TWO_REVISION = 2;

    @Test
    public void restoreFromStatusTest() {
        DeployUnitTimeline oldDeployUnitTimeline = mock(DeployUnitTimeline.class);
        DeployUnitTimeline newDeployUnitTimeline = mock(DeployUnitTimeline.class);
        DeployUnitStatus status = mock(DeployUnitStatus.class);
        when(status.getDeployUnitTimeline()).thenReturn(newDeployUnitTimeline);

        DeployUnitTimelineManager deployUnitTimelineManager = new DeployUnitTimelineManager(
                DEFAULT_CLOCK,
                STAGE_ID,
                DEPLOY_UNIT_ID,
                oldDeployUnitTimeline
        );

        deployUnitTimelineManager.restoreFromStatus(status);

        Assertions.assertEquals(newDeployUnitTimeline, deployUnitTimelineManager.getDeployUnitTimeline());
    }

    @Test
    public void noChangeDeployedTest() {
        Clock clock = getClock(ONE_INSTANT);

        DeployUnitTimeline oldDeployUnitTimeline = new DeployUnitTimeline(
                ZERO_REVISION,
                ZERO_INSTANT,
                Optional.of(ZERO_INSTANT),
                DeployUnitTimeline.Status.DEPLOYED,
                getReadyCondition(ZERO_INSTANT, true),
                ZERO_REVISION
        );

        DeployUnitTimeline expectedDeployUnitTimeline = oldDeployUnitTimeline;

        Readiness readiness = getReadiness(true);
        int currentSpecRevision = ZERO_REVISION;

        updateTest(clock, oldDeployUnitTimeline, expectedDeployUnitTimeline, currentSpecRevision, readiness);
    }

    @Test
    public void restartPodDeployedTest() {
        Clock clock = getClock(ONE_INSTANT);

        DeployUnitTimeline oldDeployUnitTimeline = new DeployUnitTimeline(
                ZERO_REVISION,
                ZERO_INSTANT,
                Optional.of(ZERO_INSTANT),
                DeployUnitTimeline.Status.DEPLOYED,
                getReadyCondition(ZERO_INSTANT, true),
                ZERO_REVISION
        );

        DeployUnitTimeline expectedDeployUnitTimeline = new DeployUnitTimeline(
                ZERO_REVISION,
                ZERO_INSTANT,
                Optional.of(ZERO_INSTANT),
                DeployUnitTimeline.Status.DEPLOYED,
                getReadyCondition(ONE_INSTANT, false),
                ZERO_REVISION
        );

        Readiness readiness = getReadiness(false);
        int currentSpecRevision = ZERO_REVISION;

        updateTest(clock, oldDeployUnitTimeline, expectedDeployUnitTimeline, currentSpecRevision, readiness);
    }

    @Test
    public void restartPodDeployingTest() {
        Clock clock = getClock(ONE_INSTANT);

        DeployUnitTimeline oldDeployUnitTimeline = new DeployUnitTimeline(
                ONE_REVISION,
                ZERO_INSTANT,
                Optional.empty(),
                DeployUnitTimeline.Status.DEPLOYING,
                getReadyCondition(ZERO_INSTANT, false),
                ZERO_REVISION
        );

        DeployUnitTimeline expectedDeployUnitTimeline = oldDeployUnitTimeline;

        Readiness readiness = getReadiness(false);
        int currentSpecRevision = ONE_REVISION;

        updateTest(clock, oldDeployUnitTimeline, expectedDeployUnitTimeline, currentSpecRevision, readiness);
    }

    @Test
    public void startDeployingTest() {
        Clock clock = getClock(ONE_INSTANT);

        DeployUnitTimeline oldDeployUnitTimeline = new DeployUnitTimeline(
                ZERO_REVISION,
                ZERO_INSTANT,
                Optional.of(ZERO_INSTANT),
                DeployUnitTimeline.Status.DEPLOYED,
                getReadyCondition(ZERO_INSTANT, true),
                ZERO_REVISION
        );

        DeployUnitTimeline expectedDeployUnitTimeline = new DeployUnitTimeline(
                ONE_REVISION,
                ONE_INSTANT,
                Optional.empty(),
                DeployUnitTimeline.Status.DEPLOYING,
                getReadyCondition(ONE_INSTANT, false),
                ZERO_REVISION
        );

        Readiness readiness = getReadiness(false);
        int currentSpecRevision = ONE_REVISION;

        updateTest(clock, oldDeployUnitTimeline, expectedDeployUnitTimeline, currentSpecRevision, readiness);
    }

    @Test
    public void finishDeployingTest() {
        Clock clock = getClock(TWO_INSTANT);

        DeployUnitTimeline oldDeployUnitTimeline = new DeployUnitTimeline(
                ONE_REVISION,
                ZERO_INSTANT,
                Optional.empty(),
                DeployUnitTimeline.Status.DEPLOYING,
                getReadyCondition(ONE_INSTANT, false),
                ZERO_REVISION
        );

        DeployUnitTimeline expectedDeployUnitTimeline = new DeployUnitTimeline(
                ONE_REVISION,
                ZERO_INSTANT,
                Optional.of(TWO_INSTANT),
                DeployUnitTimeline.Status.DEPLOYED,
                getReadyCondition(TWO_INSTANT, true),
                ONE_REVISION
        );

        Readiness readiness = getReadiness(true);
        int currentSpecRevision = ONE_REVISION;

        updateTest(clock, oldDeployUnitTimeline, expectedDeployUnitTimeline, currentSpecRevision, readiness);
    }

    @Test
    public void startDeployingBeforeFinishPrevTest() {
        Clock clock = getClock(TWO_INSTANT);

        DeployUnitTimeline oldDeployUnitTimeline = new DeployUnitTimeline(
                ONE_REVISION,
                ONE_INSTANT,
                Optional.empty(),
                DeployUnitTimeline.Status.DEPLOYING,
                getReadyCondition(ONE_INSTANT, false),
                ZERO_REVISION
        );

        DeployUnitTimeline expectedDeployUnitTimeline = new DeployUnitTimeline(
                TWO_REVISION,
                TWO_INSTANT,
                Optional.empty(),
                DeployUnitTimeline.Status.DEPLOYING,
                getReadyCondition(TWO_INSTANT, false),
                ZERO_REVISION
        );

        Readiness readiness = getReadiness(false);
        int currentSpecRevision = TWO_REVISION;

        updateTest(clock, oldDeployUnitTimeline, expectedDeployUnitTimeline, currentSpecRevision, readiness);
    }

    private void updateTest(Clock clock,
                            DeployUnitTimeline oldDeployUnitTimeline,
                            DeployUnitTimeline expectedDeployUnitTimeline,
                            int currentSpecRevision,
                            Readiness readiness) {

        DeployUnitTimelineManager deployUnitTimelineManager = new DeployUnitTimelineManager(
                clock,
                STAGE_ID,
                DEPLOY_UNIT_ID,
                oldDeployUnitTimeline
        );

        deployUnitTimelineManager.update(readiness, currentSpecRevision);

        Assertions.assertEquals(
                expectedDeployUnitTimeline,
                deployUnitTimelineManager.getDeployUnitTimeline()
        );
    }

    @Test
    public void setupMetricTest() {
        List<String> stageNameList = List.of(STAGE_ID, STAGE_ID, STAGE_ID + 1);
        Set<String> stageNameSet = Set.of(STAGE_ID, STAGE_ID + 1);
        MetricRegistry metricRegistry = mock(MetricRegistry.class);
        DeployUnitTimelineManager.setupMetric(stageNameList, metricRegistry);
        Assertions.assertEquals(DeployUnitTimelineManager.getDeployTimelineStages(), stageNameSet);
        Assertions.assertEquals(DeployUnitTimelineManager.getMetricRegistry(), metricRegistry);
    }

    @Test
    public void addMetricWithStageNotInMetricListTest() {
        MetricRegistry metricRegistry = metricTest(List.of(), getClock(ZERO_INSTANT), List.of(ONE_REVISION));
        Assertions.assertFalse(metricRegistry.getGauges().containsKey(METRIC_PREFIX + DEPLOY_UNIT_ID));
    }

    @Test
    public void addMetricWithStageInMetricListTest() {
//        ZERO_INSTANT: init, ONE_INSTANT: start deploying, TWO_INSTANT: finish deploying
        Clock clock = getClock(List.of(ZERO_INSTANT, ONE_INSTANT, TWO_INSTANT));
        MetricRegistry metricRegistry = metricTest(List.of(STAGE_ID), clock, List.of(ONE_REVISION));
        Assertions.assertTrue(metricRegistry.getGauges().containsKey(METRIC_PREFIX + DEPLOY_UNIT_ID));
        Assertions.assertEquals(metricRegistry.getGauges().get(METRIC_PREFIX + DEPLOY_UNIT_ID).getValue(), ONE_SECOND);
    }

    @Test
    public void updateMetricTest() {
//        ZERO_INSTANT: init, ONE_INSTANT: start deploying1, TWO_INSTANT: finish deploying1,
//        TWO_INSTANT: start deploying2, FOUR_INSTANT: finish deploying2
        Clock clock = getClock(List.of(ZERO_INSTANT, ONE_INSTANT, TWO_INSTANT, TWO_INSTANT, FOUR_INSTANT));
        MetricRegistry metricRegistry = metricTest(List.of(STAGE_ID), clock, List.of(ONE_REVISION, TWO_REVISION));
        Assertions.assertEquals(metricRegistry.getGauges().get(METRIC_PREFIX + DEPLOY_UNIT_ID).getValue(), TWO_SECONDS);
    }

    private static MetricRegistry metricTest(List<String> stageMetricList, Clock clock, List<Integer> revisionsToDeploy) {
        MetricRegistry metricRegistry = new MetricRegistry();
        DeployUnitTimelineManager.setupMetric(stageMetricList, metricRegistry);
        DeployUnitTimelineManager deployUnitTimelineManager = new DeployUnitTimelineManager(clock, STAGE_ID, DEPLOY_UNIT_ID);
        revisionsToDeploy.forEach(revision -> deploy(deployUnitTimelineManager, revision));
        return metricRegistry;
    }

    private static void deploy(DeployUnitTimelineManager deployUnitTimelineManager, int revision) {
        deployUnitTimelineManager.update(getReadiness(false), revision);
        deployUnitTimelineManager.update(getReadiness(true), revision);
    }

    private static Clock getClock(Instant instant) {
        return getClock(List.of(instant));
    }

    private static Clock getClock(List<Instant> instants) {
        Clock clock = mock(Clock.class);
        doAnswer(returnsElementsOf(instants)).when(clock).instant();
        return clock;
    }

    private static Readiness getReadiness(boolean isReady) {
        Readiness readiness = mock(Readiness.class);
        when(readiness.isReady()).thenReturn(isReady);
        when(readiness.getReadyCondition(any()))
                .thenAnswer(timestampArg -> getReadyCondition(timestampArg.getArgument(0), isReady));
        return readiness;
    }

    private static Condition getReadyCondition(Instant timestamp, boolean isReady) {
        return new Condition(isReady ? Condition.Status.TRUE : Condition.Status.FALSE, "", "", timestamp);
    }
}
