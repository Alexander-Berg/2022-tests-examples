package ru.yandex.disk.service;

import ru.yandex.disk.util.AbstractLogger;

import javax.annotation.NonnullByDefault;

@NonnullByDefault
public class CommandLogger extends AbstractLogger<CommandRequest> implements CommandStarter {
    @Override
    public void start(final CommandRequest request) {
        log(request);
    }
}