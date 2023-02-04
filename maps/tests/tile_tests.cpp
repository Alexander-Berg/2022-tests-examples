#include "testing_common.h"

#include <maps/factory/libs/tileindex/impl/tile.h>

#include <unordered_set>

using namespace testing;

namespace maps {
namespace tileindex {
namespace impl {
namespace tests {

Y_UNIT_TEST_SUITE(TileOffset_Should)
{
Y_UNIT_TEST(shift)
{
    TileOffset c{3, 5};
    EXPECT_THAT(c << 1, Eq(TileOffset{6, 10}));
    EXPECT_THAT(c >> 1, Eq(TileOffset{1, 2}));
}

Y_UNIT_TEST(add_subtract)
{
    TileOffset c{3, 5};
    EXPECT_THAT(c + TileOffset(1, 2), Eq(TileOffset{4, 7}));
    EXPECT_THAT(c - TileOffset(1, 2), Eq(TileOffset{2, 3}));
}

Y_UNIT_TEST(print_itself)
{
    EXPECT_THAT(boost::lexical_cast<std::string>(TileOffset(1, 2)),
        Eq("1, 2"));
}
}

Y_UNIT_TEST_SUITE(TilePos_Should)
{
Y_UNIT_TEST(create_Earth_tile)
{
    auto earth = Tile::Earth();
    EXPECT_THAT(earth, Eq(Tile{0, 0, 0}));
    EXPECT_THAT(earth.x(), Eq(0ul));
    EXPECT_THAT(earth.y(), Eq(0ul));
    EXPECT_THAT(earth.zoom(), Eq(0ul));
}

Y_UNIT_TEST(get_max_allowed_coordinates)
{
    EXPECT_THAT(maxTileOffset(0), Eq(0ull));
    EXPECT_THAT(totalTilesAlongAxis(0), Eq(1ull));
    EXPECT_THAT(maxTileOffset(1), Eq(1ull));
    EXPECT_THAT(totalTilesAlongAxis(1), Eq(2ull));
    EXPECT_THAT(maxTileOffset(3), Eq(7ull));
    EXPECT_THAT(totalTilesAlongAxis(3), Eq(8ull));
}

Y_UNIT_TEST(get_number_of_tiles_for_zoom)
{
    EXPECT_THAT(totalTilesCount(0), Eq(1ull));
    EXPECT_THAT(totalTilesCount(1), Eq(4ull));
    EXPECT_THAT(totalTilesCount(3), Eq(64ull));
}

Y_UNIT_TEST(get_parent)
{
    EXPECT_THAT(Tile(2, 3, 4).ancestor(0), Eq(Tile{2, 3, 4}));
    EXPECT_THAT(Tile(2, 3, 4).ancestor(), Eq(Tile{1, 1, 3}));
    EXPECT_THAT(Tile(3, 4, 5).ancestor(2), Eq(Tile{0, 1, 3}));
    EXPECT_THAT(Tile(4, 5, 6).ancestorWithZoom(6), Eq(Tile{4, 5, 6}));
    EXPECT_THAT(Tile(4, 5, 6).ancestorWithZoom(5), Eq(Tile{2, 2, 5}));
    EXPECT_THAT(Tile(7, 6, 5).ancestorWithZoom(0), Eq(Tile::Earth()));
}

Y_UNIT_TEST(get_quadrant)
{
    EXPECT_THAT(Tile(2, 3, 2).quadrant(), Eq(Quadrant{2}));
    EXPECT_THAT(Tile::Earth().quadrant(), Eq(Quadrant{}));
}

Y_UNIT_TEST(get_child)
{
    for (auto quad: AllQuadrants{}) {
        EXPECT_THAT(Tile::Earth().child(quad), Eq(Tile{TileOffset{quad}, 1}));
    }
}

Y_UNIT_TEST(check_contains)
{
    EXPECT_THAT(Tile::Earth().contains(Tile(1, 1, 1)), IsTrue());
    EXPECT_THAT(Tile::Earth().contains(Tile::Earth()), IsTrue());
    EXPECT_THAT(Tile(0, 1, 2).contains(Tile::Earth()), IsFalse());
    EXPECT_THAT(Tile(0, 1, 2).contains(Tile(0, 1, 2)), IsTrue());
    EXPECT_THAT(Tile(0, 1, 2).contains(Tile(0, 1, 3)), IsFalse());
    EXPECT_THAT(Tile(0, 1, 2).contains(Tile(0, 2, 3)), IsTrue());
}

Y_UNIT_TEST(check_within)
{
    for (size_t i = 0; i < 10; ++i) {
        auto first = randomTile(4);
        auto second = randomTile(4);
        EXPECT_THAT(first.contains(second), Eq(second.within(first)));
    }
}

Y_UNIT_TEST(print_itself)
{
    EXPECT_THAT(boost::lexical_cast<std::string>(Tile(1, 2, 3)),
        Eq("1, 2, 3"));
}

Y_UNIT_TEST(be_hashed)
{
    std::unordered_set<Tile, TileHash> set;
    set.emplace(3, 4, 5);
    set.emplace(5, 4, 3);
    EXPECT_THAT(set.count(Tile{3, 4, 5}), Eq(1u));
    EXPECT_THAT(set.count(Tile{5, 4, 3}), Eq(1u));
    EXPECT_THAT(set.count(Tile{3, 5, 4}), Eq(0u));
}
}

} // namespace tests
} // namespace impl
} // namespace tileindex
} // namespace maps
