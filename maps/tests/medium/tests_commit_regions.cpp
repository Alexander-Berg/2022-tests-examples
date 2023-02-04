#include <maps/wikimap/mapspro/libs/revision_meta/include/commit_regions.h>

#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/unittest/arcadia.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::revision_meta::tests {

using namespace maps::chrono::literals;

namespace {

const revision::UserID TEST_USER_ID = 1;

const TCommitId TEST_COMMIT_ID_1 = 1;
const TCommitId TEST_COMMIT_ID_2 = 2;
const TCommitId TEST_COMMIT_ID_3 = 3;

const TOId TEST_REGION_ID_A = 123;
const TOId TEST_REGION_ID_B = 456;
const TOId TEST_REGION_ID_C = 789;

} // namespace

Y_UNIT_TEST_SUITE_F(commit_regions, unittest::ArcadiaDbFixture) {

Y_UNIT_TEST(test_the_main_scenario)
{
    auto txn = pool().masterWriteableTransaction();

    revision::BranchManager branchManager(*txn);
    branchManager.createApproved(TEST_USER_ID, {});
    auto branch = branchManager.createStable(TEST_USER_ID, {});
    branch.setState(*txn, revision::BranchState::Normal);

    CommitRegions commitRegions(*txn);
    commitRegions.push(TEST_COMMIT_ID_1, { TEST_REGION_ID_A, TEST_REGION_ID_B });
    commitRegions.push(TEST_COMMIT_ID_2, { TEST_REGION_ID_B, TEST_REGION_ID_C });
    commitRegions.push(TEST_COMMIT_ID_3, { TEST_REGION_ID_A });

    auto commitToRegions = commitRegions.getCommitRegions({ TEST_COMMIT_ID_1, TEST_COMMIT_ID_2, TEST_COMMIT_ID_3 });
    UNIT_ASSERT_EQUAL(commitToRegions, TCommitToRegions({
        { TEST_COMMIT_ID_1, { TEST_REGION_ID_A, TEST_REGION_ID_B } },
        { TEST_COMMIT_ID_2, { TEST_REGION_ID_B, TEST_REGION_ID_C } },
        { TEST_COMMIT_ID_3, { TEST_REGION_ID_A } }
    }));

    auto notPublishedCommitIds = commitRegions.loadNotPublished({});
    UNIT_ASSERT(notPublishedCommitIds.empty());

    notPublishedCommitIds = commitRegions.loadNotPublished({ TEST_COMMIT_ID_1 });
    UNIT_ASSERT_EQUAL(notPublishedCommitIds, TCommitIds({ TEST_COMMIT_ID_1 }));

    notPublishedCommitIds = commitRegions.loadNotPublished({ TEST_COMMIT_ID_1, TEST_COMMIT_ID_2 });
    UNIT_ASSERT_EQUAL(notPublishedCommitIds, TCommitIds({ TEST_COMMIT_ID_1, TEST_COMMIT_ID_2 }));

    commitRegions.toStable(branch, { TEST_COMMIT_ID_1, TEST_COMMIT_ID_2 });

    auto notPublishedCommitIdsInRegion = commitRegions.getNotPublished(TEST_REGION_ID_A, { branch.id() });
    UNIT_ASSERT_EQUAL(notPublishedCommitIdsInRegion, TCommitIds({ TEST_COMMIT_ID_1 }));

    notPublishedCommitIdsInRegion = commitRegions.getNotPublished(TEST_REGION_ID_B, { branch.id() });
    UNIT_ASSERT_EQUAL(notPublishedCommitIdsInRegion, TCommitIds({ TEST_COMMIT_ID_1, TEST_COMMIT_ID_2 }));

    commitRegions.publish({ TEST_COMMIT_ID_1 });

    notPublishedCommitIds = commitRegions.loadNotPublished({ TEST_COMMIT_ID_1 });
    UNIT_ASSERT(notPublishedCommitIds.empty());

    notPublishedCommitIdsInRegion = commitRegions.getNotPublished(TEST_REGION_ID_A, { branch.id() });
    UNIT_ASSERT(notPublishedCommitIdsInRegion.empty());

    notPublishedCommitIdsInRegion = commitRegions.getNotPublished(TEST_REGION_ID_B, { branch.id() });
    UNIT_ASSERT_EQUAL(notPublishedCommitIdsInRegion, TCommitIds({ TEST_COMMIT_ID_2 }));
}

Y_UNIT_TEST(test_remove)
{
    {
        auto txn = pool().masterWriteableTransaction();
        CommitRegions commitRegions(*txn);
        commitRegions.push(TEST_COMMIT_ID_1, { TEST_REGION_ID_A, TEST_REGION_ID_B });
        txn->commit();
    }

    auto txn = pool().masterWriteableTransaction();
    CommitRegions commitRegions(*txn);

    auto affectedRows = commitRegions.removeOldPublishedCommits();
    UNIT_ASSERT_VALUES_EQUAL(affectedRows, 0); // commit is not old and not published

    affectedRows = commitRegions.removeOldPublishedCommits(0_days);
    UNIT_ASSERT_VALUES_EQUAL(affectedRows, 0); // commit is not published

    commitRegions.publish({ TEST_COMMIT_ID_1 });
    affectedRows = commitRegions.removeOldPublishedCommits(0_days);
    UNIT_ASSERT_VALUES_EQUAL(affectedRows, 1); // I got you
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::revision_meta::tests
