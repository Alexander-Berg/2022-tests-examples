#include <maps/indoor/long-tasks/src/radiomap-metrics/lib/impl.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::mirc::radiomap_metrics::impl::test {

using namespace indoor::positioning_estimator;

TEST(MergePercentiles, Throw)
{
    const std::map<AssignmentId, Percentiles> differentPercentsData{
        {AssignmentId{11}, Percentiles{{10, 11.0}, {20, 33.0}}},
        {AssignmentId{17}, Percentiles{{20, 11.0}, {30, 22.0}}}
    };

    ASSERT_THROW(merge(differentPercentsData), maps::DataValidationError);
    ASSERT_THROW(merge({}), maps::DataValidationError);
    ASSERT_THROW(merge({{AssignmentId{42}, Percentiles{}}}), maps::DataValidationError);
}

TEST(MergePercentiles, SingleAssignment)
{
    const std::map<AssignmentId, Percentiles> toMerge{
        {AssignmentId{11}, Percentiles{{50, 11}, {90, 33}, {95, {}}}}
    };

    const auto merged = merge(toMerge);
    ASSERT_EQ(merged.size(), 3u);
    EXPECT_DOUBLE_EQ(merged.at(50).value(), 11.0);
    EXPECT_DOUBLE_EQ(merged.at(90).value(), 33.0);
    EXPECT_FALSE(merged.at(95).has_value());
}

TEST(MergePercentiles, OrdinaryInputWithAllValues)
{
    const std::map<AssignmentId, Percentiles> toMerge{
        {AssignmentId{11}, Percentiles{{50, 11}, {90, 33}}},
        {AssignmentId{17}, Percentiles{{50, 22}, {90, 22}}},
        {AssignmentId{22}, Percentiles{{50, 33}, {90, 11}}}
    };

    const auto merged = merge(toMerge);
    ASSERT_EQ(merged.size(), 2u);
    EXPECT_DOUBLE_EQ(merged.at(50).value(), (33.0 * 3 + 22 * 2 + 11 * 1) / (1 + 2 + 3));
    EXPECT_DOUBLE_EQ(merged.at(90).value(), (11.0 * 3 + 22 * 2 + 33 * 1) / (1 + 2 + 3));
}

TEST(MergePercentiles, SomeNullData)
{
    const std::map<AssignmentId, Percentiles> toMerge{
        {AssignmentId{11},  Percentiles{{50, {}}, {90, 11}}},
        {AssignmentId{17},  Percentiles{{50, 22}, {90, {}}}},
        {AssignmentId{22},  Percentiles{{50, {}}, {90, 33}}},
        {AssignmentId{222}, Percentiles{{50, 44}, {90, {}}}}
    };

    const auto merged = merge(toMerge);
    ASSERT_EQ(merged.size(), 2u);
    EXPECT_DOUBLE_EQ(
        merged.at(50).value(),
        (44.0 * 4 + 22 * 2) / (4 + 2)
    );
    EXPECT_DOUBLE_EQ(
        merged.at(90).value(),
        (33.0 * 3 + 11 * 1) / (3 + 1)
    );
}

} // namespace maps::mirc::radiomap_metrics::impl::test
