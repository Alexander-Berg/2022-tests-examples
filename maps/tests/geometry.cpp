#include <maps/indoor/libs/indoor_positioning/include/common.h>
#include <maps/indoor/libs/indoor_positioning/include/geometry.h>
#include <maps/indoor/libs/indoor_positioning/include/utils.h>

#include "geometry.h"

namespace INDOOR_POSITIONING_NAMESPACE {

Polygon createPolygon(const std::vector<Point>& points)
{
    Polygon polygon;
    for(const auto& p : points) {
        polygon.outer().push_back(p);
    }

    boost::geometry::correct(polygon);

    return polygon;
}

MultiPolygon createMultiPolygon(const std::vector<Point>& points)
{
    MultiPolygon multiPolygon;
    multiPolygon.push_back(createPolygon(points));
    return multiPolygon;
}

MultiPolygon createLevelGeometry(const Transmitters& transmitters)
{
    static const double radius = 10; // radius in meters

    MultiPolygon geom;

    for(const auto& tx : transmitters) {
        const auto poly = createPolygon({
            pointFromLocalMetricCS({+radius, +radius}, tx.position),
            pointFromLocalMetricCS({+radius, -radius}, tx.position),
            pointFromLocalMetricCS({-radius, -radius}, tx.position),
            pointFromLocalMetricCS({-radius, +radius}, tx.position),
            pointFromLocalMetricCS({+radius, +radius}, tx.position)});

        MultiPolygon result;
        boost::geometry::union_(geom, poly, result);
        std::swap(geom, result);
    }

    return geom;
}

std::shared_ptr<LevelIndex> createLevelIndex(const Transmitters& transmitters)
{
    std::shared_ptr<LevelIndex> levelIndex = std::make_shared<LevelIndex>(0);
    std::unordered_map<PlanLevelId, LevelTileData> levelTiles;

    for(const auto& tx : transmitters) {
        levelTiles[tx.planLevel].transmitters.push_back(tx);
    }

    for(auto& levelTile : levelTiles) {
        levelTile.second.geometry = createLevelGeometry(levelTile.second.transmitters);
    }

    for(const auto& levelTile : levelTiles) {
        levelIndex->updateLevel(levelTile.first, levelTile.second);
    }

    return levelIndex;
}

} // INDOOR_POSITIONING_NAMESPACE
