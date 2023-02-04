#include "testing_common.h"

#include <maps/factory/libs/tileindex/impl/tile_range.h>

#include <future>
#include <unordered_set>

using namespace testing;

namespace maps {
namespace tileindex {
namespace impl {
namespace tests {

Y_UNIT_TEST_SUITE(TileRange_Should)
{
Y_UNIT_TEST(get_same_tile_when_iterate_with_same_zoom)
{
    Tile tile{0, 0, 3};
    TileRange range{3, tile};
    EXPECT_THAT(range, ElementsAre(tile));
}

Y_UNIT_TEST(iterate_in_same_order_as_quadrant)
{
    TileRange range{1};
    auto it = range.begin();
    for (auto quad: AllQuadrants{}) {
        EXPECT_THAT(it->offset(), Eq(TileOffset{quad}));
        ++it;
    }
}

Y_UNIT_TEST(iterate_next_zoom)
{
    Tile tile{1, 2, 3};
    TileRange range{4, tile};
    std::vector<Tile> tiles;
    std::copy(range.begin(), range.end(), std::back_inserter(tiles));
    EXPECT_THAT(tiles, ElementsAre(Tile{2, 4, 4}, Tile{3, 4, 4},
        Tile{2, 5, 4}, Tile{3, 5, 4}));
}

Y_UNIT_TEST(produce_different_tiles_that_lies_within_top_tile)
{
    Tile tile = randomTile(2);
    std::vector<Tile> tiles;
    for (Zoom dz = 0; dz < 3; ++dz) {
        TileRange range{dz + tile.zoom(), tile};
        tiles.clear();
        std::copy(range.begin(), range.end(), std::back_inserter(tiles));
        EXPECT_THAT(range, SizeIs(tiles.size()));
        EXPECT_THAT(tiles, AllDifferent());
        EXPECT_THAT(tiles, Each(Within(tile)));
    }
}

Y_UNIT_TEST(iterate_through_same_nuber_of_tiles_as_range_size)
{
    TileRange range{3};
    auto it = range.begin();
    auto end = range.end();
    for (size_t i = 0; i < range.size(); ++i) {
        EXPECT_THAT(it == end, IsFalse());
        ++it;
    }
    EXPECT_THAT(it == end, IsTrue());
}

Y_UNIT_TEST(iterate_using_atomic_iterator)
{
    TileRange range{3};
    auto next = range.atomic();
    auto it = range.begin();
    for (size_t i = 0; i < range.size(); ++i) {
        EXPECT_THAT(next(), Eq(*it));
        ++it;
    }
    EXPECT_THAT(next(), IsNothing());
}

Y_UNIT_TEST(iterate_in_parallel)
{
    // Run under thread sanitizer.
    TileRange range{6};
    auto next = range.atomic();
    std::vector<std::future<std::vector<Tile>>> futures;
    for (size_t i = 0; i < 64; ++i) {
        futures.push_back(std::async(std::launch::async, [&] {
            std::vector<Tile> tmp;
            while (auto tile = next()) {
                tmp.push_back(*tile);
            }
            return tmp;
        }));
    }
    std::unordered_set<Tile, TileHash> tiles;
    for (auto& future: futures) {
        future.wait();
        auto tmp = future.get();
        tiles.insert(tmp.begin(), tmp.end());
    }

    const auto expected
        = boost::copy_range<std::unordered_set<Tile, TileHash>>(range);
    EXPECT_THAT(tiles, Eq(expected));
}
}

} // namespace tests
} // namespace impl
} // namespace tileindex
} // namespace maps
