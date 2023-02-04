#define BOOST_TEST_MAIN
#include "../common/init_coverage.h"

#include <parsers/ymm_1x_parser.h>
#include <config.h>

#include <maps/infra/yacare/include/request.h>

const char queryString[] = "direction=225&type=auto&avg_speed=72&latitude=50.1&longitude=37.2&time=01112011:115736&packetid=2597366303&uuid=3d766ab98c21fff6920b8b523fbd3d7f&";

BOOST_FIXTURE_TEST_SUITE(AvailableCoverageTestSuite, CopyCoverageFiles)

BOOST_AUTO_TEST_CASE(parse_correct_request_test)
{
    Config config("common_config.xml", "config.xml");
    maps::geoinfo::GeoId geoId = getGeoIdTrf();
    auto inspector = std::make_shared<Inspector>(&config, geoId);

    Ymm1xParser parser(inspector);

    yacare::RequestBuilder requestBuilder;
    requestBuilder.putenv("QUERY_STRING", queryString);
    requestBuilder.putenv("REQUEST_METHOD", "GET");

    requestBuilder.readBody([](char*, size_t) {return false; });

    const auto& request = requestBuilder.request();

    InspectionContext context;
    std::vector<SignalData> signals;

    parser.parse(request.env(), request.input(), request.body(), &context, &signals);

    BOOST_CHECK_EQUAL(signals.size(), 1);

    const SignalData& signal = signals[0];

    // should be renamed to latitude/longitude
    BOOST_CHECK(!signal.hasAttr("lat"));
    BOOST_CHECK(!signal.hasAttr("lon"));

    BOOST_CHECK_EQUAL(signal.attr<std::string>("latitude"), "50.1");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("longitude"), "37.2");
    BOOST_CHECK_CLOSE(signal.attr<double>("avg_speed"), 20.0, 1.0);
    BOOST_CHECK_EQUAL(signal.attr<std::string>("direction"), "225");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("time"), "01112011:115736");
}

BOOST_AUTO_TEST_SUITE_END()
