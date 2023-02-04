#include <string>


namespace maps::garden::modules::osm_borders_src::tests {

std::string getRegionMetaFromCoverage(
    double lon,
    double lat,
    const std::string& coverageFileName,
    const std::string& layerName
);

} // maps::garden::modules::osm_borders_src::tests
