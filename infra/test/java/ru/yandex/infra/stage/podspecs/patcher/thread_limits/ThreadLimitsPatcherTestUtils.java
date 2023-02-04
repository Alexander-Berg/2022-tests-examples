package ru.yandex.infra.stage.podspecs.patcher.thread_limits;

import java.util.List;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.dto.LogbrokerConfig;
import ru.yandex.infra.stage.dto.TvmConfig;
import ru.yandex.lang.NonNullApi;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TComputeResources;
import ru.yandex.yp.client.pods.TPodAgentSpec;

import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.TestData.TVM_CONFIG;

@NonNullApi
public class ThreadLimitsPatcherTestUtils {

    public static TPodTemplateSpec.Builder createPodTemplateBuilder(List<TBox> boxes) {
        TPodAgentSpec.Builder podAgentSpecBuilder = createPodAgentSpecBuilder(boxes);

        return TPodTemplateSpec.newBuilder()
                .setSpec(DataModel.TPodSpec.newBuilder().setPodAgentPayload(
                        DataModel.TPodSpec.TPodAgentPayload.newBuilder().setSpec(podAgentSpecBuilder)
                ));
    }

    public static TPodAgentSpec.Builder createPodAgentSpecBuilder(List<TBox> boxes) {
        return TPodAgentSpec.newBuilder().addAllBoxes(boxes);
    }

    public static TBox createBox(String boxId, long threadLimit) {
        return createBox(boxId).toBuilder()
                .setComputeResources(
                        TComputeResources.newBuilder()
                                .setThreadLimit(threadLimit)
                                .build()
                ).build();
    }

    public static TBox createBox(String boxId) {
        return TBox.newBuilder().setId(boxId).build();
    }

    public static long getBoxThreadLimit(TPodAgentSpec podAgentSpec, String boxId) {
        return podAgentSpec.getBoxesList().stream()
                .filter(b -> b.getId().equals(boxId))
                .findFirst().orElseThrow().getComputeResources().getThreadLimit();
    }

    public static DeployUnitContext contextWithLogbrokerAndTvm(
            LogbrokerConfig.SidecarBringupMode logbrokerConfigBringupMode,
            TvmConfig.Mode tvmConfigMode) {
        LogbrokerConfig logbrokerConfig = TestData.LOGBROKER_CONFIG.toBuilder()
                .withSidecarBringupMode(logbrokerConfigBringupMode)
                .build();
        TvmConfig tvmConfig = TVM_CONFIG.withMode(tvmConfigMode);

        return DEFAULT_UNIT_CONTEXT
                .withTvmConfig(tvmConfig)
                .withLogbrokerConfig(logbrokerConfig);
    }
}
