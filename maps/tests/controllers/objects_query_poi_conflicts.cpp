#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/observers.h>
#include <maps/wikimap/mapspro/services/editor/src/observers/view_syncronizer.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/json/include/value.h>

namespace maps::wiki::tests {

namespace {
std::string toString(const json::Value& value)
{
    json::Builder builder;
    builder << value;
    return builder.str();
}
} // namespace

Y_UNIT_TEST_SUITE(query_poi_conflicts)
{
WIKI_FIXTURE_TEST_CASE(test_query_poi_conflicts, EditorTestFixture)
{
    auto observers = makeObservers<ViewSyncronizer>();
    auto basePoi = performSaveObjectRequest("tests/data/query_poi_conflicts/east_poi.json", observers);
    performSaveObjectRequest("tests/data/query_poi_conflicts/middle_poi.json", observers);
    performSaveObjectRequest("tests/data/query_poi_conflicts/west_poi.json", observers);

    // Check distant point
    // No conflicts should be reported
    auto distantResult = performAndValidateJsonGetRequest<ObjectsQueryPoiConflicts>(
        std::string(R"({"coordinates":[38.52708776270979,56.532866796919],"type":"Point"})"),
        Token {},
        revision::TRUNK_BRANCH_ID,
        std::optional<TOid>{},
        std::optional<TOid>{},
        ObjectsQueryPoiConflicts::Request::IsGeoproductParam::True,
        0);
    UNIT_ASSERT_STRINGS_EQUAL(toString(distantResult), "{\"conflicts\":[]}");

    // Check point at east position
    // Returns 3 conflicts including self
    auto eastResult = performAndValidateJsonGetRequest<ObjectsQueryPoiConflicts>(
        std::string(R"({"coordinates":[38.52702741300698,56.53267230894654],"type":"Point"})"),
        Token {},
        revision::TRUNK_BRANCH_ID,
        std::optional<TOid>{},
        std::optional<TOid>{},
        ObjectsQueryPoiConflicts::Request::IsGeoproductParam::True,
        0);
    UNIT_ASSERT_STRINGS_EQUAL(toString(eastResult),
        loadFile("tests/data/query_poi_conflicts/three_conflicts.json"));

    // Check point at east position
    // Returns 2 conflicts excluding self
    auto eastResultExcluding1 = performAndValidateJsonGetRequest<ObjectsQueryPoiConflicts>(
        std::string(R"({"coordinates":[38.52702741300698,56.53267230894654],"type":"Point"})"),
        Token {},
        revision::TRUNK_BRANCH_ID,
        std::optional<TOid>{},
        1,
        ObjectsQueryPoiConflicts::Request::IsGeoproductParam::True,
        0);
    UNIT_ASSERT_STRINGS_EQUAL(toString(eastResultExcluding1),
        loadFile("tests/data/query_poi_conflicts/two_conflicts.json"));
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
