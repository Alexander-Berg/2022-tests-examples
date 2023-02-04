#include <maps/wikimap/mapspro/services/editor/src/actions/commits/revert.h>
#include <maps/wikimap/mapspro/services/editor/src/commit.h>
#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/json/include/value.h>
#include <maps/libs/pgpool/include/pgpool3.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>

namespace maps::wiki::tests {

namespace {

TId
revert(TCommitId commitId)
{
    auto json = performAndValidateJson<CommitsRevert>(
        UserContext{TESTS_USER, {}},
        commitId,
        RevertReason::Vandalism,
        /*feedbackTaskId*/ boost::none);
    WIKI_TEST_REQUIRE(!json["token"].toString().empty());
    const auto& jsonCommit = json["commit"];
    WIKI_TEST_REQUIRE(jsonCommit.isObject());
    UNIT_ASSERT_EQUAL(jsonCommit["uid"].toString(), std::to_string(TESTS_USER));
    UNIT_ASSERT(!jsonCommit["date"].toString().empty());
    UNIT_ASSERT_EQUAL(jsonCommit["action"].toString(), common::COMMIT_PROPVAL_COMMIT_REVERTED);
    UNIT_ASSERT_EQUAL(jsonCommit["sourceBranchId"].toString(), "0");
    UNIT_ASSERT_EQUAL(jsonCommit["actionNotes"]["commit-reverted"]["revertReason"].toString(), "vandalism");
    return std::stoll(jsonCommit["id"].toString());
}

void
checkGetResult(const json::Value& json, const std::vector<TCommitId>& commitIds)
{
    WIKI_TEST_REQUIRE(json.isObject());
    UNIT_ASSERT_EQUAL(json["page"].toString(), "1");
    UNIT_ASSERT_EQUAL(json["perPage"].toString(), "10");
    UNIT_ASSERT_EQUAL(json["totalCount"].toString(), std::to_string(commitIds.size()));
    const auto& jsonCommits = json["commits"];
    for (size_t i = 0; i < commitIds.size(); ++i) {
        UNIT_ASSERT_EQUAL(jsonCommits[i]["id"].toString(), std::to_string(commitIds[i]));
    }
}

}

Y_UNIT_TEST_SUITE(create_ad_el_and_revert)
{
WIKI_FIXTURE_TEST_CASE(test_create_ad_el, EditorTestFixture)
{
    std::string result = performSaveObjectRequest("tests/data/create_ad_el.xml");

    auto outputParser = SaveObjectParser();
    UNIT_ASSERT_NO_EXCEPTION(outputParser.parse(common::FormatType::XML, result));
    WIKI_TEST_REQUIRE_EQUAL(outputParser.objects().size(), 1);
    const auto& objectData = *outputParser.objects().begin();
    auto revisionId = objectData.revision();
    WIKI_TEST_REQUIRE(revisionId.valid());
    auto objectId = revisionId.objectId();

    auto getRevision = [](const revision::RevisionID& revisionId)
    {
        auto work = cfg()->poolCore().masterReadOnlyTransaction();
        revision::RevisionsGateway revGateway(*work);
        return revGateway.reader().loadRevision(revisionId);
    };

    UNIT_ASSERT_EQUAL(getRevision(revisionId).data().deleted, false);
    auto revertCommitId = revert(revisionId.commitId());
    UNIT_ASSERT_EQUAL(getRevision({objectId, revertCommitId}).data().deleted, true);

    // Revert of revert commit:
    auto revertOfRevertedCommitId = revert(revertCommitId);
    UNIT_ASSERT_EQUAL(getRevision({objectId, revertOfRevertedCommitId}).data().deleted, false);

    auto jsonHistory = json::Value::fromString(
        performJsonGetRequest<GetHistory>(
            objectId,
            TESTS_USER,
            "", // token
            (TBranchId)0,
            (size_t)1,  // page
            (size_t)10, // perPage
            GetHistory::RelationsChangePolicy::Show,
            GetHistory::IndirectChangePolicy::Show
        )
    );
    UNIT_ASSERT_EQUAL(jsonHistory["objectId"].toString(), std::to_string(objectId));

    checkGetResult(
        jsonHistory,
        {revertOfRevertedCommitId, revertCommitId, revisionId.commitId()});

    auto jsonDependent = json::Value::fromString(
        performJsonGetRequest<GetDependentCommits>(
            TESTS_USER,
            revisionId.commitId(),
            (size_t)1,  // page
            (size_t)10, // perPage
            ""  //token
        )
    );
    checkGetResult(jsonDependent, {revertOfRevertedCommitId, revertCommitId});
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
