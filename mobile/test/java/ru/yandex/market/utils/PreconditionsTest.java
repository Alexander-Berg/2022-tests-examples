package ru.yandex.market.utils;

import org.junit.Test;

public class PreconditionsTest {

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIllegalArgumentExceptionWhenIntegerValueIsNotInRange() {
        Preconditions.checkIsInRange(0, 1, 2);
    }

    @Test
    public void testDoNothingWhenIntegerValueIsInRange() {
        Preconditions.checkIsInRange(2, 1, 2);
    }
}