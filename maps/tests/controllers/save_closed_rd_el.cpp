#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/revisions_facade.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <yandex/maps/wiki/topo/exception.h>

#include <geos/geom/LineString.h>
#include <geos/geom/Point.h>

namespace maps::wiki::tests {

namespace {
std::vector<ObjectPtr>
getRdEls()
{
    auto branchCtx = BranchContextFacade::acquireRead(
        0, "");
    ObjectsCache cache(branchCtx, boost::none);
    auto revs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
        revision::filters::Attr("cat:rd_el").defined());
    WIKI_TEST_REQUIRE_EQUAL(revs.size(), 2);
    std::vector<ObjectPtr> rdEls;
    for (const auto& rev : revs) {
        rdEls.push_back(cache.getExisting(rev.objectId()));
    }
    return rdEls;
}
}

Y_UNIT_TEST_SUITE(save_closed_rd_el)
{
WIKI_FIXTURE_TEST_CASE(test_create_closed_rd_el_with_split, EditorTestFixture)
{
    UNIT_ASSERT_NO_EXCEPTION(performSaveObjectRequest("tests/data/create_closed_valid_rd_el.json"));
    auto rdEls = getRdEls();
    auto linestring1 = dynamic_cast<const geos::geom::LineString*>(rdEls.front()->geom().geosGeometryPtr());
    auto linestring2 = dynamic_cast<const geos::geom::LineString*>(rdEls.back()->geom().geosGeometryPtr());
    WIKI_TEST_REQUIRE(linestring1 && linestring2);
    std::unique_ptr<geos::geom::Point> start1(linestring1->getStartPoint());
    std::unique_ptr<geos::geom::Point> end1(linestring1->getEndPoint());
    std::unique_ptr<geos::geom::Point> start2(linestring2->getStartPoint());
    std::unique_ptr<geos::geom::Point> end2(linestring2->getEndPoint());
    UNIT_ASSERT(start1->equalsExact(end2.get()));
    UNIT_ASSERT(start2->equalsExact(end1.get()));
}

WIKI_FIXTURE_TEST_CASE(test_fail_create_closed_rd_el_with_no_split, EditorTestFixture)
{
    UNIT_CHECK_GENERATED_EXCEPTION(
        performSaveObjectRequest("tests/data/create_closed_rd_el.json"),
        LogicExceptionWithLocation);
}

WIKI_FIXTURE_TEST_CASE(test_fail_to_create_closed_rd_el_with_split_close_to_start, EditorTestFixture)
{
    UNIT_CHECK_GENERATED_EXCEPTION(
        performSaveObjectRequest("tests/data/create_closed_rd_el_with_split_close_to_start.json"),
        LogicExceptionWithLocation);
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
