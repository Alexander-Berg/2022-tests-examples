#include <yandex_io/libs/net/net_utils.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;

Y_UNIT_TEST_SUITE(testNetUtils) {
    Y_UNIT_TEST(testCalculateIpRange1)
    {
        IpRange range("192.168.1.5", "255.255.255.0");
        UNIT_ASSERT_VALUES_EQUAL(range.hostCapacity(), 254);
        UNIT_ASSERT_VALUES_EQUAL(range.network(), "192.168.1.0");
        UNIT_ASSERT_VALUES_EQUAL(range.broadcast(), "192.168.1.255");
        UNIT_ASSERT_VALUES_EQUAL(range.ip(0), "192.168.1.1");
        UNIT_ASSERT_VALUES_EQUAL(range.ip(10), "192.168.1.11");
        UNIT_ASSERT_VALUES_EQUAL(range.ip(253), "192.168.1.254");
        UNIT_ASSERT_VALUES_EQUAL(range.ip(254), "");

        std::optional<size_t> index;

        index = range.indexOf("192.168.1.5");
        UNIT_ASSERT(index&&* index == 4);

        index = range.indexOf("192.168.2.5");
        UNIT_ASSERT(!index);

        index = range.indexOf("192.168.1.0");
        UNIT_ASSERT(!index);

        index = range.indexOf("192.168.1.255");
        UNIT_ASSERT(!index);
    }

    Y_UNIT_TEST(testCalculateIpRange2)
    {
        IpRange range("192.168.1.5", "255.255.255.252");
        UNIT_ASSERT_VALUES_EQUAL(range.hostCapacity(), 2);
        UNIT_ASSERT_VALUES_EQUAL(range.network(), "192.168.1.4");
        UNIT_ASSERT_VALUES_EQUAL(range.broadcast(), "192.168.1.7");
        UNIT_ASSERT_VALUES_EQUAL(range.ip(0), "192.168.1.5");
        UNIT_ASSERT_VALUES_EQUAL(range.ip(1), "192.168.1.6");
        UNIT_ASSERT_VALUES_EQUAL(range.ip(2), "");
    }

    Y_UNIT_TEST(testCalculateIpRange3)
    {
        IpRange range("192.168.1.5", "255.255.255.254");
        UNIT_ASSERT_VALUES_EQUAL(range.hostCapacity(), 0);
        UNIT_ASSERT_VALUES_EQUAL(range.network(), "192.168.1.4");
        UNIT_ASSERT_VALUES_EQUAL(range.broadcast(), "192.168.1.5");
        UNIT_ASSERT_VALUES_EQUAL(range.ip(0), "");
    }

    Y_UNIT_TEST(testCalculateIpRange4)
    {
        IpRange range("192.168.1.5", "255.255.255.255");
        UNIT_ASSERT_VALUES_EQUAL(range.hostCapacity(), 1);
        UNIT_ASSERT_VALUES_EQUAL(range.network(), "192.168.1.5");
        UNIT_ASSERT_VALUES_EQUAL(range.broadcast(), "192.168.1.5");
        UNIT_ASSERT_VALUES_EQUAL(range.ip(0), "192.168.1.5");
    }
}
