#include <maps/wikimap/mapspro/services/editor/src/branch_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/objects_cache.h>
#include <maps/wikimap/mapspro/services/editor/src/observers/view_syncronizer.h>
#include <maps/wikimap/mapspro/services/editor/src/revisions_facade.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/json/include/value.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(get_topology)
{
WIKI_FIXTURE_TEST_CASE(test_get_object_topology, EditorTestFixture)
{
    //Create object for test
    performSaveObjectRequest(
        "tests/data/create_ad_for_get_topology.json",
        makeObservers<ViewSyncronizer>());

    //Get id of just created ad_fc
    TOid adfcId = 0;
    {
        auto branchCtx = BranchContextFacade::acquireRead(
            0, "");
        ObjectsCache cache(branchCtx, boost::none);
        auto revs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
            revision::filters::Attr("cat:ad_fc").defined());
        WIKI_TEST_REQUIRE_EQUAL(revs.size(), 1);
        adfcId = revs.begin()->objectId();
    }
    //Get topology for that ad_fc
    {
        GetTopology::Request getRequest {
            adfcId,
            "",
            "37.51974887249752,55.72735012314865,37.552900975921595,55.73877964006198",
            0
        };
        GetTopology controller(getRequest);
        auto result = controller();
        UNIT_ASSERT(result->isComplete);
        UNIT_ASSERT_EQUAL(result->linearElements.size(), 1);
        UNIT_ASSERT_EQUAL(result->junctions.size(), 1);
        UNIT_ASSERT_EQUAL(result->adjacentLinearElements.size(), 0);

        auto formatter = Formatter::create(
            common::FormatType::JSON,
            make_unique<TestFormatterContext>());
        validateJsonResponse((*formatter)(*result), "GetTopology");
    }

}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
