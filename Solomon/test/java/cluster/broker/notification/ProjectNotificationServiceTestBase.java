package ru.yandex.solomon.alert.cluster.broker.notification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Iterables;

import ru.yandex.jns.client.JnsClientStub;
import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.api.converters.NotificationConverter;
import ru.yandex.solomon.alert.cluster.broker.notification.search.NotificationSearch;
import ru.yandex.solomon.alert.dao.EntitiesDao;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.notification.RetryOptions;
import ru.yandex.solomon.alert.notification.channel.NotificationChannelStubFactory;
import ru.yandex.solomon.alert.notification.domain.Notification;
import ru.yandex.solomon.alert.notification.domain.NotificationTestSupport;
import ru.yandex.solomon.alert.notification.state.StatefulNotificationChannel;
import ru.yandex.solomon.alert.notification.state.StatefulNotificationChannelFactoryImpl;
import ru.yandex.solomon.alert.protobuf.ERequestStatusCode;
import ru.yandex.solomon.alert.protobuf.TCreateNotificationRequest;
import ru.yandex.solomon.alert.protobuf.TCreateNotificationResponse;
import ru.yandex.solomon.alert.protobuf.TDeleteNotificationRequest;
import ru.yandex.solomon.alert.protobuf.TDeleteNotificationResponse;
import ru.yandex.solomon.alert.protobuf.TListNotificationsRequest;
import ru.yandex.solomon.alert.protobuf.TListNotificationsResponse;
import ru.yandex.solomon.alert.protobuf.TReadNotificationRequest;
import ru.yandex.solomon.alert.protobuf.TReadNotificationResponse;
import ru.yandex.solomon.alert.protobuf.TResolveNotificationDetailsRequest;
import ru.yandex.solomon.alert.protobuf.TResolveNotificationDetailsResponse;
import ru.yandex.solomon.alert.protobuf.TUpdateNotificationRequest;
import ru.yandex.solomon.alert.protobuf.TUpdateNotificationResponse;
import ru.yandex.solomon.alert.protobuf.notification.TNotification;
import ru.yandex.solomon.alert.protobuf.notification.TNotificationDetails;
import ru.yandex.solomon.alert.rule.AlertMuteStatus;
import ru.yandex.solomon.alert.rule.AlertProcessingState;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.flags.FeatureFlagHolderStub;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.util.collection.Nullables;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertArrayEquals;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class ProjectNotificationServiceTestBase {
    protected String projectId;
    protected ManualClock clock;
    protected ScheduledExecutorService executorService;
    protected EntitiesDao<Notification> notificationsDao;
    protected ProjectNotificationService service;
    protected NotificationChannelStubFactory channelFactory;
    protected NotificationConverter notificationConverter;
    protected FeatureFlagHolderStub flagsHolder;
    protected JnsClientStub jns;

    protected void checkPagedResult(TNotification[] expected, TListNotificationsRequest request) {
        final int size = ThreadLocalRandom.current().nextInt(5, 10);
        List<TNotification> buffer = new ArrayList<>(expected.length);
        String token = "";
        do {
            TListNotificationsResponse response = service.listNotification(request.toBuilder()
                    .setPageSize(size)
                    .setPageToken(token)
                    .build())
                    .join();

            assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
            assertThat(response.getNotificationCount(), lessThanOrEqualTo(size));
            buffer.addAll(response.getNotificationList());
            token = response.getNextPageToken();
        } while (!"".equals(token));

        assertArrayEquals(expected, buffer.toArray(new TNotification[0]));
    }

    protected List<TNotification> createManyNotifications() {
        return IntStream.range(1, 100)
                .parallel()
                .mapToObj(index -> TCreateNotificationRequest.newBuilder()
                        .setNotification(randomNotification()
                                .toBuilder()
                                .setName("NotificationChannel - " + index)
                                .build())
                        .build())
                .map(request -> service.createNotification(request))
                .map(future -> future.thenApply(response -> {
                    assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
                    return response.getNotification();
                }))
                .collect(collectingAndThen(toList(), CompletableFutures::joinAll));
    }

    protected StatefulNotificationChannel prepareChannel(Alert alert) {
        String channelId = Iterables.getOnlyElement(alert.getNotificationChannels().entrySet()).getKey();
        Map<String, StatefulNotificationChannel> result = service.prepareChannels(alert);
        assertThat(result, hasKey(channelId));
        assertThat(result.values(), iterableWithSize(1));
        return result.get(channelId);
    }

    protected AlertProcessingState nextEval(AlertProcessingState state, EvaluationStatus status) {
        clock.passedTime(1, TimeUnit.MINUTES);
        return new AlertProcessingState(
                state.evaluationState().nextStatus(status, clock.instant()),
                state.alertMuteStatus()
        );
    }

    protected AlertProcessingState okToAlarmState(Alert alert) {
        return okToAlarmState(alert, null);
    }

    protected AlertProcessingState okToAlarmState(Alert alert, @Nullable Instant since) {
        Instant now = clock.instant();
        return new AlertProcessingState(
                EvaluationState.newBuilder()
                        .setAlertKey(alert.getKey())
                        .setAlertVersion(alert.getVersion())
                        .setStatus(EvaluationStatus.ALARM)
                        .setSince(Nullables.orDefault(since, now))
                        .setLatestEval(now)
                        .setPreviousStatus(EvaluationStatus.OK)
                        .build(),
                new AlertMuteStatus(AlertMuteStatus.MuteStatusCode.NOT_MUTED, List.of())
        );
    }

    protected TNotification successCreate(TNotification notification) {
        TCreateNotificationResponse response = service.createNotification(TCreateNotificationRequest.newBuilder()
                .setNotification(notification)
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getNotification();
    }

    protected TNotification successRead(String id) {
        TReadNotificationResponse response = service.readNotification(TReadNotificationRequest.newBuilder()
                .setNotificationId(id)
                .setProjectId(projectId)
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getNotification();
    }

    protected TNotification successUpdate(TNotification notification) {
        TUpdateNotificationResponse response = service.updateNotification(TUpdateNotificationRequest.newBuilder()
                .setNotification(notification)
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getNotification();
    }

    protected void successDelete(String id) {
        TDeleteNotificationResponse response = service.deleteNotification(TDeleteNotificationRequest.newBuilder()
                .setNotificationId(id)
                .setProjectId(projectId)
                .build())
                .join();

        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
    }

    protected TNotification[] successList(TListNotificationsRequest request) {
        TListNotificationsResponse response = service.listNotification(request).join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getNotificationList().toArray(new TNotification[0]);
    }

    protected TNotificationDetails[] successListDetails(TResolveNotificationDetailsRequest request) {
        TResolveNotificationDetailsResponse response = service.resolveNotificationDetails(request).join();
        assertThat(response.getStatusMessage(), response.getRequestStatus(), equalTo(ERequestStatusCode.OK));
        return response.getNotificationDetailsList().toArray(new TNotificationDetails[0]);
    }

    protected TNotification randomNotification() {
        Notification notification = NotificationTestSupport.randomNotification()
                .toBuilder()
                .setProjectId(projectId)
                .build();

        return convert(notification);
    }

    protected Alert randomAlert() {
        return AlertTestSupport.randomAlert()
                .toBuilder()
                .setProjectId(projectId)
                .setNotificationChannels(Collections.emptyList())
                .setGroupByLabels(Collections.emptyList())
                .build();
    }

    protected TNotification convert(Notification notification) {
        return notificationConverter.notificationToProto(notification)
                .toBuilder()
                .clearCreatedAt()
                .clearUpdatedAt()
                .clearVersion()
                .build();
    }

    protected void createService() {
        service = new ProjectNotificationService(
                projectId,
                clock,
                notificationsDao,
                channelFactory,
                new StatefulNotificationChannelFactoryImpl(executorService, RetryOptions.empty(), notificationConverter),
                notificationConverter, new NotificationSearch(notificationConverter),
                jns = new JnsClientStub());
    }

    protected void restartService() {
        if (service != null) {
            service.close();
        }

        createService();
        service.run().join();
    }
}
