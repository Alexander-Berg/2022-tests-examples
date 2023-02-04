package ru.yandex.infra.stage;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Try;
import ru.yandex.infra.controller.concurrent.DummyLeaderService;
import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.metrics.GaugeRegistry;
import ru.yandex.infra.controller.metrics.MapGaugeRegistry;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.controller.yp.YpObjectSettings;
import ru.yandex.infra.stage.concurrent.SerialExecutor;
import ru.yandex.infra.stage.deployunit.LogbrokerTopicConfigResolver;
import ru.yandex.infra.stage.deployunit.Readiness;
import ru.yandex.infra.stage.deployunit.SandboxResourcesResolver;
import ru.yandex.infra.stage.docker.DockerImagesResolver;
import ru.yandex.infra.stage.dto.ClusterAndType;
import ru.yandex.infra.stage.inject.ControllerFactoryImpl;
import ru.yandex.infra.stage.inject.GCLimit;
import ru.yandex.infra.stage.inject.GCSettings;
import ru.yandex.infra.stage.inject.ObjectLifeCycleManagerFactory;
import ru.yandex.infra.stage.podspecs.PodSpecCompositePatcher;
import ru.yandex.infra.stage.podspecs.patcher.dynamic_resource.DynamicResourceResolvedResourcesPatcher;
import ru.yandex.infra.stage.podspecs.patcher.endpoint_set_liveness.EndpointSetLivenessPatcherV1;
import ru.yandex.infra.stage.podspecs.revision.RevisionsHolderImpl;
import ru.yandex.infra.stage.protobuf.Converter;
import ru.yandex.infra.stage.yp.AclUpdater;
import ru.yandex.infra.stage.yp.AsyncYpClientsMap;
import ru.yandex.infra.stage.yp.EpochDecoratorRepositoryFactory;
import ru.yandex.infra.stage.yp.RelationController;
import ru.yandex.infra.stage.yp.Retainment;
import ru.yandex.yp.YpRawObjectService;
import ru.yandex.yp.client.api.TProjectSpec;
import ru.yandex.yp.client.api.TProjectStatus;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;
import ru.yandex.yp.model.YpObjectType;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ControllersIT {
    private SerialExecutor serialExecutor;
    private ScheduledExecutorService executor;

    static final GaugeRegistry gaugeRegistry = GaugeRegistry.EMPTY;

    @BeforeEach
    void before() {
        serialExecutor = new SerialExecutor(getClass().getSimpleName());
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterEach
    void after() {
        serialExecutor.shutdown();
        executor.shutdownNow();
    }

    @Test
    void retainObject() {
        Clock clock = Clock.systemDefaultZone();
        Converter converter = TestData.CONVERTER;
        String cluster = TestData.REPLICA_SET_UNIT_SPEC.extractClusters().iterator().next();

        YpRawObjectService objectService = Mockito.mock(YpRawObjectService.class);
        when(objectService.getObject(any(), any())).thenReturn(new CompletableFuture<>());

        AsyncYpClientsMap clients =
                new AsyncYpClientsMap(ImmutableMap.of(cluster, objectService), objectService);

        EpochDecoratorRepositoryFactory repositoryFactory = new EpochDecoratorRepositoryFactory(emptyMap(),
                1, () -> 1L, "aaa", clients, Optional.of("vcs"),
                gaugeRegistry);

        final DummyLeaderService leaderService = new DummyLeaderService(new MetricRegistry());
        ObjectLifeCycleManagerFactory facadeFactory = new ObjectLifeCycleManagerFactory(serialExecutor,
                new MapGaugeRegistry(),
                clock, (objectId, clusterAndType) -> null, repositoryFactory,
                new GCSettings(new GCLimit(10, 10), emptyMap()),
                Map.of(YpObjectSettings.TYPE_USED_AS_KEY_WITH_DEFAULT_SETTINGS, new YpObjectSettings.Builder().setWatches(false).build())
        );

        SandboxResourcesResolver mockSandboxResolver = Mockito.mock(SandboxResourcesResolver.class);
        when(mockSandboxResolver.getResolveStatus(anyString())).thenReturn(Readiness.ready());

        ControllerFactoryImpl factory = new ControllerFactoryImpl(clients,
                clock, AclUpdater.IDENTITY,
                Mockito.mock(DockerImagesResolver.class), converter, (spec, stageId, disabledClusters) -> emptyList(), new DummyStageStatusSender(), facadeFactory, AclUpdater.IDENTITY, AclUpdater.IDENTITY, new AclPrefixFilter("deploy:"), Mockito.mock(LogbrokerTopicConfigResolver.class), mockSandboxResolver, Mockito.mock(RelationController.class), GlobalContext.EMPTY, new PodSpecCompositePatcher(new RevisionsHolderImpl(ImmutableMap.of(0, ImmutableList.of()), 0)),
                new EndpointSetLivenessPatcherV1(), new DynamicResourceResolvedResourcesPatcher(mockSandboxResolver));

        String stageId = "stage_id";
        String projectId = "project_id";
        String unitId = "unit_id";
        RootControllerImpl rootController = new RootControllerImpl(factory, leaderService, new MapGaugeRegistry(),
                clock);
        rootController.processGcForRemovedStages(ImmutableSet.of(stageId), ImmutableSet.of(stageId));
        rootController.sync(
                ImmutableMap.of(
                        stageId, Try.success(new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                                .setStatus(TStageStatus.getDefaultInstance())
                                .setMeta(new StageMeta(stageId, new Acl(emptyList()), "", "", 0, projectId))
                                .setSpecAndTimestamp(TStageSpec.newBuilder()
                                        .setRevision(1)
                                        .putDeployUnits("unit_id", converter.toProto(TestData.DEPLOY_UNIT_SPEC))
                                        .build(), 1)
                                .build()
                        )),
                ImmutableMap.of(
                        projectId, Try.success(new YpObject.Builder<SchemaMeta, TProjectSpec, TProjectStatus>()
                                .setStatus(TProjectStatus.getDefaultInstance())
                                .setMeta(new SchemaMeta(projectId, new Acl(emptyList()), "", "", 0))
                                .setSpecAndTimestamp(TProjectSpec.newBuilder().setAccountId("tmp").build(), 1)
                                .build())));

        Retainment toRetain = rootController.shouldRetain(stageId + "." + unitId,
                ClusterAndType.perClusterInstance(cluster, YpObjectType.REPLICA_SET));
        assertRetained(toRetain, true, String.format("Replica set has pods in cluster '%s'", cluster));

        String unknownUnitId = "unknown_id";
        Retainment toRemove = rootController.shouldRetain(stageId + "." + unknownUnitId,
                ClusterAndType.perClusterInstance(cluster, YpObjectType.REPLICA_SET));
        assertRetained(toRemove, false, String.format("Deploy unit '%s' is not present in stage '%s'", unknownUnitId,
                stageId));
    }

    private static void assertRetained(Retainment actual, boolean isRetained, String message) {
        assertThat(actual, equalTo(new Retainment(isRetained, message)));
    }
}
