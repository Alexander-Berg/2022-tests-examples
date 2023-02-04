#include <maps/jams/renderer2/historic/yacare/lib/util.h>

#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::jams::renderer::historic::tests {

TEST(util_tests, historic_time_string_to_version)
{
    EXPECT_EQ(historicTimeStringToVersion("19700104T000000"), 0);
    EXPECT_EQ(historicTimeStringToVersion("19700104T140000"), 50400);
    EXPECT_EQ(historicTimeStringToVersion("19700105T181500"), 152100);
    EXPECT_EQ(historicTimeStringToVersion("19700110T234500"), 603900);

    EXPECT_THROW(historicTimeStringToVersion(""), Exception);
    EXPECT_THROW(historicTimeStringToVersion("data"), Exception);
    EXPECT_THROW(historicTimeStringToVersion("603900"), Exception);
}

} // namespace maps::jams::renderer::historic::tests
