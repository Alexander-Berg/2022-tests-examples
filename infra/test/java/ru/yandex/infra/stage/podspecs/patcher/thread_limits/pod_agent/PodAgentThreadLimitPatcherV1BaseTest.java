package ru.yandex.infra.stage.podspecs.patcher.thread_limits.pod_agent;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.podspecs.patcher.PatcherTestBase;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TPodTemplateSpec;

import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.podspecs.patcher.pod_agent.PodAgentPatcherUtils.POD_AGENT_THREAD_LIMIT_V1_TO_LAST;

abstract class PodAgentThreadLimitPatcherV1BaseTest extends PatcherTestBase<PodAgentThreadLimitPatcherV1Context> {

    private static final PodAgentThreadLimitPatcherV1Context DEFAULT_PATCHER_CONTEXT =
            new PodAgentThreadLimitPatcherV1Context();

    private static Stream<Arguments> patchPodAgentThreadLimitParameters() {
        final long definedThreadLimitValue = 123;
        return Stream.of(
                // Set Pod Agent thread limits, if they are not defined
                Arguments.of(
                        createPodSpecBuilder(),
                        POD_AGENT_THREAD_LIMIT_V1_TO_LAST
                ),
                // Do not override existing value
                Arguments.of(
                        setPodAgentThreadLimit(createPodSpecBuilder(), definedThreadLimitValue),
                        definedThreadLimitValue
                )
        );
    }

    private static long getPodAgentThreadLimit(DataModel.TPodSpecOrBuilder podSpec) {
        return podSpec.getPodAgentPayloadOrBuilder().getMetaOrBuilder().getComputeResourcesOrBuilder().getThreadLimit();
    }

    private static TPodTemplateSpec.Builder setPodAgentThreadLimit(TPodTemplateSpec.Builder podSpec, long threadLimit) {
        podSpec
                .getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getMetaBuilder()
                .getComputeResourcesBuilder()
                .setThreadLimit(threadLimit);
        return podSpec;
    }

    @ParameterizedTest
    @MethodSource("patchPodAgentThreadLimitParameters")
    void patchPodAgentThreadLimitTest(TPodTemplateSpec.Builder podSpec, long expectedThreadLimit) {
        PatchResult resultSpec = patch(DEFAULT_PATCHER_CONTEXT, podSpec, DEFAULT_UNIT_CONTEXT);
        Assertions.assertEquals(expectedThreadLimit, getPodAgentThreadLimit(resultSpec.getPodSpec()));
    }
}
