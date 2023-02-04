package ru.yandex.solomon.alert.dao.memory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.solomon.alert.dao.NotificationsDao;
import ru.yandex.solomon.alert.dao.codec.NotificationCodec;
import ru.yandex.solomon.alert.dao.codec.NotificationRecord;
import ru.yandex.solomon.alert.domain.AlertSeverity;
import ru.yandex.solomon.alert.notification.domain.Notification;
import ru.yandex.solomon.idempotency.IdempotentOperation;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class InMemoryNotificationsDao extends InMemoryEntitiesDao<Notification, NotificationRecord> implements NotificationsDao {
    public InMemoryNotificationsDao() {
        super(new NotificationCodec(new ObjectMapper()));
    }

    @Override
    public CompletableFuture<Void> deleteByIdWithValidations(String projectId, String id, IdempotentOperation op, Set<AlertSeverity> validateSeverities) {
        if (validateSeverities.isEmpty()) {
            return deleteById(projectId, id, op);
        }
        var set = new HashSet<>(validateSeverities);
        return findAll(projectId)
                .thenCompose(notifications -> {
                    set.removeAll(notifications.stream()
                            .filter(notification -> !notification.getId().equals(id))
                            .flatMap(notification -> notification.getDefaultForSeverity().stream()).collect(Collectors.toSet()));
                    if (set.isEmpty()) {
                        return deleteById(projectId, id, op);
                    }
                    return CompletableFuture.failedFuture(new IllegalArgumentException("Try to delete last default channel for " + set.iterator().next().name() + " severity"));
                });
    }

    @Override
    public CompletableFuture<Optional<Notification>> updateWithValidations(Notification entity, IdempotentOperation op, Set<AlertSeverity> validateSeverities) {
        if (validateSeverities.isEmpty()) {
            return update(entity, op);
        }
        var set = new HashSet<>(validateSeverities);
        return findAll(entity.getProjectId())
                .thenCompose(notifications -> {
                    set.removeAll(notifications.stream()
                            .filter(notification -> !notification.getId().equals(entity.getId()))
                            .flatMap(notification -> notification.getDefaultForSeverity().stream()).collect(Collectors.toSet()));
                    if (set.isEmpty()) {
                        return update(entity, op);
                    }
                    return CompletableFuture.failedFuture(new IllegalArgumentException("Try to delete last default channel for " + set.iterator().next().name() + " severity"));
                });
    }
}
