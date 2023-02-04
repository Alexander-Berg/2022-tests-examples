package ru.yandex.infra.stage.cache;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.TSecret;
import ru.yandex.infra.stage.dto.Secret;
import ru.yandex.infra.stage.protobuf.Converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.controller.testutil.FutureUtils.get1s;

public class LocalFsCacheStorageTests {

    private static final CachedObjectType<Secret, TSecret> MY_TYPE = new CachedObjectType<>("mytype", TSecret::newBuilder,
            Converter::fromProto, Converter::toProto);
    private String cacheFolder;

    private TSecret val(String value) {
        return TSecret.newBuilder()
                .setId(value)
                .setVersion("version")
                .setDelegationToken("token")
                .build();
    }

    @BeforeEach
    void before() throws IOException {
        cacheFolder = Files.createTempDirectory("cache").toAbsolutePath().toString();
    }

    @AfterEach
    void after() throws IOException {
        FileUtils.deleteDirectory(new File(cacheFolder));
    }

    @Test
    void initFromScratchTest() {
        LocalFsCacheStorage<TSecret> storage = new LocalFsCacheStorage<>(cacheFolder, MY_TYPE, Duration.ZERO);
        get1s(storage.init());
        var map = get1s(storage.read());
        assertThat(map, anEmptyMap());
    }

    @Test
    void flushAndReloadTest() {
        final Map<String, TSecret> map = Map.of(
                "key1", val("value1"),
                "key2", val("value2")
        );

        LocalFsCacheStorage<TSecret> storage = new LocalFsCacheStorage<>(cacheFolder, MY_TYPE, Duration.ZERO);
        get1s(storage.init());
        get1s(storage.write(map));
        get1s(storage.flush());

        LocalFsCacheStorage<TSecret> storage2 = new LocalFsCacheStorage<>(cacheFolder, MY_TYPE, Duration.ZERO);
        get1s(storage2.init());
        var map2 = get1s(storage.read());

        assertThat(map2, equalTo(map));
    }

    @Test
    void writeAndRemoveTest() {
        LocalFsCacheStorage<TSecret> storage = new LocalFsCacheStorage<>(cacheFolder, MY_TYPE, Duration.ZERO);
        get1s(storage.init());

        storage.write("key", val("value"));
        var map = get1s(storage.read());
        assertThat(map, equalTo(Map.of("key", val("value"))));

        get1s(storage.remove("key"));
        map = get1s(storage.read());
        assertThat(map, anEmptyMap());

        //No error after second remove
        get1s(storage.remove("key"));
    }

}
