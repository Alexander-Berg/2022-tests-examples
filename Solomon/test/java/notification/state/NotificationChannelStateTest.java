package ru.yandex.solomon.alert.notification.state;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.Timeout;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.ChannelConfig;
import ru.yandex.solomon.alert.notification.NotificationKey;
import ru.yandex.solomon.alert.notification.NotificationState;
import ru.yandex.solomon.alert.notification.RetryOptions;
import ru.yandex.solomon.alert.notification.channel.Event;
import ru.yandex.solomon.alert.notification.channel.NotificationChannel;
import ru.yandex.solomon.alert.notification.channel.NotificationChannelStub;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.domain.Notification;
import ru.yandex.solomon.alert.notification.domain.NotificationTestSupport;
import ru.yandex.solomon.alert.rule.EvaluationState;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomActiveAlert;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.eval;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.nextEval;

/**
 * @author Vladimir Gordiychuk
 */
public class NotificationChannelStateTest {

    @Rule
    public TestName testName = new TestName();
    private ScheduledExecutorService executorService;

    @Rule
    public Timeout timeoutRule = Timeout.builder()
        .withLookingForStuckThread(true)
        .withTimeout(15, TimeUnit.SECONDS)
        .build();

    @Before
    public void setUp() throws Exception {
        executorService = Executors.newScheduledThreadPool(2);
    }

    @After
    public void tearDown() throws Exception {
        executorService.shutdownNow();
    }

    @Test
    public void skipByStatusOnPending() {
        Notification notification = newNotification()
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build();
        Alert alert = createAlert(notification.getId());

        NotificationKey key = NotificationKey.of(alert, notification.getId());
        StateContextStub context = makeContext(alert, notification);
        PendingState pending = new PendingState(context, NotificationState.init(key));
        context.setState(pending);
        NotificationStatus status = pending.process(eval(alert, EvaluationStatus.OK)).join();
        assertThat(status.getCode(), equalTo(NotificationStatus.Code.SKIP_BY_STATUS));
    }

    @Test
    public void skipRepeatOnPending() {
        Notification notification = NotificationTestSupport.randomEmailNotification(ThreadLocalRandom.current())
                .toBuilder()
                .setName(testName.getMethodName())
                .setNotifyAboutStatus(EvaluationStatus.Code.OK, EvaluationStatus.Code.ALARM)
                .setRepeatNotifyDelay(Duration.ZERO)
                .build();

        Alert alert = createAlert(notification.getId());
        StateContextStub context = initState(alert, notification);
        Event alarm = eval(alert, EvaluationStatus.ALARM);
        assertThat(context.syncSend(alarm).getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        Event stillAlarm = alarm;
        for (int index = 0; index < 3; index++) {
            stillAlarm = nextEval(stillAlarm, EvaluationStatus.ALARM);
            assertThat(context.syncSend(stillAlarm).getCode(), equalTo(NotificationStatus.Code.SKIP_REPEAT));
        }

        Event ok = nextEval(stillAlarm, EvaluationStatus.OK);
        assertThat(context.syncSend(ok).getCode(), equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void repeatNotifyAfterDelayPast() {
        Notification notification = newNotification()
                .setNotifyAboutStatus(EvaluationStatus.Code.OK, EvaluationStatus.Code.ALARM)
                .setRepeatNotifyDelay(Duration.ofMinutes(2))
                .build();

        Alert alert = createAlert(notification.getId());
        StateContextStub context = initState(alert, notification);

        Event alarm0Min = eval(alert, EvaluationStatus.ALARM);
        assertThat(context.syncSend(alarm0Min).getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        Event alarm1Min = nextEval(alarm0Min, EvaluationStatus.ALARM);
        assertThat(context.syncSend(alarm1Min).getCode(), equalTo(NotificationStatus.Code.SKIP_REPEAT));

        Event alarm2Min = nextEval(alarm1Min, EvaluationStatus.ALARM);
        assertThat(context.syncSend(alarm2Min).getCode(), equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void repeatEveryNotify() {
        Notification notification = newNotification()
                .setNotifyAboutStatus(EvaluationStatus.Code.OK, EvaluationStatus.Code.ALARM)
                .setRepeatNotifyDelay(Duration.ofMillis(1))
                .build();

        Alert alert = createAlert(notification.getId());
        StateContextStub context = initState(alert, notification);

        Event alarm0Min = eval(alert, EvaluationStatus.ALARM);
        assertThat(context.syncSend(alarm0Min).getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        Event alarm1Min = nextEval(alarm0Min, EvaluationStatus.ALARM);
        assertThat(context.syncSend(alarm1Min).getCode(), equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void skipByStateOnPending() {
        Notification notification = newNotification()
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build();

        Alert alert = createAlert(notification.getId());

        Event ok = eval(alert, EvaluationStatus.OK);

        NotificationKey key = NotificationKey.of(alert, notification.getId());
        StateContextStub context = makeContext(alert, notification);
        NotificationState notificationState = NotificationState.init(key)
                .nextStatus(NotificationStatus.SUCCESS, ok.getState());

        PendingState pendingState = new PendingState(context, notificationState);
        context.setState(pendingState);
        NotificationStatus status = pendingState.process(nextEval(ok, EvaluationStatus.OK)).join();
        assertThat(status.getCode(), equalTo(NotificationStatus.Code.SKIP_BY_STATUS));
    }

    @Test
    public void skipByStateOnSending() {
        Notification notification = newNotification()
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build();

        Alert alert = createAlert(notification.getId());

        EvaluationState alarm = EvaluationState.newBuilder(alert)
                .setStatus(EvaluationStatus.OK)
                .setSince(Instant.now())
                .build();

        NotificationKey key = NotificationKey.of(alert, notification.getId());
        StateContextStub context = makeContext(alert, notification);
        SendingState sendingState = new SendingState(context, NotificationState.init(key), eval(alert, alarm));
        context.setState(sendingState);
        NotificationStatus status = sendingState.process(eval(alert, nextStatus(alarm, EvaluationStatus.OK))).join();
        assertThat(status.getCode(), equalTo(NotificationStatus.Code.SKIP_BY_STATUS));
    }

    @Test
    public void skipByStateOnRetrying() {
        Notification notification = newNotification()
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build();

        Alert alert = createAlert(notification.getId());

        EvaluationState alarm = EvaluationState.newBuilder(alert)
                .setStatus(EvaluationStatus.OK)
                .setSince(Instant.now())
                .build();

        NotificationKey key = NotificationKey.of(alert, notification.getId());
        StateContextStub context = makeContext(alert, notification);
        RetryingState retryingState = new RetryingState(context, NotificationState.init(key), new EventProcessingTask(eval(alert, alarm)), 1, 0);
        context.setState(retryingState);
        NotificationStatus status = retryingState.process(eval(alert, nextStatus(alarm, EvaluationStatus.OK))).join();
        assertThat(status.getCode(), equalTo(NotificationStatus.Code.SKIP_BY_STATUS));
    }

    @Test
    public void obsoleteOnPending() {
        Notification notification = newNotification()
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build();

        Alert alert = createAlert(notification.getId());

        EvaluationState obsolete = EvaluationState.newBuilder(alert)
                .setStatus(EvaluationStatus.OK)
                .setSince(Instant.now())
                .build();

        NotificationKey key = NotificationKey.of(alert, notification.getId());
        StateContextStub context = makeContext(alert, notification);

        EvaluationState upToDate = nextStatus(obsolete, EvaluationStatus.ALARM);
        NotificationState notificationState = NotificationState.init(key).nextStatus(NotificationStatus.SUCCESS, upToDate);
        PendingState pendingState = new PendingState(context, notificationState);
        context.setState(pendingState);
        assertThat(context.syncSend(eval(alert, obsolete)).getCode(), equalTo(NotificationStatus.Code.OBSOLETE));
    }

    @Test
    public void obsoleteOnSending() {
        Notification notification = newNotification()
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build();

        Alert alert = createAlert(notification.getId());

        EvaluationState obsolete = EvaluationState.newBuilder(alert)
                .setStatus(EvaluationStatus.OK)
                .setSince(Instant.now())
                .build();

        NotificationKey key = NotificationKey.of(alert, notification.getId());
        StateContextStub context = makeContext(alert, notification);

        EvaluationState upToDate = nextStatus(obsolete, EvaluationStatus.ALARM);
        NotificationState notificationState = NotificationState.init(key).nextStatus(NotificationStatus.SUCCESS, obsolete);
        SendingState sendingState = new SendingState(context, notificationState, eval(alert, upToDate));
        context.setState(sendingState);
        assertThat(context.syncSend(eval(alert, obsolete)).getCode(), equalTo(NotificationStatus.Code.OBSOLETE));
    }

    @Test
    public void obsoleteOnRetrying() {
        Notification notification = newNotification()
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build();

        Alert alert = createAlert(notification.getId());

        EvaluationState obsolete = EvaluationState.newBuilder(alert)
                .setStatus(EvaluationStatus.OK)
                .setSince(Instant.now())
                .build();

        NotificationKey key = NotificationKey.of(alert, notification.getId());

        EvaluationState upToDate = nextStatus(obsolete, EvaluationStatus.ALARM);
        StateContextStub context = makeContext(alert, notification);
        RetryingState retryingState = new RetryingState(
                context,
                NotificationState.init(key).nextStatus(NotificationStatus.SUCCESS, obsolete),
                new EventProcessingTask(eval(alert, upToDate)),
                1,
                0);
        context.setState(retryingState);
        assertThat(context.syncSend(eval(alert, obsolete)).getCode(), equalTo(NotificationStatus.Code.OBSOLETE));
    }

    @Test
    public void successFromPending() {
        Notification notification = newNotification()
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build();

        Alert alert = createAlert(notification.getId());

        EvaluationState alarm = EvaluationState.newBuilder(alert)
                .setStatus(EvaluationStatus.ALARM)
                .setSince(Instant.now())
                .build();

        NotificationKey key = NotificationKey.of(alert, notification.getId());
        StateContextStub context = makeContext(alert, notification);
        PendingState pendingState = new PendingState(context, NotificationState.init(key));
        context.setState(pendingState);
        NotificationStatus result = pendingState.process(eval(alert, alarm)).join();
        assertThat(result.getCode(), equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void errorFromPending() {
        Notification notification = newNotification()
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build();

        Alert alert = createAlert(notification.getId());

        EvaluationState alarm = EvaluationState.newBuilder(alert)
                .setStatus(EvaluationStatus.ALARM)
                .setSince(Instant.now())
                .build();

        NotificationKey key = NotificationKey.of(alert, notification.getId());
        NotificationChannelStub channel = new NotificationChannelStub(notification);
        channel.predefineStatuses(NotificationStatus.ERROR);

        StateContextStub context = makeContext(alert, channel);
        PendingState pendingState = new PendingState(context, NotificationState.init(key));
        context.setState(pendingState);

        NotificationStatus result = pendingState.process(eval(alert, alarm)).join();
        assertThat(result.getCode(), equalTo(NotificationStatus.Code.ERROR));
    }

    @Test
    public void repeatedSendObsolete() {
        Notification notification = newNotification()
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build();

        Alert alert = createAlert(notification.getId());

        EvaluationState alarm = EvaluationState.newBuilder(alert)
                .setStatus(EvaluationStatus.ALARM)
                .setSince(Instant.now())
                .build();

        StateContextStub context = initState(alert, notification);

        Event message = eval(alert, alarm);
        assertThat(context.syncSend(message).getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        assertThat(context.syncSend(message).getCode(), equalTo(NotificationStatus.Code.OBSOLETE));
    }

    @Test
    public void giveUpOnRetryByAttemptLimit() {
        NotificationChannelStub channel = new NotificationChannelStub(newNotification()
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
                .build());

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setNotificationChannel(channel.getId())
                .build();

        RetryOptions retryOptions = RetryOptions.newBuilder()
                .setInitialDelay(10, TimeUnit.MILLISECONDS)
                .setTaskRetryLimit(3)
                .setTaskAgeLimit(1, TimeUnit.HOURS)
                .setMaxRetryDelay(1, TimeUnit.SECONDS)
                .build();

        channel.predefineStatuses(NotificationStatus.ERROR_ABLE_TO_RETRY);
        StateContextStub context = initState(alert, channel, retryOptions);

        EvaluationState alarm = EvaluationState.newBuilder(alert)
                .setStatus(EvaluationStatus.ALARM)
                .setSince(Instant.now())
                .build();

        assertThat(context.syncSend(eval(alert, alarm)).getCode(), equalTo(NotificationStatus.Code.ERROR_ABLE_TO_RETRY));
    }

    @Test
    public void giveUpOnRetryByTimeLimit() {
        NotificationChannelStub channel = new NotificationChannelStub(newNotification()
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
                .build());

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setNotificationChannel(channel.getId())
                .build();

        RetryOptions retryOptions = RetryOptions.newBuilder()
                .setInitialDelay(10, TimeUnit.MILLISECONDS)
                .setTaskRetryLimit(Integer.MAX_VALUE)
                .setTaskAgeLimit(300, TimeUnit.MILLISECONDS)
                .setMaxRetryDelay(1, TimeUnit.SECONDS)
                .build();

        channel.predefineStatuses(NotificationStatus.ERROR_ABLE_TO_RETRY);
        StateContextStub context = initState(alert, channel, retryOptions);

        EvaluationState alarm = EvaluationState.newBuilder(alert)
                .setStatus(EvaluationStatus.ALARM)
                .setSince(Instant.now())
                .build();

        assertThat(context.syncSend(eval(alert, alarm)).getCode(), equalTo(NotificationStatus.Code.ERROR_ABLE_TO_RETRY));
    }

    @Test
    public void awaitCompleteSendBeforeSendAnotherEventToSameChannel() {
        NotificationChannelStub channel = new NotificationChannelStub(newNotification()
                .setName(testName.getMethodName())
                .setRepeatNotifyDelay(Duration.ZERO)
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
                .build());

        Alert alert = createAlert(channel.getId());

        Event ok = eval(alert, EvaluationStatus.Code.OK);

        NotificationKey key = NotificationKey.of(alert, channel.getId());

        channel.predefineStatuses(NotificationStatus.SUCCESS);
        StateContextStub context = makeContext(alert, channel);

        Event alarm = nextEval(ok, EvaluationStatus.ALARM);
        NotificationState notificationState = NotificationState.init(key).nextStatus(NotificationStatus.SUCCESS, ok.getState());

        // Sending about first alarm in progress
        SendingState sendingState = new SendingState(context, notificationState, alarm);
        context.setState(sendingState);

        // One more alarm event received, previous still delivering and we can't cancel it
        // because another server can accept it but still not reply with OK for us
        Event alarmTwo = nextEval(alarm, EvaluationStatus.ALARM);
        CompletableFuture<NotificationStatus> sendingTwo = context.getCurrentChannelState().process(alarmTwo);
        CompletableFuture<NotificationStatus> sendingOne = sendingState.sendPreparedMessage();

        assertThat(sendingOne.join().getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        assertThat(sendingTwo.join().getCode(), equalTo(NotificationStatus.Code.SKIP_REPEAT));
    }

    @Test
    public void awaitCompleteRetryBeforeSendAnotherEventToSameChannel() {
        NotificationChannelStub channel = new NotificationChannelStub(newNotification()
                .setName(testName.getMethodName())
                .setRepeatNotifyDelay(Duration.ZERO)
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
                .build());

        Alert alert = createAlert(channel.getId());

        Event ok = eval(alert, EvaluationStatus.Code.OK);

        NotificationKey key = NotificationKey.of(alert, channel.getId());

        channel.predefineStatuses(NotificationStatus.SUCCESS);
        StateContextStub context = makeContext(alert, channel);

        Event alarm = nextEval(ok, EvaluationStatus.ALARM);
        NotificationState notificationState = NotificationState.init(key).nextStatus(NotificationStatus.ERROR_ABLE_TO_RETRY, ok.getState());

        // Retrying send about first alarm in progress
        RetryingState retryingState = new RetryingState(context, notificationState, new EventProcessingTask(alarm), 1, 100);
        context.setState(retryingState);

        // One more alarm event received, previous still delivering and we can't cancel it
        // because another server can accept it but still not reply with OK for us
        Event alarmTwo = nextEval(alarm, EvaluationStatus.ALARM);
        CompletableFuture<NotificationStatus> sendingTwo = context.getCurrentChannelState().process(alarmTwo);
        CompletableFuture<NotificationStatus> sendingOne = retryingState.sendPreparedMessage();

        assertThat(sendingOne.join().getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        assertThat(sendingTwo.join().getCode(), equalTo(NotificationStatus.Code.SKIP_REPEAT));
    }

    @Test
    public void retryNewEventIfItsPossible() {
        NotificationChannelStub channel = new NotificationChannelStub(newNotification()
                .setName(testName.getMethodName())
                .setRepeatNotifyDelay(Duration.ZERO)
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
                .build());

        Alert alert = createAlert(channel.getId());

        Event ok = eval(alert, EvaluationStatus.Code.OK);

        NotificationKey key = NotificationKey.of(alert, channel.getId());

        RetryOptions retryOptions = RetryOptions.newBuilder()
                .setInitialDelay(10, TimeUnit.MILLISECONDS)
                .setTaskRetryLimit(Integer.MAX_VALUE)
                .setTaskAgeLimit(1, TimeUnit.HOURS)
                .setMaxRetryDelay(30, TimeUnit.MILLISECONDS)
                .build();

        channel.predefineStatuses(
                NotificationStatus.ERROR_ABLE_TO_RETRY,
                NotificationStatus.ERROR_ABLE_TO_RETRY,
                NotificationStatus.ERROR_ABLE_TO_RETRY,
                NotificationStatus.SUCCESS);
        StateContextStub context = makeContext(alert, channel, retryOptions);

        Event alarm = nextEval(ok, EvaluationStatus.ALARM);
        NotificationState notificationState = NotificationState.init(key).nextStatus(NotificationStatus.ERROR_ABLE_TO_RETRY, ok.getState());

        // Retrying send about first alarm in progress
        var task = new EventProcessingTask(alarm);
        CompletableFuture<NotificationStatus> sendingOne = task.getFuture();
        RetryingState retryingState = new RetryingState(context, notificationState, task, 1, 100);
        context.setState(retryingState);

        // One more alarm event received and its more actual event, if we don't start sending
        // event on retry we should use more new event instead of previous
        Event alarmTwo = nextEval(alarm, EvaluationStatus.ALARM);
        CompletableFuture<NotificationStatus> sendingTwo = context.getCurrentChannelState().process(alarmTwo);
        retryingState.sendPreparedMessage();

        assertThat(sendingOne.join().getCode(), equalTo(NotificationStatus.Code.ERROR_ABLE_TO_RETRY));
        assertThat(sendingTwo.join().getCode(), equalTo(NotificationStatus.Code.SUCCESS));
    }

    private Notification.Builder<?, ?> newNotification() {
        return NotificationTestSupport.randomNotification()
                .toBuilder()
                .setName(testName.getMethodName());
    }

    private Alert createAlert(String notificationId) {
        return randomActiveAlert()
                .toBuilder()
                .setNotificationChannel(notificationId)
                .build();
    }

    private StateContextStub makeContext(Alert alert, Notification notification) {
        return makeContext(alert, new NotificationChannelStub(notification));
    }

    private StateContextStub makeContext(Alert alert, NotificationChannel channel) {
        return makeContext(alert, channel, RetryOptions.empty());
    }

    private StateContextStub makeContext(Alert alert, NotificationChannel channel, RetryOptions retryOptions) {
        ChannelConfig configOverride = alert.getNotificationChannels().get(channel.getId());
        return makeContext(channel, configOverride, retryOptions);
    }

    private StateContextStub makeContext(NotificationChannel channel, ChannelConfig channelConfig, RetryOptions retryOptions) {
        return new StateContextStub(channel, channelConfig, executorService, retryOptions);
    }

    private StateContextStub initState(Alert alert, Notification notification) {
        return initState(alert, new NotificationChannelStub(notification));
    }

    private StateContextStub initState(Alert alert, NotificationChannel channel) {
        return initState(alert, channel, RetryOptions.empty());
    }

    private StateContextStub initState(Alert alert, NotificationChannel channel, RetryOptions options) {
        NotificationKey key = NotificationKey.of(alert, channel.getId());
        StateContextStub context = makeContext(alert, channel, options);
        PendingState state = new PendingState(context, NotificationState.init(key));
        context.setState(state);
        return context;
    }

    private EvaluationState nextStatus(EvaluationState state, EvaluationStatus status) {
        Instant nextTime = state.getLatestEval().plus(10, ChronoUnit.MINUTES);
        return state.nextStatus(status, nextTime);
    }
}
