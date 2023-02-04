#include <maps/analyzer/libs/mapmatching_likelihoods/core/include/likelihoods.h>
#include <maps/analyzer/libs/mapmatching_likelihoods/core/tests/conf/likelihoods_config.h>
#include <maps/analyzer/libs/mapmatching_likelihoods/load/include/load.h>

#include <library/cpp/json/json_reader.h>
#include <util/stream/file.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>


using namespace maps::analyzer::mapmatching_likelihoods;

const std::string LIKELIHOODS_CONFIG_JSON = ArcadiaSourceRoot() +
    "/maps/analyzer/libs/mapmatching_likelihoods/load/tests/conf/likelihoods_conf.json";


TEST(LoadTest, LoadJson) {
    TFileInput jsonfile(LIKELIHOODS_CONFIG_JSON.c_str());
    const LikelihoodsConfig lhoodConfig = loadFromJson<LikelihoodsConfig>(NJson::ReadJsonTree(&jsonfile, true)["likelihoods"]);
    EXPECT_EQ(lhoodConfig, likelihoodsConfig);
}
