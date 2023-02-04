#define BOOST_TEST_MAIN
#include <boost/test/auto_unit_test.hpp>

#include <maps/analyzer/libs/gpssignal_parser/include/gpssignal_parser.h>
#include <maps/libs/geolib/include/bounding_box.h>
#include <maps/libs/geolib/include/test_tools/io_operations.h>

#include <boost/test/test_tools.hpp>
#include <boost/assign/std/vector.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <fstream>

using namespace std;
using namespace maps::geolib3;
using namespace boost::assign;
using namespace maps::analyzer;
using namespace maps::analyzer::data;

string signalData(string fname)
{
    ifstream in(fname.c_str());
    string s, res;
    while (getline(in, s)) {
        res += s + "\n";
    }
    return res;
}

void checkTestXml(GpsSignals gpsSignals)
{
    BOOST_CHECK_EQUAL(gpsSignals.size(), 6);
    for(size_t i = 0; i < gpsSignals.size(); ++i) {
        BOOST_CHECK_EQUAL(gpsSignals[i].clid(), "1");
        //there are three groups of two points with uuid 1, 2 and 3
        BOOST_CHECK_EQUAL(gpsSignals[i].uuid(), string(1, '1' + i / 2));
        //first group is slow and other two is not slow
        BOOST_CHECK_EQUAL(gpsSignals[i].isSlowVehicle(), i / 2 == 0);
    }
}

BOOST_AUTO_TEST_CASE(test_xml_parser) {
    vector<string> fields, values;
    GpsSignals gpsSignals;
    gpsSignals.clear();
    xml::parseSignalsFromFile("test.xml", &gpsSignals);
    checkTestXml(gpsSignals);

    gpsSignals.clear();
    xml::parseSignalsFromString(signalData("test.xml"), &gpsSignals);
    checkTestXml(gpsSignals);

    gpsSignals.clear();
    ifstream input("test.xml");
    xml::parseSignalsFromStream(input, &gpsSignals);
    checkTestXml(gpsSignals);
}

BOOST_AUTO_TEST_CASE(test_parser_throw) {
    vector<string> fields, values;

    // Kill output of libxml to stderr
    ASSERT(freopen("/dev/null", "w", stderr));
    GpsSignals gpsSignals;
    BOOST_CHECK_THROW(xml::parseSignalsFromString(
                signalData("test_incorrect1.xml"), &gpsSignals),
            maps::Exception);
    //no <?xml ..> header is ok
    BOOST_CHECK_NO_THROW(xml::parseSignalsFromString(
                signalData("test_incorrect2.xml"), &gpsSignals));
    BOOST_CHECK_THROW(xml::parseSignalsFromString(
                signalData("test_incorrect3.xml"), &gpsSignals),
            maps::Exception);
    BOOST_CHECK_THROW(xml::parseSignalsFromString(
                signalData("test_incorrect4.xml"), &gpsSignals),
            maps::Exception);
}

void checkTestGpx(GpsSignals gpsSignals)
{
    BOOST_CHECK_EQUAL(gpsSignals.size(), 5);
    using boost::posix_time::seconds;
    using boost::posix_time::ptime;
    ptime start = boost::posix_time::from_iso_string("20101010T000000");
    for(size_t i = 0; i < 3; ++i) {
        //clid and uuid is not specified, so it should be empty
        BOOST_CHECK_EQUAL(gpsSignals[i].clid(), "");
        BOOST_CHECK_EQUAL(gpsSignals[i].uuid(), "");
        //veicle not slow
        BOOST_CHECK(!gpsSignals[i].isSlowVehicle());

        BOOST_CHECK_EQUAL(gpsSignals[i].lon(), i + 2.0);
        BOOST_CHECK_EQUAL(gpsSignals[i].lat(), i + 1.0);
        BOOST_CHECK_EQUAL(gpsSignals[i].averageSpeed(), 11.0 * (i + 1.0));
        BOOST_CHECK_EQUAL(gpsSignals[i].time(), start +
                seconds(i * 3600 * 24));
        BOOST_CHECK_EQUAL(gpsSignals[i].direction(), 0.0);
    }
    BOOST_CHECK_EQUAL(gpsSignals[3].averageSpeed(), 0.0);
    BOOST_CHECK_EQUAL(gpsSignals[4].direction(), 33.0);
}

void checkGpxFile(const string& file)
{
    GpsSignals gpsSignals;
    gpsSignals.clear();
    gpx::parseSignalsFromFile(file, &gpsSignals);
    checkTestGpx(gpsSignals);

    gpsSignals.clear();
    gpx::parseSignalsFromString(signalData(file), &gpsSignals);
    checkTestGpx(gpsSignals);

    gpsSignals.clear();
    ifstream input(file.c_str());
    gpx::parseSignalsFromStream(input, &gpsSignals);
    checkTestGpx(gpsSignals);
}

BOOST_AUTO_TEST_CASE(test_gpx_parser)
{
    checkGpxFile("test.gpx");
    checkGpxFile("test_1_0.gpx");
    checkGpxFile("test_wo_namespace.gpx");
}

BOOST_AUTO_TEST_CASE(test_gpx_parser_traffic_color)
{
    GpsSignals gpsSignals;
    gpx::parseSignalsFromFile("test_traffic_color.gpx", &gpsSignals);
    for(size_t i = 0; i < 3; ++i) {
        BOOST_CHECK(gpsSignals[i].speedCategory());
        BOOST_CHECK_EQUAL(*gpsSignals[i].speedCategory(), i);
    }
    BOOST_CHECK(!gpsSignals[3].speedCategory());

    // Kill output of libxml to stderr
    ASSERT(freopen("/dev/null", "w", stderr));
    BOOST_CHECK_THROW(
        gpx::parseSignalsFromFile("test_traffic_color_incorrect.gpx",
            &gpsSignals),
        maps::Exception
    );
}

BOOST_AUTO_TEST_CASE(test_gpx_parser_incorrect_course)
{
    GpsSignals gpsSignals;
    gpsSignals.clear();
    gpx::parseSignalsFromFile("test_special_dir.gpx", &gpsSignals);
    BOOST_CHECK_EQUAL(gpsSignals.size(), 3);
    for(size_t i = 0; i < gpsSignals.size(); ++i) {
        BOOST_CHECK_EQUAL(gpsSignals[i].direction(), 0);
    }
}

void checkConversions(const std::string& requestTime, const std::string& isoTime)
{
    namespace pt = boost::posix_time;
    BOOST_CHECK_EQUAL(requestTime,
                      toRequestString(parseTimeFromRequest(requestTime)));
    BOOST_CHECK_EQUAL(pt::from_iso_string(isoTime),
                      parseTimeFromRequest(requestTime));
    BOOST_CHECK_EQUAL(toRequestString(pt::from_iso_string(isoTime)),
                      requestTime);
}

BOOST_AUTO_TEST_CASE(test_time_conversions)
{
    checkConversions("13032012:101004", "20120313T101004");
    checkConversions("03122010:003004", "20101203T003004");
}
