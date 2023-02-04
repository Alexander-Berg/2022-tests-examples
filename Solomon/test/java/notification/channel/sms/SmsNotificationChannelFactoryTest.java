package ru.yandex.solomon.alert.notification.channel.sms;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.SubAlert;
import ru.yandex.solomon.alert.notification.TemplateVarsFactory;
import ru.yandex.solomon.alert.notification.channel.Event;
import ru.yandex.solomon.alert.notification.channel.NotificationChannel;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.channel.email.MailTransportStub;
import ru.yandex.solomon.alert.notification.channel.sms.SmsClientStub.Message;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.unroll.MultiAlertUtils;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlert;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.eval;
import static ru.yandex.solomon.alert.notification.domain.NotificationTestSupport.randomSmsNotification;

/**
 * @author Vladimir Gordiychuk
 */
public class SmsNotificationChannelFactoryTest {
    @Rule
    public Timeout globalTimeout = Timeout.builder()
            .withTimeout(3, TimeUnit.SECONDS)
            .withLookingForStuckThread(true)
            .build();

    private SmsNotificationChannelFactory factory;
    private MailTransportStub mailSender;
    private ManualClock clock;
    private ManualScheduledExecutorService executorService;
    private SmsClientStub smsClient;

    @Before
    public void setUp() throws Exception {
        MustacheTemplateFactory templateFactory = new MustacheTemplateFactory();

        mailSender = new MailTransportStub();
        clock = new ManualClock();
        executorService = new ManualScheduledExecutorService(2, clock);
        smsClient = new SmsClientStub();
        factory = new SmsNotificationChannelFactory(
                smsClient,
                executorService,
                templateFactory,
                SmsTemplate.create(templateFactory, new TemplateVarsFactory("localhost", "localhost")),
                10);
    }

    @After
    public void tearDown() throws Exception {
        executorService.shutdownNow();
    }

    @Test
    public void defaultTemplate() {
        NotificationChannel channel = factory.createChannel(randomSmsNotification(ThreadLocalRandom.current())
                .toBuilder()
                .setTextTemplate("")
                .setPhone("+71234567890")
                .build());

        Event event = eval(randomAlert(), EvaluationStatus.Code.ALARM);
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();

        assertThat(status.getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        assertThat(smsClient.countMessageInSandbox(), equalTo(1));

        Message message = smsClient.receiveMessage();
        assertThat(message.getPhone(), equalTo("+71234567890"));
        assertThat(message.getText(), containsString(event.getAlert().getName()));
        assertThat(message.getText(), containsString("ALARM"));
    }

    @Test
    public void sendByLogin() {
        NotificationChannel channel = factory.createChannel(randomSmsNotification(ThreadLocalRandom.current())
                .toBuilder()
                .setTextTemplate("")
                .setPhone("")
                .setLogin("alice")
                .build());

        Event event = eval(randomAlert(), EvaluationStatus.Code.ALARM);
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();

        assertThat(status.getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        assertThat(smsClient.countMessageInSandbox(), equalTo(1));

        Message message = smsClient.receiveMessage();
        assertThat(message.getLogin(), equalTo("alice"));
        assertThat(message.getText(), containsString(event.getAlert().getName()));
        assertThat(message.getText(), containsString("ALARM"));
    }

    @Test
    public void customTemplate() {
        NotificationChannel channel = factory.createChannel(randomSmsNotification(ThreadLocalRandom.current())
                .toBuilder()
                .setTextTemplate("{{alert.id}} in state {{status.code}}")
                .setPhone("+72234567890")
                .build());

        Event event = eval(randomAlert(), EvaluationStatus.Code.ERROR);
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();

        assertThat(status.getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        assertThat(smsClient.countMessageInSandbox(), equalTo(1));

        Message message = smsClient.receiveMessage();
        assertThat(message.getPhone(), equalTo("+72234567890"));
        assertThat(message.getText(), equalTo(event.getAlert().getId() + " in state ERROR"));
    }

    @Test
    public void defaultMultiAlertTemplate() {
        NotificationChannel channel = factory.createChannel(randomSmsNotification(ThreadLocalRandom.current())
                .toBuilder()
                .setTextTemplate("")
                .setPhone("+72224567890")
                .build());

        Alert parent = randomAlert().toBuilder()
                .setName("My alert")
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

                    return eval(subAlert, EvaluationStatus.ALARM);
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

        assertThat(smsClient.countMessageInSandbox(), equalTo(1));

        Message message = smsClient.receiveMessage();
        assertThat(message.getPhone(), equalTo("+72224567890"));
        assertThat(message.getText(), containsString(parent.getName()));
        assertThat(message.getText(), containsString("ALARM"));
    }

    @Test
    public void limitedTextSize() {
        char[] hugeTemplate = new char[500];
        Arrays.fill(hugeTemplate, 'q');

        NotificationChannel channel = factory.createChannel(randomSmsNotification(ThreadLocalRandom.current())
                .toBuilder()
                .setTextTemplate(new String(hugeTemplate))
                .setPhone("+71234567890")
                .build());

        Event event = eval(randomAlert(), EvaluationStatus.Code.ALARM);
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();

        assertThat(status.getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        assertThat(smsClient.countMessageInSandbox(), equalTo(1));

        Message message = smsClient.receiveMessage();
        assertThat(message.getPhone(), equalTo("+71234567890"));
        assertThat(message.getText(), equalTo(new String(hugeTemplate, 0, 399) + "..."));
    }

    @Test
    public void limitSmsCount() {
        NotificationChannel channel = factory.createChannel(randomSmsNotification(ThreadLocalRandom.current())
                .toBuilder()
                .setTextTemplate("")
                .setPhone("+71234567890")
                .build());

        for (int index = 0; index < 10; index++) {
            Event event = eval(randomAlert(), EvaluationStatus.Code.ALARM);
            NotificationStatus status = channel.send(Instant.EPOCH, event).join();
            assertThat(status.getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        }

        Event event = eval(randomAlert(), EvaluationStatus.Code.ALARM);
        NotificationStatus status = channel.send(Instant.EPOCH, event).join();
        assertThat(status.getCode(), equalTo(NotificationStatus.Code.RESOURCE_EXHAUSTED));
        assertThat(smsClient.countMessageInSandbox(), equalTo(10));
    }
}
