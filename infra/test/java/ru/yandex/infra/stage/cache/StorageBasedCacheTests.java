package ru.yandex.infra.stage.cache;

import com.google.protobuf.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.metrics.MapGaugeRegistry;
import ru.yandex.infra.stage.TSecret;
import ru.yandex.infra.stage.dto.Secret;
import ru.yandex.infra.stage.dto.SecretRef;
import ru.yandex.infra.stage.protobuf.Converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class StorageBasedCacheTests {

    private CacheStorageFactory storageFactory;
    private StorageBasedCache<Secret, TSecret> cache;
    private DummyCacheStorage<TSecret> dummyStorage = new DummyCacheStorage<>();
    private MapGaugeRegistry metrics;

    @BeforeEach
    void before() {
        metrics = new MapGaugeRegistry();
        storageFactory = new CacheStorageFactory() {
            @Override
            public <TValue, TProtoValue extends Message> CacheStorage<TProtoValue> createStorage(
                    CachedObjectType<TValue, TProtoValue> cachedObjectType) {
                return (CacheStorage<TProtoValue>) dummyStorage;
            }
        };
        CachedObjectType<Secret, TSecret> mytype = new CachedObjectType<>("mytype", TSecret::newBuilder, Converter::fromProto, Converter::toProto);

        cache = new StorageBasedCache<>(mytype, storageFactory, metrics);
    }

    private Object getMetric(String name) {
        return metrics.getGaugeValue("cache.mytype." + name);
    }

    private Secret val(String value) {
        return new Secret(new SecretRef("uuid", "ver1"), value);
    }

    @Test
    void dontWriteToStorageIfValueNotChanged() {
        cache.put("key", val("1"));
        assertThat(dummyStorage.writeCount, equalTo(1));
        assertThat(getMetric(StorageBasedCache.METRIC_PUT), equalTo(1L));

        cache.put("key", val("1"));
        assertThat(dummyStorage.writeCount, equalTo(1));
        assertThat(getMetric(StorageBasedCache.METRIC_PUT), equalTo(2L));
        assertThat(cache.get("key").get(), equalTo(val("1")));
    }

    @Test
    void writeToStorageIfValueChanged() {
        cache.put("key", val("1"));
        assertThat(dummyStorage.writeCount, equalTo(1));
        assertThat(getMetric(StorageBasedCache.METRIC_PUT), equalTo(1L));

        cache.put("key", val("3"));
        assertThat(dummyStorage.writeCount, equalTo(2));
        assertThat(getMetric(StorageBasedCache.METRIC_PUT), equalTo(2L));
        assertThat(cache.get("key").get(), equalTo(val("3")));
    }
}
