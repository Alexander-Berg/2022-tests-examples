package ru.yandex.qe.dispenser.client.v1.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jetbrains.annotations.NotNull;

public class LoggingDispenserFactory extends RemoteDispenserFactory {
    private static final Log LOG = LogFactory.getLog(LoggingDispenserFactory.class);

    protected LoggingDispenserFactory(@NotNull final DispenserConfig config) {
        super(config);
    }

    @NotNull
    @Override
    public WebClient createUnconfiguredClient(@NotNull final String address) {
        return super.createUnconfiguredClient(address);
    }
}
