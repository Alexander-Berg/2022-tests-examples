package ru.yandex.infra.sidecars_updater;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.bolts.collection.Try;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.yp.LabelBasedRepository;
import ru.yandex.infra.controller.yp.SelectedObjects;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.sidecars_updater.sidecar_service.SidecarsService;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yp.client.api.TDeployUnitSpec;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StageCacheTest {
    private final String DEFAULT_STAGE_NAME = "stage";
    public final Map<String, YTreeNode> DEFAULT_STAGE_LABELS = Map.of(
            SidecarsService.DU_SIDECAR_TARGET_LABEL,
            new YTreeBuilder().beginMap().endMap().build(),
            SidecarsService.DU_PATCHERS_TARGET_LABEL,
            new YTreeBuilder().beginMap().endMap().build()
    );
    public final Map<String, Try<YpObject<StageMeta, TStageSpec, TStageStatus>>> DEFAULT_OBJECT_MAP =
            createStageObjectMap(TDeployUnitSpec.newBuilder().build(), DEFAULT_STAGE_LABELS);


    private LabelBasedRepository<StageMeta, TStageSpec, TStageStatus> stageRepo;
    private StageCache stageCache;

    @BeforeEach
    void setUp() {
        stageRepo = spy(mock(LabelBasedRepository.class));
        updateRepo(DEFAULT_OBJECT_MAP);
        stageCache = new StageCache(stageRepo);
    }

    @Test
    public void withoutRefreshTest() {
        stageCache.getStagesFutureCache();
        verify(stageRepo, times(0)).selectObjects(any(), anyMap());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    public void refreshTest(int refreshAmount) {
        for (int i = 0; i < refreshAmount; i++) {
            stageCache.refreshStageCache();
        }
        verify(stageRepo, times(refreshAmount)).selectObjects(any(), anyMap());
    }

    @Test
    void getCacheBeforeRefreshDataTest() throws ExecutionException, InterruptedException {
        assertThat(stageCache.getStagesFutureCache().get(), equalTo(Map.of()));
    }


    // TODO it's not work on JAVA 17
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getCacheAfterRefreshTest(boolean isUpdateRepo) throws ExecutionException, InterruptedException {
        stageCache.refreshStageCache();
        if (isUpdateRepo) {
            updateRepo(Map.of());
        }
        var stagesFutureCache = stageCache.getStagesFutureCache();
        YpObject<StageMeta, TStageSpec, TStageStatus> actualStageYpObject =
                stagesFutureCache.get().get(DEFAULT_STAGE_NAME);
        YpObject<StageMeta, TStageSpec, TStageStatus> expectedStageYpObject =
                DEFAULT_OBJECT_MAP.get(DEFAULT_STAGE_NAME).get();
        assertThat(actualStageYpObject, equalTo(expectedStageYpObject));
    }

    private Map<String, Try<YpObject<StageMeta, TStageSpec, TStageStatus>>> createStageObjectMap(
            TDeployUnitSpec deployUnitSpec, Map<String, YTreeNode> stageLabels
    ) {
        YpObject<StageMeta, TStageSpec, TStageStatus> ypObject = TestUtils.createStageYpObject(
                deployUnitSpec, stageLabels
        );
        return Map.of(DEFAULT_STAGE_NAME, Try.success(ypObject));
    }

    private void updateRepo(Map<String, Try<YpObject<StageMeta, TStageSpec, TStageStatus>>> objectMap) {
        SelectedObjects<StageMeta, TStageSpec, TStageStatus> selectedObjects = mock(SelectedObjects.class);
        when(selectedObjects.getObjects()).thenReturn(objectMap);
        when(stageRepo.selectObjects(any(), anyMap())).thenReturn(
                CompletableFuture.completedFuture(selectedObjects)
        );
    }
}
