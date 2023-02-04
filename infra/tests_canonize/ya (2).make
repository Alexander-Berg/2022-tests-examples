EXECTEST()

OWNER(
    amich
    g:deploy
)

RUN(
    NAME ResourceCacheControllerTest.TestAddNewResourcesToStatus
    resource_cache_controller_ut ResourceCacheControllerTest::TestAddNewResourcesToStatus
    STDOUT ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestAddNewResourcesToStatus.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestAddNewResourcesToStatus.stdout
)

RUN(
    NAME ResourceCacheControllerTest.TestRemoveResourcesFromSpecAndStatus
    resource_cache_controller_ut ResourceCacheControllerTest::TestRemoveResourcesFromSpecAndStatus
    STDOUT ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestRemoveResourcesFromSpecAndStatus.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestRemoveResourcesFromSpecAndStatus.stdout
)

RUN(
    NAME ResourceCacheControllerTest.TestAddNewRevisionWithoutChange
    resource_cache_controller_ut ResourceCacheControllerTest::TestAddNewRevisionWithoutChange
    STDOUT ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestAddNewRevisionWithoutChange.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestAddNewRevisionWithoutChange.stdout
)

RUN(
    NAME ResourceCacheControllerTest.TestAddNewRevisionWithChange
    resource_cache_controller_ut ResourceCacheControllerTest::TestAddNewRevisionWithChange
    STDOUT ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestAddNewRevisionWithChange.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestAddNewRevisionWithChange.stdout
)

RUN(
    NAME ResourceCacheControllerTest.TestMoveOldRevisionToFront
    resource_cache_controller_ut ResourceCacheControllerTest::TestMoveOldRevisionToFront
    STDOUT ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestMoveOldRevisionToFront.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestMoveOldRevisionToFront.stdout
)

RUN(
    NAME ResourceCacheControllerTest.TestRemoveExtraRevisions
    resource_cache_controller_ut ResourceCacheControllerTest::TestRemoveExtraRevisions
    STDOUT ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestRemoveExtraRevisions.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestRemoveExtraRevisions.stdout
)

RUN(
    NAME ResourceCacheControllerTest.TestRemoveSameRevisions
    resource_cache_controller_ut ResourceCacheControllerTest::TestRemoveSameRevisions
    STDOUT ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestRemoveSameRevisions.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestRemoveSameRevisions.stdout
)

RUN(
    NAME ResourceCacheControllerTest.TestCondition
    resource_cache_controller_ut ResourceCacheControllerTest::TestCondition
    STDOUT ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestCondition.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestCondition.stdout
)

RUN(
    NAME ResourceCacheControllerTest.TestConditionAllReady
    resource_cache_controller_ut ResourceCacheControllerTest::TestConditionAllReady
    STDOUT ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestConditionAllReady.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestConditionAllReady.stdout
)

RUN(
    NAME ResourceCacheControllerTest.TestGenerateUpdates
    resource_cache_controller_ut ResourceCacheControllerTest::TestGenerateUpdates
    STDOUT ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestGenerateUpdates.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestGenerateUpdates.stdout
)

RUN(
    NAME ResourceCacheControllerTest.CheckGenerateUpdatesWithSpecificUpdateWindow
    resource_cache_controller_ut ResourceCacheControllerTest::CheckGenerateUpdatesWithSpecificUpdateWindow
    STDOUT ${TEST_OUT_ROOT}/ResourceCacheControllerTest.CheckGenerateUpdatesWithSpecificUpdateWindow.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/ResourceCacheControllerTest.CheckGenerateUpdatesWithSpecificUpdateWindow.stdout
)

RUN(
    NAME ResourceCacheControllerTest.TestGeneratePodAgentResourceCacheSpec
    resource_cache_controller_ut ResourceCacheControllerTest::TestGeneratePodAgentResourceCacheSpec
    STDOUT ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestGeneratePodAgentResourceCacheSpec.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestGeneratePodAgentResourceCacheSpec.stdout
)

RUN(
    NAME ResourceCacheControllerTest.TestConditionLastTransitionTime
    resource_cache_controller_ut ResourceCacheControllerTest::TestConditionLastTransitionTime
    STDOUT ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestConditionLastTransitionTime.stdout
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/ResourceCacheControllerTest.TestConditionLastTransitionTime.stdout
)

DEPENDS(
    infra/resource_cache_controller/libs/resource_cache_controller/tests
)

END()
