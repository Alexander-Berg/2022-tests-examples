package ru.yandex.solomon.alert.cluster.broker.notification;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.api.converters.NotificationConverter;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.ChannelConfig;
import ru.yandex.solomon.alert.notification.NotificationServiceMetrics;
import ru.yandex.solomon.alert.notification.RetryOptions;
import ru.yandex.solomon.alert.notification.channel.DevNullOrDefaultsNotificationChannel;
import ru.yandex.solomon.alert.notification.channel.NotificationChannel;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.notification.state.PendingState;
import ru.yandex.solomon.alert.notification.state.StatefulNotificationChannel;
import ru.yandex.solomon.alert.notification.state.StatefulNotificationChannelImpl;
import ru.yandex.solomon.alert.rule.EvaluationState;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public class StatefulNotificationChannelFactoryStub implements AlertStatefulNotificationChannelsFactory {
    private final ScheduledExecutorService executorService;
    private final ConcurrentMap<String, DelegateNotificationChannel> channels = new ConcurrentHashMap<>();
    private final NotificationServiceMetrics metrics = new NotificationServiceMetrics();
    private final String projectId;

    public StatefulNotificationChannelFactoryStub(ScheduledExecutorService executorService, String projectId) {
        this.executorService = executorService;
        this.projectId = projectId;
    }

    public void setNotificationChannel(NotificationChannel channel) {
        getDelegate(channel.getId()).updateDelegate(channel);
    }

    private DelegateNotificationChannel getDelegate(String id) {
        return channels.computeIfAbsent(id, ignore -> new DelegateNotificationChannel(
                new DevNullOrDefaultsNotificationChannel(id, projectId, List.of())));
    }

    @Override
    public Map<String, StatefulNotificationChannel> prepareChannels(Alert alert) {
        Map<String, StatefulNotificationChannel> result = new HashMap<>(alert.getNotificationChannels().size());
        for (var notificationChannelConfig : alert.getNotificationChannels().entrySet()) {
            String id = notificationChannelConfig.getKey();
            ChannelConfig config = notificationChannelConfig.getValue();
            StatefulNotificationChannelImpl channel = new StatefulNotificationChannelImpl(
                alert, getDelegate(id), config, executorService, RetryOptions.empty(), metrics,
                new NotificationConverter(new ChatIdResolverStub()));
            // Skip isFirstOk check by adjusting lastEval time
            var ok = EvaluationState.newBuilder(alert)
                    .setLatestEval(Instant.EPOCH.plusSeconds(1))
                    .setSince(Instant.EPOCH.plusSeconds(1))
                    .setStatus(EvaluationStatus.OK)
                    .build();
            channel.overrideState(new PendingState(channel, channel.getLatestNotificationState()
                    .nextStatus(NotificationStatus.OBSOLETE, ok)));
            result.put(id, channel);
        }

        return result;
    }
}
