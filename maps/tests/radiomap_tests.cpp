#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/libs/common/include/exception.h>
#include <maps/libs/log8/include/log8.h>

#include <maps/indoor/radiomap/lib/include/tile_storage.h>

#include <fstream>

using namespace testing;

namespace maps::indoor::radiomap {

namespace tests {

namespace {

const std::string DATASET_PATH
    = "maps/indoor/radiomap/tests/data/";

const std::string RADIOMAP_TILES_VERSION = "21.04.01-2";
const std::string RADIOMAP_COVERAGE_VERSION = "21.04.01";

struct TileData
{
    int x;
    int y;
    int z;
    int size;
};

std::vector<TileData> nonEmptyRadiomapTiles = {
    {19783, 10282, 15, 186169},
    {19786, 10261, 15, 31063},
    {19786, 10262, 15, 299},
    {19787, 10261, 15, 117663},
    {19787, 10262, 15, 16359},
    {19796, 10261, 15, 35140},
    {19797, 10261, 15, 84859},
    {19799, 10266, 15, 40494},
    {19800, 10266, 15, 190473},
    {19800, 10273, 15, 110185},
    {19801, 10273, 15, 45026},
    {19803, 10273, 15, 4298},
    {19803, 10274, 15, 150516},
    {19806, 10295, 15, 21800},
    {19807, 10295, 15, 77404},
    {19808, 10271, 15, 60492},
    {19811, 10257, 15, 44392},
    {19811, 10258, 15, 92},
    {19811, 10271, 15, 8698},
    {19811, 10272, 15, 25809},
    {19812, 10257, 15, 50523},
    {19817, 10295, 15, 69952},
    {19817, 10299, 15, 90587},
    {19828, 10287, 15, 16009},
    {19828, 10288, 15, 116081},
    {19829, 10287, 15, 75},
    {19829, 10288, 15, 357},
    {20857, 10269, 15, 120},
    {20857, 10270, 15, 46205},
    {21498, 9892, 15, 29864}
};

std::vector<TileData> nonEmptyCoverageTiles = {
    {0, 0, 1, 39},
    {0, 1, 1, 39},
    {1, 0, 1, 39},
    {1, 1, 1, 39},
};

} // namespace

Y_UNIT_TEST_SUITE(radiomap_storage_should)
{

Y_UNIT_TEST(test_radiomap_check_non_empty_tiles)
{
    auto radiomapStorage = storage::createIndoorRadiomapTileStorage(BinaryPath(DATASET_PATH));

    for (const auto& tileData : nonEmptyRadiomapTiles) {
        auto tile = radiomapStorage->getTile(RADIOMAP_TILES_VERSION, tileData.x, tileData.y, tileData.z);
        ASSERT_TRUE(tile != std::nullopt);
        ASSERT_EQ((*tile).size(), tileData.size);
    }

    auto tile = radiomapStorage->getTile(RADIOMAP_TILES_VERSION, 1, 2, 15);
    ASSERT_TRUE(tile != std::nullopt);
    ASSERT_EQ((*tile).size(), 0);

    tile = radiomapStorage->getTile(RADIOMAP_TILES_VERSION, 1, 2, 14);
    ASSERT_TRUE(tile == std::nullopt);
}

Y_UNIT_TEST(test_radiomap_check_non_empty_experiment_tiles)
{
    auto radiomapStorage = storage::createIndoorRadiomapTileStorage(BinaryPath(DATASET_PATH));

    for (const auto& tileData : nonEmptyRadiomapTiles) {
        auto tile = radiomapStorage->getTile(RADIOMAP_TILES_VERSION, tileData.x, tileData.y, tileData.z, "experiment");
        ASSERT_TRUE(tile != std::nullopt);
        ASSERT_EQ((*tile).size(), tileData.size);
    }

    auto tile = radiomapStorage->getTile(RADIOMAP_TILES_VERSION, 1, 2, 15, "experiment");
    ASSERT_TRUE(tile != std::nullopt);
    ASSERT_EQ((*tile).size(), 0);

    tile = radiomapStorage->getTile(RADIOMAP_TILES_VERSION, 1, 2, 14, "experiment");
    ASSERT_TRUE(tile == std::nullopt);
}

Y_UNIT_TEST(test_radiomap_check_version)
{
    auto radiomapStorage = storage::createIndoorRadiomapTileStorage(BinaryPath(DATASET_PATH));

    auto tile = radiomapStorage->getTile(RADIOMAP_TILES_VERSION, 19783, 10282, 15);
    ASSERT_TRUE(tile != std::nullopt);
    ASSERT_EQ((*tile).size(), 186169);

    radiomapStorage->closeVersion(RADIOMAP_TILES_VERSION);

    tile = radiomapStorage->getTile(RADIOMAP_TILES_VERSION, 19783, 10282, 15);
    ASSERT_EQ((*tile).size(), 0);

    EXPECT_THROW(radiomapStorage->openVersion("invalid_version"), maps::Exception);

    tile = radiomapStorage->getTile(RADIOMAP_TILES_VERSION, 19783, 10282, 15);
    ASSERT_EQ((*tile).size(), 0);

    EXPECT_NO_THROW(radiomapStorage->openVersion(RADIOMAP_TILES_VERSION));

    tile = radiomapStorage->getTile(RADIOMAP_TILES_VERSION, 19783, 10282, 15);
    ASSERT_TRUE(tile != std::nullopt);
    ASSERT_EQ((*tile).size(), 186169);
}

Y_UNIT_TEST(test_coverage_success)
{
    auto coverageStorage = storage::createIndoorRadiomapCoverageStorage(BinaryPath(DATASET_PATH));

    for (const auto& tileData : nonEmptyCoverageTiles) {
        auto tile = coverageStorage->getTile(RADIOMAP_COVERAGE_VERSION, tileData.x, tileData.y, tileData.z);
        ASSERT_TRUE(tile != std::nullopt);
        ASSERT_EQ((*tile).size(), tileData.size);
    }

    auto tile = coverageStorage->getTile(RADIOMAP_COVERAGE_VERSION, 0, 0, 0);
    ASSERT_TRUE(tile != std::nullopt);
    ASSERT_EQ((*tile).size(), 0);

    tile = coverageStorage->getTile(RADIOMAP_COVERAGE_VERSION, 1, 2, 3);
    ASSERT_TRUE(tile == std::nullopt);
}

Y_UNIT_TEST(test_coverage_experiment_success)
{
    auto coverageStorage = storage::createIndoorRadiomapCoverageStorage(BinaryPath(DATASET_PATH));

    for (const auto& tileData : nonEmptyCoverageTiles) {
        auto tile = coverageStorage->getTile(RADIOMAP_COVERAGE_VERSION, tileData.x, tileData.y, tileData.z, "experiment");
        ASSERT_TRUE(tile != std::nullopt);
        ASSERT_EQ((*tile).size(), tileData.size);
    }

    auto tile = coverageStorage->getTile(RADIOMAP_COVERAGE_VERSION, 0, 0, 0, "experiment");
    ASSERT_TRUE(tile != std::nullopt);
    ASSERT_EQ((*tile).size(), 0);

    tile = coverageStorage->getTile(RADIOMAP_COVERAGE_VERSION, 1, 2, 3, "experiment");
    ASSERT_TRUE(tile == std::nullopt);
}

Y_UNIT_TEST(test_coverage_check_version)
{
    auto coverageStorage = storage::createIndoorRadiomapCoverageStorage(BinaryPath(DATASET_PATH));

    auto tile = coverageStorage->getTile(RADIOMAP_COVERAGE_VERSION, 0, 0, 1);
    ASSERT_TRUE(tile != std::nullopt);
    ASSERT_EQ((*tile).size(), 39);

    coverageStorage->closeVersion(RADIOMAP_COVERAGE_VERSION);

    tile = coverageStorage->getTile(RADIOMAP_COVERAGE_VERSION, 0, 0, 1);
    ASSERT_EQ((*tile).size(), 0);

    EXPECT_THROW(coverageStorage->openVersion("invalid_version"), maps::Exception);

    tile = coverageStorage->getTile(RADIOMAP_COVERAGE_VERSION, 0, 0, 1);
    ASSERT_EQ((*tile).size(), 0);

    EXPECT_NO_THROW(coverageStorage->openVersion(RADIOMAP_COVERAGE_VERSION));

    tile = coverageStorage->getTile(RADIOMAP_COVERAGE_VERSION, 0, 0, 1);
    ASSERT_TRUE(tile != std::nullopt);
    ASSERT_EQ((*tile).size(), 39);
}

} // Y_UNIT_TEST_SUITE(radiomap_storage_should)

} // namespace test

} // namespace maps::indoor::radiomap
