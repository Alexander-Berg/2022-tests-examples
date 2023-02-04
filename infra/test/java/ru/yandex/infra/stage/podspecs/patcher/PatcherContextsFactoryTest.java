package ru.yandex.infra.stage.podspecs.patcher;

import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.ConfigUtils;
import ru.yandex.infra.stage.podspecs.ResourceSupplier;
import ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherConfigParser;
import ru.yandex.infra.stage.podspecs.patcher.security.FoldersLayerUrls;

import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.COREDUMP_GDB_LAYER_RESOURCE_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.COREDUMP_INSTANCECTL_BINARY_RESOURCE_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.DEFAULTS_POD_AGENT_BINARY_RESOURCE_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.DEFAULTS_POD_AGENT_LAYER_RESOURCE_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.DYNAMIC_RESOURCE_DRU_LAYER_RESOURCE_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.JUGGLER_BINARY_RESOURCE_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.LOGBROKER_AGENT_LAYER_RESOURCE_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.LOGROTATE_BINARY_RESOURCE_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.PATCH_BOX_SPECIFIC_TYPE_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.PLACE_BINARY_REVISION_TO_POD_AGENT_META_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.SECURITY_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.SECURITY_COREDUMP_FOLDERS_LAYER_URL_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.SECURITY_FIRST_AFFECTED_POD_AGENT_RESOURCE_ID_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.SECURITY_JUGGLER_FOLDERS_LAYER_URL_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.SECURITY_LOGBROKER_FOLDERS_LAYER_URL_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.SECURITY_LOGROTATE_FOLDERS_LAYER_URL_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.SECURITY_POD_AGENT_FOLDERS_LAYER_URL_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.SECURITY_PORTOSHELL_FOLDERS_LAYER_URL_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.TVM_BASE_LAYER_RESOURCE_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.TVM_CONFIG_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.TVM_DISK_SIZE_MB_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.TVM_INSTALLATION_TAG_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.PatcherContextsFactory.TVM_LAYER_RESOURCE_NAME;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatSameInstance;

public class PatcherContextsFactoryTest extends TestWithPatcherContexts {

    private static ResourceSupplier dummyResourceSupplierWith(String resourceName, boolean useChecksum) {
        return new DummyResourceSupplier(CORRECT_PATCHERS_CONFIG, resourceName, useChecksum);
    }

    @Test
    public void testCoredumpPatcherContext() {
        var context = contexts.getCoredumpPatcherContexts()
                .getCoredumpPatcherV1Context();

        assertThatEquals(context.getInstancectlSupplier(),
                dummyResourceSupplierWith(COREDUMP_INSTANCECTL_BINARY_RESOURCE_NAME, false));

        assertThatEquals(context.getGdbSupplier(),
                dummyResourceSupplierWith(COREDUMP_GDB_LAYER_RESOURCE_NAME, false));
    }

    @Test
    public void testDefaultsPatcherContext() {
        var context = contexts.getDefaultsPatcherContexts().getDefaultsPatcherV1Context();

        assertThatEquals(context.getDefaultPodAgentBinarySupplier(),
                dummyResourceSupplierWith(DEFAULTS_POD_AGENT_BINARY_RESOURCE_NAME, true));

        assertThatEquals(context.getDefaultPodAgentLayerSupplier(),
                dummyResourceSupplierWith(DEFAULTS_POD_AGENT_LAYER_RESOURCE_NAME, true));

        assertThatEquals(context.getPodAgentAllocationId(), parameters.getPodAgentAllocationId());
        assertThatEquals(context.isPatchBoxSpecificType(), CORRECT_PATCHERS_CONFIG.getBoolean(PATCH_BOX_SPECIFIC_TYPE_CONFIG_PATH));
        assertThatEquals(context.placeBinaryRevisionToPodAgentMeta(), CORRECT_PATCHERS_CONFIG.getBoolean(PLACE_BINARY_REVISION_TO_POD_AGENT_META_CONFIG_PATH));

        assertThatEquals(context.getAllSidecarDiskAllocationIds(), parameters.getAllSidecarDiskAllocationIds());
        assertThatEquals(context.getReleaseGetterTimeoutSeconds(), parameters.getReleaseGetterTimeoutSeconds());
    }

    @Test
    public void testDynamicResourcePatcherContext() {
        var context = contexts.getDynamicResourcePatcherContexts().getDynamicResourcePatcherV1Context();

        assertThatEquals(context.getDruDefaultLayerSupplier(),
                dummyResourceSupplierWith(DYNAMIC_RESOURCE_DRU_LAYER_RESOURCE_NAME, false));

        assertThatEquals(context.getDruLayerSupplier(),
                dummyResourceSupplierWith(DYNAMIC_RESOURCE_DRU_LAYER_RESOURCE_NAME, false));

        assertThatEquals(context.getReleaseGetterTimeoutSeconds(),
                parameters.getReleaseGetterTimeoutSeconds());
    }

    @Test
    public void testJugglerPatcherContext() {
        var context = contexts.getJugglerPatcherContexts().getJugglerPatcherV1Context();

        assertThatEquals(context.getJugglerBinaryDefaultSupplier(),
                dummyResourceSupplierWith(JUGGLER_BINARY_RESOURCE_NAME, false));

        assertThatEquals(context.getAllSidecarDiskAllocationIds(), parameters.getAllSidecarDiskAllocationIds());
    }

    @Test
    public void testLogbrokerPatcherContext() {
        var context = contexts.getLogbrokerPatcherContexts().getLogbrokerPatcherV1Context();

        assertThatEquals(context.getLogbrokerAgentLayerSupplier(),
                dummyResourceSupplierWith(LOGBROKER_AGENT_LAYER_RESOURCE_NAME, false)
        );

        assertThatEquals(context.getDiskVolumeAllocationId(), parameters.getLogbrokerAllocationId());
        assertThatEquals(context.getAllSidecarDiskAllocationIds(), parameters.getAllSidecarDiskAllocationIds());

        assertThatEquals(context.isPatchBoxSpecificType(), CORRECT_PATCHERS_CONFIG.getBoolean(PATCH_BOX_SPECIFIC_TYPE_CONFIG_PATH));

        var logbrokerPatcherConfig = LogbrokerPatcherConfigParser.parseConfig(
                ConfigUtils.logbrokerPatcherConfig(CORRECT_PATCHERS_CONFIG)
        );

        assertThatEquals(context.getUnifiedAgentConfigFactory(), logbrokerPatcherConfig.getUnifiedAgentConfigFactory());
        assertThatEquals(context.getBoxResourcesConfig(), logbrokerPatcherConfig.getBoxResourcesConfig());

        assertThatEquals(context.getReleaseGetterTimeoutSeconds(), parameters.getReleaseGetterTimeoutSeconds());
    }

    @Test
    public void testLogrotatePatcherContext() {
        var context = contexts.getLogrotatePatcherContexts().getLogrotatePatcherV1Context();

        assertThatEquals(context.getLogrotateResourceSupplier(),
                dummyResourceSupplierWith(LOGROTATE_BINARY_RESOURCE_NAME, false));
    }

    @Test
    public void testTvmPatcherContext() {
        var context = contexts.getTvmPatcherContexts().getTvmPatcherV1Context();

        assertThatSameInstance(context.getBlackboxEnvironments(), parameters.getBlackboxEnvironments());

        assertThatEquals(context.getBaseLayerSupplier(),
                dummyResourceSupplierWith(TVM_BASE_LAYER_RESOURCE_NAME, false));

        assertThatEquals(context.getTvmLayerSupplier(),
                dummyResourceSupplierWith(TVM_LAYER_RESOURCE_NAME, false));

        var tvmConfig = CORRECT_PATCHERS_CONFIG.getConfig(TVM_CONFIG_CONFIG_PATH);

        assertThatEquals(context.getDiskSpaceMb(), tvmConfig.getLong(TVM_DISK_SIZE_MB_CONFIG_PATH));
        assertThatEquals(context.getInstallationTag(), tvmConfig.getString(TVM_INSTALLATION_TAG_CONFIG_PATH));

        assertThatEquals(context.getDiskVolumeAllocationId(), parameters.getTvmAllocationId());
        assertThatEquals(context.getAllSidecarDiskAllocationIds(), parameters.getAllSidecarDiskAllocationIds());

        assertThatEquals(context.isPatchBoxSpecificType(), CORRECT_PATCHERS_CONFIG.getBoolean(PATCH_BOX_SPECIFIC_TYPE_CONFIG_PATH));

        assertThatEquals(context.getReleaseGetterTimeoutSeconds(), parameters.getReleaseGetterTimeoutSeconds());
    }

    @Test
    public void testSecurityPatcherContext() {
        var context = contexts.getSecurityPatcherContexts().getSecurityPatcherV1Context();

        var securityConfig = CORRECT_PATCHERS_CONFIG.getConfig(SECURITY_CONFIG_PATH);

        assertThatEquals(context.getFirstAffectedPodAgentBinaryVersion(), securityConfig.getLong(SECURITY_FIRST_AFFECTED_POD_AGENT_RESOURCE_ID_CONFIG_PATH));
        assertThatEquals(context.getDefaultPodAgentBinarySupplier(), dummyResourceSupplierWith(DEFAULTS_POD_AGENT_BINARY_RESOURCE_NAME, true));

        var foldersLayerUrls = new FoldersLayerUrls(
                securityConfig.getString(SECURITY_POD_AGENT_FOLDERS_LAYER_URL_CONFIG_PATH),
                securityConfig.getString(SECURITY_PORTOSHELL_FOLDERS_LAYER_URL_CONFIG_PATH),
                securityConfig.getString(SECURITY_COREDUMP_FOLDERS_LAYER_URL_CONFIG_PATH),
                securityConfig.getString(SECURITY_LOGBROKER_FOLDERS_LAYER_URL_CONFIG_PATH),
                securityConfig.getString(SECURITY_JUGGLER_FOLDERS_LAYER_URL_CONFIG_PATH),
                securityConfig.getString(SECURITY_LOGROTATE_FOLDERS_LAYER_URL_CONFIG_PATH)
        );

        assertThatEquals(context.getFoldersLayerUrls(), foldersLayerUrls);
    }
}
