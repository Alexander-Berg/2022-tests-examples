#include "common.h"
#include "helpers.h"

#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/commit_manager.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>

#include <boost/test/unit_test.hpp>

namespace maps {
namespace wiki {
namespace revision {
namespace tests {

typedef RevisionsGateway::NewRevisionData NewRevisionData;

namespace {

const Attributes TEST_COMMIT_ATTRS{{STR_DESCRIPTION, "TEST COMMIT"}};

} //anonymous namespace

BOOST_FIXTURE_TEST_SUITE(LoadTests, DbFixture)

BOOST_AUTO_TEST_CASE(test_load_object_revisions_by_filter)
{
    pqxx::work txn(getConnection());
    RevisionsGateway gateway(txn);

    auto obj1Rev = createUniqueId();
    auto obj2Rev = createUniqueId();

    DBID obj1Id = obj1Rev.objectId();
    DBID obj2Id = obj2Rev.objectId();
    // DBID childId = childRev.objectId();

    /*
     * Creates the following topology
     * obj1 <-- rel --> obj2
     */
    const std::list<NewRevisionData> revData{
        {obj1Rev, TestData() },
        {obj2Rev, TestData() },
        {createUniqueId(), TestData(RelationData{obj1Id, obj2Id}) }
    };

    Commit commit = gateway.createCommit(
        revData.cbegin(), revData.cend(), TEST_UID, TEST_COMMIT_ATTRS);

    auto snapshot = gateway.snapshot(commit.id());

    Revisions data;

    data = snapshot.objectRevisionsByFilter(filters::True());
    BOOST_CHECK_EQUAL(data.size(), revData.size());

    data = snapshot.objectRevisionsByFilter(
            filters::ObjRevAttr::isRegularObject());
    BOOST_CHECK_EQUAL(data.size(), 2);

    data = snapshot.objectRevisionsByFilter(
            filters::ObjRevAttr::isRelation());
    BOOST_CHECK_EQUAL(data.size(), 1);
}


BOOST_AUTO_TEST_SUITE_END()

} // namespace tests
} // namespace revision
} // namespace wiki
} // namespace maps
