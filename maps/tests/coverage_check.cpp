#include "coverage_check.h"

#include <yandex/maps/coverage5/coverage.h>

#include <filesystem>

namespace maps::garden::modules::osm_borders_src::tests {

std::string getRegionMetaFromCoverage(
    double lon,
    double lat,
    const std::string& coverageFileName,
    const std::string& layerName
) {
    ASSERT(std::filesystem::is_regular_file(coverageFileName));

    const coverage5::Coverage coverage(coverageFileName);
    const coverage5::Layer& layer = coverage[layerName];

    coverage5::Regions regions = layer.regions(geolib3::Point2(lon, lat), boost::none);
    if (regions.empty()) {
        return {};
    };
    return regions.front().metaData();
}

} // maps::garden::modules::osm_borders_src::tests
