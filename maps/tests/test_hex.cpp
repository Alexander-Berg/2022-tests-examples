#include <maps/factory/libs/common/hex.h>

#include <maps/factory/libs/common/eigen.h>

#include <maps/libs/common/include/hex.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::tests {

Y_UNIT_TEST_SUITE(hex_should) {

Y_UNIT_TEST(decode_empty_data)
{
    EXPECT_EQ(hexDecodeUnchecked(""), "");
}

Y_UNIT_TEST(decode_random_data_lowercase)
{
    for (int i = 1; i < 1000; i *= 2) {
        Eigen::VectorXd data = Eigen::VectorXd::Random(i);
        TArrayRef<const uint8_t> arr(
            reinterpret_cast<const uint8_t*>(data.data()), data.size() * sizeof(double));
        const std::string hex = hexEncodeLowercase(arr);
        const std::string result = hexDecodeUnchecked(hex);
        TArrayRef<const uint8_t> resultArr(
            reinterpret_cast<const uint8_t*>(result.data()), result.size());
        EXPECT_EQ(arr, resultArr);
    }
}

Y_UNIT_TEST(decode_random_data_uppercase)
{
    for (int i = 1; i < 1000; i *= 2) {
        Eigen::VectorXd data = Eigen::VectorXd::Random(i);
        TArrayRef<const uint8_t> arr(
            reinterpret_cast<const uint8_t*>(data.data()), data.size() * sizeof(double));
        const std::string hex = hexEncodeUppercase(arr);
        const std::string result = hexDecodeUnchecked(hex);
        TArrayRef<const uint8_t> resultArr(
            reinterpret_cast<const uint8_t*>(result.data()), result.size());
        EXPECT_EQ(arr, resultArr);
    }
}

} // suite

} // namespace maps::factory::tests
