package ru.yandex.qe.cache;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: terry
 * Date: 05.07.13
 * Time: 20:58
 */
@ContextConfiguration({"classpath*:spring/qe-plugin-spring.xml", "classpath*:spring/qe-plugin-cache.xml", "classpath:spring/cache-ctx.xml"})
@ExtendWith(SpringExtension.class)
public class CacheConfigTest {

    @Inject
    private EhCacheCacheManager ehCacheCacheManager;

    @Inject
    private AnnotatedTestStorage annotatedTestStorage;

    @Inject
    private InjectedTestStorage injectedTestStorage;

    @BeforeEach
    void clearCaches() {
        ehCacheCacheManager.getCacheManager().clearAll();
    }

    @Test
    public void cache_hit() {
        checkCacheHit(annotatedTestStorage);
        checkCacheHit(injectedTestStorage);
    }

    private void checkCacheHit(BaseTestStorage baseTestStorage) {
        final Map keyValue = mock(HashMap.class);

        when(keyValue.put(any(), any())).thenReturn(null);
        when(keyValue.get(any())).thenReturn("test");

        baseTestStorage.setKeyValue(keyValue);

        baseTestStorage.put(1, "test");

        baseTestStorage.getData(1);
        baseTestStorage.getData(1);
        baseTestStorage.getData(1);

        verify(keyValue, atLeastOnce()).put(any(), any());
        verify(keyValue, atLeastOnce()).get(any());
        verifyNoMoreInteractions(keyValue);
    }

    @Test
    public void cache_miss() {
        checkCacheMiss(annotatedTestStorage);
        checkCacheMiss(injectedTestStorage);

    }

    private void checkCacheMiss(BaseTestStorage baseTestStorage) {
        final Map keyValue = mock(HashMap.class);

        when(keyValue.put(any(), any())).thenReturn(null);
        when(keyValue.get(any())).thenReturn("test");

        baseTestStorage.setKeyValue(keyValue);

        baseTestStorage.getData(1);
        baseTestStorage.getData(1);

        baseTestStorage.put(1, "test");

        baseTestStorage.getData(1);
        baseTestStorage.getData(1);

        verify(keyValue, atLeastOnce()).put(any(), any());
        verify(keyValue, times(2)).get(any());
        verifyNoMoreInteractions(keyValue);
    }
}
