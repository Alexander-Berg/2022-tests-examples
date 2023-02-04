package ru.yandex.solomon.alert.notification.channel.cloud.push;

import java.time.Instant;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import yandex.cloud.auth.api.Resource;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.domain.threshold.ThresholdAlert;
import ru.yandex.solomon.alert.notification.channel.Event;
import ru.yandex.solomon.alert.notification.channel.NotificationChannel;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.channel.cloud.CloudAuthClient;
import ru.yandex.solomon.alert.notification.channel.cloud.CloudAuthClientStub;
import ru.yandex.solomon.alert.notification.channel.cloud.NotifyClientStub;
import ru.yandex.solomon.alert.notification.domain.push.CloudPushNotification;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.auth.roles.Permission;
import ru.yandex.solomon.metrics.client.SolomonClientStub;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.misc.concurrent.CompletableFutures.join;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.eval;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class CloudPushNotificationChannelTest {
    private NotifyClientStub notifyClient;
    private CloudAuthClient authClient;
    private SolomonClientStub solomon;
    private CloudPushNotificationChannelFactory factory;
    private CloudPushNotification notification;
    private NotificationChannel notificationChannel;

    @Before
    public void setUp() {
        notifyClient = new NotifyClientStub();

        var iamStub = new CloudAuthClientStub();
        authClient = new CloudAuthClient(iamStub);

        iamStub.add("bfbldg1mouscbh8gcnmq", Permission.DATA_READ.getSlug(), Resource.folder("aoe5h5pn3otb41inm3tl"));

        solomon = new SolomonClientStub();
        factory = new CloudPushNotificationChannelFactory(notifyClient, authClient);
        notification = CloudPushNotification.newBuilder()
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

        var message = notifyClient.getOutboxPush(0);
        var payload = (CloudPushNotificationChannel.Payload) message.data;
        assertThat(payload.alertStatus, equalTo("ALARM"));
        assertThat(payload.alertName, equalTo(alert.getName()));
        assertThat(payload.alertId, equalTo(alert.getId()));
        assertThat(payload.folderId, equalTo(alert.getFolderId()));
        assertThat(message.transports, equalTo(List.of("web")));
    }

    @Test
    public void unknownRecipientIsNotAnError() {
        ThresholdAlert alert = AlertTestSupport.randomCloudThresholdAlert().toBuilder()
                .setFolderId("aoe5h5pn3otb41inm3tl")
                .build();

        notification = CloudPushNotification.newBuilder()
                .setRecipients(List.of("bfbldg1mouscbh8gcnmq", "bar"))
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

        NotificationStatus status = join(notificationChannel.send(Instant.EPOCH, event));
        assertThat(status.getCode(), equalTo(NotificationStatus.Code.SUCCESS));

        var message = notifyClient.getOutboxPush(0);
        var payload = (CloudPushNotificationChannel.Payload) message.data;
        assertThat(payload.alertStatus, equalTo("ALARM"));
        assertThat(payload.alertName, equalTo(alert.getName()));
        assertThat(payload.alertId, equalTo(alert.getId()));
        assertThat(payload.folderId, equalTo(alert.getFolderId()));
        assertThat(message.transports, equalTo(List.of("web")));
    }

}
