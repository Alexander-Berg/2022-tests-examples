#include "testing_common.h"

#include <maps/factory/libs/tileindex/impl/quadrant.h>

using namespace testing;

namespace maps {
namespace tileindex {
namespace impl {
namespace tests {

Y_UNIT_TEST_SUITE(Quadrant_Should)
{
Y_UNIT_TEST(get_index_from_coords)
{
    EXPECT_THAT(Quadrant(0, 0).index(), Eq(0ull));
    EXPECT_THAT(Quadrant(1, 0).index(), Eq(1ull));
    EXPECT_THAT(Quadrant(0, 1).index(), Eq(2ull));
    EXPECT_THAT(Quadrant(1, 1).index(), Eq(3ull));
}

Y_UNIT_TEST(get_coords_from_index)
{
    for (Coord v = 0; v < Quadrant::COUNT; ++v) {
        Quadrant quad{v};
        EXPECT_THAT(Quadrant(quad.x(), quad.y()), Eq(quad));
    }
}

Y_UNIT_TEST(print_itself)
{
    EXPECT_THAT(boost::lexical_cast<std::string>(Quadrant(1)), Eq("1"));
}
}

Y_UNIT_TEST_SUITE(AllQuadrants_Should)
{
Y_UNIT_TEST(iterate_from_first_to_last)
{
    for (Coord v = 0; v < Quadrant::COUNT; ++v) {
        EXPECT_THAT(Quadrant{v}.index(), Eq(v));
    }
}

Y_UNIT_TEST(range_iterate_from_first_to_last)
{
    unsigned i = 0;
    for (auto quad: AllQuadrants{}) {
        EXPECT_THAT(quad.index(), Eq(i));
        ++i;
    }
    EXPECT_THAT(i, Eq(4u));
}
}

} // namespace tests
} // namespace impl
} // namespace tileindex
} // namespace maps
