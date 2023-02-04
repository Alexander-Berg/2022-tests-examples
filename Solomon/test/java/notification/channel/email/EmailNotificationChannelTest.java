package ru.yandex.solomon.alert.notification.channel.email;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.commune.mail.HeaderNames;
import ru.yandex.commune.mail.MailUtils;
import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.ChannelConfig;
import ru.yandex.solomon.alert.domain.SubAlert;
import ru.yandex.solomon.alert.inject.spring.notification.EmailTemplate;
import ru.yandex.solomon.alert.notification.DispatchRule;
import ru.yandex.solomon.alert.notification.TemplateVarsFactory;
import ru.yandex.solomon.alert.notification.channel.Event;
import ru.yandex.solomon.alert.notification.channel.NotificationChannel;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.domain.email.EmailNotification;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.unroll.MultiAlertUtils;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomActiveAlert;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlert;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.eval;

/**
 * @author Vladimir Gordiychuk
 */
public class EmailNotificationChannelTest {
    @Rule
    public Timeout globalTimeout = Timeout.builder()
            .withTimeout(3, TimeUnit.SECONDS)
            .withLookingForStuckThread(true)
            .build();

    private EmailNotificationChannelFactory factory;
    private MailTransportStub mailSender;
    private ManualClock clock;
    private ManualScheduledExecutorService executorService;
    private EmailClient emailClient;

    @Before
    public void setUp() throws Exception {
        MustacheTemplateFactory templateFactory = new MustacheTemplateFactory();

        mailSender = new MailTransportStub();
        clock = new ManualClock();
        executorService = new ManualScheduledExecutorService(2, clock);
        emailClient = new AsyncEmailClient(mailSender);
        factory = new EmailNotificationChannelFactory(
                emailClient,
                executorService,
                templateFactory,
                new TemplateVarsFactory("localhost", "localhost"),
                EmailTemplate.create(templateFactory));
    }

    @After
    public void tearDown() throws Exception {
        emailClient.close();
        executorService.shutdownNow();
    }

    @Test
    public void sendingWhenChangeToInterestingState() throws Exception {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Email to solomon-test-notification@yandex-team.ru")
                .setRecipient("solomon-test-notification@yandex-team.ru")
                .setSubjectTemplate("[{{status.code}}]: {{alert.id}}")
                .setContentTemplate("Hello world with status {{status.code}}!")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build());

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setId("my-first-alert")
                .build();

        Event alarm = okToAlarmEvent(alert);
        assertThat(syncSend(channel, alarm).getCode(), equalTo(NotificationStatus.SUCCESS.getCode()));

        MimeMessage message = MailUtils.toMimeMessage(mailSender.receiveMessage());
        assertThat(message.getSubject(), equalTo("[ALARM]: my-first-alert"));
        assertThat(message.getContent(), equalTo("Hello world with status ALARM!"));

        String recipients = message.getHeader(HeaderNames.TO, ",");
        assertThat(recipients, containsString("solomon-test-notification@yandex-team.ru"));
    }

    @Test
    public void sendIntoMultipleRecipients() throws Exception {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Multiple recipient email")
                .setRecipients(ImmutableSet.of("test@gmail.com", "casey.goodson@gmail.com"))
                .setSubjectTemplate("[{{status.code}}]: {{alert.id}}")
                .setContentTemplate("Hello world with status {{status.code}}!")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build());

        assertThat(syncSend(channel, okToAlarmEvent()).getCode(), equalTo(NotificationStatus.SUCCESS.getCode()));

        MimeMessage message = MailUtils.toMimeMessage(mailSender.receiveMessage());

        String recipients = message.getHeader(HeaderNames.TO, ",");
        assertThat(recipients, containsString("test@gmail.com"));
        assertThat(recipients, containsString("casey.goodson@gmail.com"));
    }

    @Test
    public void successWhenUseNotExistsVariable() throws Exception {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Multiple recipient email")
                .setRecipients(ImmutableSet.of("test@gmail.com", "casey.goodson@gmail.com"))
                .setSubjectTemplate("[{{not-exists-param}}]: {{alert.id}}")
                .setContentTemplate("Hello world with status {{not-exists-param-status}}!")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build());

        assertThat(syncSend(channel, okToAlarmEvent()).getCode(), equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void repeatedNotification() throws Exception {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Email to solomon-test-notification@yandex-team.ru")
                .setRecipient("solomon-test-notification@yandex-team.ru")
                .setSubjectTemplate("[{{status.code}}]: {{alert.id}}")
                .setContentTemplate("Hello world with status {{status.code}}!")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .setRepeatNotifyDelay(Duration.ofMillis(1L))
                .build());

        Instant now = Instant.now();
        Event alarm = okToAlarmEvent(randomActiveAlert(), now);
        assertThat(syncSend(channel, alarm, Instant.EPOCH).getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        Event stillAlarm = nextEvent(alarm, EvaluationStatus.ALARM);
        assertThat(syncSend(channel, stillAlarm, now).getCode(), equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void sendEventIfNotSendYetAboutItState() throws Exception {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Email to solomon-test-notification@yandex-team.ru")
                .setRecipient("solomon-test-notification@yandex-team.ru")
                .setSubjectTemplate("[{{status.code}}]: {{alert.id}}")
                .setContentTemplate("Hello world with status {{status.code}}!")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build());

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setNotificationChannel(channel.getId())
                .build();

        Instant latestSuccess = Instant.parse("2017-10-18T13:30:00Z");
        EvaluationState state = EvaluationState.newBuilder(alert)
                .setStatus(EvaluationStatus.ALARM)
                .setSince(Instant.parse("2017-10-18T13:40:00Z"))
                .setLatestEval(Instant.parse("2017-10-18T13:50:00Z"))
                .setPreviousStatus(EvaluationStatus.ALARM)
                .build();

        Event event = eval(alert, state);
        assertThat(syncSend(channel, event, latestSuccess).getCode(), equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void cancelAlsoCompleteActiveTasks() throws Exception {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Email to solomon-test-notification@yandex-team.ru")
                .setRecipient("solomon-test-notification@yandex-team.ru")
                .setSubjectTemplate("[{{status.code}}]: {{alert.id}}")
                .setContentTemplate("Hello world with status {{status.code}}!")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build());

        CompletableFuture<ListF<NotificationStatus>> result = CompletableFutures.allOf(IntStream.range(0, 100)
                .parallel()
                .mapToObj(ignore -> asyncSend(channel, okToAlarmEvent()))
                .collect(toList()));

        channel.close();
        assertThat(result.join(), iterableWithSize(100));
    }

    @Test
    public void detailsByStatus() throws Exception {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("@email")
                .setRecipient("solomon-alerts@yandex-team.ru")
                .setSubjectTemplate("[{{status.code}}]: {{alert.id}}")
                .setContentTemplate("{{{status.details}}}")
                .setNotifyAboutStatus(EvaluationStatus.Code.ERROR)
                .build());

        String details = Throwables.getStackTraceAsString(new Exception("Hi!"));
        Event event = eval(EvaluationStatus.ERROR.withDescription(details));

        NotificationStatus result = syncSend(channel, event);
        assertThat(result.getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        MimeMessage message = MailUtils.toMimeMessage(mailSender.receiveMessage());
        assertThat(message.getContent(), equalTo(details));
    }

    @Test
    public void annotations() throws Exception {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("@email")
                .setRecipient("solomon-alerts@yandex-team.ru")
                .setSubjectTemplate("{{{annotations.summary}}} {{{serviceProviderAnnotations.summary1}}}")
                .setContentTemplate("{{{annotations.details}}} {{{serviceProviderAnnotations.details1}}}")
                .setNotifyAboutStatus(EvaluationStatus.Code.OK)
                .build());

        Alert alert = AlertTestSupport.randomAlert(ThreadLocalRandom.current())
                .toBuilder()
                .build();
        Event event = eval(alert, EvaluationStatus.OK
                .withAnnotations(ImmutableMap.of(
                        "summary", "subject for email",
                        "details", "explain evaluation status"
                )).withServiceProviderAnnotations(ImmutableMap.of(
                        "summary1", "subject for email1",
                        "details1", "explain evaluation status1"
                )));

        NotificationStatus result = syncSend(channel, event);
        assertThat(result.getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        MimeMessage message = MailUtils.toMimeMessage(mailSender.receiveMessage());
        assertThat(message.getSubject(), equalTo("subject for email subject for email1"));
        assertThat(message.getContent(), equalTo("explain evaluation status explain evaluation status1"));
    }

    @Test
    public void skipRepeatOkStatuses() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("@email")
                .setRecipient("solomon-alerts@yandex-team.ru")
                .setSubjectTemplate("{{{annotations.summary}}} {{{serviceProviderAnnotations.summary1}}}")
                .setContentTemplate("{{{annotations.details}}} {{{serviceProviderAnnotations.details1}}}")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
                .setRepeatNotifyDelay(Duration.ofMinutes(30))
                .build());

        DispatchRule dispatchRule = channel.getDispatchRule(ChannelConfig.EMPTY);

        assertFalse(dispatchRule.isTimeToRepeat(EvaluationStatus.Code.ALARM, Duration.ofMinutes(10)));
        assertFalse(dispatchRule.isTimeToRepeat(EvaluationStatus.Code.ALARM, Duration.ofMinutes(20)));
        assertFalse(dispatchRule.isTimeToRepeat(EvaluationStatus.Code.OK, Duration.ofMinutes(20)));
        assertFalse(dispatchRule.isTimeToRepeat(EvaluationStatus.Code.OK, Duration.ofMinutes(34)));

        assertTrue(dispatchRule.isTimeToRepeat(EvaluationStatus.Code.ALARM, Duration.ofMinutes(34)));
    }

    @Test
    public void sendAllSubAlertsAsSingleEmail() throws IOException, MessagingException {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("my email")
                .setRecipient("solomon-test-notification@yandex-team.ru")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
                .build());

        Alert parent = randomAlert()
                .toBuilder()
                .setGroupByLabel("host")
                .build();

        List<CompletableFuture<NotificationStatus>> futures = IntStream.range(0, 10)
                .parallel()
                .mapToObj(index -> {
                    Labels labels = Labels.of("host", "my-host-" + index);
                    SubAlert subAlert = SubAlert.newBuilder()
                            .setId(MultiAlertUtils.getAlertId(parent, labels))
                            .setParent(parent)
                            .setGroupKey(labels)
                            .build();

                    return okToAlarmEvent(subAlert, Instant.now());
                })
                .map(event -> channel.send(Instant.EPOCH, event)
                        .thenApply(status -> {
                            if (status.getCode() != NotificationStatus.Code.SUCCESS) {
                                throw new IllegalStateException(status.toString());
                            }

                            return status;
                        }))
                .collect(Collectors.toList());

        // await complete batching
        clock.passedTime(1, TimeUnit.MINUTES);
        CompletableFutures.allOfVoid(futures).join();

        assertThat(mailSender.countMessageInSandbox(), equalTo(1));
        MimeMessage message = MailUtils.toMimeMessage(mailSender.receiveMessage());
        String content = (String) message.getContent();
        assertThat(content, allOf(
                containsString("my-host-1"),
                containsString("my-host-4"),
                containsString("my-host-9")));
    }

    @Test
    public void url() throws MessagingException, IOException {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("@email")
                .setRecipient("solomon-alerts@yandex-team.ru")
                .setSubjectTemplate("{{{annotations.summary}}}{{{serviceProviderAnnotations.summary}}}")
                .setContentTemplate("{{url}}")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
                .setRepeatNotifyDelay(Duration.ofMinutes(30))
                .build());

        Alert alert = AlertTestSupport.randomAlert(ThreadLocalRandom.current());

        Event event = eval(alert, EvaluationStatus.OK
                .withAnnotations(ImmutableMap.of(
                        "subject", "subject for email",
                        "details", "explain evaluation status"
                )));

        NotificationStatus result = syncSend(channel, event);
        assertThat(result.getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        MimeMessage message = MailUtils.toMimeMessage(mailSender.receiveMessage());
        assertThat(message.getContent(), equalTo("localhost/admin/projects/" + alert.getProjectId() + "/alerts/" + alert.getId()));
    }

    private NotificationStatus syncSend(NotificationChannel channel, Event event) {
        return asyncSend(channel, event).join();
    }

    private NotificationStatus syncSend(NotificationChannel channel, Event event, Instant success) {
        return asyncSend(channel, event, success).join();
    }

    private CompletableFuture<NotificationStatus> asyncSend(NotificationChannel channel, Event event) {
        return asyncSend(channel, event, Instant.EPOCH);
    }

    private CompletableFuture<NotificationStatus> asyncSend(
            NotificationChannel channel,
            Event event,
            Instant success)
    {
        return channel.send(success, event);
    }

    private EmailNotification.Builder newNotification() {
        return EmailNotification.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setProjectId("solomon");
    }

    private Event nextEvent(Event event, EvaluationStatus status) {
        EvaluationState state = event.getState();
        Instant nextTime = state.getLatestEval().plus(10, ChronoUnit.MINUTES);
        return eval(event.getAlert(), state.nextStatus(status, nextTime));
    }

    private Event okToAlarmEvent() {
        return okToAlarmEvent(randomActiveAlert());
    }

    private Event okToAlarmEvent(Alert alert) {
        return eval(alert, okToAlarmState(alert));
    }

    private Event okToAlarmEvent(Alert alert, Instant now) {
        return eval(alert, okToAlarmState(alert, now));
    }

    private EvaluationState okToAlarmState(Alert alert) {
        return okToAlarmState(alert, Instant.now());
    }

    private EvaluationState okToAlarmState(Alert alert, Instant time) {
        return EvaluationState.newBuilder(alert)
                .setStatus(EvaluationStatus.ALARM)
                .setSince(time)
                .setLatestEval(time)
                .setPreviousStatus(EvaluationStatus.OK)
                .build();
    }
}
