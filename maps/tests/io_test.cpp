#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/mapspro/libs/editor_client/impl/parser.h>
#include <maps/wikimap/mapspro/libs/editor_client/impl/request.h>
#include <maps/libs/common/include/file_utils.h>

#include <iostream>

using namespace maps::wiki::editor_client;
using namespace maps::wiki::poi_conflicts;

auto objectTuple(const BasicEditorObject& obj)
{
    return std::tie(obj.id, obj.revisionId, obj.categoryId);
}

Y_UNIT_TEST_SUITE(parser)
{
Y_UNIT_TEST(parse_get_object)
{
    const auto jsonPath = ArcadiaSourceRoot() +
        "/maps/wikimap/mapspro/libs/editor_client/tests/data/poi.json";
    auto data = maps::common::readFileToString(jsonPath);
    auto object = parseJsonResponse(data);
    UNIT_ASSERT_VALUES_EQUAL(object.categoryId, "poi_medicine");
    UNIT_ASSERT_VALUES_EQUAL(object.getGeometryInGeodetic()->geometryType(), maps::geolib3::GeometryType::Point);
    UNIT_ASSERT_VALUES_EQUAL(object.plainAttributes.size(), 13);
    UNIT_ASSERT_VALUES_EQUAL(object.plainAttributes["poi:business_rubric_id"], "184105950");
    UNIT_ASSERT_VALUES_EQUAL(object.tableAttributes.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(object.tableAttributes["poi_nm"].size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(object.tableAttributes["poi_nm"][0]["poi_nm:name"], "apoteka");
    UNIT_ASSERT_VALUES_EQUAL(object.multiValueAttributes.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(object.multiValueAttributes["poi:location"].size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(object.multiValueAttributes["poi:location"][0], "indoor");
    UNIT_ASSERT_VALUES_EQUAL(object.multiValueAttributes["poi:location"][1], "sign");
}

Y_UNIT_TEST(create_request) {
    const auto jsonPath = ArcadiaSourceRoot() +
        "/maps/wikimap/mapspro/libs/editor_client/tests/data/poi.json";
    auto data = maps::common::readFileToString(jsonPath);
    auto object = parseJsonResponse(data);
    auto request = createJsonUpdateRequest(object);
    auto parsedRequest = parseJsonResponse(request);
    UNIT_ASSERT(objectTuple(parsedRequest) == objectTuple(object));
}

Y_UNIT_TEST(parse_conflicts) {
    const auto jsonPath = ArcadiaSourceRoot() +
        "/maps/wikimap/mapspro/schemas/editor/json/examples/editor.objects_query_poi_conflicts_response.json";
    const auto data = maps::common::readFileToString(jsonPath);
    const auto conflicts = parsePoiConflictsResponseJson(data).zoomToConflictingObjects;
    UNIT_ASSERT(conflicts.count(21));
    UNIT_ASSERT(conflicts.count(19));
    UNIT_ASSERT(conflicts.at(21).count(ConflictSeverity::Critical));
    UNIT_ASSERT(conflicts.at(21).count(ConflictSeverity::Error));
    UNIT_ASSERT(conflicts.at(19).count(ConflictSeverity::Critical));
}

} // Y_UNIT_TEST_SUITE(parser)
