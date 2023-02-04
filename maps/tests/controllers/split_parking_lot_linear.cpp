#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/json/include/builder.h>
#include <maps/libs/json/include/value.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(split_parking_lot_linear)
{
WIKI_FIXTURE_TEST_CASE(test_split_building, EditorTestFixture)
{
    performObjectsImport("tests/data/parking_controlled_zone.json", db.connectionString());
    performSaveObjectRequest("tests/data/create_and_split_parking_lot_linear.json");
    auto branchCtx = BranchContextFacade::acquireRead(0, "");
    ObjectsCache cache(branchCtx, boost::none);
    auto revs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
    revision::filters::Attr("cat:urban_roadnet_parking_lot_linear").defined());
    WIKI_TEST_REQUIRE_EQUAL(revs.size(), 2);
    for (const auto& rev : revs) {
        auto parking = cache.getExisting(rev.objectId());
        UNIT_ASSERT_EQUAL(parking->masterRelations().range().size(), 1);
    }
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
