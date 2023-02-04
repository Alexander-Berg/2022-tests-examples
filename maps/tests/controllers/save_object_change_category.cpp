#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/revisions_facade.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(save_object_change_category)
{
WIKI_FIXTURE_TEST_CASE(test_create_poi_food_and_switch_to_poi_other_and_get_diff, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_poi_food.json");
    WIKI_TEST_REQUIRE(getObject("poi_food"));
    auto id = getObject("poi_food")->id();
    performSaveObjectRequest("tests/data/update_poi.json");
    UNIT_ASSERT(!getObject("poi_food"));
    auto updatedObject = getObject("poi_other");
    UNIT_ASSERT(updatedObject);
    UNIT_ASSERT_EQUAL(updatedObject->id(), id);
    auto result = performJsonGetRequest<GetCommitDiff>(
            updatedObject->revision().commitId(),
            updatedObject->id(),
            TESTS_USER,
            /*dbToken=*/ "",
             revision::TRUNK_BRANCH_ID);
    validateJsonResponse(result, "GetCommitDiff");
    auto diffJson = maps::json::Value::fromString(result);
    UNIT_ASSERT_EQUAL(diffJson["modified"]["categoryId"]["before"].as<std::string>(),
        "poi_food");
    UNIT_ASSERT_EQUAL(diffJson["modified"]["categoryId"]["after"].as<std::string>(),
        "poi_other");
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
