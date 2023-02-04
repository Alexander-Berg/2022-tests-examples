package ru.yandex.infra.stage.deployunit;

import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.DynamicResourceMeta;
import ru.yandex.infra.stage.dto.DynamicResourceRevisionStatus;
import ru.yandex.infra.stage.yp.DummyObjectLifeCycleManager;
import ru.yandex.infra.stage.yp.SpecStatusMeta;
import ru.yandex.yp.client.api.DynamicResource.TDynamicResourceSpec;
import ru.yandex.yp.client.api.DynamicResource.TDynamicResourceStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

class DynamicResourceControllerTest {
    private final static String DYNAMIC_RESOURCE_ID = "dynamic_resource_1";
    private final static String POD_SET_ID = "DeployUnit1";
    private final static DynamicResourceMeta META =
            new DynamicResourceMeta(DYNAMIC_RESOURCE_ID, TestData.STAGE_ACL, "", "", 0, POD_SET_ID);
    private final static TDynamicResourceSpec DEFAULT_SPEC = TestData.DYNAMIC_RESOURCE_SPEC.getDynamicResource();

    private static class CountRunnable implements Consumer<DynamicResourceRevisionStatus> {
        int countRuns = 0;

        @Override
        public void accept(DynamicResourceRevisionStatus o) {
            ++countRuns;
        }
    }

    private DummyObjectLifeCycleManager<DynamicResourceMeta, TDynamicResourceSpec, TDynamicResourceStatus> repository;
    private CountRunnable updateHandler;
    private DynamicResourceController controller;

    @BeforeEach
    void before() {
        repository = new DummyObjectLifeCycleManager<>();
        updateHandler = new CountRunnable();
        controller = new DynamicResourceController(DYNAMIC_RESOURCE_ID, repository, updateHandler, POD_SET_ID,
                TestData.CONVERTER);
    }

    @Test
    void startManagingTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        assertThat(repository.wasStarted.get(DYNAMIC_RESOURCE_ID), equalTo(true));
    }

    @Test
    void createTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        repository.notifySubscriber(DYNAMIC_RESOURCE_ID, Optional.empty());
        assertThat(repository.getCurrentSpec(DYNAMIC_RESOURCE_ID), equalTo(DEFAULT_SPEC));
        assertThat(repository.wasCreated.get(DYNAMIC_RESOURCE_ID), equalTo(true));

        repository.successCall.run();
        assertThat(updateHandler.countRuns, equalTo(0));
    }

    @Test
    void updateTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        repository.notifySubscriber(DYNAMIC_RESOURCE_ID, Optional.of(new SpecStatusMeta<>(
                TDynamicResourceSpec.getDefaultInstance(),
                TDynamicResourceStatus.newBuilder()
                        .addRevisions(TDynamicResourceStatus.TRevisionStatus.newBuilder().setRevision(1))
                        .build(),
                META,
                1, 1)));

        assertThat(repository.getCurrentSpec(DYNAMIC_RESOURCE_ID), equalTo(DEFAULT_SPEC));
        assertThat(repository.wasUpdated.get(DYNAMIC_RESOURCE_ID), equalTo(true));

        assertThat(updateHandler.countRuns, equalTo(1));
        assertThat(controller.getStatus().getRevision(), equalTo(1L));
    }

    @Test
    void notUpdateTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        repository.notifySubscriber(DYNAMIC_RESOURCE_ID, Optional.of(new SpecStatusMeta<>(DEFAULT_SPEC,
                TDynamicResourceStatus.newBuilder()
                        .addRevisions(TDynamicResourceStatus.TRevisionStatus.newBuilder().setRevision(1))
                        .build(), META, 1,
                1)));

        assertThat(repository.wasUpdated, not(hasKey(DYNAMIC_RESOURCE_ID)));

        assertThat(updateHandler.countRuns, equalTo(1));
        assertThat(controller.getStatus().getRevision(), equalTo(1L));
    }

    @Test
    void onFailureTest() {
        controller.sync(DEFAULT_SPEC, TestData.DEFAULT_STAGE_CONTEXT);
        repository.notifySubscriber(DYNAMIC_RESOURCE_ID, Optional.empty());
        repository.failureCall.accept(new RuntimeException("err"));

        assertThat(repository.getCurrentSpec(DYNAMIC_RESOURCE_ID), equalTo(DEFAULT_SPEC));
        assertThat(updateHandler.countRuns, equalTo(0));
    }
}
