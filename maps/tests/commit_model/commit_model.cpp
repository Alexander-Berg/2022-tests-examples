#include <maps/wikimap/mapspro/services/editor/src/approved_commits/approver.h>
#include <maps/wikimap/mapspro/services/editor/src/branch_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/commit.h>

#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/tests_common.h>

#include <maps/wikimap/mapspro/libs/revision_meta/include/utils.h>
#include <maps/wikimap/mapspro/libs/acl/include/aclgateway.h>
#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/commit_manager.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>

#include <maps/wikimap/mapspro/libs/revision/ut/fixtures/revision_creator.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

using maps::wiki::revision::tests::RevisionCreator;

namespace maps::wiki::tests {

namespace {

MATCHER_P2(HasCommitState, value, branchCtx, "")
{
    CommitModel commitModel(std::move(arg));
    commitModel.setupState(branchCtx);
    return commitModel.state() == value;
}

} // namespace

Y_UNIT_TEST_SUITE(commit_model) {

WIKI_FIXTURE_TEST_CASE(test_state, EditorTestFixture)
{
    auto branchCtx = BranchContextFacade().acquireWrite();

    // Prepare data
    auto revisionId1 = RevisionCreator(branchCtx.txnCore())();
    auto commitId = RevisionCreator(branchCtx.txnCore()).prevRevId(revisionId1)().commitId();

    acl::ACLGateway aclGateway(branchCtx.txnCore());
    const auto user = aclGateway.user(TESTS_USER);

    revision::BranchManager(branchCtx.txnCore()).createApproved(TESTS_USER, {});

    // Check that commit is draft
    {
        auto commit = revision::Commit::load(branchCtx.txnCore(), commitId);
        EXPECT_EQ(commit.state(), revision::CommitState::Draft);
        EXPECT_THAT(commit, HasCommitState(commit_state::DRAFT, branchCtx));
    }

    // Move the commit to preapprove queue
    {
        approved_commits::CommitsApprover approver(branchCtx, user, {commitId});
        approver.closeTasks(social::CloseResolution::Approve);

        const auto commit = revision::Commit::load(branchCtx.txnCore(), commitId);
        EXPECT_EQ(commit.state(), revision::CommitState::Draft);
        EXPECT_THAT(commit, HasCommitState(commit_state::PREAPPROVING, branchCtx));
    }

    branchCtx.commit();

    // Move the commit to approve queue
    {
        auto coreWriteTxn = cfg()->poolCore().masterWriteableTransaction();

        revision_meta::moveFromPreApprovedToApprovedQueueIfPossible(
            *coreWriteTxn,
            {}
        );

        coreWriteTxn->commit();

        auto readBranchCtx = BranchContextFacade().acquireWrite();
        auto commit = revision::Commit::load(readBranchCtx.txnCore(), commitId);
        EXPECT_EQ(commit.state(), revision::CommitState::Draft);
        EXPECT_THAT(commit, HasCommitState(commit_state::APPROVING, readBranchCtx));
    }

    // Approve the commit
    {
        auto readBranchCtx = BranchContextFacade().acquireWrite();

        revision::CommitManager(readBranchCtx.txnCore()).approve({commitId});

        auto commit = revision::Commit::load(readBranchCtx.txnCore(), commitId);
        EXPECT_EQ(commit.state(), revision::CommitState::Approved);
        EXPECT_THAT(commit, HasCommitState(commit_state::APPROVED, readBranchCtx));
    }
}

} // Y_UNIT_TEST_SUITE

} // namepsace maps::wiki::tests
