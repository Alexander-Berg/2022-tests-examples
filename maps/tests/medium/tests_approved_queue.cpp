#include <maps/wikimap/mapspro/libs/revision_meta/include/approved_queue.h>

#include <maps/wikimap/mapspro/libs/revision/ut/fixtures/revision_creator.h>
#include <maps/wikimap/mapspro/libs/revision_meta/tests/fixtures/fixtures.h>
#include <yandex/maps/wiki/unittest/arcadia.h>

#include <library/cpp/testing/unittest/registar.h>

#include <array>
#include <thread>

using namespace std::chrono_literals;
using maps::wiki::revision::tests::RevisionCreator;

namespace maps::wiki::revision_meta::tests {
namespace {

const TCommitId TEST_COMMIT_ID_1 = 1;
const TCommitId TEST_COMMIT_ID_2 = 2;
const TCommitId TEST_COMMIT_ID_3 = 3;

const TCommitId UNKNOWN_COMMIT_ID = 100500;

const auto ALL_MODES = std::vector<ApprovedQueueMode>{
    ApprovedQueueMode::PreApprove,
    ApprovedQueueMode::ViewAttrs,
    ApprovedQueueMode::Labels,
    ApprovedQueueMode::Bboxes
};

} // namespace

Y_UNIT_TEST_SUITE_F(approved_queue, unittest::ArcadiaDbFixture) {

Y_UNIT_TEST(test_approved_queue)
{
    auto txn = pool().masterWriteableTransaction();

    for (auto mode : ALL_MODES) {
        ApprovedQueue queue(*txn, mode);
        queue.push({TEST_COMMIT_ID_1});
        queue.push({TEST_COMMIT_ID_2, TEST_COMMIT_ID_3});

        UNIT_ASSERT_EQUAL(queue.readCommitIds({UNKNOWN_COMMIT_ID}).size(), 0);

        auto commitIds = queue.readAllCommitIds();
        UNIT_ASSERT_EQUAL(commitIds.size(), 3);
        UNIT_ASSERT_EQUAL(queue.readCommitIds(commitIds).size(), 3);
        for (auto commitId : commitIds) {
            UNIT_ASSERT_EQUAL(queue.readCommitIds({commitId}).size(), 1);
        }
        UNIT_ASSERT_EQUAL(queue.readAllCommitIds().size(), 3);

        UNIT_ASSERT_EQUAL(queue.size(), 3ul);

        queue.deleteCommits(commitIds); // deleted 3 commits
        UNIT_ASSERT_EQUAL(queue.readAllCommitIds().size(), 0);

        queue.deleteCommits(commitIds); // deleting already deleted
        queue.deleteCommits({});

        UNIT_ASSERT_EQUAL(queue.size(), 0ul);
    }
}

} // Y_UNIT_TEST_SUITE_F(approved_queue, unittest::ArcadiaDbFixture)

Y_UNIT_TEST_SUITE_F(pre_approved_queue, unittest::ArcadiaDbFixture) {

Y_UNIT_TEST(test_oldest_commit_age)
{
    auto txn = pool().masterWriteableTransaction();
    const std::array revisionIds = {
        RevisionCreator(*txn)(),
        RevisionCreator(*txn)()
    };

    UNIT_ASSERT_EQUAL(PreApprovedQueue(*txn).oldestCommitAge(), 0s);

    PreApprovedQueue(*txn).push({revisionIds[0].commitId()});

    txn->commit();

    //---

    std::this_thread::sleep_for(2s);

    txn = pool().masterWriteableTransaction();

    UNIT_ASSERT_GE(PreApprovedQueue(*txn).oldestCommitAge(), 2s);

    PreApprovedQueue(*txn).push({revisionIds[1].commitId()});

    txn->commit();

    //---

    std::this_thread::sleep_for(2s);

    txn = pool().masterWriteableTransaction();

    UNIT_ASSERT_GE(PreApprovedQueue(*txn).oldestCommitAge(), 4s);

    PreApprovedQueue(*txn).deleteCommits({revisionIds[0].commitId()});

    UNIT_ASSERT_GE(PreApprovedQueue(*txn).oldestCommitAge(), 2s);

    PreApprovedQueue(*txn).deleteCommits({revisionIds[1].commitId()});

    UNIT_ASSERT_EQUAL(PreApprovedQueue(*txn).oldestCommitAge(), 0s);
}

Y_UNIT_TEST(should_add_no_commits)
{
    auto txn = pool().masterWriteableTransaction();

    PreApprovedQueue preApprovedQueue(*txn);

    preApprovedQueue.push({});
    UNIT_ASSERT_EQUAL(preApprovedQueue.size(), 0);
    UNIT_ASSERT(getAllRelations(*txn).empty());
}


Y_UNIT_TEST(should_add_commits)
{
    auto txn = pool().masterWriteableTransaction();

    PreApprovedQueue preApprovedQueue(*txn);

    auto revisionId1 = RevisionCreator(*txn)();
    auto revisionId2 = RevisionCreator(*txn).prevRevId(revisionId1)();

    preApprovedQueue.push({revisionId1.commitId()});
    UNIT_ASSERT_EQUAL(
        preApprovedQueue.readAllCommitIds(),
        TCommitIds{revisionId1.commitId()}
    );
    UNIT_ASSERT(getAllRelations(*txn).empty());

    preApprovedQueue.push({revisionId2.commitId()});
    UNIT_ASSERT_EQUAL(
        preApprovedQueue.readAllCommitIds(),
        TCommitIds({revisionId1.commitId(), revisionId2.commitId()})
    );
    UNIT_ASSERT_EQUAL(
        getAllRelations(*txn),
        Relations({{revisionId2.commitId(), revisionId1.commitId()}})
    );
}

Y_UNIT_TEST(should_add_relations)
{
    auto txn = pool().masterWriteableTransaction();

    // (0) <- 1 <- (2) <- 3
    // Commits in parentheses are blocking.
    std::vector<revision::RevisionID> revisionIds;
    revisionIds.emplace_back(RevisionCreator(*txn)());                           // 0
    revisionIds.emplace_back(RevisionCreator(*txn).prevRevId(revisionIds[0])()); // 1
    revisionIds.emplace_back(RevisionCreator(*txn).prevRevId(revisionIds[1])()); // 2
    revisionIds.emplace_back(RevisionCreator(*txn).prevRevId(revisionIds[2])()); // 3

    PreApprovedQueue(*txn).push({revisionIds[1].commitId(), revisionIds[3].commitId()});
    UNIT_ASSERT_EQUAL(
        getAllRelations(*txn),
        Relations({
            {revisionIds[1].commitId(), revisionIds[0].commitId()},
            {revisionIds[3].commitId(), revisionIds[0].commitId()},
            {revisionIds[3].commitId(), revisionIds[1].commitId()},
            {revisionIds[3].commitId(), revisionIds[2].commitId()}
        })
    );
}

Y_UNIT_TEST(should_add_relations_separate_chains)
{
    auto txn = pool().masterWriteableTransaction();

    // (0) <- (2) <- 4
    // (1) <- 3 <- 5
    // Commits in parentheses are blocking.
    std::vector<revision::RevisionID> revisionIds;
    revisionIds.emplace_back(RevisionCreator(*txn)());                           // 0
    revisionIds.emplace_back(RevisionCreator(*txn)());                           // 1
    revisionIds.emplace_back(RevisionCreator(*txn).prevRevId(revisionIds[0])()); // 2
    revisionIds.emplace_back(RevisionCreator(*txn).prevRevId(revisionIds[1])()); // 3
    revisionIds.emplace_back(RevisionCreator(*txn).prevRevId(revisionIds[2])()); // 4
    revisionIds.emplace_back(RevisionCreator(*txn).prevRevId(revisionIds[3])()); // 5

    PreApprovedQueue(*txn).push(
        {revisionIds[3].commitId(), revisionIds[4].commitId(), revisionIds[5].commitId()}
    );
    UNIT_ASSERT_EQUAL(
        getAllRelations(*txn),
        Relations({
            {revisionIds[3].commitId(), revisionIds[1].commitId()},
            {revisionIds[4].commitId(), revisionIds[0].commitId()},
            {revisionIds[4].commitId(), revisionIds[2].commitId()},
            {revisionIds[5].commitId(), revisionIds[1].commitId()},
            {revisionIds[5].commitId(), revisionIds[3].commitId()}
        })
    );
}

Y_UNIT_TEST(should_add_relations_for_service_task)
{
    auto txn = pool().masterWriteableTransaction();

    //   (0)
    //    ^
    //    |
    // 1, 2, 3 - commits from the same service task
    //       ^
    //       |
    //       4
    // Commits in parentheses are blocking.
    std::vector<revision::RevisionID> revisionIds;
    revisionIds.emplace_back(RevisionCreator(*txn)());                           // 0
    revisionIds.emplace_back(RevisionCreator(*txn)());                           // 1
    revisionIds.emplace_back(RevisionCreator(*txn).prevRevId(revisionIds[0])()); // 2
    revisionIds.emplace_back(RevisionCreator(*txn)());                           // 3
    revisionIds.emplace_back(RevisionCreator(*txn).prevRevId(revisionIds[3])()); // 4

    PreApprovedQueue queue(*txn);

    queue.pushServiceTaskCommits(
        {revisionIds[1].commitId(), revisionIds[2].commitId(), revisionIds[3].commitId()}
    );
    UNIT_ASSERT_EQUAL(
        getAllRelations(*txn),
        Relations({
            {revisionIds[1].commitId(), revisionIds[2].commitId()},
            {revisionIds[1].commitId(), revisionIds[3].commitId()},
            {revisionIds[2].commitId(), revisionIds[0].commitId()}
        })
    );

    queue.push({revisionIds[4].commitId()});
    UNIT_ASSERT_EQUAL(
        getAllRelations(*txn),
        Relations({
            {revisionIds[1].commitId(), revisionIds[2].commitId()},
            {revisionIds[1].commitId(), revisionIds[3].commitId()},
            {revisionIds[2].commitId(), revisionIds[0].commitId()},
            {revisionIds[4].commitId(), revisionIds[3].commitId()}
        })
    );
}

Y_UNIT_TEST(should_remove_relations)
{
    auto txn = pool().masterWriteableTransaction();

    // (0) <- (1) <- 2 <- 3
    // Commits in parentheses are blocking.
    std::vector<revision::RevisionID> revisionIds;
    revisionIds.emplace_back(RevisionCreator(*txn)());                           // 0
    revisionIds.emplace_back(RevisionCreator(*txn).prevRevId(revisionIds[0])()); // 1
    revisionIds.emplace_back(RevisionCreator(*txn).prevRevId(revisionIds[1])()); // 2
    revisionIds.emplace_back(RevisionCreator(*txn).prevRevId(revisionIds[2])()); // 3

    PreApprovedQueue queue(*txn);

    queue.push({revisionIds[2].commitId(), revisionIds[3].commitId()});
    UNIT_ASSERT_EQUAL(
        getAllRelations(*txn),
        Relations({
            {revisionIds[2].commitId(), revisionIds[0].commitId()},
            {revisionIds[2].commitId(), revisionIds[1].commitId()},
            {revisionIds[3].commitId(), revisionIds[0].commitId()},
            {revisionIds[3].commitId(), revisionIds[1].commitId()},
            {revisionIds[3].commitId(), revisionIds[2].commitId()}
        })
    );

    queue.deleteCommits({revisionIds[0].commitId()});
    UNIT_ASSERT_EQUAL(
        getAllRelations(*txn),
        Relations({
            {revisionIds[2].commitId(), revisionIds[1].commitId()},
            {revisionIds[3].commitId(), revisionIds[1].commitId()},
            {revisionIds[3].commitId(), revisionIds[2].commitId()}
        })
    );

    queue.deleteCommits({revisionIds[1].commitId()});
    UNIT_ASSERT_EQUAL(
        getAllRelations(*txn),
        Relations({
            {revisionIds[3].commitId(), revisionIds[2].commitId()}
        })
    );
}

} // Y_UNIT_TEST_SUITE_F(pre_approved_queue, unittest::ArcadiaDbFixture)

} // namespace maps::wiki::revision_meta::tests
