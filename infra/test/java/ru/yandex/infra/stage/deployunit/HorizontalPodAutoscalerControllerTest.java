package ru.yandex.infra.stage.deployunit;

import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.HorizontalPodAutoscalerMeta;
import ru.yandex.infra.stage.yp.AclUpdater;
import ru.yandex.infra.stage.yp.DummyObjectLifeCycleManager;
import ru.yandex.infra.stage.yp.SpecStatusMeta;
import ru.yandex.yp.client.api.THorizontalPodAutoscalerSpec;
import ru.yandex.yp.client.api.THorizontalPodAutoscalerStatus;
import ru.yandex.yp.client.api.TReplicaSetScaleSpec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static ru.yandex.infra.stage.util.CustomMatchers.isReady;

class HorizontalPodAutoscalerControllerTest {
    private final static String REPLICA_SET_PARENT_ID = "replica_set_id";
    private final static String HORIZONTAL_POD_AUTOSCALER_ID = REPLICA_SET_PARENT_ID;
    private final static HorizontalPodAutoscalerMeta META = new HorizontalPodAutoscalerMeta(
            HORIZONTAL_POD_AUTOSCALER_ID, TestData.STAGE_ACL, "", "", 0, REPLICA_SET_PARENT_ID);
    private final static THorizontalPodAutoscalerSpec DEFAULT_SPEC = THorizontalPodAutoscalerSpec.newBuilder()
            .setReplicaSet(TReplicaSetScaleSpec.newBuilder()
                    .setMinReplicas(1)
                    .setMaxReplicas(2)
                    .build())
            .build();

    private class CountRunnable implements Consumer<ReadinessStatus> {
        int countRuns = 0;

        @Override
        public void accept(ReadinessStatus readinessStatus) {
            ++countRuns;
        }
    }


    private DummyObjectLifeCycleManager<HorizontalPodAutoscalerMeta, THorizontalPodAutoscalerSpec, THorizontalPodAutoscalerStatus> repository;
    private CountRunnable updateHandler;
    private HorizontalPodAutoscalerController controller;

    @BeforeEach
    void before() {
        repository = new DummyObjectLifeCycleManager<>();
        updateHandler = new CountRunnable();
        controller = new HorizontalPodAutoscalerController(HORIZONTAL_POD_AUTOSCALER_ID, repository, updateHandler, AclUpdater.IDENTITY);
    }

    @Test
    void startManagingTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        assertThat(repository.wasStarted.get(HORIZONTAL_POD_AUTOSCALER_ID), equalTo(true));
    }

    @Test
    void createTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        repository.notifySubscriber(HORIZONTAL_POD_AUTOSCALER_ID, Optional.empty());
        assertThat(repository.getCurrentSpec(HORIZONTAL_POD_AUTOSCALER_ID), equalTo(DEFAULT_SPEC));
        assertThat(repository.wasCreated.get(HORIZONTAL_POD_AUTOSCALER_ID), equalTo(true));

        repository.successCall.run();
        assertThat(updateHandler.countRuns, equalTo(1));
        assertThat(controller.getStatus().getReadiness(), not(isReady()));
    }

    @Test
    void updateTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        repository.notifySubscriber(HORIZONTAL_POD_AUTOSCALER_ID,
                Optional.of(new SpecStatusMeta<>(THorizontalPodAutoscalerSpec.newBuilder()
                        .setReplicaSet(TReplicaSetScaleSpec.newBuilder()
                                .setMinReplicas(DEFAULT_SPEC.getReplicaSet().getMinReplicas())
                                .setMaxReplicas(DEFAULT_SPEC.getReplicaSet().getMaxReplicas() + 1)
                                .build())
                        .build(), THorizontalPodAutoscalerStatus.getDefaultInstance(), META, 1,
                        1)));
        assertThat(repository.getCurrentSpec(HORIZONTAL_POD_AUTOSCALER_ID), equalTo(DEFAULT_SPEC));
        assertThat(repository.wasUpdated.get(HORIZONTAL_POD_AUTOSCALER_ID), equalTo(true));
        assertThat(updateHandler.countRuns, equalTo(2));

        repository.successCall.run();
        assertThat(updateHandler.countRuns, equalTo(2));
        assertThat(controller.getStatus().getReadiness(), not(isReady()));
    }

    @Test
    void notUpdateTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        repository.notifySubscriber(HORIZONTAL_POD_AUTOSCALER_ID, Optional.of(new SpecStatusMeta<>(DEFAULT_SPEC,
                THorizontalPodAutoscalerStatus.getDefaultInstance(), META, 1, 1)));

        assertThat(repository.wasUpdated, not(hasKey(HORIZONTAL_POD_AUTOSCALER_ID)));

        assertThat(updateHandler.countRuns, equalTo(1));
        assertThat(controller.getStatus().getReadiness(), isReady());
    }

    @Test
    void onFailureTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        repository.notifySubscriber(HORIZONTAL_POD_AUTOSCALER_ID, Optional.empty());
        repository.failureCall.accept(new RuntimeException("err"));

        assertThat(repository.getCurrentSpec(HORIZONTAL_POD_AUTOSCALER_ID), equalTo(DEFAULT_SPEC));
        assertThat(updateHandler.countRuns, equalTo(2));
        assertThat(controller.getStatus().getReadiness(), not(isReady()));
    }
}
