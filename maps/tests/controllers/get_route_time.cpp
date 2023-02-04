#include <maps/wikimap/mapspro/services/editor/src/actions/routing/get_route_time.h>
#include <maps/wikimap/mapspro/services/editor/src/common.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/get_route_time_parser.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/magic_strings.h>

#include <yandex/maps/wiki/common/string_utils.h>

#include <maps/libs/json/include/builder.h>

#include <algorithm>
#include <initializer_list>
#include <iterator>
#include <sstream>
#include <string>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(get_route_time)
{
namespace {

void checkTime(
        const std::string& test,
        const std::vector<size_t>& resultTime,
        const std::vector<size_t>& expectedTime)
{
    WIKI_TEST_REQUIRE_MESSAGE(
        resultTime.size() == expectedTime.size(),
        "Test: " << test << ", result time differs from expected"
    );

    for (size_t i = 0; i < resultTime.size(); ++i) {
        WIKI_TEST_ASSERT_MESSAGE(
            resultTime[i] == expectedTime[i],
            "Test: " << test << ", time differs at position " << i
                << ", return " << resultTime[i]
                << ", but expected " << expectedTime[i]
        );
    }
}

void test(
        const std::string& name,
        const std::string& jsonRequestPath,
        const std::vector<size_t>& expectedTime)
{
    const std::string jsonRequest = loadFile(jsonRequestPath);

    validateJsonRequest(jsonRequest, GetRouteTime::taskName());

    GetRouteTimeParser parser;
    parser.parse(common::FormatType::JSON, jsonRequest);

    GetRouteTime::Request request {
        TESTS_USER,
        "",
        revision::TRUNK_BRANCH_ID,
        parser.revisionId(),
        parser.categoryId(),
        parser.addElementIds(),
        parser.removeElementIds(),
        parser.threadStopSequence()
    };
    const auto result = *GetRouteTime(makeObservers<>(), request)();

    checkTime(name, result.time, expectedTime);

    auto formatter = Formatter::create(common::FormatType::JSON);
    validateJsonResponse((*formatter)(result), GetRouteTime::taskName());
}

} // namespace

WIKI_FIXTURE_TEST_CASE(test_get_route_time, EditorTestFixture)
{
    performObjectsImport("tests/data/small_road_graph.json", db.connectionString());

    test(
        "test-1, existing thread",
        "tests/data/get_route_time/01.json",
        /* time = */ {95, 50}
    );

    test(
        "test-2, new thread",
        "tests/data/get_route_time/02.json",
        /* time = */ {71, 109}
    );
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
