#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(create_3_bld)
{
GeoObjectCollection
getBlds()
{
    auto branchCtx = BranchContextFacade::acquireRead(revision::TRUNK_BRANCH_ID, "");
    ObjectsCache cache(branchCtx, boost::none);
    auto revs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
    revision::filters::Attr("cat:bld").defined());
    WIKI_TEST_REQUIRE_EQUAL(revs.size(), 3);
    TOIds ids;
    for (const auto& rev : revs) {
        ids.insert(rev.objectId());
    }
    return cache.get(ids);
}

std::string
getCommitDiff(TCommitId commitId, TOid objectId)
{
    return performJsonGetRequest<GetCommitDiff>(
        commitId,
        objectId,
        TESTS_USER,
        "" /* dbToken */,
        revision::TRUNK_BRANCH_ID);
}

void
testResult()
{
    auto bldCollection = getBlds();
    UNIT_ASSERT_EQUAL(bldCollection.size(), 3);
    std::set<std::string> ftTypes;
    std::set<TCommitId> commitIds;
    for (const auto& bld : bldCollection) {
        const auto commitId = bld->revision().commitId();
        commitIds.insert(commitId);
        ftTypes.insert(bld->attributes().value("bld:ft_type_id"));
        const auto commitDiff = getCommitDiff(commitId, bld->id());
        const auto parsedDiff = maps::json::Value::fromString(commitDiff);
        UNIT_ASSERT_EQUAL(parsedDiff["modified"]["geometry"]["after"].size(), 1);
    }
    UNIT_ASSERT_VALUES_EQUAL(commitIds.size(), 3);
    UNIT_ASSERT_EQUAL(ftTypes.size(), 3);
    for (const auto commitId : commitIds) {
        const auto wholeCommitDiff = getCommitDiff(commitId, 0);
        const auto parsedDiff = maps::json::Value::fromString(wholeCommitDiff);
        UNIT_ASSERT_EQUAL(parsedDiff["modified"]["geometry"]["after"].size(), 1);
    }

}

WIKI_FIXTURE_TEST_CASE(test_save_3_bld_at_once_json, EditorTestFixture)
{
    performObjectsImport("tests/data/small_road_graph.json", db.connectionString());
    performSaveObjectRequest("tests/data/save_3_bld_request.json");
    testResult();
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
