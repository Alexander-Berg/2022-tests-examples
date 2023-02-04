#include "helpers.h"

#include <maps/fastcgi/keyserv/lib/utils.h>

#include <maps/libs/common/include/exception.h>

#include <contrib/libs/libidn/lib/idna.h>

#include <string>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

using namespace maps::keyserv;

Y_UNIT_TEST_SUITE(utils_test) {

Y_UNIT_TEST(test_trimUri)
{
    std::string uriFixTest = trimUri("     http://www.www.somesite.provider.com ");
    EXPECT_EQ(uriFixTest, "http://www.somesite.provider.com");
}

Y_UNIT_TEST(test_punycode)
{
    std::string original = "http://карты.яндекс.рф./page/";
    std::string uriFixTest = punycodeEncode(original);
    EXPECT_EQ(uriFixTest, "http://xn--80atti9b.xn--d1acpjx3f.xn--p1ai./page/");
    EXPECT_EQ(original, punycodeDecode(uriFixTest));
}

Y_UNIT_TEST(test_subnet)
{
    EXPECT_NO_THROW(Subnet("1.1.1.1"));
    EXPECT_NO_THROW(Subnet("1.1.1.1/1"));
    EXPECT_NO_THROW(Subnet("1.1.1.1/32"));
    EXPECT_NO_THROW(Subnet("1.1.1.1/0"));
    EXPECT_NO_THROW(Subnet(""));


    EXPECT_THROW(Subnet("1.1.1"), BadSubnetFormat);
    EXPECT_THROW(Subnet("1.1.1.1/a"), BadSubnetFormat);
    EXPECT_THROW(Subnet("1.1.1.1/33"), BadSubnetFormat);
    EXPECT_THROW(Subnet("1.1.1.1/-2"), BadSubnetFormat);
    EXPECT_THROW(Subnet("1.1.1.256"), BadSubnetFormat);

    EXPECT_NO_THROW(Subnet("2001:4f8:3:ba:2e0:81ff:fe22:d1f1"));
    EXPECT_NO_THROW(Subnet("2001:4f8:3:ba:2e0:81ff:fe22:d1f1/1"));
    EXPECT_NO_THROW(Subnet("2001:4f8:3:ba:2e0:81ff:fe22:d1f1/32"));
    EXPECT_NO_THROW(Subnet("2001:4f8:3:ba:2e0:81ff:fe22:d1f1/0"));

    EXPECT_THROW(Subnet("2001:4f8"), BadSubnetFormat);
    EXPECT_THROW(Subnet("2001:4f88888"), BadSubnetFormat);
    EXPECT_THROW(Subnet("2001:4f8:3:ba:2e0:81ff:fe22:d1f1/a"), BadSubnetFormat);
    EXPECT_THROW(Subnet("2001:4f8:3:ba:2e0:81ff:fe22:d1f1/129"), BadSubnetFormat);
    EXPECT_THROW(Subnet("2001:4f8:3:ba:2e0:81ff:fe22:d1f1/-2"), BadSubnetFormat);

    EXPECT_NO_THROW(Subnet("::ffff:125.12.33.0"));
    EXPECT_THROW(Subnet("::ffff:288.1.2.3"), BadSubnetFormat);

    EXPECT_EQ(Subnet("::ffff:125.12.33.0").str(), "125.12.33.0");
}

Y_UNIT_TEST(test_escapeForLike)
{
   EXPECT_EQ(escapeForLike("%20%30yandex", '|'), "|%20|%30yandex");
}
} // Y_UNIT_TEST_SUITE(utils_test)
