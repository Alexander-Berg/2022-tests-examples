package ru.yandex.partner.test.db;

import java.time.Duration;

import org.springframework.context.Lifecycle;

import ru.yandex.direct.mysql.MySQLInstance;
import ru.yandex.direct.test.mysql.DirectMysqlDb;
import ru.yandex.direct.test.mysql.TestMysqlConfig;

public class TestDbInitializer implements Lifecycle {
    private static final Duration DEFAULT_DURATION = Duration.ofSeconds(60);

    private boolean running;
    private MySQLInstance connector;
    private TestMysqlConfig config;

    public TestDbInitializer(TestMysqlConfig config) {
        this.config = config;
        startInternal();
    }

    public MySQLInstance getConnector() {
        return connector;
    }

    @Override
    public void start() {
        startInternal();
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        connector.close();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void startInternal() {
        if (running) {
            return;
        }
        DirectMysqlDb testDb = new DirectMysqlDb(config);
        try {
            connector = testDb.start();
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
            throw new DbInitException("Thread interrupted", exc);
        }
        connector.awaitConnectivity(DEFAULT_DURATION);
        running = true;
    }
}
