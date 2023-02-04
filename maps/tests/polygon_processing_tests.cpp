#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/libs/geolib/include/linear_ring.h>
#include <maps/libs/geolib/include/spatial_relation.h>

#include <maps/wikimap/mapspro/services/autocart/libs/geometry/include/polygon_processing.h>

namespace maps {
namespace wiki {
namespace autocart {
namespace tests {

Y_UNIT_TEST_SUITE(polygon_intersection_tests)
{

    Y_UNIT_TEST(test_not_intersected_polygons)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 polygon2({{2, 0}, {3, 0}, {3, 1}, {2, 1}});

        geolib3::MultiPolygon2 result = intersectPolygons(polygon1, polygon2);

        EXPECT_EQ(result.polygonsNumber(), 0u);
    }

    Y_UNIT_TEST(test_point_contacted_rectangles)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 polygon2({{1, 1}, {2, 1}, {2, 2}, {1, 2}});

        geolib3::MultiPolygon2 result = intersectPolygons(polygon1, polygon2);

        EXPECT_EQ(result.polygonsNumber(), 0u);
    }

    Y_UNIT_TEST(test_line_contacted_rectangles)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 polygon2({{1, 0}, {2, 0}, {2, 1}, {1, 1}});

        geolib3::MultiPolygon2 result = intersectPolygons(polygon1, polygon2);

        EXPECT_EQ(result.polygonsNumber(), 0u);
    }

    Y_UNIT_TEST(test_intersected_rectangles)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {2, 0}, {2, 1}, {0, 1}});
        geolib3::Polygon2 polygon2({{1, 0}, {3, 0}, {3, 1}, {1, 1}});

        geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{1, 0}, {2, 0}, {2, 1}, {1, 1}})
        });

        geolib3::MultiPolygon2 result = intersectPolygons(polygon1, polygon2);

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_intersected_rhombuses)
    {
        geolib3::Polygon2 polygon1({{1, 0}, {2, 1}, {1, 2}, {0, 1}});
        geolib3::Polygon2 polygon2({{2, 0}, {3, 1}, {2, 2}, {1, 1}});

        geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{1.5, 0.5}, {2, 1}, {1.5, 1.5}, {1, 1}})
        });

        geolib3::MultiPolygon2 result = intersectPolygons(polygon1, polygon2);

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_complex_polygons_with_three_point_contacts)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {2, 0}, {2, 1}, {1, 1}, {1, 2}, {2, 2}, {2, 3}, {0, 3}});
        geolib3::Polygon2 polygon2({{2, 1}, {4, 1}, {4, 4}, {2, 4}, {2, 3}, {3, 3}, {3, 2}, {2, 2}});

        geolib3::MultiPolygon2 result = intersectPolygons(polygon1, polygon2);

        EXPECT_EQ(result.polygonsNumber(), 0u);
    }

    Y_UNIT_TEST(test_complex_polygons_with_two_line_contacts)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {2, 0}, {2, 1}, {1, 1}, {1, 2}, {2, 2}, {2, 3}, {0, 3}});
        geolib3::Polygon2 polygon2({{2, 0}, {4, 0}, {4, 3}, {2, 3}, {2, 2}, {3, 2}, {3, 1}, {2, 1}});

        geolib3::MultiPolygon2 result = intersectPolygons(polygon1, polygon2);

        EXPECT_EQ(result.polygonsNumber(), 0u);
    }

    Y_UNIT_TEST(test_complex_polygons_with_two_intersected_polygons)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {3, 0}, {3, 1}, {1, 1}, {1, 2}, {3, 2}, {3, 3}, {0, 3}});
        geolib3::Polygon2 polygon2({{2, 0}, {5, 0}, {5, 3}, {2, 3}, {2, 2}, {4, 2}, {4, 1}, {2, 1}});

        geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{2, 0}, {3, 0}, {3, 1}, {2, 1}}),
            geolib3::Polygon2({{2, 2}, {3, 2}, {3, 3}, {2, 3}})
        });

        geolib3::MultiPolygon2 result = intersectPolygons(polygon1, polygon2);

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_complex_polygons_with_two_intersected_polygons_and_line_contact)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {3, 0}, {3, 1}, {1, 1}, {1, 2}, {3, 2}, {3, 3}, {0, 3}});
        geolib3::Polygon2 polygon2({{1, 0}, {4, 0}, {4, 3}, {1, 3}});

        geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{1, 0}, {3, 0}, {3, 1}, {1, 1}}),
            geolib3::Polygon2({{1, 2}, {3, 2}, {3, 3}, {1, 3}})
        });

        geolib3::MultiPolygon2 result = intersectPolygons(polygon1, polygon2);

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_complex_polygons_with_three_intersected_polygons_and_two_line_contact)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {3, 0}, {3, 1}, {1, 1}, {1, 2}, {3, 2},
                                   {3, 3}, {1, 3}, {1, 4}, {3, 4}, {3, 5}, {0, 5}});
        geolib3::Polygon2 polygon2({{1, 0}, {4, 0}, {4, 5}, {1, 5}});

        geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{1, 0}, {3, 0}, {3, 1}, {1, 1}}),
            geolib3::Polygon2({{1, 2}, {3, 2}, {3, 3}, {1, 3}}),
            geolib3::Polygon2({{1, 4}, {3, 4}, {3, 5}, {1, 5}})
        });

        geolib3::MultiPolygon2 result = intersectPolygons(polygon1, polygon2);

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_nested_polygon)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {3, 0}, {3, 3}, {0, 3}});
        geolib3::Polygon2 polygon2({{1, 1}, {2, 1}, {2, 2}, {1, 2}});

        geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{1, 1}, {2, 1}, {2, 2}, {1, 2}})
        });

        geolib3::MultiPolygon2 result = intersectPolygons(polygon1, polygon2);

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

} //Y_UNIT_TEST_SUITE(polygon_intersection_tests)

Y_UNIT_TEST_SUITE(intersection_area_tests)
{

    Y_UNIT_TEST(test_intersection_area_not_intersected_polygons_is_zero)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 polygon2({{2, 0}, {3, 0}, {3, 1}, {2, 1}});
        const double expectedArea = 0.;

        double area = intersectionArea(polygon1, polygon2);

        EXPECT_DOUBLE_EQ(expectedArea, area);
    }

    Y_UNIT_TEST(test_intersection_area_point_contacted_rectangles_is_zero)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 polygon2({{1, 1}, {2, 1}, {2, 2}, {1, 2}});
        const double expectedArea = 0.;

        double area = intersectionArea(polygon1, polygon2);

        EXPECT_DOUBLE_EQ(expectedArea, area);
    }

    Y_UNIT_TEST(test_intersection_area_line_contacted_rectangles_is_zero)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 polygon2({{1, 0}, {2, 0}, {2, 1}, {1, 1}});
        const double expectedArea = 0.;

        double area = intersectionArea(polygon1, polygon2);

        EXPECT_DOUBLE_EQ(expectedArea, area);
    }

    Y_UNIT_TEST(test_intersection_area_intersected_rectangles)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {3, 0}, {3, 3}, {0, 3}});
        geolib3::Polygon2 polygon2({{1, 1}, {4, 1}, {4, 4}, {1, 4}});
        const double expectedArea = 4.;

        double area = intersectionArea(polygon1, polygon2);

        EXPECT_DOUBLE_EQ(expectedArea, area);
    }

    Y_UNIT_TEST(test_intersection_area_with_two_polygons)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {3, 0}, {3, 1}, {1, 1}, {1, 2}, {3, 2}, {3, 3}, {0, 3}});
        geolib3::Polygon2 polygon2({{2, 0}, {5, 0}, {5, 3}, {2, 3}, {2, 2}, {4, 2}, {4, 1}, {2, 1}});
        const double expectedArea = 2.;

        double area = intersectionArea(polygon1, polygon2);

        EXPECT_DOUBLE_EQ(expectedArea, area);
    }

    Y_UNIT_TEST(test_intersection_area_with_three_polygons)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {3, 0}, {3, 1}, {1, 1}, {1, 2}, {3, 2},
                                   {3, 3}, {1, 3}, {1, 4}, {3, 4}, {3, 5}, {0, 5}});
        geolib3::Polygon2 polygon2({{1, 0}, {4, 0}, {4, 5}, {1, 5}});
        const double expectedArea = 6.;

        double area = intersectionArea(polygon1, polygon2);

        EXPECT_DOUBLE_EQ(expectedArea, area);
    }

    Y_UNIT_TEST(test_intersection_area_nested_polygon)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {3, 0}, {3, 3}, {0, 3}});
        geolib3::Polygon2 polygon2({{1, 1}, {2, 1}, {2, 2}, {1, 2}});
        const double expectedArea = 1.;

        double area = intersectionArea(polygon1, polygon2);

        EXPECT_DOUBLE_EQ(expectedArea, area);
    }

} //Y_UNIT_TEST_SUITE(intersection_area_tests)

Y_UNIT_TEST_SUITE(intersection_over_union_tests)
{

    Y_UNIT_TEST(test_iou_not_intersected_polygons_is_zero)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 polygon2({{2, 0}, {3, 0}, {3, 1}, {2, 1}});
        const double expectedIoU = 0.;

        double resultIoU = IoU(polygon1, polygon2);

        EXPECT_DOUBLE_EQ(expectedIoU, resultIoU);
    }

    Y_UNIT_TEST(test_iou_identical_polygons_is_one)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        geolib3::Polygon2 polygon2({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        const double expectedIoU = 1.;

        double resultIoU = IoU(polygon1, polygon2);

        EXPECT_DOUBLE_EQ(expectedIoU, resultIoU);
    }

    Y_UNIT_TEST(test_iou_overlapping_polygons)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {2, 0}, {2, 1}, {0, 1}});
        geolib3::Polygon2 polygon2({{1, 0}, {4, 0}, {4, 1}, {1, 1}});
        const double expectedIoU = 0.25;

        double resultIoU = IoU(polygon1, polygon2);

        EXPECT_DOUBLE_EQ(expectedIoU, resultIoU);
    }

    Y_UNIT_TEST(test_iou_nested_polygon)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {4, 0}, {4, 4}, {0, 4}});
        geolib3::Polygon2 polygon2({{1, 1}, {3, 1}, {3, 3}, {1, 3}});
        const double expectedIoU = 0.25;

        double resultIoU = IoU(polygon1, polygon2);

        EXPECT_DOUBLE_EQ(expectedIoU, resultIoU);
    }

    Y_UNIT_TEST(test_iou_nested_polygon_with_line_contact)
    {
        geolib3::Polygon2 polygon1({{0, 0}, {4, 0}, {4, 4}, {0, 4}});
        geolib3::Polygon2 polygon2({{2, 1}, {4, 1}, {4, 3}, {2, 3}});
        const double expectedIoU = 0.25;

        double resultIoU = IoU(polygon1, polygon2);

        EXPECT_DOUBLE_EQ(expectedIoU, resultIoU);
    }
} //Y_UNIT_TEST_SUITE(intersection_over_inion_tests)

Y_UNIT_TEST_SUITE(multipolygon_intersection_tests)
{

    Y_UNIT_TEST(test_not_intersected_polygons)
    {
        geolib3::MultiPolygon2 multipolygon1({
            geolib3::Polygon2({{0, 0}, {1, 0}, {1, 1}, {0, 1}})
        });
        geolib3::MultiPolygon2 multipolygon2({
            geolib3::Polygon2({{2, 0}, {3, 0}, {3, 1}, {2, 1}})
        });

        geolib3::MultiPolygon2 result = intersectMultiPolygons(multipolygon1, multipolygon2);

        EXPECT_EQ(result.polygonsNumber(), 0u);
    }

    Y_UNIT_TEST(test_intersected_rectangles)
    {
        geolib3::MultiPolygon2 multipolygon1({
            geolib3::Polygon2({{0, 0}, {2, 0}, {2, 1}, {0, 1}})
        });
        geolib3::MultiPolygon2 multipolygon2({
            geolib3::Polygon2({{1, 0}, {3, 0}, {3, 1}, {1, 1}})
        });

        geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{1, 0}, {2, 0}, {2, 1}, {1, 1}})
        });

        geolib3::MultiPolygon2 result = intersectMultiPolygons(multipolygon1, multipolygon2);

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_intersected_complex_polygons)
    {
        geolib3::MultiPolygon2 multipolygon1({
            geolib3::Polygon2({{0, 0}, {2, 0}, {2, 1}, {0, 1}}),
            geolib3::Polygon2({{3, 0}, {5, 0}, {5, 1}, {3, 1}})
        });
        geolib3::MultiPolygon2 multipolygon2({
            geolib3::Polygon2({{1, 0}, {4, 0}, {4, 1}, {1, 1}})
        });

        geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{1, 0}, {2, 0}, {2, 1}, {1, 1}}),
            geolib3::Polygon2({{3, 0}, {4, 0}, {4, 1}, {3, 1}})
        });

        geolib3::MultiPolygon2 result = intersectMultiPolygons(multipolygon1, multipolygon2);

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

} //Y_UNIT_TEST_SUITE(multipolygon_intersection_tests)

Y_UNIT_TEST_SUITE(merge_tests)
{
    Y_UNIT_TEST(test_one_polygon)
    {
        const geolib3::Polygon2 polygon({{0, 0}, {1, 0}, {1, 1}, {0, 1}});

        const geolib3::MultiPolygon2 expectedResult({polygon});

        geolib3::MultiPolygon2 result = mergePolygons({polygon});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_not_intersected_polygons)
    {
        const geolib3::Polygon2 polygon1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        const geolib3::Polygon2 polygon2({{2, 0}, {3, 0}, {3, 1}, {2, 1}});

        const geolib3::MultiPolygon2 expectedResult({polygon1, polygon2});

        geolib3::MultiPolygon2 result = mergePolygons({polygon1, polygon2});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_side_touch_polygons)
    {
        const geolib3::Polygon2 polygon1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        const geolib3::Polygon2 polygon2({{1, 0}, {2, 0}, {2, 1}, {1, 1}});

        const geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{0, 0}, {2, 0}, {2, 1}, {0, 1}})
        });

        geolib3::MultiPolygon2 result = mergePolygons({polygon1, polygon2});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_point_touch_polygons)
    {
        const geolib3::Polygon2 polygon1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        const geolib3::Polygon2 polygon2({{1, 1}, {2, 1}, {2, 2}, {1, 2}});

        const geolib3::MultiPolygon2 expectedResult({
            polygon1, polygon2
        });

        geolib3::MultiPolygon2 result = mergePolygons({polygon1, polygon2});

        EXPECT_EQ(result.polygonsNumber(), 2u);

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }


    Y_UNIT_TEST(test_intersected_polygons)
    {
        const geolib3::Polygon2 polygon1({{0, 0}, {2, 0}, {2, 1}, {0, 1}});
        const geolib3::Polygon2 polygon2({{1, 0}, {3, 0}, {3, 1}, {1, 1}});

        const geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{0, 0}, {3, 0}, {3, 1}, {0, 1}})
        });

        geolib3::MultiPolygon2 result = mergePolygons({polygon1, polygon2});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_intersected_complex_polygons)
    {
        const geolib3::Polygon2 polygon1({{0, 0}, {2, 0}, {2, 1}, {1, 1}, {1, 2}, {2, 2}, {2, 3}, {0, 3}});
        const geolib3::Polygon2 polygon2({{1, 0}, {3, 0}, {3, 3}, {1, 3}, {1, 2}, {2, 2}, {2, 1}, {1, 1}});

        const geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2(geolib3::LinearRing2({{0, 0}, {3, 0}, {3, 3}, {0 ,3}}),
                              {geolib3::LinearRing2({{1, 1}, {2, 1}, {2, 2}, {1, 2}})})
        });

        geolib3::MultiPolygon2 result = mergePolygons({polygon1, polygon2});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_one_multipolygon)
    {
        const geolib3::MultiPolygon2 multiPolygon({
            geolib3::Polygon2({{0, 0}, {1, 0}, {1, 1}, {0, 1}}),
            geolib3::Polygon2({{2, 0}, {3, 0}, {3, 1}, {2, 1}})
        });

        const geolib3::MultiPolygon2 expectedResult = multiPolygon;

        geolib3::MultiPolygon2 result = mergeMultiPolygons({multiPolygon});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_not_intersected_multipolygons)
    {
        const geolib3::MultiPolygon2 multiPolygon1({
            geolib3::Polygon2({{0, 0}, {1, 0}, {1, 1}, {0, 1}}),
            geolib3::Polygon2({{2, 0}, {3, 0}, {3, 1}, {2, 1}})
        });
        const geolib3::MultiPolygon2 multiPolygon2({
            geolib3::Polygon2({{4, 0}, {5, 0}, {5, 1}, {4, 1}})
        });

        const geolib3::MultiPolygon2 expectedResult({
            multiPolygon1.polygonAt(0),
            multiPolygon1.polygonAt(1),
            multiPolygon2.polygonAt(0)
        });

        geolib3::MultiPolygon2 result = mergeMultiPolygons({multiPolygon1, multiPolygon2});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_intersected_multipolygons)
    {
        const geolib3::MultiPolygon2 multiPolygon1({
            geolib3::Polygon2({{0, 0}, {2, 0}, {2, 1}, {0, 1}}),
            geolib3::Polygon2({{3, 0}, {5, 0}, {5, 1}, {3, 1}})
        });
        const geolib3::MultiPolygon2 multiPolygon2({
            geolib3::Polygon2({{1, 0}, {4, 0}, {4, 1}, {1, 1}})
        });

        const geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{0, 0}, {5, 0}, {5, 1}, {0, 1}})
        });

        geolib3::MultiPolygon2 result = mergeMultiPolygons({multiPolygon1, multiPolygon2});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_intersected_complex_multipolygons)
    {
        const geolib3::MultiPolygon2 multiPolygon1({
            geolib3::Polygon2({{0, 0}, {1, 0}, {1, 3}, {0, 3}}),
            geolib3::Polygon2({{2, 0}, {3, 0}, {3, 3}, {2, 3}})
        });
        const geolib3::MultiPolygon2 multiPolygon2({
            geolib3::Polygon2({{0, 0}, {3, 0}, {3, 1}, {0, 1}}),
            geolib3::Polygon2({{0, 2}, {3, 2}, {3, 3}, {0, 3}})
        });

        const geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2(geolib3::LinearRing2({{0, 0}, {3, 0}, {3, 3}, {0 ,3}}),
                              {geolib3::LinearRing2({{1, 1}, {2, 1}, {2, 2}, {1, 2}})})
        });

        geolib3::MultiPolygon2 result = mergeMultiPolygons({multiPolygon1, multiPolygon2});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

} //Y_UNIT_TEST_SUITE(merge_tests)

Y_UNIT_TEST_SUITE(cascaded_merge_tests)
{
    Y_UNIT_TEST(test_one_polygon)
    {
        const geolib3::Polygon2 polygon({{0, 0}, {1, 0}, {1, 1}, {0, 1}});

        const geolib3::MultiPolygon2 expectedResult({polygon});

        geolib3::MultiPolygon2 result = cascadedMergePolygons({polygon});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_not_intersected_polygons)
    {
        const geolib3::Polygon2 polygon1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        const geolib3::Polygon2 polygon2({{2, 0}, {3, 0}, {3, 1}, {2, 1}});

        const geolib3::MultiPolygon2 expectedResult({polygon1, polygon2});

        geolib3::MultiPolygon2 result = cascadedMergePolygons({polygon1, polygon2});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_side_touch_polygons)
    {
        const geolib3::Polygon2 polygon1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        const geolib3::Polygon2 polygon2({{1, 0}, {2, 0}, {2, 1}, {1, 1}});

        const geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{0, 0}, {2, 0}, {2, 1}, {0, 1}})
        });

        geolib3::MultiPolygon2 result = cascadedMergePolygons({polygon1, polygon2});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_point_touch_polygons)
    {
        const geolib3::Polygon2 polygon1({{0, 0}, {1, 0}, {1, 1}, {0, 1}});
        const geolib3::Polygon2 polygon2({{1, 1}, {2, 1}, {2, 2}, {1, 2}});

        const geolib3::MultiPolygon2 expectedResult({
            polygon1, polygon2
        });

        geolib3::MultiPolygon2 result = cascadedMergePolygons({polygon1, polygon2});

        EXPECT_EQ(result.polygonsNumber(), 2u);

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }


    Y_UNIT_TEST(test_intersected_polygons)
    {
        const geolib3::Polygon2 polygon1({{0, 0}, {2, 0}, {2, 1}, {0, 1}});
        const geolib3::Polygon2 polygon2({{1, 0}, {3, 0}, {3, 1}, {1, 1}});

        const geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{0, 0}, {3, 0}, {3, 1}, {0, 1}})
        });

        geolib3::MultiPolygon2 result = cascadedMergePolygons({polygon1, polygon2});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_intersected_complex_polygons)
    {
        const geolib3::Polygon2 polygon1({{0, 0}, {2, 0}, {2, 1}, {1, 1}, {1, 2}, {2, 2}, {2, 3}, {0, 3}});
        const geolib3::Polygon2 polygon2({{1, 0}, {3, 0}, {3, 3}, {1, 3}, {1, 2}, {2, 2}, {2, 1}, {1, 1}});

        const geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2(geolib3::LinearRing2({{0, 0}, {3, 0}, {3, 3}, {0 ,3}}),
                              {geolib3::LinearRing2({{1, 1}, {2, 1}, {2, 2}, {1, 2}})})
        });

        geolib3::MultiPolygon2 result = cascadedMergePolygons({polygon1, polygon2});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_one_multipolygon)
    {
        const geolib3::MultiPolygon2 multiPolygon({
            geolib3::Polygon2({{0, 0}, {1, 0}, {1, 1}, {0, 1}}),
            geolib3::Polygon2({{2, 0}, {3, 0}, {3, 1}, {2, 1}})
        });

        const geolib3::MultiPolygon2 expectedResult = multiPolygon;

        geolib3::MultiPolygon2 result = cascadedMergeMultiPolygons({multiPolygon});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_not_intersected_multipolygons)
    {
        const geolib3::MultiPolygon2 multiPolygon1({
            geolib3::Polygon2({{0, 0}, {1, 0}, {1, 1}, {0, 1}}),
            geolib3::Polygon2({{2, 0}, {3, 0}, {3, 1}, {2, 1}})
        });
        const geolib3::MultiPolygon2 multiPolygon2({
            geolib3::Polygon2({{4, 0}, {5, 0}, {5, 1}, {4, 1}})
        });

        const geolib3::MultiPolygon2 expectedResult({
            multiPolygon1.polygonAt(0),
            multiPolygon1.polygonAt(1),
            multiPolygon2.polygonAt(0)
        });

        geolib3::MultiPolygon2 result = cascadedMergeMultiPolygons({multiPolygon1, multiPolygon2});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_intersected_multipolygons)
    {
        const geolib3::MultiPolygon2 multiPolygon1({
            geolib3::Polygon2({{0, 0}, {2, 0}, {2, 1}, {0, 1}}),
            geolib3::Polygon2({{3, 0}, {5, 0}, {5, 1}, {3, 1}})
        });
        const geolib3::MultiPolygon2 multiPolygon2({
            geolib3::Polygon2({{1, 0}, {4, 0}, {4, 1}, {1, 1}})
        });

        const geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{0, 0}, {5, 0}, {5, 1}, {0, 1}})
        });

        geolib3::MultiPolygon2 result = cascadedMergeMultiPolygons({multiPolygon1, multiPolygon2});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_intersected_complex_multipolygons)
    {
        const geolib3::MultiPolygon2 multiPolygon1({
            geolib3::Polygon2({{0, 0}, {1, 0}, {1, 3}, {0, 3}}),
            geolib3::Polygon2({{2, 0}, {3, 0}, {3, 3}, {2, 3}})
        });
        const geolib3::MultiPolygon2 multiPolygon2({
            geolib3::Polygon2({{0, 0}, {3, 0}, {3, 1}, {0, 1}}),
            geolib3::Polygon2({{0, 2}, {3, 2}, {3, 3}, {0, 3}})
        });

        const geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2(geolib3::LinearRing2({{0, 0}, {3, 0}, {3, 3}, {0 ,3}}),
                              {geolib3::LinearRing2({{1, 1}, {2, 1}, {2, 2}, {1, 2}})})
        });

        geolib3::MultiPolygon2 result = cascadedMergeMultiPolygons({multiPolygon1, multiPolygon2});

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_difference_rectangles)
    {
        const geolib3::MultiPolygon2 multiPolygon1({
            geolib3::Polygon2({{0, 0}, {1, 0}, {1, 2}, {0, 2}}),
        });
        const geolib3::MultiPolygon2 multiPolygon2({
            geolib3::Polygon2({{0, 1}, {1, 1}, {1, 2}, {0, 2}}),
        });

        const geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{0, 0}, {1, 0}, {1, 1}, {0, 1}}),
        });

        geolib3::MultiPolygon2 result = differenceMultiPolygons(multiPolygon1, multiPolygon2);

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_difference_multipolygons_with_complex_result)
    {
        const geolib3::MultiPolygon2 multiPolygon1({
            geolib3::Polygon2({{0, 0}, {3, 0}, {3, 3}, {0, 3}}),
        });
        const geolib3::MultiPolygon2 multiPolygon2({
            geolib3::Polygon2({{1, 1}, {2, 1}, {2, 2}, {1, 2}}),
        });

        const geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2(geolib3::LinearRing2({{0, 0}, {3, 0}, {3, 3}, {0 ,3}}),
                              {geolib3::LinearRing2({{1, 1}, {2, 1}, {2, 2}, {1, 2}})})
        });

        geolib3::MultiPolygon2 result = differenceMultiPolygons(multiPolygon1, multiPolygon2);

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }


    Y_UNIT_TEST(test_difference_complex_multipolygons)
    {
        const geolib3::MultiPolygon2 multiPolygon1({
            geolib3::Polygon2({{0, 0}, {3, 0}, {3, 3}, {0, 3}}),
        });
        const geolib3::MultiPolygon2 multiPolygon2({
            geolib3::Polygon2({{2, 2}, {4, 2}, {4, 4}, {2, 4}}),
            geolib3::Polygon2({{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}),
        });

        const geolib3::MultiPolygon2 expectedResult({
            geolib3::Polygon2({{0, 1}, {1, 1}, {1, 0}, {3, 0}, {3, 2}, {2, 2}, {2, 3}, {0, 3}}),
        });

        geolib3::MultiPolygon2 result = differenceMultiPolygons(multiPolygon1, multiPolygon2);

        EXPECT_TRUE(geolib3::spatialRelation(
            result, expectedResult,
            geolib3::SpatialRelation::Equals));
    }

    Y_UNIT_TEST(test_difference_multipolygons_with_empty_result)
    {
        const geolib3::MultiPolygon2 multiPolygon1({
            geolib3::Polygon2({{1, 1}, {2, 1}, {2, 2}, {1, 2}}),
        });
        const geolib3::MultiPolygon2 multiPolygon2({
            geolib3::Polygon2({{0, 0}, {3, 0}, {3, 3}, {0, 3}}),
        });

        geolib3::MultiPolygon2 result = differenceMultiPolygons(multiPolygon1, multiPolygon2);

        EXPECT_EQ(result.polygonsNumber(), 0u);
    }


} //Y_UNIT_TEST_SUITE(cascaded_merge_tests)

} //namespace tests
} //namespace autocart
} //namespace wiki
} //namespace maps
