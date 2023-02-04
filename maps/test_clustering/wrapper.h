#pragma once

#include <string>
#include <vector>

namespace maps::routing::matrix_router {

std::vector<std::vector<float>> prepareSlicesWrapper(
    const std::string& routesTable,
    const std::string& timePartsTable,
    float defaultSpeed,
    size_t maxTestRoutesSize,
    double minRouteLength,
    double minRouteTravelTime);

}  // namespace maps::routing::matrix_router
