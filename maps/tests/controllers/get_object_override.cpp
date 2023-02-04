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

namespace {
GetObject::Request
createGetRequest(TOid oid, GetObject::SubstitutionPolicy policy = GetObject::SubstitutionPolicy::Deny)
{
    return GetObject::Request {
        oid,
        TESTS_USER,
        "",
        0,
        policy,
        GetObject::PartsOfComplexContourObjectsLoadPolicy::Load
    };
}
std::string
executeGetRequest(const GetObject::Request& request)
{
    GetObject getObjectController(request);
    std::string result;
    auto formatter = Formatter::create(common::FormatType::JSON,
        make_unique<TestFormatterContext>());
    UNIT_ASSERT_NO_EXCEPTION(result = (*formatter)(*getObjectController()));
    validateJsonResponse(result, "GetObject");
    return result;
}
}

Y_UNIT_TEST_SUITE(get_object_override)
{
WIKI_FIXTURE_TEST_CASE(test_get_object_substitution_for_element, EditorTestFixture)
{
    //Create object for test
    std::string result = performSaveObjectRequest("tests/data/create_simple_vegetation.json");

    auto outputParser = SaveObjectParser();
    UNIT_ASSERT_NO_EXCEPTION(outputParser.parse(common::FormatType::JSON, result));
    WIKI_TEST_REQUIRE_EQUAL(outputParser.objects().size(), 1);//veg
    const auto& objectData = *outputParser.objects().begin();
    WIKI_TEST_REQUIRE_EQUAL(objectData.categoryId(), "vegetation");
    auto parsedResult = json::Value::fromString(result);
    TOid vegetationId = boost::lexical_cast<TOid>(
        parsedResult["geoObjects"][0]["id"].toString());
    TOid vegetationElId = boost::lexical_cast<TOid>(
        parsedResult["geoObjects"][0]
        ["slaves"]["part"]["geoObjects"][0]
        ["slaves"]["part"]["geoObjects"][0]["id"].toString());

    //Now do actual test for GetObject with/without override
    auto getObjRequest = createGetRequest(
            vegetationElId,
            GetObject::SubstitutionPolicy::Deny);
    auto resultNoSubstition = executeGetRequest(getObjRequest);
    //turn substitution on and expect vegetation at root
    getObjRequest.substitutionPolicy = GetObject::SubstitutionPolicy::Allow;
    auto resultSubstition = executeGetRequest(getObjRequest);
    UNIT_ASSERT(resultSubstition != resultNoSubstition);
    UNIT_ASSERT_EQUAL(getObjectID(resultNoSubstition), vegetationElId);
    UNIT_ASSERT_EQUAL(getObjectID(resultSubstition), vegetationId);
}

WIKI_FIXTURE_TEST_CASE(test_get_object_rd_el_with_bus_route, EditorTestFixture)
{
    performObjectsImport("tests/data/rd_el_bus_stop_route_thread.json", db.connectionString());
    auto getObjRequest = createGetRequest(1892814584);//imported rd_el
    auto output = json::Value::fromString(executeGetRequest(getObjRequest));
    UNIT_ASSERT_EQUAL(output["id"].toString(), "1892814584");
    UNIT_ASSERT_EQUAL(
        output["masters"]["bus_part"]["geoObjects"][0]
                ["masters"]["assigned_thread"]["geoObjects"][0]["id"].toString(),
        "1892814590");//route id from imported file
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
