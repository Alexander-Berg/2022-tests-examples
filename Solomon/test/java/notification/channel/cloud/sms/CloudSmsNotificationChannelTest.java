package ru.yandex.solomon.alert.notification.channel.cloud.sms;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import yandex.cloud.auth.api.Resource;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.notification.TemplateVarsFactory;
import ru.yandex.solomon.alert.notification.channel.Event;
import ru.yandex.solomon.alert.notification.channel.NotificationChannel;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.channel.cloud.CloudAuthClient;
import ru.yandex.solomon.alert.notification.channel.cloud.CloudAuthClientStub;
import ru.yandex.solomon.alert.notification.channel.cloud.NotifyClientStub;
import ru.yandex.solomon.alert.notification.domain.sms.CloudSmsNotification;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.auth.roles.Permission;
import ru.yandex.solomon.metrics.client.DcMetricsClient;
import ru.yandex.solomon.metrics.client.SolomonClientStub;

import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.misc.concurrent.CompletableFutures.join;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.eval;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class CloudSmsNotificationChannelTest {
    private NotifyClientStub notifyClient;
    private CloudAuthClient authClient;
    private SolomonClientStub solomon;
    private DcMetricsClient metricsClient;
    private UrlShortenerStub urlShortener;
    private CloudSmsNotificationChannelFactory factory;
    private CloudSmsNotification notification;
    private NotificationChannel notificationChannel;

    private static class UrlShortenerStub implements UrlShortener {
        private final Map<String, CompletableFuture<String>> predefined;

        public UrlShortenerStub() {
            predefined = new HashMap<>();
        }

        public void put(String longUrl, CompletableFuture<String> shortUrlFuture) {
            predefined.put(longUrl, shortUrlFuture);
        }

        @Override
        public CompletableFuture<String> shorten(String longUrl) {
            return predefined.getOrDefault(longUrl, failedFuture(new ArrayIndexOutOfBoundsException("No predefined value")));
        }
    }

    @Before
    public void setUp() {
        notifyClient = new NotifyClientStub();

        var iamStub = new CloudAuthClientStub();
        authClient = new CloudAuthClient(iamStub);

        iamStub.add("bfbldg1mouscbh8gcnmq", Permission.DATA_READ.getSlug(), Resource.folder("aoe5h5pn3otb41inm3tl"));

        solomon = new SolomonClientStub();
        metricsClient = new DcMetricsClient("test", solomon.getMetabase(), solomon.getStockpile());
        urlShortener = new UrlShortenerStub();
        var templateVarsFactory = new TemplateVarsFactory("localhost", "https://monitoring-preprod.cloud.yandex.ru");
        factory = new CloudSmsNotificationChannelFactory(notifyClient, authClient, urlShortener, templateVarsFactory);
        notification = CloudSmsNotification.newBuilder()
                .setRecipient("bfbldg1mouscbh8gcnmq")
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
                .setId("notification-channel")
                .setName("Random name")
                .setProjectId("aoecngvoh58bgtr3s25a")
                .setFolderId("aoe5h5pn3otb41inm3tl")
                .build();
        notificationChannel = factory.createChannel(notification);
    }

    @After
    public void tearDown() {
        solomon.close();
    }

    @Test
    public void testMessageWithAlarm() {
        ThresholdAlert alert = AlertTestSupport.randomCloudThresholdAlert().toBuilder()
                .setFolderId("aoe5h5pn3otb41inm3tl")
                .build();

        EvaluationState state = EvaluationState.newBuilder()
                .setAlertKey(alert.getKey())
                .setLatestEval(Instant.EPOCH)
                .setAlertVersion(alert.getVersion())
                .setSince(Instant.EPOCH)
                .setStatus(EvaluationStatus.ALARM)
                .build();

        Event event = eval(alert, state);

        NotificationStatus status = join(notificationChannel.send(Instant.EPOCH, event));
        assertThat(status.getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        var message = notifyClient.getOutboxSms(0);
        var payload = (CloudSmsNotificationChannel.Payload) message.data;
        assertThat(payload.alertStatus, equalTo("ALARM"));
        assertThat(payload.alertName, equalTo(alert.getName()));
        assertThat(payload.url, equalTo("https://monitoring-preprod.cloud.yandex.ru/aoe5h5pn3otb41inm3tl/alert/" + alert.getId() + "/view"));
        assertThat(message.transports, equalTo(List.of("sms")));
    }

    @Test
    public void testMessageWithUrl() {
        ThresholdAlert alert = AlertTestSupport.randomCloudThresholdAlert().toBuilder()
            .setFolderId("aoe5h5pn3otb41inm3tl")
            .build();

        EvaluationState state = EvaluationState.newBuilder()
            .setAlertKey(alert.getKey())
            .setLatestEval(Instant.EPOCH)
            .setAlertVersion(alert.getVersion())
            .setSince(Instant.EPOCH)
            .setStatus(EvaluationStatus.ALARM)
            .build();

        Event event = eval(alert, state);

        CompletableFuture<String> future = new CompletableFuture<>();
        urlShortener.put(
            "https://monitoring-preprod.cloud.yandex.ru/aoe5h5pn3otb41inm3tl/alert/" + alert.getId() + "/view",
            future.completeOnTimeout("https://ya.cc/foobar", 3, TimeUnit.SECONDS));

        NotificationStatus status = join(notificationChannel.send(Instant.EPOCH, event));
        assertThat(status.getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        var message = notifyClient.getOutboxSms(0);
        var payload = (CloudSmsNotificationChannel.Payload) message.data;
        assertThat(payload.alertStatus, equalTo("ALARM"));
        assertThat(payload.alertName, equalTo(alert.getName()));
        assertThat(payload.url, equalTo("https://ya.cc/foobar"));
        assertThat(message.transports, equalTo(List.of("sms")));
    }

    @Test
    public void unknownRecipientIsNotAnError() {
        ThresholdAlert alert = AlertTestSupport.randomCloudThresholdAlert().toBuilder()
                .setFolderId("aoe5h5pn3otb41inm3tl")
                .build();

        notification = CloudSmsNotification.newBuilder()
                .setRecipients(List.of("bfbldg1mouscbh8gcnmq", "foo"))
                .setNotifyAboutStatus(EvaluationStatus.Code.ALARM, EvaluationStatus.Code.OK)
                .setId("notification-channel")
                .setName("Random name")
                .setProjectId("aoecngvoh58bgtr3s25a")
                .setFolderId("aoe5h5pn3otb41inm3tl")
                .build();
        notificationChannel = factory.createChannel(notification);

        EvaluationState state = EvaluationState.newBuilder()
                .setAlertKey(alert.getKey())
                .setLatestEval(Instant.EPOCH)
                .setAlertVersion(alert.getVersion())
                .setSince(Instant.EPOCH)
                .setStatus(EvaluationStatus.ALARM)
                .build();

        Event event = eval(alert, state);

        CompletableFuture<String> future = new CompletableFuture<>();
        urlShortener.put(
                "https://monitoring-preprod.cloud.yandex.ru/aoe5h5pn3otb41inm3tl/alert/" + alert.getId() + "/view",
                future.completeOnTimeout("https://ya.cc/foobar", 3, TimeUnit.SECONDS));

        NotificationStatus status = join(notificationChannel.send(Instant.EPOCH, event));
        assertThat(status.getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        var message = notifyClient.getOutboxSms(0);
        var payload = (CloudSmsNotificationChannel.Payload) message.data;
        assertThat(payload.alertStatus, equalTo("ALARM"));
        assertThat(payload.alertName, equalTo(alert.getName()));
        assertThat(payload.url, equalTo("https://ya.cc/foobar"));
        assertThat(message.transports, equalTo(List.of("sms")));
    }
}
