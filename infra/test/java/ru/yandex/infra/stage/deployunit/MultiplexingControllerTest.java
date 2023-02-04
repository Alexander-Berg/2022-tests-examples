package ru.yandex.infra.stage.deployunit;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.stage.TestData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class MultiplexingControllerTest {
    private static final String CLUSTER1 = "cluster1";
    private static final String CLUSTER2 = "cluster2";
    private static final String SPEC = "spec";
    private static final String NEW_SPEC = "new_spec";

    private DummyObjectController<String, ReadyStatus> controller1;
    private DummyObjectController<String, ReadyStatus> controller2;
    private MultiplexingController<String, ReadyStatus> multiplexor;

    @BeforeEach
    void before() {
        controller1 = new DummyObjectController<>();
        controller2 = new DummyObjectController<>();

        Map<String, DummyObjectController<String, ReadyStatus>> perClusterControllerMap = ImmutableMap.of(
                CLUSTER1, controller1,
                CLUSTER2, controller2
        );

        multiplexor = new MultiplexingController<>("id",
                (id, cluster, updateNotifier) -> perClusterControllerMap.get(cluster), "test", (v) -> {
        });
    }

    @Test
    void createAfterRemoval() {
        multiplexor.syncParallel(ImmutableMap.of(CLUSTER1, SPEC, CLUSTER2, SPEC), TestData.DEFAULT_STAGE_CONTEXT);
        assertStarted(controller2);
        multiplexor.syncParallel(ImmutableMap.of(CLUSTER1, SPEC), TestData.DEFAULT_STAGE_CONTEXT);
        assertStopped(controller2);
        multiplexor.syncParallel(ImmutableMap.of(CLUSTER1, SPEC, CLUSTER2, SPEC), TestData.DEFAULT_STAGE_CONTEXT);
        assertStarted(controller2);
    }

    @Test
    void updateSpecOnSameClusters() {
        multiplexor.syncParallel(ImmutableMap.of(CLUSTER1, SPEC), TestData.DEFAULT_STAGE_CONTEXT);
        multiplexor.syncParallel(ImmutableMap.of(CLUSTER1, NEW_SPEC), TestData.DEFAULT_STAGE_CONTEXT);
        assertThat(controller1.getCurrentSpec().get(), equalTo(NEW_SPEC));
        assertStopped(controller2);
    }

    @Test
    void updateSpecWithAddingClusters() {
        multiplexor.syncParallel(ImmutableMap.of(CLUSTER1, SPEC), TestData.DEFAULT_STAGE_CONTEXT);
        multiplexor.syncParallel(ImmutableMap.of(CLUSTER1, NEW_SPEC, CLUSTER2, NEW_SPEC), TestData.DEFAULT_STAGE_CONTEXT);
        assertThat(controller1.getCurrentSpec().get(), equalTo(NEW_SPEC));
        assertThat(controller2.getCurrentSpec().get(), equalTo(NEW_SPEC));
    }

    @Test
    void updateSpecWithRemovingClusters() {
        multiplexor.syncParallel(ImmutableMap.of(CLUSTER1, SPEC, CLUSTER2, SPEC), TestData.DEFAULT_STAGE_CONTEXT);
        multiplexor.syncParallel(ImmutableMap.of(CLUSTER1, NEW_SPEC), TestData.DEFAULT_STAGE_CONTEXT);
        assertThat(controller1.getCurrentSpec().get(), equalTo(NEW_SPEC));
        assertStopped(controller2);
    }

    private static void assertStarted(DummyObjectController controller) {
        assertThat("Controller should be started", !controller.isShutdown());
    }

    private static void assertStopped(DummyObjectController controller) {
        assertThat("Controller should be stopped", controller.isShutdown());
    }

    @Test
    void changeTypeOfControllerTest() {
        var multiplexingController = new MultiplexingController(
                "id",
                (id, cluster, updateNotifier) -> new DummyObjectController(),
                "",
                (c) -> { }
        );
        assertThat(multiplexingController.getSyncControllerType(),
                equalTo(DeployController.DeployControllerType.PARALLEL));
        multiplexingController.getSequentialController(
                Collections.emptyMap(),
                TestData.DEFAULT_STAGE_CONTEXT.withAcl(
                        new Acl(Collections.emptyList())
                ),
                Collections.emptyMap(),
                Collections.emptyList()
        );
        assertThat(multiplexingController.getSyncControllerType(),
                equalTo(DeployController.DeployControllerType.SEQUENTIAL));
        multiplexingController.getParallelController();
        assertThat(multiplexingController.getSyncControllerType(),
                equalTo(DeployController.DeployControllerType.PARALLEL));
    }
}
