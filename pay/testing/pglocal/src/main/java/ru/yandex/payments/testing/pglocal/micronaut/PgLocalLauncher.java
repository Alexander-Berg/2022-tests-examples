package ru.yandex.payments.testing.pglocal.micronaut;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import lombok.val;
import one.util.streamex.StreamEx;

import ru.yandex.payments.testing.pglocal.MigrationSource;
import ru.yandex.payments.testing.pglocal.Server;
import ru.yandex.payments.util.Lazy;

import static ru.yandex.payments.testing.pglocal.micronaut.PgLocalConfiguration.DB_NAME_PROPERTY;
import static ru.yandex.payments.util.CollectionUtils.mapToList;

@Context
@Requires(env = Environment.TEST, beans = PgLocalConfiguration.class)
@SuppressWarnings("HideUtilityClassConstructor")
public class PgLocalLauncher {
    private static final Lazy<ClusterManager> CLUSTER_MANAGER = new Lazy<>();
    private static final String PROPERTY_SOURCE = "pglocal";
    private static final String CLUSTER_PREFIX = "local-pg.cluster";

    @Inject
    public PgLocalLauncher(PgLocalConfiguration configuration, PgLocalInstanceConfiguration[] instanceConfigurations,
                           ApplicationContext context,
                           @Value("${" + DB_NAME_PROPERTY + "}") Optional<String> dbName) {
        if (dbName.isEmpty()) {
            return;
        }

        val manager = CLUSTER_MANAGER.getOrCompute(() -> new ClusterManager(configuration, instanceConfigurations));

        val master = manager.getCluster().master();
        val migrations = prepareMigrations(configuration);
        master.server().createDatabase(dbName.get(), Optional.of(migrations));

        val portConfig = StreamEx.of(manager.getCluster().slaves())
                .append(master)
                .mapToEntry(Cluster.Node::name, slave -> slave.server().getPort())
                .mapKeys(name -> CLUSTER_PREFIX + "." + name + ".port")
                .mapValues(Object.class::cast)
                .toImmutableMap();

        context.getEnvironment().addPropertySource(PROPERTY_SOURCE, portConfig);
    }

    private static MigrationSource prepareMigrations(PgLocalConfiguration configuration) {
        val migrationsCfg = configuration.migrations();
        val folder = migrationsCfg.folder();
        val resourceFolder = migrationsCfg.resourceFolder();

        if (folder != null) {
            return new MigrationSource.Folder(folder);
        } else {
            return new MigrationSource.ResourceFolder(resourceFolder);
        }
    }

    private static Optional<Cluster> getCluster() {
        return Optional.ofNullable(CLUSTER_MANAGER.getOrCompute(() -> null))
                .map(ClusterManager::getCluster);
    }

    public static Optional<Server> getMaster() {
        return getCluster()
                .map(Cluster::master)
                .map(Cluster.Node::server);
    }

    public static List<Server> getSlaves() {
        return getCluster()
                .map(Cluster::slaves)
                .map(slaves -> mapToList(slaves, Cluster.Node::server))
                .orElseGet(Collections::emptyList);
    }
}
