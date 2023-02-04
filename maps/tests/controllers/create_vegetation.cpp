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

Y_UNIT_TEST_SUITE(create_vegetation)
{
WIKI_FIXTURE_TEST_CASE(test_create_simple_vegetation, EditorTestFixture)
{
    std::string result = performSaveObjectRequest("tests/data/create_simple_vegetation.json");

    auto outputParser = SaveObjectParser();
    UNIT_ASSERT_NO_EXCEPTION(outputParser.parse(common::FormatType::JSON, result));
    WIKI_TEST_REQUIRE_EQUAL(outputParser.objects().size(), 1); // veg
    const auto& objectData = *outputParser.objects().begin();
    UNIT_ASSERT_EQUAL(objectData.categoryId(), "vegetation");
    auto parsedResult = json::Value::fromString(result);
    std::string geomType;
    //Check that the result is an expanded output
    //of just created simple object
    UNIT_ASSERT_NO_EXCEPTION(geomType =
        parsedResult["geoObjects"][0]
        ["slaves"]["part"]["geoObjects"][0]
        ["slaves"]["part"]["geoObjects"][0]["geometry"]["type"].as<std::string>());
    UNIT_ASSERT_EQUAL(geomType, "LineString");
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
