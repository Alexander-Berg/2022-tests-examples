package ru.yandex.solomon.alert.cluster.broker.alert.activity.search;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.api.converters.AlertConverter;
import ru.yandex.solomon.alert.api.converters.NotificationConverter;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.alert.notification.NotificationStateTestSupport;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.protobuf.TPersistAlertState;
import ru.yandex.solomon.alert.protobuf.TPersistEvaluationState;
import ru.yandex.solomon.alert.protobuf.TPersistMultiAlertState;
import ru.yandex.solomon.alert.protobuf.TPersistNotificationState;
import ru.yandex.solomon.alert.protobuf.TPersistSimpleAlertState;
import ru.yandex.solomon.alert.protobuf.TPersistSubAlertState;
import ru.yandex.solomon.alert.rule.AlertEvalStateTestSupport;
import ru.yandex.solomon.labels.protobuf.LabelConverter;
import ru.yandex.solomon.model.protobuf.Label;

/**
 * @author Vladimir Gordiychuk
 */
public final class AlertPersistStateSupport {
    public static final AlertPersistStateSupport INSTANCE = new AlertPersistStateSupport(new NotificationConverter(new ChatIdResolverStub()));

    private final NotificationConverter notificationConverter;

    public AlertPersistStateSupport(
        NotificationConverter notificationConverter)
    {
        this.notificationConverter = notificationConverter;
    }

    public TPersistAlertState state(Alert alert, EvaluationStatus status, NotificationStatus... notifications) {
        Instant now = Instant.now();
        return TPersistAlertState.newBuilder()
                .setId(alert.getId())
                .setVersion(alert.getVersion())
                .setSimpleAlertState(TPersistSimpleAlertState.newBuilder()
                        .setEvaluation(TPersistEvaluationState.newBuilder()
                                .setSinceMillis(now.toEpochMilli())
                                .setLatestEvalMillis(now.toEpochMilli())
                                .setStatus(AlertConverter.statusToProto(status))
                                .setPreviousStatus(AlertConverter.statusToProto(EvaluationStatus.OK))
                                .build())
                        .addAllNotifications(IntStream.range(0, notifications.length)
                                .mapToObj(index -> TPersistNotificationState.newBuilder()
                                        .setLatestEvalMillis(now.toEpochMilli())
                                        .setLatestSuccessMillis(now.toEpochMilli())
                                        .setNotificationChannelId("channel-" + index)
                                    .setStatus(notificationConverter.statusToProto(notifications[index]))
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .build();
    }

    public TPersistAlertState state(Alert alert, Map<Labels, EvaluationStatus> status) {
        Instant now = Instant.now();
        return TPersistAlertState.newBuilder()
                .setId(alert.getId())
                .setVersion(alert.getVersion())
                .setMultiAlertState(TPersistMultiAlertState.newBuilder()
                        .addAllSubAlerts(status.entrySet().stream()
                                .map(entry -> TPersistSubAlertState.newBuilder()
                                        .addAllLabels(LabelConverter.labelsToProtoList(entry.getKey()))
                                        .setEvaluation(TPersistEvaluationState.newBuilder()
                                                .setSinceMillis(now.toEpochMilli())
                                                .setLatestEvalMillis(now.toEpochMilli())
                                                .setStatus(AlertConverter.statusToProto(entry.getValue()))
                                                .setPreviousStatus(AlertConverter.statusToProto(EvaluationStatus.OK))
                                                .build())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .build();
    }

    public TPersistAlertState stateNotification(Alert alert, Map<Labels, List<NotificationStatus>> status) {
        Instant now = Instant.now();
        return TPersistAlertState.newBuilder()
                .setId(alert.getId())
                .setVersion(alert.getVersion())
                .setMultiAlertState(TPersistMultiAlertState.newBuilder()
                        .addAllSubAlerts(status.entrySet().stream()
                                .map(entry -> TPersistSubAlertState.newBuilder()
                                        .addAllLabels(LabelConverter.labelsToProtoList(entry.getKey()))
                                        .addAllNotifications(IntStream.range(0, entry.getValue().size())
                                                .mapToObj(index -> TPersistNotificationState.newBuilder()
                                                        .setLatestEvalMillis(now.toEpochMilli())
                                                        .setLatestSuccessMillis(now.toEpochMilli())
                                                        .setNotificationChannelId("channel-" + index)
                                                    .setStatus(notificationConverter
                                                        .statusToProto(entry.getValue().get(index)))
                                                        .build())
                                                .collect(Collectors.toList()))
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .build();
    }

    public TPersistAlertState randomState() {
        Alert alert = AlertTestSupport.randomAlert();
        if (alert.getGroupByLabels().isEmpty()) {
            return TPersistAlertState.newBuilder()
                    .setId(UUID.randomUUID().toString())
                    .setVersion(ThreadLocalRandom.current().nextInt(1, 1000))
                    .setSimpleAlertState(randomSimpleAlertState())
                    .build();
        } else {
            return TPersistAlertState.newBuilder()
                    .setId(UUID.randomUUID().toString())
                    .setVersion(ThreadLocalRandom.current().nextInt(1, 1000))
                    .setMultiAlertState(randomMultiAlertState(alert.getGroupByLabels()))
                    .build();
        }
    }

    private TPersistEvaluationState randomEvaluation() {
        Instant now = Instant.now();
        return TPersistEvaluationState.newBuilder()
                .setSinceMillis(now.toEpochMilli())
                .setLatestEvalMillis(now.toEpochMilli())
                .setStatus(AlertConverter.statusToProto(AlertEvalStateTestSupport.randomEvalStatus()))
                .setPreviousStatus(AlertConverter.statusToProto(AlertEvalStateTestSupport.randomEvalStatus()))
                .build();
    }

    private TPersistNotificationState randomNotification(String channelId) {
        Instant now = Instant.now();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        return TPersistNotificationState.newBuilder()
                .setNotificationChannelId(channelId)
                .setLatestSuccessMillis(now.minusMillis(random.nextLong(0, TimeUnit.DAYS.toMillis(10))).toEpochMilli())
                .setLatestEvalMillis(now.toEpochMilli())
            .setStatus(notificationConverter.statusToProto(NotificationStateTestSupport.randomNotificationStatus()))
                .build();
    }

    private TPersistSimpleAlertState randomSimpleAlertState() {
        int channels = ThreadLocalRandom.current().nextInt(0, 2);
        return TPersistSimpleAlertState.newBuilder()
                .setEvaluation(randomEvaluation())
                .addAllNotifications(IntStream.range(0, channels)
                        .mapToObj(ignore -> randomNotification(UUID.randomUUID().toString()))
                        .collect(Collectors.toList()))
                .build();
    }

    private TPersistMultiAlertState randomMultiAlertState(List<String> labels) {
        int countChannels = ThreadLocalRandom.current().nextInt(0, 2);
        List<String> channelIds = IntStream.range(0, countChannels)
                .mapToObj(ignore -> UUID.randomUUID().toString())
                .collect(Collectors.toList());

        int countSubAlert = ThreadLocalRandom.current().nextInt(1, 100);
        List<TPersistSubAlertState> subAlert = IntStream.range(0, countSubAlert)
                .mapToObj(index -> TPersistSubAlertState.newBuilder()
                        .addAllLabels(labels.stream()
                                .map(name -> Label.newBuilder()
                                        .setKey(name)
                                        .setValue(name + "-" + index)
                                        .build())
                                .collect(Collectors.toList()))
                        .setEvaluation(randomEvaluation())
                        .addAllNotifications(channelIds.stream()
                                .map(this::randomNotification)
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        return TPersistMultiAlertState.newBuilder()
                .addAllSubAlerts(subAlert)
                .build();
    }
}
