package ru.yandex.infra.stage.podspecs.patcher.logbroker;

import java.util.function.Function;

import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;

import static ru.yandex.infra.stage.podspecs.PodSpecUtils.MEGABYTE;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.LOGBROKER_THREAD_LIMIT;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1Base.LOGBROKER_TOOLS_HDD_CAPACITY;

public class LogbrokerPatcherV1Test extends LogbrokerPatcherV1BaseTest {

    private final static AllComputeResources DEFAULT_LOGBROKER_BOX_COMPUTING_RESOURCES_FOR_V1 = new AllComputeResources(
            400,
            512 * MEGABYTE,
            LOGBROKER_TOOLS_HDD_CAPACITY,
            LOGBROKER_THREAD_LIMIT
    );

    @Override
    protected Function<LogbrokerPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return LogbrokerPatcherV1::new;
    }

    @Override
    protected AllComputeResources getDefaultLogbrokerBoxComputingResources() {
        return DEFAULT_LOGBROKER_BOX_COMPUTING_RESOURCES_FOR_V1;
    }

    @Override
    protected String getExpectedPatcherThrottlingLimitsKey() {
        return EXPECTED_PATCHER_THROTTLING_LIMITS_KEY_FROM_V1_TO_V2;
    }

    @Override
    protected String getLogbrokerBoxId() {
        return LogbrokerPatcherUtils.LOGBROKER_BOX_ID_UNDERSCORED;
    }

    @Override
    protected boolean errorBoosterEnvironmentsExport() {
        return false;
    }

    @Override
    protected boolean errorBoosterHttpEnvironmentsExport() {
        return false;
    }

    @Override
    protected boolean autoEnableSystemLogsWhenUserLogsEnabled(){
        return false;
    }
}
