#define BOOST_TEST_MAIN
#include "../common/init_coverage.h"

#include <maps/analyzer/libs/geoinfo/include/geoid.h>

#include <parsers/extjams_parser.h>
#include <config.h>

const char requestBody[] =
"<?xml version=\"1.0\" encoding=\"utf-8\"?>"\
"<tracks clid=\"987\">"\
    "<track uuid=\"0d63b6deacb91b00e46194fac325b72a\" "\
           "deviceid=\"HTC Diamond\" "\
           "softid=\"Yandex Maps?\" "\
           "softver=\"2.10\" "\
           "category=\"n\">"\
        "<point latitude=\"55.716759\" "\
               "longitude=\"37.687881\" "\
               "avg_speed=\"36\" "\
               "direction=\"242\" "\
               "time=\"22022011:160005\"/>"\
        "<point latitude=\"55.5\" "\
               "longitude=\"31.3\" "\
               "avg_speed=\"72\" "\
               "direction=\"142\" "\
               "time=\"22022012:160006\"/>"\
    "</track>"\
"</tracks>";

BOOST_FIXTURE_TEST_SUITE(AvailableCoverageTestSuite, CopyCoverageFiles)

BOOST_AUTO_TEST_CASE(parse_correct_request_test)
{
    Config config("common_config.xml", "config.xml");
    maps::geoinfo::GeoId geoId = getGeoIdTrf();
    auto inspector = std::make_shared<Inspector>(&config, geoId);

    ExtjamsParser parser(inspector);

    Parser::Env env = {
        { "QUERY_STRING", "" },
        { "REQUEST_METHOD", "POST" },
        { "CONTENT_LENGTH", std::to_string(sizeof(requestBody)) },
        { "CONTENT_TYPE", "application/x-www-form-urlencoded" }
    };

    yacare::QueryParams input = {
        { "compressed",{ "0" } },
        { "data",{ requestBody } }
    };

    InspectionContext context;
    std::vector<SignalData> signals;

    parser.parse(env, input, "", &context, &signals);

    BOOST_CHECK_EQUAL(signals.size(), 2);

    const auto& signal = signals[0];
    BOOST_CHECK_EQUAL(signal.attr<std::string>("clid"), "987");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("uuid"), "0d63b6deacb91b00e46194fac325b72a");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("deviceid"), "HTC Diamond");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("softid"), "Yandex Maps?");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("softver"), "2.10");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("category"), "n");

    BOOST_CHECK_EQUAL(signal.attr<std::string>("latitude"), "55.716759");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("longitude"), "37.687881");
    BOOST_CHECK_CLOSE(signal.attr<double>("avg_speed"), 10.0, 1.0);
    BOOST_CHECK_EQUAL(signal.attr<std::string>("direction"), "242");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("time"), "22022011:160005");

    const auto& signal1 = signals[1];
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("clid"), "987");
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("uuid"), "0d63b6deacb91b00e46194fac325b72a");
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("deviceid"), "HTC Diamond");
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("softid"), "Yandex Maps?");
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("softver"), "2.10");
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("category"), "n");

    BOOST_CHECK_EQUAL(signal1.attr<std::string>("latitude"), "55.5");
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("longitude"), "31.3");
    BOOST_CHECK_CLOSE(signal1.attr<double>("avg_speed"), 20.0, 1.0);
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("direction"), "142");
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("time"), "22022012:160006");
}

BOOST_AUTO_TEST_SUITE_END()
