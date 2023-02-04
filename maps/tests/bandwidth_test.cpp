#include <maps/libs/common/include/exception.h>
#include <maps/infra/ecstatic/ymtorrent/lib/bandwidth.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/size_literals.h>

#include <boost/asio.hpp>

#include <string>
#include <vector>

using namespace maps::torrent;

Y_UNIT_TEST_SUITE(bandwidth_test)
{
    Y_UNIT_TEST(bandwidth)
    {
        {
            Bandwidth bw("1024", 1);
            UNIT_ASSERT_EQUAL(bw.maxBps, 1024);
            UNIT_ASSERT_EQUAL(bw.priority, 1);
        }

        {
            Bandwidth bw("1024", 2);
            UNIT_ASSERT_EQUAL(bw.maxBps, 1024);
            UNIT_ASSERT_EQUAL(bw.priority, 2);
        }

        {
            Bandwidth bw("64k", 3);
            UNIT_ASSERT_EQUAL(bw.maxBps, 64_KB);
            UNIT_ASSERT_EQUAL(bw.priority, 3);
        }

        {
            Bandwidth bw("32m", 5);
            UNIT_ASSERT_EQUAL(bw.maxBps, 32_MB);
            UNIT_ASSERT_EQUAL(bw.priority, 5);
        }

        {
            Bandwidth bw("1g", 7);
            UNIT_ASSERT_EQUAL(bw.maxBps, 1_GB);
            UNIT_ASSERT_EQUAL(bw.priority, 7);
        }

        {
            Bandwidth bw("inf", 1);
            UNIT_ASSERT_EQUAL(bw.maxBps, 0);
            UNIT_ASSERT_EQUAL(bw.priority, 1);
        }

        UNIT_ASSERT_EXCEPTION(Bandwidth("-1024", 1), maps::RuntimeError);
        UNIT_ASSERT_EXCEPTION(Bandwidth("1024", 256), maps::RuntimeError);
        UNIT_ASSERT_EXCEPTION(Bandwidth("1024", -1), maps::RuntimeError);
        UNIT_ASSERT_EXCEPTION(Bandwidth("", 1), maps::RuntimeError);
        UNIT_ASSERT_EXCEPTION(Bandwidth("foo", 1), maps::RuntimeError);
        UNIT_ASSERT_EXCEPTION(Bandwidth("1024f", 1), maps::RuntimeError);
        UNIT_ASSERT_EXCEPTION(Bandwidth("7g", 1), maps::RuntimeError);
    }

    Y_UNIT_TEST(bandwidth_rules)
    {
        {
            auto rules = parseBandwidthRules(BandwidthParameters {{"2a02:6b8:a::a", "1g"}});

            UNIT_ASSERT_EQUAL(rules.size(), 1);

            auto it = rules.begin();
            const auto& [bw, interfaces] = *it;

            UNIT_ASSERT_EQUAL(bw.maxBps, 1_GB);
            UNIT_ASSERT_EQUAL(bw.priority, LOWEST_TRAFFIC_PRIORITY);
            UNIT_ASSERT_EQUAL(
                interfaces,
                std::vector<Interface> {Interface(boost::asio::ip::make_address("2a02:6b8:a::a"))});
        }

        {
            auto rules = parseBandwidthRules(BandwidthParameters {{"any", "1g"}});

            UNIT_ASSERT_EQUAL(rules.size(), 1);

            auto it = rules.begin();
            const auto& [bw, interfaces] = *it;

            UNIT_ASSERT_EQUAL(bw.maxBps, 1_GB);
            UNIT_ASSERT_EQUAL(bw.priority, LOWEST_TRAFFIC_PRIORITY);
            UNIT_ASSERT_EQUAL(interfaces, std::vector<Interface> {Interface(Interface::Type::Any)});
        }

        {
            auto rules = parseBandwidthRules(BandwidthParameters {{"fastbone", "1g"}});

            UNIT_ASSERT_EQUAL(rules.size(), 1);

            auto it = rules.begin();
            const auto& [bw, interfaces] = *it;

            UNIT_ASSERT_EQUAL(bw.maxBps, 1_GB);
            UNIT_ASSERT_EQUAL(bw.priority, HIGHEST_TRAFFIC_PRIORITY);
            UNIT_ASSERT_EQUAL(
                interfaces, std::vector<Interface> {Interface(Interface::Type::Fastbone)});
        }

        {
            auto rules = parseBandwidthRules(
                BandwidthParameters {{"2a02:6b8:a::a,2a02:6b8:0:1421::253", "1g"}});

            UNIT_ASSERT_EQUAL(rules.size(), 1);

            auto it = rules.begin();
            const auto& [bw, interfaces] = *it;

            UNIT_ASSERT_EQUAL(bw.maxBps, 1_GB);
            UNIT_ASSERT_EQUAL(bw.priority, LOWEST_TRAFFIC_PRIORITY);

            std::vector<Interface> expected = {
                Interface(boost::asio::ip::make_address("2a02:6b8:a::a")),
                Interface(boost::asio::ip::make_address("2a02:6b8:0:1421::253"))};
            UNIT_ASSERT_EQUAL(interfaces, expected);
        }

        {
            auto rules = parseBandwidthRules(
                BandwidthParameters {{"2a02:6b8:a::a,2a02:6b8:0:1421::253,2a02:6b8::1:226", "1g"}});

            UNIT_ASSERT_EQUAL(rules.size(), 1);

            auto it = rules.begin();
            const auto& [bw, interfaces] = *it;

            UNIT_ASSERT_EQUAL(bw.maxBps, 1_GB);
            UNIT_ASSERT_EQUAL(bw.priority, LOWEST_TRAFFIC_PRIORITY);

            std::vector<Interface> expected = {
                Interface(boost::asio::ip::make_address("2a02:6b8:a::a")),
                Interface(boost::asio::ip::make_address("2a02:6b8:0:1421::253")),
                Interface(boost::asio::ip::make_address("2a02:6b8::1:226"))};
            UNIT_ASSERT_EQUAL(interfaces, expected);
        }

        {
            auto rules = parseBandwidthRules(BandwidthParameters {
                {"2a02:6b8:a::a,2a02:6b8:0:1421::253", "1g"}, {"2a02:6b8::1:226", "32m"}});

            UNIT_ASSERT_EQUAL(rules.size(), 2);

            auto it = rules.begin();

            {
                const auto& [bw, interfaces] = *it;

                UNIT_ASSERT_EQUAL(bw.maxBps, 32_MB);
                UNIT_ASSERT_EQUAL(bw.priority, LOWEST_TRAFFIC_PRIORITY);

                std::vector<Interface> expected = {
                    Interface(boost::asio::ip::make_address("2a02:6b8::1:226"))};
                UNIT_ASSERT_EQUAL(interfaces, expected);
            }

            ++it;

            {
                const auto& [bw, interfaces] = *it;

                UNIT_ASSERT_EQUAL(bw.maxBps, 1_GB);
                UNIT_ASSERT_EQUAL(bw.priority, LOWEST_TRAFFIC_PRIORITY);

                std::vector<Interface> expected = {
                    Interface(boost::asio::ip::make_address("2a02:6b8:a::a")),
                    Interface(boost::asio::ip::make_address("2a02:6b8:0:1421::253"))};
                UNIT_ASSERT_EQUAL(interfaces, expected);
            }
        }

        {
            auto rules = parseBandwidthRules(
                BandwidthParameters {{"any", "32m"}, {"2a02:6b8::1:226", "32m"}});

            UNIT_ASSERT_EQUAL(rules.size(), 1);

            auto it = rules.begin();

            const auto& [bw, interfaces] = *it;

            UNIT_ASSERT_EQUAL(bw.maxBps, 32_MB);
            UNIT_ASSERT_EQUAL(bw.priority, LOWEST_TRAFFIC_PRIORITY);

            std::vector<Interface> expected = {Interface(Interface::Type::Any)};
            UNIT_ASSERT_EQUAL(interfaces, expected);
        }

        {
            auto rules = parseBandwidthRules(
                BandwidthParameters {{"2a02:6b8::1:226", "32m"}, {"any", "32m"}});

            UNIT_ASSERT_EQUAL(rules.size(), 1);

            auto it = rules.begin();

            const auto& [bw, interfaces] = *it;

            UNIT_ASSERT_EQUAL(bw.maxBps, 32_MB);
            UNIT_ASSERT_EQUAL(bw.priority, LOWEST_TRAFFIC_PRIORITY);

            std::vector<Interface> expected = {Interface(Interface::Type::Any)};
            UNIT_ASSERT_EQUAL(interfaces, expected);
        }

        {
            auto rules = parseBandwidthRules(
                BandwidthParameters {{"fastbone", "32m"}, {"2a02:6b8::1:226", "32m"}});

            UNIT_ASSERT_EQUAL(rules.size(), 2);

            auto it = rules.begin();

            {
                const auto& [bw, interfaces] = *it;

                UNIT_ASSERT_EQUAL(bw.maxBps, 32_MB);
                UNIT_ASSERT_EQUAL(bw.priority, LOWEST_TRAFFIC_PRIORITY);

                std::vector<Interface> expected = {
                    Interface(boost::asio::ip::make_address("2a02:6b8::1:226"))};
                UNIT_ASSERT_EQUAL(interfaces, expected);
            }

            ++it;

            {
                const auto& [bw, interfaces] = *it;

                UNIT_ASSERT_EQUAL(bw.maxBps, 32_MB);
                UNIT_ASSERT_EQUAL(bw.priority, HIGHEST_TRAFFIC_PRIORITY);

                std::vector<Interface> expected = {Interface(Interface::Type::Fastbone)};
                UNIT_ASSERT_EQUAL(interfaces, expected);
            }
        }

        {
            UNIT_ASSERT_EXCEPTION(
                parseBandwidthRules(BandwidthParameters {
                    {"2a02:6b8:a::a,2a02:6b8:0:1421::253", "1g"}, {"2a02:6b8:a::a", "32m"}}),
                maps::RuntimeError);
        }

        {
            UNIT_ASSERT_EXCEPTION(
                parseBandwidthRules(BandwidthParameters {{"foo", "1g"}}), maps::RuntimeError);
        }

        {
            UNIT_ASSERT_EXCEPTION(
                parseBandwidthRules(BandwidthParameters {{"any,2a02:6b8:0:1421::253", "1g"}}),
                maps::RuntimeError);
        }

        {
            UNIT_ASSERT_EXCEPTION(
                parseBandwidthRules(BandwidthParameters {{"", "1g"}}), maps::RuntimeError);
        }
    }
}
