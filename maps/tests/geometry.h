#pragma once

#include <maps/indoor/libs/indoor_positioning/include/level_index.h>
#include <maps/indoor/libs/indoor_positioning/include/plan_level_id.h>
#include <maps/indoor/libs/indoor_positioning/include/transmitter.h>

namespace INDOOR_POSITIONING_NAMESPACE {

Polygon createPolygon(const std::vector<Point>& points);

MultiPolygon createMultiPolygon(const std::vector<Point>& points);

MultiPolygon createLevelGeometry(const Transmitters& transmitters);

std::shared_ptr<LevelIndex> createLevelIndex(const Transmitters& transmitters);

} // INDOOR_POSITIONING_NAMESPACE
