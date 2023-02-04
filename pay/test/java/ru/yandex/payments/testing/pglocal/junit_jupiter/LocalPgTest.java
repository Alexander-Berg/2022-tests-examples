package ru.yandex.payments.testing.pglocal.junit_jupiter;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Optional;

import lombok.val;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.payments.testing.pglocal.BinarySandboxSource;
import ru.yandex.payments.testing.pglocal.BinarySource;
import ru.yandex.payments.testing.pglocal.Database;
import ru.yandex.payments.testing.pglocal.DatabaseStatus;
import ru.yandex.payments.testing.pglocal.Manager;
import ru.yandex.payments.testing.pglocal.Manager.MasterOptions;
import ru.yandex.payments.testing.pglocal.Manager.SlaveOptions;
import ru.yandex.payments.testing.pglocal.MigrationSource;
import ru.yandex.payments.testing.pglocal.Server;
import ru.yandex.payments.testing.pglocal.SynchronousCommit;
import ru.yandex.payments.testing.pglocal.Version;

import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;

class LocalPgTest {
    private static final BinarySource PG_BINARY_SOURCE = new BinarySandboxSource(Version.V12);
    private static final MigrationSource MIGRATIONS = new MigrationSource.ResourceFolder("migrations");
    private static final String USER = "testuser";
    private static final String SLAVE_APP_NAME = "slave007";

    private static Manager manager;

    private static Path resolveServerPath(String name) {
        return SystemUtils.getJavaIoTmpDir()
                .toPath()
                .resolve(name);
    }

    private static boolean hasSyncedSlave(Server server) {
        val database = server.attachDatabase("postgres");
        return database.fetch("SELECT sync_state FROM pg_stat_replication", resultSet -> {
            try {
                while (resultSet.next()) {
                    val syncState = resultSet.getString("sync_state");
                    if ("async".equals(syncState)) {
                        return true;
                    }
                }
                return false;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @BeforeAll
    public static void init() {
        manager = new Manager(PG_BINARY_SOURCE);
    }

    @Test
    @DisplayName("Verify database could be started")
    void basicTest() {
        val serverDataPath = resolveServerPath("testdb");
        val options = new MasterOptions(Manager.randomPort(), USER, SynchronousCommit.OFF, emptyList());

        val master = manager.startNewServer(serverDataPath, options);
        try {
            master.createDatabase("testdb", Optional.of(MIGRATIONS));
            assertThat(master.status())
                    .isEqualTo(DatabaseStatus.STARTED)
                    .describedAs("Server expected to be started");
        } finally {
            master.purge();
        }
    }

    private static int readAge(Database db) {
        return db.fetch("SELECT age FROM users WHERE name = 'Bob'", rs -> {
            try {
                assertThat(rs.next()).isTrue();
                return rs.getInt("age");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("Verify database cluster, containing master and slave, could be started")
    void masterSyncSlaveTest() {
        val dbName = "durable_db";
        val masterDataPath = resolveServerPath("test_master_replica_db");
        val slaveDataPath = resolveServerPath("replica_db");

        val masterOptions = new MasterOptions(
                Manager.randomPort(),
                USER,
                SynchronousCommit.ON,
                emptyList()
        );

        val masterServer = manager.startNewServer(masterDataPath, masterOptions);

        val masterPort = masterOptions.port();
        val slaveOptions = new SlaveOptions(Manager.randomPort(), USER, SLAVE_APP_NAME, masterPort);
        val slaveServer = manager.startNewServer(slaveDataPath, slaveOptions);

        val masterDb = masterServer.attachDatabase(dbName);
        val slaveDb = slaveServer.attachDatabase(dbName);

        waitAtMost(20, SECONDS).until(() -> hasSyncedSlave(masterServer));
        masterServer.createDatabase(dbName, Optional.of(MIGRATIONS));
        masterDb.execute("INSERT INTO users VALUES (DEFAULT, 'Bob', 25)");

        waitAtMost(20, SECONDS)
                .ignoreExceptions()
                .until(
                        () -> readAge(slaveDb),
                        age -> age == 25
                );

        slaveServer.purge();
        masterServer.purge();
    }
}
