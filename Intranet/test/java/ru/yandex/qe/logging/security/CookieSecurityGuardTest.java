package ru.yandex.qe.logging.security;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Established by terry
 * on 27.01.16.
 */
public class CookieSecurityGuardTest {

    @Test
    public void secure_session_cookies() {
        final CookieSecurityGuard cookieSecurityGuard = new CookieSecurityGuard();
        assertEquals(cookieSecurityGuard.secure("Cookie", "test=123"), "test=***");
        assertEquals(cookieSecurityGuard.secure("Cookie", "test=123;"), "test=***");
        assertEquals(cookieSecurityGuard.secure("Authorization", "OAuth blabla"), "OAuth blabla");

        final List<String> secured = cookieSecurityGuard.secure("Cookie",
                Arrays.asList("test=123", "test2=123;test3=1234"));
        assertEquals(secured.size(), 2);
        assertEquals(secured.get(0), "test=***");
        assertEquals(secured.get(1), "test2=***;test3=***");
    }
}
