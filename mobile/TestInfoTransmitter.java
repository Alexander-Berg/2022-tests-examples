package com.yandex.frankenstein.annotations.handlers;

import org.jetbrains.annotations.NotNull;

public class TestInfoTransmitter {

    public void transmit(@NotNull final String tag) {
        transmit(tag, "");
    }

    @SuppressWarnings("PMD.SystemPrintln")
    public void transmit(@NotNull final String tag, @NotNull final String info) {
        System.err.println(tag + ": " + info);
    }
}
