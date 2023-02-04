#include <maps/analyzer/libs/geoinfo/include/geoid.h>
#include <maps/libs/xml/include/xml.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <string>
#include <sstream>
#include <filesystem>


const std::string CONFIGS_ROOT = static_cast<std::string>(ArcadiaSourceRoot() + "/maps/analyzer/services/jams_analyzer/modules/dispatcher/config");


TEST(ConfigRegionTest, RegionsExistsInGeoId) {
    maps::geoinfo::GeoId geoId{std::filesystem::path("./geoid.mms.1")};

    for (auto& p: std::filesystem::directory_iterator(CONFIGS_ROOT)) {
        if (p.path().stem() == "dispatcher.conf") {
            maps::xml3::Doc xml(p.path().generic_string());
            auto nodes = xml.nodes("//regions", /* quiet = */ true);
            for (std::size_t i = 0; i < nodes.size(); ++i) {
                auto node = nodes[i];
                std::istringstream istr(node.value<std::string>());
                maps::coverage5::RegionId regionId;
                while (istr >> regionId) {
                    auto region = geoId.layer().getRegion(regionId);
                    EXPECT_TRUE(region.id());
                }
            }
        }
    }
}
