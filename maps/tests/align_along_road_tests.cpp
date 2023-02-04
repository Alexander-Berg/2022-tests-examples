#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/libs/geolib/include/distance.h>
#include <maps/libs/geolib/include/serialization.h>

#include <maps/wikimap/mapspro/services/autocart/libs/post_processing/include/align_along_road.h>

#include <cmath>
#include <algorithm>

namespace maps {
namespace wiki {
namespace autocart {
namespace tests {

namespace {

bool isEqualSegments(const geolib3::Segment2& seg1,
                     const geolib3::Segment2& seg2) {
    return seg1.start() == seg2.start() && seg1.end() == seg2.end() ||
           seg1.start() == seg2.end() && seg1.end() == seg2.start();
}

bool isEqualPolylines(const geolib3::Polyline2& line1,
                      const geolib3::Polyline2& line2) {
    if (line1.pointsNumber() != line2.pointsNumber()) {
        return false;
    }

    size_t ptsCnt = line1.pointsNumber();

    bool isEqual = true;

    for (size_t i = 0; i < ptsCnt; i++) {
        if (line1.pointAt(i) != line2.pointAt(i)) {
            isEqual = false;
            break;
        }
    }

    if (isEqual == true) {
        return true;
    }

    isEqual = true;

    for (size_t i = 0; i < ptsCnt; i++) {
        if (line1.pointAt(i) != line2.pointAt(ptsCnt - i - 1)) {
            isEqual = false;
            break;
        }
    }
    return isEqual;
}

bool isEqualPolylinesVector(const std::vector<geolib3::Polyline2>& lines1,
                            const std::vector<geolib3::Polyline2>& lines2) {
    if (lines1.size() != lines2.size()) {
        return false;
    }

    for (const auto& line1 : lines1) {
        bool findEqual = false;
        for (const auto& line2 : lines2) {
            if (isEqualPolylines(line1, line2)) {
                findEqual = true;
            }
        }

        if (!findEqual) {
            return false;
        }
    }

    return true;
}

bool isEqualPolygons(const geolib3::Polygon2& poly1,
                     const geolib3::Polygon2& poly2) {
    if (poly1.pointsNumber() != poly2.pointsNumber()) {
        return false;
    }

    size_t ptsCnt = poly1.pointsNumber();

    for (size_t shift = 0; shift < ptsCnt; shift++) {
        bool isEqual = true;
        for (size_t i = 0; i < ptsCnt; i++) {
            geolib3::Point2 pt1 = poly1.pointAt(i);
            geolib3::Point2 pt2 = poly2.pointAt((i + shift) % ptsCnt);
            if (pt1 != pt2) {
                isEqual = false;
                break;
            }
        }
        if (isEqual) {
            return true;
        }
    }

    return false;
}

bool isEqualPolygonsVector(const std::vector<geolib3::Polygon2>& polygons1,
                           const std::vector<geolib3::Polygon2>& polygons2) {
    if (polygons1.size() != polygons2.size()) {
        return false;
    }

    for (const auto& polygon1 : polygons1) {
        bool findEqual = false;
        for (const auto& polygon2 : polygons2) {
            if (isEqualPolygons(polygon1, polygon2)) {
                findEqual = true;
            }
        }

        if (!findEqual) {
            return false;
        }
    }

    return true;
}

} //anonymous namespace


Y_UNIT_TEST_SUITE(projection_to_segment_tests)
{

    Y_UNIT_TEST(test_projection_axis_aligned_polygon_to_big_segment)
    {
        geolib3::Polygon2 polygon({{0, 0}, {2, 0}, {2, 2}, {0, 2}});
        geolib3::Segment2 segment(geolib3::Point2(-1, 0),
                                  geolib3::Point2(3, 0));

        geolib3::Segment2 expectedSegment(geolib3::Point2(0, 0),
                                          geolib3::Point2(2, 0));

        std::optional<geolib3::Segment2> result = project(polygon, segment);

        EXPECT_EQ(result.has_value(), true);
        EXPECT_EQ(isEqualSegments(*result, expectedSegment), true);
    }

    Y_UNIT_TEST(test_projection_axis_aligned_polygon_to_small_segment)
    {
        geolib3::Polygon2 polygon({{0, 0}, {2, 0}, {2, 2}, {0, 2}});
        geolib3::Segment2 segment(geolib3::Point2(-1, 0),
                                  geolib3::Point2(1, 0));

        geolib3::Segment2 expectedSegment(geolib3::Point2(0, 0),
                                          geolib3::Point2(1, 0));

        std::optional<geolib3::Segment2> result = project(polygon, segment);

        EXPECT_EQ(result.has_value(), true);
        EXPECT_EQ(isEqualSegments(*result, expectedSegment), true);
    }

    Y_UNIT_TEST(test_projection_not_axis_aligned_polygon_to_big_segment)
    {
        geolib3::Polygon2 polygon({{0, 1}, {1, 0}, {2, 1}, {1, 2}});
        geolib3::Segment2 segment(geolib3::Point2(-1, 0),
                                  geolib3::Point2(3, 0));

        geolib3::Segment2 expectedSegment(geolib3::Point2(0, 0),
                                          geolib3::Point2(2, 0));

        std::optional<geolib3::Segment2> result = project(polygon, segment);

        EXPECT_EQ(result.has_value(), true);
        EXPECT_EQ(isEqualSegments(*result, expectedSegment), true);
    }

    Y_UNIT_TEST(test_projection_not_axis_aligned_polygon_to_small_segment)
    {
        geolib3::Polygon2 polygon({{0, 1}, {1, 0}, {1, 1}, {1, 2}});
        geolib3::Segment2 segment(geolib3::Point2(-1, 0),
                                  geolib3::Point2(1, 0));

        geolib3::Segment2 expectedSegment(geolib3::Point2(0, 0),
                                          geolib3::Point2(1, 0));

        std::optional<geolib3::Segment2> result = project(polygon, segment);

        EXPECT_EQ(result.has_value(), true);
        EXPECT_EQ(isEqualSegments(*result, expectedSegment), true);
    }

    Y_UNIT_TEST(test_projection_to_rotated_segment)
    {
        geolib3::Polygon2 polygon({{0, 1}, {1, 0}, {1, 1}, {1, 2}});
        geolib3::Segment2 segment(geolib3::Point2(-2, 1),
                                  geolib3::Point2(1, -2));

        geolib3::Segment2 expectedSegment(geolib3::Point2(-1, 0),
                                          geolib3::Point2(0, -1));

        std::optional<geolib3::Segment2> result = project(polygon, segment);

        EXPECT_EQ(result.has_value(), true);
        EXPECT_EQ(isEqualSegments(*result, expectedSegment), true);
    }

    Y_UNIT_TEST(test_projection_not_exist)
    {
        geolib3::Polygon2 polygon({{0, 1}, {1, 0}, {1, 1}, {1, 2}});
        geolib3::Segment2 segment(geolib3::Point2(2, 0),
                                  geolib3::Point2(3, 0));

        std::optional<geolib3::Segment2> result = project(polygon, segment);

        EXPECT_EQ(result.has_value(), false);
    }

} //Y_UNIT_TEST_SUITE(projection_to_segment_tests)

Y_UNIT_TEST_SUITE(connect_segments_tests)
{

    Y_UNIT_TEST(test_connect_start_to_start)
    {
        geolib3::Segment2 seg1(geolib3::Point2(1, 0), geolib3::Point2(0, 0));
        geolib3::Segment2 seg2(geolib3::Point2(1, 0), geolib3::Point2(2, 0));
        AlignAlongRoadsParams params;
        params.maxConnectedRoadDist = 2.;
        params.maxLineBendingPercent = 0.1;

        std::vector<geolib3::Polyline2> expectedLines
                = {
                      geolib3::Polyline2({{0, 0}, {1, 0}, {2, 0}})
                  };
        auto result = connectSegments({seg1, seg2}, params);

        EXPECT_EQ(isEqualPolylinesVector(result, expectedLines), true);
    }

    Y_UNIT_TEST(test_connect_start_to_end)
    {
        geolib3::Segment2 seg1(geolib3::Point2(1, 0), geolib3::Point2(2, 0));
        geolib3::Segment2 seg2(geolib3::Point2(0, 0), geolib3::Point2(1, 0));
        AlignAlongRoadsParams params;
        params.maxConnectedRoadDist = 2.;
        params.maxLineBendingPercent = 0.1;

        std::vector<geolib3::Polyline2> expectedLines
                = {
                      geolib3::Polyline2({{0, 0}, {1, 0}, {2, 0}})
                  };
        auto result = connectSegments({seg1, seg2}, params);
        EXPECT_EQ(isEqualPolylinesVector(result, expectedLines), true);
    }

    Y_UNIT_TEST(test_connect_end_to_start)
    {
        geolib3::Segment2 seg1(geolib3::Point2(2, 0), geolib3::Point2(1, 0));
        geolib3::Segment2 seg2(geolib3::Point2(1, 0), geolib3::Point2(0, 0));
        AlignAlongRoadsParams params;
        params.maxConnectedRoadDist = 2.;
        params.maxLineBendingPercent = 0.1;

        std::vector<geolib3::Polyline2> expectedLines
                = {
                      geolib3::Polyline2({{0, 0}, {1, 0}, {2, 0}})
                  };
        auto result = connectSegments({seg1, seg2}, params);

        EXPECT_EQ(isEqualPolylinesVector(result, expectedLines), true);
    }

    Y_UNIT_TEST(test_connect_end_to_end)
    {
        geolib3::Segment2 seg1(geolib3::Point2(2, 0), geolib3::Point2(1, 0));
        geolib3::Segment2 seg2(geolib3::Point2(0, 0), geolib3::Point2(1, 0));
        AlignAlongRoadsParams params;
        params.maxConnectedRoadDist = 2.;
        params.maxLineBendingPercent = 0.1;

        std::vector<geolib3::Polyline2> expectedLines
                = {
                      geolib3::Polyline2({{0, 0}, {1, 0}, {2, 0}})
                  };
        auto result = connectSegments({seg1, seg2}, params);

        EXPECT_EQ(isEqualPolylinesVector(result, expectedLines), true);
    }

    Y_UNIT_TEST(test_connect_not_connected)
    {
        geolib3::Segment2 seg1(geolib3::Point2(2, 0), geolib3::Point2(1, 0));
        geolib3::Segment2 seg2(geolib3::Point2(-2, 0), geolib3::Point2(-1, 0));
        AlignAlongRoadsParams params;
        params.maxConnectedRoadDist = 2.;
        params.maxLineBendingPercent = 0.1;

        std::vector<geolib3::Polyline2> expectedLines
                = {
                      geolib3::Polyline2(seg1),
                      geolib3::Polyline2(seg2),
                  };
        auto result = connectSegments({seg1, seg2}, params);

        EXPECT_EQ(isEqualPolylinesVector(result, expectedLines), true);
    }

    Y_UNIT_TEST(test_connect_many_lines)
    {
        geolib3::Segment2 seg1(geolib3::Point2(-2, 0), geolib3::Point2(-1, 0));
        geolib3::Segment2 seg2(geolib3::Point2(-1, 0), geolib3::Point2(1, 0));
        geolib3::Segment2 seg3(geolib3::Point2(1, 0), geolib3::Point2(2, 0));
        geolib3::Segment2 seg4(geolib3::Point2(0, -2), geolib3::Point2(0, -1));
        geolib3::Segment2 seg5(geolib3::Point2(0, -1), geolib3::Point2(0, 1));
        geolib3::Segment2 seg6(geolib3::Point2(0, 1), geolib3::Point2(0, 2));
        AlignAlongRoadsParams params;
        params.maxConnectedRoadDist = 2.;
        params.maxLineBendingPercent = 0.1;

        std::vector<geolib3::Polyline2> expectedLines
                = {
                      geolib3::Polyline2({{-2, 0}, {-1, 0}, {1, 0}, {2, 0}}),
                      geolib3::Polyline2({{0, -2}, {0, -1}, {0, 1}, {0, 2}}),
                  };
        auto result = connectSegments({seg1, seg2, seg3, seg4, seg5, seg6},
                                      params);

        EXPECT_EQ(isEqualPolylinesVector(result, expectedLines), true);
    }

} //Y_UNIT_TEST_SUITE(connect_segments_tests)

Y_UNIT_TEST_SUITE(is_straight_line_tests)
{

    Y_UNIT_TEST(test_parallel_segments_is_straight_line)
    {
        geolib3::Polyline2 line({{-2, 0}, {-1, 0}, {1, 0}, {2, 0}});
        double lineEpsilon = 0.1;

        EXPECT_EQ(isStraightLine(line, lineEpsilon), true);
    }

    Y_UNIT_TEST(test_line_with_self_intersection_not_straight_line)
    {
        geolib3::Polyline2 line({{-2, 0}, {0, 0},
                                 {1, 1}, {-1, 1},
                                 {0, 0}, {2, 0}});
        double lineEpsilon = 0.1;

        EXPECT_EQ(isStraightLine(line, lineEpsilon), false);
    }

    Y_UNIT_TEST(test_line_with_small_bend_is_straight_line)
    {
        geolib3::Polyline2 line({{-2, 0}, {-1, 0.1}, {1, -0.1}, {2, 0}});
        double lineEpsilon = 0.1;

        EXPECT_EQ(isStraightLine(line, lineEpsilon), true);
    }

    Y_UNIT_TEST(test_line_with_large_bend_not_straight_line)
    {
        geolib3::Polyline2 line({{-2, 0}, {-1, 0.5}, {1, -0.5}, {2, 0}});
        double lineEpsilon = 0.1;

        EXPECT_EQ(isStraightLine(line, lineEpsilon), false);
    }

} //Y_UNIT_TEST_SUITE(is_straight_line_tests)

Y_UNIT_TEST_SUITE(align_to_road_tests)
{

    Y_UNIT_TEST(test_align_to_close_road)
    {
        geolib3::Polyline2 road({{-2, 0}, {0, 0}, {2, 0}});
        geolib3::Polygon2 bld({{-1, 1}, {0, 0}, {1, 1}, {0, 2}});
        AlignAlongRoadsParams params;
        params.maxDistToRoad = 10.;
        params.maxAngleRotationDegree = 50.;

        std::vector<geolib3::Polygon2> expectedBlds
                = {
                      geolib3::Polygon2({{-sqrt(2) / 2., 1 - sqrt(2) / 2.},
                                         {sqrt(2) / 2., 1 - sqrt(2) / 2.},
                                         {sqrt(2) / 2., 1 + sqrt(2) / 2.},
                                         {-sqrt(2) / 2., 1 +sqrt(2) / 2.}})
                  };

        auto result = alignAlongRoads({bld}, {road}, params);
        EXPECT_EQ(isEqualPolygonsVector(result, expectedBlds), true);
    }

    Y_UNIT_TEST(test_not_align_bld_with_big_angle)
    {
        geolib3::Polyline2 road({{-2, 0}, {0, 0}, {2, 0}});
        geolib3::Polygon2 bld({{-1, 1}, {0, 0}, {1, 1}, {0, 2}});
        AlignAlongRoadsParams params;
        params.maxDistToRoad = 10.;
        params.maxAngleRotationDegree = 20.;

        std::vector<geolib3::Polygon2> expectedBlds
                = {
                      geolib3::Polygon2({{-1, 1}, {0, 0}, {1, 1}, {0, 2}})
                  };

        auto result = alignAlongRoads({bld}, {road}, params);
        EXPECT_EQ(isEqualPolygonsVector(result, expectedBlds), true);
    }

    Y_UNIT_TEST(test_not_align_to_not_close_road)
    {
        geolib3::Polyline2 road({{-2, 0}, {0, 0}, {2, 0}});
        geolib3::Polygon2 bld({{-1, 15}, {0, 14}, {1, 15}, {0, 16}});
        AlignAlongRoadsParams params;
        params.maxDistToRoad = 10.;

        std::vector<geolib3::Polygon2> expectedBlds
                = {
                      geolib3::Polygon2({{-1, 15}, {0, 14}, {1, 15}, {0, 16}})
                  };

        auto result = alignAlongRoads({bld}, {road}, params);
        EXPECT_EQ(isEqualPolygonsVector(result, expectedBlds), true);
    }

} //Y_UNIT_TEST_SUITE(align_to_road_tests)

} //namespace tests
} //namespace autocart
} //namespace wiki
} //namespace maps
