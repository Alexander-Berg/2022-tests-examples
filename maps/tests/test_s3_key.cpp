#include <maps/factory/libs/common/s3_keys.h>

#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

namespace maps::factory::tests {

Y_UNIT_TEST_SUITE(s3_tile_key_should) {

constexpr tile::Tile TEST_TILE(1234, 642, 13);
const tile::Tile ANOTHER_TEST_TILE(
    /*
     * Adding 4 to x coordinate should change first byte of the key,
     * since this will change the topmost 6 bit
     * (which will be encoded into the first byte of the key)
     */
    TEST_TILE.x() + 4,
    TEST_TILE.y(),
    TEST_TILE.z()
);

constexpr uint32_t TEST_ISSUE_ID = 31337;
constexpr auto EXPECTED_KEY = "0gQAAAAAAACCAgAAAAAAAA0AAABpegAA";

Y_UNIT_TEST(make_hex_key)
{
    std::string key = makeTileKey(TEST_TILE, TEST_ISSUE_ID);
    EXPECT_EQ(key.size(), 32);
    EXPECT_EQ(key, EXPECTED_KEY);
    EXPECT_TRUE(extractTileFromKey(key) == TEST_TILE);
    EXPECT_EQ(extractIssueIdFromKey(key), TEST_ISSUE_ID);

    std::string anotherKey = makeTileKey(ANOTHER_TEST_TILE, TEST_ISSUE_ID);
    EXPECT_NE(key.front(), anotherKey.front());
}

Y_UNIT_TEST(make_pattern_key)
{
    EXPECT_EQ(makeTileKey("", TEST_TILE, TEST_ISSUE_ID), "");
    EXPECT_EQ(makeTileKey("{x}", TEST_TILE, TEST_ISSUE_ID), "1234");
    EXPECT_EQ(makeTileKey("{x}_{y}_{z}", TEST_TILE, TEST_ISSUE_ID), "1234_642_13");
    EXPECT_EQ(makeTileKey("xyz_{x}_{y}_{z}_{v}.jpg", TEST_TILE, TEST_ISSUE_ID), "xyz_1234_642_13_31337.jpg");
    EXPECT_EQ(makeTileKey("some_dir/{x}/{y}/{z}/{v}/tile.jpg", ANOTHER_TEST_TILE, TEST_ISSUE_ID),
        "some_dir/1238/642/13/31337/tile.jpg");
}

} // suite

Y_UNIT_TEST_SUITE(gps_tile_key_should) {

constexpr tile::Tile TEST_TILE(1234, 642, 13);
const tile::Tile ANOTHER_TEST_TILE(
    /*
     * Adding 4 to x coordinate should change first byte of the key,
     * since this will change the topmost 6 bit
     * (which will be encoded into the first byte of the key)
     */
    TEST_TILE.x() + 4,
    TEST_TILE.y(),
    TEST_TILE.z()
);

constexpr auto TEST_STYLE = "point";
constexpr auto EXPECTED_KEY = "0gQAAAAAAACCAgAAAAAAAA0AAAACAAAA";

Y_UNIT_TEST(gps_tile_key_test)
{
    std::string key = makeGpsTileKey(TEST_TILE, TEST_STYLE);
    EXPECT_EQ(key.size(), 32);
    EXPECT_EQ(key, EXPECTED_KEY);
    EXPECT_TRUE(extractGpsTileFromKey(key) == TEST_TILE);
    EXPECT_EQ(extractStyleFromKey(key), TEST_STYLE);

    std::string anotherKey = makeGpsTileKey(ANOTHER_TEST_TILE, TEST_STYLE);
    EXPECT_NE(key.front(), anotherKey.front());

    EXPECT_THROW(makeGpsTileKey(TEST_TILE, "incorrect"), RuntimeError);
}

} // suite

} // namespace maps::factory::tests
