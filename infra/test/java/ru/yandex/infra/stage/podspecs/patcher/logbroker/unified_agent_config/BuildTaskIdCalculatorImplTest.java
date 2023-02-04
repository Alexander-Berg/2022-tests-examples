package ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.podspecs.SandboxResourceMeta;
import ru.yandex.infra.stage.util.NamedArgument;
import ru.yandex.yp.client.pods.TLayer;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils.LOGBROKER_TOOLS_LAYER_ID;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.BuildTaskIdCalculatorImpl.FIXED_TASK_ID;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.BuildTaskIdCalculatorImpl.INSTANCE;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class BuildTaskIdCalculatorImplTest {

    static BuildTaskIdCalculator createBuildTaskIdCalculatorMock(long buildTaskId) {
        var buildTaskIdCalculator = mock(BuildTaskIdCalculator.class);

        when(buildTaskIdCalculator.calculate(
                anyCollection(),
                any(Optional.class)
        )).thenReturn(buildTaskId);

        return buildTaskIdCalculator;
    }

    static final TLayer DEFAULT_LOGBROKER_LAYER = TLayer.newBuilder()
            .setId(LOGBROKER_TOOLS_LAYER_ID)
            .build();

    private static final Collection<TLayer> EMPTY_LAYERS = emptyList();

    private static final SandboxResourceMeta DEFAULT_RESOURCE_META = TestData.RESOURCE_META;

    private static Stream<Arguments> buildTaskIdTestParameters() {
        var withoutLogbrokerLayerArgument = NamedArgument.of(
                "without logbroker layer", EMPTY_LAYERS
        );

        var withMetaArgument = NamedArgument.of(
                "with meta",
                Optional.of(DEFAULT_RESOURCE_META)
        );

        var fixedBuildTaskIdArgument = NamedArgument.of(
                "fixed build task id",
                FIXED_TASK_ID
        );

        return Stream.of(
                Arguments.of(
                        NamedArgument.of("with logbroker layer", List.of(DEFAULT_LOGBROKER_LAYER)),
                        withMetaArgument,
                        fixedBuildTaskIdArgument
                ),
                Arguments.of(
                        withoutLogbrokerLayerArgument,
                        withMetaArgument,
                        NamedArgument.of("build task id from meta", DEFAULT_RESOURCE_META.getTaskId())
                ),
                Arguments.of(
                        withoutLogbrokerLayerArgument,
                        NamedArgument.of("without meta", Optional.empty()),
                        fixedBuildTaskIdArgument
                )
        );
    }

    @ParameterizedTest
    @MethodSource("buildTaskIdTestParameters")
    void buildTaskIdTest(NamedArgument<Collection<TLayer>> podAgentPayloadLayers,
                         NamedArgument<Optional<SandboxResourceMeta>> meta,
                         NamedArgument<Long> expectedBuildTaskId) {

        var actualBuildTaskId = INSTANCE.calculate(
                podAgentPayloadLayers.getArgument(),
                meta.getArgument()
        );

        assertThatEquals(actualBuildTaskId, expectedBuildTaskId.getArgument());
    }
}
