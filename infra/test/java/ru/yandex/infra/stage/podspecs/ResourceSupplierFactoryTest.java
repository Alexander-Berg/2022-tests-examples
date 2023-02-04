package ru.yandex.infra.stage.podspecs;

import java.time.Duration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.controller.metrics.MapGaugeRegistry;
import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.cache.DummyCache;
import ru.yandex.infra.stage.concurrent.SerialExecutor;
import ru.yandex.infra.stage.dto.Checksum;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.util.SettableClock;

import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatSameInstance;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatThrowsWithMessage;

class ResourceSupplierFactoryTest {
    private static final String RESOURCE_NAME = "resource";
    private static final String RESOURCE_URL = "resource";
    private static final String RESOURCE_CHECKSUM = "MD5:abcde";
    private static final Config WITH_CHECKSUM = ConfigFactory.empty()
            .withValue(RESOURCE_NAME, ConfigValueFactory.fromMap(ImmutableMap.of(
                    "source", "config",
                    "url", RESOURCE_URL,
                    "checksum", RESOURCE_CHECKSUM
            )));
    private static final Config WITHOUT_CHECKSUM = ConfigFactory.empty()
            .withValue(RESOURCE_NAME, ConfigValueFactory.fromMap(ImmutableMap.of(
                    "source", "config",
                    "url", RESOURCE_URL
            )));

    private SerialExecutor serialExecutor;
    private ResourceSupplierFactory factory;
    private MapGaugeRegistry registry;
    private SettableClock clock;

    @BeforeEach
    void before() {
        serialExecutor = new SerialExecutor(getClass().getName());
        clock = new SettableClock();
        registry = new MapGaugeRegistry();
        factory = new ResourceSupplierFactory(serialExecutor, new DummyReleaseGetter(), Duration.ofSeconds(1),
                Duration.ofSeconds(1), clock, registry, new DummyCache<>());
    }

    @AfterEach
    void after() {
        serialExecutor.shutdown();
    }

    @Test
    void configResourceWithoutChecksum() {
        FixedResourceSupplier supplier = (FixedResourceSupplier)factory.create(WITHOUT_CHECKSUM, RESOURCE_NAME, false);
        assertThat(supplier.get().getResource(), equalTo(new DownloadableResource(RESOURCE_URL, Checksum.EMPTY)));
    }

    @Test
    void configResourceWithChecksum() {
        FixedResourceSupplier supplier = (FixedResourceSupplier)factory.create(WITH_CHECKSUM, RESOURCE_NAME, true);
        assertThat(supplier.get().getResource(), equalTo(new DownloadableResource(RESOURCE_URL, Checksum.fromString(RESOURCE_CHECKSUM))));
    }

    @Test
    void configResourceWithMeta() {
        Config config = WITHOUT_CHECKSUM.withValue(RESOURCE_NAME + "." + "sandbox_info", ConfigValueFactory.fromMap(ImmutableMap.of(
                "task_id", TestData.RESOURCE_META.getTaskId(),
                "resource_id", TestData.RESOURCE_META.getResourceId(),
                "attributes", TestData.SANDBOX_ATTRIBUTES
        )));
        FixedResourceSupplier supplier = (FixedResourceSupplier)factory.create(config, RESOURCE_NAME, false);
        assertThat(supplier.get().getMeta(), optionalWithValue(equalTo(TestData.RESOURCE_META)));
    }

    @Test
    void failForUnexpectedChecksum() {
        assertThatThrowsWithMessage(RuntimeException.class,
                String.format("Checksum should not be set for resource '%s'", RESOURCE_NAME),
                () -> factory.create(WITH_CHECKSUM, RESOURCE_NAME, false)
        );
    }

    @Test
    void failForMissingChecksum() {
        assertThatThrowsWithMessage(RuntimeException.class,
                "hardcoded value: No configuration setting found for key 'checksum'",
                () -> factory.create(WITHOUT_CHECKSUM, RESOURCE_NAME, true)
        );
    }

    @Test
    void createDelayMetricForResource() {
        factory.create(WITH_CHECKSUM, RESOURCE_NAME, true);
        factory.startAll(Duration.ofSeconds(1));
        String gaugeName = String.format("resources.%s.delay_seconds", RESOURCE_NAME);
        clock.incrementSecond();
        assertThat(registry.getGaugeValue(gaugeName), equalTo(0L));
    }

    @Test
    void failForCreatingTwice() {
        factory.create(WITHOUT_CHECKSUM, RESOURCE_NAME, false);

        assertThatThrowsWithMessage(RuntimeException.class,
                String.format("Supplier for resource '%s' has already been created", RESOURCE_NAME),
                () -> factory.create(WITHOUT_CHECKSUM, RESOURCE_NAME, false)
        );
    }

    private static Iterable<Arguments> provideParametersForCreateIfAbsent() {
        var argumentsCollectionBuilder = ImmutableList.<Arguments>builder();

        for (Config config : ImmutableList.of(WITHOUT_CHECKSUM, WITH_CHECKSUM)) {
            for (boolean useChecksum: ImmutableList.of(false, true)) {
                argumentsCollectionBuilder.add(
                        Arguments.of(config, useChecksum)
                );
            }
        }

        return argumentsCollectionBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("provideParametersForCreateIfAbsent")
    void testCreateIfAbsent(Config getConfig, boolean getUseChecksum) {
        var supplierWasCreated = factory.createIfAbsent(WITHOUT_CHECKSUM, RESOURCE_NAME, false);
        var supplierWasGot = factory.createIfAbsent(getConfig, RESOURCE_NAME, getUseChecksum);
        assertThatSameInstance(supplierWasCreated, supplierWasGot);
    }
}
