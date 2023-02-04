package ru.yandex.solomon.alert.notification.channel;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;

import ru.yandex.solomon.alert.domain.ChannelConfig;
import ru.yandex.solomon.alert.notification.DispatchRule;
import ru.yandex.solomon.alert.notification.DispatchRuleFactory;
import ru.yandex.solomon.alert.notification.domain.Notification;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public class NotificationChannelStub extends AbstractNotificationChannel<Notification> {
    private final AtomicReference<List<NotificationStatus>> predefineStatus = new AtomicReference<>(ImmutableList.of());
    private final Semaphore sync = new Semaphore(0);
    private final ConcurrentMap<String, Semaphore> syncByAlertId = new ConcurrentHashMap<>();

    public NotificationChannelStub(Notification notification) {
        super(notification);
    }

    @Override
    protected DispatchRule makeDispatchRule(ChannelConfig config) {
        return DispatchRuleFactory.statusFilteringRepeatOks(
            config.getNotifyAboutStatusesOrDefault(notification.getNotifyAboutStatus()),
            config.getRepeatNotificationDelayOrDefault(notification.getRepeatNotifyDelay()));
    }

    public void predefineStatuses(NotificationStatus... status) {
        List<NotificationStatus> old;
        List<NotificationStatus> next;
        do {
            old = predefineStatus.get();
            next = ImmutableList.<NotificationStatus>builder()
                    .add(status)
                    .build();
        } while (!predefineStatus.compareAndSet(old, next));
    }

    private NotificationStatus nextStatus() {
        NotificationStatus status;
        List<NotificationStatus> old;
        List<NotificationStatus> next;
        do {
            old = predefineStatus.get();
            if (old.size() == 0) {
                return NotificationStatus.SUCCESS;
            }

            status = old.get(0);
            if (old.size() == 1) {
                return status;
            }

            next = old.subList(1, old.size());
        } while (!predefineStatus.compareAndSet(old, next));

        return status;
    }

    @Nonnull
    @Override
    public CompletableFuture<NotificationStatus> send(Instant latestSuccessSend, Event event) {
        String alertId = event.getAlert().getId();
        return CompletableFuture.supplyAsync(this::nextStatus)
                .whenComplete((ignore1, ignore2) -> {
                    getSendSync().release();
                    getSendSync(alertId).release();
                });
    }

    @Override
    public void close() {
    }

    public Semaphore getSendSync() {
        return sync;
    }

    public Semaphore getSendSync(String alertId) {
        return syncByAlertId.computeIfAbsent(alertId, ignore -> new Semaphore(0));
    }
}
