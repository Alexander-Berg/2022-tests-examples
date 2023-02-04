package ru.yandex.infra.stage.cache;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.TSecret;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.controller.testutil.FutureUtils.get1s;

public class ReadOnlyCacheStorageTest {

    @Test
    void writeOperationsSkipped() {
        DummyCacheStorage<TSecret> storage = new DummyCacheStorage<>();
        CacheStorage<TSecret> readOnlyStorage = new ReadonlyCacheStorage<>(storage);

        get1s(readOnlyStorage.init());
        assertThat(storage.initCount, equalTo(1));

        get1s(readOnlyStorage.write("key", null));
        assertThat(storage.writeCount, equalTo(0));
        get1s(storage.write("key", null));
        assertThat(storage.writeCount, equalTo(1));

        final Map<String, TSecret> map = Map.of(
                "key", TSecret.newBuilder().build(),
                "key2", TSecret.newBuilder().build()
        );
        get1s(readOnlyStorage.write(map));
        assertThat(storage.writeCount, equalTo(1));
        get1s(storage.write(map));
        assertThat(storage.writeCount, equalTo(2));

        get1s(readOnlyStorage.flush());
        assertThat(storage.flushCount, equalTo(0));
        get1s(storage.flush());
        assertThat(storage.flushCount, equalTo(1));

        get1s(readOnlyStorage.remove("key"));
        assertThat(storage.removeCount, equalTo(0));
        get1s(storage.remove("key"));
        assertThat(storage.removeCount, equalTo(1));
    }

    @Test
    void readTest() {
        DummyCacheStorage<TSecret> storage = new DummyCacheStorage<>();
        CacheStorage<TSecret> readOnlyStorage = new ReadonlyCacheStorage<>(storage);

        final TSecret secret = TSecret.newBuilder().build();
        storage.readResult.put("mykey", secret);

        var map = get1s(readOnlyStorage.read());
        assertThat(storage.readCount, equalTo(1));
        assertThat(map, equalTo(Map.of("mykey", secret)));
    }

}
