package ru.yandex.infra.stage;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Either;
import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.stage.StageController.Status;
import ru.yandex.infra.stage.deployunit.DeployUnitController;
import ru.yandex.infra.stage.deployunit.DeployUnitStats;
import ru.yandex.infra.stage.deployunit.DeployUnitTimeline;
import ru.yandex.infra.stage.dto.ClusterAndType;
import ru.yandex.infra.stage.dto.DeployReadyCriterion;
import ru.yandex.infra.stage.dto.DeployUnitOverrides;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.DeployUnitStatus;
import ru.yandex.infra.stage.dto.DynamicResourceStatus;
import ru.yandex.infra.stage.dto.ReplicaSetDeploymentStrategy;
import ru.yandex.infra.stage.dto.ReplicaSetUnitSpec;
import ru.yandex.infra.stage.dto.RuntimeDeployControls;
import ru.yandex.infra.stage.dto.StageSpec;
import ru.yandex.infra.stage.util.StringUtils;
import ru.yandex.infra.stage.yp.AclUpdater;
import ru.yandex.infra.stage.yp.AppendingAclUpdater;
import ru.yandex.infra.stage.yp.DeployObjectId;
import ru.yandex.infra.stage.yp.Retainment;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.TestData.ABC_ACCOUNT_ID;
import static ru.yandex.infra.stage.TestData.CLUSTER;
import static ru.yandex.infra.stage.TestData.CONVERTER;
import static ru.yandex.infra.stage.TestData.DEPLOY_UNIT_ID;
import static ru.yandex.infra.stage.TestData.DEPLOY_UNIT_SPEC;
import static ru.yandex.infra.stage.TestData.MAX_UNAVAILABLE;
import static ru.yandex.infra.stage.TestData.NETWORK_DEFAULTS;
import static ru.yandex.infra.stage.TestData.POD_AGENT_CONFIG_EXTRACTOR;
import static ru.yandex.infra.stage.TestData.REPLICA_SET_SPEC;

class StageControllerImplTest {
    private static final String STAGE_ID = "stageId";
    private static final int DEFAULT_STAGE_REVISION = TestData.STAGE_SPEC.getRevision();
    private static final String PROJECT_ID = "projectId";
    private static final Matcher<DummyDeployUnitController> IS_STARTED = new IsStartedMatcher();
    private static final String ITYPE = "user_itype";

    private static final Status DEFAULT_STAGE_STATUS = new Status(
            ImmutableMap.of(DEPLOY_UNIT_ID, TestData.DEPLOY_UNIT_STATUS),
            emptyMap()
    );


    private Map<String, DummyDeployUnitController> unitControllers;
    private DeployUnitControllerFactory unitControllerFactory;
    private StageController stageController;
    private DeployUnitController mockUnitController;
    private Consumer<DeployUnitStatus> updateHandler;
    private BiConsumer<String, DynamicResourceStatus> dynamicResourceHandler;

    private static class DummyDeployUnitController implements DeployUnitController {
        StageContext context;
        DeployUnitSpec spec;
        boolean isStarted = false;

        DummyDeployUnitController() {
        }

        @Override
        public void start() {
            isStarted = true;
        }

        @Override
        public void updateSpec(DeployUnitSpec spec, StageContext context) {
            this.spec = spec;
            this.context = context;
        }

        @Override
        public void shutdown() { isStarted = false; }

        @Override
        public void restoreFromStatus(DeployUnitStatus status, StageContext context) {
            updateSpec(status.getCurrentTarget(), context);
        }

        @Override
        public Retainment shouldRetain(ClusterAndType clusterAndType) {
            return TestData.RETAINMENT;
        }

        @Override
        public void addStats(DeployUnitStats.Builder builder) {
        }
    }

    private static class IsStartedMatcher extends BaseMatcher<DummyDeployUnitController> {
        @Override
        public boolean matches(Object actual) {
            if (!(actual instanceof DummyDeployUnitController)) {
                return false;
            }
            return ((DummyDeployUnitController) actual).isStarted;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("unit controller should be started");
        }
    }

    @BeforeEach
    void before() {
        unitControllers = new HashMap<>();
        unitControllerFactory = (spec, unitId, fullUnitId, context, statusUpdateHandler, dynamicResourceStatusHandler, endpointSetStatusHandler) -> {
            DummyDeployUnitController controller = new DummyDeployUnitController();
            controller.updateSpec(spec, context);
            unitControllers.put(fullUnitId, controller);
            updateHandler = statusUpdateHandler;
            dynamicResourceHandler = dynamicResourceStatusHandler;
            return controller;
        };
        stageController = new StageControllerImpl(
                STAGE_ID,
                unitControllerFactory,
                AclUpdater.IDENTITY,
                () -> {},
                CONVERTER,
                GlobalContext.EMPTY
        );
        mockUnitController = Mockito.mock(DeployUnitController.class);
    }

    @Test
    void restoreUnitSpecsFromStatus() {
        int unitRevision = 1;

        DeployUnitStatus unitStatus = new DeployUnitStatus(
                TestData.CONDITION2,
                TestData.CONDITION3,
                DEPLOY_UNIT_SPEC,
                TestData.REPLICA_SET_UNIT_STATUS_DETAILS,
                TestData.DEPLOY_PROGRESS,
                ITYPE,
                new DeployUnitTimeline(
                        unitRevision,
                        Instant.ofEpochMilli(1),
                        TestData.CONDITION1
                )
        );

        var stageStatus = new Status(ImmutableMap.of(DEPLOY_UNIT_ID, unitStatus), emptyMap());

        stageController.restoreFromStatus(
                TestData.DEFAULT_STAGE_FQID,
                DEFAULT_STAGE_REVISION,
                stageStatus,
                TestData.RUNTIME_DEPLOY_CONTROLS,
                emptyMap(),
                ABC_ACCOUNT_ID,
                TestData.STAGE_ACL,
                PROJECT_ID,
                emptyMap()
        );

        String expectedUnitId = STAGE_ID + StringUtils.ID_SEPARATOR + DEPLOY_UNIT_ID;
        assertThat(unitControllers, hasKey(expectedUnitId));

        var expectedStageContext = new StageContext(
                TestData.DEFAULT_STAGE_FQID,
                STAGE_ID,
                DEFAULT_STAGE_REVISION,
                TestData.STAGE_SPEC.getAccountId(),
                TestData.STAGE_ACL,
                unitStatus.getTargetSpecTimestamp(),
                PROJECT_ID,
                emptyMap(),
                emptyMap(),
                TestData.RUNTIME_DEPLOY_CONTROLS,
                emptyMap(),
                GlobalContext.EMPTY
        );

        DummyDeployUnitController unitController = unitControllers.get(expectedUnitId);

        assertThat(unitController.context, equalTo(expectedStageContext));
        assertThat(unitController.spec, equalTo(DEPLOY_UNIT_SPEC));

        assertThat(unitController, IS_STARTED);
    }

    @Test
    void testCreatedUnitId() {
        stageController.sync(TestData.DEFAULT_STAGE_FQID, TestData.STAGE_SPEC, TestData.RUNTIME_DEPLOY_CONTROLS, emptyMap(), 1,
                TestData.STAGE_ACL, PROJECT_ID);

        assertThat(unitControllers, hasKey(STAGE_ID + StringUtils.ID_SEPARATOR + DEPLOY_UNIT_ID));
    }

    @Test
    void addUnitToStage() {
        String newDeployUnitId = "new_unit_id";
        updateStageSpec(ImmutableMap.of(
                DEPLOY_UNIT_ID, DEPLOY_UNIT_SPEC,
                newDeployUnitId, DEPLOY_UNIT_SPEC
        ));

        assertThat(unitControllers, hasKey(makeFullUnitId(newDeployUnitId)));
        assertThat(unitControllers.get(makeFullUnitId(newDeployUnitId)).spec, equalTo(DEPLOY_UNIT_SPEC));
    }

    @Test
    void removeUnitFromStage() {
        updateStageSpec(emptyMap());

        assertThat(unitControllers, hasKey(makeFullUnitId(DEPLOY_UNIT_ID)));
        assertThat(unitControllers.get(makeFullUnitId(DEPLOY_UNIT_ID)), not(IS_STARTED));
    }

    @Test
    void handleExceptionOnUnitCreation() {
        DeployUnitControllerFactory factory = Mockito.mock(DeployUnitControllerFactory.class);
        when(factory.createUnitController(any(), any(), any(), any(), any(), any(), any())).thenThrow(new RuntimeException("creation failed"));
        StageController stageController = new StageControllerImpl(
                STAGE_ID,
                factory,
                AclUpdater.IDENTITY,
                () -> {},
                CONVERTER,
                GlobalContext.EMPTY
        );

        stageController.sync(TestData.DEFAULT_STAGE_FQID, TestData.STAGE_SPEC, TestData.RUNTIME_DEPLOY_CONTROLS, emptyMap(), 1,
                TestData.STAGE_ACL, PROJECT_ID);
        verify(factory).createUnitController(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void handleExceptionOnUnitUpdate() {
        setupStageControllerWithMock();
        doThrow(new RuntimeException("update failed")).when(mockUnitController).updateSpec(any(), any());

        stageController.sync(TestData.DEFAULT_STAGE_FQID, TestData.STAGE_SPEC, TestData.RUNTIME_DEPLOY_CONTROLS, emptyMap(), 1, TestData.STAGE_ACL,
                PROJECT_ID);
        stageController.sync(TestData.DEFAULT_STAGE_FQID, new StageSpec(TestData.STAGE_SPEC.getDeployUnits(),
                        TestData.STAGE_SPEC.getAccountId(), TestData.STAGE_SPEC.getRevision(), false, emptyMap(),
                        emptyMap()), TestData.RUNTIME_DEPLOY_CONTROLS, emptyMap(), 2, TestData.STAGE_ACL,
                PROJECT_ID);
        verify(mockUnitController).updateSpec(any(), any());
    }

    @Test
    void handleExceptionOnUnitShutdown() {
        setupStageControllerWithMock();
        doThrow(new RuntimeException("update failed")).when(mockUnitController).shutdown();

        stageController.sync(TestData.DEFAULT_STAGE_FQID, TestData.STAGE_SPEC, TestData.RUNTIME_DEPLOY_CONTROLS, emptyMap(), 1, TestData.STAGE_ACL,
                PROJECT_ID);
        stageController.sync(TestData.DEFAULT_STAGE_FQID, new StageSpec(emptyMap(),
                        TestData.STAGE_SPEC.getAccountId(), TestData.STAGE_SPEC.getRevision(), false, emptyMap(),
                        emptyMap()), TestData.RUNTIME_DEPLOY_CONTROLS, emptyMap(), 2,
                TestData.STAGE_ACL, PROJECT_ID);
        verify(mockUnitController).shutdown();
    }

    @Test
    void restoreExistingDeployUnitControllerFromStatus() {
        stageController.restoreFromStatus(
                TestData.DEFAULT_STAGE_FQID,
                DEFAULT_STAGE_REVISION,
                DEFAULT_STAGE_STATUS,
                TestData.RUNTIME_DEPLOY_CONTROLS,
                emptyMap(),
                ABC_ACCOUNT_ID,
                TestData.STAGE_ACL,
                PROJECT_ID,
                emptyMap()
        );

        DummyDeployUnitController unitController = unitControllers.get(makeFullUnitId(DEPLOY_UNIT_ID));

        DeployUnitSpec newUnitSpec = DEPLOY_UNIT_SPEC.withNetworkDefaults(
                NETWORK_DEFAULTS.withNetworkId("newId")
        );

        var newStageStatus = new Status(
                ImmutableMap.of(DEPLOY_UNIT_ID,
                        new DeployUnitStatus(
                                TestData.CONDITION2,
                                TestData.CONDITION3,
                                newUnitSpec,
                                TestData.REPLICA_SET_UNIT_STATUS_DETAILS,
                                TestData.DEPLOY_PROGRESS,
                                ITYPE,
                                new DeployUnitTimeline(
                                        2,
                                        Instant.ofEpochMilli(2),
                                        TestData.CONDITION1
                                )
                        )
                ),
                emptyMap()
        );

        stageController.restoreFromStatus(
                TestData.DEFAULT_STAGE_FQID,
                DEFAULT_STAGE_REVISION,
                newStageStatus,
                TestData.RUNTIME_DEPLOY_CONTROLS,
                emptyMap(),
                ABC_ACCOUNT_ID,
                TestData.STAGE_ACL,
                PROJECT_ID,
                emptyMap()
        );

        assertThat(unitControllers.get(makeFullUnitId(DEPLOY_UNIT_ID)), sameInstance(unitController));
        assertThat(unitController.spec, equalTo(newUnitSpec));
    }

    @Test
    void approverTest() {
        stageController.restoreFromStatus(
                TestData.DEFAULT_STAGE_FQID,
                DEFAULT_STAGE_REVISION,
                DEFAULT_STAGE_STATUS,
                TestData.RUNTIME_DEPLOY_CONTROLS,
                emptyMap(),
                ABC_ACCOUNT_ID,
                TestData.STAGE_ACL,
                PROJECT_ID,
                emptyMap()
        );

        assertThat(stageController.shouldRetain(new DeployObjectId(STAGE_ID, DEPLOY_UNIT_ID),
                TestData.CLUSTER_AND_TYPE), equalTo(TestData.RETAINMENT));
        assertThat(stageController.shouldRetain(new DeployObjectId(STAGE_ID, "NOT_DEPLOY_UNIT_ID"),
                TestData.CLUSTER_AND_TYPE),
                equalTo(new Retainment(false, "Deploy unit 'NOT_DEPLOY_UNIT_ID' is not present in stage 'stageId'")));
    }

    @Test
    void updateTest() {
        stageController.sync(TestData.DEFAULT_STAGE_FQID, new StageSpec(ImmutableMap.of(DEPLOY_UNIT_ID, DEPLOY_UNIT_SPEC),
                        "accid", 1, false, emptyMap(), emptyMap()), TestData.RUNTIME_DEPLOY_CONTROLS, emptyMap(), 1,
                TestData.STAGE_ACL, PROJECT_ID);
        updateHandler.accept(TestData.DEPLOY_UNIT_STATUS);
        assertThat(stageController.getStatus().getDeployUnits(),
                equalTo(ImmutableMap.of(DEPLOY_UNIT_ID, TestData.DEPLOY_UNIT_STATUS)));
    }

    @Test
    void removeUnitFromStatusTest() {
        stageController.sync(TestData.DEFAULT_STAGE_FQID, TestData.STAGE_SPEC, TestData.RUNTIME_DEPLOY_CONTROLS, emptyMap(), 1, TestData.STAGE_ACL,
                PROJECT_ID);
        stageController.sync(TestData.DEFAULT_STAGE_FQID, new StageSpec(emptyMap(), TestData.STAGE_SPEC.getAccountId(), 1, false, emptyMap(),
                        emptyMap()), TestData.RUNTIME_DEPLOY_CONTROLS, emptyMap(),
                2, TestData.STAGE_ACL, PROJECT_ID);
        assertThat(stageController.getStatus().getDeployUnits(), anEmptyMap());
    }

    @Test
    void applyAclUpdateOnRestoreStatus() {
        StageController.Status status = new StageController.Status(TestData.STAGE_STATUS.getDeployUnits(), emptyMap());
        applyAclTestTemplate(() -> stageController.restoreFromStatus(
                TestData.DEFAULT_STAGE_FQID,
                DEFAULT_STAGE_REVISION,
                status,
                TestData.RUNTIME_DEPLOY_CONTROLS,
                emptyMap(),
                ABC_ACCOUNT_ID,
                Acl.EMPTY,
                PROJECT_ID,
                emptyMap())
        );
    }

    @Test
    void applyAclUpdateOnSpecUpdate() {
        applyAclTestTemplate(() -> stageController.sync(TestData.DEFAULT_STAGE_FQID, TestData.STAGE_SPEC, TestData.RUNTIME_DEPLOY_CONTROLS, emptyMap(), 100500, Acl.EMPTY,
                PROJECT_ID));
    }

    @Test
    void overridesInRuntimeDeployControlTest() {
        stageController = new StageControllerImpl(
                STAGE_ID,
                unitControllerFactory,
                AclUpdater.IDENTITY,
                () -> {},
                CONVERTER,
                GlobalContext.EMPTY
        );

        ReplicaSetUnitSpec rsSpec = new ReplicaSetUnitSpec(REPLICA_SET_SPEC,
                ImmutableMap.of(
                        CLUSTER, new ReplicaSetUnitSpec.PerClusterSettings(Either.left(100), Optional.of(
                                new ReplicaSetDeploymentStrategy(0, MAX_UNAVAILABLE, 0, 0, 0, 0, Optional.empty(), Optional.of(new DeployReadyCriterion(Optional.of("AUTO"), Optional.of(1), Optional.of(1))))))),
                POD_AGENT_CONFIG_EXTRACTOR);

        StageSpec stageSpec = new StageSpec(
                ImmutableMap.of(DEPLOY_UNIT_ID, DEPLOY_UNIT_SPEC.withDetails(rsSpec)),
                ABC_ACCOUNT_ID, 100500, false, emptyMap(), emptyMap());

        stageController.sync(TestData.DEFAULT_STAGE_FQID, stageSpec, TestData.RUNTIME_DEPLOY_CONTROLS, emptyMap(), 1,
                TestData.STAGE_ACL, PROJECT_ID);

        String duId = STAGE_ID + StringUtils.ID_SEPARATOR + DEPLOY_UNIT_ID;
        assertThat(unitControllers, hasKey(duId));
        assertThat(((ReplicaSetUnitSpec)unitControllers.get(duId).spec.getDetails()).getPerClusterSettings()
                .get(CLUSTER).getDeploymentStrategy().get().getMaxUnavailable(), equalTo(MAX_UNAVAILABLE));

        int newMaxUnavailable = 5;
        DeployUnitOverrides overrides =
                new DeployUnitOverrides(Map.of(CLUSTER, new DeployUnitOverrides.PerClusterOverrides(newMaxUnavailable)),
                        DEPLOY_UNIT_SPEC.getRevision());
        RuntimeDeployControls runtimeDeployControls = new RuntimeDeployControls(emptyMap(), Map.of(DEPLOY_UNIT_ID,
                overrides), emptyMap());

        stageController.sync(TestData.DEFAULT_STAGE_FQID, stageSpec, runtimeDeployControls, emptyMap(), 1,
                TestData.STAGE_ACL, PROJECT_ID);

        assertThat(((ReplicaSetUnitSpec)unitControllers.get(duId).spec.getDetails()).getPerClusterSettings()
                .get(CLUSTER).getDeploymentStrategy().get().getMaxUnavailable(), equalTo(newMaxUnavailable));
        assertThat(unitControllers.get(duId).context.getRuntimeDeployControls(), equalTo(runtimeDeployControls));
    }

    @Test
    void fixOrderOfEnvironmentsFromConstructor() {
        Map<String, String> unsortedMap = ImmutableMap.of("b", "b", "a", "a", "c", "c");
        Map<String, String> sortedMap = ImmutableSortedMap.of("a", "a", "b", "b", "c", "c");

        StageSpec spec = new StageSpec(emptyMap(), TestData.STAGE_SPEC.getAccountId(),
                TestData.STAGE_SPEC.getRevision(), false,
                emptyMap(), unsortedMap);

        assertThat(spec.getEnvVars().toString(), equalTo(sortedMap.toString()));

        StageContext stageContext = new StageContext(TestData.DEFAULT_STAGE_FQID, "stage_id", 100500, "abc:111", TestData.STAGE_ACL, 1, "project_id",
                emptyMap(), emptyMap(), TestData.RUNTIME_DEPLOY_CONTROLS, unsortedMap, GlobalContext.EMPTY);

        assertThat(stageContext.getEnvVars().toString(), equalTo(sortedMap.toString()));
    }

    private void applyAclTestTemplate(Runnable action) {
        AclUpdater aclUpdater = new AppendingAclUpdater(TestData.ACL_WRITE_ENTRY);
        stageController = new StageControllerImpl(
                STAGE_ID,
                unitControllerFactory,
                aclUpdater,
                () -> {},
                CONVERTER,
                GlobalContext.EMPTY
        );
        action.run();
        DummyDeployUnitController unitController = unitControllers.get(makeFullUnitId(DEPLOY_UNIT_ID));
        assertThat(unitController.context.getAcl().getEntries(), contains(TestData.ACL_WRITE_ENTRY));
    }

    private void setupStageControllerWithMock() {
        DeployUnitControllerFactory factory = Mockito.mock(DeployUnitControllerFactory.class);
        when(factory.createUnitController(any(), any(), any(), any(), any(), any(), any())).thenReturn(mockUnitController);
        stageController = new StageControllerImpl(
                STAGE_ID,
                factory,
                AclUpdater.IDENTITY,
                () -> {},
                CONVERTER,
                GlobalContext.EMPTY
        );
    }

    private void updateStageSpec(Map<String, DeployUnitSpec> deployUnitSpecs) {
        long oldTimestamp = 1;
        StageSpec stageSpec = new StageSpec(deployUnitSpecs, ABC_ACCOUNT_ID, 1, false, emptyMap(),
                emptyMap());
        DeployUnitStatus unitStatus = new DeployUnitStatus(
                TestData.CONDITION2,
                TestData.CONDITION3,
                DEPLOY_UNIT_SPEC, TestData.REPLICA_SET_UNIT_STATUS_DETAILS, TestData.DEPLOY_PROGRESS,
                ITYPE,
                new DeployUnitTimeline(
                        1,
                        Instant.ofEpochMilli(oldTimestamp),
                        TestData.CONDITION1
                )
        );
        StageController.Status stageStatus = new StageController.Status(ImmutableMap.of(
                DEPLOY_UNIT_ID, unitStatus
        ), emptyMap());

        stageController.restoreFromStatus(
                TestData.DEFAULT_STAGE_FQID,
                DEFAULT_STAGE_REVISION,
                stageStatus,
                TestData.RUNTIME_DEPLOY_CONTROLS,
                emptyMap(),
                ABC_ACCOUNT_ID,
                TestData.STAGE_ACL,
                PROJECT_ID,
                emptyMap()
        );

        stageController.sync(TestData.DEFAULT_STAGE_FQID, stageSpec, TestData.RUNTIME_DEPLOY_CONTROLS, emptyMap(), oldTimestamp + 1, TestData.STAGE_ACL, PROJECT_ID);
    }

    private static String makeFullUnitId(String unitId) {
        return STAGE_ID + StringUtils.ID_SEPARATOR + unitId;
    }
}
