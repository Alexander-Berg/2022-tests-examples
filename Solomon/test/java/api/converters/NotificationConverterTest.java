package ru.yandex.solomon.alert.api.converters;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.notification.domain.Notification;
import ru.yandex.solomon.alert.notification.domain.NotificationType;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.alert.notification.domain.NotificationTestSupport.randomNotification;

/**
 * @author Vladimir Gordiychuk
 */
@RunWith(Parameterized.class)
public class NotificationConverterTest {
    private ThreadLocalRandom random;
    private NotificationConverter notificationConverter;
    private ChatIdResolverStub chatIdResolver;

    @Parameterized.Parameter
    public NotificationType type;

    @Parameterized.Parameters(name = "{0}")
    public static NotificationType[] configs() {
        return Arrays.stream(NotificationType.values())
            .filter(type -> !type.equals(NotificationType.UNKNOWN))
            .toArray(NotificationType[]::new);
    }

    @Before
    public void setUp() throws Exception {
        random = ThreadLocalRandom.current();
        chatIdResolver = new ChatIdResolverStub();
        notificationConverter = new NotificationConverter(chatIdResolver);
    }

    @Test
    public void toProtoAndBack() {
        // TODO(uranix): Fix telegram, chatIdResover generates junk
        Assume.assumeFalse(type.equals(NotificationType.TELEGRAM));

        Notification expected = randomNotification(random, type);
        Notification converted =
            notificationConverter.protoToNotification(notificationConverter.notificationToProto(expected));
        compare(expected, converted);
    }

    @Test
    public void toDetails() {
        Notification notification = randomNotification(random, type);
        var proto = notificationConverter.notificationToDetailsProto(notification);
        assertThat(proto.getRecipientsList(), not(emptyIterable()));
    }

    private void compare(Object expected, Object converted) {
        boolean areEquals = EqualsBuilder.reflectionEquals(expected, converted);
        assertThat("Original: " + expected + ", \n                         Converted: " + converted, areEquals, equalTo(true));
    }
}
