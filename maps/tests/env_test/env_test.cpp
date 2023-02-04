#define BOOST_TEST_MAIN

#include <maps/analyzer/libs/common/include/env.h>
#include <maps/libs/common/include/exception.h>
#include <library/cpp/testing/common/env.h>

#include <boost/test/unit_test.hpp>
#include <fstream>

namespace ma = maps::analyzer;

const auto DATA_ROOT = ArcadiaSourceRoot() + "/maps/analyzer/libs/common/tests/env_test";

std::string properConfigContent(int testId) {
    const auto testIdAsString = std::to_string(testId);
    const auto configPathPrefix = DATA_ROOT + "/config_" + testIdAsString;
    const auto environmentNamePath = DATA_ROOT + "/environment_" + testIdAsString + ".name";
    const auto environmentTypePath = DATA_ROOT + "/environment_" + testIdAsString + ".type";

    const auto configPath =
        ma::properConfigPath(configPathPrefix, environmentNamePath, environmentTypePath);

    std::ifstream config(configPath);
    if (!config.good()) {
        throw std::runtime_error("The config \"" + configPath + "\" was opened with errors");
    }
    std::string configContent;
    config >> configContent;
    return configContent;
}

BOOST_AUTO_TEST_CASE(environment) {
    auto environment = ma::environment(DATA_ROOT + "/environment_1.name",
                                       DATA_ROOT + "/environment_1.type");
    BOOST_CHECK_EQUAL("stress", environment);

    environment = ma::environment(DATA_ROOT + "/environment_2.name",
                                  DATA_ROOT + "/environment_2.type");
    BOOST_CHECK_EQUAL("testing", environment);
}

BOOST_AUTO_TEST_CASE(properConfigPath) {
    BOOST_CHECK_EQUAL(properConfigContent(3), "config_3.stress");
    BOOST_CHECK_EQUAL(properConfigContent(4), "config_4.testing");
    BOOST_CHECK_EQUAL(properConfigContent(5), "config_5.default");
    BOOST_CHECK_THROW(properConfigContent(6), maps::RuntimeError);
}
