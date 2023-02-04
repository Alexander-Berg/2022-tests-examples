#include <maps/factory/libs/sproto_helpers/dg_delivery.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::sproto_helpers::tests {

const std::string TEST_ORDER_NO = "059490188170_01";
const std::string TEST_AOI = "Moscow";
const int64_t TEST_ORDER_ID = 42;
const std::string TEST_ERROR = "Some error";

TEST(test_convert_dg_delivery, test_convert_to_sproto)
{
    auto dl = db::DgDelivery{TEST_ORDER_NO, "./"};
    dl.setAreaDescription(TEST_AOI);
    dl.setOrderId(TEST_ORDER_ID);
    const auto sdl = convertToSproto(dl);
    EXPECT_EQ(sdl.orderNumber(), TEST_ORDER_NO);
    EXPECT_EQ(sdl.areaDescription(), TEST_AOI);
    EXPECT_EQ(sdl.orderId(), TEST_ORDER_ID);
    EXPECT_FALSE(sdl.errorMessage());
}

TEST(test_convert_dg_delivery, test_convert_to_sproto_with_error)
{
    auto dl = db::DgDelivery{TEST_ORDER_NO, "./"};
    dl.setAreaDescription(TEST_AOI);
    dl.setOrderId(TEST_ORDER_ID);
    dl.addErrorMessage(TEST_ERROR);
    const auto sdl = convertToSproto(dl);
    EXPECT_EQ(sdl.orderNumber(), TEST_ORDER_NO);
    EXPECT_EQ(sdl.areaDescription(), TEST_AOI);
    EXPECT_EQ(sdl.orderId(), TEST_ORDER_ID);
    EXPECT_TRUE(sdl.errorMessage());
    EXPECT_EQ(*sdl.errorMessage(), TEST_ERROR + ";");
}

TEST(test_convert_dg_delivery, test_convert_vector)
{
    auto dl = db::DgDelivery{TEST_ORDER_NO, "./"};
    dl.setAreaDescription(TEST_AOI);
    dl.setOrderId(TEST_ORDER_ID);
    const auto dls = std::vector<db::DgDelivery>{dl};
    const auto sdls = convertToSproto(dls);
    ASSERT_EQ(sdls.deliveries().size(), 1u);
    EXPECT_EQ(sdls.deliveries()[0].orderNumber(), TEST_ORDER_NO);
    EXPECT_EQ(sdls.deliveries()[0].areaDescription(), TEST_AOI);
    EXPECT_EQ(sdls.deliveries()[0].orderId(), TEST_ORDER_ID);
    EXPECT_FALSE(sdls.deliveries()[0].errorMessage());
}

TEST(test_convert_dg_delivery, test_update)
{
    auto dl = db::DgDelivery{TEST_ORDER_NO, "./"};
    dl.setAreaDescription("Other AOI");
    dl.setOrderId(123);
    auto sdld = sfactory::DgDeliveryData{};
    sdld.areaDescription() = TEST_AOI;
    sdld.orderId() = TEST_ORDER_ID;
    updateFromSproto(dl, sdld);
    EXPECT_EQ(dl.orderNumber(), TEST_ORDER_NO);
    EXPECT_EQ(dl.areaDescription(), TEST_AOI);
    EXPECT_EQ(dl.orderId(), TEST_ORDER_ID);
    EXPECT_FALSE(dl.hasError());
}

} // namespace maps::factory::sproto_helpers::tests
