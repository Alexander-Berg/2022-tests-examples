package ru.yandex.market.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PhoneNumberUtilsTest {

    @Test
    public void normalizePhoneNumber() {
        assertEquals("", PhoneNumberUtils.normalizePhoneNumber(""));
        assertEquals("+72223334455", PhoneNumberUtils.normalizePhoneNumber("+72223334455"));
        assertEquals("+72223334455", PhoneNumberUtils.normalizePhoneNumber("+7 222 333 44 55"));
        assertEquals("+72223334455", PhoneNumberUtils.normalizePhoneNumber("+7 (222) 333 44 55"));
        assertEquals("+72223334455", PhoneNumberUtils.normalizePhoneNumber("+7 (222) 333-44-55"));
        assertEquals("+72223334455", PhoneNumberUtils.normalizePhoneNumber("+7 (222) 3334455"));
    }

}