#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/revisions_facade.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/json/include/value.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(delete_simple_object_cascade)
{
WIKI_FIXTURE_TEST_CASE(test_delete_simple_object_cascade, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_simple_vegetation.json");

    TOid vegId{0};
    TOid vegFcId{0};
    TOid vegElId{0};
    {//Read ids
        auto branchCtx = BranchContextFacade::acquireRead(0, "");
        ObjectsCache cache(branchCtx, boost::none);
        auto revsVeg = cache.revisionsFacade().snapshot().revisionIdsByFilter(
            revision::filters::Attr("cat:vegetation").defined());
        WIKI_TEST_REQUIRE_EQUAL(revsVeg.size(), 1);
        vegId = revsVeg.begin()->objectId();
        auto revsVegFc = cache.revisionsFacade().snapshot().revisionIdsByFilter(
            revision::filters::Attr("cat:vegetation_fc").defined());
        WIKI_TEST_REQUIRE_EQUAL(revsVegFc.size(), 1);
        vegFcId = revsVegFc.begin()->objectId();
        auto revsVegEl = cache.revisionsFacade().snapshot().revisionIdsByFilter(
            revision::filters::Attr("cat:vegetation_el").defined());
        WIKI_TEST_REQUIRE_EQUAL(revsVegEl.size(), 1);
        vegElId = revsVegEl.begin()->objectId();
    }
    {//Now delete vegetationId and expect vegetationElId to be deleted as well
        ObjectsUpdateState::Request deleteRequest(
            {TESTS_USER, {}},
            vegId,
            "deleted",
            0, "",
            common::FormatType::JSON
        );
        UNIT_ASSERT_NO_EXCEPTION(ObjectsUpdateState(makeObservers<>(), deleteRequest)());
    }
    {//Check all objects to be deleted
        auto branchCtx = BranchContextFacade::acquireRead(0, "");
        ObjectsCache cache(branchCtx, boost::none);
        UNIT_ASSERT(cache.getExisting(vegId)->isDeleted());
        UNIT_ASSERT(cache.getExisting(vegFcId)->isDeleted());
        UNIT_ASSERT(cache.getExisting(vegElId)->isDeleted());
    }
}


WIKI_FIXTURE_TEST_CASE(test_contour_cascade_with_cross_editing, EditorTestFixture)
{
    UNIT_ASSERT_NO_EXCEPTION(
        performObjectsImport("tests/data/vegetation_with_two_contours.json", db.connectionString()));
    UNIT_ASSERT_NO_EXCEPTION(
        performSaveObjectRequest("tests/data/delete_external_contour_and_edit_internal_geom.json"));
    auto branchCtx = BranchContextFacade::acquireRead(0, "");
    ObjectsCache cache(branchCtx, boost::none);
    auto revsVeg = cache.revisionsFacade().snapshot().revisionIdsByFilter(
        revision::filters::Attr("cat:vegetation_fc").defined() &&
        revision::filters::ObjRevAttr::isNotDeleted());
    UNIT_ASSERT_EQUAL(revsVeg.size(), 1);
    auto revsVegEl = cache.revisionsFacade().snapshot().revisionIdsByFilter(
        revision::filters::Attr("cat:vegetation_el").defined() &&
        revision::filters::ObjRevAttr::isNotDeleted());
    UNIT_ASSERT_EQUAL(cache.getExisting(revsVeg[0].objectId())->slaveRelations().range().size(),
        revsVegEl.size());
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
