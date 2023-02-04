#pragma once

#include <string>

namespace maps::garden::modules::osm_coastlines_src::test {

void convertGeoJsonToSHP(const std::string& inFilePath, const std::string& outFilePath);

}
