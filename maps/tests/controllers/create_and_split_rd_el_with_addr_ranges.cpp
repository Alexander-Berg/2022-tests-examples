#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(create_and_split_rd_el_with_addr_ranges)
{
const auto BRANCH_ID = revision::TRUNK_BRANCH_ID;

WIKI_FIXTURE_TEST_CASE(test_create_and_split_rd_el_with_addr_ranges, EditorTestFixture)
{
    //Create horizontal road element split in to 3 parts.
    performSaveObjectRequest("tests/data/create_and_split_rd_el_with_addr_ranges.json");
    auto branchCtx = BranchContextFacade::acquireRead(BRANCH_ID, "");
    ObjectsCache cache(branchCtx, boost::none);
    auto revs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
        revision::filters::Attr("cat:rd_el").defined());
    WIKI_TEST_REQUIRE_EQUAL(revs.size(), 3);
    std::vector<ObjectPtr> rdEls;
    for (const auto& rev : revs) {
        rdEls.push_back(cache.getExisting(rev.objectId()));
    }
    std::sort(rdEls.begin(), rdEls.end(),
        [](const ObjectPtr& o1, const ObjectPtr& o2)
        {
            auto env1 = o1->envelope();
            auto env2 = o2->envelope();
            return env1.getMinX() < env2.getMinX();
        });
    UNIT_ASSERT_EQUAL(rdEls[0]->attributes().value("rd_el:addr_a_1"), "1");
    UNIT_ASSERT_EQUAL(rdEls[0]->attributes().value("rd_el:addr_a_2"), "2");
    UNIT_ASSERT_EQUAL(rdEls[0]->attributes().value("rd_el:addr_b_1"), "1");
    UNIT_ASSERT_EQUAL(rdEls[0]->attributes().value("rd_el:addr_b_2"), "2");

    UNIT_ASSERT_EQUAL(rdEls[1]->attributes().value("rd_el:addr_a_1"), "1");
    UNIT_ASSERT_EQUAL(rdEls[1]->attributes().value("rd_el:addr_a_2"), "2");
    UNIT_ASSERT_EQUAL(rdEls[1]->attributes().value("rd_el:addr_b_1"), "3");
    UNIT_ASSERT_EQUAL(rdEls[1]->attributes().value("rd_el:addr_b_2"), "4");

    UNIT_ASSERT_EQUAL(rdEls[2]->attributes().value("rd_el:addr_a_1"), "3");
    UNIT_ASSERT_EQUAL(rdEls[2]->attributes().value("rd_el:addr_a_2"), "4");
    UNIT_ASSERT_EQUAL(rdEls[2]->attributes().value("rd_el:addr_b_1"), "5");
    UNIT_ASSERT_EQUAL(rdEls[2]->attributes().value("rd_el:addr_b_2"), "6");

}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
