#include <maps/libs/common/include/exception.h>
#include <maps/infra/ecstatic/ymtorrent/lib/helpers.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <boost/asio.hpp>

#include <string>

using namespace maps::torrent::helpers;

Y_UNIT_TEST_SUITE(helpers_test)
{
    Y_UNIT_TEST(get_branch_path_test)
    {
        UNIT_ASSERT_EQUAL(parentPath(""), "");
        UNIT_ASSERT_EQUAL(parentPath("/"), "");
        UNIT_ASSERT_EQUAL(parentPath("//"), "/");
        UNIT_ASSERT_EQUAL(parentPath("/a"), "/");
        UNIT_ASSERT_EQUAL(parentPath("/a/"), "/");
        UNIT_ASSERT_EQUAL(parentPath("/a/b"), "/a/");
        UNIT_ASSERT_EQUAL(parentPath("/a/b/"), "/a/");
        UNIT_ASSERT_EQUAL(parentPath("/a/b//"), "/a/");
    }

    Y_UNIT_TEST(parse_ip_list_test)
    {
        {
            IpVector ips = parseIpList("");
            UNIT_ASSERT_EQUAL(ips.size(), 0);
        }

        {
            IpVector ips = parseIpList("2a02:6b8:a::a");
            IpVector expected {boost::asio::ip::make_address("2a02:6b8:a::a")};
            UNIT_ASSERT_EQUAL(ips, expected);
        }

        {
            IpVector ips = parseIpList("2a02:6b8:a::a,2a02:6b8:0:1421::253");
            IpVector expected {boost::asio::ip::make_address("2a02:6b8:a::a"),
                               boost::asio::ip::make_address("2a02:6b8:0:1421::253")};
            UNIT_ASSERT_EQUAL(ips, expected);
        }

        {
            IpVector ips = parseIpList("2a02:6b8:a::a,2a02:6b8:a::a");
            IpVector expected {boost::asio::ip::make_address("2a02:6b8:a::a"),
                               boost::asio::ip::make_address("2a02:6b8:a::a")};
            UNIT_ASSERT_EQUAL(ips, expected);
        }

        {
            IpVector ips = parseIpList("87.250.250.242");
            IpVector expected {boost::asio::ip::make_address("87.250.250.242")};
            UNIT_ASSERT_EQUAL(ips, expected);
        }

        {
            IpVector ips = parseIpList("2a02:6b8:a::a,87.250.250.242");
            IpVector expected {boost::asio::ip::make_address("2a02:6b8:a::a"),
                               boost::asio::ip::make_address("87.250.250.242")};
            UNIT_ASSERT_EQUAL(ips, expected);
        }

        {
            UNIT_ASSERT_EXCEPTION(parseIpList("foo"), maps::RuntimeError);
        }

        {
            UNIT_ASSERT_EXCEPTION(parseIpList("2a02:6b8:a::a,,87.250.250.242"), maps::RuntimeError);
        }

        {
            UNIT_ASSERT_EXCEPTION(parseIpList(",2a02:6b8:a::a,87.250.250.242"), maps::RuntimeError);
        }

        {
            UNIT_ASSERT_EXCEPTION(parseIpList("2a02:6b8:a::a,87.250.250.242,"), maps::RuntimeError);
        }

        {
            UNIT_ASSERT_EXCEPTION(parseIpList("2a02:6b8:a::a,foo"), maps::RuntimeError);
        }

        {
            UNIT_ASSERT_EXCEPTION(parseIpList("foo,87.250.250.242"), maps::RuntimeError);
        }
    }
}
