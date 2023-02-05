package ru.yandex.disk.event;

import ru.yandex.disk.util.AbstractLogger;

import javax.annotation.NonnullByDefault;

@NonnullByDefault
public class EventLogger extends AbstractLogger<Event> implements EventSender {

    @Override
    public void send(final Event event) {
        log(event);
    }

}
