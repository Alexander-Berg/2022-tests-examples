package ru.yandex.infra.stage.podspecs.patcher.logbroker;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.dto.AllComputeResourcesTest;
import ru.yandex.infra.stage.util.NamedArgument;

import static java.util.Collections.emptyMap;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class LogbrokerBoxResourcesConfigTest {

    private static final AllComputeResources DEFAULT_PATCHER_RESOURCES = AllComputeResourcesTest.DEFAULT_RESOURCES;

    private static final LogbrokerBoxResourcesConfig DEFAULT_BOX_RESOURCES_CONFIG = new LogbrokerBoxResourcesConfig(
            emptyMap()
    );

    private static final String DEFAULT_FULL_DEPLOY_UNIT_ID = TestData.DEFAULT_UNIT_CONTEXT.getFullDeployUnitId();

    private static Stream<Arguments> getDefaultBoxResourcesTestParameters() {
        var patcherDefaultResourcesArgument = NamedArgument.of(
                "patcher default resources", DEFAULT_PATCHER_RESOURCES
        );

        var customResources = DEFAULT_PATCHER_RESOURCES.toBuilder()
                .withVcpuLimit(DEFAULT_PATCHER_RESOURCES.getVcpuLimit() + 1)
                .withAnonymousMemoryLimit(DEFAULT_PATCHER_RESOURCES.getAnonymousMemoryLimit() * 2)
                .build();

        var customResourcesArgument = NamedArgument.of(
                "custom deploy unit resources", customResources
        );

        var emptyWhiteList = NamedArgument.of(
                "empty white list", emptyMap()
        );

        var customizedWhiteList = NamedArgument.of(
                "customized white list",
                Map.of(DEFAULT_FULL_DEPLOY_UNIT_ID, customResources)
        );

        return Stream.of(
                Arguments.of(emptyWhiteList, patcherDefaultResourcesArgument),
                Arguments.of(customizedWhiteList, customResourcesArgument)
        );
    }

    @ParameterizedTest
    @MethodSource("getDefaultBoxResourcesTestParameters")
    void getDefaultBoxResourcesTest(NamedArgument<Map<String, AllComputeResources>> whiteListResources,
                                    NamedArgument<AllComputeResources> expectedResources) {
        var boxResourcesConfig = DEFAULT_BOX_RESOURCES_CONFIG.toBuilder()
                .withWhiteListResources(whiteListResources.getArgument())
                .build();

        var actualResources = boxResourcesConfig.getDefaultBoxResources(
                DEFAULT_FULL_DEPLOY_UNIT_ID,
                DEFAULT_PATCHER_RESOURCES
        );

        assertThatEquals(actualResources, expectedResources.getArgument());
    }

    private static Stream<Arguments> getMergedVcpuGuaranteeTestParameters() {
        long defaultVcpuGuarantee = DEFAULT_PATCHER_RESOURCES.getVcpuGuarantee();

        var lessArgument = NamedArgument.of("less than default", defaultVcpuGuarantee - 1);
        var greaterArgument = NamedArgument.of("greater than default", defaultVcpuGuarantee + 1);

        return Stream.of(
            Arguments.of(lessArgument, lessArgument),
            Arguments.of(greaterArgument, greaterArgument)
        );
    }

    @ParameterizedTest
    @MethodSource("getMergedVcpuGuaranteeTestParameters")
    void getMergedVcpuGuaranteeTest(NamedArgument<Long> requestedValue,
                                    NamedArgument<Long> expectedValue) {
        getMergedResourcesScenario(
                requestedValue.getArgument(),
                expectedValue.getArgument(),
                AllComputeResources.Builder::withVcpuGuarantee
        );
    }

    private static Stream<Arguments> getMergedVcpuLimitTestParameters() {
        long defaultVcpuLimit = DEFAULT_PATCHER_RESOURCES.getVcpuLimit();

        var lessArgument = NamedArgument.of("less than default", defaultVcpuLimit - 1);
        var defaultArgument = NamedArgument.of("default", defaultVcpuLimit);
        var greaterArgument = NamedArgument.of("greater than default", defaultVcpuLimit + 1);

        return Stream.of(
                Arguments.of(lessArgument, defaultArgument),
                Arguments.of(greaterArgument, greaterArgument)
        );
    }

    @ParameterizedTest
    @MethodSource("getMergedVcpuLimitTestParameters")
    void getMergedVcpuLimitTest(NamedArgument<Long> requestedValue,
                                NamedArgument<Long> expectedValue) {
        getMergedResourcesScenario(
                requestedValue.getArgument(),
                expectedValue.getArgument(),
                AllComputeResources.Builder::withVcpuLimit
        );
    }

    private static void getMergedResourcesScenario(
            long requestedValue,
            long expectedValue,
            BiFunction<AllComputeResources.Builder, Long, AllComputeResources.Builder> transformer) {
        getMergedResourcesScenario(
                transformer.apply(DEFAULT_PATCHER_RESOURCES.toBuilder(), requestedValue).build(),
                transformer.apply(DEFAULT_PATCHER_RESOURCES.toBuilder(), expectedValue).build()
        );
    }

    private static void getMergedResourcesScenario(AllComputeResources requestedResources,
                                                   AllComputeResources expectedResources) {
        var actualResources = LogbrokerBoxResourcesConfig.getMergedResources(
                DEFAULT_PATCHER_RESOURCES,
                requestedResources
        );

        assertThatEquals(actualResources, expectedResources);
    }

    private static Stream<Arguments> getActualBoxTestParameters() {
        long defaultVcpuLimit = DEFAULT_PATCHER_RESOURCES.getVcpuLimit();

        var lessArgument = NamedArgument.of("less than default", defaultVcpuLimit - 1);
        var defaultArgument = NamedArgument.of("default", defaultVcpuLimit);
        var greaterArgument = NamedArgument.of("greater than default", defaultVcpuLimit + 1);

        return Stream.of(
                Arguments.of(lessArgument, defaultArgument),
                Arguments.of(greaterArgument, greaterArgument)
        );
    }

    private static Stream<Arguments> getActualBoxResourcesTestParameters() {
        var requestedResources = DEFAULT_PATCHER_RESOURCES.toBuilder()
                .withVcpuGuarantee(DEFAULT_PATCHER_RESOURCES.getVcpuGuarantee() + 10)
                .withVcpuLimit(DEFAULT_PATCHER_RESOURCES.getVcpuLimit() - 3)
                .build();

        var mergedResources = LogbrokerBoxResourcesConfig.getMergedResources(
                DEFAULT_PATCHER_RESOURCES, requestedResources
        );

        var requestPresentedArgument = NamedArgument.of(
                "request presented", Optional.of(requestedResources)
        );

        return Stream.of(
                Arguments.of(NamedArgument.of(Optional.empty()), NamedArgument.of("default", DEFAULT_PATCHER_RESOURCES)),
                Arguments.of(requestPresentedArgument, NamedArgument.of("merged", mergedResources))
        );
    }

    @ParameterizedTest
    @MethodSource("getActualBoxResourcesTestParameters")
    void getActualBoxResourcesTest(NamedArgument<Optional<AllComputeResources>> requestedResources,
                                   NamedArgument<AllComputeResources> expectedResources) {
        var actualResources = DEFAULT_BOX_RESOURCES_CONFIG.getActualBoxResources(
                DEFAULT_FULL_DEPLOY_UNIT_ID,
                DEFAULT_PATCHER_RESOURCES,
                requestedResources.getArgument()
        );

        assertThatEquals(actualResources, expectedResources.getArgument());
    }
}
