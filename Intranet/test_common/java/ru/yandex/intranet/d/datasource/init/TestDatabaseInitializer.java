package ru.yandex.intranet.d.datasource.init;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ru.yandex.intranet.d.datasource.migrations.impl.MigrationsRunner;

/**
 * Test database initializer.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@Component
public class TestDatabaseInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(TestDatabaseInitializer.class);

    private final MigrationsRunner migrationsRunner;
    private final TestDataLoader testDataLoader;

    public TestDatabaseInitializer(MigrationsRunner migrationsRunner, TestDataLoader testDataLoader) {
        this.migrationsRunner = migrationsRunner;
        this.testDataLoader = testDataLoader;
    }

    @PostConstruct
    public void postConstruct() {
        LOG.info("Preparing database...");
        migrationsRunner
                .doBootstrap()
                .then(migrationsRunner.applyMigrations())
                .then(testDataLoader.applyTestDDL())
                .then(testDataLoader.applyCleanupTestData())
                .then(testDataLoader.applyTestData()).block();
        LOG.info("Database ready");
    }

}
