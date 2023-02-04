package ru.yandex.solomon.alert.notification.channel.phone;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.WillClose;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.jns.client.JnsClientStub;
import ru.yandex.jns.dto.JnsSendMessageRequest;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertKey;
import ru.yandex.solomon.alert.notification.channel.Event;
import ru.yandex.solomon.alert.notification.channel.NotificationChannel;
import ru.yandex.solomon.alert.notification.channel.NotificationChannelFactory;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.domain.PhoneNotification;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.config.protobuf.alert.JNSChannelConfig;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomActiveAlert;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.eval;

/**
 * @author Alexey Trushkin
 */
public class PhoneNotificationChannelTest {
    @Rule
    public Timeout globalTimeout = Timeout.builder()
            .withTimeout(30, TimeUnit.SECONDS)
            .withLookingForStuckThread(true)
            .build();

    private NotificationChannelFactory factory;
    @WillClose
    private JnsClientStub jnsClient;

    @Before
    public void setUp() throws Exception {
        jnsClient = new JnsClientStub();
        factory = new PhoneNotificationChannelFactory(
                jnsClient,
                JNSChannelConfig.newBuilder()
                        .setProject("solomon1")
                        .setTemplate("123")
                        .setDestinationTvmId(1234)
                        .build());
    }

    @After
    public void tearDown() throws Exception {
        jnsClient.close();
    }

    @Test
    public void sendAlertNotification() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Test Phone")
                .setLogin("alextrushkin")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.ERROR)
                .build());

        Event alarm = okToAlarmEvent();
        assertThat(syncSend(channel, alarm), equalTo(NotificationStatus.Code.SUCCESS));

        final JnsSendMessageRequest lastRequest = jnsClient.getLastRequest();
        assertRequest(lastRequest, "alextrushkin", alarm.getAlert());
    }

    @Test
    public void sendFailedAlertNotification() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Test Phone")
                .setLogin("alextrushkin")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.ERROR)
                .build());

        jnsClient.setFail(true);
        Event alarm = okToAlarmEvent();
        assertThat(syncSend(channel, alarm), equalTo(NotificationStatus.Code.ERROR));
    }

    @Test
    public void sendEventIfNotSendYetAboutItState() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Test Phone")
                .setLogin("alextrushkin")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.ERROR)
                .build());

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setProjectId("solomon1")
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
        assertThat(syncSend(channel, event, latestSuccess), equalTo(NotificationStatus.Code.SUCCESS));
    }

    private void assertRequest(JnsSendMessageRequest lastRequest, String login, Alert alert) {
        assertEquals("solomon1", lastRequest.projectWithTemplate);
        assertEquals("123", lastRequest.template);
        assertNull(lastRequest.projectAbcService);
        assertEquals(alert.getProjectId(), lastRequest.targetJnsProject);
        assertEquals(login, lastRequest.recipient.phone.internal.get(0).login);
        assertNotNull(lastRequest.idempotencyKey);
        assertNull(lastRequest.recipient.phone.internal.get(0).duty);
        assertEquals(lastRequest.params, Map.of(
                "alertName", Map.of("string_value", alert.getName()),
                "alertId", Map.of("string_value", alert.getId()),
                "subAlertLabels", Map.of("list_value", List.of()),
                "monitoringProject", Map.of("string_value", alert.getProjectId())
        ));
    }

    private Event okToAlarmEvent(Alert alert) {
        return okToAlarmEvent(alert, Instant.now());
    }

    private Event okToAlarmEvent(Alert alert, Instant now) {
        return eval(alert, okToAlarmState(alert.getId(), now));
    }

    private Event okToAlarmEvent() {
        return okToAlarmEvent(randomActiveAlert().toBuilder()
                .setProjectId("solomon1")
                .build());
    }

    private EvaluationState okToAlarmState(String alertId, Instant time) {
        return EvaluationState.newBuilder()
                .setAlertKey(new AlertKey("solomon1", "", alertId))
                .setAlertVersion(0)
                .setStatus(EvaluationStatus.ALARM)
                .setSince(time)
                .setLatestEval(time)
                .setPreviousStatus(EvaluationStatus.OK)
                .build();
    }

    private NotificationStatus.Code syncSend(NotificationChannel channel, Event event) {
        return syncSend(channel, event, Instant.EPOCH);
    }

    private NotificationStatus.Code syncSend(NotificationChannel channel, Event event, Instant success) {
        return channel.send(success, event).join().getCode();
    }

    private PhoneNotification.Builder newNotification() {
        return PhoneNotification.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setProjectId("solomon1");
    }
}

