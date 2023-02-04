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

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(create_ad_el_and_comment)
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

    const auto objectId = revisionId.objectId();
    const auto TEXT = "test";
    const auto commentType = social::CommentType::Info;

    auto json = performAndValidateJson<CommentsCreate>(
        UserContext(TESTS_USER, {}),
        commentType,
        TEXT,
        (TCommitId)0,  // commitId
        objectId,
        std::nullopt   // feedbackTaskId
    );
    UNIT_ASSERT(!json["token"].toString().empty());
    UNIT_ASSERT(json["token"].toString().find("trunk") == std::string::npos);

    const auto& jsonComment = json["comment"];
    UNIT_ASSERT(jsonComment.isObject());
    UNIT_ASSERT(!jsonComment["id"].toString().empty());
    UNIT_ASSERT(!jsonComment["createdAt"].toString().empty());
    UNIT_ASSERT_STRINGS_EQUAL(jsonComment["createdBy"].toString(), std::to_string(TESTS_USER));
    UNIT_ASSERT_STRINGS_EQUAL(jsonComment["objectId"].toString(), std::to_string(objectId));
    UNIT_ASSERT_STRINGS_EQUAL(jsonComment["type"].toString(), boost::lexical_cast<std::string>(commentType));
    UNIT_ASSERT_STRINGS_EQUAL(jsonComment["data"].toString(), TEXT);

    auto commitIdFromComment = jsonComment["commitId"].toString();
    auto commitIdFromRevision = std::to_string(revisionId.commitId());

    UNIT_ASSERT_STRINGS_EQUAL(commitIdFromComment, commitIdFromRevision);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
