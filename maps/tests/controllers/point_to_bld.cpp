#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <yandex/maps/wiki/common/json_helpers.h>

namespace maps::wiki::tests {

namespace {

const double FLOAT_EPSILON = 1e-6;

maps::json::Value performRequest(
    const std::string& lon,
    const std::string& lat,
    const std::string& bldNumber = "")
{
    std::ostringstream point;
    point << "{\"type\":\"Point\",\"coordinates\":";
    point << "[" << lon << "," << lat << "]";
    point << "}";

    return performAndValidateJsonGetRequest<PointToBld>(
        point.str(),
        bldNumber,
        revision::TRUNK_BRANCH_ID,
        "" /* token */);
}

void checkResult(
    const maps::json::Value& json,
    double lon,
    double lat,
    double azimuth,
    bool isInsideBld,
    TOid bldId)
{
    UNIT_ASSERT_DOUBLES_EQUAL(json["point"]["coordinates"][0].as<double>(), lon, FLOAT_EPSILON);
    UNIT_ASSERT_DOUBLES_EQUAL(json["point"]["coordinates"][1].as<double>(), lat, FLOAT_EPSILON);
    UNIT_ASSERT_DOUBLES_EQUAL(json["azimuth"].as<double>(), azimuth, FLOAT_EPSILON);
    UNIT_ASSERT_EQUAL(json["isInsideBld"].as<bool>(), isInsideBld);
    UNIT_ASSERT_EQUAL(common::readField<TOid>(json, "bldId"), bldId);
}

} // namespace
Y_UNIT_TEST_SUITE(point_to_bld)
{
WIKI_FIXTURE_TEST_CASE(test_point_to_bld_on_edge, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00100", "50.00100");
    json = performRequest("50.00150", "50.00100");
    json = performRequest("50.00200", "50.00100");
    json = performRequest("50.00200", "50.00200");
    json = performRequest("50.00150", "50.00200");
    json = performRequest("50.00100", "50.00200");
    json = performRequest("50.00100", "50.00150");
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_outside_1, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00150", "50.00215");
    checkResult(
        json,
        50.001500000,
        50.002000000,
        0.000000000,
        false,
        1);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_outside_2, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00215", "50.00150");
    checkResult(
        json,
        50.002000000,
        50.001500000,
        90.000000000,
        false,
        1);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_outside_3, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00150", "50.00085");
    checkResult(
        json,
        50.001500000,
        50.001000000,
        180.000000000,
        false,
        1);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_outside_4, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00085", "50.00150");
    checkResult(
        json,
        50.001000000,
        50.001500000,
        270.000000000,
        false,
        1);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_inside_1, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00150", "50.00185");
    checkResult(
        json,
        50.001500000,
        50.002000000,
        0.000000000,
        true,
        1);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_inside_2, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00185", "50.00150");
    checkResult(
        json,
        50.002000000,
        50.001500000,
        90.000000000,
        true,
        1);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_inside_3, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00150", "50.00115");
    checkResult(
        json,
        50.001500000,
        50.001000000,
        180.000000000,
        true,
        1);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_inside_4, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00115", "50.00150");
    checkResult(
        json,
        50.001000000,
        50.001500000,
        270.000000000,
        true,
        1);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_vertex, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00100", "50.00215");
    checkResult(
        json,
        50.001000000,
        50.002000000,
        0.000000000,
        false,
        1);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_too_far, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    UNIT_CHECK_GENERATED_EXCEPTION(performRequest("60.0", "60.0"), LogicException);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_big_lon_lat, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    UNIT_CHECK_GENERATED_EXCEPTION(performRequest("1000.0", "1000.0"), LogicException);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_with_bld_number, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00350", "50.00215", "1");
    checkResult(
        json,
        50.003500000,
        50.002000000,
        0.000000000,
        false,
        2);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_incomplete_bld_number, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    UNIT_CHECK_GENERATED_EXCEPTION(performRequest("50.00350", "50.00285", "2"), LogicException);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_unexisting_bld_number, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    UNIT_CHECK_GENERATED_EXCEPTION(performRequest("50.00350", "50.00285", "3"), LogicException);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_shared_edge, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00350", "50.00335");
    checkResult(
        json,
        50.003500000,
        50.003500000,
        0.000000000,
        true,
        5);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_internal_bld_1, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00150", "50.00315");
    checkResult(
        json,
        50.001500000,
        50.003000000,
        180.000000000,
        true,
        3);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_bld_internal_bld_2, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00150", "50.00335");
    checkResult(
        json,
        50.002000000,
        50.003350000,
        90.000000000,
        true,
        3);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_adjacent_bld, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00285", "50.00335", "2А");
    checkResult(
        json,
        50.003000000,
        50.003350000,
        270.000000000,
        false,
        5);
}

WIKI_FIXTURE_TEST_CASE(test_point_to_non_adjacent_bld, EditorTestFixture)
{
    performObjectsImport("tests/data/point_to_bld.json", db.connectionString());
    auto json = performRequest("50.00350", "50.00415", "2А");
    checkResult(
        json,
        50.003500000,
        50.003850000,
        0.000000000,
        false,
        6);
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
