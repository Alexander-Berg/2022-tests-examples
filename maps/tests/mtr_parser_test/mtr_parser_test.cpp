#define BOOST_TEST_MAIN
#include "../common/init_coverage.h"

#include <parsers/mtr_parser.h>
#include <config.h>

const char* queryString = "\
latitude=51.8&longitude=55.1\
&avg_speed=36&direction=9&time=15082012:104024\
&uuid=1&clid=02&category=s&route=U&vehicle_type=bus\
&vehicle_properties=bikes_allowed%3A1%2Clow_floor%3A2";

BOOST_FIXTURE_TEST_SUITE(AvailableCoverageTestSuite, CopyCoverageFiles)

BOOST_AUTO_TEST_CASE(parse_correct_request_test)
{
    Config config("common_config.xml", "config.xml");
    maps::geoinfo::GeoId geoId = getGeoIdTrf();
    auto inspector = std::make_shared<Inspector>(&config, geoId);
    MtrParser parser(inspector);

    yacare::RequestBuilder requestBuilder;
    requestBuilder.putenv("QUERY_STRING", queryString);
    requestBuilder.putenv("REQUEST_METHOD", "GET");

    requestBuilder.readBody([](char*, size_t) {return false; });

    const auto& request = requestBuilder.request();

    InspectionContext context;
    std::vector<SignalData> signals;

    parser.parse(request.env(), request.input(), request.body(), &context, &signals);

    BOOST_CHECK_EQUAL(signals.size(), 1);

    const auto& signal = signals[0];
    BOOST_CHECK_EQUAL(signal.attr<std::string>("clid"), "02");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("uuid"), "1");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("latitude"), "51.8");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("longitude"), "55.1");
    BOOST_CHECK_CLOSE(signal.attr<double>("avg_speed"), 10.0, 1.0);
    BOOST_CHECK_EQUAL(signal.attr<std::string>("direction"), "9");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("time"), "15082012:104024");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("route"), "U");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("vehicle_type"), "bus");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("vehicle_properties"), "bikes_allowed:1,low_floor:2");
}

BOOST_AUTO_TEST_SUITE_END()
