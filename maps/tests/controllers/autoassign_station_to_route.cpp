#include <maps/wikimap/mapspro/services/editor/src/configs/categories_strings.h>
#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(autoassign_station_to_route)
{
WIKI_FIXTURE_TEST_CASE(test_auto_asssigne_and_remove_stop_from_route, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/mass_transit/create_transport_station_with_route.json");
    performSaveObjectRequest("tests/data/mass_transit/create_transport_station2.json");
    performSaveObjectRequest("tests/data/mass_transit/create_transport_thread_station1_station2.json");
    {
        auto branchCtx = BranchContextFacade::acquireRead(0, "");
        ObjectsCache cache(branchCtx, boost::none);
        auto route =  cache.getExisting(tests::getObjRevisionId(CATEGORY_TRANSPORT_BUS_ROUTE).objectId());
        WIKI_TEST_REQUIRE(route);
        UNIT_ASSERT_EQUAL(route->slaveRelations().range(ROLE_ASSIGNED).size(), 2);
    }
    performSaveObjectRequest("tests/data/mass_transit/remove_station1_from_thread.json");
    {
        auto branchCtx = BranchContextFacade::acquireRead(0, "");
        ObjectsCache cache(branchCtx, boost::none);
        auto route =  cache.getExisting(tests::getObjRevisionId(CATEGORY_TRANSPORT_BUS_ROUTE).objectId());
        WIKI_TEST_REQUIRE(route);
        UNIT_ASSERT_EQUAL(route->slaveRelations().range(ROLE_ASSIGNED).size(), 1);
    }
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
