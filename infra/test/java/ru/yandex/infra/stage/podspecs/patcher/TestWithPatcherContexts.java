package ru.yandex.infra.stage.podspecs.patcher;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import org.junit.jupiter.api.BeforeEach;

import ru.yandex.infra.stage.podspecs.ResourceSupplier;
import ru.yandex.infra.stage.podspecs.ResourceSupplierFactory;
import ru.yandex.infra.stage.podspecs.ResourceWithMeta;
import ru.yandex.infra.stage.podspecs.SandboxReleaseGetter;

import static org.mockito.Mockito.mock;

public abstract class TestWithPatcherContexts extends TestWithPatcherConfigs {

    static class DummyResourceSupplier implements ResourceSupplier {

        private final Config config;
        private final String name;
        private final boolean useChecksum;

        DummyResourceSupplier(Config config, String name, boolean useChecksum) {
            this.config = config;
            this.name = name;
            this.useChecksum = useChecksum;
        }

        @Override
        public ResourceWithMeta get() {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<?> start() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Duration timeSinceLastUpdate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SandboxReleaseGetter getSandboxReleaseGetter() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DummyResourceSupplier that = (DummyResourceSupplier) o;
            return useChecksum == that.useChecksum &&
                    Objects.equals(config, that.config) &&
                    Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(config, name, useChecksum);
        }
    }

    private static class DummyResourceSupplierFactoryHolder implements ResourceSupplierFactoryHolder {

        private static final ResourceSupplierFactory dummyResourceSupplierFactory = mock(ResourceSupplierFactory.class);

        @Override
        public ResourceSupplierFactory getResourceSupplierFactory() {
            return dummyResourceSupplierFactory;
        }

        @Override
        public ResourceSupplier createIfAbsent(Config config, String resourceName, boolean useChecksum) {
            return new DummyResourceSupplier(config, resourceName, useChecksum);
        }
    }

    PatcherParameters parameters;

    protected PatcherContexts contexts;

    @BeforeEach
    public void beforeTest() {
        var resourceSupplierFactoryHolder = new DummyResourceSupplierFactoryHolder();
        parameters = PatcherParameters.with(
                Optional.of("pod"), Optional.of("logbroker"), Optional.of("tvm"),
                Optional.of(ImmutableList.of("id1", "id2")),
                -1,
                ImmutableMap.of()
        );

        contexts = new PatcherContextsFactory(CORRECT_PATCHERS_CONFIG, resourceSupplierFactoryHolder, parameters).create();
    }
}
