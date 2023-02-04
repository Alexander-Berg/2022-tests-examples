#include <maps/analyzer/libs/time_interpolator/include/iterative.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <vector>
#include <iterator>

using maps::analyzer::time_interpolator::DropSlow;
using maps::analyzer::time_interpolator::interpolateAllPrefixes;
using maps::analyzer::time_interpolator::interpolateIterative;
using maps::analyzer::time_interpolator::interpolateIterativeDropSlow;
using maps::analyzer::time_interpolator::interpolateIterativeDropSlowFilter;
using maps::analyzer::time_interpolator::TravelTime;
using maps::analyzer::time_interpolator::UNDEFINED_TRAVEL_TIME;
using maps::analyzer::time_interpolator::isValid;

constexpr double EPS = 1e-6;

// Simple iterative test
void runTest(
    double commonRatio,
    const std::vector<TravelTime>& data,
    TravelTime expected
) {
    const double actual = interpolateIterative(data.begin(), data.end(), commonRatio);
    EXPECT_NEAR(actual, expected, EPS);
}

// Simple interpolate prefixes test, also compares last output values
void runTest(
    double commonRatio,
    const std::vector<TravelTime>& data,
    std::size_t numberOfLastOutputValues,
    const std::vector<TravelTime>& expected
) {
    std::vector<TravelTime> actual;
    interpolateAllPrefixes(
        data.begin(), data.end(), commonRatio,
        std::back_inserter(actual), numberOfLastOutputValues
    );
    ASSERT_EQ(expected.size(), actual.size());
    for (std::size_t i = 0; i < expected.size(); ++i) {
        EXPECT_NEAR(actual[i], expected[i], EPS);
    }
}

// No drop-slow interpolation should be the same as default one
void runNoDropSlowTest(
    double commonRatio, const std::vector<TravelTime>& data
) {
    const double expected = interpolateIterative(data.begin(), data.end(), commonRatio);
    const double check = interpolateIterativeDropSlow(data.begin(), data.end(), commonRatio, {}, {});
    EXPECT_EQ(expected, check);
}

// Drop-slow interpolation test with expected result
void runDropSlowTest(
    double commonRatio, const std::optional<TravelTime>& baseTravelTime,
    DropSlow dropSlow,
    const std::vector<TravelTime>& data,
    TravelTime expected
) {
    const double actual = interpolateIterativeDropSlow(
        data.begin(), data.end(), commonRatio, baseTravelTime, dropSlow
    );
    if (!isValid(expected)) { EXPECT_FALSE(isValid(actual)); }
    else { EXPECT_NEAR(actual, expected, EPS); }
}

// Drop-slow interpolation test with expected filter result; expected value is computed with simple interpolating
// thus verifying same results of drop-slow result and simple interpolating on filtered output
void runDropSlowTest(
    double commonRatio, const std::optional<TravelTime>& baseTravelTime,
    DropSlow dropSlow,
    std::vector<TravelTime> data,
    const std::vector<TravelTime>& filteredExpected
) {
    auto&& [actual, filterEnd] = interpolateIterativeDropSlowFilter(
        data.begin(), data.end(), commonRatio, baseTravelTime, dropSlow, data.begin()
    );
    data.erase(filterEnd, data.end());

    const auto expected = interpolateIterative(
        filteredExpected.begin(), filteredExpected.end(), commonRatio
    );

    ASSERT_EQ(data.size(), filteredExpected.size());
    for (std::size_t i = 0; i < filteredExpected.size(); ++i) {
        EXPECT_NEAR(data[i], filteredExpected[i], EPS);
    }
    if (!isValid(expected)) { EXPECT_FALSE(isValid(actual)); }
    else { EXPECT_NEAR(actual, expected, EPS); }
}

// Drop-slow interpolation with expected filter result and expected exact result
void runDropSlowTest(
    double commonRatio, const std::optional<TravelTime>& baseTravelTime,
    DropSlow dropSlow,
    std::vector<TravelTime> data,
    const std::vector<TravelTime>& filteredExpected,
    TravelTime expected
) {
    auto&& [actual, filterEnd] = interpolateIterativeDropSlowFilter(
        data.begin(), data.end(), commonRatio, baseTravelTime, dropSlow, data.begin()
    );
    data.erase(filterEnd, data.end());

    ASSERT_EQ(data.size(), filteredExpected.size());
    for (std::size_t i = 0; i < filteredExpected.size(); ++i) {
        EXPECT_NEAR(data[i], filteredExpected[i], EPS);
    }
    if (!isValid(expected)) { EXPECT_FALSE(isValid(actual)); }
    else { EXPECT_NEAR(actual, expected, EPS); }
}

TEST(InterpolatorTests, IterativeTest) {
    runTest(1.0, { 10, 10, 10, 10 }, 10);
    runTest(1.0, { 10, 20 }, 15);
    runTest(0.6, { 10, 20 }, 16.25);
    runTest(0.25, { 10, 20 }, 18);
    runTest(0.6, { 10, 20, 30, 40 }, 30.955882352);
}

TEST(InterpolatorTests, InterpolateAllPrefixes) {
    runTest(1.0, {10.0, 10.0, 10.0}, 3, {10.0, 10.0, 10.0});
    runTest(1.0, {10, 20, 30, 40}, 4, {10.0, 15.0, 20.0, 25.0});
    runTest(1.0, {10, 20, 30, 40}, 2, {20.0, 25.0});
}

TEST(InterpolatorTests, IterativeNoDropSlowTest) {
    runNoDropSlowTest(0.9, {1., 2., 3., 100., 4.});
    runNoDropSlowTest(0.9, {1., 2., 3., 100., 50., 4.});
}

TEST(InterpolatorTests, IterativeDropSlowTest) {
    DropSlow cfg{.dropK = 10., .useK = 5.};

    runDropSlowTest(1.0, {}, cfg, {1., 2., 3., 100., 4.}, 10. / 4.);
    runDropSlowTest(1.0, {}, cfg, {1., 2., 3., 100., 50., 4.}, 160. / 6.);
}

TEST(InterpolatorTests, IterativeDropSlowNoBaseTravelTimeTest) {
    DropSlow cfg{.dropK = 10., .useK = 5., .useBaseTime = false};

    runDropSlowTest(1.0, {1.}, cfg, {100.}, 100.);
}

TEST(InterpolatorTests, IterativeDropSlowBaseTravelTimeTest) {
    DropSlow cfg{.dropK = 10., .useK = 5., .useBaseTime = true};

    runDropSlowTest(1.0, {1.}, cfg, {1., 2., 3., 100., 4.}, 10. / 4.);
    runDropSlowTest(1.0, {1.}, cfg, {1., 2., 3., 100., 50., 4.}, 160. / 6.);
    EXPECT_THROW(
        runDropSlowTest(1.0, {}, cfg, {100., 2., 3.}, 105. / 3.),
        maps::RuntimeError
    );
    runDropSlowTest(1.0, {1.}, cfg, {100., 2., 3.}, 5. / 2.);
    runDropSlowTest(1.0, {1.}, cfg, {}, UNDEFINED_TRAVEL_TIME);
}

TEST(InterpolatorTests, IterativeDropSlowFilterTest) {
    DropSlow cfg{.dropK = 10., .useK = 5., .useBaseTime = true};

    runDropSlowTest(0.5, {1.}, cfg, {1., 2., 3., 100., 4.}, {1., 2., 3., 4.});
    runDropSlowTest(0.5, {1.}, cfg, {1., 2., 3., 100., 50., 4., 1000.}, {1., 2., 3., 100., 50., 4.});
    runDropSlowTest(0.5, {1.}, cfg, {100., 1., 2., 3., 100., 50., 4., 1000.}, {1., 2., 3., 100., 50., 4.});
}

TEST(InterpolatorTests, IterativeDropSlowFilterTest2) {
    DropSlow cfg{.dropK = 10., .useK = 5., .useBaseTime = true};

    runDropSlowTest(1.0, {1.}, cfg, {1., 1., 100., 1., 1.}, {1., 1., 1., 1.}, 1.);
    runDropSlowTest(1.0, {1.}, cfg, {1., 1., 100., 100., 1., 1., 1000.}, {1., 1., 100., 100., 1., 1.}, 204. / 6.);
    runDropSlowTest(1.0, {1.}, cfg, {100., 1., 1., 100., 100., 1., 1., 1000.}, {1., 1., 100., 100., 1., 1.}, 204. / 6.);
}
