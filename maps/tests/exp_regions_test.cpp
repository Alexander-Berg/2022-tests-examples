#include <maps/analyzer/libs/exp_regions/include/exp_regions.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace er = maps::analyzer::exp_regions;

TEST(ExpRegions, Intersection) {
    std::vector<maps::geolib3::Polygon2> polygons = {
        maps::geolib3::Polygon2(
            {
                maps::geolib3::Point2(55.73, 37.57),
                maps::geolib3::Point2(55.78, 37.62),
                maps::geolib3::Point2(55.73, 37.67)
            }
        )
    };

    const er::Regions regions(er::serializeRegions(polygons));

    EXPECT_TRUE(
        regions.intersects(
            maps::geolib3::Point2(55.75, 37.62)
        )
    );
    EXPECT_FALSE(
        regions.intersects(
            maps::geolib3::Point2(55.77, 37.59)
        )
    );

    EXPECT_TRUE(
        regions.intersects(
            maps::geolib3::BoundingBox(
                maps::geolib3::Point2(55.70, 37.55),
                maps::geolib3::Point2(55.80, 37.70)
            )
        )
    );
    EXPECT_FALSE(
        regions.intersects(
            maps::geolib3::BoundingBox(
                maps::geolib3::Point2(55.77, 37.58),
                maps::geolib3::Point2(55.79, 37.59)
            )
        )
    );
}
