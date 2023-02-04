#include "common.h"
#include "helpers.h"
#include "std_optional_printer.h"

#include <maps/wikimap/mapspro/libs/revision/sql_strings.h>
#include <maps/wikimap/mapspro/libs/revision/revisions_gateway_impl.h>
#include <maps/wikimap/mapspro/libs/revision/commit_data.h>

#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/commit_manager.h>
#include <yandex/maps/wiki/revision/exception.h>

#include <boost/test/unit_test.hpp>

#include <boost/lexical_cast.hpp>

#include <sstream>
#include <stdexcept>
#include <memory>

namespace maps {
namespace wiki {
namespace revision {

namespace tests {

namespace {

typedef RevisionsGateway::NewRevisionData NewRevisionData;

const std::string TEST_COMMIT_NOTE_A = "test commit 1";
const std::string TEST_COMMIT_NOTE_EMPTY_VALUE = "test commit (empty value)";
const std::string TEST_COMMIT_NOTE_B = "test second commit";
const std::string TEST_COMMIT_NOTE_4 = "test fix maneuver";
const std::string TEST_COMMIT_NOTE_5 = "test fix maneuver again";
const std::string TEST_COMMIT_NOTE_6 = "test fix draft maneuver";
const std::string TEST_COMMIT_NOTE_C = "test commit draft C";
const std::string TEST_COMMIT_NOTE_D = "test commit stable D (manual merge)";
const std::string TEST_COMMIT_NOTE_E = "test commit stable E (unmerged)";
const std::string TEST_COMMIT_NOTE_E_PLUS = "test commit stable E+ (unmerged)";
const std::string TEST_COMMIT_NOTE_F = "test commit stable F (merged, fix E in another stable branch)";
const std::string TEST_COMMIT_NOTE_CONFLICTED_COMMIT = "new commit with conflict";
const std::string TEST_COMMIT_NOTE_DELETE_ALREADY_DELETED = "bad commit with deleting already deleted data";
const std::string TEST_COMMIT_INSERT_DUPLICATED_OBJECT_ID = "bad commit with inserting already existed object_id";
const std::string TEST_COMMIT_CHECK_CONCURRENT_UPDATE = "concurrent commit with modifying data";
const std::string TEST_COMMIT_FAILED = "some bad commit";
const std::string DESCRIPTION_MANEUVER_1 = "the great maneuver";
const std::string TEST_PATCH_ARCHIVE_BRANCH = "test archive branch patching";

const DBID objectManeuver = 8;
const DBID objectStranger = 18;

const DBID commitA = 1;
const DBID commitB = 2;
const DBID commitRevokedFirstStable = 3;
const DBID commitRevokedDraft = 5;
const DBID commitC = 6;
const DBID commitD = 7;
const DBID commitE = 8;
const DBID commitApprovedNotCommitted = 9;
const DBID commitStableOverConflict = 10;
const DBID commitWithEmptyAttributes = 11;
const DBID commitF = 12;

const DBID TEST_APPROVED_BRANCH_ID = 1;
const DBID TEST_STABLE_BRANCH_ID = 3;
const DBID TEST_PREV_BRANCH_ID = 3;
const DBID TEST_BRANCH_ID = 5;
const DBID TEST_NEXT_BRANCH_ID = 6;

const std::string GEOMETRY_HEX_1 = // LINESTRING(1 1,5 1)
    "010200000002000000000000000000F03F000000000000F03F0000000000001440000000000000F03F";
const std::string GEOMETRY_1 = decodeWKB(GEOMETRY_HEX_1);

const std::string GEOMETRY_HEX_2 = // LINESTRING(3 1,10 1)
    "0102000000020000000000000000000840000000000000F03F0000000000002440000000000000F03F";
const std::string GEOMETRY_2 = decodeWKB(GEOMETRY_HEX_2);

const std::string GEOMETRY_HEX_3 = // [0, 0, 7, 7]
    "010200000002000000000000000000000000000000000000000000000000001C400000000000001C40";

// thegeorg@WARN:
// TEST CORRECTNESS DEPENDS ON THEIR ORDER
// DON'T REORDER TESTS
//

DbFixture& globalFixture()
{
    static DbFixture fixture;
    return fixture;
}

struct DbFixtureProxy
{
    void createSchema() { globalFixture().createSchema(); }

    std::string connectionString() { return globalFixture().connectionString(); }

    pqxx::connection& getConnection() { return globalFixture().getConnection(); }

    void setTestData(const std::string& sqlPath) { globalFixture().setTestData(sqlPath); }
    void setTestData() { globalFixture().setTestData(); }
};

} // namespace

BOOST_FIXTURE_TEST_SUITE(Tests, DbFixtureProxy)

BOOST_AUTO_TEST_CASE(test_connection)
{
    pqxx::work txn(getConnection());

    pqxx::result r = txn.exec("select 123");
    BOOST_CHECK(!r.empty());
    BOOST_CHECK_EQUAL(r[0][0].as<int>(), 123);
}

BOOST_AUTO_TEST_CASE(test_reader_load_empty_id)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gateway(txn); // Trunk

    std::vector<ObjectRevision::ID> vec { ObjectRevision::ID() };

    BOOST_CHECK_THROW(gateway.reader().loadRevisions(vec), maps::Exception); // empty revision id
}

BOOST_AUTO_TEST_CASE(test_gateway_renew_schema_and_acquire_object_ids)
{
    for (size_t pass : { 1, 2, 3 }) {
        createSchema();
        pqxx::work txn(getConnection());
        RevisionsGateway gateway(txn); // Trunk
        if (pass == 1) { // single
            BOOST_CHECK_EQUAL(gateway.acquireObjectId().objectId(), 1);
        }
        else if (pass == 2) { // bulk
            const size_t bigId = 10 * 1000 * 1000;
            auto res = gateway.acquireObjectIds(bigId);
            BOOST_CHECK_EQUAL(res.size(), 1);
            BOOST_REQUIRE(res.size() >= 1);
            BOOST_CHECK_EQUAL(res.front().first, 1);
            BOOST_CHECK_EQUAL(res.front().second, bigId);

            BOOST_CHECK_EQUAL(gateway.acquireObjectId().objectId(), bigId + 1);
        }
    }
}

BOOST_AUTO_TEST_CASE(test_reader_load_nonempty_id)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gateway(txn); // Trunk

    DBID unknownCommitId = 123;
    std::list<ObjectRevision::ID> ids { createID(unknownCommitId, 456) };

    /// can not load object data
    auto reader = gateway.reader();
    BOOST_CHECK_THROW(reader.loadRevisions(ids), maps::Exception);
    BOOST_CHECK(reader.commitRevisionIds(unknownCommitId).empty());
    BOOST_CHECK(reader.commitRevisions(unknownCommitId).empty());
}


const TestCommitData firstCommitData {
    { {STR_DESCRIPTION, TEST_COMMIT_NOTE_A} }, // attributes
    CommitState::Draft,      // state
    commitA,    // id
    TEST_UID,   // createdBy
    true,       // trunk
    0           // stable_branch_id
};

const TestCommitData secondCommitData {
    { {STR_DESCRIPTION, TEST_COMMIT_NOTE_B} }, // attributes
    CommitState::Draft,      // state
    commitB,    // id
    TEST_UID,   // createdBy
    true,       // trunk
    0           // stable_branch_id
};

const TestCommitData thirdCommitData {
    { {STR_DESCRIPTION, TEST_COMMIT_NOTE_C} }, // attributes
    CommitState::Draft,      // state
    commitC,    // id
    TEST_UID,   // createdBy
    true,       // trunk
    0           // stable_branch_id
};

const TestCommitData fourthCommitData {
    { {STR_DESCRIPTION, TEST_COMMIT_NOTE_D} }, // attributes
    CommitState::Approved,   // state
    commitD,    // id
    TEST_UID,   // createdBy
    false,      // trunk (without automerge)
    TEST_BRANCH_ID // stable_branch_id
};

const TestCommitData fourthCommitMergedData {
    { {STR_DESCRIPTION, TEST_COMMIT_NOTE_D} }, // attributes
    CommitState::Approved,   // state
    commitD,    // id
    TEST_UID,   // createdBy
    true,       // trunk (manually merged)
    TEST_BRANCH_ID // stable_branch_id
};

const TestCommitData fifthCommitData {
    { {STR_DESCRIPTION, TEST_COMMIT_NOTE_E} }, // attributes
    CommitState::Approved,   // state
    commitE,    // id
    TEST_UID,   // createdBy
    false,      // trunk
    TEST_BRANCH_ID // stable_branch_id
};

BOOST_AUTO_TEST_CASE(test_gateway_aquire_object_ids)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gateway(txn); // Trunk
    {
        auto res = gateway.acquireObjectIds(17);
        BOOST_CHECK_EQUAL(res.size(), 1);
        BOOST_REQUIRE(res.size() >= 1);
        BOOST_CHECK_EQUAL(res.front().first, 1);
        BOOST_CHECK_EQUAL(res.front().second, 17);
    }
    {
        auto res = gateway.acquireObjectIds(30);
        BOOST_CHECK_EQUAL(res.size(), 1);
        BOOST_REQUIRE(res.size() >= 1);
        BOOST_CHECK_EQUAL(res.front().first, 18);
        BOOST_CHECK_EQUAL(res.front().second, 47);
    }
    {
        auto res = gateway.acquireObjectId();
        BOOST_CHECK_EQUAL(res.objectId(), 48);
        BOOST_CHECK_EQUAL(res.commitId(), 0);
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_first_commit_create)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk

    std::list<NewRevisionData> lst {
        { createID(1), TestData {} },
        { createID(2), TestData {} },
        { createID(3), TestData {} },
        { createID(4), TestData {} },
        { createID(5), TestData {} },
        { createID(6), TestData {} },
        { createID(7), TestData {} },
        { createID(8), TestData { Attributes {{"", ""}, {"abc", ""}}, DESCRIPTION_MANEUVER_1}},
        { createID(9),  TestData { RelationData {1, 4}, Attributes {{"is_special", ""}, {"role", "start_jc"}} }},
        { createID(10), TestData { RelationData {1, 5}, Attributes {{"is_special", ""}, {"role", "end_jc"}} }},
        { createID(11), TestData { RelationData {2, 5}, Attributes {{"is_special", ""}, {"role", "start_jc"}} }},
        { createID(12), TestData { RelationData {2, 6}, Attributes {{"is_special", ""}, {"role", "end_jc"}} }},
        { createID(13), TestData { RelationData {3, 5}, Attributes {{"is_special", ""}, {"role", "start_jc"}} }},
        { createID(14), TestData { RelationData {3, 7}, Attributes {{"is_special", ""}, {"role", "end_jc"}} }},
        // maneuver
        { createID(15), TestData { RelationData {8, 3}, Attributes {{"is_special", ""}, {"role", "start_road"}} }},
        { createID(16), TestData { RelationData {8, 5}, Attributes {{"is_special", ""}, {"role", "via"}} }},
        { createID(17), TestData { RelationData {8, 2}, Attributes {{"is_special", ""}, {"role", "next_road"}} }}
    };

    const std::list<NewRevisionData> refData = lst;

    Attributes attrs {
        {STR_DESCRIPTION, TEST_COMMIT_NOTE_A},
        {TEST_COMMIT_NOTE_EMPTY_VALUE, ""} // must be skipped
    };

    for (const auto& data : lst) {
        BOOST_CHECK(!data.first.commitId());
    }

    Commit commit = gateway.createCommit(lst.begin(), lst.end(), TEST_UID, attrs);

    auto lstIt = lst.begin();
    auto refLstIt = refData.begin();
    for (; lstIt != lst.end(); ++lstIt, ++refLstIt) {
        BOOST_CHECK(lstIt->first == refLstIt->first);
        checkObjectData(lstIt->first, lstIt->second, refLstIt->second);
    }

    checkCommit(commit, firstCommitData);
    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_skip_loading_description_by_default)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk

    auto snapshot = gateway.snapshot(commitA);
    auto rev = snapshot.objectRevision(objectManeuver);
    BOOST_REQUIRE(rev);
    BOOST_CHECK_EQUAL(rev->data().description, NO_DESC);
    BOOST_CHECK(rev->hasDescription());

    snapshot.reader().setDescriptionLoadingMode(DescriptionLoadingMode::Load);

    auto revDescr = snapshot.objectRevision(objectManeuver);
    BOOST_REQUIRE(revDescr);
    BOOST_CHECK_EQUAL(*revDescr->data().description, DESCRIPTION_MANEUVER_1);
    BOOST_CHECK(rev->hasDescription());
}

BOOST_AUTO_TEST_CASE(test_gateway_first_commit_load)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk

    gateway.reader().setDescriptionLoadingMode(DescriptionLoadingMode::Load);

    auto commit = Commit::load(txn, commitA);
    checkCommit(commit, firstCommitData);

    std::map<ObjectRevision::ID, TestRevisionData> correctData {
        { createID(1, commitA), { NO_ID, NO_ID, TestData {} } },
        { createID(2, commitA), { NO_ID, NO_ID, TestData {} } },
        { createID(3, commitA), { NO_ID, NO_ID, TestData {} } },
        { createID(4, commitA), { NO_ID, NO_ID, TestData {} } },
        { createID(5, commitA), { NO_ID, NO_ID, TestData {} } },
        { createID(6, commitA), { NO_ID, NO_ID, TestData {} } },
        { createID(7, commitA), { NO_ID, NO_ID, TestData {} } },
        { createID(8, commitA), { NO_ID, NO_ID, TestData { NO_ATTR, DESCRIPTION_MANEUVER_1} } },
        { createID(9, commitA),  { NO_ID, NO_ID, TestData { RelationData {1, 4}, Attributes {{"role", "start_jc"}} } }},
        { createID(10, commitA), { NO_ID, NO_ID, TestData { RelationData {1, 5}, Attributes {{"role", "end_jc"}} } }},
        { createID(11, commitA), { NO_ID, NO_ID, TestData { RelationData {2, 5}, Attributes {{"role", "start_jc"}} } }},
        { createID(12, commitA), { NO_ID, NO_ID, TestData { RelationData {2, 6}, Attributes {{"role", "end_jc"}} } }},
        { createID(13, commitA), { NO_ID, NO_ID, TestData { RelationData {3, 5}, Attributes {{"role", "start_jc"}} } }},
        { createID(14, commitA), { NO_ID, NO_ID, TestData { RelationData {3, 7}, Attributes {{"role", "end_jc"}} } }},
        // maneuver
        { createID(15, commitA), { NO_ID, NO_ID, TestData { RelationData {8, 3}, Attributes {{"role", "start_road"}} } }},
        { createID(16, commitA), { NO_ID, NO_ID, TestData { RelationData {8, 5}, Attributes {{"role", "via"}} } }},
        { createID(17, commitA), { NO_ID, NO_ID, TestData { RelationData {8, 2}, Attributes {{"role", "next_road"}} } }}
    };

    Revisions revs = gateway.reader().commitRevisions(commitA);
    BOOST_CHECK_EQUAL(revs.size(), correctData.size());
    for (const auto& rev : revs) {
        checkRevision(rev, correctData, gateway);
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_second_commit_create_with_attrs_deduplication_relations_only)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk

    std::list<NewRevisionData> lst {
        { createID(20), TestData { Wkb {GEOMETRY_1} }},
        { createID(21), TestData {} },
        { createID(22), TestData {} },
        { createID(23), TestData { RelationData {20, 22}, Attributes {{"role", "start_jc"}} }},
        { createID(24), TestData { RelationData {20, 21}, Attributes {{"role", "end_jc"}} }}
    };

    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_NOTE_B} };

    Commit commit = gateway.createCommit(lst, TEST_UID, attrs);

    checkCommit(commit, secondCommitData);
    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_second_commit_load)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gateway(txn); // Trunk

    auto commit = Commit::load(txn, commitB);
    checkCommit(commit, secondCommitData);

    Revisions revs = gateway.reader().commitRevisions(commitB);

    std::map<ObjectRevision::ID, TestRevisionData> correctData {
        { createID(20, commitB), { NO_ID, NO_ID, TestData { Wkb {GEOMETRY_1} } } },
        { createID(21, commitB), { NO_ID, NO_ID, TestData {} } },
        { createID(22, commitB), { NO_ID, NO_ID, TestData {} } },
        { createID(23, commitB), { NO_ID, NO_ID, TestData { RelationData {20, 22}, Attributes {{"role", "start_jc"}} } } },
        { createID(24, commitB), { NO_ID, NO_ID, TestData { RelationData {20, 21}, Attributes {{"role", "end_jc"}} } } }
    };

    BOOST_CHECK_EQUAL(revs.size(), correctData.size());
    for (const auto& rev : revs) {
        checkRevision(rev, correctData, gateway);
    }
}

BOOST_AUTO_TEST_CASE(test_reader_load_revisions)
{
    pqxx::work txn(getConnection());

    auto reader = RevisionsGateway(txn).reader();

    auto commit = Commit::load(txn, commitA);
    checkCommit(commit, firstCommitData);

    auto ids1 = reader.commitRevisionIds(commitA);
    BOOST_CHECK_EQUAL(ids1.size(), 17);

    auto ids2 = reader.commitRevisionIds(commitB);
    BOOST_CHECK_EQUAL(ids2.size(), 5);

    RevisionIds ids;
    for (size_t i = 1; i < 1 + ids1.size(); ++i) {
        ids.emplace_back(i, commitA);
    }
    for (size_t i = 20; i < 20 + ids2.size(); ++i) {
        ids.emplace_back(i, commitB);
    }

    Revisions revs = reader.loadRevisions(ids);
    BOOST_CHECK_EQUAL(revs.size(), ids1.size() + ids2.size());

    auto it = revs.begin();
    for (const auto& id : ids1) {
        BOOST_CHECK_EQUAL(id, it->id());
        ++it;
    }
    for (const auto& id : ids2) {
        BOOST_CHECK_EQUAL(id, it->id());
        ++it;
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_invalid_new_deleted_object)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk

    std::list<NewRevisionData> lst {
        { createID(objectStranger), TestData { DELETED } }
    };

    Attributes attrs { {STR_DESCRIPTION, "fake"} };

    // new deleted revision
    BOOST_CHECK_THROW(
        gateway.createCommit(lst, TEST_UID, attrs),
        maps::Exception);
}

BOOST_AUTO_TEST_CASE(test_gateway_third_nested_commit)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk

    DBID fixedObjectId = objectManeuver;
    DBID fixedCommitId = commitA;

    std::list<NewRevisionData> lst {
        { createID(fixedObjectId, fixedCommitId), TestData { DELETED } },
        { createID(objectStranger), TestData {} }
    };

    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_NOTE_4} };

    for (const auto& data : lst) {
        DBID objectId = data.first.objectId();
        BOOST_CHECK_EQUAL(data.first.commitId(), objectId != objectStranger ? fixedCommitId : 0);
    }
    auto snapshotHead = gateway.snapshot(BIG_ID);
    BOOST_CHECK_EQUAL(snapshotHead.objectRevision(fixedObjectId)->id().commitId(), commitA);
    BOOST_CHECK(!snapshotHead.objectRevision(objectStranger));

    Commit commit = gateway.createCommit(lst, TEST_UID, attrs);

    auto lastRevManeuver = snapshotHead.objectRevision(fixedObjectId);
    BOOST_CHECK_EQUAL(lastRevManeuver->id().commitId(), commit.id());
    BOOST_CHECK_EQUAL(lastRevManeuver->data().deleted, true);

    auto lastRevStranger = snapshotHead.objectRevision(objectStranger);
    BOOST_CHECK_EQUAL(lastRevStranger->id().commitId(), commit.id());
    BOOST_CHECK_EQUAL(lastRevStranger->data().deleted, false);

    auto snapshotA = gateway.snapshot(commitA);
    auto snapshotB = gateway.snapshot(commitB);

    BOOST_CHECK_EQUAL(snapshotA.objectRevision(fixedObjectId)->id().commitId(), commitA);
    BOOST_CHECK_EQUAL(snapshotB.objectRevision(fixedObjectId)->id().commitId(), commitA);

    BOOST_CHECK(!snapshotA.objectRevision(objectStranger));
    BOOST_CHECK(!snapshotB.objectRevision(objectStranger));

    BOOST_CHECK_EQUAL(commit.state(), CommitState::Draft);
    BOOST_CHECK_EQUAL(commit.id(), commitRevokedFirstStable); // 3

    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_snapshot)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gateway(txn); // Trunk

    auto emptySnapshot = gateway.snapshot(0);
    BOOST_CHECK_EQUAL(emptySnapshot.revisionIdsByFilter(filters::True()).size(), 0);

    BOOST_CHECK_EQUAL(gateway.headCommitId(), 3);

    //std::pair - objects count + relations count
    const std::pair<DBID, DBID> data[] = { {8, 9}, {11, 11}, {12, 11} };

    for (DBID maxCommitId : DBIDSet{1, 2, 3}) {
        auto counts = data[maxCommitId - 1];

        auto snapshot = gateway.snapshot(maxCommitId);
        BOOST_CHECK_EQUAL(snapshot.reader().branchId(), TRUNK_BRANCH_ID);
        BOOST_CHECK_EQUAL(snapshot.maxCommitId(), maxCommitId);

        BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filters::ObjRevAttr::isNotRelation()).size(), counts.first);
        BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filters::ObjRevAttr::isRelation()).size(), counts.second);
        BOOST_CHECK(snapshot.revisionIdsByFilter(filters::False()).empty());

        BOOST_CHECK_EQUAL(snapshot.objectRevisionsByFilter(filters::ObjRevAttr::isNotRelation()).size(), counts.first);
        BOOST_CHECK_EQUAL(snapshot.objectRevisionsByFilter(filters::ObjRevAttr::isRelation()).size(), counts.second);
        BOOST_CHECK(snapshot.objectRevisionsByFilter(filters::False()).empty());

        BOOST_CHECK_EQUAL(snapshot.relationsByFilter(filters::True()).size(), counts.second);
        BOOST_CHECK(snapshot.relationsByFilter(filters::False()).empty());
        BOOST_CHECK_EQUAL(snapshot.relationsByFilter(filters::ObjRevAttr::masterObjectId() == objectManeuver).size(), 3);
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_historical_snapshot)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gateway(txn); // Trunk
    BOOST_CHECK_THROW(gateway.historicalSnapshot(1, 0), maps::Exception); // invalid interval

    BOOST_CHECK_EQUAL(gateway.headCommitId(), 3);

    auto emptySnapshot = gateway.historicalSnapshot(0, 0);
    BOOST_CHECK_EQUAL(emptySnapshot.minCommitId(), 0);
    BOOST_CHECK_EQUAL(emptySnapshot.maxCommitId(), 0);
    BOOST_CHECK_EQUAL(emptySnapshot.revisionIdsByFilter(filters::True()).size(), 0);

    const std::pair<DBID, DBID> data[] = { {13, 11}, {5, 2}, {2, 0} };

    for (DBID minCommitId : {1, 2, 3}) {
        auto counts = data[minCommitId - 1];

        auto snapshot = gateway.historicalSnapshot(minCommitId, 3);
        BOOST_CHECK_EQUAL(snapshot.reader().branchId(), TRUNK_BRANCH_ID);
        BOOST_CHECK_EQUAL(snapshot.minCommitId(), minCommitId);
        BOOST_CHECK_EQUAL(snapshot.maxCommitId(), 3);

        BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filters::ObjRevAttr::isNotRelation()).size(), counts.first);
        BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filters::ObjRevAttr::isRelation()).size(), counts.second);

        BOOST_CHECK_EQUAL(snapshot.relationsByFilter(filters::True()).size(), counts.second);
        BOOST_CHECK_EQUAL(snapshot.relationsByFilter(filters::ObjRevAttr::masterObjectId() == objectManeuver).size(),
                          minCommitId == 1 ? 3 : 0);
    }

    auto emptySnapshot2 = gateway.historicalSnapshot(4, BIG_ID);
    BOOST_CHECK_EQUAL(emptySnapshot2.revisionIdsByFilter(filters::True()).size(), 0);

    for (DBID maxCommitId : {1, 2, 3, 4}) {
        auto snapshot = gateway.historicalSnapshot(maxCommitId);
        BOOST_CHECK_EQUAL(snapshot.minCommitId(), 0);
        BOOST_CHECK_EQUAL(snapshot.maxCommitId(), maxCommitId);

        BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filters::ObjRevAttr::objectId() == objectManeuver).size(),
                          maxCommitId >= 3 ? 2 : 1);
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_check_conflicted_commit)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk

    std::list<NewRevisionData> lst {
        { createID(objectManeuver, commitA), TestData {} }
    };

    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_NOTE_CONFLICTED_COMMIT} };

    auto snapshotHead = gateway.snapshot(BIG_ID);
    BOOST_CHECK_EQUAL(snapshotHead.objectRevision(objectManeuver)->id().commitId(), commitRevokedFirstStable); // 3
    BOOST_CHECK_THROW(gateway.createCommit(lst, TEST_UID, attrs), ConflictsFoundException);
}

BOOST_AUTO_TEST_CASE(test_gateway_approve_check_fail)
{
    {
        pqxx::work txn(getConnection());
        CommitManager commitMgr(txn);
        BOOST_CHECK_THROW(commitMgr.approve(DBIDSet{0}), maps::Exception); // invalid commit, not abort txn
        BOOST_CHECK_THROW(commitMgr.approve(DBIDSet{5}), maps::Exception); // unknown commit, abort txn
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_approve_two_commits_12)
{
    pqxx::work txn(getConnection());

    CommitManager commitMgr(txn);

    std::list<DBID> commits {1, 2};

    BOOST_CHECK_EQUAL(commitMgr.approve(commits).size(), commits.size()); // done
    BOOST_CHECK_EQUAL(commitMgr.approve(commits).size(), 0); // skipped
    //txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_approve_two_commits_13)
{
    pqxx::work txn(getConnection());

    DBIDSet commits {1, 3};

    BOOST_CHECK_EQUAL(CommitManager(txn).approve(commits).size(), commits.size());

    for (auto commitId : commits) {
        auto commit = Commit::load(txn, commitId);
        BOOST_CHECK_MESSAGE(commit.approveOrder() > 0,
                            "Approved commit " << commitId
                            << " must have approve order, but it doesn't");
    }
    //txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_approve_three_commits)
{
    pqxx::work txn(getConnection());

    CommitManager commitMgr(txn);

    BOOST_CHECK_EQUAL(commitMgr.approve( DBIDSet { 1 } ).size(), 1);

    DBIDSet commits { 1, 2, 3 };
    BOOST_CHECK_EQUAL(commitMgr.approve(commits).size(), 2); // 1 - skip, 2,3 - done

    BOOST_CHECK_EQUAL(commitMgr.approve(commits).size(), 0); // skipped
    //txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_create_approved)
{
    pqxx::work txn(getConnection());

    auto branch = BranchManager(txn).createApproved(TEST_UID, {});
    BOOST_CHECK_EQUAL(branch.id(), TEST_APPROVED_BRANCH_ID);
    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_create_approved_again)
{
    pqxx::work txn(getConnection());

    BranchManager branchManager(txn);
    BOOST_CHECK_THROW(branchManager.createApproved(TEST_UID, {}), ApprovedBranchAlreadyExistsException);
}

BOOST_AUTO_TEST_CASE(test_gateway_approve_all_commits_bulk)
{
    pqxx::work txn(getConnection());

    CommitManager commitMgr(txn);

    const size_t TRUNK_COMMITS_SIZE = 3;
    const size_t EXPECTED_APPROVE_ORDER = 5;

    RevisionsGateway trunkGateway(txn);
    BOOST_CHECK_EQUAL(
        trunkGateway.headCommitId(),
        TRUNK_COMMITS_SIZE
    );

    RevisionsGateway approvedGateway(txn, BranchManager(txn).loadApproved());
    auto approvedSnapshotId = approvedGateway.maxSnapshotId();
    BOOST_CHECK_EQUAL(approvedSnapshotId.approveOrder(), 0);

    BOOST_CHECK_EQUAL(commitMgr.approveAll(TRUNK_COMMITS_SIZE).size(), TRUNK_COMMITS_SIZE);
    BOOST_CHECK_EQUAL(commitMgr.approveAll(TRUNK_COMMITS_SIZE).size(), 0); // all draft commits already approved

    approvedSnapshotId = approvedGateway.maxSnapshotId();
    BOOST_CHECK_EQUAL(
        approvedSnapshotId.approveOrder(),
        EXPECTED_APPROVE_ORDER
    );
    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_check_approved_three_commits)
{
    pqxx::work txn(getConnection());

    for (DBID commitId = 1; commitId <= 3; ++commitId) {
        auto commit = Commit::load(txn, commitId);
        BOOST_CHECK_EQUAL(commit.state(), CommitState::Approved);
    }

    for (auto branchType : {BranchType::Trunk, BranchType::Approved}) {
        auto branchTypeStr = boost::lexical_cast<std::string>(branchType);
        RevisionsGateway gateway(txn, BranchManager(txn).loadByString(branchTypeStr));

        for (DBID commitId = 1; commitId <= 3; ++commitId) {
            ObjectRevision::ID id = createID(objectManeuver, commitId);
            if (commitId == 2) { // can not load object data
                BOOST_CHECK_THROW(gateway.reader().loadRevision(id), maps::Exception);
                continue;
            }

            ObjectRevision rev = gateway.reader().loadRevision(id);
            auto nextCommitId = rev.revisionData().nextCommitId;
            if (nextCommitId) {
                auto nextCommit = Commit::load(txn, nextCommitId);
                BOOST_CHECK_EQUAL(nextCommit.state(), commitId == 1 ? CommitState::Approved : CommitState::Draft);
            }
            BOOST_CHECK_EQUAL(rev.revisionData().prevCommitId, commitId == 3 ?  1 : 0);
        }
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_nested_commit_again)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk

    DBID fixedObjectId = objectManeuver;
    DBID fixedCommitId = 3;

    std::list<NewRevisionData> lst {
        { createID(fixedObjectId, fixedCommitId), TestData {} }
    };

    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_NOTE_5} };

    for (const auto& data : lst) {
        BOOST_CHECK_EQUAL(data.first.commitId(), fixedCommitId);
    }
    auto snapshotHead = gateway.snapshot(BIG_ID);
    BOOST_CHECK_EQUAL(snapshotHead.objectRevision(fixedObjectId)->id().commitId(), fixedCommitId);

    Commit commit = gateway.createCommit(lst, TEST_UID, attrs);

    for (const auto& data : lst) {
        BOOST_CHECK_EQUAL(data.first.commitId(), fixedCommitId);
    }
    BOOST_CHECK_EQUAL(snapshotHead.objectRevision(fixedObjectId)->id().commitId(), commit.id());

    BOOST_CHECK_EQUAL(commit.state(), CommitState::Draft);
    BOOST_CHECK_EQUAL(commit.id(), 4);

    auto reader = gateway.reader();
    ObjectRevision revOld = reader.loadRevision(createID(fixedObjectId, fixedCommitId));
    auto nextOldCommitId = revOld.revisionData().nextCommitId;
    BOOST_CHECK_EQUAL(nextOldCommitId, commit.id());
    if (nextOldCommitId) {
        auto nextCommit = Commit::load(txn, nextOldCommitId);
        BOOST_CHECK_EQUAL(nextCommit.state(), CommitState::Draft);
    }

    ObjectRevision revNew = reader.loadRevision(createID(fixedObjectId, commit.id()));
    BOOST_CHECK_EQUAL(revNew.revisionData().nextCommitId, 0);
    BOOST_CHECK_EQUAL(revNew.revisionData().prevCommitId, fixedCommitId);

    txn.commit();
}


BOOST_AUTO_TEST_CASE(test_gateway_append_draft_commit)
{
    setTestData("sql/002-GatewayAppendDraftCommit.sql");
    pqxx::work txn(getConnection());

    RevisionsGateway gateway(txn); // Trunk

    auto snapshotHead = gateway.snapshot(BIG_ID);
    auto maneuverID = snapshotHead.objectRevision(objectManeuver)->id();

    std::list<NewRevisionData> lst {
        { createID(19), TestData { RelationData {8, 20}, Attributes {{"role", "next_road"}} }}
    };
    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_NOTE_6 + " (relation)"} };

    Commit commit = gateway.createCommit(lst, TEST_UID, attrs);
    BOOST_CHECK_EQUAL(commit.state(), CommitState::Draft);
    BOOST_CHECK_EQUAL(maneuverID.commitId(), commitRevokedFirstStable); // 3
    BOOST_CHECK_EQUAL(commit.id(), commitRevokedDraft); // 5

    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_branch_manager_unexisted_stable)
{
    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);
    BOOST_CHECK_EQUAL(branchManager.load( { { BranchType::Stable, 1 } } ).size(), 0);

    BOOST_CHECK_THROW(branchManager.loadStable(), BranchNotExistsException);
}

BOOST_AUTO_TEST_CASE(test_branch_manager_get_branches_only_trunk)
{
    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);
    auto branches = branchManager.load(
        { { BranchType::Trunk, 1 },
          { BranchType::Stable, 1 },
          { BranchType::Archive, 1 } });
    BOOST_REQUIRE(!branches.empty());
    BOOST_CHECK_EQUAL(branches.size(), 1);
    BOOST_CHECK_EQUAL(branches.front().type(), BranchType::Trunk);
    BOOST_CHECK_EQUAL(branches.front().id(), TRUNK_BRANCH_ID);
}

BOOST_AUTO_TEST_CASE(test_check_forbidden_upgrade_approved_branch_to_stable)
{
    pqxx::work txn(getConnection());

    BranchManager branchManager(txn);

    Branch branchApproved = branchManager.loadApproved();
    BOOST_CHECK_EQUAL(branchApproved.id(), TEST_APPROVED_BRANCH_ID);
    BOOST_CHECK_THROW(branchApproved.setType(txn, BranchType::Stable), BranchForbiddenOperationException);
}

BOOST_AUTO_TEST_CASE(test_touch_approved_and_copy_ctor)
{
    pqxx::work txn(getConnection());

    BranchManager branchManager(txn);

    Branch branchApproved = branchManager.loadApproved();
    BOOST_CHECK_EQUAL(branchApproved.id(), TEST_APPROVED_BRANCH_ID);
    BOOST_CHECK(branchApproved.touchCreated(txn, TEST_UID));

    Branch branchApprovedCopy = branchApproved; // copy ctor
    BOOST_CHECK_EQUAL(branchApproved.id(), branchApprovedCopy.id());

    branchApprovedCopy = branchManager.loadApproved(); // op =
    BOOST_CHECK_EQUAL(branchApproved.id(), branchApprovedCopy.id());

    BOOST_CHECK(branchApproved.touchCreated(txn, TEST_UID));
    //txn.commit();
}

BOOST_AUTO_TEST_CASE(test_branch_manager_create_stable)
{
    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);
    Branch branch = branchManager.createStable(TEST_UID, {});
    BOOST_CHECK_EQUAL(branch.id(), TEST_STABLE_BRANCH_ID);
    BOOST_CHECK_EQUAL(branchManager.loadStable().id(), TEST_STABLE_BRANCH_ID);
    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_branch_manager_get_branches_trunk_stable)
{
    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);
    auto branches = branchManager.load(
        { { BranchType::Trunk, 1 },
          { BranchType::Approved, 1 },
          { BranchType::Stable, 1 },
          { BranchType::Archive, 1 } });
    BOOST_REQUIRE(!branches.empty());
    BOOST_CHECK_EQUAL(branches.size(), 3);
    BOOST_CHECK_EQUAL(branches.front().type(), BranchType::Trunk);
    BOOST_CHECK_EQUAL(branches.front().id(), TRUNK_BRANCH_ID);
    auto it = branches.begin();
    ++it;
    BOOST_CHECK_EQUAL(it->type(), BranchType::Approved);
    BOOST_CHECK_EQUAL(it->id(), TEST_APPROVED_BRANCH_ID);
    BOOST_CHECK_EQUAL(branches.back().type(), BranchType::Stable);
    BOOST_CHECK_EQUAL(branches.back().id(), TEST_STABLE_BRANCH_ID);
}

BOOST_AUTO_TEST_CASE(test_gateway_insert_into_non_normal_stable_branch)
{
    pqxx::work txn(getConnection());

    BranchManager branchManager(txn);
    auto branch = branchManager.loadStable();
    BOOST_CHECK_EQUAL(branch.id(), TEST_PREV_BRANCH_ID);
    BOOST_CHECK_EQUAL(branch.state(), BranchState::Unavailable);
    BOOST_CHECK_EQUAL(branch.isReadingAllowed(), false); // for view
    BOOST_CHECK_EQUAL(branch.isWritingAllowed(), false); // uncommitable mode

    RevisionsGateway gateway(txn, branch);

    std::list<NewRevisionData> lst { { createID(BIG_ID), TestData {} } };
    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_FAILED} };

    BOOST_CHECK_THROW(gateway.createCommit(lst, TEST_UID, attrs), BranchUnavailableException);

    BOOST_CHECK(branch.setState(txn, BranchState::Progress));
    BOOST_CHECK_THROW(gateway.createCommit(lst, TEST_UID, attrs), BranchInProgressException);
}

BOOST_AUTO_TEST_CASE(test_branch_manager_create_stable_dublicate_opened_branch)
{
    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);
    BOOST_CHECK_EQUAL(branchManager.loadStable().id(), TEST_PREV_BRANCH_ID);
    BOOST_CHECK_THROW(branchManager.createStable(TEST_UID, {}), StableBranchAlreadyExistsException);
}

BOOST_AUTO_TEST_CASE(test_bulk_load_commit_by_filter)
{
    using namespace filters;

    pqxx::work txn(getConnection());

    BOOST_CHECK_EQUAL(Commit::load(txn, 1).state(), CommitState::Approved);
    BOOST_CHECK_EQUAL(Commit::load(txn, 2).state(), CommitState::Approved);
    BOOST_CHECK_EQUAL(Commit::load(txn, 3).state(), CommitState::Approved);
    BOOST_CHECK_EQUAL(Commit::load(txn, 5).state(), CommitState::Draft);

    for (DBID id : {3, 4, 5}) {
        auto commits = Commit::load(txn, CommitAttr::id() <= id);
        for (const auto& commit : commits) {
            BOOST_CHECK(commit.id() <= id);
        }
    }

    auto commitsDraft = Commit::load(
        txn, filters::CommitAttr::id() >= 2 && CommitAttr::isDraft());
    BOOST_CHECK_EQUAL(commitsDraft.size(), 1);
    for (const auto& commit : commitsDraft) {
        BOOST_CHECK_EQUAL(commit.id(), 5);
        BOOST_CHECK_EQUAL(commit.state(), CommitState::Draft);
    }

    auto commitsApproved = Commit::load(
        txn, filters::CommitAttr::id() >= 2 && CommitAttr::isApproved());
    BOOST_CHECK_EQUAL(commitsApproved.size(), 2);
    for (const auto& commit : commitsApproved) {
        BOOST_CHECK(commit.id() == 2 || commit.id() == 3);
        BOOST_CHECK_EQUAL(commit.state(), CommitState::Approved);
    }
}

BOOST_AUTO_TEST_CASE(test_branch_manager_merge_approved_to_stable)
{
    size_t approved = 3;
    DBID commitIdAll = 0;
    {
        pqxx::work txn(getConnection());
        RevisionsGateway gateway(txn); // Trunk
        commitIdAll = gateway.headCommitId();

        BranchManager branchManager(txn);
        auto branch = branchManager.loadStable();
        BOOST_CHECK_EQUAL(branch.isReadingAllowed(), false); // for view
        BOOST_CHECK_EQUAL(branch.isWritingAllowed(), false); // uncommitable mode, allow merge approved commits

        CommitManager commitMgr(txn);
        BOOST_CHECK_EQUAL(commitMgr.mergeApprovedToStable(commitIdAll).size(), approved);
    }

    for (size_t i = 1; i <= approved; ++i) {
        pqxx::work txn(getConnection());
        CommitManager commitMgr(txn);
        BOOST_CHECK_EQUAL(commitMgr.mergeApprovedToStable(i).size(), i);

        size_t count = 0;
        for (const DBID id: {1, 2, 3, 5}) {
            Commit commit = Commit::load(txn, id);
            BOOST_CHECK(commit.inTrunk());
            if (id <= 3) {
                BOOST_CHECK_EQUAL(commit.state(), CommitState::Approved);
                count += commit.inStable() ? 1 : 0;
            }
            else {
                BOOST_CHECK(!commit.inStable());
            }
        }
        BOOST_CHECK_EQUAL(count, i);
    }

    pqxx::work txn(getConnection());
    CommitManager commitMgr(txn);
    BOOST_CHECK_EQUAL(commitMgr.mergeApprovedToStable(commitIdAll).size(), approved);
    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_branch_manager_merge_approved_to_stable_in_progress_mode)
{
    pqxx::work txn(getConnection());

    CommitManager commitMgr(txn);
    BOOST_CHECK_EQUAL(commitMgr.mergeApprovedToStable(BIG_ID).size(), 0); // all commits moved

    Branch branch = BranchManager(txn).loadStable();
    BOOST_CHECK(branch.setState(txn, BranchState::Progress));

    BOOST_CHECK_THROW(commitMgr.mergeApprovedToStable(BIG_ID), BranchInProgressException);
}

BOOST_AUTO_TEST_CASE(test_branch_manager_close_stable)
{
    {
        pqxx::work txn(getConnection());
        BranchManager branchManager(txn);
        Branch branch = branchManager.loadStable();
        BOOST_CHECK_EQUAL(branch.id(), TEST_STABLE_BRANCH_ID);
        BOOST_CHECK_EQUAL(branch.finish(txn, TEST_UID), true);
        BOOST_CHECK_EQUAL(branch.type(), BranchType::Archive);
        txn.commit();
    }
    {
        pqxx::work txn(getConnection());
        BranchManager branchManager(txn);
        Branch branch = branchManager.load(TEST_STABLE_BRANCH_ID);
        BOOST_CHECK_THROW(branch.finish(txn, TEST_UID), StableBranchAlreadyFinishedException);
    }
}

BOOST_AUTO_TEST_CASE(test_branch_manager_create_empty_stable)
{
    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);

    BOOST_CHECK_THROW(branchManager.loadStable(), BranchNotExistsException); // txn not aborted

    BOOST_CHECK_EQUAL(branchManager.createStable(TEST_UID, {}).id(), TEST_BRANCH_ID);
    BOOST_CHECK_EQUAL(branchManager.loadStable().id(), TEST_BRANCH_ID);
    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_branch_manager_load_by_string)
{
    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);

    BOOST_CHECK_EQUAL(branchManager.loadByString(sql::val::BRANCH_TYPE_TRUNK).id(), TRUNK_BRANCH_ID);
    BOOST_CHECK_EQUAL(branchManager.loadByString(sql::val::BRANCH_TYPE_STABLE).id(), TEST_BRANCH_ID);

    auto trunkIdStr = boost::lexical_cast<std::string>(TRUNK_BRANCH_ID);
    BOOST_CHECK_EQUAL(branchManager.loadByString(trunkIdStr).id(), TRUNK_BRANCH_ID);

    auto stableIdStr = boost::lexical_cast<std::string>(TEST_BRANCH_ID);
    BOOST_CHECK_EQUAL(branchManager.loadByString(stableIdStr).id(), TEST_BRANCH_ID);
}

BOOST_AUTO_TEST_CASE(test_branch_manager_load_by_empty_string)
{
    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);

    BOOST_CHECK_THROW(branchManager.loadByString(""), maps::RuntimeError);
}

BOOST_AUTO_TEST_CASE(test_branch_manager_load_by_string_type)
{
    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);

    auto trunkStr = boost::lexical_cast<std::string>(BranchType::Trunk);
    BOOST_CHECK_EQUAL(branchManager.loadByString(trunkStr).id(), TRUNK_BRANCH_ID);

    auto approvedStr = boost::lexical_cast<std::string>(BranchType::Approved);
    BOOST_CHECK_EQUAL(branchManager.loadByString(approvedStr).id(), TEST_APPROVED_BRANCH_ID);

    auto stableStr = boost::lexical_cast<std::string>(BranchType::Stable);
    BOOST_CHECK_EQUAL(branchManager.loadByString(stableStr).id(), TEST_BRANCH_ID);

    auto archiveStr = boost::lexical_cast<std::string>(BranchType::Archive);
    BOOST_CHECK_THROW(branchManager.loadByString(archiveStr), maps::RuntimeError);

    auto deletedStr = boost::lexical_cast<std::string>(BranchType::Deleted);
    BOOST_CHECK_THROW(branchManager.loadByString(deletedStr), maps::RuntimeError);
}

BOOST_AUTO_TEST_CASE(test_branch_manager_get_branches_trunk_stable_archive)
{
    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);
    auto branches = branchManager.load(
        { { BranchType::Trunk, 1 },
          { BranchType::Stable, 1 },
          { BranchType::Approved, 1 },
          { BranchType::Archive, 1 } });
    BOOST_REQUIRE(!branches.empty());
    BOOST_CHECK_EQUAL(branches.size(), 4);
    BOOST_CHECK_EQUAL(branches.front().type(), BranchType::Trunk);
    BOOST_CHECK_EQUAL(branches.front().id(), TRUNK_BRANCH_ID);
    auto it = branches.begin();
    ++it;
    BOOST_CHECK_EQUAL(it->type(), BranchType::Approved);
    BOOST_CHECK_EQUAL(it->id(), TEST_APPROVED_BRANCH_ID);
    ++it;
    BOOST_CHECK_EQUAL(it->type(), BranchType::Stable);
    BOOST_CHECK_EQUAL(it->id(), TEST_BRANCH_ID);
    BOOST_CHECK_EQUAL(branches.back().type(), BranchType::Archive);
    BOOST_CHECK_EQUAL(branches.back().id(), TEST_STABLE_BRANCH_ID);
    BOOST_CHECK_EQUAL(branches.back().id(), TEST_PREV_BRANCH_ID);
}

BOOST_AUTO_TEST_CASE(test_gateway_check_conflicts)
{
    setTestData("sql/003-GatewayCheckConflicts.sql");
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk

    std::set<ObjectRevision::ID> ids;
    gateway.checkConflicts(ids); // check empty

    ids.insert(createID(1)); // invalid revision id, only updates;
    BOOST_CHECK_THROW(gateway.checkConflicts(ids), maps::Exception);

    ids.clear();
    ids.insert(createID(BIG_ID, BIG_ID)); // unexisted id
    BOOST_CHECK_THROW(gateway.checkConflicts(ids), maps::Exception);
}

BOOST_AUTO_TEST_CASE(test_gateway_create_commit_c)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk

    ObjectRevision::ID rev1A = createID(1, commitA);
    ObjectRevision::ID rev9A = createID(9, commitA);
    std::list<NewRevisionData> lst {
        { rev1A, TestData {} },
        { rev9A, TestData { DELETED } },
        { createID(30), TestData {} },
        { createID(31), TestData {} },
        { createID(32), TestData {} },
        { createID(33), TestData {} },
        { createID(34), TestData { RelationData {1, 32} } },
        { createID(35), TestData { RelationData {33, 4} } },
        { createID(36), TestData { RelationData {33, 32} } },
        { createID(37), TestData { RelationData {30, 32} } },
        { createID(38), TestData { RelationData {30, 31} } }
    };

    std::set<ObjectRevision::ID> ids { rev1A, rev9A };

    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_NOTE_C} };

    gateway.checkConflicts(ids);

    Commit commit = gateway.createCommit(lst, TEST_UID, attrs);

    BOOST_CHECK_THROW(gateway.checkConflicts(ids), ConflictsFoundException);

    checkCommit(commit, thirdCommitData);
    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_load_commit_c)
{
    pqxx::work txn(getConnection());

    auto commit = Commit::load(txn, commitC);
    checkCommit(commit, thirdCommitData);

    std::map<ObjectRevision::ID, TestRevisionData> correctData {
        { createID(1, commitC), { createID(1, commitA), NO_ID, TestData {} } },
        { createID(9, commitC), { createID(9, commitA), NO_ID, TestData {
            DELETED, Attributes {{"role", "start_jc"}}, NO_DESC, NO_GEOM, RelationData {1, 4} } } },
        { createID(30, commitC), { NO_ID, NO_ID, TestData {} } },
        { createID(31, commitC), { NO_ID, NO_ID, TestData {} } },
        { createID(32, commitC), { NO_ID, NO_ID, TestData {} } },
        { createID(33, commitC), { NO_ID, NO_ID, TestData {} } },
        { createID(34, commitC), { NO_ID, NO_ID, TestData { RelationData {1, 32} } } },
        { createID(35, commitC), { NO_ID, NO_ID, TestData { RelationData {33, 4} } } },
        { createID(36, commitC), { NO_ID, NO_ID, TestData { RelationData {33, 32} } } },
        { createID(37, commitC), { NO_ID, NO_ID, TestData { RelationData {30, 32} } } },
        { createID(38, commitC), { NO_ID, NO_ID, TestData { RelationData {30, 31} } } }
    };

    RevisionsGateway gateway(txn); // Trunk
    auto revs = gateway.reader().commitRevisions(commitC);

    BOOST_CHECK_EQUAL(revs.size(), correctData.size());

    for (const auto& rev : revs) {
        checkRevision(rev, correctData, gateway);
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_create_commit_d_with_attrs_deduplication_objects_only)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn, BranchManager(txn).loadStable());

    ObjectRevision::ID rev_20_B = createID(20, commitB);
    ObjectRevision::ID rev_21_B = createID(21, commitB);
    ObjectRevision::ID rev_24_B = createID(24, commitB);

    std::list<NewRevisionData> lst {
        { rev_20_B, TestData { Wkb { GEOMETRY_2 } } },
        { rev_21_B, TestData { DELETED } },
        { rev_24_B, TestData { DELETED } },
        { createID(39), TestData { RelationData {20, 5}, Attributes {{"role", "end_jc"}} } }
    };

    std::set<ObjectRevision::ID> ids { rev_20_B, rev_21_B, rev_24_B };

    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_NOTE_D} };

    const DBID testId = 5;

    std::map<ObjectRevision::ID, RelationData> correctMasterRelations {
        { createID(10, commitA), RelationData {1, testId} },
        { createID(11, commitA), RelationData {2, testId} },
        { createID(13, commitA), RelationData {3, testId} },
        { createID(16, commitA), RelationData {8, testId} },
    };

    auto snapshotC = gateway.snapshot(commitC);
    checkRelations(snapshotC.loadMasterRelations(testId), correctMasterRelations);

    gateway.checkConflicts(ids);

    BOOST_CHECK_THROW(gateway.createCommit(lst, TEST_UID, attrs), BranchUnavailableException);

    //switch stable branch to normal mode
    BranchManager(txn).loadStable().setState(txn, BranchState::Normal);

    Commit commit = gateway.createCommit(lst, TEST_UID, attrs);

    checkRelations(snapshotC.loadMasterRelations(testId), correctMasterRelations);

    BOOST_CHECK_THROW(gateway.checkConflicts(ids), ConflictsFoundException);

    checkCommit(commit, fourthCommitData);
    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_deduplication)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk

    DBID startJcAttributesId = 0;
    const DBIDSet startJcs { 9, 11, 13, 23 };

    DBID endJcAttributesId = 0;
    const DBIDSet endJcs { 10, 12, 13, 24 };

    auto filter = filters::ObjRevAttr::isNotDeleted();
    auto revisionIds = gateway.snapshot(commitB).revisionIdsByFilter(filter);
    auto revs = gateway.reader().loadRevisions(revisionIds);
    for (const auto& rev : revs) {
        auto oid = rev.id().objectId();
        auto attrsId = rev.revisionData().attributesId;
        if (startJcs.count(oid)) {
            BOOST_CHECK(attrsId);
            if (startJcAttributesId) {
                BOOST_CHECK_EQUAL(startJcAttributesId, attrsId);
            } else {
                startJcAttributesId = attrsId;
            }
        } else if (endJcs.count(oid)) {
            BOOST_CHECK(attrsId);
            if (endJcAttributesId) {
                BOOST_CHECK_EQUAL(endJcAttributesId, attrsId);
            } else {
                endJcAttributesId = attrsId;
            }
        }
    }
    BOOST_CHECK(startJcAttributesId);
    BOOST_CHECK(endJcAttributesId);
}

BOOST_AUTO_TEST_CASE(test_gateway_load_master_relations_after_d)
{
    setTestData("sql/001-GatewayLoadMasterRelationsAfterD.sql");
    DBID testId = 5;
    std::map<ObjectRevision::ID, RelationData> correctMasterRelations {
        { createID(10, commitA), RelationData {1, testId} },
        { createID(11, commitA), RelationData {2, testId} },
        { createID(13, commitA), RelationData {3, testId} },
        { createID(16, commitA), RelationData {8, testId} },
        { createID(39, commitD), RelationData {20, testId} }
    };

    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);

    for (auto branchType : {BranchType::Trunk, BranchType::Approved, BranchType::Stable}) {
        auto branchTypeStr = boost::lexical_cast<std::string>(branchType);
        RevisionsGateway gateway(txn, branchManager.loadByString(branchTypeStr));

        checkRelations(
            gateway.snapshot(commitD).loadMasterRelations(testId),
            correctMasterRelations);
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_load_commit_d)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gateway(txn); // Trunk

    auto commit = Commit::load(txn, commitD);
    checkCommit(commit, fourthCommitMergedData);

    std::list<DBID> updates;
    auto reader = gateway.reader();
    Revisions revs = reader.commitRevisions(commitD);

    ObjectRevision::ID rev_20_B = createID(20, commitB);
    ObjectRevision::ID rev_21_B = createID(21, commitB);
    ObjectRevision::ID rev_24_B = createID(24, commitB);

    ObjectRevision::ID rev_20_D = createID(20, commitD);
    ObjectRevision::ID rev_21_D = createID(21, commitD);
    ObjectRevision::ID rev_24_D = createID(24, commitD);
    ObjectRevision::ID rev_39_D = createID(39, commitD);

    std::map<ObjectRevision::ID, TestRevisionData> correctData {
        { rev_20_D, { rev_20_B, NO_ID, TestData { Wkb { GEOMETRY_2 } } } },
        { rev_21_D, { rev_21_B, NO_ID, TestData { DELETED } } },
        { rev_24_D, { rev_24_B, NO_ID, TestData {
            DELETED, Attributes {{"role", "end_jc"}}, NO_DESC, NO_GEOM, RelationData {20, 21} } } },
        { rev_39_D, { NO_ID, NO_ID, TestData {
            RelationData {20, 5}, Attributes {{"role", "end_jc"}} } } }
    };

    BOOST_CHECK_EQUAL(revs.size(), correctData.size());
    for (const auto& rev : revs) {
        checkRevision(rev, correctData, gateway);
    }

    std::map<ObjectRevision::ID, TestRevisionData> commitBData {
        { rev_20_B, { NO_ID, rev_20_D, TestData { Wkb {GEOMETRY_1} } } },
        { rev_21_B, { NO_ID, rev_21_D, TestData {} } },
        { rev_24_B, { NO_ID, rev_24_D, TestData {
            RelationData {20, 21}, Attributes {{"role", "end_jc"}} } } }
    };

    Revisions revisions;
    for (const auto& data : commitBData) {
        revisions.push_back(reader.loadRevision(data.first));
    }
    for (const auto& rev : revisions) {
        checkRevision(rev, commitBData, gateway);
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_create_commit_e)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn, BranchManager(txn).loadStable());

    ObjectRevision::ID rev_1_A = createID(1, commitA);
    ObjectRevision::ID rev_10_A = createID(10, commitA);

    std::list<NewRevisionData> lst {
        { rev_1_A, TestData {} },
        { rev_10_A, TestData { DELETED} },
        { createID(40), TestData {} },
        { createID(41), TestData {} },
        { createID(42), TestData {} },
        { createID(43), TestData {} },
        { createID(44), TestData { RelationData {1, 42}, Attributes {{"role", "end_jc"}} } },
        { createID(45), TestData { RelationData {41, 40}, Attributes {{"role", "start_jc"}} } },
        { createID(46), TestData { RelationData {41, 42}, Attributes {{"role", "end_jc"}} } },
        { createID(47), TestData { RelationData {43, 42}, Attributes {{"role", "start_jc"}} } },
        { createID(48), TestData { RelationData {43, 5}, Attributes {{"role", "end_jc"}} } }
    };

    std::set<ObjectRevision::ID> ids { rev_1_A, rev_10_A };

    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_NOTE_E} };

    gateway.checkConflicts(ids);

    Commit commit = gateway.createCommit(lst, TEST_UID, attrs);
    BOOST_CHECK_THROW(gateway.checkConflicts(ids), ConflictsFoundException);

    checkCommit(commit, fifthCommitData);
    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_load_commit_e)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gateway(txn, BranchManager(txn).loadStable());

    auto commit = Commit::load(txn, commitE);
    checkCommit(commit, fifthCommitData);

    std::map<ObjectRevision::ID, TestRevisionData> correctData {
        { createID(1, commitE), { createID(1, commitA), NO_ID, TestData {} } },
        { createID(10, commitE),{ createID(10, commitA), NO_ID, TestData { DELETED, Attributes {{"role", "end_jc"}}, NO_DESC, NO_GEOM, RelationData {1, 5} } } },
        { createID(40, commitE), { NO_ID, NO_ID, TestData {} } },
        { createID(41, commitE), { NO_ID, NO_ID, TestData {} } },
        { createID(42, commitE), { NO_ID, NO_ID, TestData {} } },
        { createID(43, commitE), { NO_ID, NO_ID, TestData {} } },
        { createID(44, commitE), { NO_ID, NO_ID, TestData { RelationData {1, 42}, Attributes {{"role", "end_jc"}} } } },
        { createID(45, commitE), { NO_ID, NO_ID, TestData { RelationData {41, 40}, Attributes {{"role", "start_jc"}} } } },
        { createID(46, commitE), { NO_ID, NO_ID, TestData { RelationData {41, 42}, Attributes {{"role", "end_jc"}} } } },
        { createID(47, commitE), { NO_ID, NO_ID, TestData { RelationData {43, 42}, Attributes {{"role", "start_jc"}} } } },
        { createID(48, commitE), { NO_ID, NO_ID, TestData { RelationData {43, 5}, Attributes {{"role", "end_jc"}} } } }
    };

    auto reader = gateway.reader();
    Revisions revs = reader.commitRevisions(commitE);
    BOOST_CHECK_EQUAL(revs.size(), correctData.size());
    for (const auto& rev : revs) {
        checkRevision(rev, correctData, gateway);
    }

    typedef std::pair<DBID, OptionalDBID> ObjectIDHeadCommitIDPair;
    std::list<ObjectIDHeadCommitIDPair> stableHeads {
        { 20, commitD },
        { objectManeuver, commitA },
        { objectStranger, std::nullopt }
    };
    for (const auto& objHeadPair : stableHeads) {
        std::optional<ObjectRevision> rev =
            gateway.snapshot(BIG_ID).objectRevision(objHeadPair.first);
        BOOST_REQUIRE_EQUAL(!rev, !objHeadPair.second);
        if (rev) {
            BOOST_CHECK_EQUAL((*rev).id().commitId(), *objHeadPair.second);
        }
    }

    RevisionsGateway trunkGateway(txn); // Trunk
    std::list<ObjectIDHeadCommitIDPair> trunkHeads {
        { 1, commitC },
        { 10, commitA },
        { 20, commitD },
        { objectManeuver, commitA },
        { objectStranger, std::nullopt }
    };
    for (const auto& objHeadPair : trunkHeads) {
        std::optional<ObjectRevision> rev =
            trunkGateway.snapshot(BIG_ID).objectRevision(objHeadPair.first);
        BOOST_REQUIRE_EQUAL(!rev, !objHeadPair.second);
        if (rev) {
            BOOST_CHECK_EQUAL((*rev).id().commitId(), *objHeadPair.second);
        }
    }

    ObjectRevision rev = reader.loadRevision(createID(1, commitA));
    BOOST_CHECK_EQUAL(rev.nextTrunkId().commitId(), commitC);
}

BOOST_AUTO_TEST_CASE(test_gateway_bulk_load_master_relations_after_c)
{
    DBIDSet testIds { 5, 21 };
    std::map<ObjectRevision::ID, RelationData> correctMasterRelations {
        { createID(10, commitA), RelationData {1, 5} },
        { createID(11, commitA), RelationData {2, 5} },
        { createID(13, commitA), RelationData {3, 5} },
        { createID(16, commitA), RelationData {8, 5} },
        { createID(24, commitB), RelationData {20, 21} }
    };

    for (auto branchType : {BranchType::Trunk, BranchType::Stable}) {
        pqxx::work txn(getConnection());
        BranchManager branchManager(txn);
        RevisionsGateway gateway(txn, branchType == BranchType::Trunk
            ? branchManager.loadTrunk()
            : branchManager.loadStable());

        checkRelations(
            gateway.snapshot(commitC).loadMasterRelations(testIds),
            correctMasterRelations);
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_bulk_load_master_relations_after_d)
{
    DBIDSet testIds { 5, 21, 40 };
    const std::map<ObjectRevision::ID, RelationData> correctMasterRelations {
        { createID(10, commitA), RelationData {1, 5} },
        { createID(11, commitA), RelationData {2, 5} },
        { createID(13, commitA), RelationData {3, 5} },
        { createID(16, commitA), RelationData {8, 5} },
        { createID(39, commitD), RelationData {20, 5} }
    };

    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);

    for (auto branchType : {BranchType::Trunk, BranchType::Approved, BranchType::Stable}) {
        auto branchTypeStr = boost::lexical_cast<std::string>(branchType);
        RevisionsGateway gateway(txn, branchManager.loadByString(branchTypeStr));
        checkRelations(
            gateway.snapshot(commitD).loadMasterRelations(testIds),
            correctMasterRelations);
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_bulk_load_master_relations_after_e)
{
    DBIDSet testIds { 5, 21, 40 };
    std::map<ObjectRevision::ID, RelationData> masterRelations {
        { createID(11, commitA), RelationData {2, 5} },
        { createID(13, commitA), RelationData {3, 5} },
        { createID(16, commitA), RelationData {8, 5} },
        { createID(39, commitD), RelationData {20, 5} }
    };

    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);

    for (auto branchType : {BranchType::Trunk, BranchType::Approved, BranchType::Stable}) {
        auto branchTypeStr = boost::lexical_cast<std::string>(branchType);
        RevisionsGateway gateway(txn, branchManager.loadByString(branchTypeStr));

        auto correctMasterRelations = masterRelations;
        if (branchType == BranchType::Trunk || branchType == BranchType::Approved) {
            correctMasterRelations.insert({ createID(10, commitA), RelationData(1, 5) });
        }
        else {
            correctMasterRelations.insert({ createID(45, commitE), RelationData(41, 40) });
            correctMasterRelations.insert({ createID(48, commitE), RelationData(43, 5) });
        }
        checkRelations(
            gateway.snapshot(commitE).loadMasterRelations(testIds),
            correctMasterRelations
        );
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_bulk_load_slave_relations_after_e)
{
    DBIDSet testIds { 1, 41 };
    typedef std::map<ObjectRevision::ID, RelationData> DataType;

    const std::map<BranchType, DataType> slaveRelationsByBranchType
    {
        { BranchType::Trunk, {
            { createID(10, commitA), RelationData {1, 5} },
            { createID(34, commitC), RelationData {1, 32} }
        } },
        { BranchType::Approved, {
            { createID(9, commitA), RelationData {1, 4} },
            { createID(10, commitA), RelationData {1, 5} }
        } },
        { BranchType::Stable, {
            { createID(9, commitA), RelationData {1, 4} },
            { createID(44, commitE), RelationData {1, 42} },
            { createID(45, commitE), RelationData {41, 40} },
            { createID(46, commitE), RelationData {41, 42} },
        } }
    };

    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);

    for (auto branchType : {BranchType::Trunk, BranchType::Approved, BranchType::Stable}) {
        auto branchTypeStr = boost::lexical_cast<std::string>(branchType);
        RevisionsGateway gateway(txn, branchManager.loadByString(branchTypeStr));

        checkRelations(
            gateway.snapshot(commitE).loadSlaveRelations(testIds),
            slaveRelationsByBranchType.at(branchType));
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_load_slave_relations_after_e)
{
    DBID testId = 1;
    typedef std::map<ObjectRevision::ID, RelationData> DataType;

    const std::map<BranchType, DataType> slaveRelationsByBranchType
    {
        { BranchType::Trunk, {
            { createID(10, commitA), RelationData {1, 5} },
            { createID(34, commitC), RelationData {1, 32} }
        } },
        { BranchType::Approved, {
            { createID(9, commitA), RelationData {1, 4} },
            { createID(10, commitA), RelationData {1, 5} }
        } },
        { BranchType::Stable, {
            { createID(9, commitA), RelationData {1, 4} },
            { createID(44, commitE), RelationData {1, 42} }
        } }
    };

    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);

    for (auto branchType : {BranchType::Trunk, BranchType::Approved, BranchType::Stable}) {
        auto branchTypeStr = boost::lexical_cast<std::string>(branchType);
        RevisionsGateway gateway(txn, branchManager.loadByString(branchTypeStr));

        checkRelations(
            gateway.snapshot(commitE).loadSlaveRelations(testId),
            slaveRelationsByBranchType.at(branchType));
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_check_delete_for_already_deleted)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn, BranchManager(txn).loadStable());

    auto rev = gateway.snapshot(BIG_ID).objectRevision(10);
    BOOST_CHECK_EQUAL(rev->data().deleted, true);

    std::list<NewRevisionData> lst {
        { rev->id(), TestData { DELETED} },
    };

    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_NOTE_DELETE_ALREADY_DELETED} };
    BOOST_CHECK_THROW(gateway.createCommit(lst, TEST_UID, attrs), AlreadyDeletedException);
}

BOOST_AUTO_TEST_CASE(test_gateway_append_approved_commit)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gateway(txn); // Trunk

    auto maneuverID = gateway.snapshot(BIG_ID).objectRevision(objectManeuver)->id();

    std::list<NewRevisionData> lst { { maneuverID, TestData {} } };
    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_NOTE_6} };

    Commit commit = gateway.createCommit(lst, TEST_UID, attrs);
    BOOST_CHECK_EQUAL(commit.state(), CommitState::Draft);
    BOOST_CHECK_EQUAL(maneuverID.commitId(), commitA); // 1

    CommitManager commitMgr(txn);
    BOOST_CHECK_EQUAL(commitMgr.approve(DBIDSet{commit.id()}).size(), 1);

    auto commitLoaded = Commit::load(txn, commit.id());
    BOOST_CHECK_EQUAL(commitLoaded.state(), CommitState::Approved);
    BOOST_CHECK_EQUAL(commitLoaded.id(), commitApprovedNotCommitted);
    //txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_create_stable_commit_over_trunk_conflict)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn, BranchManager(txn).loadStable());

    std::set<ObjectRevision::ID> ids;
    std::list<NewRevisionData> lst;

    for (DBID id : { 1, 40, 41 }) {
        auto revId = createID(id, commitE);
        ids.insert(revId);
        lst.push_back( { revId, TestData {} } );
    }

    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_NOTE_E_PLUS} };

    gateway.checkConflicts(ids);

    Commit commit = gateway.createCommit(lst, TEST_UID, attrs);
    BOOST_CHECK_EQUAL(commit.id(), commitStableOverConflict);

    BOOST_CHECK(commit.inStable());
    BOOST_CHECK(!commit.inTrunk());

    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_create_stable_commit_over_draft)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn, BranchManager(txn).loadStable());

    ObjectRevision::ID rev_30_C = createID(30, commitC);
    ObjectRevision::ID rev_31_C = createID(31, commitC);

    std::list<NewRevisionData> lst {
        { rev_31_C, TestData {} },
        { rev_30_C, TestData {} }
    };

    std::set<ObjectRevision::ID> ids { rev_30_C, rev_31_C };

    Attributes attrs { {STR_DESCRIPTION, "stable over draft"} };

    gateway.checkConflicts(ids);

    BOOST_CHECK_THROW(gateway.createCommit(lst, TEST_UID, attrs), maps::Exception);
}

BOOST_AUTO_TEST_CASE(test_gateway_create_commit_custom_attributes)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk

    ObjectRevision::ID rev_30_C = createID(30, commitC);
    ObjectRevision::ID rev_31_C = createID(31, commitC);

    std::list<NewRevisionData> lst {
        { rev_31_C, TestData {} },
        { rev_30_C, TestData {} }
    };

    std::set<ObjectRevision::ID> ids { rev_30_C, rev_31_C };

    Attributes attrs { {STR_DESCRIPTION, ""} };

    gateway.checkConflicts(ids);

    // invalid commit attributes
    BOOST_CHECK_THROW(gateway.createCommit(lst, TEST_UID, attrs), maps::Exception);

    attrs[STR_DESCRIPTION] = "non-empty value";
    Commit commit = gateway.createCommit(lst, TEST_UID, attrs); // ok
    BOOST_CHECK_EQUAL(commit.id(), commitWithEmptyAttributes);
    BOOST_CHECK(commit.attributes() == attrs);

    attrs["key"] = "bla-bla-bla";
    commit.setAttributes(txn, attrs);
    BOOST_CHECK(commit.attributes() == attrs);
    BOOST_CHECK_THROW(commit.addAttribute(txn, "key1", "value"), maps::Exception);

    auto commit2 = Commit::load(txn, commit.id());
    BOOST_CHECK(commit2.attributes() == attrs);
    BOOST_CHECK_THROW(commit2.setAttributes(txn, attrs), maps::RuntimeError);

    //Add attribute
    BOOST_CHECK_THROW(commit2.addAttribute(txn, "key", "value"), maps::Exception);
    BOOST_CHECK_THROW(commit2.addAttribute(txn, "key1", ""), maps::Exception);
    BOOST_CHECK_NO_THROW(commit2.addAttribute(txn, "key1", "value"));
    BOOST_CHECK(commit2.attributes().count("key1"));
    //Read again
    auto commit21 = Commit::load(txn, commit.id());
    BOOST_CHECK_THROW(commit21.addAttribute(txn, "key1", "value1"), maps::Exception);
    BOOST_CHECK(commit21.attributes().count("key1"));
    BOOST_CHECK_EQUAL(commit21.attributes().at("key1"), "value");
    //txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_object_revision_id_empty_commit)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gateway(txn); // Trunk

    DBID objectId = 1;
    BOOST_CHECK(!gateway.snapshot(commitA).objectRevision(objectId)->id().empty()); // ok

    auto emptySnapshot = gateway.snapshot(0);
    BOOST_CHECK_NO_THROW(emptySnapshot.objectRevision(objectId));

    DBIDSet ids;
    ids.insert(objectId);
    BOOST_CHECK_NO_THROW(emptySnapshot.objectRevisions(ids));
    BOOST_CHECK_NO_THROW(emptySnapshot.objectRevisionIds(ids));
}

BOOST_AUTO_TEST_CASE(test_gateway_object_revision_id_unexisted)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gateway(txn); // Trunk
    BOOST_CHECK(!gateway.snapshot(BIG_ID).objectRevision(BIG_ID));
}

BOOST_AUTO_TEST_CASE(test_gateway_object_revision_id_deleted)
{
    pqxx::work txn(getConnection());

    DBID deletedObjectId = 10; // in stable only
    {
        RevisionsGateway gateway(txn, BranchManager(txn).loadStable());
        BOOST_CHECK(gateway.snapshot(BIG_ID).objectRevision(deletedObjectId)->data().deleted);
    }
    {
        RevisionsGateway gateway(txn); // Trunk
        BOOST_CHECK(!gateway.snapshot(BIG_ID).objectRevision(deletedObjectId)->data().deleted);
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_object_revisions_bulk_trunk)
{
    DBIDSet ids;
    for (DBID i = 1; i <= 48; ++i) {
        ids.insert(i);
    }

    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk
    auto snapshot = gateway.snapshot(BIG_ID);
    BOOST_CHECK_EQUAL(snapshot.objectRevisions(ids).size(), 32);
    BOOST_CHECK_EQUAL(snapshot.objectRevisionIds(ids).size(), 32);
}

BOOST_AUTO_TEST_CASE(test_gateway_object_revisions_bulk_stable)
{
    DBIDSet ids;
    for (DBID i = 1; i <= 48; ++i) {
        ids.insert(i);
    }

    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn, BranchManager(txn).loadStable());
    auto snapshot = gateway.snapshot(BIG_ID);
    BOOST_CHECK_EQUAL(snapshot.objectRevisions(ids).size(), 32);
    BOOST_CHECK_EQUAL(snapshot.objectRevisionIds(ids).size(), 32);
}

std::vector<ObjectRevision::ID> revisionIds(
    pqxx::work& txn,
    OptionalDBID commitId = std::nullopt)
{
    RevisionsGateway gatewayTrunk(txn);

    DBID realCommitId = commitId
        ? *commitId
        : gatewayTrunk.headCommitId();

    auto filter = filters::ObjRevAttr::isNotDeleted();
    if (Commit::load(txn, realCommitId).inTrunk()) {
        auto snapshot = gatewayTrunk.snapshot(realCommitId);
        return snapshot.revisionIdsByFilter(filter);
    }

    RevisionsGateway gatewayStable(txn, BranchManager(txn).loadStable());
    return gatewayStable.snapshot(realCommitId).revisionIdsByFilter(filter);
}

std::vector<ObjectRevision::ID> revisionIdsByRegion(
    double lon1, double lat1, double lon2, double lat2,
    pqxx::work& txn,
    OptionalDBID commitId = std::nullopt)
{
    RevisionsGateway gatewayTrunk(txn);

    DBID realCommitId = commitId
        ? *commitId
        : gatewayTrunk.headCommitId();

    if (Commit::load(txn, realCommitId).inTrunk()) {
        return gatewayTrunk.snapshot(realCommitId).revisionIdsByRegion(lon1, lat1, lon2, lat2);
    }

    RevisionsGateway gatewayStable(txn, BranchManager(txn).loadStable());
    return gatewayStable.snapshot(realCommitId).revisionIdsByRegion(lon1, lat1, lon2, lat2);
}

BOOST_AUTO_TEST_CASE(test_gateway_revision_ids)
{
    pqxx::work txn(getConnection());

    BOOST_CHECK_EQUAL(revisionIds(txn, commitA).size(), 17);
    BOOST_CHECK_EQUAL(revisionIds(txn).size(), 29); // 32 - 3 deleted

    RevisionsGateway gateway(txn); // Trunk
    DBIDSet ids;
    for (DBID i = 1; i <= 48; ++i) {
        ids.insert(i);
    }

    auto snapshot = gateway.snapshot(gateway.maxSnapshotId());
    BOOST_CHECK_EQUAL(snapshot.objectRevisions(ids).size(), 32); // view deleted
    BOOST_CHECK_EQUAL(snapshot.objectRevisionIds(ids).size(), 32);
}

BOOST_AUTO_TEST_CASE(test_gateway_revision_ids_by_region_wkb_head)
{
    pqxx::work txn(getConnection());

    auto revs = revisionIdsByRegion(0, 0, 7, 7, txn);
    BOOST_CHECK_EQUAL(revs.size(), 1);
    BOOST_REQUIRE(revs.size() > 0);
    BOOST_CHECK_EQUAL(revs.begin()->objectId(), 20);
    BOOST_CHECK_EQUAL(revs.begin()->commitId(), commitD);
}

BOOST_AUTO_TEST_CASE(test_gateway_revision_ids_by_region_wkb_by_commit)
{
    pqxx::work txn(getConnection());

    auto revs = revisionIdsByRegion(0, 0, 7, 7, txn, commitB); // 2
    BOOST_CHECK_EQUAL(revs.size(), 1);
    BOOST_CHECK_EQUAL(revs.begin()->objectId(), 20);
    BOOST_CHECK_EQUAL(revs.begin()->commitId(), commitB);

    BOOST_CHECK_EQUAL(revisionIdsByRegion(0, 0, 7, 7, txn, commitA).size(), 0);

    for (const size_t i: {2, 6, 7, 8, 10}) {
        BOOST_CHECK_EQUAL(revisionIdsByRegion(0, 0, 7, 7, txn, i).size(), 1);
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_revision_ids_by_region_box_head)
{
    pqxx::work txn(getConnection());

    auto revs = revisionIdsByRegion(0, 0, 7, 7, txn);
    BOOST_CHECK_EQUAL(revs.size(), 1);
    BOOST_REQUIRE(revs.size() > 0);
    BOOST_CHECK_EQUAL(revs.begin()->objectId(), 20);
    BOOST_CHECK_EQUAL(revs.begin()->commitId(), commitD);

    auto revs2 = revisionIdsByRegion(0, 1, 3, 1, txn);
    BOOST_CHECK_EQUAL(revs2.size(), 1);
    BOOST_REQUIRE(revs2.size() > 0);
    BOOST_CHECK_EQUAL(revs2.begin()->objectId(), 20);
    BOOST_CHECK_EQUAL(revs2.begin()->commitId(), commitD);

    BOOST_CHECK(revisionIdsByRegion(0, 1, 0.5, 1, txn).empty());
    BOOST_CHECK(revisionIdsByRegion(11, 1, 12, 1, txn).empty());

    auto revs3 = revisionIdsByRegion(6, 1, 7, 1, txn);
    BOOST_CHECK_EQUAL(revs3.size(), 1);
    BOOST_REQUIRE(revs3.size() > 0);
    BOOST_CHECK_EQUAL(revs3.begin()->objectId(), 20);
    BOOST_CHECK_EQUAL(revs3.begin()->commitId(), commitD);
}

BOOST_AUTO_TEST_CASE(test_gateway_revision_ids_by_region_box_by_commit)
{
    pqxx::work txn(getConnection());

    auto revs = revisionIdsByRegion(0, 0, 7, 7, txn, commitB); // 2
    BOOST_CHECK_EQUAL(revs.size(), 1);
    BOOST_CHECK_EQUAL(revs.begin()->objectId(), 20);
    BOOST_CHECK_EQUAL(revs.begin()->commitId(), 2);

    BOOST_CHECK_EQUAL(revisionIdsByRegion(0, 0, 7, 7, txn, commitA).size(), 0);
    for (const size_t i: {2, 6, 7, 8, 10}) {
        BOOST_CHECK_EQUAL(revisionIdsByRegion(0, 0, 7, 7, txn, i).size(), 1);
    }

    BOOST_CHECK(revisionIdsByRegion(0, 1, 0.5, 1, txn, commitB).empty());
    BOOST_CHECK(revisionIdsByRegion(11, 1, 12, 1, txn, commitB).empty());

    auto revs2 = revisionIdsByRegion(0, 1, 3, 1, txn, commitB); // 2
    BOOST_CHECK_EQUAL(revs2.size(), 1);
    BOOST_CHECK_EQUAL(revs2.begin()->objectId(), 20);
    BOOST_CHECK_EQUAL(revs2.begin()->commitId(), commitB);

    for (const size_t i: {6, 7}) {
        BOOST_CHECK_EQUAL(revisionIdsByRegion(i, 1, 7, 1, txn, commitB).empty(), i >= 6);
        BOOST_CHECK_EQUAL(revisionIdsByRegion(i, 1, 7, 1, txn, commitC).empty(), i >= 6);
        BOOST_CHECK(!revisionIdsByRegion(i, 1, 7, 1, txn, commitD).empty());
        BOOST_CHECK(!revisionIdsByRegion(i, 1, 7, 1, txn, commitE).empty());
    }
    BOOST_CHECK_EQUAL(revisionIdsByRegion(3, 1, 7, 1, txn, commitB).size(), 1);
}

BOOST_AUTO_TEST_CASE(test_gateway_load_diff_commit_d)
{
    std::map<DBID, TestObjectRevisionDiff> correctDiff {
        { 20, { createID(20, commitB), createID(20, commitD), GeometryDiff {GEOMETRY_1, GEOMETRY_2} } },
        { 21, { createID(21, commitB), createID(21, commitD), DeletedDiff {false, true}, std::nullopt, std::nullopt } },
        { 24, { createID(24, commitB), createID(24, commitD), DeletedDiff {false, true}, Attributes {{"role", "end_jc"}}, RelationData {20, 21} } },
        { 39, { NO_ID, createID(39, commitD), RelationData {20, 5}, Attributes {{"role", "end_jc"}} } }
    };

    for (auto branchType : {BranchType::Trunk, BranchType::Stable}) {
        pqxx::work txn(getConnection());
        BranchManager branchManager(txn);
        RevisionsGateway gateway(txn, branchType == BranchType::Trunk
            ? branchManager.loadTrunk()
            : branchManager.loadStable());

        auto commitDiff = gateway.reader().commitDiff(commitD);
        BOOST_CHECK_EQUAL(commitDiff.size(), correctDiff.size());
        for (const auto& p : commitDiff) {
            DBID objectId = p.first;
            const ObjectRevisionDiff& objectDiff = p.second;
            auto it = correctDiff.find(objectId);
            BOOST_REQUIRE(it != correctDiff.end());
            checkObjectRevisionDiff(
                createID(objectId, commitD),
                TestObjectRevisionDiff {objectDiff.oldId(), objectDiff.newId(), objectDiff.data()},
                it->second);
        }
    }
}


BOOST_AUTO_TEST_CASE(test_gateway_load_diff_commit_e)
{
    std::map<DBID, TestObjectRevisionDiff> correctDiff {
        { 1, { createID(1, commitA), createID(1, commitE) } },
        { 10, { createID(10, commitA), createID(10, commitE), DeletedDiff {false, true}, Attributes {{"role", "end_jc"}}, RelationData {1, 5} } },
        { 40, { NO_ID, createID(40, commitE) } },
        { 41, { NO_ID, createID(41, commitE) } },
        { 42, { NO_ID, createID(42, commitE) } },
        { 43, { NO_ID, createID(43, commitE) } },
        { 44, { NO_ID, createID(44, commitE), RelationData {1, 42}, Attributes {{"role", "end_jc"}} } },
        { 45, { NO_ID, createID(45, commitE), RelationData {41, 40}, Attributes {{"role", "start_jc"}} } },
        { 46, { NO_ID, createID(46, commitE), RelationData {41, 42}, Attributes {{"role", "end_jc"}} } },
        { 47, { NO_ID, createID(47, commitE), RelationData {43, 42}, Attributes {{"role", "start_jc"}} } },
        { 48, { NO_ID, createID(48, commitE), RelationData {43, 5}, Attributes {{"role", "end_jc"}} } }
    };

    for (auto branchType : {BranchType::Trunk, BranchType::Stable}) {
        pqxx::work txn(getConnection());
        BranchManager branchManager(txn);
        RevisionsGateway gateway(txn, branchType == BranchType::Trunk
            ? branchManager.loadTrunk()
            : branchManager.loadStable());

        auto commitDiff = gateway.reader().commitDiff(commitE);
        BOOST_CHECK_EQUAL(commitDiff.size(), correctDiff.size());
        for (const auto& p : commitDiff) {
            DBID objectId = p.first;
            const ObjectRevisionDiff& objectDiff = p.second;
            auto it = correctDiff.find(objectId);
            BOOST_REQUIRE(it != correctDiff.end());
            checkObjectRevisionDiff(
                createID(objectId, commitE),
                TestObjectRevisionDiff {objectDiff.oldId(), objectDiff.newId(), objectDiff.data()},
                it->second);
        }
    }
}

BOOST_AUTO_TEST_CASE(test_detach_conflicts_on_close_stable_branch)
{
    auto id1 = createID(1, commitA);
    auto id10 = createID(10, commitA);
    auto id1_E = createID(1, commitE);
    auto id10_E = createID(10, commitE);

    pqxx::work txn(getConnection());
    auto reader = RevisionsGateway(txn).reader();

    BOOST_CHECK_EQUAL(reader.loadRevision(id1).nextTrunkId().commitId(), commitC); // conflicted
    BOOST_CHECK_EQUAL(reader.loadRevision(id10).nextTrunkId().commitId(), 0); // non-conflicted

    auto branch = BranchManager(txn).loadStable();
    BOOST_CHECK_EQUAL(branch.id(), TEST_BRANCH_ID);

    // commitE - non-merged
    auto commit = Commit::load(txn, commitE);
    BOOST_CHECK_EQUAL(commit.inTrunk(), false);
    BOOST_CHECK_EQUAL(commit.stableBranchId(), branch.id());
    BOOST_CHECK_EQUAL(commit.state(), CommitState::Approved);
    BOOST_CHECK_EQUAL(reader.loadRevision(id1_E).prevId().commitId(), commitA);
    BOOST_CHECK_EQUAL(reader.loadRevision(id10_E).prevId().commitId(), commitA);

    BOOST_CHECK_EQUAL(branch.finish(txn, TEST_UID), true);
    BOOST_CHECK_EQUAL(Commit::load(txn, commitE).stableBranchId(), TEST_BRANCH_ID);
    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_skip_detach_next_chain_conflicts_from_stable_branch)
{
    pqxx::work txn(getConnection());

    BranchManager branchManager(txn);
    BOOST_CHECK_THROW(branchManager.loadStable(), BranchNotExistsException);

    RevisionsGateway gateway(txn, BranchManager(txn).load(TEST_BRANCH_ID));
    auto reader = gateway.reader();

    for (DBID id : { 1, 40, 41 }) {
        auto rev = reader.loadRevision(createID(id, commitE)); // non-merged
        BOOST_CHECK_EQUAL(rev.prevId().commitId(), id == 1 ? commitA : 0); // keep previous id

        auto nextRev = reader.loadRevision(createID(id, commitStableOverConflict));
        BOOST_CHECK_EQUAL(nextRev.prevId().commitId(), commitE); // keep previous id
    }

    const DBID objectId = 1;

    auto snapshot = gateway.snapshot(BIG_ID);
    auto rev = snapshot.objectRevision(objectId);
    BOOST_REQUIRE(rev);
    BOOST_CHECK_EQUAL(rev->id().commitId(), commitStableOverConflict);

    auto revisions = snapshot.objectRevisions( DBIDSet { objectId } );
    BOOST_CHECK_EQUAL(revisions.size(), 1);
    auto it = revisions.find(objectId);
    BOOST_REQUIRE(it != revisions.end());
    BOOST_CHECK_EQUAL(it->second.id().commitId(), commitStableOverConflict);

    auto revisionIds = snapshot.objectRevisionIds( DBIDSet { objectId } );
    BOOST_REQUIRE_EQUAL(revisionIds.size(), 1);
    BOOST_CHECK_EQUAL(revisionIds.front().commitId(), commitStableOverConflict);
}

BOOST_AUTO_TEST_CASE(test_branch_manager_create_stable_after_detach_non_merged_conflicts)
{
    pqxx::work txn(getConnection());

    BranchManager branchManager(txn);
    BOOST_CHECK_THROW(branchManager.loadStable(), BranchNotExistsException);
    auto branch = branchManager.createStable(TEST_UID, {});
    BOOST_CHECK_EQUAL(branch.id(), TEST_NEXT_BRANCH_ID);
    branch.setState(txn, BranchState::Normal);
    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_branch_manager_get_branches_trunk_stable_archive_skip_previous)
{
    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);
    auto branches = branchManager.load(
        { { BranchType::Trunk, 1 },
          { BranchType::Stable, 1 },
          { BranchType::Archive, 1 } });
    BOOST_REQUIRE(!branches.empty());
    BOOST_CHECK_EQUAL(branches.size(), 3);
    BOOST_CHECK_EQUAL(branches.front().type(), BranchType::Trunk);
    BOOST_CHECK_EQUAL(branches.front().id(), TRUNK_BRANCH_ID);
    auto it = branches.begin();
    ++it;
    BOOST_CHECK_EQUAL(it->type(), BranchType::Stable);
    BOOST_CHECK_EQUAL(it->id(), TEST_NEXT_BRANCH_ID);
    BOOST_CHECK_EQUAL(branches.back().type(), BranchType::Archive);
    BOOST_CHECK_EQUAL(branches.back().id(), TEST_BRANCH_ID);
}

BOOST_AUTO_TEST_CASE(test_branch_manager_get_all_branches_trunk_stable_archive_unlimited)
{
    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);
    auto branches = branchManager.load(
        { { BranchType::Trunk, BranchManager::UNLIMITED },
          { BranchType::Stable, BranchManager::UNLIMITED },
          { BranchType::Archive, BranchManager::UNLIMITED } });
    BOOST_REQUIRE(!branches.empty());
    BOOST_CHECK_EQUAL(branches.size(), 4);
    BOOST_CHECK_EQUAL(branches.front().type(), BranchType::Trunk);
    BOOST_CHECK_EQUAL(branches.front().id(), TRUNK_BRANCH_ID);
    auto it = branches.begin();
    ++it;
    BOOST_CHECK_EQUAL(it->type(), BranchType::Stable);
    BOOST_CHECK_EQUAL(it->id(), TEST_NEXT_BRANCH_ID);
    ++it;
    BOOST_CHECK_EQUAL(it->type(), BranchType::Archive);
    BOOST_CHECK_EQUAL(it->id(), TEST_BRANCH_ID);
    BOOST_CHECK_EQUAL(branches.back().type(), BranchType::Archive);
    BOOST_CHECK_EQUAL(branches.back().id(), TEST_PREV_BRANCH_ID);
}

BOOST_AUTO_TEST_CASE(test_gateway_init_by_branch_and_lookup)
{
    pqxx::work txn(getConnection());

    const DBID objectId = 1;

    BranchManager branchManager(txn);
    {
        auto branch = branchManager.loadTrunk();
        BOOST_CHECK_EQUAL(branch.id(), TRUNK_BRANCH_ID);
        RevisionsGateway gateway(txn, branch);
        auto rev = gateway.snapshot(BIG_ID).objectRevision(objectId);
        BOOST_REQUIRE(rev);
        BOOST_CHECK_EQUAL(rev->id().commitId(), commitC);
    }
    {
        auto branch = branchManager.loadStable();
        BOOST_CHECK_EQUAL(branch.id(), TEST_NEXT_BRANCH_ID);
        RevisionsGateway gateway(txn, branch);
        auto rev = gateway.snapshot(BIG_ID).objectRevision(objectId);
        BOOST_REQUIRE(rev);
        BOOST_CHECK_EQUAL(rev->id().commitId(), commitA);
    }
    {
        auto branch = branchManager.load(TEST_BRANCH_ID);
        RevisionsGateway gateway(txn, branch);
        auto rev = gateway.snapshot(BIG_ID).objectRevision(objectId);
        BOOST_REQUIRE(rev);
        BOOST_CHECK_EQUAL(rev->id().commitId(), commitStableOverConflict);
        BOOST_CHECK_EQUAL(rev->prevId().commitId(), commitE);
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_append_stable_commit_in_previous_merge_base)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn, BranchManager(txn).loadStable());

    const DBID objectId = 1;

    auto snapshot = gateway.snapshot(BIG_ID);
    auto rev = snapshot.objectRevision(objectId);
    BOOST_REQUIRE(rev);
    BOOST_CHECK_EQUAL(rev->id().commitId(), commitA);
    {
        auto revisions = snapshot.objectRevisions( DBIDSet { objectId } );
        BOOST_CHECK_EQUAL(revisions.size(), 1);
        auto it = revisions.find(objectId);
        BOOST_REQUIRE(it != revisions.end());
        BOOST_CHECK_EQUAL(it->second.id().commitId(), commitA);
    }

    std::list<NewRevisionData> data { { rev->id(), TestData { DELETED } } };

    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_NOTE_F} };

    Commit commit = gateway.createCommit(data, TEST_UID, attrs);
    BOOST_CHECK_EQUAL(commit.id(), commitF);

    rev = snapshot.objectRevision(objectId);
    BOOST_REQUIRE(rev);
    BOOST_CHECK_EQUAL(rev->id().commitId(), commitF);
    {
        auto revisions = snapshot.objectRevisions( DBIDSet { objectId } );
        BOOST_CHECK_EQUAL(revisions.size(), 1);
        auto it = revisions.find(objectId);
        BOOST_REQUIRE(it != revisions.end());
        BOOST_CHECK_EQUAL(it->second.id().commitId(), commitF);
        BOOST_CHECK_EQUAL(it->second.data().deleted, true);
    }
    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_check_existed_revision_in_current_stable)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gateway(txn, BranchManager(txn).loadStable());

    const DBID objectId = 1;

    auto rev = gateway.snapshot(commitF - 1).objectRevision(objectId);
    BOOST_REQUIRE(rev);
    BOOST_CHECK_EQUAL(rev->data().deleted, false);
    BOOST_CHECK_EQUAL(rev->id().objectId(), objectId);
    BOOST_CHECK_EQUAL(rev->id().commitId(), commitA);

    BOOST_CHECK_EQUAL(Commit::load(txn, commitA).stableBranchId(), TEST_PREV_BRANCH_ID);
}

BOOST_AUTO_TEST_CASE(test_gateway_check_first_existed_revision_in_previous_stable_branch)
{
    pqxx::work txn(getConnection());

    auto branch = BranchManager(txn).load(TEST_PREV_BRANCH_ID);
    RevisionsGateway gateway(txn, branch);

    const DBID objectId = 1;

    auto rev = gateway.snapshot(BIG_ID).objectRevision(objectId);
    BOOST_REQUIRE(rev);
    BOOST_CHECK_EQUAL(rev->data().deleted, false);
    BOOST_CHECK_EQUAL(rev->id().objectId(), objectId);
    BOOST_CHECK_EQUAL(rev->id().commitId(), commitA);
}

BOOST_AUTO_TEST_CASE(test_gateway_check_deleted_object_in_stable_finished)
{
    pqxx::work txn(getConnection());

    Branch branch = BranchManager(txn).loadStable();
    BOOST_CHECK_EQUAL(branch.finish(txn, TEST_UID), true);
    BOOST_CHECK_EQUAL(branch.type(), BranchType::Archive);

    RevisionsGateway gateway(txn, branch);

    const DBID objectId = 1;

    auto snapshot = gateway.snapshot(gateway.maxSnapshotId());
    auto rev = snapshot.objectRevision(objectId);
    BOOST_REQUIRE(rev);
    BOOST_CHECK_EQUAL(rev->data().deleted, true);

    auto filter = filters::ObjRevAttr::isNotDeleted();
    auto allNonDeletedRevisionIds = snapshot.revisionIdsByFilter(filter);
    for (const auto& revId : allNonDeletedRevisionIds) {
        BOOST_CHECK(objectId != revId.objectId());
    }
}

BOOST_AUTO_TEST_CASE(test_gateway_check_insert_duplicated_object_id)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk

    const DBID objectId = 1;

    auto rev = gateway.snapshot(BIG_ID).objectRevision(objectId);
    BOOST_REQUIRE(rev);

    std::list<NewRevisionData> data { { createID(objectId), TestData { } } };

    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_INSERT_DUPLICATED_OBJECT_ID} };

    BOOST_CHECK_THROW(gateway.createCommit(data, TEST_UID, attrs), AlreadyExistsException);
}

BOOST_AUTO_TEST_CASE(test_gateway_check_insert_new_object_id_twice)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk

    const DBID objectId = BIG_ID - 1;

    auto rev = gateway.snapshot(BIG_ID).objectRevision(objectId);
    BOOST_REQUIRE(!rev);

    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_INSERT_DUPLICATED_OBJECT_ID} };

    std::list<NewRevisionData> data { { createID(objectId), TestData { } } };
    BOOST_CHECK(gateway.createCommit(data, TEST_UID, attrs).id() > 0);
    BOOST_CHECK_THROW(gateway.createCommit(data, TEST_UID, attrs), AlreadyExistsException);
}

BOOST_AUTO_TEST_CASE(test_gateway_check_delete_and_check_concurrent_update)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn); // Trunk

    const DBID objectId = 1;

    auto rev = gateway.snapshot(BIG_ID).objectRevision(objectId);
    BOOST_REQUIRE(rev);
    BOOST_REQUIRE(!rev->data().deleted);

    std::list<NewRevisionData> data { { rev->id(), TestData { DELETED } } };

    Attributes attrs { {STR_DESCRIPTION, TEST_COMMIT_CHECK_CONCURRENT_UPDATE} };

    gateway.createCommit(data, TEST_UID, attrs);

    // check concurrent locking from another connection
    pqxx::connection conn2(connectionString());
    pqxx::work txn2(conn2);
    RevisionsGateway gateway2(txn2); // Trunk

    BOOST_CHECK_THROW(gateway2.createCommit(data, TEST_UID, attrs), ConcurrentLockDataException);
}

BOOST_AUTO_TEST_CASE(test_gateway_bulk_load_commits)
{
    pqxx::work txn(getConnection());

    const std::vector<DBID> ids{7, 8, 10, 12};
    const auto commits = Commit::load(txn, filters::CommitAttr::id().in(ids));

    const std::set<DBID> idsOrdered(ids.begin(), ids.end());
    BOOST_REQUIRE(commits.size() == idsOrdered.size());
    for (const auto& commit : commits) {
        BOOST_CHECK(idsOrdered.count(commit.id()));
    }
}


BOOST_AUTO_TEST_CASE(test_load_user_count)
{
    pqxx::work txn(getConnection());

    const auto emptyUserCount = Commit::loadUserCommitsCount(txn, filters::False());
    BOOST_REQUIRE(emptyUserCount.empty());

    const auto allUserCount = Commit::loadUserCommitsCount(txn, filters::True());
    BOOST_REQUIRE(!allUserCount.empty());
    BOOST_REQUIRE(allUserCount.size() == 1);
    BOOST_REQUIRE(allUserCount.count(TEST_UID));
    BOOST_CHECK_EQUAL(allUserCount.at(TEST_UID), 7);

    const std::vector<DBID> ids{7, 8, 10, 12};
    const auto filteredUserCount = Commit::loadUserCommitsCount(txn, filters::CommitAttr::id().in(ids));
    BOOST_REQUIRE(filteredUserCount.count(TEST_UID));
    BOOST_CHECK_EQUAL(filteredUserCount.at(TEST_UID), 4);

}

BOOST_AUTO_TEST_CASE(test_gateway_insert_into_archive_branch)
{
    pqxx::work txn(getConnection());

    // Check archive branch
    const Branch branch = BranchManager(txn).load(TEST_BRANCH_ID);
    BOOST_REQUIRE_EQUAL(branch.finishedBy(), TEST_UID);
    BOOST_REQUIRE_EQUAL(branch.type(), BranchType::Archive);
    RevisionsGateway gateway(txn, branch);

    // Create new revision of object and create new object
    const Snapshot snapshot = gateway.snapshot(BIG_ID);
    const RevisionID maneuverId = snapshot.objectRevision(objectManeuver)->id();
    const RevisionID emptyNewRevisionId  = gateway.acquireObjectId();
    std::list<NewRevisionData> lst {
        {maneuverId, TestData {}},
        {emptyNewRevisionId, TestData {}}
    };
    Attributes attrs { {STR_DESCRIPTION, TEST_PATCH_ARCHIVE_BRANCH} };
    const Commit commit = gateway.createCommit(lst, TEST_UID, attrs);

    // Check commit data
    BOOST_CHECK_EQUAL(commit.state(), CommitState::Approved);
    BOOST_CHECK_EQUAL(commit.inTrunk(), false);
    BOOST_CHECK_EQUAL(commit.stableBranchId(), branch.id());

    // Check revisions' references.
    const Reader& reader = gateway.reader();
    const ObjectRevision maneuverPrevRevision = reader.loadRevision(maneuverId);
    const ObjectRevision maneuverNewRevision = reader.loadRevision({maneuverId.objectId(), commit.id()});
    const ObjectRevision newObjectRevision = reader.loadRevision({emptyNewRevisionId.objectId(), commit.id()});
    BOOST_CHECK_EQUAL(maneuverNewRevision.prevId(), maneuverPrevRevision.id());
    BOOST_CHECK_EQUAL(newObjectRevision.prevId(), RevisionID());

    // Check snapshot
    const Snapshot testSnapshot = gateway.snapshot(commit.id());
    const ObjectRevision revision = testSnapshot.objectRevision(maneuverId.objectId()).value();
    BOOST_CHECK_EQUAL(commit.id(), revision.id().commitId());
    BOOST_CHECK(testSnapshot.objectRevision(emptyNewRevisionId.objectId()));

    txn.commit();
}

BOOST_AUTO_TEST_CASE(test_gateway_insert_into_trunk_after_archive_branch_patching)
{
    pqxx::work txn(getConnection());

    // Create new revision of object in trunk
    RevisionsGateway gateway(txn);
    const Snapshot snapshot = gateway.snapshot(BIG_ID);
    const RevisionID maneuverId = snapshot.objectRevision(objectManeuver)->id();
    std::list<NewRevisionData> lst { { maneuverId, TestData {} } };
    Attributes attrs { {STR_DESCRIPTION, TEST_PATCH_ARCHIVE_BRANCH} };
    const Commit commit = gateway.createCommit(lst, TEST_UID, attrs);

    // Check commit data
    BOOST_CHECK_EQUAL(commit.state(), CommitState::Draft);
    BOOST_CHECK_EQUAL(commit.inTrunk(), true);
    BOOST_CHECK_EQUAL(commit.stableBranchId(), 0);

    // Check revisions' references.
    const Reader& reader = gateway.reader();
    const ObjectRevision prevRevision = reader.loadRevision(maneuverId);
    const ObjectRevision newRevision = reader.loadRevision({maneuverId.objectId(), commit.id()});
    BOOST_CHECK_EQUAL(prevRevision.nextTrunkId(), newRevision.id());
    BOOST_CHECK_EQUAL(newRevision.prevId(), prevRevision.id());

    // Check snapshot
    const Snapshot testSnapshot = gateway.snapshot(commit.id());
    const ObjectRevision revision = testSnapshot.objectRevision(maneuverId.objectId()).value();
    BOOST_CHECK_EQUAL(commit.id(), revision.id().commitId());
}

BOOST_AUTO_TEST_CASE(test_gateway_insert_into_stable_after_archive_branch_patching)
{
    pqxx::work txn(getConnection());

    // Create new revision of object in stable
    const Branch branch = BranchManager(txn).loadStable();
    RevisionsGateway gateway(txn, branch);
    const Snapshot snapshot = gateway.snapshot(BIG_ID);
    const RevisionID maneuverId = snapshot.objectRevision(objectManeuver)->id();
    std::list<NewRevisionData> lst { {maneuverId, TestData{}} };
    Attributes attrs { {STR_DESCRIPTION, TEST_PATCH_ARCHIVE_BRANCH} };
    const Commit commit = gateway.createCommit(lst, TEST_UID, attrs);

    // Check commit data
    BOOST_CHECK_EQUAL(commit.state(), CommitState::Approved);
    BOOST_CHECK_EQUAL(commit.inStable(), true);

    // Check revisions' references.
    const Reader& reader = gateway.reader();
    const ObjectRevision prevRevision = reader.loadRevision(maneuverId);
    const ObjectRevision newRevision = reader.loadRevision({maneuverId.objectId(), commit.id()});
    BOOST_CHECK_EQUAL(newRevision.prevId(), prevRevision.id());

    // Check snapshot
    const Snapshot testSnapshot = gateway.snapshot(commit.id());
    const ObjectRevision revision = testSnapshot.objectRevision(maneuverId.objectId()).value();
    BOOST_CHECK_EQUAL(commit.id(), revision.id().commitId());
}


BOOST_AUTO_TEST_CASE(test_gateway_insert_into_archive_branch_after_stable_branch_editing)
{
    // It's a very very long test scenario. Make yourself more comfortable.
    pqxx::work txn(getConnection());
    BranchManager branchManager(txn);
    CommitManager commitManager(txn);


    // Finish old stable branch
    branchManager.loadStable().finish(txn, TEST_UID);

    // Create new revision in trunk
    RevisionsGateway trunk(txn);
    const RevisionID emptyRevisionId = trunk.acquireObjectId();
    const DBID objectId = emptyRevisionId.objectId();
    const std::list<NewRevisionData> trunkData {{emptyRevisionId, TestData {}}};
    const Attributes attrs { {STR_DESCRIPTION, TEST_PATCH_ARCHIVE_BRANCH} };
    const Commit trunkCommit = trunk.createCommit(trunkData, TEST_UID, attrs);

    // Create stable branch
    const DBIDSet idToApprove = {trunkCommit.id()};
    CommitManager(txn).approve(idToApprove);
    const Attributes branchAttrs {};
    branchManager.createStable(TEST_UID, branchAttrs);
    commitManager.mergeApprovedToStable(trunkCommit.id());
    Branch stableBranch = branchManager.loadStable();
    stableBranch.setState(txn, BranchState::Normal);

    // Now finish stable branch and create new one.
    stableBranch.finish(txn, TEST_UID);
    branchManager.createStable(TEST_UID, branchAttrs);
    Branch newStableBranch = branchManager.loadStable();
    newStableBranch.setState(txn, BranchState::Normal);

    // Create new revision in new stable.
    RevisionsGateway newStable(txn, newStableBranch);
    const RevisionID stableRevisionId(objectId, trunkCommit.id());
    const std::list<NewRevisionData> newStableData {{stableRevisionId, TestData {}}};
    const Commit newStableCommit = newStable.createCommit(newStableData, TEST_UID, attrs);
    const RevisionID newStableRevisionId = RevisionID(objectId, newStableCommit.id());

    // Now stable (don't confuse with new stable) is archive branch.
    const Branch archiveBranch = branchManager.load(stableBranch.id());
    BOOST_REQUIRE_EQUAL(archiveBranch.finishedBy(), TEST_UID);
    BOOST_REQUIRE_EQUAL(archiveBranch.type(), BranchType::Archive);

    // Create new revision of object in archive branch (patching)
    RevisionsGateway archive(txn, archiveBranch);
    const Snapshot archiveSnapshot = archive.snapshot(BIG_ID);
    const RevisionID archiveRevisionId = archiveSnapshot.objectRevision(objectId).value().id();
    const std::list<NewRevisionData> archiveData { {archiveRevisionId, TestData {} } };
    const Commit archiveCommit = archive.createCommit(archiveData, TEST_UID, attrs);

    // Check revisions' references.
    const Reader& reader = archive.reader();
    const ObjectRevision newStableRevision = reader.loadRevision(newStableRevisionId);
    const ObjectRevision newArchiveRevision = reader.loadRevision({objectId, archiveCommit.id()});
    const ObjectRevision prevRevision = reader.loadRevision({objectId, trunkCommit.id()});
    BOOST_CHECK_EQUAL(newStableRevision.prevId(), prevRevision.id());
    BOOST_CHECK_EQUAL(newArchiveRevision.prevId(), prevRevision.id());

    // Check snapshot
    const Snapshot newArchiveSnapshot = archive.snapshot(archiveCommit.id());
    const ObjectRevision revision = newArchiveSnapshot.objectRevision(objectId).value();
    BOOST_CHECK_EQUAL(archiveCommit.id(), revision.id().commitId());

    // All actions are not commited.
}

BOOST_AUTO_TEST_CASE(test_gateway_insert_into_unavailable_approved_branch)
{
    pqxx::work txn(getConnection());

    // Check approved branch
    const Branch branch = BranchManager(txn).load(TEST_APPROVED_BRANCH_ID);
    BOOST_REQUIRE_EQUAL(branch.type(), BranchType::Approved);
    RevisionsGateway gateway(txn, branch);

    // Try create  new revision of object
    const Snapshot snapshotHead = gateway.snapshot(BIG_ID);
    const RevisionID maneuverID = snapshotHead.objectRevision(objectManeuver)->id();
    std::list<NewRevisionData> lst { { maneuverID, TestData {} } };
    Attributes attrs { {STR_DESCRIPTION, TEST_PATCH_ARCHIVE_BRANCH} };
    BOOST_CHECK_THROW(gateway.createCommit(lst, TEST_UID, attrs), BranchReadOnlyException);
}

BOOST_AUTO_TEST_CASE(test_gateway_insert_into_deleted_branch)
{
    pqxx::work txn(getConnection());

    // Check approved branch
    Branch branch = BranchManager(txn).load(TEST_BRANCH_ID);
    BOOST_REQUIRE_EQUAL(branch.type(), BranchType::Archive);
    BOOST_REQUIRE_EQUAL(branch.state(), BranchState::Normal);
    RevisionsGateway gateway(txn, branch);

    // Delete archive branch
    BOOST_REQUIRE_EQUAL(branch.setState(txn, BranchState::Unavailable), true);
    BOOST_REQUIRE_EQUAL(branch.setType(txn, BranchType::Deleted), true);

    // Try create  new revision of object
    const Snapshot snapshotHead = gateway.snapshot(BIG_ID);
    const RevisionID maneuverID = snapshotHead.objectRevision(objectManeuver)->id();
    std::list<NewRevisionData> lst { { maneuverID, TestData {} } };
    Attributes attrs { {STR_DESCRIPTION, TEST_PATCH_ARCHIVE_BRANCH} };
    BOOST_CHECK_THROW(gateway.createCommit(lst, TEST_UID, attrs), BranchUnavailableException);
}

BOOST_AUTO_TEST_CASE(test_gateway_check_conflict_for_archive_branch)
{
    const DBID archiveBranchId = 5;
    const DBID objectId = 39;

    pqxx::work work(getConnection());

    BranchManager branchManager(work);
    const Branch archiveBranch = branchManager.load(archiveBranchId);

    RevisionsGateway archive(work, archiveBranch);

    // Check conflict, when id is used already.
    const std::list<RevisionID> newIds {createID(objectId)};
    BOOST_CHECK_THROW(archive.checkConflicts(newIds), maps::Exception);

    // Prepare data
    const Snapshot snapshot = archive.snapshot(BIG_ID);
    const RevisionID prevRevisionId = snapshot.objectRevision(objectId)->id();

    Attributes attrs {{STR_DESCRIPTION, TEST_PATCH_ARCHIVE_BRANCH}};
    const std::list<NewRevisionData> data {
        {prevRevisionId, TestData {RelationData {20, 5}, Attributes {{"role", "end_jc"}}}}};

    archive.createCommit(data, TEST_UID, attrs);
    const std::list<RevisionID> ids {prevRevisionId};
    BOOST_CHECK_THROW(archive.checkConflicts(ids), ConflictsFoundException);
}

BOOST_AUTO_TEST_CASE(test_branch_attributes)
{
    pqxx::work txn(getConnection());

    BranchManager branchManager(txn);
    auto branches = branchManager.load(
            {{BranchType::Trunk, BranchManager::UNLIMITED}});
    BOOST_REQUIRE_EQUAL(branches.size(), 1);
    auto& branch = branches.front();

    BOOST_CHECK_EQUAL(branch.attributes().size(), 0);
    branch.concatenateAttributes(txn, {{"a", "1"}});
    BOOST_CHECK_EQUAL(branch.attributes().size(), 1);
    BOOST_CHECK_EQUAL(branch.attributes().at("a"), "1");

    branch.concatenateAttributes(txn, {{"a", "2"}, {"b", "3"}});
    BOOST_CHECK_EQUAL(branch.attributes().size(), 2);
    BOOST_CHECK_EQUAL(branch.attributes().at("a"), "2");
    BOOST_CHECK_EQUAL(branch.attributes().at("b"), "3");

    branch.concatenateAttributes(txn, {});
    BOOST_CHECK_EQUAL(branch.attributes().size(), 2);

    branches = branchManager.load(
            {{BranchType::Trunk, BranchManager::UNLIMITED}});
    BOOST_REQUIRE_EQUAL(branches.size(), 1);
    const auto& loadedBranch = branches.front();
    BOOST_CHECK_EQUAL(loadedBranch.attributes().size(), 2);
    BOOST_CHECK_EQUAL(loadedBranch.attributes().at("a"), "2");
    BOOST_CHECK_EQUAL(loadedBranch.attributes().at("b"), "3");
}

BOOST_AUTO_TEST_CASE(test_gateway_reserve_object_ids)
{
    const std::string LAST_ID_QUERY = "SELECT last_value FROM revision.object_id_seq";
    const size_t NUMBER = 10;

    pqxx::work txn(getConnection());

    auto res = txn.exec(LAST_ID_QUERY);
    auto lastId = res[0][0].as<DBID>();

    RevisionsGateway gateway(txn); // Trunk

    auto resultId = gateway.reserveObjectIds(NUMBER);
    BOOST_CHECK_EQUAL(resultId, lastId);

    res = txn.exec(LAST_ID_QUERY);
    auto newLastId = res[0][0].as<DBID>();
    BOOST_CHECK_EQUAL(newLastId, lastId + NUMBER);
}

// new tests should be placed here

// BOOST_AUTO_TEST_CASE(test_gateway_***)
// {
// }

// the last test case
BOOST_AUTO_TEST_CASE(test_gateway_clear_all)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gateway(txn);
    gateway.truncateAll();
    gateway.createDefaultBranches();

    BOOST_CHECK_EQUAL(gateway.headCommitId(), 0);

    ObjectRevision::ID id = gateway.acquireObjectId();
    BOOST_CHECK_EQUAL(id.objectId(), 1);
    BOOST_CHECK_EQUAL(id.commitId(), 0);
    txn.commit();
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace tests
} // namespace revision
} // namespace wiki
} // namespace maps
