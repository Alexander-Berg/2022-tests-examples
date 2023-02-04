package ru.yandex.solomon.alert;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.net.HostAndPort;
import com.yandex.ydb.core.grpc.GrpcTransport;
import com.yandex.ydb.table.SchemeClient;
import com.yandex.ydb.table.TableClient;
import com.yandex.ydb.table.rpc.grpc.GrpcSchemeRpc;
import com.yandex.ydb.table.rpc.grpc.GrpcTableRpc;

import ru.yandex.discovery.DiscoveryServices;
import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.dao.AlertStatesDao;
import ru.yandex.solomon.alert.dao.DuplicateKeyException;
import ru.yandex.solomon.alert.dao.EntitiesDao;
import ru.yandex.solomon.alert.dao.ydb.YdbAlertStatesDaoFactory;
import ru.yandex.solomon.alert.dao.ydb.YdbAlertsDaoFactory;
import ru.yandex.solomon.alert.dao.ydb.YdbNotificationsDaoFactory;
import ru.yandex.solomon.alert.dao.ydb.YdbSchemaVersion;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.notification.domain.Notification;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.core.db.dao.ProjectsDao;
import ru.yandex.solomon.core.db.dao.ydb.YdbProjectsDao;
import ru.yandex.solomon.core.db.model.Project;
import ru.yandex.solomon.idempotency.IdempotentOperation;


/**
 * @author Vladimir Gordiychuk
 */
public class MigrateFromKikimrToKikimr {

    public static void main(String[] args) {
        Unit source = Unit.create("conductor_group://solomon_prod_kfront:2135", "/Kfront/Solomon/Dev");
        Unit target = Unit.create("conductor_group://kikimr_man_slice_18:2135", "/Root/Solomon/Dev");

        List<String> projects = source.projectsDao.findAllNames()
            .join()
            .stream()
            .map(Project::getId)
            .collect(Collectors.toList());

        projects.parallelStream()
                .map(projectId -> migrateAlerts(projectId, source, target)
                        .thenCompose(ignore -> migrateNotifications(projectId, source, target))
                        .thenCompose(ignore -> migrateStates(projectId, source, target))
                        .thenRun(() -> System.out.println(projectId + ": done")))
                .collect(Collectors.collectingAndThen(Collectors.toList(), CompletableFutures::allOfUnit))
                .join();

        System.out.println("Done!");
        source.close();
        target.close();
    }

    private static CompletableFuture<?> migrateAlerts(String projectId, Unit source, Unit target) {
        var sourceDao = source.alertsDao;
        var targetDao = target.alertsDao;

        return targetDao.createSchemaForTests()
                .thenCompose(ignore -> sourceDao.findAll(projectId))
                .thenCompose(alerts -> alerts.parallelStream()
                        .map(alert -> targetDao.insert(alert, IdempotentOperation.NO_OPERATION)
                                .exceptionally(e -> {
                                    if (CompletableFutures.unwrapCompletionException(e) instanceof DuplicateKeyException) {
                                        return null;
                                    } else {
                                        throw Throwables.propagate(e);
                                    }
                                }))
                        .collect(Collectors.collectingAndThen(Collectors.toList(), CompletableFutures::allOfUnit)));
    }

    private static CompletableFuture<?> migrateNotifications(String projectId, Unit source, Unit target) {
        var sourceDao = source.notificationsDao;
        var targetDao = target.notificationsDao;

        return targetDao.createSchema(projectId)
                .thenCompose(ignore -> sourceDao.findAll(projectId))
                .thenCompose(notifications -> notifications.parallelStream()
                        .map(notification -> targetDao.insert(notification, IdempotentOperation.NO_OPERATION)
                                .exceptionally(e -> {
                                    if (CompletableFutures.unwrapCompletionException(e) instanceof DuplicateKeyException) {
                                        return null;
                                    } else {
                                        throw Throwables.propagate(e);
                                    }
                                }))
                        .collect(Collectors.collectingAndThen(Collectors.toList(), CompletableFutures::allOfUnit)));
    }

    private static CompletableFuture<?> migrateStates(String projectId, Unit source, Unit target) {
        AlertStatesDao sourceDao = source.statesDao;
        AlertStatesDao targetDao = target.statesDao;

        return targetDao.createSchema(projectId)
                .thenCompose(ignore -> sourceDao.findAll(projectId))
                .thenCompose(states -> targetDao.save(projectId, Instant.now(), new AssignmentSeqNo(1, 1), states));
    }

    private static class Unit implements AutoCloseable {
        private final TableClient tableClient;
        private final EntitiesDao<Alert> alertsDao;
        private final EntitiesDao<Notification> notificationsDao;
        private final AlertStatesDao statesDao;
        private final ProjectsDao projectsDao;

        private Unit(TableClient tableClient, SchemeClient schemeClient, String root) {
            this.tableClient = tableClient;
            ObjectMapper mapper = new ObjectMapper();
            this.alertsDao = YdbAlertsDaoFactory.create(root, tableClient, schemeClient, YdbSchemaVersion.CURRENT, mapper);
            this.notificationsDao = YdbNotificationsDaoFactory.create(root, tableClient, schemeClient, YdbSchemaVersion.CURRENT, mapper);
            this.statesDao = YdbAlertStatesDaoFactory.create(root, tableClient, schemeClient, YdbSchemaVersion.CURRENT, new MetricRegistry());
            this.projectsDao = new YdbProjectsDao(tableClient, root + "/Config/V2/Project", mapper, ForkJoinPool.commonPool());
        }

        public CompletableFuture<?> createSchema() {
            return alertsDao.createSchemaForTests()
                .thenCompose(ignore -> notificationsDao.createSchemaForTests())
                .thenCompose(ignore -> statesDao.createSchemaForTests());
        }

        private static Unit create(String address, String root) {
            List<HostAndPort> addresses = DiscoveryServices.resolve(List.of(address));
            var transport = GrpcTransport.forHosts(addresses).build();
            TableClient tableClient = TableClient.newClient(GrpcTableRpc.ownTransport(transport)).build();
            SchemeClient schemeClient = SchemeClient.newClient(GrpcSchemeRpc.useTransport(transport)).build();

            return new Unit(tableClient, schemeClient, root);
        }

        @Override
        public void close() {
            tableClient.close();
        }
    }

}
