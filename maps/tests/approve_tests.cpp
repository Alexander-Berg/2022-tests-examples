#include "common.h"
#include "helpers.h"

#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/commit_manager.h>
#include <yandex/maps/wiki/revision/common.h>
#include <yandex/maps/wiki/revision/exception.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>

#include <maps/wikimap/mapspro/libs/revision/ut/fixtures/revision_creator.h>

#include <boost/test/unit_test.hpp>

namespace maps {
namespace wiki {
namespace revision {
namespace tests {

namespace {

struct ApproveTestFixture: public DbFixture
{
    void testApprove(const DBIDSet& commitIds, const DBIDSet& answer);
    void testFindDraftContributingCommits(
        const DBIDSet& commitIds,
        const ContributingCommitsOptions& options,
        const DBIDSet& answer);
};

void ApproveTestFixture::testApprove(const DBIDSet& commitIds, const DBIDSet& answer)
{
    pqxx::work work(getConnection());
    CommitManager commiter(work);
    const auto result = commiter.approve(commitIds);
    work.commit();
    BOOST_CHECK_EQUAL_COLLECTIONS(answer.begin(), answer.end(), result.begin(), result.end());
}

void ApproveTestFixture::testFindDraftContributingCommits(
    const DBIDSet& commitIds,
    const ContributingCommitsOptions& options,
    const DBIDSet& answer)
{
    pqxx::work work(getConnection());
    CommitManager committer(work);
    const auto result = committer.findDraftContributingCommits(commitIds, options);
    BOOST_CHECK_EQUAL_COLLECTIONS(answer.begin(), answer.end(), result.begin(), result.end());
}

} // namespace

BOOST_FIXTURE_TEST_SUITE(TestApproveCommits, ApproveTestFixture)

BOOST_AUTO_TEST_CASE(T00_EmptyInput)
{
    setTestData();
    pqxx::work work(getConnection());
    CommitManager commiter(work);
    const DBIDSet commitIds {};
    BOOST_CHECK_NO_THROW(commiter.approve(commitIds));
}

BOOST_AUTO_TEST_CASE(T00_NonexistenCommitId)
{
    setTestData();
    pqxx::work work(getConnection());
    CommitManager commiter(work);
    const DBIDSet commitIds {13};
    BOOST_CHECK_THROW(commiter.approve(commitIds), maps::Exception);
}

BOOST_AUTO_TEST_CASE(T01_OneCommit)
{
    setTestData("sql/005-OneCommit.sql");
    const DBIDSet commitIds {1};
    const DBIDSet approvedCommitIds {1};
    testApprove(commitIds, approvedCommitIds);
}

BOOST_AUTO_TEST_CASE(T02_TwoRoadElementsConnection)
{
    setTestData("sql/006-TwoRoadElementsConnection.sql");
    const DBIDSet commitIds {3};
    const DBIDSet approvedCommitIds {1, 2, 3};
    testApprove(commitIds, approvedCommitIds);
}

BOOST_AUTO_TEST_CASE(T03_DeletedRoadElement)
{
    setTestData("sql/007-DeletedRoadProblem.sql");
    const DBIDSet commitIds {3};
    const DBIDSet approvedCommitIds {3};
    testApprove(commitIds, approvedCommitIds);
}

BOOST_AUTO_TEST_CASE(T04_RoadElementClosedPolyline)
{
    setTestData("sql/008-RoadElementClosedPolyline.sql");
    const DBIDSet commitIds {4};
    const DBIDSet approvedCommitIds {1, 2, 3, 4};
    testApprove(commitIds, approvedCommitIds);
}

BOOST_AUTO_TEST_CASE(T05_RoadEdit_ApprovedCommit5)
{
    setTestData("sql/009-RoadEditInTrunk.sql");
    const DBIDSet commitIds {5};
    const DBIDSet approvedCommitIds {1, 2, 5};
    testApprove(commitIds, approvedCommitIds);
}

BOOST_AUTO_TEST_CASE(T05_RoadEditInTrunk_RevertCommit6)
{
    setTestData("sql/009-RoadEditInTrunk.sql");
    const DBIDSet commitIds {6};
    const DBIDSet approvedCommitIds {1, 2, 3, 4, 6};
    testApprove(commitIds, approvedCommitIds);
}

BOOST_AUTO_TEST_CASE(T05_RoadEditInTrunk_ApproveCommit7)
{
    setTestData("sql/009-RoadEditInTrunk.sql");
    const DBIDSet commitIds {7};
    const DBIDSet approvedCommitIds {1, 2, 3, 4, 5, 6, 7};
    testApprove(commitIds, approvedCommitIds);
}

BOOST_AUTO_TEST_CASE(T06_RoadEditInStable_ApproveCommit7)
{
    setTestData("sql/012-RoadEditInStable.sql");
    const DBIDSet commitIds {7};
    const DBIDSet approvedCommitIds {};
    testApprove(commitIds, approvedCommitIds);
}

BOOST_AUTO_TEST_CASE(T06_BranchEdit_ApproveCommit5)
{
    setTestData("sql/010-BranchEdit.sql");
    const DBIDSet commitIds {5};
    const DBIDSet approvedCommitIds {};
    testApprove(commitIds, approvedCommitIds);
}

BOOST_AUTO_TEST_CASE(T06_BranchEdit_ApproveCommit6)
{
    setTestData("sql/010-BranchEdit.sql");
    const DBIDSet commitIds {6};
    const DBIDSet approvedCommitIds {6};
    testApprove(commitIds, approvedCommitIds);
}

BOOST_AUTO_TEST_CASE(T07_ReferencedAddressPoint_DoubleApprove)
{
    {
        setTestData("sql/011-ReferencedAddressPoint.sql");
        const DBIDSet commitIds {2};
        const DBIDSet approvedCommitIds {1, 2};
        testApprove(commitIds, approvedCommitIds);
    }

    {
        const DBIDSet commitIds {5};
        const DBIDSet approvedCommitIds {3, 4, 5};
        testApprove(commitIds, approvedCommitIds);
    }
}

BOOST_AUTO_TEST_CASE(T08_ConcurrentApproveSideEffect)
{
    /*
     * This test demonstrates side-effects
     * of unstable reading from approved branch
     *
     * When Snapshot is created via
     * RevisionsGateway::snapshot(commitId)
     * method, the result of concurrent transaction, which was
     * both created and commited after snapshot creation will be visible
     * from the snapshot
     *
     */
    setTestData("sql/015-ConcurrentApproveWithoutSnapshot.sql");

    pqxx::connection approvedConn(connectionString());
    pqxx::transaction<> approvedTxn(approvedConn);
    BranchManager branchMgr(approvedTxn);
    auto approvedBranch = branchMgr.loadApproved();
    RevisionsGateway approvedGateway(approvedTxn, approvedBranch);
    DBID headCommitId = approvedGateway.headCommitId();
    BOOST_CHECK_EQUAL(headCommitId, 3);

    //creating snapshot
    auto approvedSnapshot = approvedGateway.snapshot(headCommitId);

    BOOST_CHECK_EQUAL(
        approvedSnapshot.revisionIdsByFilter(filters::True()).size(),
        2
    );

    pqxx::connection approverConn(connectionString());
    pqxx::transaction<> approverTxn(approverConn);
    RevisionsGateway approverGateway(approverTxn, approvedBranch);
    CommitManager commitMgr(approverTxn);
    auto prevSnapshotId = approverGateway.maxSnapshotId();
    commitMgr.approve(DBIDSet{1});
    auto currSnapshotId = approverGateway.maxSnapshotId();
    BOOST_CHECK(prevSnapshotId.approveOrder() < currSnapshotId.approveOrder());

    //no side effects are visible to approvedTxn until
    //approverTxn is not commited
    BOOST_CHECK_EQUAL(
        approvedSnapshot.revisionIdsByFilter(filters::True()).size(),
        2
    );
    approverTxn.commit();

    //side effect: committed approverTxn is visible
    //from yet uncommited approvedTxn with ealier start time
    BOOST_CHECK_EQUAL(
        approvedSnapshot.revisionIdsByFilter(filters::True()).size(),
        4
    );
}

BOOST_AUTO_TEST_CASE(T09_ConcurrentApproveWithoutSnapshotNoSideEffect)
{
    /*
     * This test demonstrates that no side effects are visible
     * when
     * RevisionsGateway::stableSnapshot(commitId)
     * and
     * RevisionsGateway::snapshot(SnapshotId)
     * methods are used
     */
    setTestData("sql/015-ConcurrentApproveWithoutSnapshot.sql");

    pqxx::connection approvedConn(connectionString());
    pqxx::transaction<> approvedTxn(approvedConn);
    BranchManager branchMgr(approvedTxn);
    auto approvedBranch = branchMgr.loadApproved();
    RevisionsGateway approvedGateway(approvedTxn, approvedBranch);
    DBID headCommitId = approvedGateway.headCommitId();
    auto maxSnapshotId = approvedGateway.maxSnapshotId();
    BOOST_CHECK_EQUAL(headCommitId, 3);

    //creating snapshot
    auto approvedSnapshot = approvedGateway.stableSnapshot(headCommitId);
    BOOST_CHECK_EQUAL(
         approvedSnapshot.revisionIdsByFilter(filters::True()).size(),
         2
    );

    pqxx::connection approverConn(connectionString());
    pqxx::transaction<> approverTxn(approverConn);
    CommitManager commitMgr(approverTxn);
    commitMgr.approve(DBIDSet{1});
    approverTxn.commit();

    //no side effect when correct approve order is specified
    BOOST_CHECK_EQUAL(
         approvedSnapshot.revisionIdsByFilter(filters::True()).size(),
         2
    );

}

BOOST_AUTO_TEST_CASE(T10_FindDraftContributingCommits)
{
    setTestData("sql/017-ThreeCommits.sql");

    auto revert = [&] (const DBIDSet& commitIds)
    {
        pqxx::work work(getConnection());
        CommitManager committer(work);
        auto revertData = committer.revertCommitsInTrunk(commitIds, TEST_UID, {});
        work.commit();
        return revertData;
    };

    auto revertCommitId = revert({2}).createdCommit.id();
    const DBIDSet oneCommit{revertCommitId};
    const DBIDSet allDraftCommitIds{1,2,3,revertCommitId};

    testFindDraftContributingCommits(oneCommit, {}, oneCommit);
    testFindDraftContributingCommits(oneCommit, {ContributingCommitsOption::Reference}, oneCommit);
    testFindDraftContributingCommits(oneCommit, {ContributingCommitsOption::Revert}, allDraftCommitIds);
    testFindDraftContributingCommits(oneCommit,
        {ContributingCommitsOption::Revert, ContributingCommitsOption::Reference}, allDraftCommitIds);

    {
        pqxx::work work(getConnection());
        CommitManager committer(work);
        auto result = committer.hasDraftContributingCommitsWithoutOriginal(oneCommit);
        BOOST_CHECK(result);
    }

    testApprove(oneCommit, allDraftCommitIds);

    {
        pqxx::work work(getConnection());
        CommitManager committer(work);
        auto result = committer.hasDraftContributingCommitsWithoutOriginal(oneCommit);
        BOOST_CHECK(!result);
    }
}

BOOST_AUTO_TEST_CASE(T11_HasDraftContributingCommitsEmpty)
{
    pqxx::work work(getConnection());
    CommitManager committer(work);
    auto result = committer.hasDraftContributingCommitsWithoutOriginal({});
    BOOST_CHECK(!result); // empty commit set has now draft contributing commits
}

BOOST_AUTO_TEST_CASE(ShouldNotConsiderExcludedCommitsAsDraft)
{
    pqxx::work txn(getConnection());

    const auto revision1 = RevisionCreator(txn)();
    const auto revision2 = RevisionCreator(txn).prevRevId(revision1)();
    const auto revision3 = RevisionCreator(txn).prevRevId(revision2)();

    const DBIDSet commits = {revision3.commitId()};

    CommitManager commitManager(txn);

    {
        BOOST_CHECK(commitManager.hasDraftContributingCommitsWithoutOriginal(commits));
        const auto result = commitManager.findAllDraftContributingCommits(commits);
        const DBIDSet expected = {revision1.commitId(), revision2.commitId(), revision3.commitId()};
        BOOST_CHECK_EQUAL_COLLECTIONS(result.cbegin(), result.cend(), expected.cbegin(), expected.cend());
    }

    {
        const DBIDSet exclude = {revision1.commitId()};
        BOOST_CHECK(commitManager.hasDraftContributingCommitsWithoutOriginal(commits, exclude));
        const auto result = commitManager.findAllDraftContributingCommits(commits, exclude);
        const DBIDSet expected = {revision2.commitId(), revision3.commitId()};
        BOOST_CHECK_EQUAL_COLLECTIONS(result.cbegin(), result.cend(), expected.cbegin(), expected.cend());
    }

    {
        const DBIDSet exclude = {revision1.commitId(), revision2.commitId()};
        BOOST_CHECK(!commitManager.hasDraftContributingCommitsWithoutOriginal(commits, exclude));
        const auto result = commitManager.findAllDraftContributingCommits(commits, exclude);
        const DBIDSet expected = {revision3.commitId()};
        BOOST_CHECK_EQUAL_COLLECTIONS(result.cbegin(), result.cend(), expected.cbegin(), expected.cend());
    }
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace tests
} // namespace revision
} // namespace wiki
} // namespace maps
