#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/revisions_facade.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(save_delete_unused)
{
WIKI_FIXTURE_TEST_CASE(test_parser_required_pair, EditorTestFixture)
{
    executeSqlFile("tests/sql/2_rd_el_with_common_rd_jc.sql");

    //create cond
    performSaveObjectRequest("tests/data/cond_complete_required_pair_attr.xml");

    TOid condDtId = 0;
    {//read cond_dt and check state
        auto branchCtx = BranchContextFacade::acquireRead(
            0, "");
        ObjectsCache cache(branchCtx, boost::none);
        auto revs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
            revision::filters::Attr("cat:cond_dt").defined());
        WIKI_TEST_REQUIRE_EQUAL(revs.size(), 1);
        condDtId = revs.begin()->objectId();
        auto object = cache.getExisting(condDtId);
        WIKI_TEST_REQUIRE(object->categoryId() == "cond_dt");
        WIKI_TEST_REQUIRE(object->state() == GeoObject::State::Draft);
    }

    //update cond with no cond_dt
    performSaveObjectRequest("tests/data/update_cond_no_cond_dt.xml");

    {//read cond_dt and check state, should be deleted
        auto branchCtx = BranchContextFacade::acquireRead(
            0, "");
        ObjectsCache cache(branchCtx, boost::none);
        auto object = cache.getExisting(condDtId);
        WIKI_TEST_REQUIRE(object->categoryId() == "cond_dt");
        UNIT_ASSERT(object->isDeleted());
    }
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
