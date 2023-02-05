package com.yandex.mail.util.rfc822;

import androidx.annotation.NonNull;

public final class ConcurrentRfc822TokenParserWithCacheTest extends Rfc822TokenParserTest {
    @Override
    @NonNull
    protected Rfc822TokenParser createParser() {
        return Rfc822TokenParsers.newConcurrentParserWithCache(10);
    }
}
