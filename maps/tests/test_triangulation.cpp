#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/libs/concave_hull/impl/triangulation.h>
#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/geolib/include/test_tools/random_geometry_factory.h>
#include <geos/triangulate/quadedge/TrianglePredicate.h>
#include <geos/geom/Coordinate.h>


using namespace maps::geolib3;

namespace maps::concave_hull {
namespace {

const Point2 MOSCOW_CENTER = convertGeodeticToMercator(
    Point2{37.617635, 55.755814});

test_tools::PseudoRandom pseudoRandom;


geos::geom::Coordinate toGeos(const Point2& p)
{
    return {p.x(), p.y()};
}


bool isDelaunay(const Triangulation& triang)
{
    std::set<int> vertices;
    for (int edge : triang.getAllEdges())
    {
        vertices.insert(triang.edgeSrcId(edge));
        vertices.insert(triang.edgeDstId(edge));
    }

    for (int edge1 : triang.getAllEdges()) {
        int edge2 = triang.getNextLeftEdge(edge1);
        int edge3 = triang.getNextLeftEdge(edge2);
        int a = triang.edgeDstId(edge1);
        int b = triang.edgeDstId(edge2);
        int c = triang.edgeDstId(edge3);
        auto pa = triang.vertexPoint(a);
        auto pb = triang.vertexPoint(b);
        auto pc = triang.vertexPoint(c);

        for (int v : vertices) {
            if (v == a || v == b || v == c)
                continue;

            auto pv = triang.vertexPoint(v);
            bool isInCircle = geos::geom::TrianglePredicate::isInCircleRobust(
                toGeos(pa), toGeos(pb), toGeos(pc), toGeos(pv));
            if (isInCircle) {
                return false;
            }
        }
    }
    return true;
}

} // namespace


Y_UNIT_TEST_SUITE(TestTriangulation)
{

Y_UNIT_TEST(TestPrecision)
{
    // Not enough float precision here, if we use whole Mercator area
    PointsVector points = convertGeodeticToMercator(PointsVector{
        {27.524149, 53.918795},
        {27.524149, 53.918000},
        {27.524149, 53.917901},
        {27.524234, 53.918835},
    });

    Triangulation triang(points);
    EXPECT_TRUE(isDelaunay(triang));
}


Y_UNIT_TEST(TestPseudoRandomSmallScale)
{
    BoundingBox bbox{MOSCOW_CENTER, 300, 300};

    test_tools::RandomGeometryFactory factory{bbox, &pseudoRandom};
    auto points = factory.getPointsVector(1000);
    Triangulation triang(points);
    EXPECT_TRUE(isDelaunay(triang));
}


Y_UNIT_TEST(TestPseudoRandomMoscowScale)
{
    BoundingBox bbox{MOSCOW_CENTER, 50000, 50000};

    test_tools::RandomGeometryFactory factory{bbox, &pseudoRandom};
    auto points = factory.getPointsVector(1000);
    Triangulation triang(points);
    EXPECT_TRUE(isDelaunay(triang));
}


Y_UNIT_TEST(TestPseudoRandomWorldScale)
{
    // http://epsg.io/3395
    auto bbox = convertGeodeticToMercator(BoundingBox{{-180, -80}, {180, 84}});

    test_tools::RandomGeometryFactory factory{bbox, &pseudoRandom};
    auto points = factory.getPointsVector(1000);
    Triangulation triang(points);
    EXPECT_TRUE(isDelaunay(triang));
}

}

} // namespace maps::concave_hull

