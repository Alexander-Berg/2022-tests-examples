#include <maps/analyzer/services/jams_analyzer/modules/dispatcher/lib/config.h>

#include <library/cpp/testing/gtest/gtest.h>

TEST(ObservedRegionsTest, MissingFileReadTest) {
    const std::string filepath{};

    EXPECT_THROW(
        auto _ = ObservedRegions(filepath),
        maps::Exception
    );
}

TEST(ObservedRegionsTest, BadFileReadTest) {
    const std::string filepath{"bad_observed_regions.txt"};

    EXPECT_THROW(
        auto _ = ObservedRegions(filepath),
        maps::Exception
    );
}

TEST(ObservedRegionsTest, GoodFileReadTest) {
    const std::string filepath{"dummy_observed_regions.txt"};

    ObservedRegions observedRegions(filepath);

    EXPECT_EQ(observedRegions.size(), 2ul);
    EXPECT_FALSE(observedRegions.empty());

    const auto metricNamesOpt = observedRegions.getRegionMetricNames(213);
    ASSERT_TRUE(metricNamesOpt.has_value());
    const auto& metricNames = metricNamesOpt->get();
    EXPECT_EQ(metricNames.total, "total-region-Moscow-213-signals-count");
    EXPECT_EQ(metricNames.good, "good-region-Moscow-213-signals-count");
    EXPECT_EQ(metricNames.filtered, "filtered-region-Moscow-213-signals-count");

    EXPECT_FALSE(observedRegions.getRegionMetricNames(0).has_value());
}
