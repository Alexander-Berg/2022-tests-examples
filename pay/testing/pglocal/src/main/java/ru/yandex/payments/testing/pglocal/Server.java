package ru.yandex.payments.testing.pglocal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Optional;

import lombok.Getter;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {
    private final Path data;
    @Getter
    private final int port;
    @Getter
    private final String user;
    @Getter
    private final ServerType type;
    private final Logger log;
    private final PgExecutor executor;
    private volatile boolean running;

    public Server(Path pgPath, Path data, int port, String user, ServerType type) {
        this.data = data;
        this.port = port;
        this.user = user;
        this.type = type;
        this.running = true;
        log = LoggerFactory.getLogger("pg_instance_" + data.getFileName());
        executor = new PgExecutor(pgPath, data, log);
        registerServerStop();
    }

    public DatabaseStatus status() {
        return executor.status(data);
    }

    public void start() {
        if (status() == DatabaseStatus.STOPPED) {
            executor.start(data, port, type);
        }
    }

    public void stop() {
        running = false;
        if (status() == DatabaseStatus.STARTED) {
            executor.stop(data, type);
        }
    }

    public Database createDatabase(String name, Optional<MigrationSource> migrations) {
        val tmpDb = attachDatabase("postgres");
        tmpDb.execute("CREATE DATABASE " + name);
        migrations.ifPresent(source -> applyMigrations(name, port, user, source));
        return new Database(name, port, user);
    }

    public Database attachDatabase(String name) {
        return new Database(name, port, user);
    }

    public Database attachDefaultDatabase() {
        return attachDatabase("postgres");
    }

    public void purge() {
        try {
            stop();
            FileUtils.forceDelete(data.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void applyMigrations(String database, int port, String user, MigrationSource migrations) {
        log.info("Apply migrations");
        val connString = String.format("jdbc:postgresql://localhost:%d/%s", port, database);
        val config = Flyway.configure()
                .dataSource(connString, user, "")
                .locations(migrations.flywayPaths());

        val flyway = new Flyway(config);
        val applied = flyway.migrate();
        log.info("{} migrations applied", applied);
    }

    private void registerServerStop() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (running) {
                    stop();
                }
            } catch (Exception ignored) {
            }
        }));
    }
}
