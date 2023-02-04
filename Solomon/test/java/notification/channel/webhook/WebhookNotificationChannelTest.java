package ru.yandex.solomon.alert.notification.channel.webhook;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.WillClose;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpStatusCode;

import ru.yandex.misc.io.http.HttpHeaderNames;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertKey;
import ru.yandex.solomon.alert.domain.ChannelConfig;
import ru.yandex.solomon.alert.notification.DispatchRule;
import ru.yandex.solomon.alert.notification.TemplateVarsFactory;
import ru.yandex.solomon.alert.notification.channel.Event;
import ru.yandex.solomon.alert.notification.channel.NotificationChannel;
import ru.yandex.solomon.alert.notification.channel.NotificationChannelFactory;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.domain.webhook.WebhookNotification;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.alert.template.MustacheTemplateFactory;
import ru.yandex.solomon.alert.template.Template;
import ru.yandex.solomon.alert.template.TemplateFactory;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomActiveAlert;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.eval;

/**
 * @author Vladimir Gordiychuk
 */
public class WebhookNotificationChannelTest {
    @Rule
    public Timeout globalTimeout = Timeout.builder()
            .withTimeout(30, TimeUnit.SECONDS)
            .withLookingForStuckThread(true)
            .build();

    private NotificationChannelFactory factory;
    @WillClose
    private AsyncHttpClientStub httpClient;

    @Before
    public void setUp() throws Exception {
        httpClient = new AsyncHttpClientStub();
        TemplateFactory templateFactory = new MustacheTemplateFactory();

        Template defaultContent = templateFactory.createTemplate("{\"id\": \"{{alert.id}}\"}");
        factory = new WebhookNotificationChannelFactory(
                httpClient,
                templateFactory,
                defaultContent,
                new TemplateVarsFactory("localhost", "localhost"));
    }

    @After
    public void tearDown() throws Exception {
        httpClient.close();
    }

    @Test
    public void successSendWhenUseNotExistsVariable() throws Exception {
        String url = "http://localhost:8080/alert";
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Test webhook")
                .setUrl(url)
                .setTemplate("{\"alertId\": \"{{notExistsTag}}\", \"status\": \"{{status.code}}\", \"when\": \"{{since}}\"}")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.ERROR)
                .build());

        httpClient.when(request(url)).respond(response().withStatusCode(200));

        Event alarm = okToAlertEvent();
        assertThat(syncSend(channel, alarm), equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void sendEventIfNotSendYetAboutItState() throws Exception {
        String url = "http://localhost:8080/alert";
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Test webhook")
                .setUrl(url)
                .setTemplate("{\"id\": \"{{alert.id}}\"}")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build());

        httpClient.when(request(url)).respond(response().withStatusCode(200));

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
        assertThat(syncSend(channel, event, latestSuccess), equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void notify200() throws Exception {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Test webhook")
                .setUrl("http://localhost:8080/alert/200")
                .setTemplate("{\"alertId\": \"{{alert.id}}\", \"status\": \"{{status.code}}\", \"since\": \"{{since}}\"}")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.ERROR)
                .build());

        httpClient.when(request("http://localhost:8080/alert/200")
                .withMethod("POST")
                .withHeader("Content-Type", MediaType.JSON_UTF_8.toString())
                .withBody("{\"alertId\": \"alert-with-predefine-id\", \"status\": \"ALARM\", \"since\": \"2017-10-12T09:39:20.546Z\"}"))
                .respond(response()
                        .withStatusCode(HttpStatusCode.OK_200.code()));

        Instant now = Instant.parse("2017-10-12T09:39:20.546Z");

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setId("alert-with-predefine-id")
                .build();

        assertThat(syncSend(channel, okToAlertEvent(alert, now)), equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void overrideContentType() throws Exception {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Test webhook")
                .setUrl("http://localhost:8080/alert/200")
                .setTemplate("{\"alertId\": \"{{alert.id}}\", \"status\": \"{{status.code}}\", \"since\": \"{{since}}\"}")
                .setHeaders(Map.of("Content-Type", "application/json"))
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.ERROR)
                .build());

        httpClient.when(request("http://localhost:8080/alert/200")
                .withMethod("POST")
                .withHeader("Content-Type", "application/json")
                .withBody("{\"alertId\": \"alert-with-predefine-id\", \"status\": \"ALARM\", \"since\": \"2017-10-12T09:39:20.546Z\"}"))
                .respond(response()
                        .withStatusCode(HttpStatusCode.OK_200.code()));

        Instant now = Instant.parse("2017-10-12T09:39:20.546Z");

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setId("alert-with-predefine-id")
                .build();

        assertThat(syncSend(channel, okToAlertEvent(alert, now)), equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void annotations() {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Webhook with annotation")
                .setUrl("http://localhost:8080/alert/200")
                .setTemplate("{\"alertId\": \"{{alert.id}}\", \"value\": \"{{{annotations.value}}}\", \"value2\": \"{{{serviceProviderAnnotations.value+1}}}\"}")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.ERROR)
                .build());

        httpClient.when(request("http://localhost:8080/alert/200")
                .withMethod("POST")
                .withHeader("Content-Type", MediaType.JSON_UTF_8.toString())
                .withBody("{\"alertId\": \"myId\", \"value\": \"42\", \"value2\": \"43\"}"))
                .respond(response()
                        .withStatusCode(HttpStatusCode.OK_200.code()));

        Alert alert = randomActiveAlert()
                .toBuilder()
                .setId("myId")
                .build();

        NotificationStatus.Code result = syncSend(channel, eval(alert, EvaluationStatus.ALARM
                .withAnnotations(ImmutableMap.of(
                        "value", "42"
                )).withServiceProviderAnnotations(ImmutableMap.of(
                        "value+1", "43"
                ))));
        assertThat(result, equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void notify2xx() throws Exception {
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Test webhook")
                .setUrl("http://localhost:8080/alert/202")
                .setTemplate("{\"alertId\": \"{{alert.id}}\", \"status\": \"{{status.code}}\", \"since\": \"{{since}}\"}")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.ERROR)
                .build());

        httpClient.when(request("http://localhost:8080/alert/202")
                .withMethod("POST")
                .withHeader("Content-Type", MediaType.JSON_UTF_8.toString()))
                .respond(response()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code()));

        assertThat("2xx statuses means success for notification via webhook",
                syncSend(channel, okToAlertEvent()),
                equalTo(NotificationStatus.Code.SUCCESS)
        );
    }

    @Test
    public void notify4xx() throws Exception {
        String contentTemplate = "{\"id\": \"{{alert.id}}\"}";
        String url400 = "http://localhost:8080/alert/400";
        String url404 = "http://localhost:8080/alert/404";

        NotificationChannel ch400 = factory.createChannel(newNotification()
                .setName("Test webhook 400")
                .setUrl(url400)
                .setTemplate(contentTemplate)
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build());

        NotificationChannel ch404 = factory.createChannel(newNotification()
                .setName("Test webhook 404")
                .setUrl(url404)
                .setTemplate(contentTemplate)
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build());

        httpClient.when(request(url400)).respond(response().withStatusCode(400));
        httpClient.when(request(url400)).respond(response().withStatusCode(200));
        httpClient.when(request(url404)).respond(response().withStatusCode(404));

        Event alarm = okToAlertEvent();
        assertThat(syncSend(ch400, alarm), equalTo(NotificationStatus.Code.INVALID_REQUEST));
        assertThat(syncSend(ch404, alarm), equalTo(NotificationStatus.Code.INVALID_REQUEST));
    }

    @Test
    public void notify5xx() throws Exception {
        String url = "http://localhost:8080/alert";

        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Test webhook 503")
                .setUrl(url)
                .setTemplate("{\"id\": \"{{alert.id}}\"}")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build());

        // Twice will be reject for request
        httpClient.when(request(url), Times.once()).respond(response().withStatusCode(500));
        httpClient.when(request(url), Times.once()).respond(response().withStatusCode(503));
        httpClient.when(request(url), Times.once()).respond(response().withStatusCode(200));

        assertThat(syncSend(channel, okToAlertEvent()), equalTo(NotificationStatus.Code.ERROR_ABLE_TO_RETRY));
        assertThat(syncSend(channel, okToAlertEvent()), equalTo(NotificationStatus.Code.ERROR_ABLE_TO_RETRY));
        assertThat(syncSend(channel, okToAlertEvent()), equalTo(NotificationStatus.Code.SUCCESS));
    }

    @Test
    public void notify5xxFriendlyRetryDelayAsSec() throws Exception {
        String url = "http://localhost:8080/alert";

        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Test webhook 503")
                .setUrl(url)
                .setTemplate("{\"id\": \"{{alert.id}}\"}")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build());

        // Friendly ask retry after 3 seconds
        httpClient.when(request(url))
                .respond(response()
                        .withStatusCode(503)
                        .withHeaders(header(HttpHeaderNames.RETRY_AFTER, 3)));

        NotificationStatus result = channel.send(Instant.EPOCH, okToAlertEvent()).join();
        assertThat(result.getCode(), equalTo(NotificationStatus.Code.ERROR_ABLE_TO_RETRY));
        assertThat("Retry-After header allow not overload target server and webhook should " +
                        "consider it header and retry only after specified " +
                        "time if alert will still actual",
                result.getRetryAfterMillisHint(), equalTo(TimeUnit.SECONDS.toMillis(3))
        );
    }

    @Test
    public void notify5xxFriendlyRetryDelayToAvailabilityDay() throws Exception {
        String url = "http://localhost:8080/alert";
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Test webhook 503")
                .setUrl(url)
                .setTemplate("{\"id\": \"{{alert.id}}\"}")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .build());

        final ZonedDateTime after30seconds = ZonedDateTime.now().plusSeconds(30);
        // Friendly ask retry after 30 seconds
        httpClient.when(request(url))
                .respond(response()
                        .withStatusCode(503)
                        .withHeaders(header(HttpHeaderNames.RETRY_AFTER, DateTimeFormatter.RFC_1123_DATE_TIME.format(after30seconds))));

        NotificationStatus status = channel.send(Instant.EPOCH, okToAlertEvent()).join();
        assertThat(status.getCode(), equalTo(NotificationStatus.Code.ERROR_ABLE_TO_RETRY));
        assertThat("Retry-After header allow not overload target server and webhook should " +
                        "consider it header and retry only after specified " +
                        "time if alert will still actual",
                status.getRetryAfterMillisHint(),
                allOf(
                        greaterThanOrEqualTo(TimeUnit.SECONDS.toMillis(29)),
                        lessThanOrEqualTo(TimeUnit.SECONDS.toMillis(30))
                )
        );
    }

    @Test
    public void repeatNotificationIfDelayPassed() {
        String url = "http://localhost:8080/alert";
        NotificationChannel channel = factory.createChannel(newNotification()
                .setName("Test webhook")
                .setUrl(url)
                .setTemplate("{\"id\": \"{{alert.id}}\"}")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM)
                .setRepeatNotifyDelay(Duration.ofMinutes(2))
                .build());

        httpClient.when(request(url)).respond(response().withStatusCode(200));

        DispatchRule dispatchRule = channel.getDispatchRule(ChannelConfig.EMPTY);

        assertThat(dispatchRule.isTimeToRepeat(EvaluationStatus.Code.ALARM, Duration.ofMinutes(1)), equalTo(false));
        assertThat(dispatchRule.isTimeToRepeat(EvaluationStatus.Code.ALARM, Duration.ofMinutes(3)), equalTo(true));

        Instant now = Instant.now();
        Event alarm = okToAlertEvent(randomActiveAlert(), now);
        assertThat(syncSend(channel, alarm, Instant.EPOCH), equalTo(NotificationStatus.Code.SUCCESS));

        Event stillAlarm = nextEvent(alarm, EvaluationStatus.ALARM);
        assertThat(syncSend(channel, stillAlarm, now), equalTo(NotificationStatus.Code.SUCCESS));
    }

    private Event okToAlertEvent(Alert alert) {
        return okToAlertEvent(alert, Instant.now());
    }

    private Event okToAlertEvent(Alert alert, Instant now) {
        return eval(alert, okToAlarmState(alert.getId(), now));
    }

    private Event okToAlertEvent() {
        return okToAlertEvent(randomActiveAlert());
    }

    private EvaluationState okToAlarmState(String alertId, Instant time) {
        return EvaluationState.newBuilder()
                .setAlertKey(new AlertKey("junk", "", alertId))
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

    private WebhookNotification.Builder newNotification() {
        return WebhookNotification.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setProjectId("junk");
    }

    private Event nextEvent(Event event, EvaluationStatus status) {
        EvaluationState prevStatus = event.getState();
        Instant nextTime = prevStatus.getLatestEval().plus(10, ChronoUnit.MINUTES);
        EvaluationState nextStatus = prevStatus.nextStatus(status, nextTime);
        return eval(event.getAlert(), nextStatus);
    }
}
