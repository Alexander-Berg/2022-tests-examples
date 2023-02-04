#include <maps/factory/libs/tileindex/tools/lib/approximate_polygon.h>

#include <maps/factory/libs/tileindex/impl/tree_editable.h>
#include <maps/factory/libs/tileindex/tests/testing_common.h>

#include <fstream>

using namespace testing;

namespace maps {
namespace tileindex {
namespace impl {
namespace tests {

Y_UNIT_TEST_SUITE(ApproximatePolygon2_Should)
{
Y_UNIT_TEST(check_border_tiles)
{
    std::ifstream ifs{"./Release_1_85_0.bin"};
    const auto poly
        = geolib3::EWKB::read<geolib3::SpatialReference::Epsg3395,
                              geolib3::MultiPolygon2>(ifs);
    ifs.close();

    ApproximatePolygon2 apPoly{poly};

    const auto disjoint = [&](Tile tile) {
        return !apPoly.intersectsApprox(MercatorProjection{}(tile));
    };
    const auto contains = [&](Tile tile) {
        return apPoly.containsApprox(MercatorProjection{}(tile));
    };
    const auto border = [&](Tile tile) {
        return apPoly.isBorder(MercatorProjection{}(tile));
    };

    EXPECT_TRUE(disjoint({53175, 22409, 16})); // border
    EXPECT_TRUE(!disjoint({53174, 22409, 16}));
    EXPECT_TRUE(!disjoint({53175, 22410, 16}));
    EXPECT_TRUE(disjoint({53176, 22409, 16}));
    EXPECT_TRUE(disjoint({106350, 44819, 17})); // border
    EXPECT_TRUE(!disjoint({26587, 11204, 15}));
    EXPECT_TRUE(disjoint({114789, 37591, 17})); // border
    EXPECT_TRUE(!disjoint({114789, 37590, 17}));
    EXPECT_TRUE(!disjoint({114789, 37589, 17}));
    EXPECT_TRUE(!disjoint({114789, 37588, 17}));
    EXPECT_TRUE(disjoint({114789, 37592, 17}));
    //
    EXPECT_TRUE(disjoint({53175, 22409, 16}));  // border
    EXPECT_TRUE(disjoint({106350, 44819, 17})); // border
    EXPECT_TRUE(disjoint({114789, 37591, 17})); // border
    EXPECT_TRUE(disjoint({212700, 89639, 18})); // border
    EXPECT_TRUE(disjoint({212998, 77251, 18})); // border
    EXPECT_TRUE(disjoint({211000, 86925, 18})); // border
    //
    EXPECT_TRUE(disjoint({217579, 84387, 18})); // border
    EXPECT_TRUE(!contains({217579, 84387, 18}));
    EXPECT_TRUE(!disjoint({217579 - 1, 84387, 18})); // inside
    EXPECT_TRUE(!disjoint({217579 - 2, 84387, 18})); // inside
    EXPECT_TRUE(contains({217579 - 2, 84387, 18}));
    EXPECT_TRUE(disjoint({217579 + 1, 84387, 18})); // outside
    EXPECT_TRUE(!contains({217579 + 1, 84387, 18}));
    EXPECT_TRUE(disjoint({217579 + 2, 84387, 18})); // outside
    EXPECT_TRUE(!contains({217579 + 2, 84387, 18}));
    EXPECT_TRUE(!disjoint({217579, 84387 - 1, 18})); // intersects
    EXPECT_TRUE(disjoint({217579, 84387 + 1, 18}));  // not intersects
    //
    EXPECT_TRUE(disjoint({229578, 75182, 18}));  // border
    EXPECT_TRUE(disjoint({216303, 85572, 18}));  // border
    EXPECT_TRUE(disjoint({425400, 179279, 19})); // border
    EXPECT_TRUE(disjoint({458940, 150234, 19})); // border
    EXPECT_TRUE(disjoint({463345, 177397, 19})); // border
    EXPECT_TRUE(disjoint({426523, 163108, 19})); // border
    EXPECT_TRUE(disjoint({462787, 153440, 19})); // border
    EXPECT_TRUE(disjoint({449641, 171269, 19})); // border
    EXPECT_TRUE(disjoint({466458, 180063, 19})); // border
    EXPECT_TRUE(disjoint({425997, 154502, 19})); // border
    EXPECT_TRUE(disjoint({446211, 170859, 19})); // border
    EXPECT_TRUE(disjoint({432606, 171145, 19})); // border
    EXPECT_TRUE(disjoint({414369, 172265, 19})); // border
    EXPECT_TRUE(disjoint({466578, 180394, 19})); // border
    EXPECT_TRUE(disjoint({461755, 208085, 19})); // border
    EXPECT_TRUE(disjoint({446202, 170858, 19})); // border
    EXPECT_TRUE(disjoint({447970, 176791, 19})); // border
    EXPECT_TRUE(disjoint({446220, 170860, 19})); // border
    EXPECT_TRUE(disjoint({465573, 207431, 19})); // border
    EXPECT_TRUE(disjoint({520516, 137446, 19})); // border
    EXPECT_TRUE(disjoint({459434, 190190, 19})); // border
    EXPECT_TRUE(disjoint({465604, 207322, 19})); // border
    EXPECT_TRUE(disjoint({457333, 189391, 19})); // border
    EXPECT_TRUE(disjoint({493376, 163800, 19})); // border
    EXPECT_TRUE(disjoint({459838, 207774, 19})); // border
    EXPECT_TRUE(disjoint({459156, 150364, 19})); // border
    EXPECT_TRUE(disjoint({465618, 207595, 19})); // border
    EXPECT_TRUE(disjoint({435158, 168774, 19})); // border
    EXPECT_TRUE(disjoint({422001, 173851, 19})); // border
    EXPECT_TRUE(disjoint({427274, 143270, 19})); // border
    EXPECT_TRUE(disjoint({422339, 174810, 19})); // border

    EXPECT_TRUE(border({53175, 22409, 16})); // border
    EXPECT_TRUE(!border({53174, 22409, 16}));
    EXPECT_TRUE(!border({53175, 22410, 16}));
    EXPECT_TRUE(!border({53176, 22409, 16}));
    EXPECT_TRUE(border({106350, 44819, 17})); // border
    EXPECT_TRUE(!border({26587, 11204, 15}));
    EXPECT_TRUE(border({114789, 37591, 17})); // border
    EXPECT_TRUE(!border({114789, 37590, 17}));
    EXPECT_TRUE(!border({114789, 37589, 17}));
    EXPECT_TRUE(!border({114789, 37588, 17}));
    EXPECT_TRUE(!border({114789, 37592, 17}));

    EXPECT_TRUE(border({229578, 75182, 18}));  // border
    EXPECT_TRUE(border({216303, 85572, 18}));  // border
    EXPECT_TRUE(border({425400, 179279, 19})); // border
    EXPECT_TRUE(border({458940, 150234, 19})); // border
    EXPECT_TRUE(border({463345, 177397, 19})); // border
    EXPECT_TRUE(border({426523, 163108, 19})); // border
    EXPECT_TRUE(border({462787, 153440, 19})); // border
    EXPECT_TRUE(border({449641, 171269, 19})); // border
    EXPECT_TRUE(border({466458, 180063, 19})); // border
    EXPECT_TRUE(border({425997, 154502, 19})); // border
    EXPECT_TRUE(border({446211, 170859, 19})); // border
    EXPECT_TRUE(border({432606, 171145, 19})); // border
    EXPECT_TRUE(border({414369, 172265, 19})); // border
    EXPECT_TRUE(border({466578, 180394, 19})); // border
    EXPECT_TRUE(border({461755, 208085, 19})); // border
    EXPECT_TRUE(border({446202, 170858, 19})); // border
    EXPECT_TRUE(border({447970, 176791, 19})); // border
    EXPECT_TRUE(border({446220, 170860, 19})); // border
    EXPECT_TRUE(border({465573, 207431, 19})); // border
    EXPECT_TRUE(border({520516, 137446, 19})); // border
    EXPECT_TRUE(border({459434, 190190, 19})); // border
    EXPECT_TRUE(border({465604, 207322, 19})); // border
    EXPECT_TRUE(border({457333, 189391, 19})); // border
    EXPECT_TRUE(border({493376, 163800, 19})); // border
    EXPECT_TRUE(border({459838, 207774, 19})); // border
    EXPECT_TRUE(border({459156, 150364, 19})); // border
    EXPECT_TRUE(border({465618, 207595, 19})); // border
    EXPECT_TRUE(border({435158, 168774, 19})); // border
    EXPECT_TRUE(border({422001, 173851, 19})); // border
    EXPECT_TRUE(border({427274, 143270, 19})); // border
    EXPECT_TRUE(border({422339, 174810, 19})); // border
}
}

} // namespace tests
} // namespace impl
} // namespace tileindex
} // namespace maps
