#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/observers/view_syncronizer.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <yandex/maps/wiki/revision/branch.h>

namespace maps::wiki::tests {

const auto BRANCH_ID = revision::TRUNK_BRANCH_ID;

Y_UNIT_TEST_SUITE(create_road_and_bind_to_city)
{
WIKI_FIXTURE_TEST_CASE(test_create_road_and_associate_to_locality, EditorTestFixture)
{
    {
        const auto observers = makeObservers<ViewSyncronizer>();
        performSaveObjectRequest("tests/data/create_test_city.json", observers);
        executeSqlFile("tests/sql/fill_ad_contour_objects_geom.sql");
        performSaveObjectRequest("tests/data/create_test_road.json", observers);
    }
    {
        auto branchCtx = BranchContextFacade::acquireRead(
            BRANCH_ID, "");
        ObjectsCache cache(branchCtx, boost::none);
        auto revs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
            revision::filters::Attr("cat:rd").defined());
        WIKI_TEST_REQUIRE_EQUAL(revs.size(), 1);
        auto id  = revs.begin()->objectId();
        auto rd =  cache.getExisting(id);
        WIKI_TEST_REQUIRE(rd);
        UNIT_ASSERT_EQUAL(rd->masterRelations().range(ROLE_ASSOCIATED_WITH).size(), 1);
    }
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
