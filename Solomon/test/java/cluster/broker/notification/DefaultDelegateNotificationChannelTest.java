package ru.yandex.solomon.alert.cluster.broker.notification;

import java.io.StringWriter;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.monlib.metrics.encode.text.MetricTextEncoder;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.notification.channel.DevNullOrDefaultsNotificationChannel;
import ru.yandex.solomon.alert.notification.channel.Event;
import ru.yandex.solomon.alert.notification.channel.NotificationChannelStub;
import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.domain.Notification;
import ru.yandex.solomon.alert.notification.domain.NotificationTestSupport;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomActiveAlert;
import static ru.yandex.solomon.alert.notification.channel.EventTestSupport.eval;
import static ru.yandex.solomon.alert.rule.AlertEvalStateTestSupport.randomState;

/**
 * @author Vladimir Gordiychuk
 */
public class DefaultDelegateNotificationChannelTest {
    private Notification def;
    private DevNullOrDefaultsNotificationChannel channel;
    private NotificationChannelStub default1;
    private DelegateNotificationChannel default1Delegate;
    private NotificationChannelStub default2;
    private DelegateNotificationChannel default2Delegate;
    private DelegateNotificationChannel delegator;

    @Before
    public void setUp() throws Exception {
        def = NotificationTestSupport.randomNotification().toBuilder()
                .setDefaultForSeverity(Set.of())
                .setDefaultForProject(false)
                .build();
        default1 = new NotificationChannelStub(def);
        default2 = new NotificationChannelStub(def);
        default1Delegate = new DelegateNotificationChannel(default1);
        default2Delegate = new DelegateNotificationChannel(default2);
        channel = new DevNullOrDefaultsNotificationChannel(def.getId(), def.getProjectId(), List.of(default1Delegate, default2Delegate));
        delegator = new DelegateNotificationChannel(channel);
    }

    @Test
    public void skipReportMetricsWhenNoOneSend() {
        String result = getMetrics(delegator);
        assertThat(result, Matchers.isEmptyString());
    }

    @Test
    public void skipStatusesWithZeroCount() {
        default1.predefineStatuses(NotificationStatus.SUCCESS);
        default2.predefineStatuses(NotificationStatus.SUCCESS);
        delegator.send(Instant.now(), randomEvent()).join();

        String success = expectCount(NotificationStatus.Code.SUCCESS, 1);
        String errors = expectCount(NotificationStatus.Code.ERROR, 0);
        String absent = expectCount(NotificationStatus.Code.ABSENT_NOTIFICATION_CHANNEL, 1);

        String result = getMetrics(delegator);
        assertThat(result, containsString(absent));

        result = getMetrics(default1Delegate);
        assertThat(result, containsString(success));
        assertThat(result, not(containsString(errors)));

        result = getMetrics(default2Delegate);
        assertThat(result, containsString(success));
        assertThat(result, not(containsString(errors)));
    }

    @Test
    public void parallelCounting() {
        default1.predefineStatuses(NotificationStatus.SUCCESS);
        default2.predefineStatuses(NotificationStatus.SUCCESS);

        IntStream.range(0, 100)
                .parallel()
                .mapToObj(value -> delegator.send(Instant.now(), randomEvent()))
                .collect(collectingAndThen(toList(), CompletableFutures::allOf))
                .join();

        String success = expectCount(NotificationStatus.Code.SUCCESS, 100);
        String absent = expectCount(NotificationStatus.Code.ABSENT_NOTIFICATION_CHANNEL, 100);

        String result = getMetrics(delegator);
        assertThat(result, containsString(absent));

        result = getMetrics(default1Delegate);
        assertThat(result, containsString(success));

        result = getMetrics(default2Delegate);
        assertThat(result, containsString(success));
    }

    @Test
    public void countingStatusAsWell() {
        int globalCount = 0;
        for (NotificationStatus.Code code : NotificationStatus.Code.values()) {
            default1.predefineStatuses(code.toStatus());
            default2.predefineStatuses(code.toStatus());
            int count = ThreadLocalRandom.current().nextInt(1, 5);
            for (int index = 0; index < count; index++) {
                delegator.send(Instant.now(), randomEvent()).join();
            }
            globalCount += count;
            String absent = expectCount(NotificationStatus.Code.ABSENT_NOTIFICATION_CHANNEL, globalCount);
            String expected = expectCount(code, count);
            String result = getMetrics(delegator);
            assertThat(result, containsString(absent));

            result = getMetrics(default1Delegate);
            assertThat(result, containsString(expected));

            result = getMetrics(default2Delegate);
            assertThat(result, containsString(expected));
        }
    }

    private String expectCount(NotificationStatus.Code code, long count) {
        return String.format("COUNTER channel.notification.status{channelId='%s', projectId='%s', status='%s'} [%s]",
                def.getId(), def.getProjectId(), code.name(), count);
    }

    private Event randomEvent() {
        Alert alert = randomActiveAlert()
                .toBuilder()
                .setProjectId(def.getProjectId())
                .setNotificationChannel(def.getId())
                .setGroupByLabels(Collections.emptyList())
                .build();

        return eval(alert, randomState(alert));
    }

    private String getMetrics(DelegateNotificationChannel delegator) {
        StringWriter writer = new StringWriter();
        try (MetricTextEncoder e = new MetricTextEncoder(writer, true)) {
            e.onStreamBegin(-1);
            delegator.appendNotificationMetrics(def.getProjectId(), e);
            e.onStreamEnd();
        }
        return writer.toString();
    }

}
