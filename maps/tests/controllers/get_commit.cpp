#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(get_commit)
{
WIKI_FIXTURE_TEST_CASE(test_get_commit, EditorTestFixture)
{
    auto feedbackTask = addFeedbackTask();

    performSaveObjectRequest("tests/data/create_test_country.json");
    auto adRevisionId = getObjRevisionId("ad");
    bindCommitToFeedbackTask(adRevisionId.commitId(), feedbackTask.id());

    auto jsonResult = performJsonGetRequest<GetCommit>(
            adRevisionId.commitId(),
            TESTS_USER,
            /*branchId=*/ static_cast<TBranchId>(0),
            /*dbToken=*/ "");
    validateJsonResponse(jsonResult, "GetCommit");

    auto xmlResult = performXmlGetRequest<GetCommit>(
            adRevisionId.commitId(),
            TESTS_USER,
            /*branchId=*/ static_cast<TBranchId>(0),
            /*dbToken=*/ "");
    validateXmlResponse(xmlResult);
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
