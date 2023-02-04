#include <maps/b2bgeo/libs/traffic_info/timeouts.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>


#define ASSERT_SECONDS_EQ(left, right) ASSERT_EQ((left).count(), (right).count());

Y_UNIT_TEST_SUITE(Timeouts)
{
using namespace maps::b2bgeo::traffic_info::detail;

Y_UNIT_TEST(mapsFetchTimeout)
{
    ASSERT_SECONDS_EQ(routerTimeout(1), std::chrono::seconds{2});
    ASSERT_SECONDS_EQ(routerTimeout(100), std::chrono::seconds{12});
    ASSERT_SECONDS_EQ(routerTimeout(10000), std::chrono::seconds{600});

    ASSERT_SECONDS_EQ(mrapiTimeout(1, 0.1), std::chrono::seconds{0});
    ASSERT_SECONDS_EQ(mrapiTimeout(100, 1.5), std::chrono::seconds{4});
    ASSERT_SECONDS_EQ(mrapiTimeout(10000, 0.8), std::chrono::seconds{600});
}

Y_UNIT_TEST(mrapiRetryTimeoutTest)
{
    ASSERT_SECONDS_EQ(mrapiRetryTimeout(1), std::chrono::milliseconds{1000});
    ASSERT_SECONDS_EQ(mrapiRetryTimeout(10000), std::chrono::milliseconds{10000});
}

}
