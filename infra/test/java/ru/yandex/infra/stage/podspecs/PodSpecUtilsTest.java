package ru.yandex.infra.stage.podspecs;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.dto.SandboxResourceInfo;
import ru.yandex.infra.stage.dto.SidecarVolumeSettings;
import ru.yandex.infra.stage.util.NamedArgument;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TComputeResources;
import ru.yandex.yp.client.pods.TLayer;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TResource;
import ru.yandex.yp.client.pods.TResourceGang;
import ru.yandex.yp.client.pods.TVolume;
import ru.yandex.yt.ytree.TAttributeDictionary;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.TestData.DEFAULT_STAGE_CONTEXT;
import static ru.yandex.infra.stage.TestData.NOT_USED_BY_INFRA_VOLUME_REQUEST;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.DISABLE_DISK_ISOLATION_LABEL_ATTRIBUTE;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.addBoxIfAbsent;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.addLayerIfAbsent;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.addStaticResourceIfAbsent;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.addVolumeIfAbsent;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.allocationAccordingToDiskIsolationLabel;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.calculateSidecarDiskVolumeDescription;
import static ru.yandex.infra.stage.podspecs.SidecarDiskVolumeDescription.HDD_STORAGE_CLASS;
import static ru.yandex.infra.stage.podspecs.SidecarDiskVolumeDescription.SSD_STORAGE_CLASS;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatThrowsWithMessage;

public class PodSpecUtilsTest {
    private static final long DISK_CAPACITY = 1024L * 1024 * 1024;
    private static final long THREAD_LIMIT = 100L;

    private static final AllComputeResources COMPUTE_RESOURCES = new AllComputeResources(10, 100,
            1024 * 8, 1024 * 128, 1024, DISK_CAPACITY, THREAD_LIMIT);

    @Test
    void addLayerShouldOverwriteAllocationId() {
        String diskVolumeAllocationId1 = "allocation_id_1";
        String diskVolumeAllocationId2 = "allocation_id_2";
        String layerId = "layer_id";

        TResourceGang.Builder resourcesBuilder = TResourceGang.newBuilder()
                .addStaticResources(TResource.newBuilder()
                        .setId(layerId)
                        .setVirtualDiskIdRef(diskVolumeAllocationId1)
                        .build()
                );
        addLayerIfAbsent(resourcesBuilder, layerId, TLayer.newBuilder(), Optional.of(diskVolumeAllocationId2));

        assertThat(resourcesBuilder.getLayers(0).getVirtualDiskIdRef(), equalTo(diskVolumeAllocationId2));
    }

    @Test
    void addVolumeShouldOverwriteAllocationId() {
        String diskVolumeAllocationId1 = "allocation_id_1";
        String diskVolumeAllocationId2 = "allocation_id_2";
        String volumeId = "volume_id";

        TPodAgentSpec.Builder spec = TPodAgentSpec.newBuilder()
                .addVolumes(TVolume.newBuilder()
                        .setId(volumeId)
                        .setVirtualDiskIdRef(diskVolumeAllocationId1)
                        .build());

        addVolumeIfAbsent(spec, volumeId, Optional.of(diskVolumeAllocationId2));

        assertThat(spec.getVolumes(0).getVirtualDiskIdRef(), equalTo(diskVolumeAllocationId2));
    }

    @Test
    void addBoxShouldOverwriteAllocationId() {
        String diskVolumeAllocationId1 = "allocation_id_1";
        String diskVolumeAllocationId2 = "allocation_id_2";
        String boxId = "box_id";

        TPodAgentSpec.Builder spec = TPodAgentSpec.newBuilder()
                .addBoxes(TBox.newBuilder()
                        .setId(boxId)
                        .setVirtualDiskIdRef(diskVolumeAllocationId1)
                        .build());

        addBoxIfAbsent(spec, TBox.newBuilder(), boxId, Optional.of(diskVolumeAllocationId2));

        assertThat(spec.getBoxes(0).getVirtualDiskIdRef(), equalTo(diskVolumeAllocationId2));
    }

    @Test
    void addStaticResourceShouldOverwriteAllocationId() {
        String diskVolumeAllocationId1 = "allocation_id_1";
        String diskVolumeAllocationId2 = "allocation_id_2";
        String resourceId = "resource_id";

        TResourceGang.Builder resourcesBuilder = TResourceGang.newBuilder()
                .addStaticResources(TResource.newBuilder()
                        .setId(resourceId)
                        .setVirtualDiskIdRef(diskVolumeAllocationId1)
                        .build()
                );

        addStaticResourceIfAbsent(resourcesBuilder, TResource.newBuilder(), resourceId, Optional.of(diskVolumeAllocationId2));
        assertThat(resourcesBuilder.getStaticResources(0).getVirtualDiskIdRef(), equalTo(diskVolumeAllocationId2));
    }

    @Test
    void patchRootDiskTest() {
        DataModel.TPodSpec.Builder podSpec = DataModel.TPodSpec.newBuilder()
                .addDiskVolumeRequests(TestData.USED_BY_INFRA_VOLUME_REQUEST);
        PodSpecUtils.patchRootDiskVolumeRequests(podSpec, DISK_CAPACITY);
        assertThat(podSpec.getDiskVolumeRequests(0).getQuotaPolicy().getCapacity(),
                equalTo(TestData.USED_BY_INFRA_VOLUME_REQUEST.getQuotaPolicy().getCapacity() + DISK_CAPACITY));
    }

    @Test
    void addResourceForBoxTest() {
        TBox.Builder box = TBox.newBuilder();
        PodSpecUtils.addResources(box, COMPUTE_RESOURCES, true);
        assertThat(box.getComputeResources(), equalTo(TComputeResources.newBuilder()
                .setMemoryGuarantee(COMPUTE_RESOURCES.getMemoryGuarantee())
                .setVcpuGuarantee(COMPUTE_RESOURCES.getVcpuGuarantee())
                .build()));

        box = TBox.newBuilder().setComputeResources(COMPUTE_RESOURCES.multiply(2).toProto());
        PodSpecUtils.addResources(box, COMPUTE_RESOURCES, true);
        assertBoxConstraints(box);
    }

    @Test
    void addResourceForBoxWithNoConstraintsTest() {
        TBox.Builder box = TBox.newBuilder();
        PodSpecUtils.addResources(box, COMPUTE_RESOURCES, false);
        assertThat(box.getComputeResources(), equalTo(TComputeResources.newBuilder()
                .setMemoryGuarantee(0)
                .setVcpuGuarantee(0)
                .build()));

        box = TBox.newBuilder().setComputeResources(COMPUTE_RESOURCES.multiply(2).toProto());
        PodSpecUtils.addResources(box, COMPUTE_RESOURCES, false);
        assertBoxConstraints(box);
    }

    private void assertBoxConstraints(TBox.Builder box) {
        assertThat(box.getComputeResources(), equalTo(TComputeResources.newBuilder()
                .setMemoryGuarantee(COMPUTE_RESOURCES.getMemoryGuarantee() * 3)
                .setVcpuGuarantee(COMPUTE_RESOURCES.getVcpuGuarantee() * 3)
                .setMemoryLimit(COMPUTE_RESOURCES.getMemoryLimit() * 3)
                .setVcpuLimit(COMPUTE_RESOURCES.getVcpuLimit() * 3)
                .setAnonymousMemoryLimit(COMPUTE_RESOURCES.getAnonymousMemoryLimit() * 3)
                .setThreadLimit(COMPUTE_RESOURCES.getThreadLimit() * 3)
                .build()));
    }

    @Test
    void allocationAccordingToDiskIsolationLabelTest() {
        Optional<String> allocationId = Optional.of("allocation_id");
        TPodTemplateSpec.Builder spec1 = TPodTemplateSpec.newBuilder();
        TPodTemplateSpec.Builder spec2 = TPodTemplateSpec.newBuilder()
                .setLabels(TAttributeDictionary.newBuilder()
                        .addAttributes(DISABLE_DISK_ISOLATION_LABEL_ATTRIBUTE)
                        .build());

        assertThat(allocationAccordingToDiskIsolationLabel(spec1, allocationId), equalTo(allocationId));
        assertThat(allocationAccordingToDiskIsolationLabel(spec2, allocationId), equalTo(Optional.empty()));
    }

    @Test
    void calculateSidecarDiskVolumeDescriptionWhenOneUserDiskTest() {
        String stageId = DEFAULT_STAGE_CONTEXT.getStageId();
        String sidecarDiskAllocationId = "sidecar_allocation_id";
        List<String> allSidecarDiskAllocationIds = ImmutableList.of(sidecarDiskAllocationId);
        String rootDiskStorageClass = TestData.USED_BY_INFRA_VOLUME_REQUEST.getStorageClass();

        Optional<SidecarVolumeSettings> sidecarVolumeSettings1 = Optional.empty();
        Optional<SidecarVolumeSettings> sidecarVolumeSettings2 = Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.AUTO));
        Optional<SidecarVolumeSettings> sidecarVolumeSettings3 = Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.HDD));
        Optional<SidecarVolumeSettings> sidecarVolumeSettings4 = Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.SSD));
        Optional<SidecarVolumeSettings> sidecarVolumeSettings5 = Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.UNRECOGNIZED));

        DataModel.TPodSpec.Builder podSpec = DataModel.TPodSpec.newBuilder()
                .addDiskVolumeRequests(TestData.USED_BY_INFRA_VOLUME_REQUEST);

        SidecarDiskVolumeDescription volDescription1 = calculateSidecarDiskVolumeDescription(stageId, podSpec, sidecarDiskAllocationId, sidecarVolumeSettings1, allSidecarDiskAllocationIds);
        assertThat(volDescription1, equalTo(new SidecarDiskVolumeDescription(sidecarDiskAllocationId, rootDiskStorageClass)));

        SidecarDiskVolumeDescription volDescription2 = calculateSidecarDiskVolumeDescription(stageId, podSpec, sidecarDiskAllocationId, sidecarVolumeSettings2, allSidecarDiskAllocationIds);
        assertThat(volDescription2, equalTo(new SidecarDiskVolumeDescription(sidecarDiskAllocationId, rootDiskStorageClass)));

        SidecarDiskVolumeDescription volDescription3 = calculateSidecarDiskVolumeDescription(stageId, podSpec, sidecarDiskAllocationId, sidecarVolumeSettings3, allSidecarDiskAllocationIds);
        assertThat(volDescription3, equalTo(new SidecarDiskVolumeDescription(sidecarDiskAllocationId, HDD_STORAGE_CLASS)));

        SidecarDiskVolumeDescription volDescription4 = calculateSidecarDiskVolumeDescription(stageId, podSpec, sidecarDiskAllocationId, sidecarVolumeSettings4, allSidecarDiskAllocationIds);
        assertThat(volDescription4, equalTo(new SidecarDiskVolumeDescription(sidecarDiskAllocationId, SSD_STORAGE_CLASS)));

        assertThatThrowsWithMessage(IllegalArgumentException.class,
                "Unrecognized storage class in disk volume sidecar_allocation_id is not permitted, stage id: stage_id",
                () -> calculateSidecarDiskVolumeDescription(stageId, podSpec, sidecarDiskAllocationId, sidecarVolumeSettings5, allSidecarDiskAllocationIds)
        );
    }

    @Test
    void calculateSidecarDiskVolumeDescriptionWhenMultipleUserDisksTest() {
        String stageId = DEFAULT_STAGE_CONTEXT.getStageId();
        String sidecarDiskAllocationId = "sidecar_allocation_id";
        List<String> allSidecarDiskAllocationIds = ImmutableList.of(sidecarDiskAllocationId);

        Optional<SidecarVolumeSettings> sidecarVolumeSettings1 = Optional.empty();
        Optional<SidecarVolumeSettings> sidecarVolumeSettings2 = Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.AUTO));
        Optional<SidecarVolumeSettings> sidecarVolumeSettings3 = Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.HDD));
        Optional<SidecarVolumeSettings> sidecarVolumeSettings4 = Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.SSD));
        Optional<SidecarVolumeSettings> sidecarVolumeSettings5 = Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.UNRECOGNIZED));

        DataModel.TPodSpec.Builder podSpec = DataModel.TPodSpec.newBuilder()
                .addDiskVolumeRequests(TestData.USED_BY_INFRA_VOLUME_REQUEST)
                .addDiskVolumeRequests(TestData.NOT_USED_BY_INFRA_VOLUME_REQUEST);

        assertThatThrowsWithMessage(IllegalArgumentException.class,
                "Empty storage class in disk volume sidecar_allocation_id is not permitted, stage id: stage_id",
                () -> calculateSidecarDiskVolumeDescription(stageId, podSpec, sidecarDiskAllocationId, sidecarVolumeSettings1, allSidecarDiskAllocationIds)
        );

        assertThatThrowsWithMessage(IllegalArgumentException.class,
                "Auto storage class in disk volume sidecar_allocation_id is not permitted with multiple user disks, stage id: stage_id",
                () -> calculateSidecarDiskVolumeDescription(stageId, podSpec, sidecarDiskAllocationId, sidecarVolumeSettings2, allSidecarDiskAllocationIds)
        );

        SidecarDiskVolumeDescription volDescription3 = calculateSidecarDiskVolumeDescription(stageId, podSpec, sidecarDiskAllocationId, sidecarVolumeSettings3, allSidecarDiskAllocationIds);
        assertThat(volDescription3, equalTo(new SidecarDiskVolumeDescription(sidecarDiskAllocationId, HDD_STORAGE_CLASS)));

        SidecarDiskVolumeDescription volDescription4 = calculateSidecarDiskVolumeDescription(stageId, podSpec, sidecarDiskAllocationId, sidecarVolumeSettings4, allSidecarDiskAllocationIds);
        assertThat(volDescription4, equalTo(new SidecarDiskVolumeDescription(sidecarDiskAllocationId, SSD_STORAGE_CLASS)));

        assertThatThrowsWithMessage(IllegalArgumentException.class,
                "Unrecognized storage class in disk volume sidecar_allocation_id is not permitted, stage id: stage_id",
                () -> calculateSidecarDiskVolumeDescription(stageId, podSpec, sidecarDiskAllocationId, sidecarVolumeSettings5, allSidecarDiskAllocationIds)
        );
    }

    @Test
    void calculateSidecarDiskVolumeDescriptionWhenNoUserDiskTest() {
        String sidecarDiskAllocationId = "sidecar_allocation_id";
        List<String> allSidecarDiskAllocationIds = ImmutableList.of(sidecarDiskAllocationId);

        DataModel.TPodSpec.Builder podSpec = DataModel.TPodSpec.newBuilder();

        assertThatThrowsWithMessage(IllegalArgumentException.class,
                "0 number of user disks is not permittted, stage id: stage_id",
                () -> calculateSidecarDiskVolumeDescription(DEFAULT_STAGE_CONTEXT.getStageId(), podSpec, sidecarDiskAllocationId, Optional.empty(), allSidecarDiskAllocationIds)
        );
    }

    @Test
    void addSidecarDiskTest() {
        String allocationId = "allocation_id";
        DataModel.TPodSpec.Builder podSpec = DataModel.TPodSpec.newBuilder();

        SidecarDiskVolumeDescription diskVolumeDescription = new SidecarDiskVolumeDescription(allocationId, HDD_STORAGE_CLASS);
        PodSpecUtils.addSidecarDiskVolumeRequest(podSpec, DISK_CAPACITY, diskVolumeDescription);

        DataModel.TPodSpec.TDiskVolumeRequest sidecarDiskRequest = podSpec.build().getDiskVolumeRequestsList().stream()
                .filter(request->request.getId().equals(allocationId))
                .findFirst()
                .get();

        assertThat(sidecarDiskRequest.getQuotaPolicy().getCapacity(), equalTo(DISK_CAPACITY));
        assertThat(sidecarDiskRequest.getStorageClass(), equalTo(HDD_STORAGE_CLASS));
    }

    @Test
    void addResourceWithoutSidecarDiskForPodTest() {
        DataModel.TPodSpec.TDiskVolumeRequest initRootDiskRequest = TestData.USED_BY_INFRA_VOLUME_REQUEST;

        DataModel.TPodSpec.Builder podSpec = createPodSpecWithResources(COMPUTE_RESOURCES, initRootDiskRequest);

        AllComputeResources resourcesToAdd = COMPUTE_RESOURCES.multiply(3);
        PodSpecUtils.addResources(podSpec, resourcesToAdd, Optional.empty());
        assertDiskCapacityEqualTo(podSpec, initRootDiskRequest.getId(), initRootDiskRequest.getQuotaPolicy().getCapacity() + resourcesToAdd.getDiskCapacity());
    }

    @Test
    void addResourceWithSidecarDiskForPodTest() {
        DataModel.TPodSpec.TDiskVolumeRequest initRootDiskRequest = TestData.USED_BY_INFRA_VOLUME_REQUEST;

        DataModel.TPodSpec.Builder podSpec = createPodSpecWithResources(COMPUTE_RESOURCES, initRootDiskRequest);

        String sidecarDiskAllocationId = "allocation_id";
        SidecarDiskVolumeDescription additionalDiskDescriotion = new SidecarDiskVolumeDescription(sidecarDiskAllocationId, HDD_STORAGE_CLASS);
        AllComputeResources resourcesToAdd = COMPUTE_RESOURCES.multiply(3);
        PodSpecUtils.addResources(podSpec, resourcesToAdd, Optional.of(additionalDiskDescriotion));

        AllComputeResources expectedFinalResources = COMPUTE_RESOURCES.multiply(4);
        assertPodCpuMemResourcesEqualTo(podSpec, expectedFinalResources);
        assertDiskCapacityEqualTo(podSpec, initRootDiskRequest.getId(), initRootDiskRequest.getQuotaPolicy().getCapacity());
        assertDiskCapacityEqualTo(podSpec, sidecarDiskAllocationId, resourcesToAdd.getDiskCapacity());
    }

    @Test
    void addCpuMemResourcesTest() {
        DataModel.TPodSpec.TDiskVolumeRequest initRootDiskRequest = TestData.USED_BY_INFRA_VOLUME_REQUEST;

        DataModel.TPodSpec.Builder podSpec = createPodSpecWithResources(COMPUTE_RESOURCES, initRootDiskRequest);
        AllComputeResources resourcesToAdd = COMPUTE_RESOURCES.multiply(3);
        PodSpecUtils.addCpuMemResources(podSpec, resourcesToAdd);

        AllComputeResources expectedFinalResources = COMPUTE_RESOURCES.multiply(4);
        assertPodCpuMemResourcesEqualTo(podSpec, expectedFinalResources);
    }

    @Test
    void patchDiskVolumeRequestTest() {
        DataModel.TPodSpec.TDiskVolumeRequest.Builder diskVolumeRequest = TestData.NOT_USED_BY_INFRA_VOLUME_REQUEST.toBuilder();
        long additionalDiskCapacity = 300000;

        PodSpecUtils.patchDiskVolumeRequest(diskVolumeRequest, additionalDiskCapacity);
        assertThat(diskVolumeRequest.getQuotaPolicy().getCapacity(), equalTo(NOT_USED_BY_INFRA_VOLUME_REQUEST.getQuotaPolicy().getCapacity() + additionalDiskCapacity));
    }

    private static DataModel.TPodSpec.Builder createPodSpecWithResources(AllComputeResources resources, DataModel.TPodSpec.TDiskVolumeRequest rootDiskRequest) {
        return DataModel.TPodSpec.newBuilder()
                .addDiskVolumeRequests(rootDiskRequest)
                .setResourceRequests(DataModel.TPodSpec.TResourceRequests.newBuilder()
                        .setMemoryLimit(resources.getMemoryLimit())
                        .setAnonymousMemoryLimit(resources.getAnonymousMemoryLimit())
                        .setMemoryGuarantee(resources.getMemoryGuarantee())
                        .setVcpuLimit(resources.getVcpuLimit())
                        .setVcpuGuarantee(resources.getVcpuGuarantee()));
    }

    private static void assertPodCpuMemResourcesEqualTo(DataModel.TPodSpec.Builder podSpec, AllComputeResources resources) {
        assertThat(podSpec.getResourceRequests(), equalTo(DataModel.TPodSpec.TResourceRequests.newBuilder()
                .setMemoryGuarantee(resources.getMemoryGuarantee())
                .setVcpuGuarantee(resources.getVcpuGuarantee())
                .setMemoryLimit(resources.getMemoryLimit())
                .setVcpuLimit(resources.getVcpuLimit())
                .setAnonymousMemoryLimit(resources.getAnonymousMemoryLimit())
                .build()));
    }

    private static void assertDiskCapacityEqualTo(DataModel.TPodSpec.Builder podSpec, String diskAllocationId, long capacity) {
        DataModel.TPodSpec.TDiskVolumeRequest diskRequest = podSpec.build().getDiskVolumeRequestsList().stream()
                .filter(request -> request.getId().equals(diskAllocationId))
                .findFirst()
                .get();

        assertThat(diskRequest.getQuotaPolicy().getCapacity(), equalTo(capacity));
    }

    private static Stream<Arguments> sandboxResourceIdCalculatorTestParameters() {
        var resourceWithMeta = mock(ResourceWithMeta.class);
        when(resourceWithMeta.getMeta()).thenReturn(Optional.of(TestData.RESOURCE_META));

        var withMetaArgument = NamedArgument.of(
                "resource supplier with meta",
                createResourceSupplierMock(resourceWithMeta)
        );

        var resourceWithoutMeta = mock(ResourceWithMeta.class);
        when(resourceWithoutMeta.getMeta()).thenReturn(Optional.empty());

        var withoutMeta = createResourceSupplierMock(resourceWithoutMeta);

        var emptyInfoArgument = NamedArgument.of(
                "empty resource info",
                Optional.empty()
        );

        return Stream.of(
                Arguments.of(
                        NamedArgument.of(
                                "with sandbox resource info",
                                Optional.of(TestData.DEFAULT_SIDECAR_RESOURCE_INFO)
                        ),
                        withMetaArgument,
                        NamedArgument.of(
                                "revision from resource info",
                                Optional.of(TestData.DEFAULT_SIDECAR_RESOURCE_INFO.getRevision())
                        )
                ),
                Arguments.of(
                        emptyInfoArgument,
                        withMetaArgument,
                        NamedArgument.of(
                                "resource id from meta",
                                Optional.of(TestData.RESOURCE_META.getResourceId())
                        )
                ),
                Arguments.of(
                        emptyInfoArgument,
                        NamedArgument.of(
                                "resource supplier without meta",
                                withoutMeta
                        ),
                        NamedArgument.of(
                                "empty resource id",
                                Optional.empty()
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("sandboxResourceIdCalculatorTestParameters")
    void sandboxResourceIdCalculatorTest(NamedArgument<Optional<SandboxResourceInfo>> resourceInfo,
                                         NamedArgument<ResourceSupplier> defaultResourceSupplier,
                                         NamedArgument<Optional<Long>> expectedResourceId) {
        var actualResourceId = PodSpecUtils.SANDBOX_RESOURCE_ID_CALCULATOR.calculate(
                resourceInfo.getArgument(),
                defaultResourceSupplier.getArgument()
        );

        assertThatEquals(actualResourceId, expectedResourceId.getArgument());
    }

    public static ResourceSupplier createResourceSupplierMock(ResourceWithMeta resourceWithMeta) {
        var resourceSupplier = mock(ResourceSupplier.class);
        when(resourceSupplier.get()).thenReturn(resourceWithMeta);
        return resourceSupplier;
    }

    public static PodSpecUtils.SandboxResourceIdCalculator createSandboxResourceIdCalculatorMock(Optional<Long> resourceId) {
        var sandboxResourceIdCalculator = mock(PodSpecUtils.SandboxResourceIdCalculator.class);

        when(sandboxResourceIdCalculator.calculate(
                any(Optional.class),
                any(ResourceSupplier.class)
        )).thenReturn(resourceId);

        return sandboxResourceIdCalculator;
    }
}

