package ru.yandex.infra.sidecars_updater.sidecar_service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.sidecars_updater.DeployUnitSidecarInfo;
import ru.yandex.infra.sidecars_updater.LabelUpdater;
import ru.yandex.infra.sidecars_updater.StageCache;
import ru.yandex.infra.sidecars_updater.StageUpdateNotifier;
import ru.yandex.infra.sidecars_updater.TestUtils;
import ru.yandex.infra.sidecars_updater.sandbox.SandboxClient;
import ru.yandex.infra.sidecars_updater.sandbox.SandboxInfoGetter;
import ru.yandex.infra.sidecars_updater.sandbox.SandboxResourceInfo;
import ru.yandex.infra.sidecars_updater.sidecars.PodAgentSidecar;
import ru.yandex.infra.sidecars_updater.sidecars.Sidecar;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yp.client.api.TDeployUnitSpec;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.sidecars_updater.TestData.DEFAULT_LAYER_ATTRIBUTES;
import static ru.yandex.infra.sidecars_updater.TestData.DEFAULT_LAYER_INFO;
import static ru.yandex.infra.sidecars_updater.sidecar_service.SidecarsService.CACHED_SIDECAR_TYPES;
import static ru.yandex.infra.sidecars_updater.sidecar_service.SidecarsService.DU_PATCHERS_TARGET_LABEL;

public class SidecarsServiceTest {
    public static final Sidecar DEFAULT_SIDECAR = new PodAgentSidecar(Map.of(Sidecar.Type.POD_AGENT_BINARY, Map.of()));
    public static final String DEFAULT_STAGE_ID = "stage_id";
    public static final String DEFAULT_DEPLOY_UNIT_ID = "deploy_unit_id";
    public static final int OLD_PATCHERS_REVISION = 3;
    public static final int ANOTHER_OLD_PATCHERS_REVISION = OLD_PATCHERS_REVISION + 1;
    public static final int NEW_PATCHERS_REVISION = ANOTHER_OLD_PATCHERS_REVISION + 1;
    public static final int FULL_PERCENT = 100;
    public static final int DEFAULT_SPEC_TIMESTAMP = 100;
    public static final int DEFAULT_META_TIMESTAMP = 200;
    public static final Map<String, YTreeNode> DEFAULT_STAGE_LABELS = Map.of(
            SidecarsService.DU_SIDECAR_TARGET_LABEL,
            new YTreeBuilder().beginMap().endMap().build(),
            SidecarsService.DU_PATCHERS_TARGET_LABEL,
            new YTreeBuilder().beginMap().endMap().build()
    );
    public static final Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> DEFAULT_OBJECT_MAP =
            createStageObjectMap(TDeployUnitSpec.newBuilder().build(), DEFAULT_STAGE_LABELS);

    @Test
    void applyOnEmptyStagesTest() {
        applyOnPercentScenario(emptyMap(), PatchLabelsAssertion.PATCH_NOT_CALLED);
    }

    private static Stream<Arguments> applyOnPercentOneStageTestParameters() {
        return Stream.of(
                Arguments.of(
                        OptionalInt.of(NEW_PATCHERS_REVISION),
                        DEFAULT_STAGE_LABELS,
                        PatchLabelsAssertion.PATCH_NOT_CALLED
                ),
                Arguments.of(
                        OptionalInt.of(OLD_PATCHERS_REVISION),
                        DEFAULT_STAGE_LABELS,
                        PatchLabelsAssertion.PATCH_CALLED
                ),
                Arguments.of(
                        OptionalInt.empty(),
                        DEFAULT_STAGE_LABELS,
                        PatchLabelsAssertion.PATCH_CALLED
                ),
                Arguments.of(
                        OptionalInt.empty(),
                        createStageLabelsWithDUTargetPatchersVersionLabel(ANOTHER_OLD_PATCHERS_REVISION),
                        PatchLabelsAssertion.PATCH_CALLED
                ),
                Arguments.of(
                        OptionalInt.empty(),
                        createStageLabelsWithDUTargetPatchersVersionLabel(NEW_PATCHERS_REVISION),
                        PatchLabelsAssertion.PATCH_NOT_CALLED
                )
        );
    }

    @ParameterizedTest
    @MethodSource("applyOnPercentOneStageTestParameters")
    void applyOnPercentOneStageTest(
            OptionalInt deployUnitRuntimeVersion,
            Map<String, YTreeNode> stageLabels,
            PatchLabelsAssertion patchLabelsAssertion
    ) {
        TDeployUnitSpec.Builder deployUnitSpecBuilder = TDeployUnitSpec.newBuilder();
        deployUnitRuntimeVersion.ifPresent(deployUnitSpecBuilder::setPatchersRevision);

        Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> objectsMap = createStageObjectMap(
                deployUnitSpecBuilder.build(), stageLabels
        );
        applyOnPercentScenario(objectsMap, patchLabelsAssertion);
    }

    private static void applyOnPercentScenario(
            Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> objectsMap,
            PatchLabelsAssertion patchLabelsAssertion
    ) {
        SidecarsService sidecarsService = new SidecarsServiceTestBuilder()
                .setObjectsMap(objectsMap)
                .build();
        sidecarsService.applyOnPercent(Optional.empty(), OptionalInt.of(NEW_PATCHERS_REVISION), FULL_PERCENT, "initiator");
        patchLabelsAssertion.check(sidecarsService.getLabelUpdater());
    }

    private static void assertPatchLabelsWasNotCalled(LabelUpdater<TStageSpec, TStageStatus> labelUpdater) {
        verify(labelUpdater, never()).patchLabels(anyString(), anyMap());
    }

    private static void assertPatchLabelsCalled(LabelUpdater<TStageSpec, TStageStatus> labelUpdater) {
        YTreeNode yTreeNode = new YTreeBuilder().beginMap()
                .key(DEFAULT_DEPLOY_UNIT_ID)
                .value(NEW_PATCHERS_REVISION)
                .endMap()
                .build();
        Map<String, YTreeNode> expectedPatches = Map.of(DU_PATCHERS_TARGET_LABEL, yTreeNode);

        verify(labelUpdater).patchLabels(eq(DEFAULT_STAGE_ID), eq(expectedPatches));
    }

    private enum PatchLabelsAssertion {
        PATCH_CALLED(SidecarsServiceTest::assertPatchLabelsCalled),
        PATCH_NOT_CALLED(SidecarsServiceTest::assertPatchLabelsWasNotCalled);

        private final Consumer<LabelUpdater<TStageSpec, TStageStatus>> assertion;

        PatchLabelsAssertion(Consumer<LabelUpdater<TStageSpec, TStageStatus>> assertion) {
            this.assertion = assertion;
        }

        public void check(LabelUpdater<TStageSpec, TStageStatus> labelUpdater) {
            assertion.accept(labelUpdater);
        }
    }

    private static Map<String, YTreeNode> createStageLabelsWithDUTargetPatchersVersionLabel(int runtimeVersion) {
        Map<String, YTreeNode> result = new HashMap<>(DEFAULT_STAGE_LABELS);
        result.put(
                DU_PATCHERS_TARGET_LABEL,
                new YTreeBuilder().beginMap()
                        .key(DEFAULT_DEPLOY_UNIT_ID)
                        .value(runtimeVersion)
                        .endMap()
                        .build()
        );
        return result;
    }

    private static Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> createStageObjectMap(
            TDeployUnitSpec deployUnitSpec, Map<String, YTreeNode> stageLabels
    ) {
        YpObject<StageMeta, TStageSpec, TStageStatus> ypObject = TestUtils.createStageYpObject(
                deployUnitSpec, stageLabels
        );
        return Map.of(DEFAULT_STAGE_ID, ypObject);
    }

    @Test
    void collectDeployUnitDataUsesCachedStagesTest() throws ExecutionException, InterruptedException {
        SidecarsService sidecarsService = new SidecarsServiceTestBuilder().build();
        YpObject<StageMeta, TStageSpec, TStageStatus> ypObject = TestUtils.createStageYpObject(
                TDeployUnitSpec.newBuilder().build(),
                DEFAULT_STAGE_LABELS
        );
        when(sidecarsService.getStageCache().getStagesFutureCache()).thenReturn(
                CompletableFuture.completedFuture(
                        Map.of(DEFAULT_STAGE_ID, ypObject)
                )
        );
        List<DeployUnitSidecarInfo> deployUnitSidecarInfos = sidecarsService.collectDeployUnitData().get();
        DeployUnitSidecarInfo expectedInfo = new DeployUnitSidecarInfo(
                DEFAULT_OBJECT_MAP.get(DEFAULT_STAGE_ID),
                DEFAULT_DEPLOY_UNIT_ID,
                new PodAgentSidecar(Map.of(Sidecar.Type.POD_AGENT_BINARY, DEFAULT_LAYER_ATTRIBUTES))
        );
        DeployUnitSidecarInfo actualInfo = deployUnitSidecarInfos.get(0);
        assertThat(actualInfo.getDeployUnitName(), equalTo(expectedInfo.getDeployUnitName()));
        assertThat(actualInfo.getSidecarType(), equalTo(expectedInfo.getSidecarType()));
        assertThat(actualInfo.getStageName(), equalTo(expectedInfo.getStageName()));
    }

    @ParameterizedTest
    @EnumSource(Sidecar.Type.class)
    void refreshCachesTest(Sidecar.Type type) {
        SidecarsService sidecarsService = refreshSidecarInfoScenario(type);

        if (CACHED_SIDECAR_TYPES.contains(type)) {
            verify(sidecarsService.getSandboxInfoGetter()).refreshTypeCache(
                    eq(type.toString()),
                    eq(DEFAULT_LAYER_ATTRIBUTES),
                    anyBoolean()
            );
        } else {
            verifyNoInteractions(sidecarsService.getSandboxInfoGetter());
        }
    }

    @ParameterizedTest
    @EnumSource(Sidecar.Type.class)
    void collectLastReleasesTest(Sidecar.Type type) {
        SidecarsService sidecarsService = refreshSidecarInfoScenario(type);

        verify(sidecarsService.getSidecars().get(0)).setRevision(eq(DEFAULT_LAYER_INFO.getRevision()));
    }

    private static SidecarsService refreshSidecarInfoScenario(
            Sidecar.Type type
    ) {
        Sidecar sidecar = mock(Sidecar.class);
        when(sidecar.getResourceType()).thenReturn(type);
        when(sidecar.getAttributes()).thenReturn(DEFAULT_LAYER_ATTRIBUTES);

        SidecarsService sidecarsService = new SidecarsServiceTestBuilder()
                .setSidecars(List.of(sidecar))
                .addSandboxClientResponse(type, List.of(DEFAULT_LAYER_INFO))
                .build();

        sidecarsService.refreshData();

        return sidecarsService;
    }

    static class SidecarsServiceTestBuilder {
        private Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> objectsMap;
        private LabelUpdater<TStageSpec, TStageStatus> labelUpdater;
        private SandboxClient sandboxClient;
        private Map<Sidecar.Type, List<SandboxResourceInfo>> sandboxClientResponses;
        private SandboxInfoGetter sandboxInfoGetter;
        private StageUpdateNotifier stageUpdateNotifier;
        private Set<String> blackList;
        private Set<String> whiteList;
        private StageCache stageCache;
        private List<Sidecar> sidecars;

        public SidecarsServiceTestBuilder() {
            objectsMap = emptyMap();
            labelUpdater = mock(LabelUpdater.class);
            sandboxClient = mock(SandboxClient.class);
            sandboxClientResponses = new HashMap<>();
            sandboxInfoGetter = mock(SandboxInfoGetter.class);
            stageUpdateNotifier = mock(StageUpdateNotifier.class);
            blackList = emptySet();
            whiteList = emptySet();
            sidecars = List.of(DEFAULT_SIDECAR);
            stageCache = mock(StageCache.class);
        }

        public SidecarsServiceTestBuilder setObjectsMap(Map<String, YpObject<StageMeta, TStageSpec,
                TStageStatus>> objectsMap) {
            this.objectsMap = objectsMap;
            return this;
        }

        public SidecarsServiceTestBuilder setLabelUpdater(LabelUpdater<TStageSpec, TStageStatus> labelUpdater) {
            this.labelUpdater = labelUpdater;
            return this;
        }

        public SidecarsServiceTestBuilder setSandboxClient(SandboxClient sandboxClient) {
            this.sandboxClient = sandboxClient;
            return this;
        }

        public SidecarsServiceTestBuilder addSandboxClientResponse(Sidecar.Type type, List<SandboxResourceInfo> infos) {
            sandboxClientResponses.put(type, infos);
            return this;
        }

        public SidecarsServiceTestBuilder setSandboxInfoGetter(SandboxInfoGetter sandboxInfoGetter) {
            this.sandboxInfoGetter = sandboxInfoGetter;
            return this;
        }

        public SidecarsServiceTestBuilder setSidecars(List<Sidecar> sidecars) {
            this.sidecars = sidecars;
            return this;
        }

        public SidecarsServiceTestBuilder setBlackList(Set<String> blackList) {
            this.blackList = blackList;
            return this;
        }

        public SidecarsServiceTestBuilder setWhiteList(Set<String> whiteList) {
            this.whiteList = whiteList;
            return this;
        }

        public LabelUpdater<TStageSpec, TStageStatus> getLabelUpdater() {
            return labelUpdater;
        }

        public List<Sidecar> getSidecars() {
            return sidecars;
        }

        public SandboxClient getSandboxClient() {
            return sandboxClient;
        }

        public SandboxInfoGetter getSandboxInfoGetter() {
            return sandboxInfoGetter;
        }

        public Set<String> getBlackList() {
            return blackList;
        }

        public Set<String> getWhiteList() {
            return whiteList;
        }

        public SidecarsService build() {
            when(stageCache.getStagesFutureCache()).thenReturn(CompletableFuture.completedFuture(objectsMap));
            sandboxClientResponses.forEach((type, infos) -> {
                CompletableFuture<List<SandboxResourceInfo>> getResourcesCalled = sandboxClient.getResources(
                        eq(type.toString()),
                        anyMap(),
                        anyBoolean(),
                        anyLong()
                );
                CompletableFuture<List<SandboxResourceInfo>> getResourcesResult = CompletableFuture.completedFuture(
                        infos
                );
                when(getResourcesCalled).thenReturn(getResourcesResult);
            });

            return new SidecarsService(
                    labelUpdater, sidecars, sandboxClient, sandboxInfoGetter,
                    stageUpdateNotifier, blackList, whiteList, stageCache
            );
        }
    }
}
