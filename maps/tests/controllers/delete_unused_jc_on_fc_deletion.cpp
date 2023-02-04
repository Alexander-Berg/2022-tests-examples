#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/edit_options.h>
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

Y_UNIT_TEST_SUITE(delete_unused_jc_on_fc_deletion)
{
WIKI_FIXTURE_TEST_CASE(test_delete_unused_jc_on_fc_deletion, EditorTestFixture)
{
    //Create object for test
    performSaveObjectRequest("tests/data/create_ad_fc_el_el.json");

    TOIds junctionIds;
    TOid fcId{0};
    {//read and check state
        auto branchCtx = BranchContextFacade::acquireRead(
            0, "");
        ObjectsCache cache(branchCtx, boost::none);
        auto revs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
            revision::filters::Attr("cat:ad_jc").defined());
        WIKI_TEST_REQUIRE_EQUAL(revs.size(), 2);
        for (const auto& rev : revs) {
            auto jcId = rev.objectId();
            auto object = cache.getExisting(jcId);
            WIKI_TEST_REQUIRE_EQUAL(object->categoryId(), "ad_jc");
            WIKI_TEST_REQUIRE(object->state() == GeoObject::State::Draft);
            junctionIds.insert(jcId);
        }
        auto fcRevs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
            revision::filters::Attr("cat:ad_fc").defined());
        WIKI_TEST_REQUIRE_EQUAL(fcRevs.size(), 1);
        fcId = fcRevs.begin()->objectId();
    }
    {//Delete ad_fc
        ObjectsUpdateState::Request deleteRequest(
            {TESTS_USER, {}},
            fcId,
            "deleted",
            0, "",
            common::FormatType::JSON
        );
        UNIT_ASSERT_NO_EXCEPTION(ObjectsUpdateState(makeObservers<>(), deleteRequest)());
    }
    {//check that junctions are deleted
        auto branchCtx = BranchContextFacade::acquireRead(
            0, "");
        ObjectsCache cache(branchCtx, boost::none);
        for (const auto& jcId : junctionIds) {
            auto object = cache.getExisting(jcId);
            UNIT_ASSERT(object->isDeleted());
        }
    }
}
} //Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
