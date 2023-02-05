#include "helpers.h"

#include <yandex/maps/wiki/diffalert/revision/diff_context.h>
#include <yandex/maps/wiki/diffalert/revision/editor_config.h>
#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/snapshot_id.h>
#include <yandex/maps/wiki/unittest/arcadia.h>
#include <maps/wikimap/mapspro/libs/acl/include/aclgateway.h>

#include <maps/libs/common/include/file_utils.h>

namespace maps {
namespace wiki {
namespace diffalert {
namespace tests {

namespace {

const uint64_t CARTOGRAPHER_USER_UID = 100001;
const uint64_t OUTSOURCE_USER_UID = 100002;
const uint64_t COMMON_USER_UID = 100003;

const TString EDITOR_CONFIG_PATH =
    ArcadiaSourceRoot() + "/maps/wikimap/mapspro/cfg/editor/editor.xml";

struct Fixture : public unittest::ArcadiaDbFixture
{
    Fixture()
    {
        maps::log8::setLevel(maps::log8::Level::FATAL);

        executeSqlInTransaction(
            maps::common::readFileToString(
                dataPath("sql/commit_filter_tests.sql")));

        auto txn = pool().masterWriteableTransaction();

        acl::ACLGateway aclGateway(*txn);
        auto outsourceRole = aclGateway.createRole("outsource-role", "");
        auto cartographerRole = aclGateway.createRole("cartographer", "");

        auto cartographer = aclGateway.createUser(
            CARTOGRAPHER_USER_UID, "cartographer-user", "", CARTOGRAPHER_USER_UID);
        auto outsourcer = aclGateway.createUser(
            OUTSOURCE_USER_UID, "outsourcer-user", "", OUTSOURCE_USER_UID);
        auto commonUser = aclGateway.createUser(
            COMMON_USER_UID, "common-user", "", COMMON_USER_UID);

        aclGateway.createPolicy(cartographer, cartographerRole, aclGateway.aoi(0));
        aclGateway.createPolicy(outsourcer, outsourceRole, aclGateway.aoi(0));

        txn->commit();
    }

    CompareSnapshotsResult compare(
        uint64_t commit1,
        uint64_t commit2,
        const CommitFilter& commitFilter)
    {
        EditorConfig editorConfig(EDITOR_CONFIG_PATH);
        auto txn = pool().masterReadOnlyTransaction();
        revision::BranchManager branchMgr(*txn);
        auto oldBranch = branchMgr.load(2);
        auto oldSnapshotId =
            revision::SnapshotId::fromCommit(commit1, oldBranch.type(), *txn);
        auto newBranch = branchMgr.load(2);
        auto newSnapshotId =
            revision::SnapshotId::fromCommit(commit2, newBranch.type(), *txn);
        txn.releaseConnection();

        return LongtaskDiffContext::compareSnapshots(
            oldBranch, oldSnapshotId,
            newBranch, newSnapshotId,
            pool(),
            pool(),
            editorConfig,
            commitFilter);
    }
};

} // namespace

Y_UNIT_TEST_SUITE_F(commit_filter, Fixture) {

Y_UNIT_TEST(test_exclude_group_moved_objects)
{
    CommitFilter commitFilter;
    commitFilter.setExcludedActionTypes({ActionType::GroupMove});

    auto result = compare(1, 6, commitFilter);

    // There are 2 objects affected by the specified commits
    // One of them is excluded
    UNIT_ASSERT_VALUES_EQUAL(result.diffContexts().size(), 2);
}

Y_UNIT_TEST(test_exclude_imported_objects)
{
    CommitFilter commitFilter;

    auto result = compare(6, 8, commitFilter);

    UNIT_ASSERT_VALUES_EQUAL(result.diffContexts().size(), 2);

    commitFilter.setExcludedActionTypes({ActionType::Import, ActionType::GroupEditAttributes});

    result = compare(6, 8, commitFilter);

    // There are 2 objects affected by the specified commits
    // One of them is edited by a long task, so it is excluded
    UNIT_ASSERT_VALUES_EQUAL(result.diffContexts().size(), 1);
}

Y_UNIT_TEST(test_exclude_objects_edited_by_cartographers)
{
    CommitFilter commitFilter;
    commitFilter.setIncludedUserTypes({UserType::Common});

    auto result = compare(1, 6, commitFilter);

    // There are 3 objects edited by different types of users
    // One of them is edited by the common user
    UNIT_ASSERT_VALUES_EQUAL(result.diffContexts().size(), 1);

    commitFilter.setIncludedUserTypes({UserType::Common, UserType::Outsourcer});

    result = compare(1, 6, commitFilter);

    // There are 3 objects edited by different types of users
    // One of them is edited by the common user
    // Another is edited by the outsourcer
    UNIT_ASSERT_VALUES_EQUAL(result.diffContexts().size(), 2);
}

Y_UNIT_TEST(test_propagate_commits_to_masters)
{
    CommitFilter commitFilter;

    auto result = compare(9, 11, commitFilter);

    // There are 2 objects: rd_el and rd
    UNIT_ASSERT_VALUES_EQUAL(result.diffContexts().size(), 2);

    commitFilter.setIncludedUserTypes({UserType::Common});

    result = compare(9, 11, commitFilter);

    // There are 2 objects: rd_el and rd
    // rd_el is edited by the cartographer
    // rd is edited by the common user
    // due to commit propagation rd should get the commit made by the cartographer
    UNIT_ASSERT_VALUES_EQUAL(result.diffContexts().size(), 0);
}

} // Y_UNIT_TEST_SUITE_F

} // namespace tests
} // namespace diffalert
} // namespace wiki
} // namespace maps
