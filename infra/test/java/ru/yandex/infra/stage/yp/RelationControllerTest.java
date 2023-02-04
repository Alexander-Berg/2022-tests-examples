package ru.yandex.infra.stage.yp;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Try;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.controller.dto.RelationMeta;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.metrics.MapGaugeRegistry;
import ru.yandex.infra.controller.yp.DummyYpObjectRepository;
import ru.yandex.infra.controller.yp.SelectedObjects;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.stage.TestData;
import ru.yandex.yp.YpRawObjectService;
import ru.yandex.yp.client.api.TRelationSpec;
import ru.yandex.yp.client.api.TRelationStatus;
import ru.yandex.yp.model.YpError;
import ru.yandex.yp.model.YpErrorCodes;
import ru.yandex.yp.model.YpException;
import ru.yandex.yp.model.YpSelectedObjects;
import ru.yandex.yp.model.YpTransaction;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.infra.controller.testutil.FutureUtils.get5s;

class RelationControllerTest {

    public static final Acl DEFAULT_ACL = new Acl(ImmutableList.of(TestData.ACL_WRITE_ENTRY, TestData.ACL_READ_ENTRY));
    private static final String FROM_FQID = "yp|sas-test|stage|vzstage9|d7c28a6b-a59aedd2-25eb200a-301468c4";
    private static final String TO_FQID = "yp|sas-test|replica_set|vzstage9.deployUnit|6663e2b0-de8c3921-c96d8141-4be3c037";

    private static final String FROM_FQID2 = "yp|sas-test|stage|test_stage|1d457f2b-84fb4d5e-c7c2624-9d12e8b9";
    private static final String TO_FQID2 = "yp|sas-test|replica_set|test_stage.deployUnit|169fcfcb-14d12709-2afd6b66-dbffa2aa";

    private static final String STAGE_ID_WITH_ABSENT_RELATION = "stage_without_relation";
    private static final String STAGE_WITH_ABSENT_RELATION_UUID = "stageUuid";
    private static final String STAGE_FQID_WITH_ABSENT_RELATION =
            TestData.composeStageFqid(STAGE_ID_WITH_ABSENT_RELATION, STAGE_WITH_ABSENT_RELATION_UUID);
    private static final String RS_FQID_WITH_ABSENT_RELATION = "yp|sas-test|replica_set|rs_without_relation|uuid2";

    public static final String ID1 = "a4ool73fsdyfx97j";
    public static final String ID2 = "950txbei6ngd5ijp";

    private static final YpObject<RelationMeta, TRelationSpec, TRelationStatus> RELATION_META1 =
            new YpObject.Builder<RelationMeta, TRelationSpec, TRelationStatus>()
                    .setMeta(new RelationMeta(ID1, DEFAULT_ACL, "fqid1", "3668e366-7bfb8964-b36f3c63-a4da95d4", 0,
                            FROM_FQID, TO_FQID))
                    .build();
    private static final YpObject<RelationMeta, TRelationSpec, TRelationStatus> RELATION_META2 =
            new YpObject.Builder<RelationMeta, TRelationSpec, TRelationStatus>()
                    .setMeta(new RelationMeta(ID2, DEFAULT_ACL, "fqid2", "6567db9f-aa8b171a-ff7b064e-623d4b2d", 0,
                            FROM_FQID2, TO_FQID2))
                    .build();

    private static final StageMeta STAGE_META1 = new StageMeta("vzstage9", DEFAULT_ACL, FROM_FQID, "stage_uuid1", 0,
            "projectId");
    private static final StageMeta STAGE_META2 = new StageMeta("test_stage", DEFAULT_ACL, FROM_FQID2, "stage_uuid2",
            0, "projectId");
    private static final StageMeta STAGE_META_MISSED_RELATION = new StageMeta(STAGE_ID_WITH_ABSENT_RELATION,
            DEFAULT_ACL, STAGE_FQID_WITH_ABSENT_RELATION, STAGE_WITH_ABSENT_RELATION_UUID, 0, "projectId");
    public static final Map<Tuple2<String, String>, String> SOME_RELATIONS = Map.of(
            new Tuple2<>(FROM_FQID, TO_FQID), ID1,
            new Tuple2<>(FROM_FQID2, TO_FQID2), ID2);

    private static final Duration syncTimeout = Duration.ofSeconds(10);
    private DummyYpObjectRepository<RelationMeta, TRelationSpec, TRelationStatus> repository;
    private YpRawObjectService objectService;
    private MapGaugeRegistry gaugeRegistry;
    private RelationControllerImpl controller;

    private List<Tuple2<Tuple2<String, String>, String>> storedRelations;

    @BeforeEach
    void before() {
        gaugeRegistry = new MapGaugeRegistry();

        repository = new DummyYpObjectRepository<>();
        repository.selectResponse = completedFuture(new SelectedObjects<>(ImmutableMap.of(
                RELATION_META1.getMeta().getId(), Try.success(RELATION_META1),
                RELATION_META2.getMeta().getId(), Try.success(RELATION_META2)), 0L));
        storedRelations = List.of(
                Tuple2.tuple(Tuple2.tuple(FROM_FQID, TO_FQID), RELATION_META1.getMeta().getId()),
                Tuple2.tuple(Tuple2.tuple(FROM_FQID2, TO_FQID2), RELATION_META2.getMeta().getId()));

        objectService = Mockito.mock(YpRawObjectService.class);
        doReturn(completedFuture(new YpSelectedObjects<>(storedRelations, 0))).when(objectService).selectObjects(any(), any());
        doReturn(completedFuture(0L)).when(objectService).generateTimestamp();
        doReturn(completedFuture(new YpTransaction("id", 0, 0))).when(objectService).startTransaction();
        doReturn(completedFuture(0L)).when(objectService).commitTransaction(any());
        doReturn(completedFuture(0)).when(objectService).abortTransaction(any());
        doReturn(completedFuture(null)).when(objectService).removeObject(any());

        mockStageRepositoryReturnValue(STAGE_META1);
        mockStageRepositoryReturnValue(STAGE_META2);
        mockStageRepositoryReturnValue(STAGE_META_MISSED_RELATION);

        doReturn(completedFuture(0)).when(objectService).updateObject(argThat(arg -> arg.getSetUpdates().get(0).getPath().equals("/control/add_deploy_child")), any());
    }

    private void mockStageRepositoryReturnValue(StageMeta stageMeta) {
        doReturn(CompletableFuture.completedFuture(stageMeta))
                .when(objectService)
                .getObject(argThat(arg -> arg.getId().getId().equals(stageMeta.getId())), any());
    }

    @AfterEach
    void after() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    private void createController(boolean addMissedRelations, Duration cacheResetInterval) {
        createController(addMissedRelations, true, cacheResetInterval);
    }

    private void createController(boolean addMissedRelations, boolean removeRelations, Duration cacheResetInterval) {
        createController(addMissedRelations, removeRelations, cacheResetInterval, Duration.ofSeconds(1));
    }

    private void createController(boolean addMissedRelations, boolean removeRelations, Duration cacheResetInterval, Duration initRetryInterval) {
        controller = new RelationControllerImpl(objectService, gaugeRegistry, addMissedRelations, removeRelations,
                cacheResetInterval, initRetryInterval, syncTimeout,  100500);
    }

    @Test
    void loadRelationsOnStart() {
        createController(false, Duration.ofMinutes(1));

        assertThat(gaugeRegistry.getGaugeValue(RelationControllerImpl.METRIC_RELATIONS_RELOAD_COUNT), equalTo(1L));
        assertThat(gaugeRegistry.getGaugeValue(RelationControllerImpl.METRIC_RELATIONS), equalTo(2));
    }

    @Test
    void containsRelation() {
        createController(false, Duration.ofMinutes(1));

        assertTrue(controller.containsRelation(FROM_FQID, TO_FQID));
        assertTrue(controller.containsRelation(FROM_FQID2, TO_FQID2));

        assertFalse(controller.containsRelation(STAGE_FQID_WITH_ABSENT_RELATION, RS_FQID_WITH_ABSENT_RELATION));
        assertFalse(controller.containsRelation(STAGE_FQID_WITH_ABSENT_RELATION, TO_FQID));
        assertFalse(controller.containsRelation(FROM_FQID, RS_FQID_WITH_ABSENT_RELATION));
    }

    @ParameterizedTest
    @CsvSource({
            YpErrorCodes.DUPLICATE_OBJECT_ID + ",true",
            YpErrorCodes.AUTHENTICATION_ERROR + ",false"
    })
    void recoverFromYpAlreadyExistError(int ypErrorCode, boolean expectedContainsRelationResult) {
        doReturn(failedFuture(new YpException("very message", "much description", new YpError(ypErrorCode,
                "Object already exist", Collections.emptyMap(), Collections.emptyList()), null)))
                .when(objectService).updateObject(any(), any());

        createController(false, Duration.ofMinutes(1));


        assertThrows(YpException.class, () -> get5s(controller.addRelation(STAGE_FQID_WITH_ABSENT_RELATION,
                RS_FQID_WITH_ABSENT_RELATION)));
        assertThat(controller.containsRelation(STAGE_FQID_WITH_ABSENT_RELATION, RS_FQID_WITH_ABSENT_RELATION),
                equalTo(expectedContainsRelationResult));
    }

    @Test
    void addRelation() {
        createController(false, Duration.ofMinutes(1));

        assertDoesNotThrow(() -> get5s(controller.addRelation(STAGE_FQID_WITH_ABSENT_RELATION,
                RS_FQID_WITH_ABSENT_RELATION)));
        assertTrue(controller.containsRelation(STAGE_FQID_WITH_ABSENT_RELATION, RS_FQID_WITH_ABSENT_RELATION));
    }

    @Test
    void removeRelation() {
        doReturn(completedFuture(0)).when(objectService)
                .updateObject(argThat(arg -> arg.getSetUpdates().get(0).getPath().equals("/control/remove_deploy_child")), any());

        createController(false, Duration.ofMinutes(1));
        assertTrue(controller.containsRelation(FROM_FQID, TO_FQID));
        assertDoesNotThrow(() -> get5s(controller.removeRelation(FROM_FQID, TO_FQID)));
        assertFalse(controller.containsRelation(FROM_FQID, TO_FQID));

        assertThat(gaugeRegistry.getGaugeValue(RelationControllerImpl.METRIC_REMOVE_RELATION_COUNT), equalTo(1L));
        assertThat(gaugeRegistry.getGaugeValue(RelationControllerImpl.METRIC_FAILED_REMOVE_RELATION_COUNT),
                equalTo(0L));
    }

    @Test
    void removeAbsentRelation() {
        createController(false, Duration.ofMinutes(1));
        assertFalse(controller.containsRelation(STAGE_FQID_WITH_ABSENT_RELATION, RS_FQID_WITH_ABSENT_RELATION));

        repository.selectResponse = completedFuture(new SelectedObjects<>(ImmutableMap.of(), 0L));
        assertThrows(RuntimeException.class, () -> get5s(controller.removeRelation(STAGE_FQID_WITH_ABSENT_RELATION,
                RS_FQID_WITH_ABSENT_RELATION)));

        assertThat(gaugeRegistry.getGaugeValue(RelationControllerImpl.METRIC_REMOVE_RELATION_COUNT), equalTo(1L));
        assertThat(gaugeRegistry.getGaugeValue(RelationControllerImpl.METRIC_FAILED_REMOVE_RELATION_COUNT),
                equalTo(1L));
    }

    @ParameterizedTest
    @CsvSource({
            "false, 0",
            "true, 1"
    })
    void allowAddMissedRelation(boolean addMissedRelations, int expectedNewRelationsCount) {
        createController(addMissedRelations, Duration.ofMinutes(1));


        assertThat(controller.containsRelation(STAGE_FQID_WITH_ABSENT_RELATION, RS_FQID_WITH_ABSENT_RELATION),
                equalTo(addMissedRelations));

        if (addMissedRelations) {
            verify(objectService).updateObject(argThat(arg -> arg.getId().getId().equals(STAGE_ID_WITH_ABSENT_RELATION)), any());
        } else {
            verify(objectService, never()).updateObject(any(), any());
        }

        assertThat(gaugeRegistry.getGaugeValue(RelationControllerImpl.METRIC_ADD_DEPLOY_CHILD_COUNT),
                equalTo((long) expectedNewRelationsCount));
        assertThat(gaugeRegistry.getGaugeValue(RelationControllerImpl.METRIC_FAILED_ADD_DEPLOY_CHILD_COUNT),
                equalTo(0L));
    }

    @ParameterizedTest
    @CsvSource({
            "false, 0",
            "true, 1"
    })
    void stageWasReplacedWithTheSameNameButNewFqid(boolean addMissedRelations, long expectedAddDeployChildCalls) {

        final String theSameStageNameButAnotherFqid = TestData.composeStageFqid(STAGE_ID_WITH_ABSENT_RELATION,
                "wrong_uuid");

        createController(addMissedRelations, Duration.ofMinutes(1));
        assertThat(controller.containsRelation(theSameStageNameButAnotherFqid, RS_FQID_WITH_ABSENT_RELATION),
                equalTo(addMissedRelations));

        verify(objectService, never()).updateObject(any(), any());
        assertThat(gaugeRegistry.getGaugeValue(RelationControllerImpl.METRIC_ADD_DEPLOY_CHILD_COUNT),
                equalTo(expectedAddDeployChildCalls));
        assertThat(gaugeRegistry.getGaugeValue(RelationControllerImpl.METRIC_FAILED_ADD_DEPLOY_CHILD_COUNT),
                equalTo(expectedAddDeployChildCalls));
    }


    @Test
    void firstLoadFailed() {
        doReturn(failedFuture(new RuntimeException("Some throw")),
                completedFuture(new YpSelectedObjects<>(storedRelations, 0)))
                .when(objectService).selectObjects(any(), any());
        createController(true, true, Duration.ofMinutes(1), Duration.ofMillis(10));
        verify(objectService, Mockito.atLeast(2)).selectObjects(any(), any());
        Assertions.assertEquals(controller.getAllRelations(), SOME_RELATIONS);
    }
}
