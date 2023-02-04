#pragma once

#include <boost/filesystem.hpp>
#include <boost/test/unit_test.hpp>
#include <maps/libs/json/include/value.h>

#ifdef YANDEX_MAPS_BUILD
    #include <boost/test/detail/unit_test_parameters.hpp>  //Y_IGNORE
#else
    #include <boost/test/unit_test_parameters.hpp>
#endif //YANDEX_MAPS_BUILD

#include <string>

namespace bt = boost::unit_test;

namespace maps {
namespace wiki {
namespace graph {
namespace tests {

#ifdef YANDEX_MAPS_BUILD
#define MAKE_TEST_CASE( test_function, name ) \
    bt::make_test_case(bt::callback0<>(test_function), \
        name)
#else
#define MAKE_TEST_CASE( test_function, name ) \
    bt::make_test_case(boost::function<void ()>(test_function), \
        name, \
        __FILE__, __LINE__ )
#endif //YANDEX_MAPS_BUILD


template<typename Constructor, typename Checker>
bool initTestSuite(const std::string& suiteName, Constructor construct, Checker check)
{
    namespace fs = boost::filesystem;

    const fs::path path("tests/" + suiteName);
    const fs::directory_iterator endIt;
    for (fs::directory_iterator it(path); it != endIt; ++it) {
        const auto& testPath = it->path();
        const auto testJson = json::Value::fromFile(testPath.string());
        const std::string testName = testJson.hasField("description")
            ? testPath.string() + ": " + testJson["description"].toString()
            : testPath.string();
        bt::framework::master_test_suite().add(
            MAKE_TEST_CASE(
                [=]{ check(construct(testJson)); },
                testName
            )
        );
    }

#ifdef YANDEX_MAPS_BUILD
    if (bt::runtime_config::log_level() > bt::log_messages) {
        bt::unit_test_log.set_threshold_level(bt::log_messages);
    }
#else
#if BOOST_VERSION == 106000 //deprecated
    if (bt::runtime_config::get<bt::log_level>(bt::runtime_config::LOG_LEVEL) > bt::log_messages) {
#else
    if (bt::runtime_config::get<bt::log_level>(bt::runtime_config::btrt_log_level) > bt::log_messages) {
#endif
        bt::unit_test_log.set_threshold_level(bt::log_messages);
    }
#endif //YANDEX_MAPS_BUILD

    return true;
}

} // namespace tests
} // namespace graph
} // namespace wiki
} // namespace maps
