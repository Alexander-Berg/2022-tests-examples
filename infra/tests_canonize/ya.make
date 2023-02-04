EXECTEST()

OWNER(
    amich
    g:deploy
)

TAG(
    ya:force_sandbox
    ya:fat
    sb:portod
)

SIZE(LARGE)

TIMEOUT(1300)

DEPENDS(
    infra/pod_agent/daemons/pod_agent
)

DATA(
    arcadia/infra/pod_agent/libs/daemon/tests/test_lib/specs.json
    sbr://948421454=search_ubuntu_precise
    sbr://948421454=search_ubuntu_precise_copy
    sbr://942043863=layer_small_data_0
    sbr://836357881=layer_small_data_0_copy
    sbr://617090665=layer_small_data_1
)

RUN(
    NAME DaemonTest.TestAliveAndPing
    infra-pod_agent-libs-daemon-tests DaemonTest::TestAliveAndPing
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestAliveAndPing.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestAliveAndPing.stdout
)

RUN(
    NAME DaemonTest.TestEmptySpec
    infra-pod_agent-libs-daemon-tests DaemonTest::TestEmptySpec
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestEmptySpec.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestEmptySpec.stdout
)

RUN(
    NAME DaemonTest.TestFiles
    infra-pod_agent-libs-daemon-tests DaemonTest::TestFiles
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestFiles.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestFiles.stdout
)

RUN(
    NAME DaemonTest.TestStdoutFile
    infra-pod_agent-libs-daemon-tests DaemonTest::TestStdoutFile
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestStdoutFile.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestStdoutFile.stdout
)

RUN(
    NAME DaemonTest.TestInvalidStartCmd
    infra-pod_agent-libs-daemon-tests DaemonTest::TestInvalidStartCmd
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestInvalidStartCmd.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestInvalidStartCmd.stdout
)

RUN(
    NAME DaemonTest.TestOneLayer
    infra-pod_agent-libs-daemon-tests DaemonTest::TestOneLayer
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestOneLayer.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestOneLayer.stdout
)

RUN(
    NAME DaemonTest.TestLayerBadName
    infra-pod_agent-libs-daemon-tests DaemonTest::TestLayerBadName
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestLayerBadName.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestLayerBadName.stdout
)

RUN(
    NAME DaemonTest.TestOneStaticResource
    infra-pod_agent-libs-daemon-tests DaemonTest::TestOneStaticResource
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestOneStaticResource.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestOneStaticResource.stdout
)

RUN(
    NAME DaemonTest.TestUpdateOneLayer
    infra-pod_agent-libs-daemon-tests DaemonTest::TestUpdateOneLayer
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestUpdateOneLayer.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestUpdateOneLayer.stdout
)

RUN(
    NAME DaemonTest.TestVolumeSpec
    infra-pod_agent-libs-daemon-tests DaemonTest::TestVolumeSpec
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestVolumeSpec.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestVolumeSpec.stdout
)

RUN(
    NAME DaemonTest.TestUpdateWorkloadSpec
    infra-pod_agent-libs-daemon-tests DaemonTest::TestUpdateWorkloadSpec
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestUpdateWorkloadSpec.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestUpdateWorkloadSpec.stdout
)

RUN(
    NAME DaemonTest.TestRemoveWorkloadSpec
    infra-pod_agent-libs-daemon-tests DaemonTest::TestRemoveWorkloadSpec
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestRemoveWorkloadSpec.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestRemoveWorkloadSpec.stdout
)

RUN(
    NAME DaemonTest.TestPodAttributesAndGetStatus
    infra-pod_agent-libs-daemon-tests DaemonTest::TestPodAttributesAndGetStatus
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestPodAttributesAndGetStatus.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestPodAttributesAndGetStatus.stdout
)

RUN(
    NAME DaemonTest.TestSaveSpec
    infra-pod_agent-libs-daemon-tests DaemonTest::TestSaveSpec
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestSaveSpec.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestSaveSpec.stdout
)

RUN(
    NAME DaemonTest.TestVolumePersistency
    infra-pod_agent-libs-daemon-tests DaemonTest::TestVolumePersistency
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestVolumePersistency.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestVolumePersistency.stdout
)

RUN(
    NAME DaemonTest.TestSensors
    infra-pod_agent-libs-daemon-tests DaemonTest::TestSensors
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestSensors.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestSensors.stdout
)

RUN(
    NAME DaemonTest.TestWorkloadAndGC
    infra-pod_agent-libs-daemon-tests DaemonTest::TestWorkloadAndGC
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestWorkloadAndGC.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestWorkloadAndGC.stdout
)

RUN(
    NAME DaemonTest.TestUpdateSpecWithSelectiveRebuild
    infra-pod_agent-libs-daemon-tests DaemonTest::TestUpdateSpecWithSelectiveRebuild
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestUpdateSpecWithSelectiveRebuild.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestUpdateSpecWithSelectiveRebuild.stdout
)

RUN(
    NAME DaemonTest.TestUpdateSpecWithSelectiveRebuildNoWait
    infra-pod_agent-libs-daemon-tests DaemonTest::TestUpdateSpecWithSelectiveRebuildNoWait
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestUpdateSpecWithSelectiveRebuildNoWait.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestUpdateSpecWithSelectiveRebuildNoWait.stdout
)

RUN(
    NAME DaemonTest.TestUpdateSpecWithRemove
    infra-pod_agent-libs-daemon-tests DaemonTest::TestUpdateSpecWithRemove
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestUpdateSpecWithRemove.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestUpdateSpecWithRemove.stdout
)

RUN(
    NAME DaemonTest.TestAddWithTargetCheck
    infra-pod_agent-libs-daemon-tests DaemonTest::TestAddWithTargetCheck
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestAddWithTargetCheck.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestAddWithTargetCheck.stdout
)

RUN(
    NAME DaemonTest.TestChaosInSpec
    infra-pod_agent-libs-daemon-tests DaemonTest::TestChaosInSpec
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestChaosInSpec.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestChaosInSpec.stdout
)

RUN(
    NAME DaemonTest.TestWaitingForLayers
    infra-pod_agent-libs-daemon-tests DaemonTest::TestWaitingForLayers
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestWaitingForLayers.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestWaitingForLayers.stdout
)

RUN(
    NAME DaemonTest.TestSecretsWithWorkload
    infra-pod_agent-libs-daemon-tests DaemonTest::TestSecretsWithWorkload
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestSecretsWithWorkload.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestSecretsWithWorkload.stdout
)

RUN(
    NAME DaemonTest.TestSecretsWithBoxInit
    infra-pod_agent-libs-daemon-tests DaemonTest::TestSecretsWithBoxInit
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestSecretsWithBoxInit.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestSecretsWithBoxInit.stdout
)

RUN(
    NAME DaemonTest.TestManyUlimit
    infra-pod_agent-libs-daemon-tests DaemonTest::TestManyUlimit
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestManyUlimit.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestManyUlimit.stdout
)

RUN(
    NAME DaemonTest.TestBoxPersistIpAllocation
    infra-pod_agent-libs-daemon-tests DaemonTest::TestBoxPersistIpAllocation
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestBoxPersistIpAllocation.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestBoxPersistIpAllocation.stdout
)

RUN(
    NAME DaemonTest.TestLayerTreeMerge
    infra-pod_agent-libs-daemon-tests DaemonTest::TestLayerTreeMerge
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestLayerTreeMerge.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestLayerTreeMerge.stdout
)

RUN(
    NAME DaemonTest.TestCacheLayerMerge
    infra-pod_agent-libs-daemon-tests DaemonTest::TestCacheLayerMerge
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestCacheLayerMerge.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestCacheLayerMerge.stdout
)

RUN(
    NAME DaemonTest.TestStaticResourceTreeMerge
    infra-pod_agent-libs-daemon-tests DaemonTest::TestStaticResourceTreeMerge
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestStaticResourceTreeMerge.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestStaticResourceTreeMerge.stdout
)

RUN(
    NAME DaemonTest.TestCacheStaticResourceMerge
    infra-pod_agent-libs-daemon-tests DaemonTest::TestCacheStaticResourceMerge
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestCacheStaticResourceMerge.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestCacheStaticResourceMerge.stdout
)

RUN(
    NAME DaemonTest.TestBoxWithSkynet
    infra-pod_agent-libs-daemon-tests DaemonTest::TestBoxWithSkynet
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithSkynet.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithSkynet.stdout
)

RUN(
    NAME DaemonTest.TestVirtualDisks
    infra-pod_agent-libs-daemon-tests DaemonTest::TestVirtualDisks
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestVirtualDisks.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestVirtualDisks.stdout
)

RUN(
    NAME DaemonTest.TestBoxWithYt
    infra-pod_agent-libs-daemon-tests DaemonTest::TestBoxWithYt
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithYt.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithYt.stdout
)

RUN(
    NAME DaemonTest.TestBoxWithBaseSearch
    infra-pod_agent-libs-daemon-tests DaemonTest::TestBoxWithBaseSearch
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithBaseSearch.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithBaseSearch.stdout
)

RUN(
    NAME DaemonTest.TestBoxWithRbindVolume
    infra-pod_agent-libs-daemon-tests DaemonTest::TestBoxWithRbindVolume
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithRbindVolume.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithRbindVolume.stdout
)

RUN(
    NAME DaemonTest.TestWorkloadNoReadiness
    infra-pod_agent-libs-daemon-tests DaemonTest::TestWorkloadNoReadiness
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestWorkloadNoReadiness.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestWorkloadNoReadiness.stdout
)

RUN(
    NAME DaemonTest.TestPublicVolume
    infra-pod_agent-libs-daemon-tests DaemonTest::TestPublicVolume
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestPublicVolume.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestPublicVolume.stdout
)

RUN(
    NAME DaemonTest.TestExecWrapper
    infra-pod_agent-libs-daemon-tests DaemonTest::TestExecWrapper
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestExecWrapper.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestExecWrapper.stdout
)

RUN(
    NAME DaemonTest.TestUnixSignalStop
    infra-pod_agent-libs-daemon-tests DaemonTest::TestUnixSignalStop
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestUnixSignalStop.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestUnixSignalStop.stdout
)

RUN(
    NAME DaemonTest.TestPodAgentTargetState
    infra-pod_agent-libs-daemon-tests DaemonTest::TestPodAgentTargetState
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestPodAgentTargetState.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestPodAgentTargetState.stdout
)

RUN(
    NAME DaemonTest.TestHiddenSecretsAndDynamicAttributesInStoredSpec
    infra-pod_agent-libs-daemon-tests DaemonTest::TestHiddenSecretsAndDynamicAttributesInStoredSpec
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestHiddenSecretsAndDynamicAttributesInStoredSpec.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestHiddenSecretsAndDynamicAttributesInStoredSpec.stdout
)

RUN(
    NAME DaemonTest.TestBoxWithRoRootfs
    infra-pod_agent-libs-daemon-tests DaemonTest::TestBoxWithRoRootfs
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithRoRootfs.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithRoRootfs.stdout
)

RUN(
    NAME DaemonTest.TestBoxWithChildOnlyIsolation
    infra-pod_agent-libs-daemon-tests DaemonTest::TestBoxWithChildOnlyIsolation
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithChildOnlyIsolation.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithChildOnlyIsolation.stdout
)

RUN(
    NAME DaemonTest.TestBoxWithCgroupFsMount
    infra-pod_agent-libs-daemon-tests DaemonTest::TestBoxWithCgroupFsMount
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithCgroupFsMount.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithCgroupFsMount.stdout
)

RUN(
    NAME DaemonTest.TestVolumeChangePersistentType
    infra-pod_agent-libs-daemon-tests DaemonTest::TestVolumeChangePersistentType
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestVolumeChangePersistentType.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestVolumeChangePersistentType.stdout
)

RUN(
    NAME DaemonTest.TestBoxWithRoRootfsAndNotDirectoryMountPoints
    infra-pod_agent-libs-daemon-tests DaemonTest::TestBoxWithRoRootfsAndNotDirectoryMountPoints
    STDOUT ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithRoRootfsAndNotDirectoryMountPoints.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/DaemonTest.TestBoxWithRoRootfsAndNotDirectoryMountPoints.stdout
)

DEPENDS(
    infra/pod_agent/libs/daemon/tests
)

END()
