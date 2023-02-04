#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/create_intersections_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/json/include/value.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(create_intersections_vegetation)
{
WIKI_FIXTURE_TEST_CASE(test_create_intersections_vegetation, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_vegetation_for_intersections.json");

    {
        auto parser = CreateIntersectionsParser();
        auto request = loadFile("tests/data/create_intersections_vegetation.json");
        validateJsonRequest(request, "CreateIntersections");
        parser.parse(common::FormatType::JSON, request);
        WIKI_TEST_REQUIRE_EQUAL(parser.objects().size(), 1);// one linestring
        CreateIntersections::Request contrRequest {
                {TESTS_USER, {}},
                0,
                parser.objects(),
                parser.editContext(),
                /*feedbackTaskId*/ boost::none
            };
        const auto noObservers = makeObservers<>();
        CreateIntersections controller(noObservers, contrRequest);
        auto formatter = Formatter::create(common::FormatType::JSON,
            make_unique<TestFormatterContext>());
        std::string result;
        UNIT_ASSERT_NO_EXCEPTION(result = (*formatter)(*controller()));
        validateJsonResponse(result, "CreateIntersections");
        json::Value parsedResult = json::Value::fromString(result);
        WIKI_TEST_REQUIRE(parsedResult["splitObjectIds"].size() == 1);
        WIKI_TEST_REQUIRE_EQUAL(parsedResult["splitObjectIds"][0]["parts"].size(), 3);
        WIKI_TEST_REQUIRE(parsedResult["geoObjects"].size() == 1);
        WIKI_TEST_REQUIRE(
            parsedResult["geoObjects"][0]["geometry"]["type"].as<std::string>() == "LineString");
        WIKI_TEST_REQUIRE_EQUAL(parsedResult["geoObjects"][0]["geometry"]["coordinates"].size(), 6);
    }
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
