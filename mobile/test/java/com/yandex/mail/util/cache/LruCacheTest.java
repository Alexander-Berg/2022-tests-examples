package com.yandex.mail.util.cache;

import org.junit.Test;

import androidx.collection.LruCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class LruCacheTest {

    @Test
    public void shouldBeInstanceOfLruCache() {
        // For some reason .class.isAssignableFrom() not working
        assertThat(new com.yandex.mail.util.cache.LruCache(1)).isInstanceOf(LruCache.class);
    }

    @Test
    public void sizeOfShouldReturnSizeOfPassedValue() {
        Sizeable sizeable = mock(Sizeable.class);
        when(sizeable.size()).thenReturn(150);

        //noinspection unchecked
        int size = new com.yandex.mail.util.cache.LruCache(200).sizeOf(new Object(), sizeable);

        verify(sizeable).size();
        assertThat(size).isEqualTo(150);
    }
}
