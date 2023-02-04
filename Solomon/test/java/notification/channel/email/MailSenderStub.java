package ru.yandex.solomon.alert.notification.channel.email;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Throwables;

import ru.yandex.commune.mail.MailMessage;
import ru.yandex.commune.mail.send.MailSender;

/**
 * @author Vladimir Gordiychuk
 */
public class MailSenderStub implements MailSender {
    private final ConcurrentLinkedQueue<MailMessage> sandbox = new ConcurrentLinkedQueue<>();

    @Override
    public void send(MailMessage message) {
        try {
            // emulate send delay
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(0, 10));
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }

        sandbox.add(message);
    }

    public MailMessage receiveMessage() {
        return sandbox.poll();
    }

    public int countMessageInSandbox() {
        return sandbox.size();
    }
}
