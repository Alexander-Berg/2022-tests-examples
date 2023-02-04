#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

revision::Commit
getHeadCommit()
{
    auto branchCtx = BranchContextFacade::acquireRead(revision::TRUNK_BRANCH_ID, "");
    ObjectsCache cache(branchCtx, boost::none);
    return cache.revisionsFacade().loadHeadCommit();
}

Y_UNIT_TEST_SUITE(save_with_commit_attributes)
{
WIKI_FIXTURE_TEST_CASE(test_save_with_commit_attributes, EditorTestFixture)
{
    const std::string& NO_VALUE_ATTRIBUTE_SHOULD_NOT_BE_STORED = "no-value-attribute-should-not-be-stored";
    const std::string& BLD_RECOGNITION_TASK_ID = "bld-recognition-task-id";
    const std::string& BLD_RECOGNITION_TASK_ID_VALUE = "12345678902898345";

    auto parser = SaveObjectParser();
    auto requestBody = loadFile("tests/data/save_3_bld_request.json");
    parser.parse(common::FormatType::JSON, requestBody);
    const auto noObservers = makeObservers<>();
    UNIT_ASSERT_NO_EXCEPTION(
        performRequest<SaveObject>(
            noObservers,
            UserContext(TESTS_USER, {}),
            true,
            revision::TRUNK_BRANCH_ID,
            parser.objects(),
            parser.editContexts(),
            SaveObject::IsLocalPolicy::Manual,
            boost::none,
            StringMap({
                {BLD_RECOGNITION_TASK_ID, BLD_RECOGNITION_TASK_ID_VALUE},
                {NO_VALUE_ATTRIBUTE_SHOULD_NOT_BE_STORED, ""}
            }),
            requestBody
        )
    );
    auto commit = getHeadCommit();
    UNIT_ASSERT(!commit.attributes().count(NO_VALUE_ATTRIBUTE_SHOULD_NOT_BE_STORED));
    auto it = commit.attributes().find(BLD_RECOGNITION_TASK_ID);
    WIKI_TEST_REQUIRE(it != commit.attributes().end());
    UNIT_ASSERT_EQUAL(it->second, BLD_RECOGNITION_TASK_ID_VALUE);
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
