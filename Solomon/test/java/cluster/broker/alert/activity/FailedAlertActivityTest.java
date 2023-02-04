package ru.yandex.solomon.alert.cluster.broker.alert.activity;

import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.monlib.metrics.encode.text.MetricTextEncoder;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.cluster.broker.evaluation.EvaluationAssignmentServiceStub;
import ru.yandex.solomon.alert.cluster.broker.mute.MuteMatcherStub;
import ru.yandex.solomon.alert.cluster.broker.notification.StatefulNotificationChannelFactoryStub;
import ru.yandex.solomon.alert.cluster.project.ProjectAssignment;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertState;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.notification.channel.NotificationChannelStub;
import ru.yandex.solomon.alert.notification.domain.Notification;
import ru.yandex.solomon.alert.notification.domain.NotificationTestSupport;
import ru.yandex.solomon.alert.protobuf.TPersistAlertState;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.alert.unroll.UnrollExecutorStub;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Alexey Trushkin
 */
public class FailedAlertActivityTest {
    private ScheduledExecutorService executor;
    private EvaluationAssignmentServiceStub evaluationStub;
    private StatefulNotificationChannelFactoryStub notificationFactoryStub;
    private ActivityFactory factory;
    @Rule
    public Timeout timeoutRule = Timeout.builder()
            .withLookingForStuckThread(true)
            .withTimeout(15, TimeUnit.SECONDS)
            .build();
    private ManualClock clock;

    @Before
    public void setUp() throws Exception {
        clock = new ManualClock();
        executor = new ManualScheduledExecutorService(2, clock);
        evaluationStub = new EvaluationAssignmentServiceStub(clock, executor);
        UnrollExecutorStub unrollStub = new UnrollExecutorStub(executor);
        String projectId = "junk";
        notificationFactoryStub = new StatefulNotificationChannelFactoryStub(executor, projectId);
        MuteMatcherStub muteMatcherStub = new MuteMatcherStub();
        factory = new ActivityFactory(new SimpleActivitiesFactory(
                new ProjectAssignment(projectId, "localhost", new AssignmentSeqNo(42, 1)),
                unrollStub,
                evaluationStub,
                notificationFactoryStub,
                muteMatcherStub), null);
    }

    @After
    public void tearDown() throws Exception {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void evaluate() {
        Alert alert = randomAlert();

        EvaluationStatus expected = EvaluationStatus.ERROR.withDescription("some error details");
        FailedAlertActivity activity = makeAndRun(alert);

        EvaluationState evalV1 = activity.getLatestEvaluation();
        assertThat(evalV1.getStatus(), equalTo(expected));
    }

    @Test
    public void reportLatestEvaluationTime() {
        Alert alert = randomAlert();

        evaluationStub.predefineStatus(alert.getId(), () -> EvaluationStatus.ALARM);
        FailedAlertActivity activity = makeAndRun(alert);

        String v1 = getEncodedStatus(activity);
        assertThat(encodeMetrics(activity), containsString(v1));

        String v2 = getEncodedStatus(activity);
        assertThat(encodeMetrics(activity), containsString(v2));
    }

    @Test
    public void reportLatestEvaluationStatus() {
        Alert alert = randomAlert();

        FailedAlertActivity activity = makeAndRun(alert);
        for (EvaluationStatus.Code code : EvaluationStatus.Code.values()) {
            evaluationStub.predefineStatus(alert.getId(), code::toStatus);

            String expect = getEncodedStatus(activity);
            assertThat(encodeMetrics(activity), containsString(expect));
        }
    }

    @Test
    public void restore() {
        NotificationChannelStub channel = new NotificationChannelStub(randomNotification());
        notificationFactoryStub.setNotificationChannel(channel);
        Alert alert = randomAlert()
                .toBuilder()
                .setNotificationChannel(channel.getId())
                .build();

        FailedAlertActivity activity = makeAndRun(alert);
        evaluationStub.predefineStatus(alert.getId(), () -> EvaluationStatus.ERROR);
        activity.cancel();
        TPersistAlertState dump = activity.dumpState();
        restore(alert, dump);
    }

    private FailedAlertActivity restore(Alert alert, TPersistAlertState dump) {
        FailedAlertActivity result = (FailedAlertActivity) factory.makeFailedActivity(alert, new IllegalArgumentException());
        result.restore(dump);
        return result;
    }

    private Notification randomNotification() {
        return NotificationTestSupport.randomNotification()
                .toBuilder()
                .setNotifyAboutStatus(EvaluationStatus.Code.ERROR)
                .setProjectId("junk")
                .setRepeatNotifyDelay(Duration.ofMillis(10))
                .build();
    }

    private String getEncodedStatus(FailedAlertActivity activity) {
        EvaluationState state = requireNonNull(activity.getLatestEvaluation());

        return String.format("IGAUGE alert.evaluation.status{alertId='%s', projectId='%s'} [%s]",
                state.getAlertId(),
                state.getProjectId(),
                state.getStatus().getCode().getNumber());
    }

    private String encodeMetrics(FailedAlertActivity activity) {
        StringWriter writer = new StringWriter();
        try (MetricTextEncoder e = new MetricTextEncoder(writer, true)) {
            e.onStreamBegin(-1);
            activity.appendAlertMetrics(e);
            e.onStreamEnd();
        }
        return writer.toString();
    }

    private Alert randomAlert() {
        return AlertTestSupport.randomAlert(ThreadLocalRandom.current(), false)
                .toBuilder()
                .setProjectId("junk")
                .setState(AlertState.ACTIVE)
                .setGroupByLabels(Collections.emptyList())
                .build();
    }

    private FailedAlertActivity makeAndRun(Alert alert) {
        FailedAlertActivity result = (FailedAlertActivity) factory.makeFailedActivity(alert, new IllegalArgumentException());
        result.run();
        return result;
    }

}
