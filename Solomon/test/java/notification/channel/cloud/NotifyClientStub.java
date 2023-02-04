package ru.yandex.solomon.alert.notification.channel.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.solomon.alert.notification.channel.NotificationStatus;
import ru.yandex.solomon.alert.notification.channel.cloud.dto.NotifyDto;
import ru.yandex.solomon.alert.notification.channel.cloud.dto.NotifyDtoV1;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class NotifyClientStub implements NotifyClient {

    private List<NotifyDto<?>> outboxEmail = new ArrayList<>();
    private List<NotifyDtoV1<?>> outboxSms = new ArrayList<>();
    private List<NotifyDtoV1<?>> outboxPush = new ArrayList<>();

    @Override
    public <T> CompletableFuture<NotificationStatus> sendEmail(NotifyDto<T> email) {
        outboxEmail.add(email);
        return completedFuture(NotificationStatus.SUCCESS);
    }

    @Override
    public <T> CompletableFuture<NotificationStatus> sendSms(NotifyDtoV1<T> sms) {
        outboxSms.add(sms);
        return completedFuture(NotificationStatus.SUCCESS);
    }

    @Override
    public <T> CompletableFuture<NotificationStatus> sendPush(NotifyDtoV1<T> push) {
        outboxPush.add(push);
        return completedFuture(NotificationStatus.SUCCESS);
    }

    public NotifyDto<?> getOutboxEmail(int index) {
        return outboxEmail.get(index);
    }

    public NotifyDtoV1<?> getOutboxSms(int index) {
        return outboxSms.get(index);
    }

    public NotifyDtoV1<?> getOutboxPush(int index) {
        return outboxPush.get(index);
    }
}
