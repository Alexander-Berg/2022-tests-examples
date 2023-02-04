package ru.yandex.solomon.alert.notification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

import ru.yandex.solomon.alert.domain.AlertKey;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;

/**
 * @author Vladimir Gordiychuk
 */
public class NotificationStateTestSupport {
    private static final List<String> PROJECTS =
            ImmutableList.of("junk", "solomon", "kikimr", UUID.randomUUID().toString());

    public static NotificationState randomState() {
        Instant latestEval = Instant.now().plusSeconds(randomSecondsNearYear());
        Instant latestSuccess = latestEval.minusSeconds(randomSecondsNearYear());

        return NotificationState.newBuilder()
                .setKey(randomKey())
                .setLatestEval(latestEval)
                .setLatestSuccessNotify(latestSuccess)
                .setLatestStatus(randomNotificationStatus())
                .build();
    }

    public static NotificationKey randomKey() {
        return new NotificationKey(
                new AlertKey(PROJECTS.get(ThreadLocalRandom.current().nextInt(PROJECTS.size())),
                        "",
                        "alert-" + UUID.randomUUID().toString()),
                "notification-" + UUID.randomUUID().toString());
    }

    public static NotificationStatus randomNotificationStatus() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        NotificationStatus.Code[] source = NotificationStatus.Code.values();
        NotificationStatus.Code code = source[random.nextInt(0, source.length)];
        if (code == NotificationStatus.Code.ERROR) {
            return code.toStatus(Throwables.getStackTraceAsString(new RuntimeException("expected rnd: " + random.nextInt())));
        }
        return code.toStatus("rnd: " + random.nextInt());
    }

    private static long randomSecondsNearYear() {
        return ThreadLocalRandom.current().nextLong(0, ChronoUnit.YEARS.getDuration().getSeconds());
    }

}
