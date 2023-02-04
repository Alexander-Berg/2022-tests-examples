package ru.yandex.infra.stage;

import java.time.Clock;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Try;
import ru.yandex.infra.controller.concurrent.DummyLeaderService;
import ru.yandex.infra.controller.concurrent.LeaderService;
import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.metrics.MapGaugeRegistry;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.stage.yp.DeployObjectId;
import ru.yandex.infra.stage.yp.Retainment;
import ru.yandex.yp.client.api.TProjectSpec;
import ru.yandex.yp.client.api.TProjectStatus;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.matchers.IsFailure.isFailure;

class RootControllerImplTest {
    private static final String STAGE_ID = "stage_id";
    private static final String UNIT_ID = "unit_id";

    private ValidationController mockValidationController;
    private MapGaugeRegistry gaugeRegistry;
    private static final Clock CLOCK = Clock.systemDefaultZone();
    private static final Set<String> SET_STAGES = ImmutableSet.of(STAGE_ID);
    private RootControllerImpl controller;
    private LeaderService leaderService;

    @BeforeEach
    void before() {
        mockValidationController = Mockito.mock(ValidationController.class);
        doReturn(ValidationController.ValidityType.VALID).when(mockValidationController).getValidConditionType();
        doReturn(TestData.RETAINMENT).when(mockValidationController).shouldRetain(any(), any());
        gaugeRegistry = new MapGaugeRegistry();
        leaderService = new DummyLeaderService(new MetricRegistry());
        controller = new RootControllerImpl(id -> mockValidationController, leaderService, gaugeRegistry, CLOCK);
        controller.beginStatisticsCollection();
        controller.processGcForRemovedStages(SET_STAGES, SET_STAGES);
    }

    @Test
    void handleExceptionOnStageCreation() {
        ValidationControllerFactory factory = Mockito.mock(ValidationControllerFactory.class);
        when(factory.createValidationController(any())).thenThrow(new RuntimeException("creation failed"));
        RootControllerImpl controller = new RootControllerImpl(factory, leaderService, gaugeRegistry, CLOCK);
        controller.sync(ImmutableMap.of(STAGE_ID, TestData.STAGE), emptyMap());
        verify(factory).createValidationController(eq(STAGE_ID));
    }

    @Test
    void handleExceptionOnStageUpdate() {
        doThrow(new RuntimeException("update failed")).when(mockValidationController).sync(any(), any());
        controller.sync(ImmutableMap.of(STAGE_ID, TestData.STAGE), ImmutableMap.of(TestData.PROJECT_ID, TestData.PROJECT));
        verify(mockValidationController).sync(TestData.STAGE, Optional.of(TestData.PROJECT));
    }

    @Test
    void stageUpdateWhenProjectIdIsEmpty() {
        Try<YpObject<StageMeta, TStageSpec, TStageStatus>> stage = Try.success(
                new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                        .setSpecAndTimestamp(TestData.PROTO_STAGE_SPEC, 1)
                        .setStatus(TStageStatus.getDefaultInstance())
                        .setMeta(new StageMeta("id", TestData.STAGE_ACL, "", "", 0, ""))
                        .build());

        controller.sync(ImmutableMap.of(STAGE_ID, stage), ImmutableMap.of(TestData.PROJECT_ID, TestData.PROJECT));
        ArgumentCaptor<Optional<Try<YpObject<SchemaMeta, TProjectSpec, TProjectStatus>>>> parameterCaptor = ArgumentCaptor
                .forClass(Optional.class);
        verify(mockValidationController).sync(eq(stage), parameterCaptor.capture());
        assertThat(parameterCaptor.getValue(), emptyOptional());
    }

    @Test
    void stageUpdateWhenProjectIdDoesNotExist() {
        controller.sync(ImmutableMap.of(STAGE_ID, TestData.STAGE), emptyMap());
        ArgumentCaptor<Optional<Try<YpObject<SchemaMeta, TProjectSpec, TProjectStatus>>>> parameterCaptor = ArgumentCaptor
                .forClass(Optional.class);
        verify(mockValidationController).sync(eq(TestData.STAGE), parameterCaptor.capture());
        assertThat(parameterCaptor.getValue(), emptyOptional());
    }

    @Test
    void stageUpdateWhenStageIsFailure() {
        controller.sync(ImmutableMap.of(STAGE_ID, Try.failure(new RuntimeException())), emptyMap());
        ArgumentCaptor<Try<YpObject<StageMeta, TStageSpec, TStageStatus>>> stageParameterCaptor = ArgumentCaptor
                .forClass(Try.class);
        ArgumentCaptor<Optional<Try<YpObject<SchemaMeta, TProjectSpec, TProjectStatus>>>> projectParameterCaptor = ArgumentCaptor
                .forClass(Optional.class);
        verify(mockValidationController).sync(stageParameterCaptor.capture(), projectParameterCaptor.capture());
        assertThat(stageParameterCaptor.getValue(), isFailure());
        assertThat(projectParameterCaptor.getValue(), emptyOptional());
    }

    @Test
    void stageUpdateWhenProjectIsFailure() {
        controller.sync(ImmutableMap.of(STAGE_ID, TestData.STAGE),
                ImmutableMap.of(TestData.PROJECT_ID, Try.failure(new RuntimeException())));

        ArgumentCaptor<Optional<Try<YpObject<SchemaMeta, TProjectSpec, TProjectStatus>>>> parameterCaptor = ArgumentCaptor
                .forClass(Optional.class);
        verify(mockValidationController).sync(eq(TestData.STAGE), parameterCaptor.capture());
        assertThat(parameterCaptor.getValue().get(), isFailure());
    }

    @Test
    void handleExceptionOnStageShutdown() {
        doThrow(new RuntimeException("shutdown failed")).when(mockValidationController).shutdown();
        controller.sync(ImmutableMap.of(STAGE_ID, TestData.STAGE), emptyMap());
        controller.processGcForRemovedStages(emptySet(), emptySet());
        verify(mockValidationController).shutdown();
    }

    @Test
    void metricValidTest() {
        controller.sync(ImmutableMap.of(STAGE_ID, TestData.STAGE), emptyMap());
        controller.buildStatistics();
        assertMetric(1, 0, 0);
    }

    @Test
    void metricInvalidTest() {
        assertFailMetric(ValidationController.ValidityType.INVALID, 0, 1, 0);
    }

    @Test
    void metricParsingFailedTest() {
        assertFailMetric(ValidationController.ValidityType.PARSING_FAILED, 0, 0, 1);
    }

    @Test
    void shouldRetainDelegationTest() {
        controller.sync(ImmutableMap.of(STAGE_ID, TestData.STAGE), emptyMap());
        assertThat(controller.shouldRetain("stage_id.replica_set_id", TestData.CLUSTER_AND_TYPE),
                equalTo(TestData.RETAINMENT));
    }

    @Test
    void shouldRetainEmptyStageSetTest() {
        controller.processGcForRemovedStages(Collections.emptySet(), Collections.emptySet());
        controller.sync(emptyMap(), emptyMap());
        assertThat(controller.shouldRetain(new DeployObjectId(STAGE_ID, UNIT_ID).toStringId(), TestData.CLUSTER_AND_TYPE),
                equalTo(new Retainment(true, "Stage list is empty, will not remove")));
    }

    @Test
    void shouldRetainUnknownLabelsStageTest() {
        String unknownStageId = "NOT_STAGE_ID";
        controller.processGcForRemovedStages(ImmutableSet.of(STAGE_ID), ImmutableSet.of(STAGE_ID, unknownStageId));
        controller.sync(ImmutableMap.of(STAGE_ID, TestData.STAGE), emptyMap());
        assertThat(controller.shouldRetain(new DeployObjectId(unknownStageId, UNIT_ID).toStringId(), TestData.CLUSTER_AND_TYPE),
                equalTo(new Retainment(true, "Stage 'NOT_STAGE_ID' is present with unknown labels")));
    }

    @Test
    void shouldRemoveIfStageAbsent() {
        String unknownStageId = "NOT_STAGE_ID";
        controller.sync(ImmutableMap.of(STAGE_ID, TestData.STAGE), emptyMap());
        assertThat(controller.shouldRetain(new DeployObjectId(unknownStageId, UNIT_ID).toStringId(), TestData.CLUSTER_AND_TYPE),
                equalTo(new Retainment(false, "Stage 'NOT_STAGE_ID' is absent")));
    }

    @Test
    void shouldRetainNotDeployId() {
        controller.sync(ImmutableMap.of(STAGE_ID, TestData.STAGE), emptyMap());
        assertThat(controller.shouldRetain("abcde", TestData.CLUSTER_AND_TYPE),
                equalTo(new Retainment(true, "Object id was not generated by Deploy")));
    }

    private void assertFailMetric(ValidationController.ValidityType type, int valid, int invalid, int parsingFail) {
        doReturn(type).when(mockValidationController).getValidConditionType();
        RootControllerImpl controller = new RootControllerImpl(id -> mockValidationController, leaderService, gaugeRegistry, CLOCK);
        controller.beginStatisticsCollection();
        controller.sync(ImmutableMap.of(STAGE_ID, TestData.STAGE), emptyMap());
        controller.buildStatistics();
        assertMetric(valid, invalid, parsingFail);
    }

    private void assertMetric(int valid, int invalid, int parsingFail) {
        assertThat(gaugeRegistry.getGaugeValue(RootControllerImpl.INVALID_METRIC), equalTo(invalid));
        assertThat(gaugeRegistry.getGaugeValue(RootControllerImpl.VALID_METRIC), equalTo(valid));
        assertThat(gaugeRegistry.getGaugeValue(RootControllerImpl.PARSING_FAILED_METRIC), equalTo(parsingFail));
        assertThat(gaugeRegistry.getGaugeValue(RootControllerImpl.TOTAL_METRIC), equalTo(valid + invalid + parsingFail));
    }
}
