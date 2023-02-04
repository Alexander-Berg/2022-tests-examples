#include <maps/factory/libs/rendering/mosaic_parameters.h>

#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/unittest/fixture.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::factory::rendering::tests {
namespace {
const geolib3::MultiPolygon2 GEOMETRY({
    geolib3::Polygon2(
        geolib3::PointsVector{
            {0, 0},
            {0, 5},
            {5, 5},
            {5, 0}
        }
    )
});

const json::Value RENDERING_PARAMS{
    {"hue", json::Value{90}},
    {"saturation", json::Value{50}},
    {"lightness", json::Value{50}},
    {"sigma", json::Value{0.5}},
    {"radius", json::Value{0.9}}
};
} // namespace

Y_UNIT_TEST_SUITE(mosaic_parameters_should)
{
Y_UNIT_TEST(set_and_load_rendering_parameters)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());

    db::MosaicSource source("test_source");
    {
        pqxx::work txn(conn);
        db::MosaicSourceGateway(txn).insert(source);
    }
    db::Mosaic mosaic(source.id(), 1, 10, 19, GEOMETRY);
    mosaic.setRenderingParams(RENDERING_PARAMS);

    pqxx::work txn(conn);
    auto parameters = parametersForMosaic(mosaic);
    EXPECT_TRUE(parameters.get(parameter::COLOR_HUE));
    EXPECT_TRUE(parameters.get(parameter::COLOR_SATURATION));
    EXPECT_TRUE(parameters.get(parameter::COLOR_LIGHTNESS));
    EXPECT_TRUE(parameters.get(parameter::UNSHARP_SIGMA));
    EXPECT_TRUE(parameters.get(parameter::UNSHARP_RADIUS));

    double hue, saturation, lightness, sigma, radius;
    parameters.get(parameter::COLOR_HUE, hue);
    parameters.get(parameter::COLOR_SATURATION, saturation);
    parameters.get(parameter::COLOR_LIGHTNESS, lightness);
    parameters.get(parameter::UNSHARP_SIGMA, sigma);
    parameters.get(parameter::UNSHARP_RADIUS, radius);

    EXPECT_EQ(hue, 0.5);
    EXPECT_EQ(saturation, 0.5);
    EXPECT_EQ(lightness, 0.5);
    EXPECT_EQ(sigma, 0.5);
    EXPECT_EQ(radius, 0.9);
}

} // suite
} // namespace maps::factory::rendering::tests
