#include "common.h"
#include "helpers.h"

#include <yandex/maps/wiki/revision/branch.h>
#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/historical_snapshot.h>

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
    HistoricalSnapshot snapshot = RevisionsGateway(work).historicalSnapshot(1);

    checkRevisionIds(
        snapshot.revisionIdsByFilter(DBIDSet{}, filters::False()),
        {}
    );
}

BOOST_AUTO_TEST_CASE(TrunkBranchSimpleInput)
{
    setTestData("sql/005-OneCommit.sql");

    pqxx::work work(getConnection());
    HistoricalSnapshot snapshot = RevisionsGateway(work).historicalSnapshot(1);

    checkRevisionIds(
        snapshot.revisionIdsByFilter(DBIDSet{1, 3}, filters::True()),
        {{1, 1}, {3, 1}}
    );
}

BOOST_AUTO_TEST_CASE(TrunkBranchWithFilter)
{
    setTestData("sql/005-OneCommit.sql");

    pqxx::work work(getConnection());
    HistoricalSnapshot snapshot = RevisionsGateway(work).historicalSnapshot(1);

    checkRevisionIds(
        snapshot.revisionIdsByFilter(DBIDSet{1, 2}, filters::ObjRevAttr::isNotRelation()),
        {{1, 1}, {2, 1}}
    );
}

BOOST_AUTO_TEST_CASE(TrunkBranchComplicatedInput)
{
    setTestData("sql/009-RoadEditInTrunk.sql");

    pqxx::work work(getConnection());
    HistoricalSnapshot snapshot = RevisionsGateway(work).historicalSnapshot(3, 7);

    checkRevisionIds(
        snapshot.revisionIdsByFilter(
            DBIDSet{11, 12, 14, 22, 23},
            filters::ObjRevAttr::isNotRelation() &&
                filters::ObjRevAttr::isNotDeleted()
        ),
        {{22, 3}, {22, 6}}
    );
}

BOOST_AUTO_TEST_CASE(StableBranchWithFilter)
{
    setTestData("sql/012-RoadEditInStable.sql");

    pqxx::work work(getConnection());
    const Branch stable = BranchManager(work).loadStable();
    HistoricalSnapshot snapshot = RevisionsGateway(work, stable).historicalSnapshot(4);

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
    HistoricalSnapshot snapshot = RevisionsGateway(work, approved).historicalSnapshot(5);

    checkRevisionIds(
        snapshot.revisionIdsByFilter(
            DBIDSet{12, 21, 23}, filters::ObjRevAttr::isNotRelation()
        ),
        {{12, 2}, {21, 3}, {21, 5}}
    );
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace tests
} // namespace revision
} // namespace wiki
} // namespace maps
