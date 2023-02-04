package ru.yandex.solomon.alert.cluster.broker.alert.activity;

import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.monlib.metrics.encode.text.MetricTextEncoder;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.cluster.broker.evaluation.EvaluationAssignmentServiceStub;
import ru.yandex.solomon.alert.cluster.broker.mute.MuteMatcherStub;
import ru.yandex.solomon.alert.cluster.broker.notification.StatefulNotificationChannelFactoryStub;
import ru.yandex.solomon.alert.cluster.project.ProjectAssignment;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertState;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.SubAlert;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.notification.NotificationState;
import ru.yandex.solomon.alert.notification.channel.NotificationChannelStub;
import ru.yandex.solomon.alert.notification.domain.NotificationTestSupport;
import ru.yandex.solomon.alert.protobuf.TPersistAlertState;
import ru.yandex.solomon.alert.protobuf.TPersistEvaluationState;
import ru.yandex.solomon.alert.protobuf.TPersistMultiAlertState;
import ru.yandex.solomon.alert.protobuf.TPersistSubAlertState;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.alert.unroll.UnrollExecutorStub;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.model.protobuf.Label;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.alert.util.CommonMatchers.reflectionEqualTo;

/**
 * @author Vladimir Gordiychuk
 */
public class MultiAlertActivityTest {
    private ManualClock clock;
    private ScheduledExecutorService executor;
    private UnrollExecutorStub unrollStub;
    private EvaluationAssignmentServiceStub evaluationStub;
    private StatefulNotificationChannelFactoryStub notificationFactoryStub;
    private MuteMatcherStub muteMatcherStub;
    private SimpleActivitiesFactory simpleActivitiesFactory;
    @Rule
    public Timeout timeoutRule = Timeout.builder()
            .withLookingForStuckThread(true)
            .withTimeout(15, TimeUnit.SECONDS)
            .build();

    @Before
    public void setUp() throws Exception {
        clock = new ManualClock();
        executor = new ManualScheduledExecutorService(2, clock);
        unrollStub = new UnrollExecutorStub(executor);
        evaluationStub = new EvaluationAssignmentServiceStub(clock, executor);
        String projectId = "junk";
        notificationFactoryStub = new StatefulNotificationChannelFactoryStub(executor, projectId);
        muteMatcherStub = new MuteMatcherStub();
        simpleActivitiesFactory = new SimpleActivitiesFactory(new ProjectAssignment(projectId, "localhost", AssignmentSeqNo.EMPTY),
                unrollStub,
                evaluationStub,
                notificationFactoryStub,
                muteMatcherStub);
    }

    @After
    public void tearDown() throws Exception {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void subAlertWithoutChild() throws InterruptedException {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();

        MultiAlertActivity activity = new MultiAlertActivity(parent, simpleActivitiesFactory);
        unrollStub.predefineUnroll(parent.getId(), Collections::emptySet);
        activity.run();
        var sync = unrollStub.getSyncEval(parent.getId(), parent.getVersion());
        clock.passedTime(1, TimeUnit.MINUTES);
        sync.acquire();

        assertThat(activity.getSubActivities(), emptyIterable());
    }

    @Test
    public void newSubAlert() throws InterruptedException {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();

        unrollStub.predefineUnroll(parent.getId(), () -> Collections.singleton(Labels.of("host", "woodpecker")));
        MultiAlertActivity activity = makeAndRun(parent);
        awaitUnrolling(activity);

        assertThat(activity.getSubActivities(), iterableWithSize(1));
        assertThat(Iterables.getOnlyElement(activity.getSubActivities()).getAlert().getGroupKey(), equalTo(Labels.of("host", "woodpecker")));
    }

    @Test
    public void subAlertCanceledWhenCanceledParent() throws InterruptedException {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();

        unrollStub.predefineUnroll(parent.getId(), () -> Collections.singleton(Labels.of("host", "woodpecker")));
        MultiAlertActivity activity = makeAndRun(parent);
        awaitUnrolling(activity);

        assertThat(Iterables.getOnlyElement(activity.getSubActivities()).isCanceled(), equalTo(false));
        activity.cancel();
        assertThat(activity.isCanceled(), equalTo(true));
        assertThat(Iterables.getOnlyElement(activity.getSubActivities()).isCanceled(), equalTo(true));
    }

    @Test
    public void deleteSubAlert() throws InterruptedException {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();

        unrollStub.predefineUnroll(parent.getId(), () -> Collections.singleton(Labels.of("host", "woodpecker")));
        MultiAlertActivity activity = makeAndRun(parent);
        awaitUnrolling(activity);

        SubAlertActivity sub = Iterables.getOnlyElement(activity.getSubActivities());
        unrollStub.predefineUnroll(parent.getId(), Collections::emptySet);
        awaitUnrolling(activity);

        assertThat(sub.isCanceled(), equalTo(true));
        assertThat(activity.isCanceled(), equalTo(false));
        assertThat(activity.getSubActivities(), emptyIterable());
    }

    @Test
    public void constIdForSubAlert() throws InterruptedException {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();

        unrollStub.predefineUnroll(parent.getId(), () -> Collections.singleton(Labels.of("host", "woodpecker")));
        MultiAlertActivity activity = makeAndRun(parent);
        awaitUnrolling(activity);

        SubAlert childV1 = Iterables.getOnlyElement(activity.getSubActivities()).getAlert();
        activity.cancel();
        MultiAlertActivity activityV2 = makeAndRun(parent.toBuilder()
                .setVersion(42)
                .build());
        awaitUnrolling(activityV2);

        SubAlert childV2 = Iterables.getOnlyElement(activityV2.getSubActivities()).getAlert();
        assertThat(childV2.getId(), equalTo(childV1.getId()));
    }

    @Test
    public void dumpRestore() throws InterruptedException {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();

        unrollStub.predefineUnroll(parent.getId(), () -> ImmutableSet.of(Labels.of("host", "solomon-test-1")));
        MultiAlertActivity activity = makeAndRun(parent);
        awaitUnrolling(activity);

        SubAlert child = Iterables.getOnlyElement(activity.getSubActivities()).getAlert();
        TPersistAlertState dump = activity.dumpState();
        activity.cancel();

        MultiAlertActivity restoreActivity = restore(parent, dump);
        SubAlert restoreChild = Iterables.getOnlyElement(restoreActivity.getSubActivities()).getAlert();
        assertThat(restoreChild.getId(), equalTo(child.getId()));
        assertThat(restoreChild.getGroupKey(), equalTo(child.getGroupKey()));
    }


    @Test
    public void restoreFromFailedState() throws InterruptedException {
        ThresholdAlert parentV1 = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .setVersion(1)
                .build();

        unrollStub.predefineUnroll(parentV1.getId(), () -> ImmutableSet.of(Labels.of("host", "solomon-test-1")));
        MultiAlertActivity activity = makeAndRun(parentV1);
        awaitUnrolling(activity);
        activity.cancel();

        var failedActivity = new FailedAlertActivity(parentV1, new IllegalArgumentException(), Map.of());
        TPersistAlertState dump = failedActivity.dumpState();

        unrollStub.predefineUnroll(parentV1.getId(), Collections::emptySet);
        ThresholdAlert parentV2 = parentV1.toBuilder()
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=freeSpace, host=*")
                .setVersion(2)
                .build();
        MultiAlertActivity restoreActivity = restore(parentV2, dump);
        assertThat(restoreActivity.getSubActivities(), emptyIterable());
    }

    @Test
    public void skipRestoreFromObsoleteState() throws InterruptedException {
        ThresholdAlert parentV1 = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .setVersion(1)
                .build();

        unrollStub.predefineUnroll(parentV1.getId(), () -> ImmutableSet.of(Labels.of("host", "solomon-test-1")));
        MultiAlertActivity activity = makeAndRun(parentV1);
        awaitUnrolling(activity);

        TPersistAlertState dump = activity.dumpState();
        activity.cancel();

        unrollStub.predefineUnroll(parentV1.getId(), Collections::emptySet);
        ThresholdAlert parentV2 = parentV1.toBuilder()
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=freeSpace, host=*")
                .setVersion(2)
                .build();
        MultiAlertActivity restoreActivity = restore(parentV2, dump);
        assertThat(restoreActivity.getSubActivities(), emptyIterable());
    }

    @Test
    public void newSubAlertsAssignToEvaluate() throws InterruptedException {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();

        unrollStub.predefineUnroll(parent.getId(), () -> Collections.singleton(Labels.of("host", "woodpecker")));
        MultiAlertActivity activity = makeAndRun(parent);
        awaitUnrolling(activity);

        SubAlertActivity child = Iterables.getOnlyElement(activity.getSubActivities());
        evaluationStub.predefineStatus(child.getAlert().getId(), () -> EvaluationStatus.ALARM);
        awaitEvaluation(child);

        EvaluationState eval = child.getLatestEvaluation();
        assertThat(eval.getStatus(), equalTo(EvaluationStatus.ALARM));
    }

    @Test
    public void restoreEvaluation() throws InterruptedException {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();

        unrollStub.predefineUnroll(parent.getId(), () -> ImmutableSet.of(Labels.of("host", "solomon-test-1")));
        MultiAlertActivity activity = makeAndRun(parent);
        awaitUnrolling(activity);

        SubAlertActivity childActivity = Iterables.getOnlyElement(activity.getSubActivities());
        evaluationStub.predefineStatus(childActivity.getAlert().getId(), () -> EvaluationStatus.ALARM);
        awaitEvaluation(childActivity);

        var childEvalState = childActivity.getProcessingState();
        TPersistAlertState dump = activity.dumpState();
        activity.cancel();

        MultiAlertActivity restoreActivity = restore(parent, dump);
        SubAlertActivity restoreChildActivity = Iterables.getOnlyElement(restoreActivity.getSubActivities());
        var restoredChildEvalState = restoreChildActivity.getProcessingState();
        assertThat(restoredChildEvalState.evaluationState(), reflectionEqualTo(childEvalState.evaluationState()));
        assertThat(restoredChildEvalState.alertMuteStatus(), reflectionEqualTo(childEvalState.alertMuteStatus()));
    }

    @Test
    public void skipRestoreDefaultEvaluation() throws InterruptedException {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();

        MultiAlertActivity restoreActivity = restore(parent, TPersistAlertState.newBuilder()
                .setId(parent.getId())
                .setVersion(parent.getVersion())
                .setMultiAlertState(TPersistMultiAlertState.newBuilder()
                        .addSubAlerts(TPersistSubAlertState.newBuilder()
                                .addLabels(Label.newBuilder()
                                        .setKey("host")
                                        .setValue("solomon")
                                        .build())
                                .setEvaluation(TPersistEvaluationState.newBuilder()
                                        .build())
                                .build())
                        .build())
                .build());

        SubAlertActivity subActivity = Iterables.getOnlyElement(restoreActivity.getSubActivities());
        assertThat(subActivity.getLatestEvaluation(), nullValue());
        assertThat(subActivity.getProcessingState(), nullValue());
    }

    @Test
    public void notifySubAlertStatus() throws InterruptedException {
        NotificationChannelStub channel = new NotificationChannelStub(NotificationTestSupport.randomNotification()
                .toBuilder()
                .setProjectId("junk")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
                .setRepeatNotifyDelay(Duration.ofMillis(100))
                .build());
        notificationFactoryStub.setNotificationChannel(channel);

        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setProjectId("junk")
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .setNotificationChannel(channel.getId())
                .build();

        unrollStub.predefineUnroll(parent.getId(), () -> ImmutableSet.of(Labels.of("host", "test")));
        MultiAlertActivity parentActivity = makeAndRun(parent);
        awaitUnrolling(parentActivity);

        SubAlertActivity child = Iterables.getOnlyElement(parentActivity.getSubActivities());
        evaluationStub.predefineStatus(child.getAlert().getId(), () -> EvaluationStatus.ALARM);
        awaitNotification(channel, child);

        NotificationState notifyOne = getNotificationState(child);
        evaluationStub.predefineStatus(child.getAlert().getId(), () -> EvaluationStatus.OK);
        awaitNotification(channel, child);
        NotificationState notifyTwo = getNotificationState(child);

        assertThat(notifyTwo.getLatestEval().toEpochMilli(), greaterThan(notifyOne.getLatestEval().toEpochMilli()));
        assertThat(notifyTwo.getLatestSuccessNotify().toEpochMilli(), greaterThan(notifyOne.getLatestSuccessNotify().toEpochMilli()));
    }

    @Test
    public void dumpAndRestoreNotificationState() throws InterruptedException {
        NotificationChannelStub channel = new NotificationChannelStub(NotificationTestSupport.randomNotification()
                .toBuilder()
                .setProjectId("junk")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .setRepeatNotifyDelay(Duration.ofSeconds(30))
                .build());
        notificationFactoryStub.setNotificationChannel(channel);

        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setProjectId("junk")
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .setNotificationChannel(channel.getId())
                .build();

        unrollStub.predefineUnroll(parent.getId(), () -> ImmutableSet.of(Labels.of("host", "test")));
        MultiAlertActivity parentActivity = makeAndRun(parent);
        awaitUnrolling(parentActivity);

        SubAlertActivity child = Iterables.getOnlyElement(parentActivity.getSubActivities());
        evaluationStub.predefineStatus(child.getAlert().getId(), () -> EvaluationStatus.ALARM);
        awaitNotification(channel, child);
        NotificationState notifyState = getNotificationState(child);

        TPersistAlertState dump = parentActivity.dumpState();
        parentActivity.cancel();

        MultiAlertActivity parentRestoreActivity = restore(parent, dump);
        SubAlertActivity childRestore = Iterables.getOnlyElement(parentRestoreActivity.getSubActivities());
        NotificationState notificationStateRestore = getNotificationState(childRestore);

        assertThat(notificationStateRestore, reflectionEqualTo(notifyState));
    }

    @Test
    public void skipMetricReportWhenAbsentSubAlerts() {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setProjectId("junk")
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();

        MultiAlertActivity activity = new MultiAlertActivity(parent, simpleActivitiesFactory);
        String metric = encodeMetrics(activity);
        assertThat(metric, Matchers.isEmptyString());
    }

    @Test
    public void reportLatestEvaluationTime() throws InterruptedException {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setProjectId("junk")
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();

        unrollStub.predefineUnroll(parent.getId(), () -> ImmutableSet.of(Labels.of("host", "test")));
        MultiAlertActivity parentActivity = makeAndRun(parent);
        awaitUnrolling(parentActivity);

        SubAlertActivity subActivity = Iterables.getOnlyElement(parentActivity.getSubActivities());
        evaluationStub.predefineStatus(subActivity.getAlert().getId(), () -> EvaluationStatus.ALARM);
        awaitEvaluation(subActivity);


        String v1 = getEncodedStatus(subActivity);
        assertThat(encodeMetrics(parentActivity), containsString(v1));

        awaitEvaluation(subActivity);

        String v2 = getEncodedStatus(subActivity);
        assertThat(encodeMetrics(parentActivity), containsString(v2));
    }

    @Test
    public void reportLatestEvaluationStatus() throws InterruptedException {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setProjectId("junk")
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();

        unrollStub.predefineUnroll(parent.getId(), () -> ImmutableSet.of(Labels.of("host", "test")));
        MultiAlertActivity parentActivity = makeAndRun(parent);
        awaitUnrolling(parentActivity);

        SubAlertActivity subActivity = Iterables.getOnlyElement(parentActivity.getSubActivities());
        for (EvaluationStatus.Code code : EvaluationStatus.Code.values()) {
            evaluationStub.predefineStatus(subActivity.getAlert().getId(), code::toStatus);
            awaitEvaluation(subActivity);

            String expect = getEncodedStatus(subActivity);
            assertThat(encodeMetrics(parentActivity), containsString(expect));
        }
    }

    @Test
    public void reportAllSubAlerts() throws InterruptedException {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setProjectId("junk")
                .setState(AlertState.ACTIVE)
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();

        unrollStub.predefineUnroll(parent.getId(), () -> ImmutableSet.of(
                Labels.of("host", "test"),
                Labels.of("host", "dev")
        ));

        MultiAlertActivity parentActivity = makeAndRun(parent);
        awaitUnrolling(parentActivity);
        for (SubAlertActivity activity : parentActivity.getSubActivities()) {
            evaluationStub.predefineStatus(activity.getAlert().getId(), () -> EvaluationStatus.ALARM);
            awaitEvaluation(activity);

            String expected = getEncodedStatus(activity);
            assertThat(encodeMetrics(parentActivity), containsString(expected));
        }
    }

    @Test
    public void reportMultiAlertSummary() throws InterruptedException {
        ThresholdAlert parent = AlertTestSupport.randomThresholdAlert()
                .toBuilder()
                .setProjectId("junk")
                .setState(AlertState.ACTIVE)
                .setId("myId")
                .setSelectors("project=solomon, cluster=foo, service=misc, sensor=idleTime, host=*")
                .setGroupByLabels(ImmutableList.of("host"))
                .build();

        unrollStub.predefineUnroll(parent.getId(), () -> ImmutableSet.of(
                Labels.of("host", "test"),
                Labels.of("host", "dev"),
                Labels.of("host", "prod")
        ));

        MultiAlertActivity parentActivity = makeAndRun(parent);
        awaitUnrolling(parentActivity);

        ImmutableList<SubAlertActivity> child = ImmutableList.copyOf(parentActivity.getSubActivities());
        evaluationStub.predefineStatus(child.get(0).getAlert().getId(), () -> EvaluationStatus.ALARM);
        evaluationStub.predefineStatus(child.get(1).getAlert().getId(), () -> EvaluationStatus.ALARM);
        evaluationStub.predefineStatus(child.get(2).getAlert().getId(), () -> EvaluationStatus.OK);

        awaitEvaluation(child.get(0));
        awaitEvaluation(child.get(1));
        awaitEvaluation(child.get(2));

        String metrics = encodeMetrics(parentActivity);
        assertThat(metrics, containsString("IGAUGE multiAlert.evaluation.status{alertId='myId', projectId='junk', status='NO_DATA'} [0]"));
        assertThat(metrics, containsString("IGAUGE multiAlert.evaluation.status{alertId='myId', projectId='junk', status='OK'} [1]"));
        assertThat(metrics, containsString("IGAUGE multiAlert.evaluation.status{alertId='myId', projectId='junk', status='ALARM'} [2]"));
        assertThat(metrics, containsString("IGAUGE multiAlert.evaluation.status{alertId='myId', projectId='junk', status='ERROR'} [0]"));
    }

    private NotificationState getNotificationState(SubAlertActivity activity) {
        return Iterables.getOnlyElement(activity.getNotificationStates(Collections.emptySet()).collect(Collectors.toList()));
    }

    private String getEncodedStatus(SubAlertActivity activity) {
        EvaluationState state = requireNonNull(activity.getLatestEvaluation());

        return String.format("IGAUGE alert.evaluation.status{alertId='%s', parentId='%s', projectId='%s'} [%s]",
                state.getAlertId(),
                activity.getAlert().getParent().getId(),
                state.getProjectId(),
                state.getStatus().getCode().getNumber());
    }

    private String encodeMetrics(MultiAlertActivity activity) {
        StringWriter writer = new StringWriter();
        try (MetricTextEncoder e = new MetricTextEncoder(writer, true)) {
            e.onStreamBegin(-1);
            activity.appendAlertMetrics(e);
            e.onStreamEnd();
        }
        System.out.println(writer.toString());
        return writer.toString();
    }

    private MultiAlertActivity makeAndRun(Alert parent) {
        MultiAlertActivity result = new MultiAlertActivity(parent, simpleActivitiesFactory);
        result.run();
        return result;
    }

    private MultiAlertActivity restore(Alert parent, TPersistAlertState dump) {
        MultiAlertActivity result = new MultiAlertActivity(parent, simpleActivitiesFactory);
        result.restore(dump);
        return result;
    }

    private void awaitUnrolling(MultiAlertActivity activity) throws InterruptedException {
        var sync = unrollStub.getSyncEval(activity.getAlert().getId(), activity.getAlert().getVersion());
        clock.passedTime(1, TimeUnit.MINUTES);
        sync.acquire();
    }

    private void awaitEvaluation(SubAlertActivity activity) throws InterruptedException {
        Semaphore sync = evaluationStub.getSyncEval(activity.getAlert().getId());
        clock.passedTime(1, TimeUnit.MINUTES);
        sync.acquire();
    }

    private void awaitNotification(NotificationChannelStub channel, SubAlertActivity activity) throws InterruptedException {
        NotificationState prev = getNotificationState(activity);
        Semaphore sync = channel.getSendSync();
        clock.passedTime(1, TimeUnit.MINUTES);
        sync.acquire();

        NotificationState current;
        do {
            current = getNotificationState(activity);
            if (prev.getLatestEval().isBefore(current.getLatestEval())) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(0, 10));
        } while (true);
    }
}
