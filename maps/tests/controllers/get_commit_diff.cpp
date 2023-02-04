#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(get_commit_diff)
{
WIKI_FIXTURE_TEST_CASE(test_commit_geom_diff, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_test_country.json");

    auto adRevisionId = getObjRevisionId("ad");

    auto result = performJsonGetRequest<GetCommitDiff>(
            adRevisionId.commitId(),
            adRevisionId.objectId(),
            TESTS_USER,
            /*dbToken=*/ "",
            revision::TRUNK_BRANCH_ID);

    validateJsonResponse(result, "GetCommitDiff");
}

WIKI_FIXTURE_TEST_CASE(test_commit_update_relations_diff, EditorTestFixture)
{
    performObjectsImport("tests/data/two_threads.json", db.connectionString());
    performAndValidateJson<ObjectsUpdateRelations>(
        loadFile("tests/data/two_threads_update_relations.json"),
        UserContext(TESTS_USER, {}),
        revision::TRUNK_BRANCH_ID,
        boost::none, /* feedbackTaskId */
        common::FormatType::JSON);
    auto result = performJsonGetRequest<GetCommitDiff>(
            TCommitId(3),
            TOid(0),
            TESTS_USER,
            "",
            revision::TRUNK_BRANCH_ID);
    auto resultParsed = json::Value::fromString(result);
    UNIT_ASSERT(resultParsed["modified"]["geometry"]["before"][0].isObject());
    UNIT_ASSERT(resultParsed["modified"]["geometry"]["after"][0].isObject());
}

} //Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
