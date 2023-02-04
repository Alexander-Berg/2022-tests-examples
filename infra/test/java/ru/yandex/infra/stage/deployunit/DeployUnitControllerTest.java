package ru.yandex.infra.stage.deployunit;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.bolts.collection.Either;
import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.stage.DeployPrimitiveControllerFactory;
import ru.yandex.infra.stage.GlobalContext;
import ru.yandex.infra.stage.StageContext;
import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.docker.DockerImagesResolver;
import ru.yandex.infra.stage.docker.DockerResolveResultHandler;
import ru.yandex.infra.stage.docker.DockerResolveStatus;
import ru.yandex.infra.stage.dto.Condition;
import ru.yandex.infra.stage.dto.DeployProgress;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.DeployUnitSpecDetails;
import ru.yandex.infra.stage.dto.DeployUnitStatus;
import ru.yandex.infra.stage.dto.DockerImageDescription;
import ru.yandex.infra.stage.dto.DynamicResourceRevisionStatus;
import ru.yandex.infra.stage.dto.DynamicResourceStatus;
import ru.yandex.infra.stage.dto.McrsUnitSpec;
import ru.yandex.infra.stage.dto.ReplicaSetUnitSpec;
import ru.yandex.infra.stage.dto.ReplicaSetUnitStatus;
import ru.yandex.infra.stage.podspecs.EndpointSetSpecCompositePatcher;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.infra.stage.podspecs.patcher.dynamic_resource.DynamicResourceResolvedResourcesPatcher;
import ru.yandex.infra.stage.primitives.AggregatedRawStatus;
import ru.yandex.infra.stage.primitives.DeployPrimitiveController;
import ru.yandex.infra.stage.primitives.DeployPrimitiveStatus;
import ru.yandex.infra.stage.util.SettableClock;
import ru.yandex.yp.client.api.AccessControl;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.DynamicResource.TDynamicResourceSpec;
import ru.yandex.yp.client.api.THorizontalPodAutoscalerSpec;
import ru.yandex.yp.client.api.TMultiClusterReplicaSetSpec;
import ru.yandex.yp.client.api.TReplicaSetScaleSpec;
import ru.yandex.yp.client.api.TReplicaSetSpec;
import ru.yandex.yp.client.pods.TWorkload;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.TestData.ACL_READ_ENTRY;
import static ru.yandex.infra.stage.TestData.ACL_WRITE_ENTRY;
import static ru.yandex.infra.stage.TestData.DEPLOY_UNIT_STATUS;
import static ru.yandex.infra.stage.TestData.DEPLOY_UNIT_TIMELINE;
import static ru.yandex.infra.stage.TestData.DOCKER_IMAGE_CONTENTS;
import static ru.yandex.infra.stage.TestData.DOCKER_IMAGE_CONTENTS_WITH_EMPTY_DIGEST;
import static ru.yandex.infra.stage.TestData.DOCKER_IMAGE_DESCRIPTION;
import static ru.yandex.infra.stage.TestData.POD_AGENT_CONFIG_EXTRACTOR;
import static ru.yandex.infra.stage.deployunit.DeployUnitControllerImpl.DEFAULT_LATEST_DEPLOYED_REVISION;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

class DeployUnitControllerTest {
    private static final String DEPLOY_UNIT_ID = TestData.DEPLOY_UNIT_ID;
    private static final String FULL_DEPLOY_UNIT_ID = "full_deploy_unit_id";
    private static final String STAGE_FQID = "yp|sas-test|stage|stage-id|d7c28a6b-a59aedd2-25eb200a-301468c4";
    private static final StageContext STAGE_CONTEXT1 = new StageContext(STAGE_FQID,
            "stage-id", 1, "accId", TestData.STAGE_ACL, 1,
            "project-id", emptyMap(), emptyMap(), TestData.RUNTIME_DEPLOY_CONTROLS, emptyMap(), GlobalContext.EMPTY);
    private static final StageContext STAGE_CONTEXT2 = new StageContext(STAGE_FQID,
            "stage-id", 1, "accId", TestData.STAGE_ACL, 2,
            "project-id", emptyMap(), emptyMap(), TestData.RUNTIME_DEPLOY_CONTROLS, emptyMap(), GlobalContext.EMPTY);
    private static final StageContext STAGE_CONTEXT_DR = new StageContext(STAGE_FQID,
            "stage-id", 1, "accId", TestData.STAGE_ACL, 1, "project-id", ImmutableMap.of(TestData.DYNAMIC_RESOURCE_ID,
            TestData.DYNAMIC_RESOURCE_SPEC),
            emptyMap(), TestData.RUNTIME_DEPLOY_CONTROLS, emptyMap(), GlobalContext.EMPTY);
    private static final TReplicaSetScaleSpec DEFAULT_AUTOSCALE_SPEC = TReplicaSetScaleSpec.newBuilder()
            .setMinReplicas(1)
            .setMaxReplicas(5)
            .build();

    private static final LogbrokerTopicConfigResolver DUMMY_LOGBROCKER_TOPIC_CONFIG_RESOLVER =
            mock(LogbrokerTopicConfigResolver.class);

    private static final SandboxResourcesResolver DUMMY_SANDBOX_RESOLVER = mock(SandboxResourcesResolver.class);

    static {
        when(DUMMY_SANDBOX_RESOLVER.get(anyString())).thenReturn(TestData.SANDBOX_RESOLVED_RESOURCES);
        when(DUMMY_SANDBOX_RESOLVER.getResolveStatus(anyString())).thenReturn(Readiness.ready());
    }

    private static final DeployUnitSpecDetails UNIT_DETAILS =
            new ReplicaSetUnitSpec(TReplicaSetSpec.getDefaultInstance(), emptyMap(), POD_AGENT_CONFIG_EXTRACTOR);
    private static final DeployUnitSpec NOT_DOCKER_UNIT_SPEC = getUnitSpec(emptyMap(), 1);
    private static final DeployUnitSpec DOCKER_UNIT_SPEC = getUnitSpec(ImmutableMap.of("box",
            DOCKER_IMAGE_DESCRIPTION), 1);
    private static final SettableClock CLOCK = new SettableClock();

    private static class CustomDockerResolver implements DockerImagesResolver {
        private boolean isSubscribed = false;
        private DockerResolveStatus status = DockerResolveStatus.getInProgressStatus("RESOLVING", "");
        private DockerResolveResultHandler resolveResultHandler;
        private boolean wasUnregistered = false;
        private int forceResolveCallsCount;

        @Override
        public void registerResultHandler(String unitId, DockerResolveResultHandler handler) {
            this.resolveResultHandler = handler;
        }

        @Override
        public void addResolve(DockerImageDescription description, String unitId) {
            isSubscribed = true;
        }

        @Override
        public void removeResolve(DockerImageDescription description, String unitId) {
            isSubscribed = false;
        }

        @Override
        public void forceResolve(DockerImageDescription description) {
            forceResolveCallsCount++;
        }

        @Override
        public DockerResolveStatus getResolveStatus(DockerImageDescription description) {
            if (!isSubscribed && status.getResult().isEmpty()) {
                return DockerResolveStatus.getEmptyStatus();
            }
            return status;
        }

        @Override
        public void unregisterResultHandler(String unitId) {
            wasUnregistered = true;
        }
    }

    private static class CustomDeployPrimitiveController implements DeployPrimitiveController {
        Optional<DeployUnitContext> receivedContext = empty();
        boolean wasShutdown = false;
        boolean wasAddedMetric = false;

        @Override
        public void sync(DeployUnitSpecDetails deployUnitSpecDetails, DeployUnitContext context) {
            receivedContext = Optional.of(context);
        }

        @Override
        public void shutdown() {
            wasShutdown = true;
        }

        @Override
        public AggregatedRawStatus getStatus() {
            return new AggregatedRawStatus<>(new DeployPrimitiveStatus<>(Readiness.ready(),
                    new DeployProgress(1, 0, 1), empty()));
        }

        @Override
        public void addStats(DeployUnitStats.Builder builder) {
            wasAddedMetric = true;
        }
    }

    private static class DeployUnitControllerTestBuilder {
        private DeployUnitSpec spec = NOT_DOCKER_UNIT_SPEC;
        private String deployUnitId = DEPLOY_UNIT_ID;
        private String fullDeployUnitId = FULL_DEPLOY_UNIT_ID;
        private StageContext stageContext = STAGE_CONTEXT1;
        private DeployPrimitiveControllerFactory factory = (a, b, c, d) -> new CustomDeployPrimitiveController();
        private MultiplexingController.Factory<DataModel.TEndpointSetSpec, ReadinessStatus> endpointFactory = null;
        private MultiplexingController.Factory<TDynamicResourceSpec, DynamicResourceRevisionStatus> dynamicResourceFactory = null;
        private MultiplexingController.Factory<THorizontalPodAutoscalerSpec, ReadinessStatus> horizontalPodAutoscalerFactory = null;
        private LogbrokerTopicConfigResolver logbrokerTopicConfigResolver = DUMMY_LOGBROCKER_TOPIC_CONFIG_RESOLVER;
        private SandboxResourcesResolver sandboxResourcesResolver = DUMMY_SANDBOX_RESOLVER;
        public EndpointSetSpecCompositePatcher endpointSetSpecCompositePatcher = new EndpointSetSpecCompositePatcher(new DummyRevisionHolder());
        private final SpecPatcher<TDynamicResourceSpec.Builder> dynamicResurceSpecPatcher = new DynamicResourceResolvedResourcesPatcher(sandboxResourcesResolver);

        DockerImagesResolver dockerImagesResolver = new CustomDockerResolver();
        Consumer<DeployUnitStatus> statusUpdateHandler = s -> {
        };
        final BiConsumer<String, DynamicResourceStatus> dynamicResourceStatusUpdateHandler = (id, s) -> {
        };
        final BiConsumer<String, DeployUnitStatus> endpointSetStatusUpdateHandler = (id, s) -> {
        };

        Clock clock = CLOCK;

        public DeployUnitControllerTestBuilder setSpec(DeployUnitSpec spec) {
            this.spec = spec;
            return this;
        }

        public DeployUnitControllerTestBuilder setDeployUnitId(String deployUnitId) {
            this.deployUnitId = deployUnitId;
            return this;
        }

        public DeployUnitControllerTestBuilder setFullDeployUnitId(String fullDeployUnitId) {
            this.fullDeployUnitId = fullDeployUnitId;
            return this;
        }

        public DeployUnitControllerTestBuilder setStageContext(StageContext stageContext) {
            this.stageContext = stageContext;
            return this;
        }

        public DeployUnitControllerTestBuilder setDeployPrimitiveControllerFactory(DeployPrimitiveControllerFactory factory) {
            this.factory = factory;
            return this;
        }

        public DeployUnitControllerTestBuilder setDeployPrimitiveController(DeployPrimitiveController deployPrimitiveController) {
            this.factory = (a, b, c, d) -> deployPrimitiveController;
            return this;
        }

        public DeployUnitControllerTestBuilder setEndpointFactory(
                MultiplexingController.Factory<DataModel.TEndpointSetSpec, ReadinessStatus> endpointFactory) {
            this.endpointFactory = endpointFactory;
            return this;
        }

        public DeployUnitControllerTestBuilder setHorizontalPodAutoscalerFactory(
                MultiplexingController.Factory<THorizontalPodAutoscalerSpec, ReadinessStatus> horizontalPodAutoscalerFactory) {
            this.horizontalPodAutoscalerFactory = horizontalPodAutoscalerFactory;
            return this;
        }

        public DeployUnitControllerTestBuilder setLogbrokerTopicConfigResolver(
                LogbrokerTopicConfigResolver logbrokerTopicConfigResolver) {
            this.logbrokerTopicConfigResolver = logbrokerTopicConfigResolver;
            return this;
        }

        public DeployUnitControllerTestBuilder setDynamicResourceFactory(
                MultiplexingController.Factory<TDynamicResourceSpec, DynamicResourceRevisionStatus> dynamicResourceFactory) {
            this.dynamicResourceFactory = dynamicResourceFactory;
            return this;
        }

        public DeployUnitControllerTestBuilder setSandboxResourcesResolver(
                SandboxResourcesResolver sandboxResourcesResolver) {
            this.sandboxResourcesResolver = sandboxResourcesResolver;
            return this;
        }

        public DeployUnitControllerTestBuilder setDockerImagesResolver(
                DockerImagesResolver dockerImagesResolver) {
            this.dockerImagesResolver = dockerImagesResolver;
            return this;
        }

        public DeployUnitControllerTestBuilder setStatusUpdateHandler(
                Consumer<DeployUnitStatus> statusUpdateHandler) {
            this.statusUpdateHandler = statusUpdateHandler;
            return this;
        }

        public DeployUnitControllerTestBuilder setStatusUpdateHandler(AtomicInteger upperUpdateHandlerCounter,
                                                                      AtomicReference<DeployUnitStatus> lastStatus) {
            this.statusUpdateHandler = s -> {
                upperUpdateHandlerCounter.incrementAndGet();
                lastStatus.set(s);
            };
            return this;
        }

        public DeployUnitControllerTestBuilder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public DeployUnitControllerImpl build() {
            return new DeployUnitControllerImpl(spec, deployUnitId, fullDeployUnitId, stageContext,
                    factory, endpointFactory, dynamicResourceFactory,
                    horizontalPodAutoscalerFactory, dockerImagesResolver,
                    statusUpdateHandler, dynamicResourceStatusUpdateHandler,
                    endpointSetStatusUpdateHandler, clock,
                    logbrokerTopicConfigResolver,
                    sandboxResourcesResolver,
                    endpointSetSpecCompositePatcher, dynamicResurceSpecPatcher);
        }
    }

    private static class AsyncSandboxResourcesResolver implements SandboxResourcesResolver {
        private final CountDownLatch latch;
        private final AtomicReference<Map<String, String>> resolvedUrl = new AtomicReference<>();

        public AsyncSandboxResourcesResolver(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public Map<String, String> get(String fullDeployUnitId) {
            return resolvedUrl.get();
        }

        @Override
        public void registerResultHandlerAndTryGet(String unitId, SandboxResourcesResolveResultHandler handler,
                                                   DeployUnitSpec spec,
                                                   List<TDynamicResourceSpec> dynamicResourceSpecs) {
            CompletableFuture.runAsync(() -> {
                resolvedUrl.set(TestData.SANDBOX_RESOLVED_RESOURCES);
                handler.onSandboxResourceResolveSuccess();
                latch.countDown();
            }, CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS));
        }

        @Override
        public Readiness getResolveStatus(String fullDeployUnitId) {
            if (resolvedUrl.get() == null) {
                return Readiness.inProgress("in progress");
            } else {
                return Readiness.ready();
            }
        }

        @Override
        public void unregisterResultHandler(String unitId) {

        }
    }

    private static DeployUnitSpec.Builder getSpecBuilder(DeployUnitSpecDetails withClustersDetails) {
        return TestData.DEPLOY_UNIT_SPEC.toBuilder().withDetails(withClustersDetails);
    }

    private static DeployUnitSpec getSpec(DeployUnitSpecDetails withClustersDetails) {
        return getSpecBuilder(withClustersDetails).build();
    }

    private static DeployUnitSpec getUnitSpec(Map<String, DockerImageDescription> imagesForBoxes, int revision) {
        return getSpecBuilder(UNIT_DETAILS)
                .withRevision(revision)
                .withImagesForBoxes(imagesForBoxes)
                .build();
    }

    private static DeployUnitSpec getDeployUnitSpecWithoutLogs() {
        var replicaSetSpec = TReplicaSetSpec.newBuilder();
        var workload = TWorkload.newBuilder().setTransmitLogs(false);
        replicaSetSpec
                .getPodTemplateSpecBuilder()
                .getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .addWorkloads(workload);

        return getSpecBuilder(
                new ReplicaSetUnitSpec(
                        replicaSetSpec.build(),
                        emptyMap(),
                        POD_AGENT_CONFIG_EXTRACTOR
                )
        ).withNetworkDefaults(TestData.EMPTY_NETWORK_DEFAULTS)
                .build();
    }

    @Test
    void notDockerDeployUnitControllerStartTest() {
        CustomDeployPrimitiveController deployPrimitiveController = new CustomDeployPrimitiveController();
        AtomicInteger upperUpdateHandlerCounter = new AtomicInteger(0);
        AtomicReference<DeployUnitStatus> lastStatus = new AtomicReference<>();

        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setDeployPrimitiveController(deployPrimitiveController)
                .setStatusUpdateHandler(upperUpdateHandlerCounter, lastStatus)
                .build();

        controller.start();

        // Send context down. Send status up (ready)
        assertThat(deployPrimitiveController.receivedContext, optionalWithValue());
        assertThat(upperUpdateHandlerCounter.get(), equalTo(1));
        assertThat(lastStatus.get().getReady().isTrue(), equalTo(true));
    }

    @Test
    void notDockerDeployUnitControllerUpdateSpecTest() {
        CustomDeployPrimitiveController deployPrimitiveController = new CustomDeployPrimitiveController();
        AtomicInteger upperUpdateHandlerCounter = new AtomicInteger(0);
        AtomicReference<DeployUnitStatus> lastStatus = new AtomicReference<>();

        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setDeployPrimitiveController(deployPrimitiveController)
                .setStatusUpdateHandler(upperUpdateHandlerCounter, lastStatus)
                .build();

        controller.start();
        deployPrimitiveController.receivedContext = empty();

        // update spec
        controller.updateSpec(getUnitSpec(emptyMap(), 2), STAGE_CONTEXT2);

        // Send context down. Send status up (ready)
        assertThat(deployPrimitiveController.receivedContext, optionalWithValue());
        assertThat(upperUpdateHandlerCounter.get(), equalTo(2));
        assertThat(lastStatus.get().getReady().isTrue(), equalTo(true));
    }

    @Test
    void dockerSuccessDeployUnitControllerTest() {
        CustomDeployPrimitiveController deployPrimitiveController = new CustomDeployPrimitiveController();
        AtomicInteger upperUpdateHandlerCounter = new AtomicInteger(0);
        AtomicReference<DeployUnitStatus> lastStatus = new AtomicReference<>();
        var resolver = new CustomDockerResolver();

        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setDeployPrimitiveController(deployPrimitiveController)
                .setStatusUpdateHandler(upperUpdateHandlerCounter, lastStatus)
                .setSpec(DOCKER_UNIT_SPEC)
                .setDockerImagesResolver(resolver)
                .build();
        controller.start();

        // Sub on dockerResolver. Send status up (unready). Don't call sync for lower
        assertThat(resolver.isSubscribed, equalTo(true));
        assertThat(upperUpdateHandlerCounter.get(), equalTo(1));
        assertThat(lastStatus.get().getReady().isTrue(), equalTo(false));
        assertThat(deployPrimitiveController.receivedContext, equalTo(empty()));

        // Receive response
        resolver.status = DockerResolveStatus.getReadyStatus(DOCKER_IMAGE_CONTENTS);
        resolver.resolveResultHandler.onDockerResolveSuccess(DOCKER_IMAGE_DESCRIPTION, DOCKER_IMAGE_CONTENTS);

        // Send context down. (Context with dockerImagesContents) Send status up (ready)
        assertThat(deployPrimitiveController.receivedContext.orElseThrow().getDockerImagesContents().get(DOCKER_IMAGE_DESCRIPTION),
                equalTo(DOCKER_IMAGE_CONTENTS));
        assertThat(upperUpdateHandlerCounter.get(), equalTo(2));
        assertThat(lastStatus.get().getReady().isTrue(), equalTo(true));
    }

    @Test
    void dockerFailureDeployUnitControllerTest() {
        CustomDeployPrimitiveController deployPrimitiveController = new CustomDeployPrimitiveController();
        AtomicInteger upperUpdateHandlerCounter = new AtomicInteger(0);
        AtomicReference<DeployUnitStatus> lastStatus = new AtomicReference<>();
        var resolver = new CustomDockerResolver();

        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setDeployPrimitiveController(deployPrimitiveController)
                .setStatusUpdateHandler(upperUpdateHandlerCounter, lastStatus)
                .setDockerImagesResolver(resolver)
                .setSpec(DOCKER_UNIT_SPEC)
                .build();

        controller.start();

        // Receive error
        resolver.resolveResultHandler.onDockerResolveFailure(DOCKER_IMAGE_DESCRIPTION, new RuntimeException());

        // Send status up (unready). Don't call sync for lower
        assertThat(upperUpdateHandlerCounter.get(), equalTo(2));
        assertThat(lastStatus.get().getReady().isTrue(), equalTo(false));
        assertThat(deployPrimitiveController.receivedContext, equalTo(empty()));
        // Update spec - without docker
        controller.updateSpec(getUnitSpec(emptyMap(), 2), STAGE_CONTEXT2);

        // Unsub on resolver. Send context down (context with empty dockerImagesContents). Send status up (ready).
        assertThat(resolver.isSubscribed, equalTo(false));
        assertThat(deployPrimitiveController.receivedContext.orElseThrow().getDockerImagesContents(),
                Matchers.anEmptyMap());
        assertThat(upperUpdateHandlerCounter.get(), equalTo(3));
        assertThat(lastStatus.get().getReady().isTrue(), equalTo(true));
    }

    @Test
    void changeDeployUnitTypeTest() {
        CustomDeployPrimitiveController deployPrimitiveController = new CustomDeployPrimitiveController();
        AtomicInteger upperUpdateHandlerCounter = new AtomicInteger(0);
        AtomicReference<DeployUnitStatus> lastStatus = new AtomicReference<>();
        DummyObjectController<THorizontalPodAutoscalerSpec, ReadinessStatus> customHorizontalPodAutoscalerController =
                new DummyObjectController<>();
        customHorizontalPodAutoscalerController.setStatus(new ReadinessStatus(Readiness.ready()));

        // NOT_DOCKER_UNIT_SPEC is rs spec
        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setDeployPrimitiveController(deployPrimitiveController)
                .setHorizontalPodAutoscalerFactory((a, b, c) -> customHorizontalPodAutoscalerController)
                .setStatusUpdateHandler(upperUpdateHandlerCounter, lastStatus)
                .build();
        controller.start();

        var deployUnitSpec = getSpecBuilder(
                new McrsUnitSpec(
                        TMultiClusterReplicaSetSpec.getDefaultInstance(),
                        POD_AGENT_CONFIG_EXTRACTOR
                )).withRevision(2)
                .build();

        controller.updateSpec(deployUnitSpec, STAGE_CONTEXT2);
        assertThat(deployPrimitiveController.wasShutdown, equalTo(true));

        // MCRS does not have horizontal pod autoscaler
        assertThat(customHorizontalPodAutoscalerController.isShutdown(), equalTo(true));
    }

    @Test
    void unregisterDockerResolverOnShutdownTest() {
        AtomicInteger upperUpdateHandlerCounter = new AtomicInteger(0);
        AtomicReference<DeployUnitStatus> lastStatus = new AtomicReference<>();
        CustomDockerResolver resolver = new CustomDockerResolver();

        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setStatusUpdateHandler(upperUpdateHandlerCounter, lastStatus)
                .setDockerImagesResolver(resolver)
                .build();

        controller.start();
        controller.shutdown();
        assertThat(resolver.wasUnregistered, equalTo(true));
    }

    @Test
    void endpointSetTemplateTest() {
        DummyObjectController<DataModel.TEndpointSetSpec, ReadinessStatus> customEndpointSetController1 =
                new DummyObjectController<>();
        customEndpointSetController1.setStatus(new ReadinessStatus(Readiness.ready()));
        DummyObjectController<DataModel.TEndpointSetSpec, ReadinessStatus> customEndpointSetController2 =
                new DummyObjectController<>();
        customEndpointSetController2.setStatus(new ReadinessStatus(Readiness.ready()));
        DeployUnitSpecDetails withClustersDetails = new ReplicaSetUnitSpec(TReplicaSetSpec.getDefaultInstance(),
                ImmutableMap.of("sas-test", new ReplicaSetUnitSpec.PerClusterSettings(Either.left(1), empty())),
                POD_AGENT_CONFIG_EXTRACTOR);
        int port1 = 137;
        int port2 = 138;
        String protocol1 = "protocol";
        String protocol2 = "protocol2";
        double livinessRatio1 = 0.66;
        double livinessRatio2 = 0.99;
        AtomicInteger endpointSetCounter = new AtomicInteger();
        var endpointSetSpecMap = Map.of(
                "", DataModel.TEndpointSetSpec.newBuilder()
                        .setPort(port1)
                        .setProtocol(protocol1)
                        .setLivenessLimitRatio(livinessRatio1)
                        .build(),
                "second", DataModel.TEndpointSetSpec.newBuilder()
                        .setPort(port2)
                        .setProtocol(protocol2)
                        .setLivenessLimitRatio(livinessRatio2)
                        .build());
        var spec = getSpecBuilder(withClustersDetails)
                .withEndpointSets(endpointSetSpecMap)
                .build();

        DeployUnitControllerImpl controller = new DeployUnitControllerTestBuilder()
                .setSpec(spec)
                .setEndpointFactory((id, cluster, updateNotifier) -> {
                    if (endpointSetCounter.get() == 0) {
                        endpointSetCounter.incrementAndGet();
                        return customEndpointSetController1;
                    } else {
                        return customEndpointSetController2;
                    }
                })
                .build();
        controller.start();
        assertThat(customEndpointSetController1.getCurrentSpec().get().getPort(), equalTo(port1));
        assertThat(customEndpointSetController1.getCurrentSpec().get().getProtocol(), equalTo(protocol1));
        assertThat(customEndpointSetController1.getCurrentSpec().get().getLivenessLimitRatio(),
                equalTo(livinessRatio1));
        assertThat(customEndpointSetController2.getCurrentSpec().get().getPort(), equalTo(port2));
        assertThat(customEndpointSetController2.getCurrentSpec().get().getProtocol(), equalTo(protocol2));
        assertThat(customEndpointSetController2.getCurrentSpec().get().getLivenessLimitRatio(),
                equalTo(livinessRatio2));
        assertThat(
                ((ReplicaSetUnitStatus) controller.getStatus().getDetails()).getClusterStatuses().get("sas-test")
                        .getEndpointSetRefs(),
                containsInAnyOrder(FULL_DEPLOY_UNIT_ID, FULL_DEPLOY_UNIT_ID + ".second"));
    }

    @Test
    void endpointSetDefaultValuesTest() {
        DummyObjectController<DataModel.TEndpointSetSpec, ReadinessStatus> customEndpointSetController =
                new DummyObjectController<>();
        customEndpointSetController.setStatus(new ReadinessStatus(Readiness.ready()));
        DeployUnitSpecDetails withClustersDetails = new ReplicaSetUnitSpec(TReplicaSetSpec.getDefaultInstance(),
                ImmutableMap.of("sas-test", new ReplicaSetUnitSpec.PerClusterSettings(Either.left(1), empty())),
                POD_AGENT_CONFIG_EXTRACTOR);
        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setSpec(getSpec(withClustersDetails))
                .setEndpointFactory((id, cluster, updateNotifier) -> customEndpointSetController)
                .build();
        controller.start();
        assertThat(customEndpointSetController.getCurrentSpec().orElseThrow().getPort(), equalTo(80));
        assertThat(customEndpointSetController.getCurrentSpec().orElseThrow().getLivenessLimitRatio(), equalTo(0.35));
    }

    @Test
    void horizontalPodAutoscalerControllerWillNotSyncWithoutAutoscaleTest() {
        DummyObjectController<THorizontalPodAutoscalerSpec, ReadinessStatus> customHorizontalPodAutoscalerController =
                new DummyObjectController<>();
        customHorizontalPodAutoscalerController.setStatus(new ReadinessStatus(Readiness.ready()));
        DummyObjectController<DataModel.TEndpointSetSpec, ReadinessStatus> customEndpointSetController =
                new DummyObjectController<>();
        customEndpointSetController.setStatus(new ReadinessStatus(Readiness.ready()));

        DeployUnitSpecDetails withClustersDetails = new ReplicaSetUnitSpec(
                TReplicaSetSpec.getDefaultInstance(),
                ImmutableMap.of("sas-test", new ReplicaSetUnitSpec.PerClusterSettings(Either.left(1), empty())),
                POD_AGENT_CONFIG_EXTRACTOR
        );
        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setSpec(getSpec(withClustersDetails))
                .setHorizontalPodAutoscalerFactory((id, cluster, updateNotifier) -> customHorizontalPodAutoscalerController)
                .setEndpointFactory((id, cluster, updateNotifier) -> customEndpointSetController)
                .build();
        controller.start();

        assertThat(customHorizontalPodAutoscalerController.getCurrentSpec(), emptyOptional());
    }

    @Test
    void horizontalPodAutoscalerControllerSyncTest() {
        DummyObjectController<THorizontalPodAutoscalerSpec, ReadinessStatus> customHorizontalPodAutoscalerController =
                new DummyObjectController<>();
        customHorizontalPodAutoscalerController.setStatus(new ReadinessStatus(Readiness.ready()));
        DummyObjectController<DataModel.TEndpointSetSpec, ReadinessStatus> customEndpointSetController =
                new DummyObjectController<>();
        customEndpointSetController.setStatus(new ReadinessStatus(Readiness.ready()));

        DeployUnitSpecDetails withClustersDetails = new ReplicaSetUnitSpec(
                TReplicaSetSpec.getDefaultInstance(),
                ImmutableMap.of("sas-test",
                        new ReplicaSetUnitSpec.PerClusterSettings(Either.right(DEFAULT_AUTOSCALE_SPEC), empty())),
                POD_AGENT_CONFIG_EXTRACTOR);
        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setSpec(getSpec(withClustersDetails))
                .setHorizontalPodAutoscalerFactory((id, cluster, updateNotifier) -> customHorizontalPodAutoscalerController)
                .setEndpointFactory((id, cluster, updateNotifier) -> customEndpointSetController)
                .build();
        controller.start();

        assertThat(customHorizontalPodAutoscalerController.getCurrentSpec().orElseThrow().getReplicaSet(),
                equalTo(DEFAULT_AUTOSCALE_SPEC));
    }

    @Test
    void dynamicResourcesControllerSyncTest() throws InterruptedException {
        DummyObjectController<TDynamicResourceSpec, DynamicResourceRevisionStatus> customDynamicResourceController =
                new DummyObjectController<>();

        DeployUnitSpecDetails withClustersDetails = new ReplicaSetUnitSpec(
                TReplicaSetSpec.getDefaultInstance(),
                ImmutableMap.of("sas-test", new ReplicaSetUnitSpec.PerClusterSettings(Either.left(1), empty())),
                POD_AGENT_CONFIG_EXTRACTOR);

        DummyObjectController<DataModel.TEndpointSetSpec, ReadinessStatus> customEndpointSetController =
                new DummyObjectController<>();
        customEndpointSetController.setStatus(new ReadinessStatus(Readiness.ready()));
        CountDownLatch latch = new CountDownLatch(1);

        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setSpec(getSpec(withClustersDetails))
                .setStageContext(STAGE_CONTEXT_DR)
                .setDynamicResourceFactory(((id, cluster, updateNotifier) -> customDynamicResourceController))
                .setEndpointFactory((id, cluster, updateNotifier) -> customEndpointSetController)
                .setSandboxResourcesResolver(new AsyncSandboxResourcesResolver(latch))
                .build();
        controller.start();

        assertThat(customDynamicResourceController.getCurrentSpec(), emptyOptional());

        latch.await();
        assertThat(customDynamicResourceController.getCurrentSpec().isPresent(), equalTo(true));
        assertThat(customDynamicResourceController.getCurrentSpec().get().getDeployGroups(0).getUrlsList(),
                equalTo(TestData.DYNAMIC_RESOURCE_RESOLVED_URLS));
    }

    @Test
    void DUStatusIsUnreadyWhenEndpointSetIsUnready() {
        DummyObjectController<THorizontalPodAutoscalerSpec, ReadinessStatus> customHorizontalPodAutoscalerController =
                new DummyObjectController<>();
        customHorizontalPodAutoscalerController.setStatus(new ReadinessStatus(Readiness.ready()));
        DummyObjectController<DataModel.TEndpointSetSpec, ReadinessStatus> customEndpointSetController =
                new DummyObjectController<>();
        customEndpointSetController.setStatus(new ReadinessStatus(Readiness.inProgress("IN_PROGRESS")));

        AtomicInteger upperUpdateHandlerCounter = new AtomicInteger(0);
        AtomicReference<DeployUnitStatus> lastStatus = new AtomicReference<>();

        DeployUnitSpecDetails withClustersDetails = new ReplicaSetUnitSpec(
                TReplicaSetSpec.getDefaultInstance(),
                ImmutableMap.of("sas-test",
                        new ReplicaSetUnitSpec.PerClusterSettings(Either.right(DEFAULT_AUTOSCALE_SPEC),
                                empty())),
                POD_AGENT_CONFIG_EXTRACTOR);
        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setSpec(getSpec(withClustersDetails))
                .setHorizontalPodAutoscalerFactory((id, cluster, updateNotifier) -> customHorizontalPodAutoscalerController)
                .setEndpointFactory((id, cluster, updateNotifier) -> customEndpointSetController)
                .setStatusUpdateHandler(upperUpdateHandlerCounter, lastStatus)
                .build();
        controller.start();

        assertThat(upperUpdateHandlerCounter.get(), equalTo(1));
        assertThat(lastStatus.get().getReady().isTrue(), equalTo(false));
        assertThat(customHorizontalPodAutoscalerController.getCurrentSpec().get().getReplicaSet(),
                equalTo(DEFAULT_AUTOSCALE_SPEC));
    }

    @Test
    void DUStatusIsUnreadyWhenAutoscalerIsUnready() {
        DummyObjectController<THorizontalPodAutoscalerSpec, ReadinessStatus> customHorizontalPodAutoscalerController =
                new DummyObjectController<>();
        customHorizontalPodAutoscalerController.setStatus(new ReadinessStatus(Readiness.inProgress("IN_PROGRESS")));
        DummyObjectController<DataModel.TEndpointSetSpec, ReadinessStatus> customEndpointSetController =
                new DummyObjectController<>();
        customEndpointSetController.setStatus(new ReadinessStatus(Readiness.ready()));

        AtomicInteger upperUpdateHandlerCounter = new AtomicInteger(0);
        AtomicReference<DeployUnitStatus> lastStatus = new AtomicReference<>();

        DeployUnitSpecDetails withClustersDetails = new ReplicaSetUnitSpec(
                TReplicaSetSpec.getDefaultInstance(),
                ImmutableMap.of("sas-test",
                        new ReplicaSetUnitSpec.PerClusterSettings(Either.right(DEFAULT_AUTOSCALE_SPEC),
                                empty())),
                POD_AGENT_CONFIG_EXTRACTOR);
        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setSpec(getSpec(withClustersDetails))
                .setHorizontalPodAutoscalerFactory((id, cluster, updateNotifier) -> customHorizontalPodAutoscalerController)
                .setEndpointFactory((id, cluster, updateNotifier) -> customEndpointSetController)
                .setStatusUpdateHandler(upperUpdateHandlerCounter, lastStatus)
                .build();
        controller.start();

        assertThat(upperUpdateHandlerCounter.get(), equalTo(1));
        assertThat(lastStatus.get().getReady().isTrue(), equalTo(false));
        assertThat(customHorizontalPodAutoscalerController.getCurrentSpec().orElseThrow().getReplicaSet(),
                equalTo(DEFAULT_AUTOSCALE_SPEC));
    }

    @Test
    void metricTest() {
        CustomDockerResolver resolver = new CustomDockerResolver();
        CustomDeployPrimitiveController deployPrimitiveController = new CustomDeployPrimitiveController();

        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setDockerImagesResolver(resolver)
                .setDeployPrimitiveController(deployPrimitiveController)
                .setSpec(DOCKER_UNIT_SPEC)
                .build();

        controller.start();

        DeployUnitStats.Builder builder = new DeployUnitStats.Builder();
        controller.addStats(builder);
        DeployUnitStats stats = builder.build();
        assertThat(stats.prerequisitesReady, equalTo(0));
        assertThat(stats.prerequisitesUnready, equalTo(1));
        assertThat(deployPrimitiveController.wasAddedMetric, equalTo(false));

        // resolving complete
        resolver.status = DockerResolveStatus.getReadyStatus(DOCKER_IMAGE_CONTENTS);
        resolver.resolveResultHandler.onDockerResolveSuccess(DOCKER_IMAGE_DESCRIPTION, DOCKER_IMAGE_CONTENTS);

        builder = new DeployUnitStats.Builder();
        controller.addStats(builder);
        stats = builder.build();
        assertThat(stats.prerequisitesReady, equalTo(1));
        assertThat(stats.prerequisitesUnready, equalTo(0));
        assertThat(deployPrimitiveController.wasAddedMetric, equalTo(true));
    }

    @Test
    void dontUpdateStatusTimestampTest() {
        AtomicReference<DeployUnitStatus> lastStatus = new AtomicReference<>();

        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setStatusUpdateHandler(new AtomicInteger(), lastStatus)
                .build();

        Instant firstReady = CLOCK.instant();
        controller.start();

        CLOCK.incrementSecond();
        controller.updateSpec(NOT_DOCKER_UNIT_SPEC, STAGE_CONTEXT2);
        assertThat(lastStatus.get().getReady().getTimestamp(), equalTo(firstReady));
    }

    @Test
    void updateAclTest() {
        AtomicReference<DeployUnitStatus> lastStatus = new AtomicReference<>();
        CustomDeployPrimitiveController primitiveController = new CustomDeployPrimitiveController();
        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setStatusUpdateHandler(new AtomicInteger(), lastStatus)
                .setDeployPrimitiveController(primitiveController)
                .build();

        controller.updateSpec(NOT_DOCKER_UNIT_SPEC, STAGE_CONTEXT1);
        List<AccessControl.TAccessControlEntry> entries = new ArrayList<>(STAGE_CONTEXT1.getAcl().getEntries());
        entries.add(AccessControl.TAccessControlEntry.newBuilder()
                .setAction(AccessControl.EAccessControlAction.ACA_DENY)
                .addSubjects("newSubject")
                .build());
        Acl newAcl = new Acl(entries);
        assertThat(newAcl, not(equalTo(STAGE_CONTEXT1.getAcl())));
        controller.updateSpec(
                NOT_DOCKER_UNIT_SPEC,
                STAGE_CONTEXT1.withAcl(newAcl)
        );
        assertThat(primitiveController.receivedContext.orElseThrow().getStageContext().getAcl(), equalTo(newAcl));
    }

    @Test
    void reportItypeInStatus() {
        CustomDeployPrimitiveController deployPrimitiveController = new CustomDeployPrimitiveController();
        DeployUnitControllerImpl controller = new DeployUnitControllerTestBuilder()
                .setDeployPrimitiveController(deployPrimitiveController)
                .build();
        TReplicaSetSpec.Builder replicaSetSpec = TReplicaSetSpec.newBuilder();
        replicaSetSpec
                .getPodTemplateSpecBuilder()
                .getSpecBuilder()
                .getHostInfraBuilder()
                .getMonitoringBuilder()
                .putLabels("itype", "custom_itype");
        DeployUnitSpec spec = getSpecBuilder(
                new ReplicaSetUnitSpec(
                        replicaSetSpec.build(),
                        emptyMap(),
                        POD_AGENT_CONFIG_EXTRACTOR
                ))
                .withRevision(2)
                .withNetworkDefaults(TestData.EMPTY_NETWORK_DEFAULTS)
                .build();
        controller.updateSpec(spec, STAGE_CONTEXT2);
        assertThat(controller.getStatus().getYasmItype(), equalTo("custom_itype"));
    }

    @Test
    void checkLogBrokerUseOnlyInCaseLogTransmittedEnabledInStart() {
        DeployUnitSpec spec = getDeployUnitSpecWithoutLogs();
        LogbrokerTopicConfigResolver logbrokerTopicConfigResolver = mock(LogbrokerTopicConfigResolver.class);
        DeployUnitControllerImpl controller = new DeployUnitControllerTestBuilder()
                .setSpec(spec)
                .setLogbrokerTopicConfigResolver(logbrokerTopicConfigResolver)
                .build();

        controller.start();
        verify(logbrokerTopicConfigResolver, never())
                .get(any());
    }

    @Test
    void checkLogBrokerUseOnlyInCaseLogTransmittedEnabledInStatus() {
        DeployUnitSpec spec = getDeployUnitSpecWithoutLogs();
        LogbrokerTopicConfigResolver logbrokerTopicConfigResolver = mock(LogbrokerTopicConfigResolver.class);
        DeployUnitControllerImpl controller = new DeployUnitControllerTestBuilder()
                .setSpec(spec)
                .setLogbrokerTopicConfigResolver(logbrokerTopicConfigResolver)
                .build();

        // Check for call in prerequisitesAreReady
        controller.getStatus();
        verify(logbrokerTopicConfigResolver, never()).get(any());
    }

    @Test
    void dockerForceResolveAfterEachRevisionUpdateTest() {
        var resolver = new CustomDockerResolver();

        resolver.status = DockerResolveStatus.getReadyStatus(DOCKER_IMAGE_CONTENTS);

        int revision = 1;

        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setSpec(getUnitSpec(ImmutableMap.of("box", DOCKER_IMAGE_DESCRIPTION), revision))
                .setDockerImagesResolver(resolver)
                .build();
        controller.start();
        assertThat(resolver.forceResolveCallsCount, equalTo(0));

        revision++;
        controller.updateSpec(getUnitSpec(ImmutableMap.of("box", DOCKER_IMAGE_DESCRIPTION), revision), STAGE_CONTEXT1);
        assertThat(resolver.forceResolveCallsCount, equalTo(1));

        controller.updateSpec(getUnitSpec(ImmutableMap.of("box", DOCKER_IMAGE_DESCRIPTION), revision), STAGE_CONTEXT1);
        assertThat(resolver.forceResolveCallsCount, equalTo(1));

        revision++;
        controller.updateSpec(getUnitSpec(ImmutableMap.of("box", DOCKER_IMAGE_DESCRIPTION), revision), STAGE_CONTEXT1);
        assertThat(resolver.forceResolveCallsCount, equalTo(2));
    }

    @Test
    void skipDockerForceResolveDuringInitializationTest() {
        var resolver = new CustomDockerResolver();

        resolver.status = DockerResolveStatus.getReadyStatus(DOCKER_IMAGE_CONTENTS_WITH_EMPTY_DIGEST);

        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setSpec(DOCKER_UNIT_SPEC)
                .setDockerImagesResolver(resolver)
                .build();
        controller.start();

        DeployUnitStatus unitStatus = new DeployUnitStatus(
                TestData.CONDITION2, TestData.CONDITION3,
                TestData.DEPLOY_UNIT_SPEC, TestData.REPLICA_SET_UNIT_STATUS_DETAILS, TestData.DEPLOY_PROGRESS,
                "user_itype",
                new DeployUnitTimeline(
                        1,
                        Instant.ofEpochMilli(1),
                        TestData.CONDITION1
                )
        );
        controller.restoreFromStatus(unitStatus, STAGE_CONTEXT1);

        assertThat(resolver.forceResolveCallsCount, equalTo(0));
    }

    enum ReadinessEnum {
        READY(Readiness.ready(), Condition.Status.TRUE),
        IN_PROGRESS(Readiness.inProgress("in progress"), Condition.Status.FALSE),
        FAILURE(Readiness.failed("failure reason", "failure message"), Condition.Status.FALSE);

        private final Readiness readiness;
        private final Condition.Status readyStatus;

        ReadinessEnum(Readiness readiness, Condition.Status readyStatus) {
            this.readiness = readiness;
            this.readyStatus = readyStatus;
        }

        public Readiness getReadiness() {
            return readiness;
        }

        public Condition getReadyCondition() {
            return new Condition(readyStatus, "", "", Instant.now());
        }

        public int getExpectedLatestDeployedRevision(int previousLatestDeployedRevision, int currentRevision) {
            if (Condition.Status.TRUE != readyStatus) {
                return previousLatestDeployedRevision;
            }

            return Math.max(previousLatestDeployedRevision, currentRevision);
        }

        public int getExpectedTargetRevision(int previousTargetRevision, int currentTargetRevision) {
            // Todo: in ideal situation must be Math.max(previousTargetRevision, currentTargetRevision);
            return currentTargetRevision;
        }
    }

    private static final class LatestDeployedRevisionTestUtils {

        private static final int DEFAULT_START_SPEC_REVISION = 2;
        private static final StageContext DEFAULT_STAGE_CONTEXT = STAGE_CONTEXT1;
        private static final DeployUnitSpec DEFAULT_UNIT_SPEC = NOT_DOCKER_UNIT_SPEC;

        private static class CustomReadinessController extends CustomDeployPrimitiveController {

            ReadinessEnum nextReadiness;

            public void setNextReadiness(ReadinessEnum nextReadiness) {
                this.nextReadiness = nextReadiness;
            }

            @Override
            public AggregatedRawStatus getStatus() {
                return new AggregatedRawStatus<>(new DeployPrimitiveStatus<>(
                        nextReadiness.getReadiness(),
                        new DeployProgress(1, 0, 1), empty())
                );
            }
        }

        static LatestDeployedRevisionUpdatingTest createDefaultTest() {
            return createDefaultTest(DEFAULT_START_SPEC_REVISION);
        }

        static LatestDeployedRevisionUpdatingTest createDefaultTest(int startSpecRevision) {
            return new LatestDeployedRevisionUpdatingTest(
                    DEFAULT_STAGE_CONTEXT, DEFAULT_UNIT_SPEC, startSpecRevision
            );
        }

        static class LatestDeployedRevisionUpdatingTest {

            private final DeployUnitSpec deployUnitSpec;

            private final AtomicReference<DeployUnitStatus> lastStatus;
            private final CustomReadinessController customReadinessController;

            private final DeployUnitController deployUnitController;

            LatestDeployedRevisionUpdatingTest(StageContext defaultStageContext,
                                               DeployUnitSpec deployUnitSpec,
                                               int startSpecRevision) {
                this.deployUnitSpec = deployUnitSpec;

                this.lastStatus = new AtomicReference<>();
                this.customReadinessController = new CustomReadinessController();

                this.deployUnitController = new DeployUnitControllerTestBuilder()
                        .setStageContext(defaultStageContext)
                        .setSpec(deployUnitSpec.withRevision(startSpecRevision))
                        .setStatusUpdateHandler(new AtomicInteger(), lastStatus)
                        .setDeployPrimitiveController(customReadinessController)
                        .build();
            }

            private DeployUnitSpec getDeployUnitSpec() {
                return deployUnitSpec;
            }

            void process(Consumer<DeployUnitController> deployUnitControllerUpdater,
                         ReadinessEnum updatedSpecReadiness) {
                customReadinessController.setNextReadiness(updatedSpecReadiness);
                deployUnitControllerUpdater.accept(deployUnitController);
            }

            void ensureLatestDeployRevision(ReadinessEnum nextReadiness,
                                            int previousLatestDeployedRevision,
                                            int nextRevision) {
                int expectedLatestRevision = nextReadiness.getExpectedLatestDeployedRevision(
                        previousLatestDeployedRevision, nextRevision
                );
                assertThatEquals((int) lastStatus.get().getLatestDeployedRevision(), expectedLatestRevision);
            }

            void ensureTargetRevision(ReadinessEnum nextReadiness,
                                      int previousTargetRevision,
                                      int nextTargetRevision) {
                long expectedTargetRevision = nextReadiness.getExpectedTargetRevision(
                        previousTargetRevision, nextTargetRevision
                );
                assertThatEquals(lastStatus.get().getDeployUnitTimeline().getTargetRevision(), expectedTargetRevision);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ReadinessEnum.class)
    public void latestDeployedRevisionAfterStartTest(ReadinessEnum startReadiness) {
        var test = LatestDeployedRevisionTestUtils.createDefaultTest();

        test.process(DeployUnitController::start, startReadiness);

        test.ensureLatestDeployRevision(startReadiness, DEFAULT_LATEST_DEPLOYED_REVISION,
                LatestDeployedRevisionTestUtils.DEFAULT_START_SPEC_REVISION);
    }

    @ParameterizedTest
    @EnumSource(ReadinessEnum.class)
    public void targetRevisionAfterStartTest(ReadinessEnum startReadiness) {
        var test = LatestDeployedRevisionTestUtils.createDefaultTest();

        test.process(DeployUnitController::start, startReadiness);

        test.ensureTargetRevision(
                startReadiness,
                LatestDeployedRevisionTestUtils.DEFAULT_START_SPEC_REVISION,
                LatestDeployedRevisionTestUtils.DEFAULT_START_SPEC_REVISION
        );
    }

    private static Stream<Arguments> provideForLDRRestoreStatusTest() {
        var streamBuilder = Stream.<Arguments>builder();

        int restoredSpecRevision = LatestDeployedRevisionTestUtils.DEFAULT_START_SPEC_REVISION + 2;

        for (var startReadiness : ReadinessEnum.values()) {
            for (var restoredReadiness : ReadinessEnum.values()) {
                for (int restoredRevisionDelta = -1; restoredRevisionDelta <= 1; ++restoredRevisionDelta) {
                    int restoredLatestDeployedRevision = restoredSpecRevision + restoredRevisionDelta;

                    streamBuilder.add(Arguments.of(startReadiness, restoredSpecRevision, restoredReadiness,
                            restoredLatestDeployedRevision));
                }
            }
        }

        return streamBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("provideForLDRRestoreStatusTest")
    public void latestDeployedRevisionAfterRestoreStatusTest(ReadinessEnum startReadiness,
                                                             int restoredRevision,
                                                             ReadinessEnum restoredReadiness,
                                                             int restoredLatestDeployedRevision) {
        var test = LatestDeployedRevisionTestUtils.createDefaultTest();
        test.process(DeployUnitController::start, startReadiness);

        var restoredStatus = DEPLOY_UNIT_STATUS.toBuilder()
                .withCurrentTarget(test.getDeployUnitSpec().withRevision(restoredRevision))
                .withDeployUnitTimeline(
                        DEPLOY_UNIT_TIMELINE.toBuilder()
                                .withLatestReadyCondition(restoredReadiness.getReadyCondition())
                                .withLatestDeployedRevision(restoredLatestDeployedRevision)
                                .build()
                )
                .build();

        test.process(deployUnitController -> deployUnitController.restoreFromStatus(restoredStatus,
                LatestDeployedRevisionTestUtils.DEFAULT_STAGE_CONTEXT), restoredReadiness);
        test.ensureLatestDeployRevision(restoredReadiness, restoredLatestDeployedRevision, restoredRevision);
    }

    @ParameterizedTest
    @MethodSource("provideForLDRRestoreStatusTest")
    public void targetRevisionAfterRestoreStatusTest(ReadinessEnum startReadiness,
                                                     int restoredRevision,
                                                     ReadinessEnum restoredReadiness,
                                                     int restoredTargetRevision) {
        var test = LatestDeployedRevisionTestUtils.createDefaultTest();
        test.process(DeployUnitController::start, startReadiness);

        var restoredStatus = DEPLOY_UNIT_STATUS.toBuilder()
                .withCurrentTarget(test.getDeployUnitSpec().withRevision(restoredRevision))
                .withDeployUnitTimeline(
                        DEPLOY_UNIT_TIMELINE.toBuilder()
                                .withLatestReadyCondition(restoredReadiness.getReadyCondition())
                                .withTargetRevision(restoredTargetRevision)
                                .build()
                ).build();

        test.process(deployUnitController -> deployUnitController.restoreFromStatus(restoredStatus,
                LatestDeployedRevisionTestUtils.DEFAULT_STAGE_CONTEXT), restoredReadiness);
        test.ensureTargetRevision(restoredReadiness, restoredTargetRevision, restoredRevision);
    }

    private static Stream<Arguments> provideForLDRUpdateTest() {
        var streamBuilder = Stream.<Arguments>builder();

        for (ReadinessEnum startReadiness : ReadinessEnum.values()) {
            for (ReadinessEnum nextReadiness : ReadinessEnum.values()) {
                streamBuilder.add(Arguments.of(startReadiness, nextReadiness));
            }
        }

        return streamBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("provideForLDRUpdateTest")
    public void latestDeployedRevisionAfterUpdateNextRevisionTest(ReadinessEnum startReadiness,
                                                                  ReadinessEnum nextReadiness) {
        int startRevision = LatestDeployedRevisionTestUtils.DEFAULT_START_SPEC_REVISION;
        int nextRevision = startRevision + 1;

        var nextStageContext = LatestDeployedRevisionTestUtils.DEFAULT_STAGE_CONTEXT;
        latestDeployedRevisionAfterUpdateSpecScenario(startRevision, startReadiness, nextRevision, nextReadiness,
                nextStageContext);
    }

    @ParameterizedTest
    @EnumSource(ReadinessEnum.class)
    public void latestDeployedRevisionAfterUpdatePreviousRevisionTest(ReadinessEnum startReadiness) {
        int startRevision = LatestDeployedRevisionTestUtils.DEFAULT_START_SPEC_REVISION;
        int nextRevision = startRevision - 1;

        var nextStageContext = LatestDeployedRevisionTestUtils.DEFAULT_STAGE_CONTEXT;
        latestDeployedRevisionAfterUpdateSpecScenario(startRevision, startReadiness, nextRevision,
                ReadinessEnum.READY, nextStageContext);
    }

    @ParameterizedTest
    @MethodSource("provideForLDRUpdateTest")
    public void latestDeployedRevisionAfterUpdateSameRevisionTest(ReadinessEnum startReadiness,
                                                                  ReadinessEnum nextReadiness) {
        int revision = LatestDeployedRevisionTestUtils.DEFAULT_START_SPEC_REVISION;

        var startStageContext = LatestDeployedRevisionTestUtils.DEFAULT_STAGE_CONTEXT;

        /*
        StageContext should be changed for processing with the same revision
         */
        var nextStageContext = startStageContext.withAcl(
                new Acl(ImmutableList.of(ACL_READ_ENTRY, ACL_WRITE_ENTRY))
        );

        latestDeployedRevisionAfterUpdateSpecScenario(revision, startReadiness, revision, nextReadiness,
                nextStageContext);
    }

    private static void latestDeployedRevisionAfterUpdateSpecScenario(int startRevision,
                                                                      ReadinessEnum startReadiness,
                                                                      int nextRevision,
                                                                      ReadinessEnum nextReadiness,
                                                                      StageContext nextStageContext) {
        var test = LatestDeployedRevisionTestUtils.createDefaultTest(startRevision);
        test.process(DeployUnitController::start, startReadiness);

        int latestDeployedRevisionAfterStart =
                startReadiness.getExpectedLatestDeployedRevision(DEFAULT_LATEST_DEPLOYED_REVISION, startRevision);

        var nextSpec = test.getDeployUnitSpec().withRevision(nextRevision);

        test.process(
                deployUnitController -> deployUnitController.updateSpec(nextSpec, nextStageContext),
                nextReadiness
        );
        test.ensureLatestDeployRevision(nextReadiness, latestDeployedRevisionAfterStart, nextRevision);
    }

    @ParameterizedTest
    @MethodSource("provideForLDRUpdateTest")
    public void targetRevisionAfterUpdateNextRevisionTest(ReadinessEnum startReadiness,
                                                          ReadinessEnum nextReadiness) {
        int startRevision = LatestDeployedRevisionTestUtils.DEFAULT_START_SPEC_REVISION;
        int nextRevision = startRevision + 1;

        var nextStageContext = LatestDeployedRevisionTestUtils.DEFAULT_STAGE_CONTEXT;
        targetRevisionAfterUpdateSpecScenario(
                startRevision, startReadiness, nextRevision, nextReadiness, nextStageContext
        );
    }

    @ParameterizedTest
    @EnumSource(ReadinessEnum.class)
    public void targetRevisionAfterUpdatePreviousRevisionTest(ReadinessEnum startReadiness) {
        int startRevision = LatestDeployedRevisionTestUtils.DEFAULT_START_SPEC_REVISION;
        int nextRevision = startRevision - 1;

        var nextStageContext = LatestDeployedRevisionTestUtils.DEFAULT_STAGE_CONTEXT;
        targetRevisionAfterUpdateSpecScenario(
                startRevision, startReadiness, nextRevision, ReadinessEnum.READY, nextStageContext
        );
    }

    @ParameterizedTest
    @MethodSource("provideForLDRUpdateTest")
    public void targetRevisionAfterUpdateSameRevisionTest(ReadinessEnum startReadiness,
                                                          ReadinessEnum nextReadiness) {
        int revision = LatestDeployedRevisionTestUtils.DEFAULT_START_SPEC_REVISION;

        var startStageContext = LatestDeployedRevisionTestUtils.DEFAULT_STAGE_CONTEXT;

        /*
        StageContext should be changed for processing with the same revision
         */
        var nextStageContext = startStageContext.withAcl(
                new Acl(ImmutableList.of(ACL_READ_ENTRY, ACL_WRITE_ENTRY))
        );

        targetRevisionAfterUpdateSpecScenario(revision, startReadiness, revision, nextReadiness, nextStageContext);
    }

    private static void targetRevisionAfterUpdateSpecScenario(int startRevision,
                                                              ReadinessEnum startReadiness,
                                                              int nextTargetRevision,
                                                              ReadinessEnum nextReadiness,
                                                              StageContext nextStageContext) {
        var test = LatestDeployedRevisionTestUtils.createDefaultTest(startRevision);
        test.process(DeployUnitController::start, startReadiness);

        int targetRevisionAfterStart = startReadiness.getExpectedTargetRevision(
                LatestDeployedRevisionTestUtils.DEFAULT_START_SPEC_REVISION, startRevision
        );

        var nextSpec = test.getDeployUnitSpec().withRevision(nextTargetRevision);

        test.process(
                deployUnitController -> deployUnitController.updateSpec(nextSpec, nextStageContext),
                nextReadiness
        );
        test.ensureTargetRevision(nextReadiness, targetRevisionAfterStart, nextTargetRevision);
    }

    @Test
    void processUpdateIfStageFqidWasChanged() {
        CustomDeployPrimitiveController deployPrimitiveController = new CustomDeployPrimitiveController();
        AtomicInteger upperUpdateHandlerCounter = new AtomicInteger(0);
        AtomicReference<DeployUnitStatus> lastStatus = new AtomicReference<>();

        DeployUnitController controller = new DeployUnitControllerTestBuilder()
                .setDeployPrimitiveController(deployPrimitiveController)
                .setStatusUpdateHandler(upperUpdateHandlerCounter, lastStatus)
                .build();

        controller.start();

        assertThat(deployPrimitiveController.receivedContext, optionalWithValue());
        assertThat(upperUpdateHandlerCounter.get(), equalTo(1));

        deployPrimitiveController.receivedContext = empty();

        // update spec
        controller.updateSpec(getSpec(UNIT_DETAILS), STAGE_CONTEXT1);

        assertThat(deployPrimitiveController.receivedContext, emptyOptional());
        assertThat(upperUpdateHandlerCounter.get(), equalTo(1));

        // update spec
        controller.updateSpec(
                getSpec(UNIT_DETAILS),
                STAGE_CONTEXT1.withStageFqid("new_stage_fqid")
        );

        assertThat(deployPrimitiveController.receivedContext, optionalWithValue());
        assertThat(upperUpdateHandlerCounter.get(), equalTo(2));
    }
}
