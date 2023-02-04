#include <maps/indoor/long-tasks/src/pg-dumper/lib/dump_rotation.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <chrono>
#include <string>

namespace maps::mirc::pg_dumper::tests {

TEST(GetDateString, FromMicroseconds)
{
    const auto nowMicroSeconds = std::chrono::microseconds(1654508279000000); // GMT: 2022-06-06, 9:37:59
    const auto actualDate = getDateString(nowMicroSeconds);
    const std::string expectedDate = "2022-06-06";
    ASSERT_EQ(expectedDate, actualDate);
}

TEST(GetDateString, FromSeconds)
{
    const auto nowSeconds = std::chrono::seconds(1654508279); // GMT: 2022-06-06, 9:37:59
    const auto actualDate = getDateString(nowSeconds);
    const std::string expectedDate = "2022-06-06";
    ASSERT_EQ(expectedDate, actualDate);
}

TEST(GetDateString, GeneratesValidDateString)
{
    const size_t microsecondsSinceEpoch = 1654508279000000; // GMT: 2022-06-06, 9:37:59
    const auto string = getDateString(Microseconds(microsecondsSinceEpoch));
    EXPECT_TRUE(isValidDateString(string));
    const std::string expectedDate = "2022-06-06";
    ASSERT_EQ(expectedDate, string);
}

TEST(IsValidDateString, Valid)
{
    EXPECT_TRUE(isValidDateString("2022-01-01"));
    EXPECT_TRUE(isValidDateString("5555-55-55"));
    EXPECT_TRUE(isValidDateString("0000-00-00"));
}

TEST(IsValidDateString, Invalid)
{
    EXPECT_FALSE(isValidDateString("2022-01-02T12:12:12"));

    EXPECT_FALSE(isValidDateString("20220102"));
    EXPECT_FALSE(isValidDateString("2022-1-1"));

    EXPECT_FALSE(isValidDateString("a022-01-01"));
    EXPECT_FALSE(isValidDateString("2a22-01-01"));
    EXPECT_FALSE(isValidDateString("20a2-01-01"));
    EXPECT_FALSE(isValidDateString("202a-01-01"));
    EXPECT_FALSE(isValidDateString("2022a01-01"));
    EXPECT_FALSE(isValidDateString("2022-a1-01"));
    EXPECT_FALSE(isValidDateString("2022-0a-01"));
    EXPECT_FALSE(isValidDateString("2022-01a01"));
    EXPECT_FALSE(isValidDateString("2022-01-a1"));
    EXPECT_FALSE(isValidDateString("2022-01-0a"));
}

} // namespace maps::mirc::pg_dumper::tests
