#include <maps/indoor/libs/radiomap_metrics/optimal_location_provider.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::indoor::positioning_estimator::tests {

TEST(ChooseBestProvider, CoeffProviderConversion) {
    for (auto&& provider : enum_io::enumerateValues<LocationProvider>()) {
        EXPECT_EQ(provider, getLocationProvider(getFuseCoefficient(provider)));
    }
}

TEST(ChooseBestProvider, Empty) {

    const PercentilesByProvider empty;
    ASSERT_THROW_MESSAGE_HAS_SUBSTR(
        getOptimalProvider(empty),
        maps::RuntimeError,
        "Can't choose the best out of nothing"
    );
}

TEST(ChooseBestProvider, SingleProvider) {
    const auto percentilesByProvider = [] {
        return PercentilesByProvider{
            {
                LocationProvider::Fused01,
                Percentiles{ { 50, 1.0 }, { 75, 2.0 }, { 95, 3.0 } }
            }
        };
    }();

    const auto optimal = getOptimalProvider(percentilesByProvider);
    ASSERT_TRUE(optimal.has_value());

    EXPECT_EQ(
        optimal.value(),
        LocationProvider::Fused01
    );
}

TEST(ChooseBestProvider, ManyProviders) {
    const auto percentilesByProvider = [] {
        return PercentilesByProvider{
            {
                LocationProvider::Indoor,
                Percentiles{ { 50, 111.0 }, { 75, 222.0 }, { 95, 333.0 } }
            },
            {
                LocationProvider::Fused01,
                Percentiles{ { 50, 11.0 }, { 75, 22.0 }, { 95, 33.0 } }
            },
            {
                LocationProvider::Fused09,
                Percentiles{ { 50, 1.0 }, { 75, 2.0 }, { 95, 3.0 } }
            }
        };
    }();

    const auto optimal = getOptimalProvider(percentilesByProvider);
    ASSERT_TRUE(optimal.has_value());

    EXPECT_EQ(
        optimal.value(),
        LocationProvider::Fused09
    );
}

TEST(ChooseBestProvider, ManyProvidersWithNans) {
    const auto percentilesByProvider = [] {
        return PercentilesByProvider{
            {
                LocationProvider::Indoor,
                Percentiles{ { 50, {} }, { 75, {} }, { 95, {} } }
            },
            {
                LocationProvider::Fused01,
                Percentiles{ { 50, 11.0 }, { 75, {} }, { 95, {} } }
            },
            {
                LocationProvider::Fused09,
                Percentiles{ { 50, 111.0 }, { 75, 222.0 }, { 95, {} } }
            }
        };
    }();

    const auto optimal = getOptimalProvider(percentilesByProvider);
    ASSERT_TRUE(optimal.has_value());

    EXPECT_EQ(
        optimal.value(),
        LocationProvider::Fused09
    );
}

TEST(ChooseBestProvider, NoOptimal) {
    const auto percentilesByProvider = [] {
        return PercentilesByProvider{
            {
                LocationProvider::Indoor,
                Percentiles{ { 50, {} }, { 75, {} }, { 95, {} } }
            },
            {
                LocationProvider::Fused01,
                Percentiles{ { 50, {} }, { 75, {} }, { 95, {} } }
            },
            {
                LocationProvider::Fused09,
                Percentiles{ { 50, {} }, { 75, {} }, { 95, {} } }
            }
        };
    }();

    const auto optimal = getOptimalProvider(percentilesByProvider);
    ASSERT_FALSE(optimal.has_value());
}

TEST(ChooseBestProvider, NonRectangularData) {
    const auto percentilesByProvider = [] {
        return PercentilesByProvider{
            {
                LocationProvider::Indoor,
                Percentiles{ { 50, 1 }, { 70, 2 }, { 90, 3 } }
            },
            {
                LocationProvider::Fused01,
                Percentiles{ { 51, 1 }, { 71, 2 }, { 91, 3 } }
            },
            {
                LocationProvider::Fused09,
                Percentiles{ { 52, 1 }, { 72, 2 }, { 92, 3 } }
            }
        };
    }();

    ASSERT_THROW_MESSAGE_HAS_SUBSTR(
        getOptimalProvider(percentilesByProvider),
        DataValidationError,
        "Expected rectangular data"
    );
}

} // namespace maps::indoor::positioning_estimator::tests
