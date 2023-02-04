package ru.yandex.qe.dispenser.standalone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.mail.SimpleMailMessage;

import ru.yandex.qe.dispenser.domain.dao.notifications.EmailSender;

public class MockEmailSender extends EmailSender {

    private final List<SimpleMailMessage> messages = new ArrayList<>();

    @Override
    public void sendMessage(final @NotNull String subject, final @NotNull String text, final @NotNull Collection<String> recipients, final boolean isHtml) {
        final SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(recipients.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        msg.setText(text);
        msg.setSubject(subject);

        messages.add(msg);
    }

    public List<SimpleMailMessage> getMessages() {
        return messages;
    }

    public void clear() {
        messages.clear();
    }
}
