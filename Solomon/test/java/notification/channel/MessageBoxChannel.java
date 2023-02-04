package ru.yandex.solomon.alert.notification.channel;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.solomon.alert.domain.ChannelConfig;
import ru.yandex.solomon.alert.notification.DispatchRule;
import ru.yandex.solomon.alert.notification.DispatchRuleFactory;
import ru.yandex.solomon.alert.notification.domain.Notification;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class MessageBoxChannel extends AbstractNotificationChannel<Notification> {
    private final Clock clock;
    public static record Envelope(Event event, Instant lastSuccessSend, Instant sendTime) {
    }

    private final ConcurrentLinkedQueue<Envelope> outbox = new ConcurrentLinkedQueue<>();

    public MessageBoxChannel(Clock clock, Notification notification) {
        super(notification);
        this.clock = clock;
    }

    @Override
    protected DispatchRule makeDispatchRule(ChannelConfig config) {
        return DispatchRuleFactory.statusFiltering(
                config.getNotifyAboutStatusesOrDefault(notification.getNotifyAboutStatus()),
                config.getRepeatNotificationDelayOrDefault(notification.getRepeatNotifyDelay()));
    }

    @Override
    public CompletableFuture<NotificationStatus> send(Instant latestSuccessSend, Event event) {
        outbox.add(new Envelope(event, latestSuccessSend, clock.instant()));
        return CompletableFuture.completedFuture(NotificationStatus.SUCCESS);
    }

    public java.util.List<Envelope> getOutbox() {
        return outbox.stream().collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void close() {
    }
}
