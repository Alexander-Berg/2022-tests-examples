#include "hydro_test.h"

#include <maps/libs/log8/include/log8.h>
#include <maps/libs/geolib/include/algorithm.h>

namespace maps::renderer::check {

namespace {

namespace basemap = yandex::maps::proto::renderer::layers::basemap;

bool hasWaterTag(const std::vector<basemap::Tag>& tags)
{
    return std::find(tags.begin(), tags.end(), basemap::Tag::WATER) != tags.end();
}

std::vector<geolib3::Polygon2>
getWaterPolygons(const TileData& tile)
{
    std::vector<geolib3::Polygon2> waterPolygons;
    for (const auto& p : tile.polygonFeatures()) {
        if (hasWaterTag(p.tags))
            waterPolygons.push_back(p.geom.get<geolib3::Polygon2>());
    }

    return waterPolygons;
}

float getUnionArea(const std::vector<geolib3::Polygon2>& polygons)
{
    double area;
    if (polygons.size() == 1)
        area = polygons.front().area();
    else
        area = geolib3::unitePolygons(polygons).area();

    return float(area);
}

float TILE_AREA = 32767 * 32767;

} // namespace

TileError HydroTest::testTile(const TileData& testTile,
                              const TileData& goldenTile)
{
    auto testWaterArea = getUnionArea(getWaterPolygons(testTile));
    auto goldenWaterArea = getUnionArea(getWaterPolygons(goldenTile));

    return {fabs(testWaterArea - goldenWaterArea) / TILE_AREA, ""};
}

std::unique_ptr<IMapTest> HydroTestCreator::getTest(json::ValueRef /*testConfig*/,
                                                   const TileLoaders& tileLoader) const
{
    return convert(std::make_unique<HydroTest>(),
                   tileLoader,
                   "ru_RU",
                   10);
}

} // namespace maps::renderer::check
