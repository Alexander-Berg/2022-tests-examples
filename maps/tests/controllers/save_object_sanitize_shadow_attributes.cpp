#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/revisions_facade.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

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
    auto rdEl =  cache.getExisting(revs.begin()->objectId());
    WIKI_TEST_REQUIRE(rdEl);
    return rdEl;
}
} //namespace
Y_UNIT_TEST_SUITE(save_object_sanitize_shadow_attributes)
{
WIKI_FIXTURE_TEST_CASE(test_create_rd_el_and_sanitize_shadow, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/rd_el_with_shadow_attributes.xml");
    {
        auto rdEl = getRdEl();
        UNIT_ASSERT_EQUAL(rdEl->attributes().value("rd_el:fc_shadow"), "7");
        UNIT_ASSERT_EQUAL(rdEl->attributes().value("rd_el:fow_shadow"), "0");
        UNIT_ASSERT_EQUAL(rdEl->attributes().value("rd_el:oneway_shadow"), "B");
        UNIT_ASSERT_EQUAL(rdEl->attributes().value("rd_el:struct_type_use_shadow"), "1");
        UNIT_ASSERT_EQUAL(rdEl->attributes().value("rd_el:access_id_shadow"), "");
    }
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
