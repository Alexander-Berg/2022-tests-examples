#include <maps/carparks/tools/renderer_comparator/lib/region.h>
#include <maps/carparks/tools/renderer_comparator/lib/report.h>
#include <maps/carparks/tools/renderer_comparator/lib/compare.h>
#include <maps/libs/common/include/zlib.h>
#include <maps/libs/http/include/test_utils.h>
#include <library/cpp/testing/unittest/registar.h>
#include <iostream>
#include <sstream>


using namespace maps::carparks::renderer_comparator;

namespace {

constexpr auto EPSILON = 1.e-6;

TileProto createTileProto(
    int pointsCount,
    int polylinesCount,
    int polygonsCount,
    // Attributes are multimap, or vector of maps with same key sets.
    const std::vector<std::string>& attributeKeys,
    const std::vector<std::string>& attributeValues)
{
    namespace proto = yandex::maps::proto::renderer::vmap2;

    TileProto tileProto;

    if (pointsCount) {
        auto points = new proto::Tile_PointObjects;
        while (pointsCount-- > 0) {
            points->add_coordsx(pointsCount);
            points->add_coordsy(pointsCount);
            points->add_classid(pointsCount);
        }
        tileProto.set_allocated_points(points);
    }

    if (polylinesCount) {
        auto polylines = new proto::Tile_PolylineObjects;
        while (polylinesCount-- > 0) {
            polylines->add_coordsx(polylinesCount);
            polylines->add_coordsy(polylinesCount);
            polylines->add_coordsx(polylinesCount + 1);
            polylines->add_coordsy(polylinesCount + 1);
            polylines->add_linesize(2);
            polylines->add_classid(polylinesCount);
        }
        tileProto.set_allocated_polylines(polylines);
    }

    if (polygonsCount) {
        auto polygons = new proto::Tile_PolygonObjects;
        while (polygonsCount-- > 0) {
            polygons->add_coordsx(polygonsCount);
            polygons->add_coordsy(polygonsCount);
            polygons->add_coordsx(polygonsCount + 1);
            polygons->add_coordsy(polygonsCount);
            polygons->add_coordsx(polygonsCount);
            polygons->add_coordsy(polygonsCount + 1);
            polygons->add_ringcount(1);
            polygons->add_ringsize(3);
            polygons->add_classid(polygonsCount);
        }
        tileProto.set_allocated_polygons(polygons);
    }

    if (!attributeKeys.empty() && !attributeValues.empty()) {
        auto *layerProto = tileProto.add_layers();
        for (const auto& key: attributeKeys) {
            layerProto->add_keys(key.c_str());
        }
        for (const auto& value: attributeValues) {
            layerProto->add_values(value.c_str());
        }
    }

    return tileProto;
}

}

Y_UNIT_TEST_SUITE(RendererComparator) {

Y_UNIT_TEST(GeoPointToTile)
{
    auto tile = geoPointToTile({37.620153, 55.753385}, 10);
    UNIT_ASSERT_EQUAL(tile.x(), 619);
    UNIT_ASSERT_EQUAL(tile.y(), 321);
    UNIT_ASSERT_EQUAL(tile.z(), 10);

    tile = geoPointToTile({-51.465680, -10.549860}, 15);
    UNIT_ASSERT_EQUAL(tile.x(), 11699);
    UNIT_ASSERT_EQUAL(tile.y(), 17343);
    UNIT_ASSERT_EQUAL(tile.z(), 15);
}

Y_UNIT_TEST(TileCoordinateParams)
{
    auto url = maps::http::URL{"http://a.b/"};
    auto tile = maps::tile::Tile{100, 100, 10};
    UNIT_ASSERT_STRINGS_EQUAL(
        addTileCoordinateParams(url, tile).toString(),
        "http://a.b/?x=100&y=100&z=10");

    url = maps::http::URL{"http://a.b/?a=b"};
    UNIT_ASSERT_STRINGS_EQUAL(
        addTileCoordinateParams(url, tile).toString(),
        "http://a.b/?a=b&x=100&y=100&z=10");
}

Y_UNIT_TEST(TileLoading)
{
    // We don't want check protobuf loading, only pipeline. Couple fields
    // should be enough.
    TileProto tileProto;
    tileProto.add_indoorplanid("some_field");
    const auto tileData = tileProto.SerializeAsString();
    const auto tileCompressedData = maps::zlibCompress(tileData);
    const auto tileCompressedString =
        std::string{tileCompressedData.begin(), tileCompressedData.end()};

    const auto mockHandle = maps::http::addMock(
        "http://a.b/?x=1&y=1&z=10",
        [&tileCompressedString](const maps::http::MockRequest&) {
            return maps::http::MockResponse(tileCompressedString);
        });

    const auto loadedTileProto = loadTileProto("http://a.b/?x=1&y=1&z=10");

    UNIT_ASSERT_EQUAL(loadedTileProto.indoorplanid().size(), 1);
    UNIT_ASSERT_STRINGS_EQUAL(loadedTileProto.indoorplanid(0), "some_field");
}

Y_UNIT_TEST(EmptyTileComparison)
{
    const auto comparisonResult = compareTiles(TileProto{}, TileProto{});

    UNIT_ASSERT(comparisonResult.attributesAreEqual);
    UNIT_ASSERT_EQUAL(
        comparisonResult.firstTileStatistics.attributesCount, 0);
    UNIT_ASSERT_EQUAL(
        comparisonResult.firstTileStatistics.pointsCount, 0);
    UNIT_ASSERT_EQUAL(
        comparisonResult.firstTileStatistics.polylinesCount, 0);
    UNIT_ASSERT_EQUAL(
        comparisonResult.firstTileStatistics.polygonsCount, 0);
    UNIT_ASSERT_EQUAL(
        comparisonResult.secondTileStatistics.attributesCount, 0);
    UNIT_ASSERT_EQUAL(
        comparisonResult.secondTileStatistics.pointsCount, 0);
    UNIT_ASSERT_EQUAL(
        comparisonResult.secondTileStatistics.polylinesCount, 0);
    UNIT_ASSERT_EQUAL(
        comparisonResult.secondTileStatistics.polygonsCount, 0);
}

Y_UNIT_TEST(TileComparison)
{
    const auto comparisonResult = compareTiles(
        createTileProto(0, 0, 0, {"key1", "key2"}, {"a1", "b1", "a2", "b2"}),
        createTileProto(1, 2, 3, {}, {}));

    UNIT_ASSERT(!comparisonResult.attributesAreEqual);
    UNIT_ASSERT_EQUAL(
        comparisonResult.firstTileStatistics.attributesCount, 2);
    UNIT_ASSERT_EQUAL(
        comparisonResult.firstTileStatistics.pointsCount, 0);
    UNIT_ASSERT_EQUAL(
        comparisonResult.firstTileStatistics.polylinesCount, 0);
    UNIT_ASSERT_EQUAL(
        comparisonResult.firstTileStatistics.polygonsCount, 0);
    UNIT_ASSERT_EQUAL(
        comparisonResult.secondTileStatistics.attributesCount, 0);
    UNIT_ASSERT_EQUAL(
        comparisonResult.secondTileStatistics.pointsCount, 1);
    UNIT_ASSERT_EQUAL(
        comparisonResult.secondTileStatistics.polylinesCount, 2);
    UNIT_ASSERT_EQUAL(
        comparisonResult.secondTileStatistics.polygonsCount, 3);
}

Y_UNIT_TEST(TileAttributesPermutation)
{
    auto comparisonResult = compareTiles(
        createTileProto(0, 2, 3, {"key1", "key2"}, {"a1", "b1", "a2", "b2"}),
        createTileProto(1, 2, 3, {"key2", "key1"}, {"b1", "a1", "b2", "a2"}));
    UNIT_ASSERT(comparisonResult.attributesAreEqual);

    comparisonResult = compareTiles(
        createTileProto(0, 2, 3, {"key1", "key2"}, {"a1", "b1", "a2", "b2"}),
        createTileProto(1, 2, 3, {"key2", "key1"}, {"b2", "a2", "b1", "a1"}));
    UNIT_ASSERT(comparisonResult.attributesAreEqual);

    comparisonResult = compareTiles(
        createTileProto(0, 2, 3, {"key1", "key2"}, {"a1", "b1", "a2", "b2"}),
        createTileProto(1, 2, 3, {"key2", "key1"}, {"b2", "a2", "a1", "b1"}));
    UNIT_ASSERT(!comparisonResult.attributesAreEqual);
}

Y_UNIT_TEST(TilesComparison)
{
    static const auto FIRST_TILES = std::map<maps::tile::Tile, TileProto> {
        {{1, 1, 15}, createTileProto(0, 0, 0, {}, {})},
        {{2, 1, 15}, createTileProto(0, 2, 0, {"key1"}, {"a1"})},
        {{1, 2, 15}, createTileProto(1, 2, 3, {"key2"}, {"a2"})},
        {{2, 2, 15}, createTileProto(2, 3, 4, {"key2"}, {"b2", "a2"})}};

    static const auto SECOND_TILES = std::map<maps::tile::Tile, TileProto> {
        {{1, 1, 15}, createTileProto(0, 0, 0, {}, {})},
        {{2, 1, 15}, createTileProto(0, 0, 0, {}, {})},
        {{1, 2, 15}, createTileProto(1, 2, 3, {"key1"}, {"a1"})},
        {{2, 2, 15}, createTileProto(1, 2, 3, {"key2"}, {"a2", "b2"})}};

    const auto comparisonResult = compareTiles(
        maps::tile::TileRange{{1, 1, 15}, {2, 2, 15}},
        [](const maps::tile::Tile tile){ return FIRST_TILES.at(tile); },
        [](const maps::tile::Tile tile){ return SECOND_TILES.at(tile); });

    UNIT_ASSERT_EQUAL(comparisonResult.tiles.size(), FIRST_TILES.size());
    UNIT_ASSERT_EQUAL(comparisonResult.differentTilesCount, 3);
    UNIT_ASSERT_EQUAL(comparisonResult.tilesCountWithDifferentFeatures, 2);
    UNIT_ASSERT_EQUAL(comparisonResult.tilesCountWithDifferentAttributes, 2);

    UNIT_ASSERT_EQUAL(
        comparisonResult.firstTilesTotalStatistics.attributesCount, 4);
    UNIT_ASSERT_EQUAL(
        comparisonResult.firstTilesTotalStatistics.pointsCount, 3);
    UNIT_ASSERT_EQUAL(
        comparisonResult.firstTilesTotalStatistics.polylinesCount, 7);
    UNIT_ASSERT_EQUAL(
        comparisonResult.firstTilesTotalStatistics.polygonsCount, 7);

    UNIT_ASSERT_EQUAL(
        comparisonResult.secondTilesTotalStatistics.attributesCount, 3);
    UNIT_ASSERT_EQUAL(
        comparisonResult.secondTilesTotalStatistics.pointsCount, 2);
    UNIT_ASSERT_EQUAL(
        comparisonResult.secondTilesTotalStatistics.polylinesCount, 4);
    UNIT_ASSERT_EQUAL(
        comparisonResult.secondTilesTotalStatistics.polygonsCount, 6);

    const auto pickedTiles = pickTilesWithMaxKey(
        comparisonResult.tiles, getTilesDiffSize, 0, 5);
    UNIT_ASSERT_EQUAL(pickedTiles.size(), 4);
    // Order of other tiles is arbitrary, but tile (1, 1, 15) must always be
    // last, because first and second tiles at (1, 1, 15) are equal.
    UNIT_ASSERT_UNEQUAL(pickedTiles.front(), maps::tile::Tile(1, 1, 15));
    UNIT_ASSERT_EQUAL(pickedTiles.back(), maps::tile::Tile(1, 1, 15));
}

Y_UNIT_TEST(TilesSorting)
{
    const auto COMPARED_TILES =
            std::map<maps::tile::Tile, TileComparisonResult> {
        {{1, 0, 10}, {.firstTileStatistics = {.attributesCount = 1}}},
        {{2, 0, 10}, {.firstTileStatistics = {.attributesCount = 0}}},
        {{3, 0, 10}, {.firstTileStatistics = {.attributesCount = 5}}},
        {{4, 0, 10}, {.firstTileStatistics = {.attributesCount = 2}}}
    };

    const auto sortedTiles = sortComparisonResult(COMPARED_TILES,
        [](const TileComparisonResult& t) {
            return t.firstTileStatistics.attributesCount;
        });

    UNIT_ASSERT_EQUAL(sortedTiles.size(), COMPARED_TILES.size());
    UNIT_ASSERT_EQUAL(sortedTiles[0].x(), 2);
    UNIT_ASSERT_EQUAL(sortedTiles[1].x(), 1);
    UNIT_ASSERT_EQUAL(sortedTiles[2].x(), 4);
    UNIT_ASSERT_EQUAL(sortedTiles[3].x(), 3);
}

Y_UNIT_TEST(RegionsLoading)
{
    const std::string REGIONS_STRING = R"([
    {
        "name": "moscow_1",
        "zoom": 15,
        "top_left": { "lon": 0, "lat": 1 },
        "bottom_right": { "lon": 2, "lat": 3 }
    },
    {
        "name": "moscow_2",
        "zoom": 16,
        "top_left": { "lon": -4, "lat": -5 },
        "bottom_right": { "lon": 6.5, "lat": 7 }
    }
])";

    std::istringstream iss(REGIONS_STRING);
    const auto regions = readRegions(maps::json::Value{iss});

    UNIT_ASSERT_EQUAL(regions.size(), 2);

    UNIT_ASSERT_STRINGS_EQUAL(regions[0].name, "moscow_1");
    UNIT_ASSERT_EQUAL(regions[0].zoom, 15);
    UNIT_ASSERT_DOUBLES_EQUAL(regions[0].topLeft.x(), 0., EPSILON);
    UNIT_ASSERT_DOUBLES_EQUAL(regions[0].topLeft.y(), 1., EPSILON);
    UNIT_ASSERT_DOUBLES_EQUAL(regions[0].bottomRight.x(), 2., EPSILON);
    UNIT_ASSERT_DOUBLES_EQUAL(regions[0].bottomRight.y(), 3., EPSILON);

    UNIT_ASSERT_STRINGS_EQUAL(regions[1].name, "moscow_2");
    UNIT_ASSERT_EQUAL(regions[1].zoom, 16);
    UNIT_ASSERT_DOUBLES_EQUAL(regions[1].topLeft.x(), -4., EPSILON);
    UNIT_ASSERT_DOUBLES_EQUAL(regions[1].topLeft.y(), -5., EPSILON);
    UNIT_ASSERT_DOUBLES_EQUAL(regions[1].bottomRight.x(), 6.5, EPSILON);
    UNIT_ASSERT_DOUBLES_EQUAL(regions[1].bottomRight.y(), 7, EPSILON);
}

}
