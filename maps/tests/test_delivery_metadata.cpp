#include <maps/factory/libs/delivery/metadata.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::delivery::tests {
using namespace testing;
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(deliverymetadata_should) {

constexpr auto productJsonStr = R"({
"area_description":"Kahramanmaras_AC",
"azim_angle":"141.800000",
"clouds":"0.100000",
"datetime":"2018-09-10 08:29:42",
"dg_order_item_no":"058800151040",
"dg_order_no":"058800151",
"elev":"74.300000",
"err_bias":"1.180000",
"err_rand":"0.220000",
"image_id":"058800151040_01_P001",
"rpc_d_height_pix":"0.563225",
"satellite":"WV02",
"strip_id":"10300100855F3700",
"sun_azum":"153.600000",
"sun_elev":"54.800000",
"terrain_hae":"562.210000",
"log":"something;"
})";

ProductMetadata product()
{
    ProductMetadata m;
    m.clouds = 0.1;
    m.datetime = chrono::parseIsoDateTime("2018-09-10T08:29:42Z");
    m.imageId = "058800151040_01_P001";
    m.stripId = "10300100855F3700";
    m.sunAzum = 153.6;
    m.sunElev = 54.8;
    m.azimAngle = 141.8;
    m.elev = 74.3;
    m.satellite = "WV02";
    m.errBias = 1.18;
    m.errRand = 0.22;
    m.dgOrderNo = "058800151";
    m.dgOrderItemNo = "058800151040";
    m.areaDescription = "Kahramanmaras_AC";
    m.terrainHae = 562.21;
    m.rpcDHeightPix = 0.563225;
    m.log = "something;";
    return m;
}

void checkProduct(const ProductMetadata& m)
{
    const ProductMetadata expected = product();
    EXPECT_DOUBLE_EQ(m.clouds, expected.clouds);
    EXPECT_EQ(m.datetime, expected.datetime);
    EXPECT_EQ(m.imageId, expected.imageId);
    EXPECT_EQ(m.stripId, expected.stripId);
    EXPECT_DOUBLE_EQ(m.sunAzum, expected.sunAzum);
    EXPECT_DOUBLE_EQ(m.sunElev, expected.sunElev);
    EXPECT_DOUBLE_EQ(m.azimAngle, expected.azimAngle);
    EXPECT_DOUBLE_EQ(m.elev, expected.elev);
    EXPECT_EQ(m.satellite, expected.satellite);
    EXPECT_DOUBLE_EQ(m.errBias, expected.errBias);
    EXPECT_DOUBLE_EQ(m.errRand, expected.errRand);
    EXPECT_EQ(m.dgOrderNo, expected.dgOrderNo);
    EXPECT_EQ(m.dgOrderItemNo, expected.dgOrderItemNo);
    EXPECT_EQ(m.areaDescription, expected.areaDescription);
    EXPECT_DOUBLE_EQ(m.terrainHae, expected.terrainHae);
    EXPECT_DOUBLE_EQ(m.rpcDHeightPix, expected.rpcDHeightPix);
    EXPECT_EQ(m.log, expected.log);
}

void checkProduct(const json::Value& val)
{
    EXPECT_EQ(val["clouds"].as<std::string>(), "0.100000");
    EXPECT_EQ(val["datetime"].as<std::string>(), "2018-09-10 08:29:42");
    EXPECT_EQ(val["image_id"].as<std::string>(), "058800151040_01_P001");
    EXPECT_EQ(val["strip_id"].as<std::string>(), "10300100855F3700");
    EXPECT_EQ(val["sun_azum"].as<std::string>(), "153.600000");
    EXPECT_EQ(val["sun_elev"].as<std::string>(), "54.800000");
    EXPECT_EQ(val["azim_angle"].as<std::string>(), "141.800000");
    EXPECT_EQ(val["elev"].as<std::string>(), "74.300000");
    EXPECT_EQ(val["satellite"].as<std::string>(), "WV02");
    EXPECT_EQ(val["err_bias"].as<std::string>(), "1.180000");
    EXPECT_EQ(val["err_rand"].as<std::string>(), "0.220000");
    EXPECT_EQ(val["dg_order_no"].as<std::string>(), "058800151");
    EXPECT_EQ(val["dg_order_item_no"].as<std::string>(), "058800151040");
    EXPECT_EQ(val["area_description"].as<std::string>(), "Kahramanmaras_AC");
    EXPECT_EQ(val["terrain_hae"].as<std::string>(), "562.210000");
    EXPECT_EQ(val["rpc_d_height_pix"].as<std::string>(), "0.563225");
    EXPECT_EQ(val["log"].as<std::string>(), "something;");
}

Y_UNIT_TEST(init_product_from_json)
{
    const json::Value val = json::Value::fromString(productJsonStr);
    const ProductMetadata m(val);
    checkProduct(m);
}

Y_UNIT_TEST(make_json_for_product)
{
    const ProductMetadata m = product();
    const json::Value val = m.toJson();
    checkProduct(val);
}

} //suite
} //namespace maps::factory::delivery::tests
