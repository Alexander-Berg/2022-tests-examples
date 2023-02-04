package ru.yandex.qe.dispenser.ws.logic;

import org.jetbrains.annotations.NotNull;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;

public class DiAmountWithNegativeValueAllowed extends DiAmount {
    private final long value;

    private DiAmountWithNegativeValueAllowed(final long value, final @NotNull DiUnit unit) {
        super(0, unit);
        this.value = value;
    }

    @NotNull
    public static DiAmount of(final long value, @NotNull final DiUnit unit) {
        return new DiAmountWithNegativeValueAllowed(value, unit);
    }

    @Override
    public long getValue() {
        return value;
    }
}
