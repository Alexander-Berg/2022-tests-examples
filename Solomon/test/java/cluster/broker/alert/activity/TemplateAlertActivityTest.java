package ru.yandex.solomon.alert.cluster.broker.alert.activity;

import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
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
import ru.yandex.solomon.alert.dao.memory.InMemoryAlertTemplateDao;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertState;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.template.AlertFromTemplatePersistent;
import ru.yandex.solomon.alert.notification.NotificationState;
import ru.yandex.solomon.alert.notification.channel.NotificationChannelStub;
import ru.yandex.solomon.alert.notification.domain.Notification;
import ru.yandex.solomon.alert.notification.domain.NotificationTestSupport;
import ru.yandex.solomon.alert.protobuf.TPersistAlertState;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.unroll.UnrollExecutorStub;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.alert.util.CommonMatchers.reflectionEqualTo;

/**
 * @author Alexey Trushkin
 */
public class TemplateAlertActivityTest {

    private ManualClock clock;
    private ScheduledExecutorService executor;
    private EvaluationAssignmentServiceStub evaluationStub;
    private StatefulNotificationChannelFactoryStub notificationFactoryStub;
    private MuteMatcherStub muteMatcherStub;
    private SimpleActivitiesFactory simpleActivitiesFactory;
    private TemplateAlertFactory templateAlertFactory;
    private InMemoryAlertTemplateDao alertTemplateDao;
    @Rule
    public Timeout timeoutRule = Timeout.builder()
            .withLookingForStuckThread(true)
            .withTimeout(15, TimeUnit.SECONDS)
            .build();

    @Before
    public void setUp() throws Exception {
        clock = new ManualClock();
        executor = new ManualScheduledExecutorService(2, clock);
        evaluationStub = new EvaluationAssignmentServiceStub(clock, executor);
        UnrollExecutorStub unrollStub = new UnrollExecutorStub(executor);
        String projectId = "junk";
        notificationFactoryStub = new StatefulNotificationChannelFactoryStub(executor, projectId);
        muteMatcherStub = new MuteMatcherStub();
        simpleActivitiesFactory = new SimpleActivitiesFactory(
                new ProjectAssignment(projectId, "localhost", new AssignmentSeqNo(42, 1)),
                unrollStub,
                evaluationStub,
                notificationFactoryStub,
                muteMatcherStub);
        alertTemplateDao = new InMemoryAlertTemplateDao(true, true);
        templateAlertFactory = new TemplateAlertFactory(new MustacheTemplateFactory());
    }

    @After
    public void tearDown() throws Exception {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void assignToEvaluate() throws InterruptedException {
        Alert alert = randomAlert();

        EvaluationStatus alarm = EvaluationStatus.ALARM.withDescription("some alarm details");
        evaluationStub.predefineStatus(alert.getId(), () -> alarm);
        TemplateAlertActivity activity = makeAndRun(alert);
        awaitEvaluation(activity);

        EvaluationState evalV1 = activity.getLatestEvaluation();
        assertThat(evalV1.getStatus(), equalTo(alarm));

        EvaluationStatus ok = EvaluationStatus.OK.withDescription("back to normal");
        evaluationStub.predefineStatus(alert.getId(), () -> ok);
        awaitEvaluation(activity);

        EvaluationState evalV2 = activity.getLatestEvaluation();
        assertThat(evalV2.getStatus(), equalTo(ok));
    }

    @Test
    public void restoreEvaluationState() throws InterruptedException {
        Alert alert = randomAlert();

        evaluationStub.predefineStatus(alert.getId(), () -> EvaluationStatus.ALARM.withDescription("some details"));
        TemplateAlertActivity activity = makeAndRun(alert);
        awaitEvaluation(activity);

        activity.cancel();
        EvaluationState evaluationState = activity.getLatestEvaluation();
        TPersistAlertState dump = activity.dumpState();

        TemplateAlertActivity activityRestore = restore(alert, dump);
        EvaluationState evaluationStateRestore = activityRestore.getLatestEvaluation();
        assertThat(evaluationStateRestore, reflectionEqualTo(evaluationState));
    }

    @Test
    public void cancelEvaluation() throws InterruptedException {
        Alert alert = randomAlert();
        TemplateAlertActivity activity = makeAndRun(alert);
        awaitEvaluation(activity);
        EvaluationState latestEvaluation = activity.getLatestEvaluation();
        assertThat(activity.isCanceled(), equalTo(false));
        activity.cancel();
        assertThat(activity.isCanceled(), equalTo(true));

        var sync = evaluationStub.getSyncEval(activity.getAlert().getId());
        clock.passedTime(5, TimeUnit.MINUTES);
        boolean syncWait = sync.tryAcquire(10, TimeUnit.MILLISECONDS);
        assertThat(syncWait, equalTo(false));
        assertThat(latestEvaluation, equalTo(activity.getLatestEvaluation()));
    }

    @Test
    public void notifyAboutEvaluationStatus() throws InterruptedException {
        NotificationChannelStub channel = new NotificationChannelStub(randomNotification());
        notificationFactoryStub.setNotificationChannel(channel);
        Alert alert = randomAlert()
                .toBuilder()
                .setNotificationChannel(channel.getId())
                .build();

        TemplateAlertActivity activity = makeAndRun(alert);
        evaluationStub.predefineStatus(alert.getId(), () -> EvaluationStatus.ALARM);
        awaitNotification(channel, activity);

        NotificationState notifyOne = getNotificationState(activity);
        evaluationStub.predefineStatus(alert.getId(), () -> EvaluationStatus.OK);
        awaitNotification(channel, activity);
        NotificationState notifyTwo = getNotificationState(activity);

        assertThat(notifyTwo.getLatestEval().toEpochMilli(), greaterThan(notifyOne.getLatestEval().toEpochMilli()));
        assertThat(notifyTwo.getLatestSuccessNotify().toEpochMilli(), greaterThan(notifyOne.getLatestSuccessNotify().toEpochMilli()));
    }

    @Test
    public void restoreNotificationStatus() throws InterruptedException {
        NotificationChannelStub channel = new NotificationChannelStub(randomNotification());
        notificationFactoryStub.setNotificationChannel(channel);
        Alert alert = randomAlert()
                .toBuilder()
                .setNotificationChannel(channel.getId())
                .build();

        TemplateAlertActivity activity = makeAndRun(alert);
        awaitNotification(channel, activity);

        evaluationStub.predefineStatus(alert.getId(), () -> EvaluationStatus.ALARM);
        awaitNotification(channel, activity);

        activity.cancel();
        NotificationState notificationState = getNotificationState(activity);
        TPersistAlertState dump = activity.dumpState();

        TemplateAlertActivity restoredActivity = restore(alert, dump);
        NotificationState restoredNotificationState = getNotificationState(restoredActivity);
        assertThat(notificationState, reflectionEqualTo(restoredNotificationState));
    }

    @Test
    public void skipMetricReportWhenNotEvaluatedYet() {
        AlertFromTemplatePersistent alert = randomAlert();
        var templated = templateAlertFactory.createAlertFromTemplate(alert, alertTemplateDao.findById(alert.getTemplateId(), alert.getTemplateVersionTag()).join().get());
        TemplateAlertActivity activity = new TemplateAlertActivity(alert, templated, simpleActivitiesFactory, null);
        String metrics = encodeMetrics(activity);
        assertThat(metrics, Matchers.isEmptyString());
    }

    @Test
    public void reportLatestEvaluationTime() throws InterruptedException {
        Alert alert = randomAlert();

        evaluationStub.predefineStatus(alert.getId(), () -> EvaluationStatus.ALARM);
        TemplateAlertActivity activity = makeAndRun(alert);
        awaitEvaluation(activity);

        String v1 = getEncodedStatus(activity);
        assertThat(encodeMetrics(activity), containsString(v1));

        awaitEvaluation(activity);

        String v2 = getEncodedStatus(activity);
        assertThat(encodeMetrics(activity), containsString(v2));
    }

    @Test
    public void reportLatestEvaluationStatus() throws InterruptedException {
        Alert alert = randomAlert();

        TemplateAlertActivity activity = makeAndRun(alert);
        for (EvaluationStatus.Code code : EvaluationStatus.Code.values()) {
            evaluationStub.predefineStatus(alert.getId(), code::toStatus);
            awaitEvaluation(activity);

            String expect = getEncodedStatus(activity);
            assertThat(encodeMetrics(activity), containsString(expect));
        }
    }

    private String getEncodedStatus(TemplateAlertActivity activity) {
        EvaluationState state = requireNonNull(activity.getLatestEvaluation());

        return String.format("IGAUGE alert.evaluation.status{alertId='%s', projectId='%s'} [%s]",
                state.getAlertId(),
                state.getProjectId(),
                state.getStatus().getCode().getNumber());
    }

    private String encodeMetrics(TemplateAlertActivity activity) {
        StringWriter writer = new StringWriter();
        try (MetricTextEncoder e = new MetricTextEncoder(writer, true)) {
            e.onStreamBegin(-1);
            activity.appendAlertMetrics(e);
            e.onStreamEnd();
        }
        return writer.toString();
    }

    private Notification randomNotification() {
        return NotificationTestSupport.randomNotification()
                .toBuilder()
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
                .setProjectId("junk")
                .setRepeatNotifyDelay(Duration.ofMillis(10))
                .build();
    }

    private AlertFromTemplatePersistent randomAlert() {
        return AlertTestSupport.alertFromTemplatePersistent(ThreadLocalRandom.current())
                .toBuilder()
                .setProjectId("junk")
                .setState(AlertState.ACTIVE)
                .setGroupByLabels(Collections.emptyList())
                .build();
    }

    private TemplateAlertActivity makeAndRun(Alert alertParam) {
        AlertFromTemplatePersistent alert = (AlertFromTemplatePersistent) alertParam;
        var templated = templateAlertFactory.createAlertFromTemplate(alert, alertTemplateDao.findById(alert.getTemplateId(), alert.getTemplateVersionTag()).join().get());
        TemplateAlertActivity result = new TemplateAlertActivity(alert, templated, simpleActivitiesFactory, null);
        result.run();
        return result;
    }

    private TemplateAlertActivity restore(Alert alertParam, TPersistAlertState dump) {
        AlertFromTemplatePersistent alert = (AlertFromTemplatePersistent) alertParam;
        var templated = templateAlertFactory.createAlertFromTemplate(alert, alertTemplateDao.findById(alert.getTemplateId(), alert.getTemplateVersionTag()).join().get());
        TemplateAlertActivity result = new TemplateAlertActivity(alert, templated, simpleActivitiesFactory, null);
        result.restore(dump);
        return result;
    }

    private void awaitEvaluation(TemplateAlertActivity activity) throws InterruptedException {
        var sync = evaluationStub.getSyncEval(activity.getAlert().getId());
        clock.passedTime(1, TimeUnit.MINUTES);
        sync.acquire();
    }

    private void awaitNotification(NotificationChannelStub channel, TemplateAlertActivity activity) throws InterruptedException {
        NotificationState prev = getNotificationState(activity);
        var sync = channel.getSendSync();
        clock.passedTime(1, TimeUnit.MINUTES);
        sync.acquire();

        NotificationState current;
        do {
            current = getNotificationState(activity);
            if (prev.getLatestEval().isBefore(current.getLatestEval())) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(0, 5));
        } while (true);
    }

    private NotificationState getNotificationState(TemplateAlertActivity activity) {
        return Iterables.getOnlyElement(activity
                .getNotificationStates(Collections.emptySet())
                .collect(Collectors.toList()));
    }
}
