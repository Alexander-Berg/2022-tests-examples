package ru.yandex.infra.stage.podspecs;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.podspecs.patcher.PatchersHolder;
import ru.yandex.infra.stage.podspecs.patcher.PodSpecPatchersHolderFactory;
import ru.yandex.infra.stage.podspecs.patcher.TestWithPatcherContexts;
import ru.yandex.infra.stage.podspecs.patcher.common_env.CommonEnvPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.common_env.CommonEnvPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.common_env.CommonEnvPatcherV3;
import ru.yandex.infra.stage.podspecs.patcher.coredump.CoredumpPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.coredump.CoredumpPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.coredump.CoredumpPatcherV3;
import ru.yandex.infra.stage.podspecs.patcher.default_anon_limit.AnonLimitPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.default_anon_limit.AnonLimitPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.defaults.DefaultsPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.defaults.DefaultsPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.defaults.DefaultsPatcherV3;
import ru.yandex.infra.stage.podspecs.patcher.defaults.DefaultsPatcherV4;
import ru.yandex.infra.stage.podspecs.patcher.defaults.DefaultsPatcherV5;
import ru.yandex.infra.stage.podspecs.patcher.docker.DockerPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.docker.DockerPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.docker.DockerPatcherV3;
import ru.yandex.infra.stage.podspecs.patcher.dynamic_resource.DynamicResourcePatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.dynamic_resource.DynamicResourcePatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.dynamic_resource.DynamicResourcePatcherV3;
import ru.yandex.infra.stage.podspecs.patcher.dynamic_resource.DynamicResourcePatcherV4;
import ru.yandex.infra.stage.podspecs.patcher.juggler.JugglerPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.juggler.JugglerPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.juggler.JugglerPatcherV3;
import ru.yandex.infra.stage.podspecs.patcher.juggler.JugglerPatcherV4;
import ru.yandex.infra.stage.podspecs.patcher.juggler.JugglerPatcherV5;
import ru.yandex.infra.stage.podspecs.patcher.juggler.JugglerPatcherV6;
import ru.yandex.infra.stage.podspecs.patcher.juggler.JugglerPatcherV7;
import ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV3;
import ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV4;
import ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV5;
import ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherV6;
import ru.yandex.infra.stage.podspecs.patcher.logrotate.LogrotatePatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.monitoring.MonitoringPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.monitoring.MonitoringPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.network.NetworkPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.network.NetworkPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.network.NetworkPatcherV3;
import ru.yandex.infra.stage.podspecs.patcher.pod_agent.PodAgentPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.sandbox.SandboxPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.security.SecurityPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.thread_limits.ThreadLimitsPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.thread_limits.ThreadLimitsPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.thread_limits.pod_agent.PodAgentThreadLimitPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherV1;
import ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherV2;
import ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherV3;
import ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherV4;
import ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherV5;
import ru.yandex.yp.client.api.TPodTemplateSpec;

import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class MainPatchersHolderFactoryTest extends TestWithPatcherContexts {

    // TODO add missing patcher classes
    static final List<Class<? extends SpecPatcher<TPodTemplateSpec.Builder>>> PATCHER_CLASSES = ImmutableList.of(
            AnonLimitPatcherV1.class, AnonLimitPatcherV2.class,
            CommonEnvPatcherV1.class, CommonEnvPatcherV2.class, CommonEnvPatcherV3.class,
            CoredumpPatcherV1.class, CoredumpPatcherV2.class, CoredumpPatcherV3.class,
            DefaultsPatcherV1.class, DefaultsPatcherV2.class, DefaultsPatcherV3.class,
            DefaultsPatcherV4.class, DefaultsPatcherV5.class,
            DockerPatcherV1.class, DockerPatcherV2.class, DockerPatcherV3.class,
            DynamicResourcePatcherV1.class, DynamicResourcePatcherV2.class, DynamicResourcePatcherV3.class,
            DynamicResourcePatcherV4.class,
            JugglerPatcherV1.class, JugglerPatcherV2.class, JugglerPatcherV3.class, JugglerPatcherV4.class,
            JugglerPatcherV5.class, JugglerPatcherV6.class, JugglerPatcherV7.class,
            LogbrokerPatcherV1.class, LogbrokerPatcherV2.class, LogbrokerPatcherV3.class, LogbrokerPatcherV4.class,
            LogbrokerPatcherV5.class, LogbrokerPatcherV6.class,
            LogrotatePatcherV1.class, MonitoringPatcherV1.class, MonitoringPatcherV2.class,
            NetworkPatcherV1.class, NetworkPatcherV2.class, NetworkPatcherV3.class,
            PodAgentPatcherV1.class,
            PodAgentThreadLimitPatcherV1.class,
            SandboxPatcherV1.class,
            SecurityPatcherV1.class, SecurityPatcherV2.class,
            ThreadLimitsPatcherV1.class, ThreadLimitsPatcherV2.class,
            TvmPatcherV1.class, TvmPatcherV2.class, TvmPatcherV3.class, TvmPatcherV4.class, TvmPatcherV5.class
    );

    private PatchersHolder<TPodTemplateSpec.Builder> createPatchersHolder() {
        return PodSpecPatchersHolderFactory.fromContexts(contexts);
    }

    @Test
    void createPatchersHolderTest() {
        Assertions.assertDoesNotThrow(this::createPatchersHolder);
    }

    @Test
    void allPatchersCreatedTest() {
        var patchersHolder = createPatchersHolder();

        var patchers = patchersHolder.getPatchersInExactOrder(PATCHER_CLASSES);
        assertThatEquals(patchers.size(), PATCHER_CLASSES.size());
    }
}
