package ru.yandex.solomon.alert.notification.channel;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.solomon.alert.notification.domain.Notification;
import ru.yandex.solomon.alert.notification.domain.NotificationType;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public class NotificationChannelStubFactory implements NotificationChannelFactory {
    private final ConcurrentMap<Key, NotificationChannelStub> channelById = new ConcurrentHashMap<>();

    @Override
    public Map<NotificationType, Integer> getSupportTypes() {
        return EnumSet.allOf(NotificationType.class).stream()
                .collect(Collectors.toMap(Function.identity(), ignore -> 100));
    }

    @Nonnull
    @Override
    public NotificationChannelStub createChannel(Notification notification) {
        Key key = new Key(notification);
        return channelById.computeIfAbsent(key, ignore -> new NotificationChannelStub(notification));
    }

    @ParametersAreNonnullByDefault
    private static class Key {
        private final String id;
        private final int version;

        public Key(String id, int version) {
            this.id = id;
            this.version = version;
        }

        public Key(Notification notification) {
            this.id = notification.getId();
            this.version = notification.getVersion();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (version != key.version) return false;
            return id.equals(key.id);
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + version;
            return result;
        }
    }
}
