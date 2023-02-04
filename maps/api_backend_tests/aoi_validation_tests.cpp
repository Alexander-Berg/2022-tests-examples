
#include <maps/factory/services/sputnica_back/lib/aoi_validation.h>
#include <maps/factory/libs/db/order.h>

#include <maps/libs/introspection/include/comparison.h>
#include <maps/libs/introspection/include/stream_output.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::factory::sputnica::tests {

Y_UNIT_TEST_SUITE(aoi_validation_should)
{

Y_UNIT_TEST(geometry_validation)
{
    backend::GeometryValidationPolicy policy{
        .minSegmentLengthKm = 5.,
        .minAreaSqrKm = 100,
        .minVerticesNumber = 4,
        .maxVerticesNumber = 999
    };

    {
        auto errors =
            backend::validate(geolib3::Polygon2({{0, 0}, {0.044632, 0}, {0.044632, 1}, {0, 1}}), policy);
        EXPECT_EQ(errors.size(), 2u);
        auto& error = errors.at(0);
        EXPECT_EQ(error.metric(), db::AoiValidationMetric::SegmentLength);
        EXPECT_FLOAT_EQ(error.value(), 4.9684114);
        EXPECT_TRUE(error.minAllowedValue().has_value());
        EXPECT_FLOAT_EQ(error.minAllowedValue().value(), policy.minSegmentLengthKm);
        EXPECT_TRUE(error.outerRingFailedSegmentIndex().has_value());
        EXPECT_EQ(error.outerRingFailedSegmentIndex().value(), 0);
    }

    {
        auto errors =
            backend::validate(geolib3::Polygon2({{0, 0}, {0.086174, 0}, {0.086174, 0.086174}, {0, 0.086174}}),
                policy);
        EXPECT_EQ(errors.size(), 1u);
        auto& error = errors.at(0);
        EXPECT_EQ(error.metric(), db::AoiValidationMetric::Area);
        EXPECT_FLOAT_EQ(error.value(), 91.406639);
        EXPECT_TRUE(error.minAllowedValue().has_value());
        EXPECT_FLOAT_EQ(error.minAllowedValue().value(), policy.minAreaSqrKm);
    }

    {
        auto errors = backend::validate(geolib3::Polygon2({{0, 0}, {1, 0}, {1, 1}}), policy);
        EXPECT_EQ(errors.size(), 1u);
        auto& error = errors.at(0);
        EXPECT_EQ(error.metric(), db::AoiValidationMetric::VerticesNumber);
        EXPECT_FLOAT_EQ(error.value(), 3);
        EXPECT_TRUE(error.minAllowedValue().has_value());
        EXPECT_FLOAT_EQ(error.minAllowedValue().value(), policy.minVerticesNumber);
    }
}

Y_UNIT_TEST(name_validation)
{
    EXPECT_THAT(backend::findAllForbiddenSymbolSequences(""), ::testing::ElementsAre());
    EXPECT_THAT(backend::findAllForbiddenSymbolSequences("Abc_123_Ts"), ::testing::ElementsAre());
    EXPECT_THAT(backend::findAllForbiddenSymbolSequences("Abc 123  Ts"),
        ::testing::ElementsAre(std::make_pair(3u, 3u), std::make_pair(7u, 8u))
    );
    EXPECT_THAT(backend::findAllForbiddenSymbolSequences("  "),
        ::testing::ElementsAre(std::make_pair(0u, 1u))
    );
    EXPECT_THAT(backend::findAllForbiddenSymbolSequences("AbcМосква"),
        ::testing::ElementsAre(std::make_pair(3u, 8u))
    );
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::factory::sputnica::tests
