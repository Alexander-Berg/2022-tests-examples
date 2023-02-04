package ru.yandex.infra.stage.podspecs.patcher.thread_limits;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.infra.stage.podspecs.patcher.PatcherTestBase;
import ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils;
import ru.yandex.infra.stage.podspecs.patcher.thread_limits.box.BoxThreadLimitsDistributorImpl;
import ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherUtils;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TPodAgentSpec;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.ThreadLimitsPatcherTestUtils.createBox;
import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.ThreadLimitsPatcherTestUtils.createPodTemplateBuilder;
import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.ThreadLimitsPatcherTestUtils.getBoxThreadLimit;
import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.box.BoxThreadLimitsDistributorImplTest.createBoxThreadLimitsDistributorMock;

public abstract class ThreadLimitsPatcherV1BaseTest extends PatcherTestBase<ThreadLimitsPatcherV1Context> {
    public static final long CUSTOM_THREAD_LIMIT = 9;
    public static final String USER_BOX_WITHOUT_LIMIT_ID_1 = "user_box_without_limit_id_1";
    public static final String USER_BOX_WITHOUT_LIMIT_ID_2 = "user_box_without_limit_id_2";
    public static final String USER_BOX_WITH_CUSTOM_THREAD_LIMIT_ID = "user_box_with_custom_thread_limit_id";
    public static final TBox USER_BOX_WITHOUT_LIMIT_1 = createBox(USER_BOX_WITHOUT_LIMIT_ID_1);
    public static final TBox USER_BOX_WITHOUT_LIMIT_2 = createBox(USER_BOX_WITHOUT_LIMIT_ID_2);
    public static final TBox LOGBROKER_BOX = createBox(
            LogbrokerPatcherUtils.LOGBROKER_BOX_ID, LogbrokerPatcherUtils.LOGBROKER_THREAD_LIMIT
    );
    public static final TBox TVM_BOX = createBox(TvmPatcherUtils.TVM_BOX_ID, TvmPatcherUtils.TVM_THREAD_LIMIT);
    public static final TBox USER_BOX_WITH_CUSTOM_THREAD_LIMIT = createBox(
            USER_BOX_WITH_CUSTOM_THREAD_LIMIT_ID, CUSTOM_THREAD_LIMIT
    );
    public static final TPodTemplateSpec.Builder DEFAULT_POD_SPEC = createPodTemplateBuilder(List.of(
            USER_BOX_WITHOUT_LIMIT_1,
            USER_BOX_WITHOUT_LIMIT_2,
            USER_BOX_WITH_CUSTOM_THREAD_LIMIT,
            TVM_BOX));

    protected final ThreadLimitsPatcherV1Context DEFAULT_PATCHER_CONTEXT =
            new ThreadLimitsPatcherV1Context(BoxThreadLimitsDistributorImpl.INSTANCE);

    @Test
    void boxThreadLimitsDistributorArgumentsTest() {
        var boxThreadLimitDistributor = createBoxThreadLimitsDistributorMock(100);

        patch(
                DEFAULT_PATCHER_CONTEXT.toBuilder().with(boxThreadLimitDistributor).build(),
                DEFAULT_POD_SPEC,
                DEFAULT_UNIT_CONTEXT
        );

        verify(boxThreadLimitDistributor).distributeThreadLimitsAvailableForUserBoxes(
                eq(DEFAULT_POD_SPEC.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder()),
                anyLong()
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {123, 321})
    void boxThreadLimitsDistributorUsedTest(int threadLimitToSet) {
        var boxThreadLimitDistributor = createBoxThreadLimitsDistributorMock(threadLimitToSet);

        List<TBox> boxesWithoutThreadLimit = DEFAULT_POD_SPEC.getSpec().getPodAgentPayload().getSpec()
                .getBoxesList().stream()
                .filter(box -> box.getComputeResources().getThreadLimit() == 0)
                .collect(Collectors.toList());

        TPodAgentSpec resultPodAgentSpec = patch(
                DEFAULT_PATCHER_CONTEXT.toBuilder().with(boxThreadLimitDistributor).build(),
                DEFAULT_POD_SPEC,
                DEFAULT_UNIT_CONTEXT
        ).getPodAgentPayload().getSpec();

        boxesWithoutThreadLimit.forEach(boxWithoutThreadLimit -> Assertions.assertEquals(
                threadLimitToSet,
                getBoxThreadLimit(resultPodAgentSpec, boxWithoutThreadLimit.getId()))
        );
    }

}
