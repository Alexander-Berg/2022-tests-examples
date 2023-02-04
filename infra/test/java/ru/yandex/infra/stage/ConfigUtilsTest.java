package ru.yandex.infra.stage;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatThrowsWithMessage;

class ConfigUtilsTest {

    private static Optional<String> allocationWith(String suffix) {
        return Optional.of("disk_allocation_" + suffix);
    }

    @SafeVarargs
    private void okWhenValidateSidecarDiskAllocationsScenario(Optional<String>... allocations) {
        assertDoesNotThrow(() ->
                ConfigUtils.validateSidecarDiskAllocations(
                    ImmutableList.copyOf(allocations)
                )
        );
    }

    @Test
    void okWhenValidateSidecarDiskAllocationsAllNonEmpty() {
        okWhenValidateSidecarDiskAllocationsScenario(allocationWith("1"), allocationWith("2"), allocationWith("3"));
    }

    @Test
    void okWhenValidateSidecarDiskAllocationsAllEmpty() {
        okWhenValidateSidecarDiskAllocationsScenario(Optional.empty(), Optional.empty());
    }

    @SafeVarargs
    private void failWhenValidateSidecarDiskAllocationsScenario(String expectedExceptionMessage, Optional<String>... allocations) {
        assertThatThrowsWithMessage(RuntimeException.class, expectedExceptionMessage,
                () -> ConfigUtils.validateSidecarDiskAllocations(
                        ImmutableList.copyOf(allocations)
                )
        );
    }

    @Test
    void failWhenValidateSidecarDiskAllocationsEmptyAndNonEmpty() {
        failWhenValidateSidecarDiskAllocationsScenario( "Allocations should all be empty or all should be filled",
                allocationWith("1"), allocationWith("2"), Optional.empty()
        );
    }

    @Test
    void failWhenValidateSidecarDiskAllocationsHaveDuplications() {
        failWhenValidateSidecarDiskAllocationsScenario("Allocations have duplications",
                allocationWith("1"), allocationWith("1")
                );
    }

    /*
    This is code from old version (before contexts) of ConfigUtils.podSpecPatcher
    It contains required (at this moment) order of patchers
    It is temporary way of documenting important part of system
    It would be replaced by special field in revisions config
    that would contain order of patchers and validation rules for it

    return new CompositePatcher(Map.of(CompositePatcher.DEFAULT_VERSION, ImmutableList.of(
                new DefaultsPatcher(factory, config, podAgentDiskAllocationId,
                        patchBoxSpecificType, useInfraResourceUpdate),
                new MonitoringPatcher(),
                new TvmPatcher(blackboxEnvironments, factory.create(config, "tvm_base_layer", false),
                        factory.create(config, "tvm_layer", false), config.getLong("tvm.disk_size_mb"), tvmDiskAllocationId, allSidecarDiskAllocationIds,
                        config.getString("tvm.installation_tag"),
                        patchBoxSpecificType, releaseGetterTimeout),
                new LogrotatePatcher(factory.create(config, "logrotate_binary", false)),
                new LogbrokerPatcher(factory.create(config, "logbroker_agent_layer", false),
                        logbrokerDiskAllocationId, allSidecarDiskAllocationIds,
                        logbrokerGetterConfig, patchBoxSpecificType,
                        config.getBoolean("enable_logbroker_static_secret_generation"),
                        ConfigUtils.stagesWithEnabledStaticSecretGeneration(config),
                        config.getBoolean("use_reduced_logbroker_guarantees_for_all_stages"),
                        ConfigUtils.stagesWithReducedLogbokerGuarantees(config),
                        config.getString("logbroker_default_stage_rate_limit"),
                        stagesRateLimit(config.getConfig("logbroker_stages_rate_limit")),
                        releaseGetterTimeout),
                new SandboxPatcher(),
                new DockerPatcher(),
                new JugglerPatcher(factory.create(config, "juggler_binary", false)),
                new ThreadLimitsPatcher(),
                new CoredumpPatcher(factory.create(config, "instancectl_binary", false),
                        factory.create(config, "gdb_layer", false)),
                new DynamicResourcePatcher(factory.create(config, "dru_layer", false)),
                new CommonEnvPatcher())));
     */
}
