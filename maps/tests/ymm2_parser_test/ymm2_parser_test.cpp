#define BOOST_TEST_MAIN
#include "../common/init_coverage.h"

#include <parsers/ymm_2x_parser.h>
#include <config.h>

const char requestBody[] =
"<?xml version=\"1.0\" encoding=\"utf-8\"?>"\
"<traffic_collect>"\
    "<point map=\"2000\" "\
            "avg_speed=\"36\" "\
            "time=\"15022011:170759\" "\
            "direction=\"38\" "\
            "lat=\"55.7\" "\
            "lon=\"37.5\" "\
            "cellid=\"9835590\" "\
            "lac=\"7700\" "\
            "operatorid=\"02\" "\
            "countrycode=\"250\" "\
            "signalstrength=\"91\"/>"\
    "<point map=\"2000\" "\
            "avg_speed=\"36\" "\
            "time=\"15022011:170759\" "\
            "direction=\"38\" "\
            "lat=\"55.711983\" "\
            "lon=\"37.587433\" "\
            "cellid=\"9835590\" "\
            "lac=\"7700\" "\
            "operatorid=\"02\" "\
            "countrycode=\"250\" "\
            "signalstrength=\"91\"/>"\
    "<point lat=\"55.734207\" " \
            "lon=\"37.587318\" " \
            "direction=\"\" " \
            "time=\"02112011:175111\" " \
            "charger=\"0\" />"\
"</traffic_collect>";

BOOST_FIXTURE_TEST_SUITE(AvailableCoverageTestSuite, CopyCoverageFiles)

BOOST_AUTO_TEST_CASE(parse_correct_request_test)
{
    Config config("common_config.xml", "config.xml");
    maps::geoinfo::GeoId geoId = getGeoIdTrf();
    auto inspector = std::make_shared<Inspector>(&config, geoId);

    Ymm2xParser parser(inspector);

    Parser::Env env = {
        { "QUERY_STRING", "uuid=1" },
        { "REQUEST_METHOD", "POST" },
        { "CONTENT_LENGTH", std::to_string(sizeof(requestBody)) },
        { "CONTENT_TYPE", "application/x-www-form-urlencoded" }
    };

    yacare::QueryParams input = {
        {"packetid", {"3548823098"} },
        {"uuid", {"1"}},
        {"compressed", {"0"}},
        {"data", {requestBody}}
    };

    InspectionContext context;
    std::vector<SignalData> signals;

    parser.parse(env, input, "", &context, &signals);

    BOOST_CHECK_EQUAL(signals.size(), 3);

    const auto& signal0 = signals[0];

    // should be renamed to latitude/longitude
    BOOST_CHECK(!signal0.hasAttr("lat"));
    BOOST_CHECK(!signal0.hasAttr("lon"));

    BOOST_CHECK_EQUAL(signal0.attr<std::string>("latitude"), "55.7");
    BOOST_CHECK_EQUAL(signal0.attr<std::string>("longitude"), "37.5");
    BOOST_CHECK_CLOSE(signal0.attr<double>("avg_speed"), 10.0, 1.0);
    BOOST_CHECK_EQUAL(signal0.attr<std::string>("direction"), "38");
    BOOST_CHECK_EQUAL(signal0.attr<std::string>("operatorid"), "02");
    BOOST_CHECK_EQUAL(signal0.attr<std::string>("countrycode"), "250");
    BOOST_CHECK_EQUAL(signal0.attr<std::string>("cellid"), "9835590");
    BOOST_CHECK_EQUAL(signal0.attr<std::string>("lac"), "7700");
    BOOST_CHECK_EQUAL(signal0.attr<std::string>("signalstrength"), "91");
    BOOST_CHECK_EQUAL(signal0.attr<std::string>("time"), "15022011:170759");

    const auto& signal2 = signals[2];
    BOOST_CHECK(!signal2.hasAttr("avg_speed"));
    BOOST_CHECK_EQUAL(signal2.attr<int>("charger"),  0);
}

BOOST_AUTO_TEST_SUITE_END()
