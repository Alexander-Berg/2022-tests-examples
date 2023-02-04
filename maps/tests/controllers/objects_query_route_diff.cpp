#include <maps/wikimap/mapspro/services/editor/src/actions/routing/objects_query_route_diff.h>
#include <maps/wikimap/mapspro/services/editor/src/common.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/objects_query_route_diff_parser.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <yandex/maps/wiki/common/string_utils.h>

#include <maps/libs/json/include/builder.h>

#include <algorithm>
#include <initializer_list>
#include <iterator>
#include <sstream>
#include <string>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(objects_query_route_diff)
{
namespace {

void checkElementsById(
    const std::string& test,
    const std::string& field,
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
        "missed elements: " << toString(missedIds) << " for field " << field
    );
}

void test(
        const std::string& name,
        const std::string& jsonRequestPath,
        const TOIds& expectedAddElementIds,
        const TOIds& expectedRemoveElementIds)
{
        const std::string jsonRequest = loadFile(jsonRequestPath);
        validateJsonRequest(jsonRequest, ObjectsQueryRouteDiff::taskName());

        ObjectsQueryRouteDiffParser parser;
        parser.parse(common::FormatType::JSON, jsonRequest);

        ObjectsQueryRouteDiff::Request request {
            TESTS_USER,
            "",
            revision::TRUNK_BRANCH_ID,
            parser.revisionId(),
            parser.categoryId(),
            parser.addElementIds(),
            parser.removeElementIds(),
            parser.threadStopSequence(),
            parser.fromThreadStopIdx(),
            parser.toThreadStopIdx(),
            /* limit = */ 0
        };
        const auto result = *ObjectsQueryRouteDiff(makeObservers<>(), request)();

        checkElementsById(name, "add", result.addElements, expectedAddElementIds);
        checkElementsById(name, "remove", result.removeElements, expectedRemoveElementIds);

        auto formatter = Formatter::create(common::FormatType::JSON);
        validateJsonResponse((*formatter)(result), ObjectsQueryRouteDiff::taskName());
}

} // namespace

WIKI_FIXTURE_TEST_CASE(test_objects_query_route_diff, EditorTestFixture)
{
    performObjectsImport("tests/data/small_road_graph.json", db.connectionString());

    test(
       "Test-1, simple route",
       "tests/data/objects_query_route_diff/01.json",
        /* addElementIds = */ {
            3403809,
            3403818,
            1538504211,
            3286698,
            21695608,
            21695509,
            1566844403,
            21696546,
        },
        /* removeElementIds = */ {}
    );

    test(
       "Test-2, simple straight route",
       "tests/data/objects_query_route_diff/02.json",
        /* addElementIds = */ {
            5905454,
            2198166,
            1617229446,
            1607923918,
            3079976,
            2197762,
            1607923847,
            5905364,
            5857911,
            5857902,
        },
        /* removeElementIds = */ {}
    );

    test(
       "Test-3, stops route with turnabout",
       "tests/data/objects_query_route_diff/03.json",
        /* addElementIds = */ {
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
            3415659,
            1551872750,
            1618002901,
            3415674,
        },
        /* removeElementIds = */ {}
    );

    test(
       "Test-4, bridge turnabout",
       "tests/data/objects_query_route_diff/04.json",
        /* addElementIds = */ {
            2422136,
            3403809,
            3403842,
            3403852,
            3479583,
            5857719,
            21695975,
            775179797,
            1538743578,
            1613102561,
            1613102566,
            1619166140,
        },
        /* removeElementIds = */ {}
    );

    test(
       "Test-5, long route with circle",
       "tests/data/objects_query_route_diff/05.json",
        /* addElementIds = */ {
            2196624,
            2196735,
            2196852,
            2197058,
            2422074,
            2422374,
            3078337,
            3078998,
            3282754,
            3287141,
            3403427,
            3403445,
            3406140,
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
            21695871,
            21696699,
            21696700,
            52654412,
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
        },
        /* removeElementIds = */ {}
    );

    test(
       "Test-6, diff for stop move",
       "tests/data/objects_query_route_diff/06.json",
        /* addElementIds = */ {
            2649555,
            3287588,
            1614294701,
        },
        /* removeElementIds = */ {
            2197913,
            2423362,
            2423556,
            3287370,
            3287597,
            1614294647,
            1614294741,
            1614294821,
            1614294890,
            1622011511,
            1633897689,
        }
    );

    test(
       "Test-7, diff for new thread",
       "tests/data/objects_query_route_diff/07.json",
        /* addElementIds = */ {
            2196624,
            2196735,
            1607924980,
            1607925041,
            1607925131,
        },
        /* removeElementIds = */ {
            1607924994,
            3403445,
            1599465275,
            1599465090,
            3403433,
            1634708071,
            2196917,
            3286423,
            2422084,
        }
    );

    test(
       "Test-8, diff for new thread, no remove elements",
       "tests/data/objects_query_route_diff/08.json",
        /* addElementIds = */ {
            3478195,
            3479932,
            21695878,
            21696211,
            771345066,
            1538368872,
        },
        /* removeElementIds = */ {}
    );
}

WIKI_FIXTURE_TEST_CASE(test_objects_query_route_diff_reroute_broken, EditorTestFixture)
{
    performObjectsImport("tests/data/objects_query_route_diff/reroute_broken_revisions.json", db.connectionString());
    test(
       "Test-9, find remove elments in broken route",
       "tests/data/objects_query_route_diff/reroute_broken_request.json",
        /* addElementIds = */ {
            13787128263
        },
        /* removeElementIds = */ {
            13787128147
        }//element 23787128147 should not appear in removed list due to buffer search
    );
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
