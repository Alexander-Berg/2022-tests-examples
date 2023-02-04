package ru.yandex.payments.testing.pglocal.micronaut;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;

import io.micronaut.context.exceptions.ConfigurationException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.SystemUtils;

import ru.yandex.payments.testing.pglocal.BinarySandboxSource;
import ru.yandex.payments.testing.pglocal.Manager;
import ru.yandex.payments.testing.pglocal.Server;
import ru.yandex.payments.testing.pglocal.SynchronousCommit;

import static java.util.Collections.emptyList;
import static org.awaitility.Awaitility.waitAtMost;
import static ru.yandex.payments.util.CollectionUtils.mapToList;

class ClusterManager {
    private static final String SINGLE_SERVER_FOLDER = "single-pg-server";
    private static final Duration SLAVE_WAIT_TIMEOUT = Duration.ofSeconds(30);

    private final Manager manager;
    @Getter(AccessLevel.PACKAGE)
    private final Cluster cluster;

    ClusterManager(PgLocalConfiguration configuration, PgLocalInstanceConfiguration[] instanceConfigurations) {
        if (instanceConfigurations.length == 0) {
            throw new ConfigurationException("pg-local.cluster needs to contain at least one instance");
        }

        val masterConfig = StreamEx.of(instanceConfigurations)
                .findAny(PgLocalInstanceConfiguration::isMaster)
                .orElseThrow(() -> new ConfigurationException("pg-local.cluster needs to have at least one master"));

        val slaveConfigs = StreamEx.of(instanceConfigurations)
                .remove(PgLocalInstanceConfiguration::isMaster)
                .toImmutableList();

        if (instanceConfigurations.length - slaveConfigs.size() != 1) {
            throw new ConfigurationException("Multi-master configuration is not possible");
        }

        val binarySource = new BinarySandboxSource(configuration.pgVersion());

        this.manager = new Manager(binarySource);
        if (slaveConfigs.isEmpty()) {
            val masterServer = startSingleServer(configuration);
            this.cluster = new Cluster(new Cluster.Node(masterServer, masterConfig.name()), emptyList());
        } else {
            this.cluster = startCluster(configuration, masterConfig, slaveConfigs);
            waitForSlaves(cluster.slaves().size());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (Throwable ignored) {
            }
        }));
    }

    private int fetchActiveSlavesCount() {
        val db = cluster.master().server().attachDatabase("postgres");
        return db.fetch("SELECT sync_state FROM pg_stat_replication", resultSet -> {
            int count = 0;
            try {
                while (resultSet.next()) {
                    val syncState = resultSet.getString("sync_state");
                    if ("async".equals(syncState)) {
                        ++count;
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return count;
        });
    }

    private void waitForSlaves(int expectedSlavesCount) {
        if (expectedSlavesCount == 0) {
            return;
        }

        waitAtMost(SLAVE_WAIT_TIMEOUT)
                .ignoreExceptions()
                .until(
                        this::fetchActiveSlavesCount,
                        slavesCount -> slavesCount == expectedSlavesCount
                );
    }

    private static Path resolveServerPath(String name) {
        return SystemUtils.getJavaIoTmpDir()
                .toPath()
                .resolve(name);
    }

    private Server startSingleServer(PgLocalConfiguration configuration) {
        return startMaster(configuration);
    }

    private Server startMaster(PgLocalConfiguration configuration) {
        val masterDataPath = resolveServerPath(SINGLE_SERVER_FOLDER + "-master");
        val options = new Manager.MasterOptions(
                Manager.randomPort(),
                configuration.user(),
                SynchronousCommit.OFF,
                emptyList()
        );
        return manager.startNewServer(masterDataPath, options);
    }

    private Server startSlave(PgLocalConfiguration configuration, PgLocalInstanceConfiguration slaveConfig,
                              int masterPort) {
        val appName = "slave_cluster_" + slaveConfig.name();
        val slaveDataPath = resolveServerPath(SINGLE_SERVER_FOLDER + "-" + appName);

        val slaveOptions = new Manager.SlaveOptions(
                Manager.randomPort(),
                configuration.user(),
                appName,
                masterPort
        );
        return manager.startNewServer(slaveDataPath, slaveOptions);
    }

    private Cluster startCluster(PgLocalConfiguration configuration, PgLocalInstanceConfiguration masterConfig,
                                 List<PgLocalInstanceConfiguration> slaveConfigs) {
        val master = startMaster(configuration);
        final var slaves = mapToList(slaveConfigs, cfg -> {
            val slaveServer = startSlave(configuration, cfg, master.getPort());
            return new Cluster.Node(slaveServer, cfg.name());
        });
        return new Cluster(new Cluster.Node(master, masterConfig.name()), slaves);
    }

    private void stop() {
        cluster.master().server().purge();
        cluster.slaves()
                .stream()
                .map(Cluster.Node::server)
                .forEach(Server::purge);
    }
}
