#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/libs/geolib/include/distance.h>
#include <maps/libs/geolib/include/serialization.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <maps/libs/tile/include/const.h>
#include <maps/libs/tile/include/utils.h>

#include <maps/wikimap/mapspro/services/autocart/libs/post_processing/include/post_processing.h>

#include <cmath>

namespace maps::wiki::autocart {
namespace tests {

namespace {

bool compareAngles(const float& angle1, const float& angle2) {
    const float EPS = 1e-5f;
    return fabs(angle1 - angle2) < EPS || 90.f - fabs(angle1 - angle2) < EPS;
}

bool isPointsEqual(const geolib3::Point2& pt1, const geolib3::Point2& pt2) {
    const float EPS = 1e-5f;
    if(geolib3::distance(pt1, pt2) > EPS) {
        return false;
    }
    return true;
}

bool compare(const geolib3::Polygon2& poly1,
             const geolib3::Polygon2& poly2) {
    if(poly1.pointsNumber() != poly2.pointsNumber()) {
        return false;
    }

    const size_t ptsCnt = poly1.pointsNumber();
    for (size_t shift = 0; shift < ptsCnt; shift++) {
        bool isEqual = true;
        for(size_t i = 0; i < ptsCnt; i++) {
            if(!isPointsEqual(poly1.pointAt((shift+i) % ptsCnt), poly2.pointAt(i))) {
                isEqual = false;
            }
        }
        if (isEqual) {
            return true;
        }
    }

    return false;
}

bool compare(const std::vector<geolib3::Point2>& pts1,
             const std::vector<geolib3::Point2>& pts2) {
    if (pts1.size() != pts2.size()) {
        return false;
    }

    for (const auto& pt : pts1) {
        if (std::find(pts2.begin(), pts2.end(), pt) == pts2.end()) {
            return false;
        }
    }
    return true;
}

} //anonymous namespace

Y_UNIT_TEST_SUITE(align_rectangles_tests)
{

    Y_UNIT_TEST(test_normalized_angle)
    {
        geolib3::Polygon2 rect({{12, 4}, {2, 9}, {0, 5}, {10, 0}});
        float expectedAngle = atan2(2, 1) * 180.f / M_PI;

        EXPECT_TRUE(compareAngles(expectedAngle, getNormalizedAngle(rect)));
    }

    Y_UNIT_TEST(test_equal_rects_have_equal_normalized_angle)
    {
        geolib3::Polygon2 rect1({{10, 0}, {20, 10}, {10, 20}, {0, 10}});
        geolib3::Polygon2 rect2({{20, 10}, {10, 20}, {0, 10}, {10, 0}});
        float expectedAngle = 45.f;

        EXPECT_TRUE(compareAngles(expectedAngle, getNormalizedAngle(rect1)));
        EXPECT_TRUE(compareAngles(expectedAngle, getNormalizedAngle(rect2)));
    }

    Y_UNIT_TEST(test_two_rect_have_equal_angle_after_align)
    {
        geolib3::Polygon2 rect1({{0, 0}, {2, 0}, {2, 2}, {0, 2}});
        geolib3::Polygon2 rect2({{1, 0}, {2, 1}, {1, 2}, {0, 1}});
        float avgAngle = 22.5f;

        auto alignedRects = alignRectangles({rect1, rect2}, 2, 50.f, 10.f, 1);

        EXPECT_TRUE(compareAngles(avgAngle, getNormalizedAngle(alignedRects[0])));
        EXPECT_TRUE(compareAngles(avgAngle, getNormalizedAngle(alignedRects[1])));
    }

    Y_UNIT_TEST(test_not_change_rects_with_large_distance)
    {
        geolib3::Polygon2 rect1({{10, 0}, {12, 0}, {12, 2}, {10, 2}});
        geolib3::Polygon2 rect2({{1, 0}, {2, 1}, {1, 2}, {0, 1}});
        geolib3::Polygon2 rect3({{1, 10}, {2, 11}, {1, 12}, {0, 11}});

        auto alignedRects = alignRectangles({rect1, rect2, rect3}, 2, 50.f, 5.f, 5);

        EXPECT_TRUE(compare(rect1, alignedRects[0]));
        EXPECT_TRUE(compare(rect2, alignedRects[1]));
        EXPECT_TRUE(compare(rect3, alignedRects[2]));
    }

    Y_UNIT_TEST(test_not_change_rects_with_large_angle_diff)
    {
        geolib3::Polygon2 rect1({{5, 0}, {7, 0}, {7, 2}, {5, 2}});
        geolib3::Polygon2 rect2({{1, 0}, {2, 1}, {1, 2}, {0, 1}});

        auto alignedRects = alignRectangles({rect1, rect2}, 2, 10.f, 10.f, 5);

        EXPECT_TRUE(compare(rect1, alignedRects[0]));
        EXPECT_TRUE(compare(rect2, alignedRects[1]));
    }

    Y_UNIT_TEST(test_align_only_neighbors)
    {
        geolib3::Polygon2 rect1({{0, 0}, {2, 0}, {2, 2}, {0, 2}});
        geolib3::Polygon2 rect2({{1, 1}, {2, 2}, {1, 3}, {0, 2}});
        float avgAngle = 22.5f;

        geolib3::Polygon2 rect3({{10, 0}, {12, 0}, {12, 2}, {10, 2}});


        auto alignedRects = alignRectangles({rect1, rect2, rect3}, 2, 50.f, 5.f, 5);

        EXPECT_TRUE(compareAngles(avgAngle, getNormalizedAngle(alignedRects[0])));
        EXPECT_TRUE(compareAngles(avgAngle, getNormalizedAngle(alignedRects[1])));
        EXPECT_TRUE(compare(rect3, alignedRects[2]));
    }

} //Y_UNIT_TEST_SUITE(align_rectangles_tests)

Y_UNIT_TEST_SUITE(move_edge_tests)
{

    Y_UNIT_TEST(test_not_change_axis_aligned_rectangle_with_zero_step)
    {
        geolib3::Polygon2 rect({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        int edgeIndex = 0;
        double step = 0.;

        geolib3::Polygon2 expectedRect({{0, 0}, {1, 0}, {1, 1}, {0, 1}});

        moveEdge(rect, edgeIndex, step);

        EXPECT_TRUE(compare(rect, expectedRect));
    }

    Y_UNIT_TEST(test_not_change_rotated_rectangle_with_zero_step)
    {
        geolib3::Polygon2 rect({{0, 3}, {4, 0}, {7, 4}, {3, 7}});
        int edgeIndex = 0;
        double step = 0.;

        geolib3::Polygon2 expectedRect({{0, 3}, {4, 0}, {7, 4}, {3, 7}});

        moveEdge(rect, edgeIndex, step);

        EXPECT_TRUE(compare(rect, expectedRect));
    }

    Y_UNIT_TEST(test_increase_axis_aligned_rectangle_with_negative_step)
    {
        geolib3::Polygon2 rect({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        int edgeIndex = 0;
        double step = -1.;

        geolib3::Polygon2 expectedRect({{0, -1}, {1, -1}, {1, 1}, {0, 1}});

        moveEdge(rect, edgeIndex, step);

        EXPECT_TRUE(compare(rect, expectedRect));
    }

    Y_UNIT_TEST(test_increase_rotated_rectangle_with_negative_step)
    {
        geolib3::Polygon2 rect({{0, 3}, {4, 0}, {7, 4}, {3, 7}});
        int edgeIndex = 2;
        double step = -5.;

        geolib3::Polygon2 expectedRect({{0, 3}, {4, 0}, {10, 8}, {6, 11}});

        moveEdge(rect, edgeIndex, step);

        EXPECT_TRUE(compare(rect, expectedRect));
    }

    Y_UNIT_TEST(test_decrease_axis_aligned_rectangle_with_positive_step)
    {
        geolib3::Polygon2 rect({{0, -1}, {1, -1}, {1, 1}, {0, 1}});
        int edgeIndex = 0;
        double step = 1.;

        geolib3::Polygon2 expectedRect({{0, 0}, {1, 0}, {1, 1}, {0, 1}});

        moveEdge(rect, edgeIndex, step);

        EXPECT_TRUE(compare(rect, expectedRect));
    }

    Y_UNIT_TEST(test_decrease_rotated_rectangle_with_positive_step)
    {
        geolib3::Polygon2 rect({{0, 3}, {4, 0}, {10, 8}, {6, 11}});
        int edgeIndex = 2;
        double step = 5.;

        geolib3::Polygon2 expectedRect({{0, 3}, {4, 0}, {7, 4}, {3, 7}});

        moveEdge(rect, edgeIndex, step);

        EXPECT_TRUE(compare(rect, expectedRect));
    }


} //Y_UNIT_TEST_SUITE(move_edge_tests)

Y_UNIT_TEST_SUITE(rectangle_intersection_tests)
{

    Y_UNIT_TEST(test_not_intersected_rectangles)
    {
        geolib3::Polygon2 rect1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 rect2({{2, 0}, {3, 0}, {3, 1}, {2, 1}});

        auto ptsAndEdges = intersection(rect1, rect2);
        const std::vector<geolib3::Point2>& pts = ptsAndEdges.first;
        const std::unordered_set<int>& edges = ptsAndEdges.second;

        EXPECT_TRUE(pts.empty());

        EXPECT_TRUE(edges.empty());
    }

    Y_UNIT_TEST(test_point_contact)
    {
        geolib3::Polygon2 rect1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 rect2({{1, 1}, {2, 1}, {2, 2}, {1, 2}});

        std::vector<geolib3::Point2> expectedPts({{1, 1}});
        std::unordered_set<int> expectedEdges({1, 2});

        auto ptsAndEdges = intersection(rect1, rect2);
        const std::vector<geolib3::Point2>& pts = ptsAndEdges.first;
        const std::unordered_set<int>& edges = ptsAndEdges.second;

        EXPECT_TRUE(compare(pts, expectedPts));
        EXPECT_TRUE(edges == expectedEdges);
    }

    Y_UNIT_TEST(test_line_contact)
    {
        geolib3::Polygon2 rect1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 rect2({{1, 0}, {2, 1}, {2, 1}, {1, 1}});

        std::vector<geolib3::Point2> expectedPts({{1, 0}, {1, 1}});
        std::unordered_set<int> expectedEdges({0, 1, 2});

        auto ptsAndEdges = intersection(rect1, rect2);
        const std::vector<geolib3::Point2>& pts = ptsAndEdges.first;
        const std::unordered_set<int>& edges = ptsAndEdges.second;

        EXPECT_TRUE(compare(pts, expectedPts));
        EXPECT_TRUE(edges == expectedEdges);
    }

    Y_UNIT_TEST(test_opposite_edges_in_intersection)
    {
        geolib3::Polygon2 rect1({{0, 2}, {6, 2}, {6, 4}, {0, 4}});
        geolib3::Polygon2 rect2({{2, 0}, {4, 0}, {4, 6}, {2, 6}});

        std::vector<geolib3::Point2> expectedPts({{2, 2}, {2, 4}, {4, 4}, {4, 2}});
        std::unordered_set<int> expectedEdges({0, 2});

        auto ptsAndEdges = intersection(rect1, rect2);
        const std::vector<geolib3::Point2>& pts = ptsAndEdges.first;
        const std::unordered_set<int>& edges = ptsAndEdges.second;

        EXPECT_TRUE(compare(pts, expectedPts));
        EXPECT_TRUE(edges == expectedEdges);
    }

    Y_UNIT_TEST(test_neighbor_edges_in_intersection)
    {
        geolib3::Polygon2 rect1({{2, 0}, {4, 0}, {4, 2}, {2, 2}});
        geolib3::Polygon2 rect2({{0, 5}, {5, 0}, {10, 5}, {5, 10}});

        std::vector<geolib3::Point2> expectedPts({{3, 2}, {4, 1}, {4, 2}});
        std::unordered_set<int> expectedEdges({1, 2});

        auto ptsAndEdges = intersection(rect1, rect2);
        const std::vector<geolib3::Point2>& pts = ptsAndEdges.first;
        const std::unordered_set<int>& edges = ptsAndEdges.second;

        EXPECT_TRUE(compare(pts, expectedPts));
        EXPECT_TRUE(edges == expectedEdges);
    }

    Y_UNIT_TEST(test_self_intersection)
    {
        geolib3::Polygon2 rect1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 rect2({{0, 0}, {1, 0}, {1, 1}, {0, 1}});

        std::vector<geolib3::Point2> expectedPts({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        std::unordered_set<int> expectedEdges({0, 1, 2, 3});

        auto ptsAndEdges = intersection(rect1, rect2);
        const std::vector<geolib3::Point2>& pts = ptsAndEdges.first;
        const std::unordered_set<int>& edges = ptsAndEdges.second;

        EXPECT_TRUE(compare(pts, expectedPts));
        EXPECT_TRUE(edges == expectedEdges);
    }

    Y_UNIT_TEST(test_rectangle_within_another)
    {
        geolib3::Polygon2 rect1({{1, 1}, {2, 1}, {2, 2}, {1, 2}});
        geolib3::Polygon2 rect2({{0, 0}, {4, 0}, {4, 4}, {0, 4}});

        std::vector<geolib3::Point2> expectedPts({{1, 1}, {2, 1}, {2, 2}, {1, 2}});
        std::unordered_set<int> expectedEdges({0, 1, 2, 3});

        auto ptsAndEdges = intersection(rect1, rect2);
        const std::vector<geolib3::Point2>& pts = ptsAndEdges.first;
        const std::unordered_set<int>& edges = ptsAndEdges.second;

        EXPECT_TRUE(compare(pts, expectedPts));
        EXPECT_TRUE(edges == expectedEdges);
    }

    Y_UNIT_TEST(test_three_intersected_edges)
    {
        geolib3::Polygon2 rect1({{0, 2}, {4, 2}, {4, 4}, {0, 4}});
        geolib3::Polygon2 rect2({{1, 0}, {2, 0}, {2, 3}, {1, 3}});

        std::vector<geolib3::Point2> expectedPts({{2, 3}, {1, 3}, {2, 2}, {1, 2}});
        std::unordered_set<int> expectedEdges({0});

        auto ptsAndEdges = intersection(rect1, rect2);
        const std::vector<geolib3::Point2>& pts = ptsAndEdges.first;
        const std::unordered_set<int>& edges = ptsAndEdges.second;

        EXPECT_TRUE(compare(pts, expectedPts));
        EXPECT_TRUE(edges == expectedEdges);
    }

} //Y_UNIT_TEST_SUITE(rectancle_intersection_tests)

Y_UNIT_TEST_SUITE(remove_overlaps_tests)
{

    Y_UNIT_TEST(test_not_change_no_overlaps_rectagles)
    {
        geolib3::Polygon2 rect1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 rect2({{2, 0}, {3, 0}, {3, 1}, {2, 1}});
        std::vector<geolib3::Polygon2> rects({rect1, rect2});

        geolib3::Polygon2 expectedRect1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 expectedRect2({{2, 0}, {3, 0}, {3, 1}, {2, 1}});

        removeIntersections(rects);

        EXPECT_TRUE(compare(rects[0], expectedRect1));
        EXPECT_TRUE(compare(rects[1], expectedRect2));
    }

    Y_UNIT_TEST(test_not_change_rectagles_with_point_contact)
    {
        geolib3::Polygon2 rect1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 rect2({{1, 1}, {2, 1}, {2, 2}, {1, 2}});
        std::vector<geolib3::Polygon2> rects({rect1, rect2});

        geolib3::Polygon2 expectedRect1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 expectedRect2({{1, 1}, {2, 1}, {2, 2}, {1, 2}});

        removeIntersections(rects);

        EXPECT_TRUE(compare(rects[0], expectedRect1));
        EXPECT_TRUE(compare(rects[1], expectedRect2));
    }

    Y_UNIT_TEST(test_not_change_rectagles_with_line_contact)
    {
        geolib3::Polygon2 rect1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 rect2({{1, 0}, {2, 0}, {2, 1}, {1, 1}});
        std::vector<geolib3::Polygon2> rects({rect1, rect2});

        geolib3::Polygon2 expectedRect1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 expectedRect2({{1, 0}, {2, 0}, {2, 1}, {1, 1}});

        removeIntersections(rects);

        EXPECT_TRUE(compare(rects[0], expectedRect1));
        EXPECT_TRUE(compare(rects[1], expectedRect2));
    }

    Y_UNIT_TEST(test_axis_aligned_rectangles_with_two_points_inside)
    {
        geolib3::Polygon2 rect1({{0, 0}, {4, 0}, {4, 8}, {0, 8}});
        geolib3::Polygon2 rect2({{2, 2}, {10, 2}, {10, 6}, {2, 6}});
        std::vector<geolib3::Polygon2> rects({rect1, rect2});

        geolib3::Polygon2 expectedRect1({{0, 0}, {3, 0}, {3, 8}, {0, 8}});
        geolib3::Polygon2 expectedRect2({{3, 2}, {10, 2}, {10, 6}, {3, 6}});

        removeIntersections(rects);

        EXPECT_TRUE(compare(rects[0], expectedRect1));
        EXPECT_TRUE(compare(rects[1], expectedRect2));
    }

    Y_UNIT_TEST(test_axis_aligned_rectangles_with_corner_intersection)
    {
        geolib3::Polygon2 rect1({{0, 0}, {4, 0}, {4, 4}, {0, 4}});
        geolib3::Polygon2 rect2({{1, 3}, {9, 3}, {9, 7}, {1, 7}});
        std::vector<geolib3::Polygon2> rects({rect1, rect2});

        geolib3::Polygon2 expectedRect1({{0, 0}, {4, 0}, {4, 3.5}, {0, 3.5}});
        geolib3::Polygon2 expectedRect2({{1, 3.5}, {9, 3.5}, {9, 7}, {1, 7}});

        removeIntersections(rects);

        EXPECT_TRUE(compare(rects[0], expectedRect1));
        EXPECT_TRUE(compare(rects[1], expectedRect2));
    }

    Y_UNIT_TEST(test_axis_aligned_rectangles_with_same_height)
    {
        geolib3::Polygon2 rect1({{0, 0}, {10, 0}, {10, 10}, {0, 10}});
        geolib3::Polygon2 rect2({{8, 0}, {18, 0}, {18, 10}, {8, 10}});
        std::vector<geolib3::Polygon2> rects({rect1, rect2});

        geolib3::Polygon2 expectedRect1({{0, 0}, {9, 0}, {9, 10}, {0, 10}});
        geolib3::Polygon2 expectedRect2({{9, 0}, {18, 0}, {18, 10}, {9, 10}});

        removeIntersections(rects);

        EXPECT_TRUE(compare(rects[0], expectedRect1));
        EXPECT_TRUE(compare(rects[1], expectedRect2));
    }

    Y_UNIT_TEST(test_rotated_rectangles_with_two_points_inside)
    {
        geolib3::Polygon2 rect1({{0, 5}, {5, 0}, {10, 5}, {5, 10}});
        geolib3::Polygon2 rect2({{2, 0}, {4, 0}, {4, 5}, {2, 5}});
        std::vector<geolib3::Polygon2> rects({rect1, rect2});

        geolib3::Polygon2 expectedRect1({{1, 6}, {6, 1}, {10, 5}, {5, 10}});
        geolib3::Polygon2 expectedRect2({{2, 0}, {4, 0}, {4, 3}, {2, 3}});

        removeIntersections(rects);

        EXPECT_TRUE(compare(rects[0], expectedRect1));
        EXPECT_TRUE(compare(rects[1], expectedRect2));
    }

    Y_UNIT_TEST(test_rotated_rectangles_with_one_point_inside_one_on_side)
    {
        geolib3::Polygon2 rect1({{0, 5}, {5, 0}, {10, 5}, {5, 10}});
        geolib3::Polygon2 rect2({{2, 0}, {4, 0}, {4, 3}, {2, 3}});
        std::vector<geolib3::Polygon2> rects({rect1, rect2});

        geolib3::Polygon2 expectedRect1({{0.5, 5.5}, {5.5, 0.5}, {10, 5}, {5, 10}});
        geolib3::Polygon2 expectedRect2({{2, 0}, {4, 0}, {4, 2}, {2, 2}});

        removeIntersections(rects);

        EXPECT_TRUE(compare(rects[0], expectedRect1));
        EXPECT_TRUE(compare(rects[1], expectedRect2));
    }

    Y_UNIT_TEST(test_rotated_rectangles_with_one_point_inside)
    {
        geolib3::Polygon2 rect1({{0, 5}, {5, 0}, {10, 5}, {5, 10}});
        geolib3::Polygon2 rect2({{2, 0}, {4, 0}, {4, 2}, {2, 2}});
        std::vector<geolib3::Polygon2> rects({rect1, rect2});

        geolib3::Polygon2 expectedRect1({{0.25, 5.25}, {5.25, 0.25}, {10, 5}, {5, 10}});
        geolib3::Polygon2 expectedRect2({{2, 0}, {3.5, 0}, {3.5, 2}, {2, 2}});

        removeIntersections(rects);

        EXPECT_TRUE(compare(rects[0], expectedRect1));
        EXPECT_TRUE(compare(rects[1], expectedRect2));
    }

    Y_UNIT_TEST(test_rotated_rectangles_with_corner_intersect)
    {
        geolib3::Polygon2 rect1({{0, 5}, {5, 0}, {10, 5}, {5, 10}});
        geolib3::Polygon2 rect2({{3, 0}, {6, 0}, {6, 4}, {3, 4}});
        std::vector<geolib3::Polygon2> rects({rect1, rect2});

        geolib3::Polygon2 expectedRect1({{1.25, 6.25}, {6.25, 1.25}, {10, 5}, {5, 10}});
        geolib3::Polygon2 expectedRect2({{3, 0}, {6, 0}, {6, 1.5}, {3, 1.5}});

        removeIntersections(rects);

        EXPECT_TRUE(compare(rects[0], expectedRect1));
        EXPECT_TRUE(compare(rects[1], expectedRect2));
    }

    Y_UNIT_TEST(test_rotated_rectangles_with_parallel_side)
    {
        geolib3::Polygon2 rect1({{0, 5}, {5, 0}, {10, 5}, {5, 10}});
        geolib3::Polygon2 rect2({{2, 0}, {6, 4}, {4, 6}, {0, 2}});
        std::vector<geolib3::Polygon2> rects({rect1, rect2});

        geolib3::Polygon2 expectedRect1({{1.25, 6.25}, {6.25, 1.25}, {10, 5}, {5, 10}});
        geolib3::Polygon2 expectedRect2({{2, 0}, {4.75, 2.75}, {2.75, 4.75}, {0, 2}});

        removeIntersections(rects);

        EXPECT_TRUE(compare(rects[0], expectedRect1));
        EXPECT_TRUE(compare(rects[1], expectedRect2));
    }

    Y_UNIT_TEST(test_rotated_rectangles_with_side_on_one_line)
    {
        geolib3::Polygon2 rect1({{0, 5}, {5, 0}, {10, 5}, {5, 10}});
        geolib3::Polygon2 rect2({{-2, 3}, {3, -2}, {7, 2}, {2, 7}});
        std::vector<geolib3::Polygon2> rects({rect1, rect2});

        geolib3::Polygon2 expectedRect1({{1, 6}, {6, 1}, {10, 5}, {5, 10}});
        geolib3::Polygon2 expectedRect2({{-2, 3}, {3, -2}, {6, 1}, {1, 6}});

        removeIntersections(rects);

        EXPECT_TRUE(compare(rects[0], expectedRect1));
        EXPECT_TRUE(compare(rects[1], expectedRect2));
    }

    Y_UNIT_TEST(test_not_change_rectangles_with_intersected_parallel_sides)
    {
        geolib3::Polygon2 rect1({{0, 2}, {6, 2}, {6, 4}, {0, 4}});
        geolib3::Polygon2 rect2({{2, 0}, {4, 0}, {4, 6}, {2, 6}});
        std::vector<geolib3::Polygon2> rects({rect1, rect2});

        geolib3::Polygon2 expectedRect1({{0, 2}, {6, 2}, {6, 4}, {0, 4}});
        geolib3::Polygon2 expectedRect2({{2, 0}, {4, 0}, {4, 6}, {2, 6}});

        removeIntersections(rects);

        EXPECT_TRUE(compare(rects[0], expectedRect1));
        EXPECT_TRUE(compare(rects[1], expectedRect2));
    }

    Y_UNIT_TEST(test_three_rectangles)
    {
        geolib3::Polygon2 rect1({{0, 0}, {4, 0}, {4, 4}, {0, 4}});
        geolib3::Polygon2 rect2({{2, 0}, {8, 0}, {8, 4}, {2, 4}});
        geolib3::Polygon2 rect3({{6, 0}, {10, 0}, {10, 4}, {6, 4}});

        std::vector<geolib3::Polygon2> rects = {rect1, rect2, rect3};

        geolib3::Polygon2 expectedRect1({{0, 0}, {3, 0}, {3, 4}, {0, 4}});
        geolib3::Polygon2 expectedRect2({{3, 0}, {7, 0}, {7, 4}, {3, 4}});
        geolib3::Polygon2 expectedRect3({{7, 0}, {10, 0}, {10, 4}, {7, 4}});

        removeIntersections(rects);

        EXPECT_TRUE(compare(rects[0], expectedRect1));
        EXPECT_TRUE(compare(rects[1], expectedRect2));
        EXPECT_TRUE(compare(rects[2], expectedRect3));
    }
} //Y_UNIT_TEST_SUITE(remove_overlaps_tests)

Y_UNIT_TEST_SUITE(rotated_bounding_box_tests)
{

    Y_UNIT_TEST(test_bounding_box_of_axis_aligned_rectangle_is_itself)
    {
        geolib3::Polygon2 polygon({{10, 0}, {13, 10}, {3, 13}, {0, 3}});
        geolib3::Polygon2 expectedBBox({{10, 0}, {13, 10}, {3, 13}, {0, 3}});

        geolib3::Polygon2 boundingBox = autocart::getBoundingRectangle(polygon);

        EXPECT_TRUE(compare(boundingBox, expectedBBox));
    }

    Y_UNIT_TEST(test_bounding_box_of_not_axis_aligned_rectangle_is_itself)
    {
        geolib3::Polygon2 polygon({{5, 0}, {10, 5}, {5, 10}, {0, 5}});
        geolib3::Polygon2 expectedBBox({{5, 0}, {10, 5}, {5, 10}, {0, 5}});

        geolib3::Polygon2 boundingBox = autocart::getBoundingRectangle(polygon);

        EXPECT_TRUE(compare(boundingBox, expectedBBox));
    }

    Y_UNIT_TEST(test_bounding_box_of_polygon_with_axis_aligned_sides)
    {
        geolib3::Polygon2 polygon({{0, 0}, {10, 0}, {10, 5}, {5, 10}, {0, 5}});
        geolib3::Polygon2 expectedBBox({{0, 0}, {10, 0}, {10, 10}, {0, 10}});

        geolib3::Polygon2 boundingBox = autocart::getBoundingRectangle(polygon);

        EXPECT_TRUE(compare(boundingBox, expectedBBox));
    }

    Y_UNIT_TEST(test_bounding_box_of_polygon_wo_axis_aligned_sides)
    {
        geolib3::Polygon2 polygon({{2, 0}, {4, 1}, {2, 3}, {0, 2}});
        geolib3::Polygon2 expectedBBox({{0, 2}, {2.5, -0.5}, {4, 1}, {1.5, 3.5}});

        geolib3::Polygon2 boundingBox = autocart::getBoundingRectangle(polygon);

        EXPECT_TRUE(compare(boundingBox, expectedBBox));
    }

    Y_UNIT_TEST(test_failed_polygon)
    {
        //MAPSMRC-615
        geolib3::Polygon2 polygon =
            geolib3::WKT::read<geolib3::Polygon2>(
                R"(POLYGON ((3235304.7117411107756197 4865526.1814503101631999,
3235304.1145768272690475 4865526.1814503101631999, 3235301.1287554102018476
4865526.7786145936697721, 3235285.6024840394966304 4865531.5559288617223501,
3235273.0620340867899358 4865537.5275716958567500, 3235271.8677055197767913
4865538.1247359793633223, 3235271.8677055197767913 4865539.9162288298830390,
3235273.6591983702965081 4865539.9162288298830390, 3235276.6450197873637080
4865539.3190645463764668, 3235283.2138269059360027 4865537.5275716958567500,
3235295.1571125751361251 4865533.9445859957486391, 3235304.1145768272690475
4865526.7786145936697721, 3235304.7117411107756197 4865526.1814503101631999)))"
        );

        autocart::getBoundingRectangle(polygon);
    }

    Y_UNIT_TEST(image_to_mercator_regression_test)
    {
        const geolib3::BoundingBox bbox(
            geolib3::Point2(2562.43, 56463.2),
            geolib3::Point2(82552.2, 1340.77)
        );
        const size_t zoom = 18;
        geolib3::Point2 pt(10, 5);

        geolib3::Point2 origin = getDisplayOrigin(bbox, zoom);


        double coeff  = geolib3::WGS84_EQUATOR_LENGTH / ((1 << zoom) * tile::TILE_SIZE);
        double offset = ((1 << zoom) * tile::TILE_SIZE) / 2;
        geolib3::Point2 expectedPoint(
            (pt.x() + origin.x() - offset) * coeff,
            -(pt.y() + origin.y() - offset) * coeff
        );

        EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                        expectedPoint,
                        imageToMercator(pt, origin, zoom),
                        geolib3::EPS));
    }

} //Y_UNIT_TEST_SUITE(rotated_bounding_box_tests)

} //namespace tests
} //namespace maps::wiki::autocart
