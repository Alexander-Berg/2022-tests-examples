package ru.yandex.solomon.alert.notification.channel.sms;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;

import ru.yandex.solomon.alert.notification.channel.NotificationStatus;

/**
 * @author Vladimir Gordiychuk
 */
public class SmsClientStub implements SmsClient {
    private final ConcurrentLinkedQueue<Message> sandbox = new ConcurrentLinkedQueue<>();

    public Message receiveMessage() {
        return sandbox.poll();
    }

    public int countMessageInSandbox() {
        return sandbox.size();
    }

    @Override
    public CompletableFuture<NotificationStatus> sendToPhone(String phone, String text, String identity) {
        return CompletableFuture.supplyAsync(() -> {
            sandbox.add(new Message(null, phone, text, identity));
            return NotificationStatus.SUCCESS;
        });
    }

    @Override
    public CompletableFuture<NotificationStatus> sendToUser(String login, String text, String identity) {
        return CompletableFuture.supplyAsync(() -> {
            sandbox.add(new Message(login, null, text, identity));
            return NotificationStatus.SUCCESS;
        });
    }

    public static class Message {
        @Nullable
        private final String login;
        @Nullable
        private final String phone;
        private final String text;
        private final String identity;

        public Message(@Nullable String login, @Nullable String phone, String text, String identity) {
            this.login = login;
            this.phone = phone;
            this.text = text;
            this.identity = identity;
        }

        @Nullable
        public String getLogin() {
            return login;
        }

        @Nullable
        public String getPhone() {
            return phone;
        }

        public String getText() {
            return text;
        }

        public String getIdentity() {
            return identity;
        }
    }
}
