#include <boost/test/unit_test.hpp>

#include <yandex/maps/navikit/uri.h>

namespace yandex::maps::navikit {

BOOST_AUTO_TEST_CASE(OnlyHostTest)
{
    Uri uri("show_ui");
    
    BOOST_CHECK_EQUAL(uri.host, "show_ui");
    BOOST_CHECK(uri.path.empty());
    BOOST_CHECK(uri.params.empty());
}

BOOST_AUTO_TEST_CASE(HostPathTest)
{
    Uri uri("show_ui/menu/fines");
    
    BOOST_CHECK_EQUAL(uri.host, "show_ui");
    
    std::vector<std::string> expectedPath {
        "menu",
        "fines"
    };
    BOOST_CHECK_EQUAL_COLLECTIONS(
        uri.path.begin(), uri.path.end(),
        expectedPath.begin(), expectedPath.end());
    
    BOOST_CHECK(uri.params.empty());
}

BOOST_AUTO_TEST_CASE(HostParamsTest)
{
    Uri uri("show_ui?param1=73&param2=ok");
    
    BOOST_CHECK_EQUAL(uri.host, "show_ui");
    
    BOOST_CHECK(uri.path.empty());
    
    std::map<std::string, std::string> expectedParams {
        {"param1", "73"},
        {"param2", "ok"}
    };
    BOOST_CHECK(uri.params == expectedParams);
}

BOOST_AUTO_TEST_CASE(HostPathParamsTest)
{
    Uri uri("show_ui/menu/fines?param1=73&param2=ok");
    
    BOOST_CHECK_EQUAL(uri.host, "show_ui");
    
    std::vector<std::string> expectedPath {
        "menu",
        "fines"
    };
    BOOST_CHECK_EQUAL_COLLECTIONS(
        uri.path.begin(), uri.path.end(),
        expectedPath.begin(), expectedPath.end());
    
    std::map<std::string, std::string> expectedParams {
        {"param1", "73"},
        {"param2", "ok"}
    };
    BOOST_CHECK(uri.params == expectedParams);
}

BOOST_AUTO_TEST_CASE(ExtraQuestionMarkTest)
{
    Uri uri("show_ui?p1=v1?p2=v2");

    BOOST_CHECK(uri.host.empty());
    BOOST_CHECK(uri.path.empty());
    BOOST_CHECK(uri.params.empty());
}

BOOST_AUTO_TEST_CASE(ParameterWithNoOrSeveralValuesTest)
{
    Uri uri("show_ui?p1&p2=v2&p3=v3=v4");
    
    BOOST_CHECK_EQUAL(uri.host, "show_ui");
    BOOST_CHECK(uri.path.empty());
    std::map<std::string, std::string> expectedParams {
        {"p1", ""},
        {"p2", "v2"}
    };
    BOOST_CHECK(uri.params == expectedParams);
}

} // namespace yandex
