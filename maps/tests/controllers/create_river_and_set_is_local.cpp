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
/*
    Test country name is set as official, while rivers is not
    After saving rivers name expected to be marked as official
    as well
*/
Y_UNIT_TEST_SUITE(create_river_and_set_is_local)
{
WIKI_FIXTURE_TEST_CASE(test_create_river_and_set_is_local, EditorTestFixture)
{
    const auto observers = makeObservers<ViewSyncronizer>();

    performSaveObjectRequest("tests/data/create_test_country.json", observers);
    {
        auto branchCtx = BranchContextFacade::acquireRead(revision::TRUNK_BRANCH_ID, "");
        ObjectsCache cache(branchCtx, boost::none);
        auto revs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
            revision::filters::Attr("cat:ad").defined());
        WIKI_TEST_REQUIRE_EQUAL(revs.size(), 1);
        auto id  = revs.begin()->objectId();
        auto contry = cache.getExisting(id);
        WIKI_TEST_REQUIRE(contry);
        auto nameAttr = contry->tableAttributes().find("ad_nm");
        WIKI_TEST_REQUIRE_EQUAL(nameAttr.numRows(), 1);
        UNIT_ASSERT_EQUAL(nameAttr.value(0, "ad_nm:is_local"), TRUE_VALUE);
    }

    executeSqlFile("tests/sql/fill_test_contry_contour_objects_geom.sql");
    performSaveObjectRequest("tests/data/create_test_river.json", observers);
    {
        auto branchCtx = BranchContextFacade::acquireRead(revision::TRUNK_BRANCH_ID, "");
        ObjectsCache cache(branchCtx, boost::none);
        auto hydroIds = objectIdsByCategory(cache, "hydro_ln");
        WIKI_TEST_REQUIRE_EQUAL(hydroIds.size(), 1);
        auto id  = *hydroIds.begin();
        auto river = cache.getExisting(id);
        WIKI_TEST_REQUIRE(river);
        auto nameAttr = river->tableAttributes().find("hydro_nm");
        WIKI_TEST_REQUIRE_EQUAL(nameAttr.numRows(), 1);
        UNIT_ASSERT_EQUAL(nameAttr.value(0, "hydro_nm:is_local"), TRUE_VALUE);
    }
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
