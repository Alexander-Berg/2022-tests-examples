#define BOOST_TEST_MAIN
#include "../common/init_coverage.h"

#include <parsers/csv_parser.h>
#include <config.h>

#include <fstream>
#include <streambuf>

std::ifstream gzipBodyFile("csv_body.csv.gz");
const std::string requestBody{
    std::istreambuf_iterator<char>(gzipBodyFile),
    std::istreambuf_iterator<char>()
};

BOOST_FIXTURE_TEST_SUITE(AvailableCoverageTestSuite, CopyCoverageFiles)

BOOST_AUTO_TEST_CASE(parse_correct_request_test)
{
    Config config("common_config.xml", "config.xml");
    maps::geoinfo::GeoId geoId = getGeoIdTrf();
    auto inspector = std::make_shared<Inspector>(&config, geoId);

    CsvParser parser(inspector);

    Parser::Env env = {
        { "QUERY_STRING", "" },
        { "REQUEST_METHOD", "POST" },
        { "CONTENT_LENGTH", std::to_string(requestBody.size()) },
        { "CONTENT_TYPE", "application/x-www-form-urlencoded" }
    };

    yacare::QueryParams input = {
        { "clid", { "sygic" } }
    };

    InspectionContext context;
    std::vector<SignalData> signals;

    parser.parse(env, input, requestBody, &context, &signals);

    BOOST_CHECK_EQUAL(signals.size(), 2);

    const auto& signal = signals[0];
    BOOST_CHECK_EQUAL(signal.attr<std::string>("clid"), "sygic");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("uuid"), "722691254b8c413e9467961918071433");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("latitude"), "33.54405");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("longitude"), "-117.63764");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("altitude"), "128");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("accuracy"), "0");
    BOOST_CHECK_CLOSE(signal.attr<double>("avg_speed"), 20, 0.1);
    BOOST_CHECK_EQUAL(signal.attr<std::string>("direction"), "124");
    BOOST_CHECK_EQUAL(signal.attr<std::string>("time"), "06052018:211557");

    const auto& signal1 = signals[1];
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("clid"), "sygic");
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("uuid"), "722691254b8c413e9467961918071433");
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("latitude"), "33.54351");
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("longitude"), "-117.6366");
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("altitude"), "133");
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("accuracy"), "0");
    BOOST_CHECK_CLOSE(signal1.attr<double>("avg_speed"), 19, 0.1);
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("direction"), "116");
    BOOST_CHECK_EQUAL(signal1.attr<std::string>("time"), "06052018:211603");
}

BOOST_AUTO_TEST_SUITE_END()
