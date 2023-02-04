#include <maps/libs/common/include/hmac.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::tests {

constexpr std::string_view KEY = "key";
constexpr std::array<uint8_t, 3> ARRAY_KEY = {'k', 'e', 'y'};

TEST(test_hmac, test_hmac_hex) {
    //NB: one can validate this data using e. g. https://cryptii.com/pipes/hmac
    EXPECT_EQ(
        computeHmacMD5Hex(KEY, "data"),
        "9d5c73ef85594d34ec4438b7c97e51d8"
    );

    EXPECT_EQ(
        computeHmacMD5Hex(ARRAY_KEY, "data"),
        "9d5c73ef85594d34ec4438b7c97e51d8"
    );

    EXPECT_EQ(
        computeHmacSha256Hex(KEY, "data"),
        "5031fe3d989c6d1537a013fa6e739da23463fdaec3b70137d828e36ace221bd0"
    );
    EXPECT_EQ(
        computeHmacSha256Hex(ARRAY_KEY, "data"),
        "5031fe3d989c6d1537a013fa6e739da23463fdaec3b70137d828e36ace221bd0"
    );
}

} //namespace maps::tests
