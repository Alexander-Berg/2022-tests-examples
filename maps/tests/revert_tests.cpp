#include "common.h"
#include "helpers.h"
#include "std_optional_printer.h"
#include <maps/wikimap/mapspro/libs/revision/revision_data.h>

#include <yandex/maps/wiki/revision/commit_manager.h>
#include <yandex/maps/wiki/revision/exception.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>

#include <boost/test/unit_test.hpp>

namespace std {
std::ostream& operator<<(std::ostream& os, const maps::wiki::revision::Attributes& attributes)
{
    os << "attributes:" << std::endl;
    for (const auto& pair : attributes) {
        os << "(" << pair.first << ", " << pair.second << ")" << std::endl;
    }
    return os;
}
}

namespace maps {
namespace wiki {
namespace revision {

std::ostream& operator<<(std::ostream& os, const RelationData& relationData)
{
    os << "relation(" << relationData.masterObjectId() << "; " << relationData.slaveObjectId() << ")";
    return os;
}

namespace tests {

namespace {

struct RevisionTestData {
    DBID restoreCommitId;
    DBID prevCommitId;
};

typedef std::map<DBID, RevisionTestData> RevisionTestDataMap;

struct RevertTestFixture: public DbFixture
{
    void testFindDependentCommitsInTrunk(const DBIDSet& commitIds, const DBIDSet& answer);
    void testRevertCommitsInTrunk(const DBIDSet& commitIds, const RevisionTestDataMap& revisionTestDataMap);
};

void RevertTestFixture::testFindDependentCommitsInTrunk(const DBIDSet& commitIds, const DBIDSet& answer)
{
    pqxx::work work(getConnection());
    CommitManager committer(work);
    const auto result = committer.findDependentCommitsInTrunk(commitIds);
    BOOST_CHECK_EQUAL_COLLECTIONS(answer.begin(), answer.end(), result.begin(), result.end());
}

void RevertTestFixture::testRevertCommitsInTrunk(const DBIDSet& commitIds, const RevisionTestDataMap& revisionTestDataMap)
{
    pqxx::work work(getConnection());
    CommitManager committer(work);
    const Attributes attrs{
        {STR_DESCRIPTION, "Test revert"}
    };
    const auto result = committer.revertCommitsInTrunk(commitIds, TEST_UID, attrs);
    const Commit& commit = result.createdCommit;
    BOOST_REQUIRE(commit.revertedCommitIds() == result.revertedCommitIds);
    BOOST_REQUIRE(commit.revertedDirectlyCommitIds() == commitIds);
    auto revertedDirectlyCommits = Commit::load(work, filters::CommitAttr::id().in(commit.revertedDirectlyCommitIds()));
    for (const auto& revertedCommit: revertedDirectlyCommits) {
        BOOST_REQUIRE(revertedCommit.revertingDirectlyCommitId() == commit.id());
    }
    auto revertedCommits = Commit::load(work, filters::CommitAttr::id().in(commit.revertedCommitIds()));
    for (const auto& revertedCommit: revertedCommits) {
        BOOST_REQUIRE(revertedCommit.revertingCommitIds().count(commit.id()) > 0);
    }

    RevisionsGateway gateway(work);
    Reader reader = gateway.reader();
    RevisionIdToRevisionMap revisions;
    if (!revisionTestDataMap.empty()) {
        for (const auto& object: reader.commitRevisions(commit.id())) {
            revisions.insert({object.id(), object});
        }
    }

    BOOST_CHECK_EQUAL(revisions.size(), revisionTestDataMap.size());
    for (const auto& pair: revisionTestDataMap) {
        const DBID& objectId = pair.first;
        const RevisionTestData& revisionTestData = pair.second;
        const auto it = revisions.find({objectId, commit.id()});
        BOOST_CHECK(it != revisions.cend());
        const ObjectRevision& revision = it->second;
        if (revisionTestData.restoreCommitId == 0) {
            BOOST_CHECK_EQUAL(revision.data().deleted, true);
        } else {
            const RevisionID revisionId{objectId, revisionTestData.restoreCommitId};
            const std::optional<ObjectRevision> expectedRevision = reader.tryLoadRevision(revisionId);
            BOOST_CHECK(expectedRevision);
            const ObjectData data = revision.data();
            const ObjectData expectedData = expectedRevision->data();
            BOOST_CHECK_EQUAL(data.deleted, expectedData.deleted);
            BOOST_CHECK_EQUAL(data.attributes, expectedData.attributes);
            BOOST_CHECK_EQUAL(data.description, expectedData.description);
            BOOST_CHECK_EQUAL(data.geometry, expectedData.geometry);
            BOOST_CHECK_EQUAL(data.relationData, expectedData.relationData);
        }
        const RevisionID previousRevisionId{objectId, revisionTestData.prevCommitId };
        BOOST_CHECK_EQUAL(revision.prevId(), previousRevisionId);
    }

    work.commit();
}

} // namespace

BOOST_FIXTURE_TEST_SUITE(TestRevertCommitsInTrunk, RevertTestFixture)

BOOST_AUTO_TEST_CASE(T00_EmptyInput)
{
    const DBIDSet commitIds{};
    const DBIDSet dependentCommitIds{};
    pqxx::work work(getConnection());
    CommitManager committer(work);
    const DBIDSet result = committer.findDependentCommitsInTrunk(commitIds);
    BOOST_CHECK_EQUAL_COLLECTIONS(dependentCommitIds.begin(), dependentCommitIds.end(),
        result.begin(), result.end());
    BOOST_CHECK_THROW(committer.revertCommitsInTrunk(commitIds, TEST_UID,
        {{"action", "revert"}}), RuntimeError);
}


BOOST_AUTO_TEST_CASE(T01_OneCommit)
{
    setTestData("sql/005-OneCommit.sql");
    const DBIDSet commitIds{1};
    const DBIDSet dependentCommitIds{1};
    const RevisionTestDataMap revisionTestDataMap{
        { 1, {0, 1}},
        { 2, {0, 1}},
        { 3, {0, 1}}
    };
    testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
    testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
    BOOST_CHECK_THROW(
        testRevertCommitsInTrunk(commitIds, revisionTestDataMap),
        AlreadyRevertedDirectlyCommitsException);
}

BOOST_AUTO_TEST_CASE(T02_TwoRoadElementsConnection)
{
    setTestData("sql/006-TwoRoadElementsConnection.sql");
    const DBIDSet commitIds{1};
    const DBIDSet dependentCommitIds{1, 2, 3, 4};
    const RevisionTestDataMap revisionTestDataMap{
        { 1, {0, 2}},
        { 2, {0, 1}},
        { 3, {0, 2}},
        { 4, {0, 1}},
        { 5, {0, 1}}
    };
    testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
    testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
}

// Recomended to see:
// https://wiki.yandex-team.ru/maps/dev/core/wikimap/mapspro/revert#peresecheniesudalennymobektom
BOOST_AUTO_TEST_CASE(T03_DeletedRoadProblem)
{
    setTestData("sql/007-DeletedRoadProblem.sql");
    const DBIDSet commitIds{2};
    const DBIDSet dependentCommitIds{2};
    const RevisionTestDataMap revisionTestDataMap{
        { 1, {1, 2}},
        { 2, {1, 2}},
        { 3, {1, 2}},
        { 4, {1, 2}},
        { 5, {1, 2}}
    };
    testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
    testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
}

BOOST_AUTO_TEST_CASE(T04_RoadElementClosedPolyline)
{
    setTestData("sql/008-RoadElementClosedPolyline.sql");
    const DBIDSet commitIds{3};
    const DBIDSet dependentCommitIds{3, 4, 5};
    const RevisionTestDataMap revisionTestDataMap{
        { 21, {0, 3}},
        { 22, {0, 3}},
        { 23, {0, 3}},
        { 24, {0, 3}},
        { 31, {0, 4}},
        { 32, {0, 4}},
        { 33, {0, 4}},
        { 34, {0, 4}},
        { 41, {0, 5}},
        { 42, {0, 5}},
        { 43, {0, 5}}
    };
    testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
    testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
}

BOOST_AUTO_TEST_CASE(T05_RoadEdit_RevertCommit_1)
{
    setTestData("sql/012-RoadEditInStable.sql");
    const DBIDSet commitIds{1};
    const DBIDSet dependentCommitIds{1, 2, 3, 4, 5, 6, 7};
    const RevisionTestDataMap revisionTestDataMap{
        { 1, {0, 1}},
        { 2, {0, 6}},
        { 3, {0, 6}},
        { 4, {0, 1}},
        { 5, {0, 1}},
        { 12, {0, 2}},
        { 21, {0, 3}},
        { 22, {0, 6}},
        { 23, {0, 3}},
        { 24, {0, 3}}
    };
    testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
    testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
}

BOOST_AUTO_TEST_CASE(T06_RoadEdit_RevertCommit_2)
{
    setTestData("sql/012-RoadEditInStable.sql");
    const DBIDSet commitIds{2};
    const DBIDSet dependentCommitIds{2, 4, 5, 6, 7};
    const RevisionTestDataMap revisionTestDataMap{
        {2, {1, 6}},
        {3, {1, 6}},
        {22, {3, 6}},
        {12, {0, 2}}
    };
    testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
    testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
}

BOOST_AUTO_TEST_CASE(T07_RoadEdit_RevertCommit_3)
{
    setTestData("sql/012-RoadEditInStable.sql");
    const DBIDSet commitIds{3};
    const DBIDSet dependentCommitIds{3, 4, 6, 7};
    const RevisionTestDataMap revisionTestDataMap {
        {2, {1, 6}},
        {3, {1, 6}},
        {11, {2, 7}},
        {14, {2, 7}},
        {21, {0, 3}},
        {22, {0, 6}},
        {23, {0, 3}},
        {24, {0, 3}},
        {41, {5, 7}},
        {42, {5, 7}}
    };
    testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
    testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
}

BOOST_AUTO_TEST_CASE(T08_BranchEdit_TrunkTest)
{
    setTestData("sql/010-BranchEdit.sql");
    const DBIDSet commitIds{1};
    const DBIDSet dependentCommitIds{1, 2, 4, 6};
    const RevisionTestDataMap revisionTestDataMap{
        {1, {0, 1}},
        {2, {0, 1}},
        {3, {0, 1}},
        {4, {0, 1}},
        {5, {0, 1}},
        {11, {0, 2}},
        {12, {0, 2}},
        {13, {0, 2}},
        {14, {0, 2}},
        {31, {0, 4}},
        {32, {0, 4}},
        {33, {0, 4}},
        {34, {0, 4}},
        {51, {0, 6}},
        {52, {0, 6}},
        {53, {0, 6}},
        {54, {0, 6}}
    };
    testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
    testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
}

BOOST_AUTO_TEST_CASE(T09_NotTrunkCommitException)
{
    setTestData("sql/010-BranchEdit.sql");
    const DBIDSet commitIds{1, 2, 3, 5};
    pqxx::work work(getConnection());
    CommitManager committer(work);
    BOOST_CHECK_THROW(committer.findDependentCommitsInTrunk(commitIds), RuntimeError);
    BOOST_CHECK_THROW(committer.revertCommitsInTrunk(commitIds, TEST_UID,
        {{"action", "revert"}}), RuntimeError);
}

BOOST_AUTO_TEST_CASE(T10_checkEmptyCommitProblem)
{
    setTestData("sql/006-TwoRoadElementsConnection.sql");
    const DBIDSet commitIds{3};
    const DBIDSet dependentCommitIds{3, 4};
    const RevisionTestDataMap revisionTestDataMap{};
    testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
    testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
}

BOOST_AUTO_TEST_CASE(T11_TripleRevert)
{
    { // Revert unlink address point A
        setTestData("sql/011-ReferencedAddressPoint.sql");
        const DBIDSet commitIds{5};
        const DBIDSet dependentCommitIds{5, 6};
        const RevisionTestDataMap revisionTestDataMap {
            {21, {3, 6}},
            {22, {3, 6}},
            {23, {3, 6}},
            {31, {4, 5}}
        };
        testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
        testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
    }

    { // Revert link address point A to road
        const DBIDSet commitIds{4};
        const DBIDSet dependentCommitIds{4, 5, 6, 7};
        const RevisionTestDataMap revisionTestDataMap {
            {31, {0, 7}}
        };
        testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
        testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
    }

    { // Revert the first revert commit.
        const DBIDSet commitIds{7};
        const DBIDSet dependentCommitIds{7, 8};
        const RevisionTestDataMap revisionTestDataMap {};
        testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
        BOOST_CHECK_THROW(testRevertCommitsInTrunk(commitIds, revisionTestDataMap),
            AlreadyRevertedCommitsException);
    }

    { // Revert the second revert commit.
        const DBIDSet commitIds{8};
        const DBIDSet dependentCommitIds{8};
        const RevisionTestDataMap revisionTestDataMap {
            {31, {7, 8}}
        };
        testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
        testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
    }

}

BOOST_AUTO_TEST_CASE(T12_TwoCrossedRoadProblem)
{
    setTestData("sql/014-TwoCrossedRoadProblem.sql");

    {
        const DBIDSet commitIds{3};
        const DBIDSet dependentCommitIds{3};
        const RevisionTestDataMap revisionTestDataMap {
            {3, {1, 3}},
            {5, {1, 3}},
            {13, {2, 3}},
            {15, {2, 3}},
            {21, {0, 3}},
            {22, {0, 3}},
            {23, {0, 3}},
            {24, {0, 3}},
            {25, {0, 3}},
            {26, {0, 3}},
            {27, {0, 3}},
            {28, {0, 3}},
            {29, {0, 3}},
            {30, {0, 3}},
            {31, {0, 3}},
            {32, {0, 3}},
            {33, {0, 3}},
            {34, {0, 3}},
            {35, {0, 3}},
            {36, {0, 3}},
            {37, {0, 3}},
            {38, {0, 3}},
            {39, {0, 3}},
            {40, {0, 3}},
            {41, {0, 3}}
        };
        testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
        testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
    }

    {
        const DBIDSet commitIds{2};
        const DBIDSet dependentCommitIds{2, 3, 4};
        const RevisionTestDataMap revisionTestDataMap {
            {11, {0, 2}},
            {12, {0, 2}},
            {13, {0, 4}},
            {14, {0, 2}},
            {15, {0, 4}}
        };
        testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
        testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
    }

    {
        const DBIDSet commitIds{1};
        const DBIDSet dependentCommitIds{1, 3, 4, 5};
        const RevisionTestDataMap revisionTestDataMap {
            {1, {0, 1}},
            {2, {0, 1}},
            {3, {0, 4}},
            {4, {0, 1}},
            {5, {0, 4}}
        };
        testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
        testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
    }

    {
        const DBIDSet commitIds{6};
        const DBIDSet dependentCommitIds{6};
        const RevisionTestDataMap revisionTestDataMap {
            {1, {1, 6}},
            {2, {1, 6}},
            {3, {4, 6}},
            {4, {1, 6}},
            {5, {4, 6}}
        };
        testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
        testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
    }
}

// Attention!!!
// In case of relation duplication in prepared revert-commit
// the deduplication algorithm chooses the one with the lowest object_id.
// This property is used during testing (algorithm is deterministic).
BOOST_AUTO_TEST_CASE(T13_TwoEqualRelations_1)
{
    setTestData("sql/016-TwoEqualRelations-1.sql");

    const DBIDSet commitIds{4};
    const DBIDSet dependentCommitIds{4, 6};
    const RevisionTestDataMap revisionTestDataMap {
        {11, {2, 6}},
        {12, {2, 6}},
        {13, {2, 6}},
        {14, {2, 6}},
        {25, {3, 4}},
        // {31, {5, 6}}, deduplicated relation
    };
    testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
    testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
}

BOOST_AUTO_TEST_CASE(T13_TwoEqualRelations_2)
{
    setTestData("sql/016-TwoEqualRelations-2.sql");

    const DBIDSet commitIds{4};
    const DBIDSet dependentCommitIds{4};
    const RevisionTestDataMap revisionTestDataMap {
        // {25, {3, 4}} deduplicated relation
    };
    testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
    testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
}

BOOST_AUTO_TEST_CASE(T13_TwoEqualRelations_3)
{
    setTestData("sql/016-TwoEqualRelations-3.sql");

    const DBIDSet commitIds{2};
    const DBIDSet dependentCommitIds{2, 3, 4};
    const RevisionTestDataMap revisionTestDataMap {
        {3, {1, 4}},
        {4, {1, 2}},
        {5, {1, 2}},
        {12, {0, 2}},
        {17, {0, 2}},
        {18, {0, 2}},
        {19, {0, 2}},
        {20, {0, 2}},
        {21, {0, 3}},
        {22, {0, 2}},
        {31, {0, 3}},
        {32, {0, 3}},
        {41, {0, 4}}
    };
    testFindDependentCommitsInTrunk(commitIds, dependentCommitIds);
    testRevertCommitsInTrunk(commitIds, revisionTestDataMap);
}

BOOST_AUTO_TEST_SUITE_END()


} // namespace tests
} // namespace revision
} // namespace wiki
} // namespace maps
