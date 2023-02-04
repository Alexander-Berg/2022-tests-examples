#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/wikimap/mapspro/services/autocart/libs/post_processing/include/align_angles.h>

#include <cmath>

namespace maps {
namespace wiki {
namespace autocart {
namespace tests {

namespace {

std::vector<geolib3::Point2> getVertices(const geolib3::Polygon2& poly) {
    std::vector<geolib3::Point2> vertices;
    for (size_t i = 0; i < poly.pointsNumber(); i++) {
        vertices.push_back(poly.pointAt(i));
    }
    return vertices;
}

bool isPointsEqual(const geolib3::Point2& pt1, const geolib3::Point2& pt2) {
    const float EPS = 0.01;
    if(fabs(pt1.x()-pt2.x()) > EPS || fabs(pt1.y()-pt2.y()) > EPS) {
        return false;
    }

    return true;
}

bool compare(const std::vector<geolib3::Point2>& arr1,
             const std::vector<geolib3::Point2>& arr2) {
    if(arr1.size() != arr2.size()) {
        return false;
    }

    for(size_t i = 0; i < arr1.size(); i++) {
        if(!isPointsEqual(arr1[i], arr2[i])) {
            return false;
        }
    }
    return true;
}

bool isDirectionEqual(const Edge& edge1, const Edge& edge2) {
    const float& angle1 = edge1.getAngle();
    const float& angle2 = edge2.getAngle();

    float direction1;
    if (angle1 < 0) {
        direction1 = M_PI + angle1;
    } else if (angle1 > M_PI) {
        direction1 = angle1 - M_PI;
    } else {
        direction1 = angle1;
    }

    float direction2;
    if (angle2 < 0) {
        direction2 = M_PI + angle2;
    } else if (angle2 > M_PI) {
        direction2 = angle2 - M_PI;
    } else {
        direction2 = angle2;
    }

    const float DIR_EPS = 0.01;
    if (fabs(direction1 - direction2) < DIR_EPS) {
        return true;
    } else {
        return false;
    }
}

bool isEdgesEqual(const Edge& edge1, const Edge& edge2) {
    if(!isDirectionEqual(edge1, edge2)) {
        return false;
    } else {
        const geolib3::Point2& pt1 = edge1.getPoint();
        const geolib3::Point2& pt2 = edge2.getPoint();
        if (isPointsEqual(pt1, pt2)) {
            return true;
        } else {
            if(isDirectionEqual(Edge(pt1, pt2), edge1) && isDirectionEqual(Edge(pt1, pt2), edge2)) {
                return true;
            } else {
                return false;
            }
        }
    }
}

bool compare(const std::vector<Edge>& arr1,
             const std::vector<Edge>& arr2) {
    if(arr1.size() != arr2.size()) {
        return false;
    }

    for(size_t i = 0; i < arr1.size(); i++) {
        if (!isEdgesEqual(arr1[i], arr2[i])) {
            return false;
        }
    }

    return true;
}

} //anonymous namespace

Y_UNIT_TEST_SUITE(align_angles_tests)
{

    Y_UNIT_TEST(test_simplify_triangle)
    {
        std::vector<geolib3::Point2> srcVertices = {{0, 0}, {0.5, 0.5}, {1, 1}, {0, 1}};
        std::vector<geolib3::Point2> expectedVertices = {{0, 0}, {1, 1}, {0, 1}};

        geolib3::Polygon2 dstPolygon = alignAngles(geolib3::Polygon2(srcVertices));
        std::vector<geolib3::Point2> dstVertices = getVertices(dstPolygon);

        EXPECT_TRUE(compare(expectedVertices, dstVertices));
    }

    Y_UNIT_TEST(test_simplify_rectangle)
    {
        std::vector<geolib3::Point2> srcVertices = {{0, 0}, {10, 0}, {10, 5}, {10, 10}, {0, 10}};
        std::vector<geolib3::Point2> expectedVertices = {{0, 0}, {10, 0}, {10, 10}, {0, 10}};

        geolib3::Polygon2 dstPolygon = alignAngles(geolib3::Polygon2(srcVertices));
        std::vector<geolib3::Point2> dstVertices = getVertices(dstPolygon);

        EXPECT_TRUE(compare(expectedVertices, dstVertices));
    }

    Y_UNIT_TEST(test_align_triangle)
    {
        std::vector<geolib3::Point2> srcVertices = {{0, 0}, {15, 0}, {17, 17}};
        std::vector<geolib3::Point2> expectedVertices = {{0, 0}, {16, 0}, {16, 16}};

        geolib3::Polygon2 dstPolygon = alignAngles(geolib3::Polygon2(srcVertices));
        std::vector<geolib3::Point2> dstVertices = getVertices(dstPolygon);

        EXPECT_TRUE(compare(expectedVertices, dstVertices));
    }

    Y_UNIT_TEST(test_not_change_rectangle)
    {
        std::vector<geolib3::Point2> srcVertices = {{0, 0}, {10, 0}, {10, 10}, {0, 10}};
        std::vector<geolib3::Point2> expectedVertices = {{0, 0}, {10, 0}, {10, 10}, {0, 10}};

        geolib3::Polygon2 dstPolygon = alignAngles(geolib3::Polygon2(srcVertices));
        std::vector<geolib3::Point2> dstVertices = getVertices(dstPolygon);

        EXPECT_TRUE(compare(expectedVertices, dstVertices));
    }

    Y_UNIT_TEST(test_align_polyhedron)
    {
        std::vector<geolib3::Point2> srcVertices = {{0, 0}, {20, 0}, {20, 20}, {10, 22}, {0, 20}};
        std::vector<geolib3::Point2> expectedVertices = {{0, 0}, {20, 0}, {20, 21}, {0, 21}};

        geolib3::Polygon2 dstPolygon = alignAngles(geolib3::Polygon2(srcVertices));
        std::vector<geolib3::Point2> dstVertices = getVertices(dstPolygon);

        EXPECT_TRUE(compare(expectedVertices, dstVertices));
    }

    Y_UNIT_TEST(test_not_align_big_obtuse_angle)
    {
        std::vector<geolib3::Point2> srcVertices = {{0, 0}, {20, 0}, {20, 20}, {10, 25}, {0, 20}};
        std::vector<geolib3::Point2> expectedVertices = {{0, 0}, {20, 0}, {20, 20}, {10, 25}, {0, 20}};

        geolib3::Polygon2 dstPolygon = alignAngles(geolib3::Polygon2(srcVertices), M_PI/15.f);
        std::vector<geolib3::Point2> dstVertices = getVertices(dstPolygon);

        EXPECT_TRUE(compare(expectedVertices, dstVertices));
    }

    Y_UNIT_TEST(test_not_rotate_fixed_edge)
    {
        std::vector<Edge> srcEdges = {
                                         {{0, 0}, {15, 0}},
                                         {{15, 0}, {17, 17}},
                                         {{17, 17}, {0, 0}}
                                     };

        std::vector<Edge> expectedEdges = {
                                              {{0, 0}, {15, 0}},
                                              {{15, 0}, {17, 17}},
                                              {{17, 17}, {0, 0}}
                                          };

        std::vector<Edge> dstEdges = doAlignAngles(1, 0, srcEdges, M_PI/15.f);

        EXPECT_TRUE(compare(expectedEdges, dstEdges));
    }

    Y_UNIT_TEST(test_rotate_not_fixed_edge)
    {
        std::vector<Edge> srcEdges = {
                                         {{0, 0}, {15, 0}},
                                         {{15, 0}, {17, 17}},
                                         {{17, 17}, {0, 0}}
                                     };
        std::vector<Edge> expectedEdges = {
                                              {{0, 0}, {16, 0}},
                                              {{16, 0}, {16, 16}},
                                              {{16, 16}, {0, 0}}
                                          };

        std::vector<Edge> dstEdges = doAlignAngles(0, 1, srcEdges, M_PI/15.f);

        EXPECT_TRUE(compare(expectedEdges, dstEdges));
    }

} //Y_UNIT_TEST_SUITE(align_angles_tests)

} //namespace tests
} //namespace autocart
} //namespace wiki
} //namespace maps
