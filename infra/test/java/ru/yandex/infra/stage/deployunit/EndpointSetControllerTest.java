package ru.yandex.infra.stage.deployunit;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.yp.DummyObjectLifeCycleManager;
import ru.yandex.infra.stage.yp.SpecStatusMeta;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yp.client.api.DataModel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static ru.yandex.infra.stage.util.CustomMatchers.isReady;

class EndpointSetControllerTest {
    private final static String ENDPOINT_SET_ID = "endpoint_id";
    private final static SchemaMeta META = new SchemaMeta(ENDPOINT_SET_ID, TestData.STAGE_ACL, "", "", 0);
    private final static DataModel.TEndpointSetSpec DEFAULT_SPEC = DataModel.TEndpointSetSpec.newBuilder()
            .setPodFilter("filter")
            .setPort(137)
            .build();

    private class CountRunnable implements Consumer<ReadinessStatus> {
        int countRuns = 0;

        @Override
        public void accept(ReadinessStatus readinessStatus) {
            ++countRuns;
        }
    }

    private DummyObjectLifeCycleManager<SchemaMeta, DataModel.TEndpointSetSpec, DataModel.TEndpointSetStatus> repository;
    private CountRunnable updateHandler;
    private EndpointSetController controller;

    @BeforeEach
    void before() {
        repository = new DummyObjectLifeCycleManager<>();
        updateHandler = new CountRunnable();
        controller = new EndpointSetController(ENDPOINT_SET_ID, repository, updateHandler);
    }

    @Test
    void startManagingTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        assertThat(repository.wasStarted.get(ENDPOINT_SET_ID), equalTo(true));
    }

    @Test
    void createTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        repository.notifySubscriber(ENDPOINT_SET_ID, Optional.empty());
        assertThat(repository.getCurrentSpec(ENDPOINT_SET_ID), equalTo(DEFAULT_SPEC));
        assertThat(repository.wasCreated.get(ENDPOINT_SET_ID), equalTo(true));

        repository.successCall.run();
        assertThat(updateHandler.countRuns, equalTo(1));
        assertThat(controller.getStatus().getReadiness(), not(isReady()));
    }

    @Test
    void updateTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        repository.notifySubscriber(ENDPOINT_SET_ID, Optional.of(new SpecStatusMeta<>(DataModel.TEndpointSetSpec.newBuilder()
                .setPodFilter(DEFAULT_SPEC.getPodFilter())
                .setPort(DEFAULT_SPEC.getPort() + 1)
                .build(), DataModel.TEndpointSetStatus.getDefaultInstance(), META, 1, 1)));
        assertThat(repository.getCurrentSpec(ENDPOINT_SET_ID), equalTo(DEFAULT_SPEC));
        assertThat(repository.wasUpdated.get(ENDPOINT_SET_ID), equalTo(true));
        assertThat(updateHandler.countRuns, equalTo(2));

        //repository.successCalls.run();
        assertThat(updateHandler.countRuns, equalTo(2));
        assertThat(controller.getStatus().getReadiness(), not(isReady()));
    }

    @Test
    void resetSupervisorLabelTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        repository.notifySubscriber(ENDPOINT_SET_ID, Optional.of(new SpecStatusMeta<>(DEFAULT_SPEC,
                DataModel.TEndpointSetStatus.getDefaultInstance(), META, 1, 1,
                Map.of(EndpointSetController.SUPERVISOR_LABEL_KEY, YTree.stringNode("freezed-2022-04-24")))));
        assertThat(repository.wasUpdated.get(ENDPOINT_SET_ID), equalTo(true));
        assertThat(repository.removedLabels, equalTo(Map.of(ENDPOINT_SET_ID, EndpointSetController.SUPERVISOR_LABEL_KEY)));
    }

    @Test
    void notUpdateTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        repository.notifySubscriber(ENDPOINT_SET_ID, Optional.of(new SpecStatusMeta<>(DEFAULT_SPEC,
                DataModel.TEndpointSetStatus.getDefaultInstance(), META, 1, 1)));

        assertThat(repository.wasUpdated, not(hasKey(ENDPOINT_SET_ID)));

        assertThat(updateHandler.countRuns, equalTo(1));
        assertThat(controller.getStatus().getReadiness(), isReady());
    }

    @Test
    void onFailureTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        repository.notifySubscriber(ENDPOINT_SET_ID, Optional.empty());
        repository.failureCall.accept(new RuntimeException("err"));

        assertThat(repository.getCurrentSpec(ENDPOINT_SET_ID), equalTo(DEFAULT_SPEC));
        assertThat(updateHandler.countRuns, equalTo(2));
        assertThat(controller.getStatus().getReadiness(), not(isReady()));
    }

    @Test
    void containsSupervisorLabelWithFreezeValueTest() {
        Predicate<Map<String, YTreeNode>> hasSupervisor = EndpointSetController::containsFreezeLabel;
        assertThat(hasSupervisor.test(Map.of()), equalTo(false));
        assertThat(hasSupervisor.test(Map.of("somelabel", YTree.stringNode("value"))), equalTo(false));
        assertThat(hasSupervisor.test(Map.of(EndpointSetController.SUPERVISOR_LABEL_KEY, YTree.stringNode(""))), equalTo(false));
        assertThat(hasSupervisor.test(Map.of(EndpointSetController.SUPERVISOR_LABEL_KEY, YTree.stringNode("value"))), equalTo(false));
        assertThat(hasSupervisor.test(
                Map.of(EndpointSetController.SUPERVISOR_LABEL_KEY, YTree.mapBuilder().buildMap())
        ), equalTo(false));

        assertThat(hasSupervisor.test(
                Map.of(EndpointSetController.SUPERVISOR_LABEL_KEY,
                        YTree.stringNode(EndpointSetController.SUPERVISOR_LABEL_VALUE))
        ), equalTo(true));
    }
}
