package ru.yandex.infra.stage.podspecs.patcher.thread_limits;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TPodAgentSpec;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static ru.yandex.infra.stage.dto.LogbrokerConfig.SidecarBringupMode.MANDATORY;
import static ru.yandex.infra.stage.dto.LogbrokerConfig.SidecarBringupMode.UNRECOGNIZED;
import static ru.yandex.infra.stage.dto.TvmConfig.Mode.DISABLED;
import static ru.yandex.infra.stage.dto.TvmConfig.Mode.ENABLED;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.LOGBROKER_THREAD_LIMIT;
import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.ThreadLimitsPatcherTestUtils.contextWithLogbrokerAndTvm;
import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.ThreadLimitsPatcherTestUtils.createBox;
import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.ThreadLimitsPatcherTestUtils.createPodAgentSpecBuilder;
import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.ThreadLimitsPatcherUtils.MAX_AVAILABLE_THREADS_PER_POD;
import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.ThreadLimitsPatcherV1.POD_AGENT_RESERVED_THREAD_COUNT;
import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.box.BoxThreadLimitsDistributorImplTest.createBoxThreadLimitsDistributorMock;
import static ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherUtils.TVM_THREAD_LIMIT;

public class ThreadLimitsPatcherV1Test extends ThreadLimitsPatcherV1BaseTest {
    @Override
    protected Function<ThreadLimitsPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return ThreadLimitsPatcherV1::new;
    }

    private static Stream<Arguments> threadsDistributorReceiveTestParameters() {
        final long customTreadLimit = 124;
        return Stream.of(
                Arguments.of(
                        contextWithLogbrokerAndTvm(UNRECOGNIZED, DISABLED),
                        createPodAgentSpecBuilder(List.of(
                                createBox("id1")
                        )),
                        MAX_AVAILABLE_THREADS_PER_POD - POD_AGENT_RESERVED_THREAD_COUNT
                ),
                Arguments.of(
                        contextWithLogbrokerAndTvm(UNRECOGNIZED, ENABLED),
                        createPodAgentSpecBuilder(List.of(
                                createBox("id1"),
                                TVM_BOX
                        )),
                        MAX_AVAILABLE_THREADS_PER_POD - POD_AGENT_RESERVED_THREAD_COUNT - TVM_THREAD_LIMIT
                ),
                Arguments.of(
                        contextWithLogbrokerAndTvm(MANDATORY, DISABLED),
                        createPodAgentSpecBuilder(List.of(
                                createBox("id1"),
                                LOGBROKER_BOX
                        )),
                        MAX_AVAILABLE_THREADS_PER_POD - POD_AGENT_RESERVED_THREAD_COUNT - LOGBROKER_THREAD_LIMIT
                ),
                Arguments.of(
                        contextWithLogbrokerAndTvm(MANDATORY, ENABLED),
                        createPodAgentSpecBuilder(List.of(
                                createBox("id1"),
                                LOGBROKER_BOX,
                                TVM_BOX
                        )),
                        MAX_AVAILABLE_THREADS_PER_POD - POD_AGENT_RESERVED_THREAD_COUNT - LOGBROKER_THREAD_LIMIT - TVM_THREAD_LIMIT
                ),
                Arguments.of(
                        contextWithLogbrokerAndTvm(UNRECOGNIZED, ENABLED),
                        createPodAgentSpecBuilder(List.of(
                                createBox("id1", customTreadLimit),
                                createBox("id2"),
                                TVM_BOX
                        )),
                        MAX_AVAILABLE_THREADS_PER_POD - POD_AGENT_RESERVED_THREAD_COUNT - TVM_THREAD_LIMIT
                ),
                Arguments.of(
                        contextWithLogbrokerAndTvm(UNRECOGNIZED, ENABLED),
                        createPodAgentSpecBuilder(List.of(
                                createBox("id1", customTreadLimit),
                                createBox("id2"),
                                createBox("id3"),
                                TVM_BOX
                        )),
                        MAX_AVAILABLE_THREADS_PER_POD - POD_AGENT_RESERVED_THREAD_COUNT - TVM_THREAD_LIMIT
                )
        );
    }

    @ParameterizedTest
    @MethodSource("threadsDistributorReceiveTestParameters")
    void threadsDistributorReceiveTest(
            DeployUnitContext deployUnitContext,
            TPodAgentSpec.Builder podAgentSpec,
            long threadsDistributorReceive) {

        DEFAULT_POD_SPEC.getSpecBuilder().getPodAgentPayloadBuilder().setSpec(podAgentSpec);

        var boxThreadLimitDistributor = createBoxThreadLimitsDistributorMock(100);

        patch(
                DEFAULT_PATCHER_CONTEXT.toBuilder().with(boxThreadLimitDistributor).build(),
                DEFAULT_POD_SPEC,
                deployUnitContext
        );

        verify(boxThreadLimitDistributor).distributeThreadLimitsAvailableForUserBoxes(
                any(TPodAgentSpec.Builder.class),
                eq(threadsDistributorReceive)
        );
    }
}
