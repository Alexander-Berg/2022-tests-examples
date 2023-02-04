#include <maps/factory/libs/rendering/sat_tile.h>

#include <maps/factory/libs/storage/local_storage.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::factory::rendering::tests {

Y_UNIT_TEST_SUITE(sat_tile_should) {

Y_UNIT_TEST(render_all_scanex_steps)
{
    //const auto tile = tile::Tile{34679, 23853, 16};
    //const auto cogPath = "./scanex_20201165";
    //const auto storage = storage::localStorage(cogPath);
    //const auto cog = delivery::Cog{*storage};
    //const auto backgroundPath =
    //    std::string(SRC_("data/058800151040_01_TILES/prod_{x}_{y}_{z}.png"));
    //
    //
}

} // Y_UNIT_TEST_SUITE(sat_tile_should)
} // namespace maps::factory::rendering::tests
