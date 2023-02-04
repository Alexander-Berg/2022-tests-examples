#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(test_update_seq_nums)
{
WIKI_FIXTURE_TEST_CASE(test_update_seq_nums, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/update_seq_nums/create_rd_el.json");
    performSaveObjectRequest("tests/data/update_seq_nums/create_cond.json");
    performSaveObjectRequest("tests/data/update_seq_nums/split_first_to_rd_el.json");

    auto branchCtx = BranchContextFacade::acquireRead(
        0, "");
    ObjectsCache cache(branchCtx, boost::none);
    auto revs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
    revision::filters::Attr("cat:cond").defined());
    WIKI_TEST_REQUIRE_EQUAL(revs.size(), 1);
    auto id  = revs.begin()->objectId();
    auto cond =  cache.getExisting(id);
    WIKI_TEST_REQUIRE(cond);
    for (const auto& rel : cond->slaveRelations().range(ROLE_TO)) {
        UNIT_ASSERT(
            (rel.geom()->getNumPoints() == 3 && rel.seqNum() == 0)
            ||
            (rel.geom()->getNumPoints() == 4 && rel.seqNum() == 1)
            || rel.seqNum() == 2);
    }
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
