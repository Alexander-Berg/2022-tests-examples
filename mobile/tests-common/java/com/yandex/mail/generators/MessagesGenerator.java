package com.yandex.mail.generators;

import com.yandex.mail.wrappers.MessageWrapper;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import androidx.annotation.NonNull;

@SuppressWarnings("checkstyle:magicnumber")
public class MessagesGenerator {

    private long currentMid = 10000;

    private static final long startTime = Calendar.getInstance().getTime().getTime();

    private AtomicLong timeOffset = new AtomicLong(0);

    @NonNull
    public String nextMid() {
        return Long.toString(currentMid++);
    }

    @NonNull
    public MessageWrapper.MessageWrapperBuilder makeEmptyReadMessage() {
        String mid = nextMid();
        return new MessageWrapper.MessageWrapperBuilder()
                .mid(mid)
                .rfcId(mid + "@default-fake-rfc-id.com")
                .tid(mid)
                .read(true)
                .timestamp(new Date(startTime + timeOffset.getAndAdd(1000)))
                .subjPrefix("")
                .subjText("default fake subject")
                .from("default-fake-sender@ya.ru", "Default fake name")
                .content("");
    }

    @NonNull
    public MessageWrapper.MessageWrapperBuilder makeEmptyUnreadMessage() {
        return makeEmptyReadMessage().read(false);
    }
}
