#include <yandex/maps/wiki/common/geom.h>
#include <maps/libs/json/include/value.h>
#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/serialization.h>
#include <maps/libs/geolib/include/variant.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <geos/geom/CoordinateArraySequenceFactory.h>
#include <geos/geom/Point.h>
#include <geos/geom/GeometryFactory.h>
#include <sstream>

using namespace maps::wiki::common;
using namespace maps::geolib3;

Y_UNIT_TEST_SUITE(geom) {

Y_UNIT_TEST(test_geos_buffer)
{
    geos::geom::Coordinate ptCoord(10, 10);
    geos::geom::Point* geosPt =
        geos::geom::GeometryFactory::getDefaultInstance()->createPoint(ptCoord);
    Geom center(geosPt);
    Geom buffer = center.createBuffer(5.0);
    std::ostringstream s;
    center.coordinatesJson(s);
    buffer.coordinatesJson(s);
    UNIT_ASSERT_STRINGS_EQUAL(s.str(), "[[10,10]][[15,15],[15,5],[5,5],[5,15],[15,15]]");
}

namespace {

void testSplitPoly(const std::string& filename, size_t partCount)
{
    auto json = maps::json::Value::fromFile(ArcadiaSourceRoot() + "/maps/wikimap/mapspro/libs/common/" + filename);
    auto geoGeometry = readGeojson<Polygon2>(json["polygon"]);
    auto mercatorGeometry = convertGeodeticToMercator(geoGeometry);
    auto polyWkb = WKB::toString(mercatorGeometry);
    Geom poly(polyWkb);

    std::vector<Geom> splitLines;
    auto linesJson = json["lines"];
    for (size_t i = 0; i < linesJson.size(); i++) {
        auto geoGeometry = readGeojson<Polyline2>(json["lines"][0]);
        auto mercatorGeometry = convertGeodeticToMercator(geoGeometry);
        auto splitLineWkb = WKB::toString(mercatorGeometry);
        splitLines.emplace_back(splitLineWkb);
    }

    auto resultPolygons = poly.splitPolyByLines(splitLines);
    UNIT_ASSERT_VALUES_EQUAL(resultPolygons.size(), partCount);
}

} // namespace

Y_UNIT_TEST(test_split_poly_by_line_1)
{
    //aligned polygon with hole
    testSplitPoly("tests/data/splitPolygon_1.json", 4);
}

Y_UNIT_TEST(test_split_poly_by_line_2_0)
{
    //aligned polygon without holes; snap line to the polygon vertices
    testSplitPoly("tests/data/splitPolygon_2.json", 2);
}

Y_UNIT_TEST(test_split_poly_by_line_3)
{
    //aligned polygon without holes; snap line to the polygon edges
    testSplitPoly("tests/data/splitPolygon_3.json", 2);
}

Y_UNIT_TEST(test_split_poly_by_line_4)
{
    //rotated polygon without holes; snap line to the polygon edges
    testSplitPoly("tests/data/splitPolygon_4.json", 2);
}

Y_UNIT_TEST(test_split_poly_by_line_5)
{
    //rotated polygon with holes; snap line to the hole edge
    testSplitPoly("tests/data/splitPolygon_5.json", 2);
}

Y_UNIT_TEST(test_split_poly_by_line_6)
{
    //rotated polygon without holes; snap line segment is aligned with polygon edge
    testSplitPoly("tests/data/splitPolygon_6.json", 2);
}

Y_UNIT_TEST(test_split_poly_by_line_7)
{
    //rotated polygon without holes; snap line segments snap in one vertex
    testSplitPoly("tests/data/splitPolygon_7.json", 1);
}

namespace {
Geom
lineString(const std::vector<Point2>& coordinates)
{
    auto coordsSize = coordinates.size();
    UNIT_ASSERT(coordsSize);
    std::unique_ptr<std::vector<geos::geom::Coordinate>> coordArray(new std::vector<geos::geom::Coordinate>());
    coordArray->reserve(coordsSize);
    for (const auto& coord : coordinates) {
        coordArray->emplace_back(coord.x(), coord.y());
    }
    Geom newLineString;
    newLineString.setLineString(
        geos::geom::CoordinateArraySequenceFactory::instance()->create(coordArray.release()));
    return newLineString;
}
} // namespace

Y_UNIT_TEST(test_geom_split_linestring_throw_empty_splits)
{
    std::vector<Point2> lineCoords {{0, 0}, {1, 1}};
    auto line = lineString(lineCoords);
    UNIT_CHECK_GENERATED_EXCEPTION(
        line.splitLineStringByPoints({}, 0.1),
        geom::BadParam);
}

Y_UNIT_TEST(test_geom_split_linestring_throw_split_point_away)
{
    std::vector<Point2> lineCoords {{0, 0}, {0, 10}};
    auto line = lineString(lineCoords);
    UNIT_CHECK_GENERATED_EXCEPTION(
        line.splitLineStringByPoints(
            {{0, 11}}, 0.1),
        maps::LogicError);
    UNIT_CHECK_GENERATED_EXCEPTION(
        line.splitLineStringByPoints(
            {{0.2, 5}}, 0.1),
        geom::LogicError);
}

Y_UNIT_TEST(test_geom_split_linestring_split_edge)
{
    std::vector<Point2> lineCoords {{0, 0}, {0, 10}};
    auto line = lineString(lineCoords);
    auto splitResult =
        line.splitLineStringByPoints(
            {{0.01, 5}}, 0.1);
    UNIT_ASSERT_VALUES_EQUAL(splitResult.size(), 2);
}

Y_UNIT_TEST(test_geom_split_linestring_dont_split_endpoint)
{
    std::vector<Point2> lineCoords {{0, 0}, {0, 10}};
    auto line = lineString(lineCoords);
    auto splitResultBegin =
        line.splitLineStringByPoints(
            {{0.01, 0}}, 0.1);
    UNIT_ASSERT_VALUES_EQUAL(splitResultBegin.size(), 1);
    auto splitResultEnd =
        line.splitLineStringByPoints(
            {{0.01, 10}}, 0.1);
    UNIT_ASSERT_VALUES_EQUAL(splitResultEnd.size(), 1);
}

Y_UNIT_TEST(test_geom_split_linestring_split_inner_vertex)
{
    std::vector<Point2> lineCoords {{0, 0}, {0, 5}, {0, 10}};
    auto line = lineString(lineCoords);
    auto splitResult =
        line.splitLineStringByPoints(
            {{0.01, 5}}, 0.1);
    UNIT_ASSERT_VALUES_EQUAL(splitResult.size(), 2);
    for (const auto& subLine : splitResult) {
        auto subLineSplitAgain = subLine.splitLineStringByPoints(
            {{0.01, 5}}, 0.1);
        UNIT_ASSERT_VALUES_EQUAL(subLineSplitAgain.size(), 1);
    }
}

Y_UNIT_TEST(test_geom_split_linestring_split_vertex_wins_edge)
{
    std::vector<Point2> lineCoords {{0, 0}, {0, 5}};
    auto line = lineString(lineCoords);
    auto splitResult =
        line.splitLineStringByPoints(
            {{0.01, 4.999}}, 0.1);
    UNIT_ASSERT_VALUES_EQUAL(splitResult.size(), 1);
}

} // Y_UNIT_TEST_SUITE
