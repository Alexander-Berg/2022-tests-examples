package ru.yandex.qe.dispenser.utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

public class DiEntityIdSupplier implements Supplier<String> {
    @NotNull
    private final String prefix;
    @NotNull
    private final AtomicInteger entityCounter;

    public DiEntityIdSupplier() {
        this("entity");
    }

    public DiEntityIdSupplier(@NotNull final String prefix) {
        this.prefix = prefix;
        this.entityCounter = new AtomicInteger();
    }

    @Override
    public String get() {
        return prefix + "-" + entityCounter.incrementAndGet();
    }
}
