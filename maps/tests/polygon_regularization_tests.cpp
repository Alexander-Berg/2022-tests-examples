#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/wikimap/mapspro/services/autocart/libs/post_processing/include/polygon_regularization.h>

#include <random>

namespace maps {
namespace wiki {
namespace autocart {
namespace tests {

Y_UNIT_TEST_SUITE(grid_polyline_is_simple_tests)
{

    Y_UNIT_TEST(straight_line_has_self_intersections)
    {
        std::vector<GridPoint> gridPolygon({{0, 0}, {1, 0}, {2, 0}, {3, 0}});
        EXPECT_TRUE(!isSimple(gridPolygon));
    }

    Y_UNIT_TEST(polygon_with_equal_endpoints_has_not_self_intersections)
    {
        std::vector<GridPoint> gridPolyline({{0, 0}, {1, 0}, {1, 1}, {0, 0}});
        EXPECT_TRUE(isSimple(gridPolyline));
    }

    Y_UNIT_TEST(can_detect_line_contact)
    {
        std::vector<GridPoint> gridPolyline({{0, -1}, {0, 0}, {1, 0},
                                             {1, 1},
                                             {1, 0}, {2, 0}, {2, -1}});
        EXPECT_TRUE(!isSimple(gridPolyline));
    }

    Y_UNIT_TEST(can_detect_self_intersections)
    {
        std::vector<GridPoint> gridPolyline({{0, 0}, {1, 0}, {1, 1}, {0, -1}});
        EXPECT_TRUE(!isSimple(gridPolyline));
    }

} //Y_UNIT_TEST_SUITE(grid_polyline_is_simple_tests)

Y_UNIT_TEST_SUITE(grid_points_set_tests)
{

    Y_UNIT_TEST(no_points_with_big_grid_side_and_small_tolerance)
    {
        geolib3::Point2 point(15, 15);
        double gridSide = 10.;
        double tolerance = 2.;

        GridPointsSet pointsSet(point, tolerance, gridSide);

        EXPECT_EQ(pointsSet.pointsNumber(), 0);
    }

    Y_UNIT_TEST(set_with_equals_grid_side_and_tolerance_has_nine_points)
    {
        geolib3::Point2 point(10., 10.);
        double gridSide = 5.;
        double tolerance = 5.;

        GridPointsSet pointsSet(point, tolerance, gridSide);

        EXPECT_EQ(pointsSet.pointsNumber(), 9);
    }

    Y_UNIT_TEST(check_grid_points_position_for_close_grid_side_and_tolerance)
    {
        geolib3::Point2 point(10.1, 9.9);
        double gridSide = 5.;
        double tolerance = 6.;
        std::vector<GridPoint> expectedGridPoints({
                                                    {1, 1}, {2, 1}, {3, 1},
                                                    {1, 2}, {2, 2}, {3, 2},
                                                    {1, 3}, {2, 3}, {3, 3}
                                                  });

        GridPointsSet pointsSet(point, tolerance, gridSide);

        EXPECT_EQ(pointsSet.pointsNumber(), 9);
        for (size_t i = 0; i < 9; i++) {
            GridPoint gridPoint = pointsSet.pointAt(i);
            EXPECT_EQ(gridPoint.col, expectedGridPoints[i].col);
            EXPECT_EQ(gridPoint.row, expectedGridPoints[i].row);
        }
    }

    Y_UNIT_TEST(check_grid_points_position_for_far_grid_side_and_tolerance)
    {
        geolib3::Point2 point(100.1, 100.9);
        double gridSide = 100.;
        double tolerance = 6.;
        std::vector<GridPoint> expectedGridPoints({
                                                    {1, 1}
                                                  });

        GridPointsSet pointsSet(point, tolerance, gridSide);

        EXPECT_EQ(pointsSet.pointsNumber(), 1);
        GridPoint gridPoint = pointsSet.pointAt(0);
        EXPECT_EQ(gridPoint.col, expectedGridPoints[0].col);
        EXPECT_EQ(gridPoint.row, expectedGridPoints[0].row);
    }

} //Y_UNIT_TEST_SUITE(grid_points_set_tests)

Y_UNIT_TEST_SUITE(rotate_points_tests)
{

    Y_UNIT_TEST(rotation_by_zero_degree_does_not_change_points)
    {
        geolib3::PointsVector srcPoints({{1, 0}, {0, 1}, {-1, 0}, {0, -1}});
        double angle = 0.;
        geolib3::PointsVector expectedPoints({{1, 0}, {0, 1}, {-1, 0}, {0, -1}});

        geolib3::PointsVector rotatedPoints = rotatePoints(srcPoints, angle);

        EXPECT_EQ(rotatedPoints.size(), expectedPoints.size());
        for (size_t i = 0; i < rotatedPoints.size(); i++) {
            EXPECT_DOUBLE_EQ(rotatedPoints[i].x(), expectedPoints[i].x());
            EXPECT_DOUBLE_EQ(rotatedPoints[i].y(), expectedPoints[i].y());
        }
    }

    Y_UNIT_TEST(rotation_by_360_degrees_does_not_change_points)
    {
        geolib3::PointsVector srcPoints({{1, 0}, {0, 1}, {-1, 0}, {0, -1}});
        double angle = 360.;
        geolib3::PointsVector expectedPoints({{1, 0}, {0, 1}, {-1, 0}, {0, -1}});

        geolib3::PointsVector rotatedPoints = rotatePoints(srcPoints, angle);

        EXPECT_EQ(rotatedPoints.size(), expectedPoints.size());
        for (size_t i = 0; i < rotatedPoints.size(); i++) {
            EXPECT_DOUBLE_EQ(rotatedPoints[i].x(), expectedPoints[i].x());
            EXPECT_DOUBLE_EQ(rotatedPoints[i].y(), expectedPoints[i].y());
        }
    }

    Y_UNIT_TEST(rotation_by_random_angle)
    {
        size_t NUMBER_OF_TEST = 10;
        std::random_device rd;
        std::default_random_engine gen(rd());
        std::uniform_real_distribution<double> angleDistribution(0., 360.);

        geolib3::PointsVector srcPoints({{1, 0}, {0, 1}});
        for (size_t i = 0; i < NUMBER_OF_TEST; i++) {
            double angle = angleDistribution(gen);
            double sina = sin(M_PI * angle / 180.);
            double cosa = cos(M_PI * angle / 180.);
            geolib3::PointsVector expectedPoints({{cosa, sina}, {-sina, cosa}});

            geolib3::PointsVector rotatedPoints = rotatePoints(srcPoints, angle);

            EXPECT_EQ(rotatedPoints.size(), expectedPoints.size());
            for (size_t i = 0; i < rotatedPoints.size(); i++) {
                EXPECT_DOUBLE_EQ(rotatedPoints[i].x(), expectedPoints[i].x());
                EXPECT_DOUBLE_EQ(rotatedPoints[i].y(), expectedPoints[i].y());
            }
        }
    }

} //Y_UNIT_TEST_SUITE(rotate_points_tests)

Y_UNIT_TEST_SUITE(simplify_border_tests)
{

    Y_UNIT_TEST(does_not_simplify_rectangle_with_big_sides)
    {
        geolib3::PointsVector srcBorder({
                                         {0., -10.}, {10., 0.},
                                         {0., 10.}, {-10., 0.}
                                       });
        double epsilon = 1.;
        geolib3::PointsVector expectedBorder({
                                              {0., -10.}, {10., 0.},
                                              {0., 10.}, {-10., 0.}
                                            });

        geolib3::PointsVector simplifiedBorder = simplifyBoundary(srcBorder, epsilon);

        EXPECT_EQ(simplifiedBorder.size(), expectedBorder.size());
        for (size_t i = 0; i < simplifiedBorder.size(); i++) {
            EXPECT_DOUBLE_EQ(simplifiedBorder[i].x(), expectedBorder[i].x());
            EXPECT_DOUBLE_EQ(simplifiedBorder[i].y(), expectedBorder[i].y());
        }
    }

    Y_UNIT_TEST(delete_points_with_small_deviation)
    {
        geolib3::PointsVector srcBorder({
                                         {0., -10.}, {5.05, -5.5}, {10., 0.},
                                         {0., 10.}, {-4.4, 4.2}, {-10., 0.}
                                       });
        double epsilon = 3.;
        geolib3::PointsVector expectedBorder({
                                              {0., -10.}, {10., 0.},
                                              {0., 10.}, {-10., 0.}
                                            });

        geolib3::PointsVector simplifiedBorder = simplifyBoundary(srcBorder, epsilon);
        EXPECT_EQ(simplifiedBorder.size(), expectedBorder.size());
        for (size_t i = 0; i < simplifiedBorder.size(); i++) {
            EXPECT_DOUBLE_EQ(simplifiedBorder[i].x(), expectedBorder[i].x());
            EXPECT_DOUBLE_EQ(simplifiedBorder[i].y(), expectedBorder[i].y());
        }
    }

    Y_UNIT_TEST(does_not_delete_points_with_big_deviation)
    {
        geolib3::PointsVector srcBorder({
                                         {0., -10.}, {10., -10.}, {10., 0.},
                                         {0., 10.}, {-4.4, 4.2}, {-10., 0.}
                                       });
        double epsilon = 3.;
        geolib3::PointsVector expectedBorder({
                                              {0., -10.}, {10., -10.}, {10., 0.},
                                              {0., 10.}, {-10., 0.}
                                            });

        geolib3::PointsVector simplifiedBorder = simplifyBoundary(srcBorder, epsilon);
        EXPECT_EQ(simplifiedBorder.size(), expectedBorder.size());
        for (size_t i = 0; i < simplifiedBorder.size(); i++) {
            EXPECT_DOUBLE_EQ(simplifiedBorder[i].x(), expectedBorder[i].x());
            EXPECT_DOUBLE_EQ(simplifiedBorder[i].y(), expectedBorder[i].y());
        }
    }

} //Y_UNIT_TEST_SUITE(simplify_border_tests)

Y_UNIT_TEST_SUITE(remove_extra_polygon_grid_points_tests)
{

    Y_UNIT_TEST(does_not_remove_points_on_different_segments)
    {
        std::vector<GridPoint> srcPoints({
                                           {0, 0}, {3, 0}, {3, 2}, {2, 2},
                                           {2, 1}, {1, 1}, {1, 2}, {0, 2}
                                         });
        std::vector<GridPoint> expectedPoints({
                                                {0, 0}, {3, 0}, {3, 2}, {2, 2},
                                                {2, 1}, {1, 1}, {1, 2}, {0, 2}
                                              });

        std::vector<GridPoint> clearedPoints = removeExtraPolygonPoints(srcPoints);

        EXPECT_EQ(clearedPoints.size(), expectedPoints.size());
        for (size_t i = 0; i < clearedPoints.size(); i++) {
            EXPECT_EQ(clearedPoints[i].col, expectedPoints[i].col);
            EXPECT_EQ(clearedPoints[i].row, expectedPoints[i].row);
        }
    }

    Y_UNIT_TEST(remove_three_points_on_vertical_segment)
    {
        std::vector<GridPoint> srcPoints({
                                           {0, 0}, {1, 0}, {1, 1}, {1, 2}, {0, 2}
                                         });
        std::vector<GridPoint> expectedPoints({
                                                {0, 0}, {1, 0}, {1, 2}, {0, 2}
                                              });

        std::vector<GridPoint> clearedPoints = removeExtraPolygonPoints(srcPoints);
        EXPECT_EQ(clearedPoints.size(), expectedPoints.size());
        for (size_t i = 0; i < clearedPoints.size(); i++) {
            EXPECT_EQ(clearedPoints[i].col, expectedPoints[i].col);
            EXPECT_EQ(clearedPoints[i].row, expectedPoints[i].row);
        }
    }

    Y_UNIT_TEST(remove_five_points_on_horizontal_segment)
    {
        std::vector<GridPoint> srcPoints({
                                           {0, 0},
                                           {1, 0}, {2, 0}, {3, 0}, // extra points
                                           {4, 0}, {4, 1}, {0, 1}
                                         });
        std::vector<GridPoint> expectedPoints({
                                                {0, 0}, {4, 0}, {4, 1}, {0, 1}
                                              });

        std::vector<GridPoint> clearedPoints = removeExtraPolygonPoints(srcPoints);
        EXPECT_EQ(clearedPoints.size(), expectedPoints.size());
        for (size_t i = 0; i < clearedPoints.size(); i++) {
            EXPECT_EQ(clearedPoints[i].col, expectedPoints[i].col);
            EXPECT_EQ(clearedPoints[i].row, expectedPoints[i].row);
        }
    }

} //Y_UNIT_TEST_SUITE(remove_extra_polygon_grid_points_tests)

Y_UNIT_TEST_SUITE(convert_grid_points_to_polygon_tests)
{

    Y_UNIT_TEST(convert_simple_polygon)
    {
        std::vector<GridPoint> srcPoints({
                                           {0, 0}, {3, 0}, {3, 3}, {0, 3}
                                         });
        double gridSide = 3.;
        geolib3::PointsVector expectedPoints({
                                              {0., 0.}, {9., 0.}, {9., 9.}, {0., 9.}
                                            });

        geolib3::PointsVector convertedPoints = convertToPolygon(srcPoints, gridSide);

        EXPECT_EQ(convertedPoints.size(), expectedPoints.size());
        for (size_t i = 0; i < convertedPoints.size(); i++) {
            EXPECT_DOUBLE_EQ(convertedPoints[i].x(), expectedPoints[i].x());
            EXPECT_DOUBLE_EQ(convertedPoints[i].y(), convertedPoints[i].y());
        }
    }

    Y_UNIT_TEST(convert_polygon_with_extra_points)
    {
        std::vector<GridPoint> srcPoints({
                                           {0, 0},
                                           {1, 0}, {2, 0}, // extra points
                                           {3, 0}, {3, 3}, {0, 3}
                                         });
        double gridSide = 3.;
        geolib3::PointsVector expectedPoints({
                                              {0., 0.}, {9., 0.}, {9., 9.}, {0., 9.}
                                            });

        geolib3::PointsVector convertedPoints = convertToPolygon(srcPoints, gridSide);

        EXPECT_EQ(convertedPoints.size(), expectedPoints.size());
        for (size_t i = 0; i < convertedPoints.size(); i++) {
            EXPECT_DOUBLE_EQ(convertedPoints[i].x(), expectedPoints[i].x());
            EXPECT_DOUBLE_EQ(convertedPoints[i].y(), convertedPoints[i].y());
        }
    }

} //Y_UNIT_TEST_SUITE(convert_grid_points_to_polygon_tests)

Y_UNIT_TEST_SUITE(direction_tests)
{

    Y_UNIT_TEST(can_detect_opposite_directions)
    {
        // Opposite directions
        EXPECT_TRUE(isOppositeDirections(Direction::RIGHT, Direction::LEFT));
        EXPECT_TRUE(isOppositeDirections(Direction::LEFT, Direction::RIGHT));
        EXPECT_TRUE(isOppositeDirections(Direction::UP, Direction::DOWN));
        EXPECT_TRUE(isOppositeDirections(Direction::DOWN, Direction::UP));
        // Not opposite directions
        EXPECT_TRUE(!isOppositeDirections(Direction::RIGHT, Direction::UP));
        EXPECT_TRUE(!isOppositeDirections(Direction::UP, Direction::RIGHT));
        EXPECT_TRUE(!isOppositeDirections(Direction::RIGHT, Direction::DOWN));
        EXPECT_TRUE(!isOppositeDirections(Direction::DOWN, Direction::RIGHT));
        EXPECT_TRUE(!isOppositeDirections(Direction::LEFT, Direction::UP));
        EXPECT_TRUE(!isOppositeDirections(Direction::UP, Direction::LEFT));
        EXPECT_TRUE(!isOppositeDirections(Direction::LEFT, Direction::DOWN));
        EXPECT_TRUE(!isOppositeDirections(Direction::DOWN, Direction::LEFT));
    }

    Y_UNIT_TEST(can_detect_correct_direction)
    {
        GridPoint point1(0, 0);
        GridPoint point2(1, 0);
        GridPoint point3(1, 1);

        // Correct direction
        EXPECT_TRUE(isCorrectDirection(point1, point2, Direction::RIGHT));
        EXPECT_TRUE(isCorrectDirection(point2, point1, Direction::LEFT));
        EXPECT_TRUE(isCorrectDirection(point2, point3, Direction::UP));
        EXPECT_TRUE(isCorrectDirection(point3, point2, Direction::DOWN));

        // Incorrect direction
        EXPECT_TRUE(!isCorrectDirection(point1, point2, Direction::LEFT));
        EXPECT_TRUE(!isCorrectDirection(point2, point1, Direction::RIGHT));
        EXPECT_TRUE(!isCorrectDirection(point1, point3, Direction::UP));
        EXPECT_TRUE(!isCorrectDirection(point2, point3, Direction::LEFT));
        EXPECT_TRUE(!isCorrectDirection(point3, point1, Direction::LEFT));
        EXPECT_TRUE(!isCorrectDirection(point2, point3, Direction::DOWN));
    }

} //Y_UNIT_TEST_SUITE(direction_tests)

Y_UNIT_TEST_SUITE(projection_tests)
{

    Y_UNIT_TEST(can_project_points_that_lies_on_segment)
    {
        geolib3::Point2 point(0.5, 0.5);
        geolib3::Segment2 segment(geolib3::Point2(0., 0.),
                                  geolib3::Point2(1., 1.));
        geolib3::Point2 expectedProjection(0.5, 0.5);
        double expectedAlpha = 0.5;

        double alpha;
        geolib3::Point2 projection = project(segment, point, &alpha);

        EXPECT_DOUBLE_EQ(alpha, expectedAlpha);
        EXPECT_DOUBLE_EQ(projection.x(), expectedProjection.x());
        EXPECT_DOUBLE_EQ(projection.y(), expectedProjection.y());
    }

    Y_UNIT_TEST(can_project_points_that_lies_on_line)
    {
        geolib3::Point2 point(1.5, 1.5);
        geolib3::Segment2 segment(geolib3::Point2(0., 0.),
                                  geolib3::Point2(1., 1.));
        geolib3::Point2 expectedProjection(1.5, 1.5);
        double expectedAlpha = 1.5;

        double alpha;
        geolib3::Point2 projection = project(segment, point, &alpha);

        EXPECT_DOUBLE_EQ(alpha, expectedAlpha);
        EXPECT_DOUBLE_EQ(projection.x(), expectedProjection.x());
        EXPECT_DOUBLE_EQ(projection.y(), expectedProjection.y());
    }

    Y_UNIT_TEST(can_project_points_that_not_lies_on_line)
    {
        geolib3::Point2 point(0.5, 2.5);
        geolib3::Segment2 segment(geolib3::Point2(0., 0.),
                                  geolib3::Point2(1., 1.));
        geolib3::Point2 expectedProjection(1.5, 1.5);
        double expectedAlpha = 1.5;

        double alpha;
        geolib3::Point2 projection = project(segment, point, &alpha);

        EXPECT_DOUBLE_EQ(alpha, expectedAlpha);
        EXPECT_DOUBLE_EQ(projection.x(), expectedProjection.x());
        EXPECT_DOUBLE_EQ(projection.y(), expectedProjection.y());
    }

    Y_UNIT_TEST(can_project_points_with_negative_coeff)
    {
        geolib3::Point2 point(-2.5, -0.5);
        geolib3::Segment2 segment(geolib3::Point2(0., 0.),
                                  geolib3::Point2(1., 1.));
        geolib3::Point2 expectedProjection(-1.5, -1.5);
        double expectedAlpha = -1.5;

        double alpha;
        geolib3::Point2 projection = project(segment, point, &alpha);

        EXPECT_DOUBLE_EQ(alpha, expectedAlpha);
        EXPECT_DOUBLE_EQ(projection.x(), expectedProjection.x());
        EXPECT_DOUBLE_EQ(projection.y(), expectedProjection.y());
    }

} //Y_UNIT_TEST_SUITE(projection_tests)

Y_UNIT_TEST_SUITE(close_boundary_tests)
{

    Y_UNIT_TEST(start_point_projection_lies_on_start_edge)
    {
        geolib3::PointsVector boundary({
                                        {0., 2.}, {0., 0.}, {2., 0.},
                                        {2., 1.}, {1., 1.}
                                      });
        geolib3::PointsVector expectedClosedBoundary({
                                                      {0., 0.}, {2., 0.},
                                                      {2., 1.}, {0., 1.}
                                                    });

        geolib3::PointsVector closedBoundary = closeBoundary(boundary);

        EXPECT_EQ(closedBoundary.size(), expectedClosedBoundary.size());
        for (size_t i = 0; i < closedBoundary.size(); i++) {
            EXPECT_DOUBLE_EQ(closedBoundary[i].x(), expectedClosedBoundary[i].x());
            EXPECT_DOUBLE_EQ(closedBoundary[i].y(), expectedClosedBoundary[i].y());
        }
    }

    Y_UNIT_TEST(start_point_lies_on_line_that_passes_through_end_edge)
    {
        geolib3::PointsVector boundary({
                                        {0., 2.}, {0., 0.}, {2., 0.},
                                        {2., 2.}, {1., 2.}
                                      });
        geolib3::PointsVector expectedClosedBoundary({
                                                      {0., 0.}, {2., 0.},
                                                      {2., 2.}, {0., 2.}
                                                    });

        geolib3::PointsVector closedBoundary = closeBoundary(boundary);

        EXPECT_EQ(closedBoundary.size(), expectedClosedBoundary.size());
        for (size_t i = 0; i < closedBoundary.size(); i++) {
            EXPECT_DOUBLE_EQ(closedBoundary[i].x(), expectedClosedBoundary[i].x());
            EXPECT_DOUBLE_EQ(closedBoundary[i].y(), expectedClosedBoundary[i].y());
        }
    }

    Y_UNIT_TEST(start_and_end_point_on_parallel_lines)
    {
        geolib3::PointsVector boundary({
                                        {0., 2.}, {0., 0.}, {2., 0.}, {2., 1.}
                                      });
        geolib3::PointsVector expectedClosedBoundary({
                                                      {0., 2.}, {0., 0.},
                                                      {2., 0.}, {2., 2.}
                                                    });

        geolib3::PointsVector closedBoundary = closeBoundary(boundary);
        EXPECT_EQ(closedBoundary.size(), expectedClosedBoundary.size());
        for (size_t i = 0; i < closedBoundary.size(); i++) {
            EXPECT_DOUBLE_EQ(closedBoundary[i].x(), expectedClosedBoundary[i].x());
            EXPECT_DOUBLE_EQ(closedBoundary[i].y(), expectedClosedBoundary[i].y());
        }
    }

    Y_UNIT_TEST(add_new_edge)
    {
        geolib3::PointsVector boundary({
                                        {0., 1.}, {0., 0.}, {2., 0.},
                                        {2., 2.}, {1., 2.}
                                      });
        geolib3::PointsVector expectedClosedBoundary({
                                                      {0., 0.}, {2., 0.},
                                                      {2., 2.}, {0., 2.}
                                                    });

        geolib3::PointsVector closedBoundary = closeBoundary(boundary);

        EXPECT_EQ(closedBoundary.size(), expectedClosedBoundary.size());
        for (size_t i = 0; i < closedBoundary.size(); i++) {
            EXPECT_DOUBLE_EQ(closedBoundary[i].x(), expectedClosedBoundary[i].x());
            EXPECT_DOUBLE_EQ(closedBoundary[i].y(), expectedClosedBoundary[i].y());
        }
    }

} //Y_UNIT_TEST_SUITE(close_boundary_tests)

} //namespace tests
} //namespace autocart
} //namespace wiki
} //namespace maps
