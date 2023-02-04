package ru.yandex.solomon.alert.api.validators;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.notification.channel.telegram.StaffClientStub;
import ru.yandex.solomon.alert.protobuf.TCreateNotificationRequest;
import ru.yandex.solomon.alert.protobuf.TDeleteNotificationRequest;
import ru.yandex.solomon.alert.protobuf.TReadNotificationRequest;
import ru.yandex.solomon.alert.protobuf.TUpdateNotificationRequest;
import ru.yandex.solomon.alert.protobuf.notification.PhoneType;
import ru.yandex.solomon.alert.protobuf.notification.TEmailType;
import ru.yandex.solomon.alert.protobuf.notification.TNotification;
import ru.yandex.solomon.alert.protobuf.notification.TTelegramType;

import static org.junit.Assert.fail;

/**
 * @author Vladimir Gordiychuk
 */
public class NotificationValidatorTest {

    private StaffClientStub staffClientStub;
    private ChatIdResolverStub chatIdResolverStub;
    private NotificationValidator notificationValidator;

    @Before
    public void setUp() {
        staffClientStub = new StaffClientStub();
        staffClientStub.generateUsers();
        chatIdResolverStub = new ChatIdResolverStub();
        notificationValidator = new NotificationValidator(staffClientStub, chatIdResolverStub, null);
    }

    @Test(expected = ValidationException.class)
    public void projectRequiredForCreateNotification() throws Exception {
        ensureNotValid(notificationValidator::ensureValid,
            TCreateNotificationRequest.newBuilder()
                .setNotification(TNotification.newBuilder()
                    .setId(UUID.randomUUID().toString())
                    .setName("My notification channel")
                    .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void projectRequiredForUpdateNotification() throws Exception {
        ensureNotValid(notificationValidator::ensureValid,
            TUpdateNotificationRequest.newBuilder()
                .setNotification(TNotification.newBuilder()
                    .setId(UUID.randomUUID().toString())
                    .setName("My notification channel")
                    .setVersion(100500)
                    .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void projectRequiredForDeleteNotification() throws Exception {
        ensureNotValid(notificationValidator::ensureValid,
            TDeleteNotificationRequest.newBuilder()
                .setNotificationId(UUID.randomUUID().toString())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void projectRequiredForReadNotification() throws Exception {
        ensureNotValid(notificationValidator::ensureValid,
            TReadNotificationRequest.newBuilder()
                .setNotificationId(UUID.randomUUID().toString())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void invalidEvail() {
        ensureNotValid(notificationValidator::ensureValid, TCreateNotificationRequest.newBuilder()
                .setNotification(TNotification.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setProjectId("solomon")
                        .setName("invalid email")
                        .setEmail(TEmailType.newBuilder()
                                .addRecipients("gordiychuk")
                                .build())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void invalidStaffLogin() {
        staffClientStub.doNotGenerateUsers();
        ensureNotValid(notificationValidator::ensureValid, TCreateNotificationRequest.newBuilder()
                .setNotification(TNotification.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setProjectId("solomon")
                        .setName("invalid staff")
                        .setTelegram(TTelegramType.newBuilder()
                                .setLogin("nobody")
                                .build())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void userWithNoTelegram() {
        staffClientStub.generateUsersWithoutTelegram();
        ensureValid(notificationValidator::ensureValid, TCreateNotificationRequest.newBuilder()
                .setNotification(TNotification.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setProjectId("solomon")
                        .setName("invalid telegram")
                        .setTelegram(TTelegramType.newBuilder()
                                .setLogin("nobody")
                                .build())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void userDidNotSpeakToBot() {
        chatIdResolverStub.returnNull();
        ensureValid(notificationValidator::ensureValid, TCreateNotificationRequest.newBuilder()
                .setNotification(TNotification.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setProjectId("solomon")
                        .setName("no /start")
                        .setTelegram(TTelegramType.newBuilder()
                                .setLogin("nobody")
                                .build())
                        .build())
                .build());
    }

    @Test
    public void validTelegram() {
        ensureValid(notificationValidator::ensureValid, TCreateNotificationRequest.newBuilder()
                .setNotification(TNotification.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setProjectId("solomon")
                        .setName("invalid telegram")
                        .setTelegram(TTelegramType.newBuilder()
                                .setLogin("nobody")
                                .build())
                        .build())
                .build());
    }

    @Test(expected = ValidationException.class)
    public void invalidStaffLogin_phone() {
        staffClientStub.doNotGenerateUsers();
        ensureNotValid(notificationValidator::ensureValid, TCreateNotificationRequest.newBuilder()
                .setNotification(TNotification.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setProjectId("solomon")
                        .setName("invalid staff")
                        .setPhone(PhoneType.newBuilder()
                                .setLogin("nobody")
                                .build())
                        .build())
                .build());
    }

    @Test
    public void validPhoneLogin() {
        ensureValid(notificationValidator::ensureValid, TCreateNotificationRequest.newBuilder()
                .setNotification(TNotification.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setProjectId("solomon")
                        .setName("invalid telegram")
                        .setPhone(PhoneType.newBuilder()
                                .setLogin("nobody")
                                .build())
                        .build())
                .build());
    }

    private <Req> void ensureValid(Function<Req, CompletableFuture<Req>> fn, Req request) {
        try {
            fn.apply(request).join();
        } catch (CompletionException ce) {
            Throwable t = CompletableFutures.unwrapCompletionException(ce);
            if (t instanceof RuntimeException re) {
                System.err.println(re.getMessage());
                throw re;
            }
            throw new RuntimeException(t);
        }
    }

    private <Req> void ensureNotValid(Function<Req, CompletableFuture<Req>> fn, Req request) {
        ensureValid(fn, request);
        fail("Expected that request will be invalid: " + request);
    }
}
