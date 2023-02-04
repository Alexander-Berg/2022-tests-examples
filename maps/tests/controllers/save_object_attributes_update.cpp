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
    WIKI_TEST_REQUIRE_EQUAL(revs.begin()->objectId(), 3);
    auto rdEl =  cache.getExisting(3);
    WIKI_TEST_REQUIRE(rdEl);
    return rdEl;
}

ObjectPtr
getAd()
{
    auto branchCtx = BranchContextFacade::acquireRead(
        0, "");
    ObjectsCache cache(branchCtx, boost::none);
    auto revs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
    revision::filters::Attr("cat:ad").defined());
    WIKI_TEST_REQUIRE_EQUAL(revs.size(), 1);
    auto id  = revs.begin()->objectId();
    auto ad =  cache.getExisting(id);
    WIKI_TEST_REQUIRE(ad);
    WIKI_TEST_REQUIRE_EQUAL(id, 1); //for update xml
    return ad;
}
}//namespace
Y_UNIT_TEST_SUITE(save_object_attributes_update)
{
WIKI_FIXTURE_TEST_CASE(test_create_rd_el_and_update_no_fow, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_rd_el.json");
    {
        auto rdEl = getRdEl();
        UNIT_ASSERT_EQUAL(rdEl->attributes().value("rd_el:fow"), "11");
        UNIT_ASSERT_EQUAL(rdEl->attributes().value("rd_el:fc"), "9");
    }
    performSaveObjectRequest("tests/data/update_rd_el_no_fow.json");
    {
        auto rdEl = getRdEl();
        UNIT_ASSERT_EQUAL(rdEl->attributes().value("rd_el:fow"), "11");
        UNIT_ASSERT_EQUAL(rdEl->attributes().value("rd_el:fc"), "5");
    }
}

WIKI_FIXTURE_TEST_CASE(test_reset_multivalue_xml, EditorTestFixture)
{
    {
        std::string result = performSaveObjectRequest("tests/data/create_ad_with_recogn.xml");
        auto ad = getAd();
        UNIT_ASSERT_EQUAL(ad->attributes().values("ad:recognition").size(), 2);
    }
    {
        std::string result = performSaveObjectRequest("tests/data/reset_ad_with_recogn.xml");
        auto ad = getAd();
        UNIT_ASSERT_EQUAL(ad->attributes().value("ad:recognition"), s_emptyString);
    }
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
