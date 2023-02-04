#include <maps/analyzer/libs/exp_regions/include/query_parsing.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace er = maps::analyzer::exp_regions;

TEST(QueryParsing, Parsing) {
    const std::vector<er::ExperimentDescription> descriptions =
        er::extractExperimentDescriptions(
            "/v2/route?"
            "experimental_regions_123=abc&"
            "experimental_regions_456=def"
        );

    ASSERT_EQ(descriptions.size(), 2u);
    EXPECT_EQ(descriptions.at(0).testId, 123u);
    EXPECT_EQ(descriptions.at(0).cypressFilename, "abc");
    EXPECT_EQ(descriptions.at(1).testId, 456u);
    EXPECT_EQ(descriptions.at(1).cypressFilename, "def");

    const std::string queries[] = {
        "/v2/route",
        "/v2/route?experimental_regions_123_some_rubbish=aaa",
        "/v2/route?experimental_regions_some_rubbish=aaa",
        "/v2/route?experimental_regions=aaa"
    };

    for (const std::string& query : queries) {
        EXPECT_TRUE(er::extractExperimentDescriptions(query).empty());
    }
}
