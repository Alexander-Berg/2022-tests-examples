#include "common.h"
#include "helpers.h"

#include <yandex/maps/wiki/revision/branch.h>
#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/revision/snapshot.h>

#include <boost/test/unit_test.hpp>

namespace maps {
namespace wiki {
namespace revision {
namespace tests {

BOOST_FIXTURE_TEST_SUITE(TestRevisionIdsByObjectIdsAndFilter, DbFixture)

BOOST_AUTO_TEST_CASE(TrunkBranchEmptyInput)
{
    setTestData("sql/005-OneCommit.sql");

    pqxx::work work(getConnection());
    Snapshot snapshot = RevisionsGateway(work).snapshot(1);

    checkRevisionIds(
        snapshot.revisionIdsByFilter(DBIDSet{}, filters::False()),
        {}
    );
}

BOOST_AUTO_TEST_CASE(TrunkBranchSimpleInput)
{
    setTestData("sql/005-OneCommit.sql");

    pqxx::work work(getConnection());
    Snapshot snapshot = RevisionsGateway(work).snapshot(1);

    checkRevisionIds(
        snapshot.revisionIdsByFilter(DBIDSet{1, 3}, filters::True()),
        {{1, 1}, {3, 1}}
    );
}

BOOST_AUTO_TEST_CASE(TrunkBranchWithFilter)
{
    setTestData("sql/005-OneCommit.sql");

    pqxx::work work(getConnection());
    Snapshot snapshot = RevisionsGateway(work).snapshot(1);

    checkRevisionIds(
        snapshot.revisionIdsByFilter(DBIDSet{1, 2}, filters::ObjRevAttr::isNotRelation()),
        {{1, 1}, {2, 1}}
    );
}

BOOST_AUTO_TEST_CASE(TrunkBranchComplicatedInput)
{
    setTestData("sql/009-RoadEditInTrunk.sql");

    pqxx::work work(getConnection());
    Snapshot snapshot = RevisionsGateway(work).snapshot(7);

    checkRevisionIds(
        snapshot.revisionIdsByFilter(
            DBIDSet{11, 12, 14, 22, 23},
            filters::ObjRevAttr::isNotRelation() &&
                filters::ObjRevAttr::isNotDeleted()
        ),
        {{12, 2}, {22, 6}}
    );
}

BOOST_AUTO_TEST_CASE(StableBranchWithFilter)
{
    setTestData("sql/012-RoadEditInStable.sql");

    pqxx::work work(getConnection());
    const Branch stable = BranchManager(work).loadStable();
    Snapshot snapshot = RevisionsGateway(work, stable).snapshot(4);

    checkRevisionIds(
        snapshot.revisionIdsByFilter(
            DBIDSet{12, 22, 31, 41},
            filters::ObjRevAttr::isNotRelation() &&
                filters::ObjRevAttr::isNotDeleted()
        ),
        {{12, 2}, {22, 3}}
    );
}

BOOST_AUTO_TEST_CASE(ApprovedBranchWithFilter)
{
    setTestData("sql/013-AddressPointEditInBranches.sql");

    pqxx::work work(getConnection());
    const Branch approved = BranchManager(work).loadApproved();
    Snapshot snapshot = RevisionsGateway(work, approved).snapshot(
        SnapshotId::fromCommit(5, BranchType::Approved, work)
    );

    checkRevisionIds(
        snapshot.revisionIdsByFilter(
            DBIDSet{12, 21, 23}, filters::ObjRevAttr::isNotRelation()
        ),
        {{12, 2}, {21, 5}}
    );
}

BOOST_AUTO_TEST_CASE(ApprovedBranchTryLoadRelations)
{
    setTestData("sql/012-RoadEditInStable.sql");

    pqxx::work work(getConnection());
    auto approved = BranchManager(work).loadApproved();
    RevisionsGateway gateway(work, approved);
    auto snapshot = gateway.snapshot(gateway.headCommitId());

    auto revs = snapshot.tryLoadRevisionIdsByFilter(
        filters::ObjRevAttr::isNotDeleted()
        && filters::ObjRevAttr::isRelation(), 10);
    BOOST_REQUIRE(revs);
    checkRevisionIds(
        *revs,
        {{4, 1}, {5, 1}, {23, 3}, {24, 3}}
    );

    revs = snapshot.tryLoadRevisionIdsByFilter(
        filters::ObjRevAttr::isNotDeleted()
        && filters::ObjRevAttr::isRelation(), 3);
    BOOST_REQUIRE(!revs);
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace tests
} // namespace revision
} // namespace wiki
} // namespace maps
