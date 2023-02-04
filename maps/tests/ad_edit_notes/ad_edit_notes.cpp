#include <maps/wikimap/mapspro/services/editor/src/commit.h>
#include <maps/wikimap/mapspro/services/editor/src/edit_notes.h>
#include <maps/wikimap/mapspro/services/editor/src/revisions_facade.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/tests_common.h>

#include <maps/libs/json/include/value.h>

namespace maps::wiki::tests {

revision::Commit
loadCommit(TCommitId commitId)
{
    auto branchCtx = BranchContextFacade::acquireRead(0, "");
    ObjectsCache cache(branchCtx, boost::none);
    return revision::Commit::load(branchCtx.txnCore(),commitId);
}

Y_UNIT_TEST_SUITE(ad_edit_notes)
{
WIKI_FIXTURE_TEST_CASE(test_ad_edit_notes, EditorTestFixture)
{
    //Create object for test
    performSaveObjectRequest("tests/data/create_ad_for_notes_test.json");
    WIKI_TEST_REQUIRE(getObject("ad") && getObject("ad")->id() == 1);
    WIKI_TEST_REQUIRE(getObject("ad_fc") && getObject("ad_fc")->id() ==  2);
    WIKI_TEST_REQUIRE(getObject("ad_el") && getObject("ad_el")->id() == 7);

    performSaveObjectRequest("tests/data/update_ad_for_notes_test.json");
    auto adNotes = commitAttributesObjectEditNotes(loadCommit(2).attributes(), 1);
    UNIT_ASSERT(std::string::npos != adNotes.find(edit_notes::MODIFIED_ATTRIBUTES_NAMES_ALTERNATIVE_ADDED));
    UNIT_ASSERT(std::string::npos != adNotes.find(edit_notes::MODIFIED_ATTRIBUTES_NAMES_OFFICIAL));
    UNIT_ASSERT(std::string::npos != adNotes.find(edit_notes::MODIFIED_GEOMETRY_CONTOURS));
}
} //Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
