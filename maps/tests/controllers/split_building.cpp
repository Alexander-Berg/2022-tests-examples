#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/json/include/builder.h>
#include <maps/libs/json/include/value.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(split_building)
{
WIKI_FIXTURE_TEST_CASE(test_split_building, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_bld_for_splitting.json");

    performSaveObjectRequest("tests/data/split_bld.json");

    auto branchCtx = BranchContextFacade::acquireRead(0, "");
    ObjectsCache cache(branchCtx, boost::none);
    auto bldRevisions = cache.revisionsFacade().snapshot().revisionIdsByFilter(
            revision::filters::Attr("cat:bld").defined());
    UNIT_ASSERT_EQUAL(bldRevisions.size(), 4);
}

WIKI_FIXTURE_TEST_CASE(test_split_building_thorn, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_bld_for_splitting.json");

    UNIT_CHECK_GENERATED_EXCEPTION(
        performSaveObjectRequest("tests/data/split_bld_thorn.json"),
        LogicException);
}

WIKI_FIXTURE_TEST_CASE(test_split_building_small_area, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_bld_for_splitting.json");

    UNIT_CHECK_GENERATED_EXCEPTION(
        performSaveObjectRequest("tests/data/split_bld_small_area.json"),
        LogicException);
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
