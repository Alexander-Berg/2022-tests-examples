#define BOOST_TEST_MAIN
#include <boost/test/auto_unit_test.hpp>

#include <config.h>

#include <fstream>
#include <boost/assign/std/vector.hpp>


const auto MOCK = [](const int& regionId) {
    return [&regionId]() {
        std::vector<int> res{regionId};
        if (res.back() != 10000) {
            res.push_back(10000);
        }
        return res;
    };
};

BOOST_AUTO_TEST_CASE(test_allowed_clids)
{
    Config config("common_config.xml", "config.xml");
    BOOST_CHECK(config.allowedClid("auto", 10000, MOCK(10000)())); // all allowed for auto in config.xml
    BOOST_CHECK(config.allowedClid("1", 10000, MOCK(10000)())); // all allowed for 1 in config.xml
    BOOST_CHECK(config.allowedClid("2", 213, MOCK(213)())); // only 213 allowed for 2 in config.xml (overrides common)
    BOOST_CHECK(!config.allowedClid("2", 214, MOCK(214)())); // only 213 allowed for 2 in config.xml (overrides common)
    BOOST_CHECK(!config.allowedClid("2", 215, MOCK(215)())); // only 213 allowed for 2 in config.xml (overrides common)
    BOOST_CHECK(config.allowedClid("3", 214, MOCK(214)())); // only 214, 215 allowed for 3 in common_config.xml
    BOOST_CHECK(config.allowedClid("3", 215, MOCK(215)())); // only 214, 215 allowed for 3 in common_config.xml

    BOOST_CHECK(config.allowedClid("auto", 100500, MOCK(100500)())); // all allowed for auto in config.xml
    BOOST_CHECK(config.allowedClid("1", 213, MOCK(213)())); // all allowed for 1 in config.xml
    BOOST_CHECK(!config.allowedClid("2", 100500, MOCK(100500)())); // only 213 allowed for 2 in config.xml (overrides common)
    BOOST_CHECK(!config.allowedClid("3", 213, MOCK(213)())); // only 214, 215 allowed for 3 in common_config.xml
    BOOST_CHECK(!config.allowedClid("3", 10500, MOCK(10500)())); // only 214, 215 allowed for 3 in common_config.xml

    const auto getEmptyRegions = []() {
        return std::vector<int>();
    };
    BOOST_CHECK_THROW(config.allowedClid("3", 10501, getEmptyRegions()), maps::Exception); // region won't be cached with empty getRegions
}

BOOST_AUTO_TEST_CASE(test_common_config_nowhitelist)
{
    Config config("config_nowhitelist.xml", "config.xml");
    BOOST_CHECK(config.allowedClid("1", 10000, MOCK(10000)())); // all allowed for 1 in config.xml
    BOOST_CHECK(config.allowedClid("2", 213, MOCK(213)())); // only 213 allowed for 2 in config.xml (overrides no wl common)
    BOOST_CHECK(config.allowedClid("1", 100500, MOCK(100500)())); // all allowed for 1 in config.xml
    BOOST_CHECK(!config.allowedClid("2", 214, MOCK(214)())); // only 213 allowed for 2 in config.xml (overrides no wl common)
    BOOST_CHECK(!config.allowedClid("2", 100500, MOCK(100500)())); // only 213 allowed for 2 in config.xml (overrides empty common)
}

BOOST_AUTO_TEST_CASE(test_common_config_empty_whitelist)
{
    Config config("config_empty_whitelist.xml", "config.xml");
    BOOST_CHECK(config.allowedClid("1", 10000, MOCK(10000)())); // all allowed for 1 in config.xml
    BOOST_CHECK(config.allowedClid("2", 213, MOCK(213)())); // only 213 allowed for 2 in config.xml (overrides empty wl common)
    BOOST_CHECK(config.allowedClid("1", 100500, MOCK(100500)())); // all allowed for 1 in config.xml
    BOOST_CHECK(!config.allowedClid("2", 214, MOCK(214)())); // only 213 allowed for 2 in config.xml (overrides empty wl common)
    BOOST_CHECK(!config.allowedClid("2", 100500, MOCK(100500)())); // only 213 allowed for 2 in config.xml (overrides empty wl common)
}

BOOST_AUTO_TEST_CASE(test_load_whitelist) {
    BOOST_CHECK_THROW(Config("config_nowhitelist.xml", "config_nowhitelist.xml"), maps::Exception);
    BOOST_CHECK_THROW(Config("config_empty_whitelist.xml", "config_empty_whitelist.xml"), maps::Exception);
    BOOST_CHECK_THROW(Config("config_invalid_whitelist.xml", "config.xml"), maps::Exception);
}

BOOST_AUTO_TEST_CASE(test_hosts_config)
{
    Config config("common_config.xml", "config.xml");
    BOOST_CHECK_EQUAL(config.parsers().size(), 4);
}

BOOST_AUTO_TEST_CASE(test_target_accept_config)
{
    Config config("config_empty_whitelist.xml", "config_with_accept.xml");
    BOOST_CHECK_EQUAL(config.parsers().size(), 1);
    const auto& targets = config.parsers().at(0).targets();
    BOOST_CHECK_EQUAL(targets.size(), 2);
    auto t1idx = targets.at(0).type() == "test-target1" ? 0 : 1;
    auto t2idx = targets.at(0).type() == "test-target1" ? 1 : 0;

    const auto& accClids = targets.at(t1idx).acceptedClids();
    BOOST_CHECK_EQUAL(accClids.size(), 2);
    BOOST_CHECK(accClids.find("com.example.app1") != accClids.end());
    BOOST_CHECK(accClids.find("com.example.app2") != accClids.end());

    BOOST_CHECK_EQUAL(targets.at(t2idx).acceptedClids().size(), 0);
}

BOOST_AUTO_TEST_CASE(test_subregions)
{
    Config config("common_config.xml", "config.xml");
    BOOST_CHECK(config.allowedClid("auto", 10000, MOCK(10000)())); // all allowed for auto in config.xml
    BOOST_CHECK(config.allowedClid("1", 10000, MOCK(10000)())); // all allowed for 1 in config.xml
    BOOST_CHECK(config.allowedClid("2", 213, MOCK(213)())); // only 213 allowed for 2 in config.xml (overrides common)
    BOOST_CHECK(!config.allowedClid("2", 214, MOCK(214)())); // only 213 allowed for 2 in config.xml (overrides common)
    BOOST_CHECK(!config.allowedClid("2", 215, MOCK(215)())); // only 213 allowed for 2 in config.xml (overrides common)
    BOOST_CHECK(config.allowedClid("3", 214, MOCK(214)())); // only 214, 215 allowed for 3 in common_config.xml
    BOOST_CHECK(config.allowedClid("3", 215, MOCK(215)())); // only 214, 215 allowed for 3 in common_config.xml

    Config config2("common_config.xml", "config.xml");
    auto getRegions1 = []() {
        return std::vector<int>{213, 100500, 10000};
    };
    BOOST_CHECK(config2.allowedClid("auto", 100500, MOCK(100500)())); // all allowed for auto in config.xml
    BOOST_CHECK(config2.allowedClid("auto", 213, getRegions1())); // all allowed for auto in config.xml
    BOOST_CHECK(config2.allowedClid("auto", 214, MOCK(214)())); // all allowed for auto in config.xml

    Config config3("common_config.xml", "config.xml");
    auto getRegions2 = [](const int& idx) {
        return [&idx]() {
            const std::vector<int> regions = {1, 2, 213, 100500, 100501, 10000};
            return std::vector<int>(regions.begin() + idx, regions.end());
        };
    };
    BOOST_CHECK(config3.allowedClid("2", 1, getRegions2(0)())); // only 213 allowed for 2 in config.xml (overrides common), 1 is descendant
    BOOST_CHECK(config3.allowedClid("2", 2, getRegions2(1)())); // only 213 allowed for 2 in config.xml (overrides common), 2 is descendant

    auto getRegions3 = [](const int& idx) {
        return [&idx]() {
            const std::vector<int> regions = {3, 214, 213, 100500, 100501, 10000};
            return std::vector<int>(regions.begin() + idx, regions.end());
        };
    };
    BOOST_CHECK(config3.allowedClid("2", 3, getRegions3(0)())); // only 213 allowed for 2 in config.xml (overrides common), 3 is descendant
    BOOST_CHECK(config3.allowedClid("2", 213, getRegions3(2)())); // only 213 allowed for 2 in config.xml (overrides common)
    BOOST_CHECK(config3.allowedClid("2", 214, getRegions3(1)())); // only 213 allowed for 2 in config.xml (overrides common), 214 is descendant
    BOOST_CHECK(!config3.allowedClid("2", 100500, getRegions3(3)())); // only 213 allowed for 2 in config.xml (overrides common)
    BOOST_CHECK(!config3.allowedClid("2", 100501, getRegions3(4)())); // only 213 allowed for 2 in config.xml (overrides common)
}
