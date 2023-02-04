#include <maps/wikimap/mapspro/services/editor/src/actions/make_junction.h>
#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>

#include <yandex/maps/wiki/topo/exception.h>

namespace maps::wiki::tests {
namespace {
ObjectPtr
getRdEl()
{
    auto branchCtx = BranchContextFacade::acquireRead(
        0, "");
    ObjectsCache cache(branchCtx, boost::none);
    auto revs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
    revision::filters::Attr("cat:rd_el").defined());
    WIKI_TEST_REQUIRE_EQUAL(revs.size(), 1);
    WIKI_TEST_REQUIRE_EQUAL(revs.begin()->objectId(), 3);
    auto rdEl =  cache.getExisting(3);
    WIKI_TEST_REQUIRE(rdEl);
    return rdEl;
}
}//namespace
Y_UNIT_TEST_SUITE(make_junction)
{
WIKI_FIXTURE_TEST_CASE(test_make_junction, EditorTestFixture)
{
    const auto noObservers = makeObservers<>();

    performSaveObjectRequest("tests/data/create_rd_el.json");
    auto rdEl = getRdEl();
    //hit existing
    {
        ObjectsQueryJunction::Request contrRequest {
            "{\"type\":\"Point\",\"coordinates\":[37.76079899666591,55.76677622328369]}",
            21,
            rdEl->id(),
            {TESTS_USER, {}},
            revision::TRUNK_BRANCH_ID
        };
        UNIT_CHECK_GENERATED_EXCEPTION(ObjectsQueryJunction(noObservers, contrRequest)(), LogicException);
    }
    //hit in the middle
    {
        ObjectsQueryJunction::Request contrRequest {
            "{\"type\":\"Point\",\"coordinates\":[37.76365286705776,55.76684880536747]}",
            21,
            rdEl->id(),
            {TESTS_USER, {}},
            revision::TRUNK_BRANCH_ID
        };
        ObjectsQueryJunction controller(noObservers, contrRequest);
        TOid junctionId = 0;
        UNIT_ASSERT_NO_EXCEPTION(junctionId = controller()->junctionId);
        UNIT_ASSERT_EQUAL(junctionId, 11);
    }
    //hit nowhere
    {
        ObjectsQueryJunction::Request contrRequest {
            "{\"type\":\"Point\",\"coordinates\":[47.76079899666591,65.76677622328369]}",
            21,
            rdEl->id(),
            {TESTS_USER, {}},
            revision::TRUNK_BRANCH_ID
        };
        UNIT_CHECK_GENERATED_EXCEPTION(
            ObjectsQueryJunction(noObservers, contrRequest)(),
            LogicException
        );
    }
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
