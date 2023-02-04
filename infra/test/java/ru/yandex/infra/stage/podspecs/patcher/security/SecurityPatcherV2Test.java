package ru.yandex.infra.stage.podspecs.patcher.security;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.dto.SandboxResourceInfo;
import ru.yandex.infra.stage.dto.SecuritySettings;
import ru.yandex.infra.stage.podspecs.SandboxResourceMeta;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yt.ytree.TAttribute;

import static ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1Base.SOX_SERVICE_LABEL;

public class SecurityPatcherV2Test extends SecurityPatcherV1BaseTest {
    @Override
    protected Function<SecurityPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return SecurityPatcherV2::new;
    }

    private static Stream<Arguments> enableSoxSecurityFeaturesTestParameters() {
        return Stream.of(
                //// Arguments with disable default child-only == false and disable default secret env == false
                // Arguments with sox == false
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.empty(),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        true
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        true
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.empty(),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_NOT_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_NOT_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        true
                ),
                // Arguments with isSox == true
                Arguments.of(
                        true,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.empty(),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        true
                ),
                Arguments.of(
                        true,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        true
                ),
                Arguments.of(
                        true,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.empty(),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        true,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_NOT_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        true,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_NOT_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        true,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        true
                ),
                // arguments with sox label and isSox == false
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.empty(),
                        SOX_SERVICE_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        true
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED),
                        SOX_SERVICE_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        true
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.empty(),
                        SOX_SERVICE_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_NOT_AFFECTED),
                        SOX_SERVICE_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_NOT_AFFECTED),
                        SOX_SERVICE_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED),
                        SOX_SERVICE_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_AND_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        true
                ),
                //// Arguments with disable default child-only == true and disable default secret env == true
                // Arguments with sox == false
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.empty(),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.empty(),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_NOT_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_NOT_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                // Arguments with isSox == true
                Arguments.of(
                        true,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.empty(),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        true
                ),
                Arguments.of(
                        true,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        true
                ),
                Arguments.of(
                        true,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.empty(),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        true,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_NOT_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        true,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_NOT_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        true,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        true
                ),
                // arguments with sox label and isSox == false
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.empty(),
                        SOX_SERVICE_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        true
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED),
                        SOX_SERVICE_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        true
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.empty(),
                        SOX_SERVICE_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_NOT_AFFECTED),
                        SOX_SERVICE_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_NOT_AFFECTED),
                        SOX_SERVICE_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        EMPTY_LABELS,
                        false
                ),
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED),
                        SOX_SERVICE_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_AND_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        true
                ),
                //// Arguments with disable default child-only == false and disable default secret env == true
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_TRUE_SECRET_ENV_DISABLE_FALSE_ISOLATION,
                        EMPTY_LABELS,
                        true
                ),
                //// Arguments with disable default child-only == true and disable default secret env == false
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED),
                        EMPTY_SOX_LABEL,
                        SECURITY_SETTINGS_DISABLE_FALSE_SECRET_ENV_DISABLE_TRUE_ISOLATION,
                        LABELS_WITH_ENV_SECRET,
                        false
                ),
                //// Arguments with empty security settings
                Arguments.of(
                        false,
                        POD_AGENT_BINARY_RESOURCE_META_NOT_AFFECTED,
                        Optional.of(POD_AGENT_BINARY_RESOURCE_INFO_AFFECTED),
                        EMPTY_SOX_LABEL,
                        Optional.empty(),
                        LABELS_WITH_ENV_SECRET,
                        true
                )
        );
    }

    @ParameterizedTest
    @MethodSource("enableSoxSecurityFeaturesTestParameters")
    void enableSoxSecurityFeaturesTest(boolean isSox,
                                       SandboxResourceMeta defaultPodAgentBinaryMeta,
                                       Optional<SandboxResourceInfo> userSpecifiedPodAgentBinaryInfo,
                                       TAttribute soxLabel,
                                       Optional<SecuritySettings> securitySettings,
                                       YTreeNode expectedLabels,
                                       boolean childOnlyIsolationEnabledForUserBoxes
    ) {
        enableSoxSecurityFeaturesTestScenario(isSox,
                defaultPodAgentBinaryMeta,
                userSpecifiedPodAgentBinaryInfo,
                soxLabel,
                securitySettings,
                expectedLabels,
                childOnlyIsolationEnabledForUserBoxes);
    }
}
