#include <maps/wikimap/mapspro/libs/revision_meta/include/utils.h>

#include <maps/wikimap/mapspro/libs/revision_meta/include/approved_queue.h>

#include <maps/wikimap/mapspro/libs/revision/ut/fixtures/revision_creator.h>
#include <yandex/maps/wiki/unittest/arcadia.h>

#include <library/cpp/testing/unittest/registar.h>

using maps::wiki::revision::tests::RevisionCreator;

namespace maps::wiki::revision_meta::tests {

namespace {

const TCommitIds NO_ACTIVE_TASKS;

struct Fixture: public unittest::ArcadiaDbFixture {
    Fixture()
        : txn(pool().masterWriteableTransaction())
        , approvedQueue(*txn, ApprovedQueueMode::ViewAttrs)
        , preApprovedQueue(*txn)
    {}

    pgpool3::TransactionHandle txn;
    ApprovedQueue approvedQueue;
    PreApprovedQueue preApprovedQueue;
};

} // namespace

Y_UNIT_TEST_SUITE_F(utils, Fixture) {

Y_UNIT_TEST(should_not_move_commit_with_active_task) {
    const auto commitId = RevisionCreator(*txn)().commitId();
    preApprovedQueue.push({commitId});

    const TCommitIds activeTasks = {commitId};
    moveFromPreApprovedToApprovedQueueIfPossible(*txn, activeTasks);
    UNIT_ASSERT_EQUAL(preApprovedQueue.readAllCommitIds(), TCommitIds({commitId}));
    UNIT_ASSERT_EQUAL(approvedQueue.size(), 0);
}

Y_UNIT_TEST(should_not_move_commit_that_depends_from_commit_with_active_task) {
    const auto revisionId1 = RevisionCreator(*txn)();
    const auto revisionId2 = RevisionCreator(*txn).prevRevId(revisionId1)();
    preApprovedQueue.push({revisionId2.commitId()});

    const TCommitIds activeTasks = {revisionId1.commitId()};
    moveFromPreApprovedToApprovedQueueIfPossible(*txn, activeTasks);
    UNIT_ASSERT_EQUAL(preApprovedQueue.readAllCommitIds(), TCommitIds({revisionId2.commitId()}));
    UNIT_ASSERT_EQUAL(approvedQueue.size(), 0);
}

Y_UNIT_TEST(should_move_commit_if_there_is_commit_with_active_task_on_top_of_it) {
    const auto revisionId1 = RevisionCreator(*txn)();
    const auto revisionId2 = RevisionCreator(*txn).prevRevId(revisionId1)();
    preApprovedQueue.push({revisionId1.commitId()});

    const TCommitIds activeTasks = {revisionId2.commitId()};
    moveFromPreApprovedToApprovedQueueIfPossible(*txn, activeTasks);
    UNIT_ASSERT_EQUAL(preApprovedQueue.size(), 0);
    UNIT_ASSERT_EQUAL(approvedQueue.readAllCommitIds(), TCommitIds({revisionId1.commitId()}));
}

Y_UNIT_TEST(should_not_move_commit_if_commit_on_top_is_blocked) {
    // 1 <- (2) <- 3
    // 2 is blocked
    const auto revisionId1 = RevisionCreator(*txn)();
    const auto revisionId2 = RevisionCreator(*txn).prevRevId(revisionId1)();
    const auto revisionId3 = RevisionCreator(*txn).prevRevId(revisionId2)();
    preApprovedQueue.push({revisionId1.commitId(), revisionId3.commitId()});

    const TCommitIds activeTasks = {revisionId2.commitId()};
    moveFromPreApprovedToApprovedQueueIfPossible(*txn, activeTasks);
    UNIT_ASSERT_EQUAL(
        preApprovedQueue.readAllCommitIds(),
        TCommitIds({revisionId1.commitId(), revisionId3.commitId()})
    );
    UNIT_ASSERT_EQUAL(approvedQueue.size(), 0);
}

Y_UNIT_TEST(should_not_move_commit_if_commit_on_top_is_blocked_via_service_task) {
    //   1 <- 2
    //        |
    // (3) <- 4
    // 2 and 4 from the same service task, 3 is blocked
    const auto revisionId1 = RevisionCreator(*txn)();
    const auto revisionId2 = RevisionCreator(*txn).prevRevId(revisionId1)();
    const auto revisionId3 = RevisionCreator(*txn)();
    const auto revisionId4 = RevisionCreator(*txn).prevRevId(revisionId3)();
    preApprovedQueue.push({revisionId1.commitId()});
    preApprovedQueue.pushServiceTaskCommits({revisionId2.commitId(), revisionId4.commitId()});

    const TCommitIds activeTasks = {revisionId3.commitId()};
    moveFromPreApprovedToApprovedQueueIfPossible(*txn, activeTasks);
    UNIT_ASSERT_EQUAL(
        preApprovedQueue.readAllCommitIds(),
        TCommitIds({revisionId1.commitId(), revisionId2.commitId(), revisionId4.commitId()})
    );
    UNIT_ASSERT_EQUAL(approvedQueue.size(), 0);
}

Y_UNIT_TEST(should_move_commit_without_active_task) {
    const auto commitId = RevisionCreator(*txn)().commitId();
    preApprovedQueue.push({commitId});

    moveFromPreApprovedToApprovedQueueIfPossible(*txn, NO_ACTIVE_TASKS);
    UNIT_ASSERT_EQUAL(preApprovedQueue.size(), 0);
    UNIT_ASSERT_EQUAL(approvedQueue.readAllCommitIds(), TCommitIds({commitId}));
}

Y_UNIT_TEST(should_move_contributing_commit_without_active_task) {
    const auto revisionId1 = RevisionCreator(*txn)();
    const auto revisionId2 = RevisionCreator(*txn).prevRevId(revisionId1)();
    preApprovedQueue.push({revisionId2.commitId()});

    moveFromPreApprovedToApprovedQueueIfPossible(*txn, NO_ACTIVE_TASKS);
    UNIT_ASSERT_EQUAL(preApprovedQueue.size(), 0);
    UNIT_ASSERT_EQUAL(
        approvedQueue.readAllCommitIds(),
        TCommitIds({revisionId1.commitId(), revisionId2.commitId()})
    );
}

Y_UNIT_TEST(should_move_commits_from_non_blocked_component) {
    // (1) <- 2; 3 <- 4
    // 1 is blocked.
    const auto revisionId1 = RevisionCreator(*txn)();
    const auto revisionId2 = RevisionCreator(*txn).prevRevId(revisionId1)();
    const auto revisionId3 = RevisionCreator(*txn)();
    const auto revisionId4 = RevisionCreator(*txn).prevRevId(revisionId3)();
    preApprovedQueue.push({revisionId2.commitId(), revisionId3.commitId(), revisionId4.commitId()});

    const TCommitIds activeTasks = {revisionId1.commitId()};
    moveFromPreApprovedToApprovedQueueIfPossible(*txn, activeTasks);
    UNIT_ASSERT_EQUAL(
        preApprovedQueue.readAllCommitIds(),
        TCommitIds({revisionId2.commitId()})
    );
    UNIT_ASSERT_EQUAL(
        approvedQueue.readAllCommitIds(),
        TCommitIds({revisionId3.commitId(), revisionId4.commitId()})
    );
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::revision_meta::tests
