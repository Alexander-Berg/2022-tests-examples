package ru.yandex.payments.tvmlocal.testing.options;

import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record TvmToolOptions(OptionalInt port,
                             ConfigLocation configLocation,
                             Mode mode,
                             Map<String, String> env,
                             String connectionAuthToken) {
    private static final String DIGITS = "0123456789abcdef";
    private static final int AUTH_TOKEN_LENGTH = 32;

    public TvmToolOptions {
        if (connectionAuthToken.length() != AUTH_TOKEN_LENGTH) {
            throw new IllegalArgumentException("Auth token length must be exactly 32 symbols long");
        }
    }

    public static String generateAuthToken() {
        return Stream.generate(() -> ThreadLocalRandom.current().nextInt(DIGITS.length()))
                .map(DIGITS::charAt)
                .limit(AUTH_TOKEN_LENGTH)
                .map(Object::toString)
                .collect(Collectors.joining());
    }
}
