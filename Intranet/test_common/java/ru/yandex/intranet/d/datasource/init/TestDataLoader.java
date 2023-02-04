package ru.yandex.intranet.d.datasource.init;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;
import com.google.common.primitives.Longs;
import com.yandex.ydb.table.transaction.TransactionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import ru.yandex.intranet.d.datasource.impl.YdbQuerySource;
import ru.yandex.intranet.d.datasource.impl.YdbRetry;
import ru.yandex.intranet.d.datasource.migrations.model.MigrationType;
import ru.yandex.intranet.d.datasource.model.YdbSession;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.metrics.YdbMetrics;

/**
 * Test data loader.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@Component
public class TestDataLoader {

    private static final Logger LOG = LoggerFactory.getLogger(TestDataLoader.class);

    private final YdbQuerySource ydbQuerySource;
    private final YdbTableClient tableClient;
    private final YdbMetrics ydbMetrics;

    public TestDataLoader(YdbQuerySource ydbQuerySource, YdbTableClient tableClient, YdbMetrics ydbMetrics) {
        this.ydbQuerySource = ydbQuerySource;
        this.tableClient = tableClient;
        this.ydbMetrics = ydbMetrics;
    }

    public Mono<Void> applyCleanupTestData() {
        return Mono.fromRunnable(() -> LOG.info("Preparing for cleaning up test data..."))
                .then(getTestDataResources("classpath:db/cleanup/*.yql")
                        .doOnSuccess(m -> LOG.info("Total {} cleanups found", m.size()))
                        .flatMap(this::applyTestData)
                        .doOnSuccess(v -> LOG.info("Test data cleanup successfully applied")));
    }

    public Mono<Void> applyTestData() {
        return Mono.fromRunnable(() -> LOG.info("Preparing for applying test data..."))
                .then(getTestDataResources("classpath:db/test_data/*.yql")
                        .doOnSuccess(m -> LOG.info("Total {} test data found", m.size()))
                        .flatMap(this::applyTestData)
                        .doOnSuccess(v -> LOG.info("Test data successfully applied")));
    }

    public Mono<Void> applyTestDDL() {
        return Mono.fromRunnable(() -> LOG.info("Preparing for applying test data..."))
                .then(getTestDataResources("classpath:db/test_data/*.yql")
                        .doOnSuccess(m -> LOG.info("Total {} test data found", m.size()))
                        .flatMap(this::applyTestDDL)
                        .doOnSuccess(v -> LOG.info("Test data successfully applied")));
    }

    private Mono<Void> applyTestData(List<TestDataResource> migrations) {
        return tableClient.usingSessionMonoRetryable(session ->
                Mono.just(migrations.stream().sorted(Comparator.comparing(TestDataResource::getOrder))
                        .collect(Collectors.toList()))
                        .flatMapMany(Flux::fromIterable)
                        .concatMap(m -> applyTestData(session, m))
                        .thenEmpty(Mono.empty())).retryWhen(YdbRetry.retryTimeout(1, ydbMetrics));
    }

    private Mono<Void> applyTestDDL(List<TestDataResource> migrations) {
        return tableClient.usingSessionMonoRetryable(session ->
                Mono.just(migrations.stream()
                        .filter(r -> MigrationType.DDL == r.migrationType)
                        .sorted(Comparator.comparing(TestDataResource::getOrder))
                        .collect(Collectors.toList()))
                        .flatMapMany(Flux::fromIterable)
                        .concatMap(m -> applyTestData(session, m))
                        .thenEmpty(Mono.empty())).retryWhen(YdbRetry.retryTimeout(1, ydbMetrics));
    }

    private Mono<Void> applyTestData(YdbSession session, TestDataResource testData) {
        return Mono.fromRunnable(() -> LOG.info("Applying test data for order {}", testData.getOrder()))
                .then(testData.run(session)
                        .doOnSuccess(v -> LOG.info("Test data successfully applied for order {}",
                                testData.getOrder())));
    }

    private Mono<List<TestDataResource>> getTestDataResources(String pathPattern) {
        PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        return Mono.just(pathPattern)
                .publishOn(Schedulers.boundedElastic())
                .map(pattern -> getResources(resourceResolver, pattern))
                .<List<TestDataResource>>flatMap(resources -> {
                    if (resources.isEmpty()) {
                        return Mono.just(List.of());
                    }
                    return Flux.fromIterable(resources)
                            .publishOn(Schedulers.boundedElastic())
                            .map(this::getTestDataResource)
                            .collectList();
                }).flatMap(l -> {
                    List<Long> orders = l.stream().map(TestDataResource::getOrder).collect(Collectors.toList());
                    Set<Long> uniqueOrders = new HashSet<>(orders);
                    if (uniqueOrders.size() != orders.size()) {
                        return Mono.error(new IllegalStateException("Duplicate test data found"));
                    }
                    return Mono.just(l);
                });
    }

    private TestDataResource getTestDataResource(Resource resource) {
        String resourceName = resource.getFilename();
        if (resourceName == null) {
            throw new IllegalStateException("Invalid test data name: null");
        }
        List<String> parts = Splitter.on("-").splitToList(resourceName);
        if (parts.size() < 2 || parts.get(0).isEmpty() || parts.get(1).isEmpty()) {
            throw new IllegalStateException("Invalid test data name: " + resourceName);
        }
        Long version = Longs.tryParse(parts.get(0));
        if (version == null) {
            throw new IllegalStateException("Invalid test data name: " + resourceName);
        }
        String partType = parts.get(1);
        if (!partType.equalsIgnoreCase("ddl") && !partType.equalsIgnoreCase("dml")) {
            throw new IllegalStateException("Invalid test data name: " + resourceName);
        }
        MigrationType migrationType = partType.equalsIgnoreCase("ddl")
                ? MigrationType.DDL : MigrationType.DML;
        String text = resourceToString(resource);
        return new TestDataResource(text, version, migrationType);
    }

    private List<Resource> getResources(PathMatchingResourcePatternResolver resourceResolver,
                                        String locationPattern) {
        try {
            return List.of(resourceResolver.getResources(locationPattern));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String resourceToString(Resource resource) {
        try (InputStream stream = resource.getInputStream()) {
            return ydbQuerySource.preprocessRawQuery(StreamUtils.copyToString(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class TestDataResource {

        private final String text;
        private final long order;
        private final MigrationType migrationType;

        private TestDataResource(String text, long order, MigrationType migrationType) {
            this.text = text;
            this.order = order;
            this.migrationType = migrationType;
        }

        public Mono<Void> run(YdbSession session) {
            if (migrationType == MigrationType.DDL) {
                return session.executeSchemeQuery(text);
            } else {
                return session.executeDataQueryCommitRetryable(text, TransactionMode.SERIALIZABLE_READ_WRITE).then();
            }
        }

        public long getOrder() {
            return order;
        }

    }

}
