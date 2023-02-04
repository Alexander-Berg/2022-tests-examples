package ru.yandex.solomon.alert.notification.channel.juggler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.juggler.dto.EventStatus;
import ru.yandex.juggler.dto.JugglerEvent;
import ru.yandex.juggler.dto.JugglerEvent.Status;
import ru.yandex.juggler.validation.JugglerValidator;
import ru.yandex.misc.io.http.HttpStatus;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.ChannelConfig;
import ru.yandex.solomon.alert.mute.domain.MuteStatus;
import ru.yandex.solomon.alert.notification.DispatchRule;
import ru.yandex.solomon.alert.notification.TemplateVarsFactory;
import ru.yandex.solomon.alert.notification.channel.Event;
import ru.yandex.solomon.alert.notification.channel.NotificationChannel;
import ru.yandex.solomon.alert.notification.channel.NotificationChannelFactory;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.domain.juggler.JugglerNotification;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.ut.ManualClock;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomActiveAlert;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.eval;
import static ru.yandex.solomon.alert.notification.domain.NotificationTestSupport.randomJugglerNotification;

/**
 * @author Vladimir Gordiychuk
 */
public class JugglerNotificationChannelTest {

    private ManualClock clock;
    private JugglerClientStub client;
    private NotificationChannelFactory factory;

    @Before
    public void setUp() throws Exception {
        clock = new ManualClock();
        client = new JugglerClientStub();
        MustacheTemplateFactory templateFactory = new MustacheTemplateFactory();
        factory = new JugglerNotificationChannelFactory(clock, client, templateFactory, new TemplateVarsFactory("localhost", "localhost"));
    }

    @Test
    public void defaults() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("juggler notification with defaults")
                .setNotifyAboutStatus(EnumSet.allOf(EvaluationStatus.Code.class))
                .build());

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setId("my-first-alert")
                .build();

        Event alarm = okToAlarmEvent(alert);
        assertThat(syncSend(channel, alarm).getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        JugglerEvent jugglerEvent = client.getLatestEvent();
        assertThat(jugglerEvent.host, equalTo("solomon-alert"));
        assertThat(jugglerEvent.service, equalTo(alert.getId()));
        assertThat(jugglerEvent.instance, isEmptyString());
        assertThat(jugglerEvent.status, equalTo(Status.CRIT));
    }

    @Test
    public void retryError() {
        NotificationChannel channel = factory.createChannel(randomJugglerNotification(ThreadLocalRandom.current()));

        Event alarm = okToAlarmEvent(randomActiveAlert());
        int code = ThreadLocalRandom.current().nextInt(500, 600);
        client.setStatus(new EventStatus("Unknown server error", code));

        assertThat(syncSend(channel, alarm).getCode(), equalTo(NotificationStatus.Code.ERROR_ABLE_TO_RETRY));
    }

    @Test
    public void notValidParamsError() {
        NotificationChannel channel = factory.createChannel(randomJugglerNotification(ThreadLocalRandom.current()));

        Event alarm = okToAlarmEvent(randomActiveAlert());
        client.setStatus(new EventStatus("Unknown client error", 400));

        assertThat(syncSend(channel, alarm).getCode(), equalTo(NotificationStatus.Code.INVALID_REQUEST));
    }

    @Test
    public void error() {
        NotificationChannel channel = factory.createChannel(randomJugglerNotification(ThreadLocalRandom.current()));

        Event alarm = okToAlarmEvent(randomActiveAlert());
        client.setStatus(new EventStatus("Unknown client error", HttpStatus.SC_305_USE_PROXY));

        assertThat(syncSend(channel, alarm).getCode(), equalTo(NotificationStatus.Code.ERROR));
    }

    @Test
    public void sendEachEvent() {
        NotificationChannel channel = factory.createChannel(randomJugglerNotification(ThreadLocalRandom.current()));

        Event alarm = okToAlarmEvent(randomActiveAlert());
        Event stillAlarm = okToAlarmEvent(randomActiveAlert());

        assertThat(syncSend(channel, alarm, Instant.now()).getCode(), equalTo(NotificationStatus.Code.SUCCESS));
        assertThat(syncSend(channel, stillAlarm, Instant.now()).getCode(), equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void customHostTemplate() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("test channel")
                .setHost("custom-{{alert.id}}-host")
                .build());

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setId("TEST")
                .build();

        Event alarm = okToAlarmEvent(alert);
        syncSend(channel, alarm, Instant.now());
        JugglerEvent jugglerEvent = client.getLatestEvent();
        assertThat(jugglerEvent.host, equalTo("custom-TEST-host"));
    }

    @Test
    public void customServiceTemplate() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("test channel")
                .setService("my-custom-{{alert.name}}-test")
                .build());

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setName("bestAlert")
                .build();

        Event alarm = okToAlarmEvent(alert);
        syncSend(channel, alarm, Instant.now());
        JugglerEvent jugglerEvent = client.getLatestEvent();
        assertThat(jugglerEvent.service, equalTo("my-custom-bestAlert-test"));
    }

    @Test
    public void customInstanceTemplate() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("test channel")
                .setInstance("inst-{{alert.id}}")
                .build());

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setName("bestAlert")
                .build();

        Event alarm = okToAlarmEvent(alert);
        syncSend(channel, alarm, Instant.now());
        JugglerEvent jugglerEvent = client.getLatestEvent();
        assertThat(jugglerEvent.instance, equalTo("inst-" + alert.getId()));
    }

    @Test
    public void customTags() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("test channel")
                .setTags(Arrays.asList("{{alert.name}}", "{{alert.id}}", "customConstantTag"))
                .build());

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setId("myTestId")
                .setName("myTestName")
                .build();

        Event alarm = okToAlarmEvent(alert);
        syncSend(channel, alarm, Instant.now());
        JugglerEvent jugglerEvent = client.getLatestEvent();
        assertThat(jugglerEvent.tags, allOf(hasItem("myTestId"), hasItem("myTestName"), hasItem("customConstantTag")));
    }

    @Test
    public void customDescription() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("test channel")
                .setJugglerDescription("custom description: {{alert.id}} -> {{alert.name}}")
                .build());

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setId("myTestId")
                .setName("myTestName")
                .build();

        Event alarm = okToAlarmEvent(alert);
        syncSend(channel, alarm, Instant.now());
        JugglerEvent jugglerEvent = client.getLatestEvent();
        assertThat(jugglerEvent.description, equalTo("custom description: myTestId -> myTestName"));
    }

    @Test
    public void doNotSendObsoleteEvents() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("test channel")
                .setJugglerDescription("custom description: {{alert.id}} -> {{alert.name}}")
                .build());

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setId("mytestId")
                .setName("testName")
                .build();

        Event alarm = okToAlarmEvent(alert);
        Instant latestSuccess = clock.instant().minus(1, ChronoUnit.MINUTES);

        clock.passedTime(10, TimeUnit.MINUTES);
        NotificationStatus status = syncSend(channel, alarm, latestSuccess);
        assertThat(status.getCode(), equalTo(NotificationStatus.Code.OBSOLETE));

        JugglerEvent jugglerEvent = client.getLatestEvent();
        assertThat(jugglerEvent, nullValue());
    }

    @Test
    public void annotations() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("juggler@")
                .setJugglerDescription("{{{annotations.summary}}} {{{serviceProviderAnnotations.summary1}}}")
                .build());

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setId("myTestId")
                .setName("myTestName")
                .build();

        Event alarm = eval(alert, EvaluationStatus.ALARM
                .withAnnotations(ImmutableMap.of(
                        "summary", "It's my custom summary for particular alert"
                )).withServiceProviderAnnotations(ImmutableMap.of(
                        "summary1", "It's my custom summary for particular alert"
                )));
        NotificationStatus result = syncSend(channel, alarm, Instant.now());
        assertThat(result.getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        JugglerEvent jugglerEvent = client.getLatestEvent();
        assertThat(jugglerEvent.description, equalTo("It's my custom summary for particular alert It's my custom summary for particular alert"));
    }

    @Test
    public void repeatEveryEventToJuggler() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("juggler notification with defaults")
                .setNotifyAboutStatus(EnumSet.allOf(EvaluationStatus.Code.class))
                .build());

        DispatchRule dispatchRule = channel.getDispatchRule(ChannelConfig.EMPTY);

        for (EvaluationStatus.Code code : EvaluationStatus.Code.values()) {
            for (int index = 0; index < 10; index++) {
                assertTrue(dispatchRule.isTimeToRepeat(code, Duration.ofMinutes(index)));
            }
        }
    }

    @Test
    public void multipleTagsIntoAnnotation() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("juggler@")
                .setTag("{{annotations.tags}}")
                .build());

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setId("myTestId")
                .setName("myTestName")
                .build();

        Event alarm = eval(alert, EvaluationStatus.ALARM
                .withAnnotations(ImmutableMap.of("tags", "t1, t2, t5")));
        NotificationStatus result = syncSend(channel, alarm, Instant.now());
        assertThat(result.getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        JugglerEvent jugglerEvent = client.getLatestEvent();
        assertThat(jugglerEvent.tags, hasItems("t1", "t2", "t5"));
    }

    @Test
    public void invalidCharsInTags() {
        NotificationChannel channel = factory.createChannel(newNotification()
            .setName("juggler@")
            .setTag("/home/solomon/example-ru")
            .build());

        Alert alert = randomActiveAlert()
            .toBuilder()
            .setId("myTestId")
            .setName("myTestName")
            .build();

        Event alarm = eval(alert, EvaluationStatus.ALARM);
        NotificationStatus result = syncSend(channel, alarm, Instant.now());
        assertThat(result.getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        JugglerEvent jugglerEvent = client.getLatestEvent();
        assertTrue(jugglerEvent.tags.stream().allMatch(JugglerValidator::isValidTagName));
    }

    @Test
    public void invalidChars() {
        var channel = factory.createChannel(
            newNotification()
                .setName("juggler@")
                .setService("$erv1ce")
                .setInstance("!a@b#c%")
                .build());

        var alert = randomActiveAlert()
            .toBuilder()
            .setId("myTestId")
            .setName("myTestName")
            .build();

        var alarm = eval(alert, EvaluationStatus.ALARM);
        var result = syncSend(channel, alarm, Instant.now());
        assertThat(result.getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        var jugglerEvent = client.getLatestEvent();

        var validation = JugglerValidator.validateEvent(jugglerEvent);
        assertNull(prettify(validation), validation);
    }

    @Test
    public void trafficLightHack() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("juggler@")
                .setTag("{{annotations.tags}}")
                .build());

        syncSend(channel, eval(EvaluationStatus.OK
                .withAnnotations(ImmutableMap.of("trafficLight.color", "yellow"))));
        assertThat(client.getLatestEvent().status, equalTo(Status.WARN));

        syncSend(channel, eval(EvaluationStatus.OK
                .withAnnotations(ImmutableMap.of("trafficLight.color", "red"))));
        assertThat(client.getLatestEvent().status, equalTo(Status.CRIT));

        syncSend(channel, eval(EvaluationStatus.OK
                .withAnnotations(ImmutableMap.of("trafficLight.color", "green"))));
        assertThat(client.getLatestEvent().status, equalTo(Status.OK));
    }

    @Test
    public void trafficLightHackMuted() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("juggler@")
                .setTag("{{annotations.tags}}")
                .build());

        syncSend(channel, eval(EvaluationStatus.OK
                .withAnnotations(ImmutableMap.of("trafficLight.color", "yellow")), MuteStatus.PENDING));
        assertThat(client.getLatestEvent().status, equalTo(Status.WARN));

        syncSend(channel, eval(EvaluationStatus.OK
                .withAnnotations(ImmutableMap.of("trafficLight.color", "red")), MuteStatus.ACTIVE));
        assertThat(client.getLatestEvent().status, equalTo(Status.OK));

        syncSend(channel, eval(EvaluationStatus.OK
                .withAnnotations(ImmutableMap.of("trafficLight.color", "green")), MuteStatus.EXPIRED));
        assertThat(client.getLatestEvent().status, equalTo(Status.OK));
    }

    @Test
    public void jugglerStatusMapping() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("juggler@")
                .setTag("{{annotations.tags}}")
                .build());

        syncSend(channel, eval(EvaluationStatus.OK));
        assertThat(client.getLatestEvent().status, equalTo(Status.OK));

        syncSend(channel, eval(EvaluationStatus.WARN));
        assertThat(client.getLatestEvent().status, equalTo(Status.WARN));

        syncSend(channel, eval(EvaluationStatus.ALARM));
        assertThat(client.getLatestEvent().status, equalTo(Status.CRIT));

        syncSend(channel, eval(EvaluationStatus.DEADLINE));
        assertThat(client.getLatestEvent().status, equalTo(Status.WARN));

        syncSend(channel, eval(EvaluationStatus.ERROR));
        assertThat(client.getLatestEvent().status, equalTo(Status.WARN));

        syncSend(channel, eval(EvaluationStatus.NO_DATA));
        assertThat(client.getLatestEvent().status, equalTo(Status.INFO));
    }

    @Test
    public void jugglerMuteForceOk() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("juggler@")
                .setTag("{{annotations.tags}}")
                .build());

        syncSend(channel, eval(EvaluationStatus.OK, MuteStatus.PENDING));
        assertThat(client.getLatestEvent().status, equalTo(Status.OK));

        syncSend(channel, eval(EvaluationStatus.WARN, MuteStatus.PENDING));
        assertThat(client.getLatestEvent().status, equalTo(Status.WARN));

        syncSend(channel, eval(EvaluationStatus.ALARM, MuteStatus.ACTIVE));
        assertThat(client.getLatestEvent().status, equalTo(Status.OK));

        syncSend(channel, eval(EvaluationStatus.DEADLINE, MuteStatus.ACTIVE));
        assertThat(client.getLatestEvent().status, equalTo(Status.OK));

        syncSend(channel, eval(EvaluationStatus.ERROR, MuteStatus.EXPIRED));
        assertThat(client.getLatestEvent().status, equalTo(Status.WARN));

        syncSend(channel, eval(EvaluationStatus.NO_DATA, MuteStatus.EXPIRED));
        assertThat(client.getLatestEvent().status, equalTo(Status.INFO));
    }

    private NotificationStatus syncSend(NotificationChannel channel, Event event) {
        return channel.send(Instant.EPOCH, event).join();
    }

    private NotificationStatus syncSend(NotificationChannel channel, Event event, Instant latestSuccess) {
        return channel.send(latestSuccess, event).join();
    }

    private JugglerNotification.Builder newNotification() {
        return JugglerNotification.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setProjectId("kikimr")
                .setNotifyAboutStatus(EnumSet.allOf(EvaluationStatus.Code.class));
    }

    private Event okToAlarmEvent(Alert alert) {
        return eval(alert, okToAlarmState(alert));
    }

    private EvaluationState okToAlarmState(Alert alert) {
        return okToAlarmState(alert, clock.instant());
    }

    private EvaluationState okToAlarmState(Alert alert, Instant time) {
        return EvaluationState.newBuilder(alert)
                .setStatus(EvaluationStatus.ALARM)
                .setSince(time)
                .setLatestEval(time)
                .setPreviousStatus(EvaluationStatus.OK)
                .build();
    }

    private static String prettify(EventStatus validation)  {
        try {
            return new ObjectMapper().writeValueAsString(validation);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }
}
