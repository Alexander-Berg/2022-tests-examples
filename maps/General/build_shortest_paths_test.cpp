#include <maps/libs/jams/static-graph/tools/shortest_paths_builder_tools/shortest_paths_builder_tools.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>
#include <yandex/maps/mms/cast.h>

#include <boost/optional.hpp>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

using maps::geolib3::Polyline2;
using maps::geolib3::Point2;
using maps::geolib3::Segment2;
using maps::geolib3::test_tools::approximateEqual;

namespace geo = maps::geolib3;
namespace jsg = maps::jams::static_graph2;

void checkCurvative(boost::optional<Segment2> segment,
                    const Polyline2& polyline,
                    double correct)
{
    const static double EPS = 1e-6;
    const double candidate = curvative(segment, polyline);
    UNIT_ASSERT(approximateEqual(candidate, correct, EPS));
}

std::vector<Point2>
addDegenerate(std::vector<Point2> points, size_t count = 3)
{
    for(size_t i = 0; i < count; ++i) {
        size_t index = (i * 2134 + 123) % points.size();
        points.insert(points.begin() + index, points[index]);
    }
    return points;
}


Y_UNIT_TEST_SUITE(BuildShortestPaths) {

Y_UNIT_TEST(checkCurvativeCalculation)
{
    Segment2 segment(Point2(0.0, -1.0), Point2(0.0, 0.0));
    {
        std::vector<Point2> points = {{0.0, 0.0}, {1.0, 0.0}, {1.0, 1.0}};
        Polyline2 polyline(points.begin(), points.end());
        checkCurvative(boost::none, polyline, M_PI / 2);
        checkCurvative(boost::optional<Segment2>(segment),
                                    polyline, M_PI);
        points = addDegenerate(points);
        polyline = Polyline2(points);
        checkCurvative(boost::none, polyline, M_PI / 2);
        checkCurvative(boost::optional<Segment2>(segment),
                                    polyline, M_PI);
    }
    {
        //only degenerates
        const std::vector<Point2> points = {{13.0, 13.0}, {13.0, 13.0}, {13.0, 13.0}};
        Polyline2 polyline(points.begin(), points.end());
        checkCurvative(boost::none, polyline, 0.0);
        checkCurvative(boost::optional<Segment2>(segment),
                                    polyline, 0.0);
    }
}

Y_UNIT_TEST(checkUpdateLastNotDegenerateSegment)
{
    static const double EPS = 1e-7;
    Segment2 segment(Point2(0.0, -1.0), Point2(0.0, 0.0));
    {
        //without degenerate segments
        Polyline2 polyline;
        polyline.add(Point2(0.0, 0.0));
        polyline.add(Point2(1.0, 0.0));
        polyline.add(Point2(1.0, 1.0));
        UNIT_ASSERT(approximateEqual(
                      *updateLastNotDegenerateSegment(boost::none, polyline),
                      polyline.segmentAt(1), EPS));
        UNIT_ASSERT(approximateEqual(
                      *updateLastNotDegenerateSegment(
                            boost::optional<Segment2>(segment),
                            polyline),
                      polyline.segmentAt(1), EPS));
    }
    {
        Polyline2 polyline;
        polyline.add(Point2(13.0, 13.0));
        polyline.add(Point2(13.0, 13.0));
        polyline.add(Point2(13.0, 13.0));
        UNIT_ASSERT(!updateLastNotDegenerateSegment(boost::none, polyline));
        UNIT_ASSERT(approximateEqual(
                      *updateLastNotDegenerateSegment(
                            boost::optional<Segment2>(segment),
                            polyline),
                      segment, EPS));
        UNIT_ASSERT(approximateEqual(
                      *updateLastNotDegenerateSegment(
                            boost::optional<Segment2>(segment),
                            Polyline2()),
                      segment, EPS));
    }
}

Y_UNIT_TEST(checkNotDegenerateSegmentIndices)
{
    {
        std::vector<int> xCoords;
        for(size_t i = 0; i < 10; ++i) {
            xCoords.push_back(i);
        }
        xCoords.push_back(0);
        xCoords.push_back(1);
        xCoords.push_back(9);
        std::sort(xCoords.begin(), xCoords.end());
        Polyline2 polyline;
        for (int x: xCoords) {
            polyline.add(Point2(x, 0.0));
        }
        std::vector<size_t> correct = {1, 3, 4, 5, 6, 7, 8, 9, 10};
        UNIT_ASSERT(correct == notDegenerateSegmentIndices(polyline));
    }
}

void checkDistance(const jsg::Graph& graph,
                const SourceToTargetVerticesInfo& info,
                TargetVertexInfo vertexInfo)
{
    const static double EPS = 1e-5;
    while (vertexInfo.hasAncestor()) {
        double distance = vertexInfo.distance()
                    - jsg::edgeGeoLength(graph, vertexInfo.prevEdge());
        vertexInfo = info.at(vertexInfo.prevVertex());
        UNIT_ASSERT(approximateEqual(distance,
                        static_cast<double>(vertexInfo.distance()), EPS));
    }
}

void checkDrivingTime(const jsg::GraphData::EdgesData& edgesData,
                const SourceToTargetVerticesInfo& info,
                TargetVertexInfo vertexInfo)
{
    const static double EPS = 1e-3;
    while (vertexInfo.hasAncestor()) {
        double drivingTime_ = vertexInfo.drivingTime()
                    - drivingTime(edgesData[vertexInfo.prevEdge()]);
        vertexInfo = info.at(vertexInfo.prevVertex());
        UNIT_ASSERT(approximateEqual(drivingTime_,
                        static_cast<double>(vertexInfo.drivingTime()), EPS));
    }
}

void checkCurvative(const jsg::GraphData::EdgesData& edgesData,
                const SourceToTargetVerticesInfo& info,
                TargetVertexInfo vertexInfo)
{
    const static double EPS = 1e-6;
    std::vector<Segment2> segments;
    double curvative = vertexInfo.curvative();
    while (vertexInfo.hasAncestor()) {
        const Polyline2& polyline =
                        edgesData[vertexInfo.prevEdge()].polyline();
        std::vector<Segment2> tmp;
        for(const Segment2& segment: polyline.segments()) {
            if (!segment.isDegenerate()) {
                tmp.push_back(segment);
            }
        }
        std::reverse(tmp.begin(), tmp.end());
        for(const Segment2& segment: tmp) {
            segments.push_back(segment);
        }
        vertexInfo = info.at(vertexInfo.prevVertex());
    }
    std::reverse(segments.begin(), segments.end());
    geo::Radians checkingCurvative{0.0};
    for(size_t i = 0; i + 1 < segments.size(); ++i) {
        checkingCurvative += geo::angleBetween(
            geo::fastGeoDirection(segments[i]),
            geo::fastGeoDirection(segments[i + 1])
        );
    }
    UNIT_ASSERT(approximateEqual(curvative, checkingCurvative.value(), EPS));
}

//checks that no vertex can be added to shortest paths tree
//also checks that the tree is a shortest paths tree
void checkFullCorrectTree(const jsg::Graph& graph,
                const SourceToTargetVerticesInfo& info,
                double distanceBound)
{
    const static double EPS = 1e-3;
    for(const auto& vInfo: info) {
        const TargetVertexInfo& vertexInfo = vInfo.second;
        UNIT_ASSERT(vertexInfo.distance() >= vertexInfo.openairDistance());
        UNIT_ASSERT(vertexInfo.openairDistance() <= distanceBound);
        for(const jsg::Edge& edge: graph.outEdges(vInfo.first)) {
            if (info.count(edge.target())) {
                UNIT_ASSERT(vertexInfo.distance()
                    + jsg::edgeGeoLength(graph, edge.id()) + EPS >=
                    info.at(edge.target()).distance());
            }
            else {
                UNIT_ASSERT(jsg::EdgeData::Tunnel != graph.edgesData().at(edge.id()).structType());
                UNIT_ASSERT(vertexInfo.openairDistance()
                    + jsg::edgeGeoLength(graph, edge.id()) > distanceBound);
            }
        }
    }
}

void checkDijkstraFromSource(jsg::Id source, const jsg::Graph& graph)
{
    const static double DISTANCE_BOUND = 1000.0;
    SourceToTargetVerticesInfo info;
    dijkstraRun(
            source,
            DISTANCE_BOUND,
            graph,
            &info);
    UNIT_ASSERT_NO_EXCEPTION(info.at(source));
    for(const auto& vInfo: info) {
        const TargetVertexInfo& vertexInfo = vInfo.second;
        checkDistance(graph, info, vertexInfo);
        checkCurvative(graph.data().edgesData(), info, vertexInfo);
        checkDrivingTime(graph.data().edgesData(), info, vertexInfo);
    }
    checkFullCorrectTree(graph, info, DISTANCE_BOUND);
}

Y_UNIT_TEST(checkDijkstra)
{
    const std::string graphTopologyFilename = BinaryPath("maps/data/test/graph3/topology.mms.2");
    const std::string graphDataFilename = BinaryPath("maps/data/test/graph3/data.mms.2");
    jsg::GraphHolder graph(graphTopologyFilename, graphDataFilename);
    std::vector<jsg::Id> sources = {0, 10, 100, 1000, 10000, 100000, 110000};
    for(jsg::Id source: sources) {
        checkDijkstraFromSource(source, *graph);
    }
}

};
