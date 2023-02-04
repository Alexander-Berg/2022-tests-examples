#ifndef MOBILE_BUILD

#include <maps/analyzer/libs/guidance/include/version.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <regex>
#include <fstream>
#include <string>

const std::string DEBIAN_CHANGELOG = ArcadiaSourceRoot() + "/maps/analyzer/libs/guidance/changelog";
const std::regex VERSION_RX("\\((\\d+\\.\\d+\\.\\d+\\-\\d+)\\)");

Y_UNIT_TEST_SUITE(VersionTests) {
    Y_UNIT_TEST(VersionTest) {
        std::ifstream changelog(DEBIAN_CHANGELOG);
        std::string header;
        std::getline(changelog, header);

        std::smatch matched;
        const bool foundVersion = std::regex_search(header, matched, VERSION_RX);
        EXPECT_TRUE(foundVersion);

        const std::string matchedVersion = matched[1];
        EXPECT_EQ(matchedVersion, maps::analyzer::guidance::VERSION);
    }
}

#endif
