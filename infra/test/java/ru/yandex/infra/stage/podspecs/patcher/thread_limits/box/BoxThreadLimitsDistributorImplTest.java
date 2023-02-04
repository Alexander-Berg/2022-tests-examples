package ru.yandex.infra.stage.podspecs.patcher.thread_limits.box;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils;
import ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherUtils;
import ru.yandex.yp.client.pods.TPodAgentSpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.ThreadLimitsPatcherTestUtils.createBox;
import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.ThreadLimitsPatcherTestUtils.createPodAgentSpecBuilder;
import static ru.yandex.infra.stage.podspecs.patcher.thread_limits.box.BoxThreadLimitsDistributorImpl.INSTANCE;

public class BoxThreadLimitsDistributorImplTest {

    public static BoxThreadLimitsDistributor createBoxThreadLimitsDistributorMock(int threadLimitToSet) {

        BoxThreadLimitsDistributor distributor = mock(BoxThreadLimitsDistributor.class);

        Mockito.doAnswer(invocation -> {
            TPodAgentSpec.Builder podAgentSpec = invocation.getArgument(0);
            podAgentSpec.getBoxesBuilderList().forEach(box -> {
                if (box.getComputeResourcesBuilder().getThreadLimit() == 0) {
                    box.getComputeResourcesBuilder().setThreadLimit(threadLimitToSet);
                }
            });

            return null;
        }).when(distributor).distributeThreadLimitsAvailableForUserBoxes(any(TPodAgentSpec.Builder.class), anyLong());

        return distributor;
    }

    private static Stream<Arguments> distributeThreadLimitsTestParameters() {
        final long threadsToDistribute = 12340;
        final long customThreads = 124;
        return Stream.of(
                Arguments.of(
                        createPodAgentSpecBuilder(List.of(
                                createBox("id1"),
                                createBox("id2")
                        )),
                        threadsToDistribute,
                        createPodAgentSpecBuilder(List.of(
                                createBox("id1", threadsToDistribute/2),
                                createBox("id2", threadsToDistribute/2)
                        ))
                ),
                Arguments.of(
                        createPodAgentSpecBuilder(List.of(
                                createBox("id1", customThreads),
                                createBox("id2"),
                                createBox("id3")
                        )),
                        threadsToDistribute,
                        createPodAgentSpecBuilder(List.of(
                                createBox("id1", customThreads),
                                createBox("id2", (threadsToDistribute - customThreads)/2),
                                createBox("id3", (threadsToDistribute - customThreads)/2)
                        ))
                ),
                Arguments.of(
                        createPodAgentSpecBuilder(List.of(
                                createBox("id1", 500),
                                createBox("id2", 300)
                        )),
                        threadsToDistribute,
                        createPodAgentSpecBuilder(List.of(
                                createBox("id1", 500),
                                createBox("id2", 300)
                        ))
                ),
                Arguments.of(
                        createPodAgentSpecBuilder(List.of(
                                createBox(LogbrokerPatcherUtils.LOGBROKER_BOX_ID),
                                createBox("id2")
                        )),
                        threadsToDistribute,
                        createPodAgentSpecBuilder(List.of(
                                createBox(LogbrokerPatcherUtils.LOGBROKER_BOX_ID),
                                createBox("id2", threadsToDistribute)
                        ))
                ),
                Arguments.of(
                        createPodAgentSpecBuilder(List.of(
                                createBox(TvmPatcherUtils.TVM_BOX_ID),
                                createBox("id2")
                        )),
                        threadsToDistribute,
                        createPodAgentSpecBuilder(List.of(
                                createBox(TvmPatcherUtils.TVM_BOX_ID),
                                createBox("id2", threadsToDistribute)
                        ))
                ),
                Arguments.of(
                        createPodAgentSpecBuilder(List.of(
                                createBox(LogbrokerPatcherUtils.LOGBROKER_BOX_ID),
                                createBox(TvmPatcherUtils.TVM_BOX_ID)
                        )),
                        threadsToDistribute,
                        createPodAgentSpecBuilder(List.of(
                                createBox(LogbrokerPatcherUtils.LOGBROKER_BOX_ID),
                                createBox(TvmPatcherUtils.TVM_BOX_ID)
                        ))
                )
        );
    }

    @ParameterizedTest
    @MethodSource("distributeThreadLimitsTestParameters")
    void distributeThreadLimitsTest(TPodAgentSpec.Builder podAgentSpec, long threadsToDistribute,
                                    TPodAgentSpec.Builder podAgentSpecExpected) {
        INSTANCE.distributeThreadLimitsAvailableForUserBoxes(podAgentSpec, threadsToDistribute);
        int boxesCount = podAgentSpecExpected.getBoxesCount();
        for (int i = 0; i < boxesCount; i++) {
            assertEquals(
                    podAgentSpecExpected.getBoxes(i).getComputeResources().getThreadLimit(),
                    podAgentSpec.getBoxes(i).getComputeResources().getThreadLimit()
            );
        }
    }
}
