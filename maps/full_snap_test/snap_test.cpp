#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polyline.h>

#include <maps/libs/road_graph/include/graph.h>
#include <maps/libs/snap/include/snap.h>
#include <maps/libs/succinct_rtree/include/rtree.h>

#include <geos/geom/Geometry.h>
#include <geos/geom/LineString.h>
#include <geos/geom/MultiLineString.h>
#include <geos/geom/CoordinateArraySequenceFactory.h>
#include <geos/geom/GeometryFactory.h>

#include <boost/format.hpp>

#include <cmath>
#include <fstream>
#include <iostream>
#include <memory>
#include <string>
#include <sstream>
#include <utility>
#include <vector>

namespace mrg = maps::road_graph;
namespace ms = maps::snap;

using maps::geolib3::Point2;
using maps::geolib3::Polyline2;
using maps::geolib3::Segment2;
using maps::geolib3::WGS84_MAJOR_SEMIAXIS;
using mrg::SegmentId;

using geos::geom::Coordinate;
using geos::geom::CoordinateArraySequenceFactory;
using geos::geom::CoordinateSequence;
using geos::geom::Geometry;
using geos::geom::GeometryFactory;
using geos::geom::LineString;
using geos::geom::MultiLineString;

const std::string RTREE = "maps/data/test/graph3/rtree.fb";
const std::string ROAD_GRAPH = "maps/data/test/graph3/road_graph.fb";
const std::string ROUTES_GEOMETRY_GRAPH = "routes_geometry.graph.txt";
const std::string ROUTES_GEOMETRY_SIMPLIFIED =
    "routes_geometry.simplified.txt";
const double SNAP_TOLERANCE = 20;
const double SNAP_ANGLE_TOLERANCE = 0.5;
const double MARGIN = 20;

const mrg::Graph roadGraph(BinaryPath(ROAD_GRAPH));
const maps::succinct_rtree::Rtree rtree(BinaryPath(RTREE), roadGraph);

Polyline2 parsePolyline(const std::string& rawPolyline) {
    Polyline2 polyline;
    std::istringstream stream(rawPolyline);
    double x, y;
    while (stream >> x >> y) {
        polyline.add({x, y});
    }
    return polyline;
}

std::unique_ptr<LineString> toLineString(const Segment2& segment) {
    std::vector<geos::geom::Coordinate> points;
    for (auto point: {segment.start(), segment.end()}) {
        auto cartesianPoint =
            geoToCartesian(point, WGS84_MAJOR_SEMIAXIS);
        points.emplace_back(Coordinate(
            cartesianPoint.x(),
            cartesianPoint.y(),
            cartesianPoint.z()
        ));
    }
    const GeometryFactory* factory = GeometryFactory::getDefaultInstance();
    auto coords = CoordinateArraySequenceFactory::instance()->create(
        std::move(points)
    );
    return factory->createLineString(std::move(coords));
}

std::unique_ptr<MultiLineString> toMultiLineString(
    const std::set<SegmentId> segmentIds)
{
    auto miltiLineParts = std::make_unique<std::vector<Geometry*>>();
    for (auto segmentId: segmentIds) {
        Segment2 geom = mrg::segmentGeometry(roadGraph, segmentId);
        miltiLineParts->push_back(toLineString(geom).release());
    }
    const GeometryFactory* factory = GeometryFactory::getDefaultInstance();
    return std::unique_ptr<MultiLineString>(
        factory->createMultiLineString(miltiLineParts.release()));
}

double getLengthPercentageInTubularNeighbourhood(
        const std::unique_ptr<Geometry>& measurableObject,
        const std::unique_ptr<Geometry>& tubularNeighbourhoodGeneratorObject) {

    // supress geos log
    std::streambuf* oldCerrStreamBuf = std::cerr.rdbuf();
    std::ostringstream dummy;
    std::cerr.rdbuf(dummy.rdbuf());
    const std::unique_ptr<Geometry> tubularNeighbourhood(
        tubularNeighbourhoodGeneratorObject->buffer(MARGIN));
    const std::unique_ptr<Geometry> intersection(
        tubularNeighbourhood->intersection(measurableObject.get()));
    double lengthInTubularNeighbourhood = intersection->getLength();
    std::cerr.rdbuf(oldCerrStreamBuf);
    return
        lengthInTubularNeighbourhood / measurableObject->getLength() * 100.0;
}

struct SnapResult {
    std::set<SegmentId> segmentIds;
    double snappedPercentage;
};

SnapResult snapPolyline(const Polyline2& polyline) {
    std::set<SegmentId> snappedSegmentIds;
    double snappedLength = 0, totalLength = 0;
    for (auto piece : ms::snap(
            roadGraph, rtree, polyline, SNAP_TOLERANCE, SNAP_ANGLE_TOLERANCE)) {
        if (piece.segmentId()) {
            snappedLength += piece.length();
            snappedSegmentIds.insert(*piece.segmentId());
        }
        totalLength += piece.length();
    }

    return {snappedSegmentIds, snappedLength / totalLength * 100};
}

std::vector<SnapResult> snapAllRoutesFromFile(
    const std::string& fileName)
{
    std::ifstream input(fileName);
    UNIT_ASSERT(input.is_open());
    std::string rawPolyline;
    std::vector<SnapResult> snapResults;
    while (std::getline(input, rawPolyline)) {
        snapResults.push_back(
            snapPolyline(parsePolyline(rawPolyline)));
    }
    return snapResults;
}

void checkPolylinesInFile(
    const std::string& fileWithPolylines,
    double minPercentage)
{
    auto snapResults = snapAllRoutesFromFile(fileWithPolylines);
    for (size_t resultId = 0; resultId < snapResults.size(); ++resultId) {
        if (snapResults[resultId].snappedPercentage < minPercentage) {
            UNIT_FAIL((boost::format(
                "polyline[%d] in file %s snapped only %lf%% (>= %lf%% need)") %
                resultId %
                fileWithPolylines %
                snapResults[resultId].snappedPercentage %
                minPercentage).str());
        }
    }
}

void snapToSameSegmentIds() {
    auto snapGraphRoutesResults = snapAllRoutesFromFile(
        SRC_(ROUTES_GEOMETRY_GRAPH));
    auto snapSimplifiedRoutesResults = snapAllRoutesFromFile(
        SRC_(ROUTES_GEOMETRY_SIMPLIFIED));
    for (size_t resultId = 0;
            resultId < snapGraphRoutesResults.size();
            ++resultId) {
        std::unique_ptr<Geometry> graphRouteMultiLineString =
            toMultiLineString(
                snapGraphRoutesResults[resultId].segmentIds);
        std::unique_ptr<Geometry> simplifiedRouteMultiLineString =
            toMultiLineString(
                snapSimplifiedRoutesResults[resultId].segmentIds);
        if (std::min(
                getLengthPercentageInTubularNeighbourhood(
                    graphRouteMultiLineString,
                    simplifiedRouteMultiLineString),
                getLengthPercentageInTubularNeighbourhood(
                    simplifiedRouteMultiLineString,
                    graphRouteMultiLineString)) <= 98) {
            UNIT_FAIL((boost::format(
                    "polylines at index %d snapped very differently") %
                resultId).str());
        }
    }
}

Y_UNIT_TEST_SUITE(FullSnap) {

Y_UNIT_TEST(snap_at_least) {
    checkPolylinesInFile(SRC_(ROUTES_GEOMETRY_GRAPH), 98);
}

Y_UNIT_TEST(snap_simplified_at_least) {
    checkPolylinesInFile(SRC_(ROUTES_GEOMETRY_SIMPLIFIED), 98);
}

Y_UNIT_TEST(snap_to_same_segmentIds) {
    snapToSameSegmentIds();
}

}
