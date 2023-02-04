package ru.yandex.qe.logging.security;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Established by terry
 * on 27.01.16.
 */
public class AuthorizationSecurityGuardTest {

    @Test
    public void secure_oauth() {
        final AuthorizationSecurityGuard authorizationSecurityGuard = new AuthorizationSecurityGuard();
        assertEquals(authorizationSecurityGuard.secure("Authorization", "OAuth blabla"), "OAuth ***");
        assertEquals(authorizationSecurityGuard.secure("Authorization", "blabla"), "blabla");
        assertEquals(authorizationSecurityGuard.secure("Bla", "OAuth blabla"), "OAuth blabla");

        final List<String> secured = authorizationSecurityGuard.secure("Authorization",
                Arrays.asList("OAuth blabla", "OAuth haha", "Basic blabla"));
        assertEquals(secured.size(), 3);
        assertEquals(secured.get(0), "OAuth ***");
        assertEquals(secured.get(1), "OAuth ***");
        assertEquals(secured.get(2), "Basic blabla");
    }
}
