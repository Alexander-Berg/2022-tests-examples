package ru.yandex.infra.stage.podspecs.patcher.thread_limits;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils;
import ru.yandex.infra.stage.podspecs.patcher.pod_agent.PodAgentPatcherUtils;
import ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherUtils;

import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.ThreadLimitsPatcherUtils.MAX_AVAILABLE_THREADS_PER_POD;
import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.ThreadLimitsPatcherUtils.POD_UTILS_THREAD_LIMIT;
import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.ThreadLimitsPatcherUtils.maxAvailableThreadsToUserBoxesForValidation;

public final class ThreadLimitsPatcherUtilsTest {

    @Test
    public void testMaxAvailableThreadsToUserBoxesForValidation() {
        final long threadsForUserBoxes = maxAvailableThreadsToUserBoxesForValidation();
        long expectedThreadsForUserBoxes = MAX_AVAILABLE_THREADS_PER_POD
                - PodAgentPatcherUtils.POD_AGENT_THREAD_LIMIT_V1_TO_LAST
                - POD_UTILS_THREAD_LIMIT
                - TvmPatcherUtils.TVM_THREAD_LIMIT
                - LogbrokerPatcherUtils.LOGBROKER_THREAD_LIMIT;
        Assertions.assertEquals(threadsForUserBoxes, expectedThreadsForUserBoxes);
    }
}
