#include <maps/wikimap/mapspro/services/editor/src/actions/routing/objects_query_path.h>
#include <maps/wikimap/mapspro/services/editor/src/common.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/objects_query_path_parser.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
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

namespace {

void checkElementsById(
    const std::string& test,
    const views::ViewObjects& viewObjects,
    const TOIds& expectedElementIds)
{
    TOIds resultElementIds;
    for (const auto& object: viewObjects) {
        resultElementIds.insert(object.id());
    }

    std::vector<TOid> unexpectedIds;
    std::set_difference(
        resultElementIds.begin(), resultElementIds.end(),
        expectedElementIds.begin(), expectedElementIds.end(),
        std::back_inserter(unexpectedIds)
    );

    std::vector<TOid> missedIds;
    std::set_difference(
        expectedElementIds.begin(), expectedElementIds.end(),
        resultElementIds.begin(), resultElementIds.end(),
        std::back_inserter(missedIds)
    );

    WIKI_TEST_REQUIRE_MESSAGE(
        unexpectedIds.empty() && missedIds.empty(),
        "Test: " << test << ", unexpected elements: " + toString(unexpectedIds) << " "
        "missed elements: " << toString(missedIds)
    );
}

std::string createJsonRequest(std::initializer_list<TOid> elementIds)
{

    json::Builder json;
    json << [&](json::ObjectBuilder request) {
        request[ELEMENT_IDS] << [&](json::ArrayBuilder elementIdsArray) {
            for (const auto& id: elementIds) {
                elementIdsArray << std::to_string(id);
            }
        };
    };

    return json.str();
}

void test(
    const std::string& name,
    const std::string& jsonRequest,
    const TOIds& expectedElementIds)
{
    ObjectsQueryPathParser parser;
    validateJsonRequest(jsonRequest, ObjectsQueryPath::taskName());
    parser.parse(common::FormatType::JSON, jsonRequest);

    ObjectsQueryPath::Request request {
        TESTS_USER,
        "",
        revision::TRUNK_BRANCH_ID,
        parser.elementIds(),
        /* limit = */ 0
    };
    const auto result = *ObjectsQueryPath(makeObservers<>(), request)();

    checkElementsById(name, result.elements, expectedElementIds);

    auto formatter = Formatter::create(common::FormatType::JSON);
    validateJsonResponse((*formatter)(result), ObjectsQueryPath::taskName());
}

} // namespace

Y_UNIT_TEST_SUITE(objects_query_path)
{
WIKI_FIXTURE_TEST_CASE(test_objects_query_path, EditorTestFixture)
{
    performObjectsImport("tests/data/small_road_graph.json", db.connectionString());

    test(
        "Test-1, trivial path",
        createJsonRequest(
            /* elementIds = */ {3403809, 3403809}
        ),
        /* pathElementIds = */ {3403809}
    );

    test(
        "test-2, simple path",
        createJsonRequest(
            /* elementIds = */ {3403809, 21696546}
        ),
        /* pathElementIds = */ {
            3403809,
            3403818,
            1538504211,
            3286698,
            21695608,
            21695509,
            1566844403,
            21696546,
        }
    );

    test(
        "test-3, simple straight path",
        createJsonRequest(
            /* elementIds = */ {5905454, 5857902}
        ),
        /* pathElementIds = */ {
            5905454,
            2198166,
            1617229446,
            1607923918,
            3079976,
            2197762,
            1607923847,
            5905364,
            5857911,
            5857902
        }
    );

    test(
        "test-4, Middle element",
        createJsonRequest(
            /* elementIds = */ {5857902, 3416820, 21696072, 21696211}
        ),
        /* pathElementIds = */ {
            5857902,
            1538746892,
            1617373881,
            21697049,
            772410182,
            772410188,
            3416809,
            1662463931,
            3416820,
            3415668,
            21696072,
            21696211,
        }
    );

    test(
        "test-5, long route with circle",
        createJsonRequest(
            /* elementIds = */ {
                3282754,
                3403445,
                3078998,
                5857588,
                5904965,
                3282754,
                1608194161,
            }
        ),
        /* pathElementIds = */ {
            2196624,
            2196735,
            2196852,
            2197058,
            2422074,
            2422374,
            3078337,
            3078998,
            3282754,
            3403427,
            3403445,
            3406150,
            5857588,
            5857601,
            5857680,
            5857753,
            5904965,
            5904967,
            5905207,
            5905242,
            21694406,
            21696699,
            21696700,
            774324437,
            1495660038,
            1544815266,
            1544818336,
            1559474913,
            1559476663,
            1559529614,
            1559529704,
            1560753400,
            1564437713,
            1589512572,
            1595676099,
            1595677076,
            1597977659,
            1607924980,
            1607924988,
            1607924994,
            1607925041,
            1607925131,
            1607925416,
            1607925461,
            1608194161,
            1608194241,
            1623137501,
            1638909665,
            1664144291,
            1665741847,
        }
    );
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
