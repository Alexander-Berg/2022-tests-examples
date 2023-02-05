package com.yandex.mail.util.rfc822;

import android.text.util.Rfc822Token;

import com.yandex.mail.util.Cache;

import org.junit.Test;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests specific to caching functionality of the Rfc822TokenParser
public final class Rfc822TokenParserWithCacheTest extends Rfc822TokenParserTest {

    @NonNull
    @Override
    protected final Rfc822TokenParser createParser() {
        return new Rfc822TokenParserWithCache(new Cache<String, List<Rfc822Token>>() {
            @Nullable
            @Override
            public List<Rfc822Token> get(@NonNull String s) {
                // no impl
                return null;
            }

            @Override
            public void put(@NonNull String s, @NonNull List<Rfc822Token> rfc822Tokens) {
                // no impl
            }
        });
    }

    @Test
    public void shouldPutValueIntoTheCache() {
        //noinspection unchecked
        Cache<String, List<Rfc822Token>> cache = mock(Cache.class);
        Rfc822TokenParser parser = new Rfc822TokenParserWithCache(cache);

        parser.parse("Neuman@BBN-TENEXA");
        //noinspection unchecked
        verify(cache).put(eq("Neuman@BBN-TENEXA"), any(List.class));
    }

    @Test
    public void shouldGetValueFromCache() {
        //noinspection unchecked
        Cache<String, List<Rfc822Token>> cache = mock(Cache.class);
        Rfc822TokenParser parser = new Rfc822TokenParserWithCache(cache);

        //noinspection unchecked
        List<Rfc822Token> tokens = mock(List.class);

        when(cache.get("Neuman@BBN-TENEXA"))
                .thenReturn(tokens);

        assertThat(parser.parse("Neuman@BBN-TENEXA")).isSameAs(tokens);

        verify(cache).get("Neuman@BBN-TENEXA");
    }
}
