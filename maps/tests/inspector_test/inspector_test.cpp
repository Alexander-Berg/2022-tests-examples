#define BOOST_TEST_MAIN
#include "../common/init_coverage.h"

#include <signal/inspector.h>
#include <config.h>

#include <library/cpp/testing/common/env.h>

using std::string;
using std::auto_ptr;
using namespace boost::posix_time;
using maps::geoinfo::GeoId;
using maps::analyzer::data::GpsSignal;

string timeToString(ptime time)
{
    std::stringstream str;
    str.imbue(std::locale(
                std::locale::classic(),
                new time_facet("%d%m%Y:%H%M%S")));
    str << time;
    return str.str();
}

SignalData getSignal()
{
    SignalData signal;
    signal.setAttr("clid",      "1");
    signal.setAttr("uuid",      "5");
    signal.setAttr("direction", "1");
    signal.setAttr("avg_speed", "20.0"); // MPS
    signal.setAttr("longitude", "37.48");
    signal.setAttr("latitude",  "55.78");
    signal.setAttr("time", timeToString(maps::nowUtc() - boost::posix_time::seconds(5)));
    return signal;
}

BOOST_FIXTURE_TEST_SUITE(AvailableCoverageTestSuite, CopyCoverageFiles)

BOOST_AUTO_TEST_CASE(check_fix_direction)
{
    Config config("common_config.xml", "config.xml");
    GeoId geoId = getGeoIdTrf();
    const Inspector inspector(&config, geoId);
    SignalData signal = getSignal();
    InspectionContext context;

    inspector.inspect(&signal, &context);
    BOOST_CHECK(signal.status().accepted());
    BOOST_CHECK_EQUAL(context.badSignalsCount(), 0);
    BOOST_CHECK_EQUAL(context.reasons(), "");

    signal.setAttr("direction", "723");
    inspector.inspect(&signal, &context);
    BOOST_CHECK(signal.status().accepted());
    BOOST_CHECK_EQUAL(context.badSignalsCount(), 1);
    BOOST_CHECK_EQUAL(context.reasons(),
            " direction out of range");
}

BOOST_AUTO_TEST_CASE(check_banned)
{
    Config config("common_config.xml", "config.xml");
    GeoId geoId = getGeoIdTrf();
    const Inspector inspector(&config, geoId);
    SignalData signal = getSignal();
    InspectionContext context;

    signal.setAttr("clid", "someclid");
    inspector.inspect(&signal, &context);
    BOOST_CHECK(!signal.status().accepted());
    BOOST_CHECK_EQUAL(context.badSignalsCount(), 0);
    BOOST_CHECK_EQUAL(context.reasons(), " banned");
}

BOOST_AUTO_TEST_CASE(check_not_coverage)
{
    Config config("common_config.xml", "config.xml");
    GeoId geoId = getGeoIdTrf();
    const Inspector inspector(&config, geoId);
    SignalData signal = getSignal();
    InspectionContext context;

    signal.setAttr("longitude", "5.0");
    inspector.inspect(&signal, &context);
    BOOST_CHECK(!signal.status().accepted());
    BOOST_CHECK_EQUAL(context.badSignalsCount(), 0);
    BOOST_CHECK_EQUAL(context.reasons(), " not in coverage");
}

BOOST_AUTO_TEST_CASE(check_additional_parameters)
{
    Config config("common_config.xml", "config.xml");
    GeoId geoId = getGeoIdTrf();
    const Inspector inspector(&config, geoId);
    SignalData signal = getSignal();
    InspectionContext context;

    inspector.inspect(&signal, &context);
    BOOST_CHECK(signal.status().accepted());
    BOOST_CHECK_EQUAL(signal.attr<int>("region_id"), 213);
}

// moved from gpsSignal to dispatcher
BOOST_AUTO_TEST_CASE(check_sanity_inclorrect_id_test)
{
    SignalData signal = getSignal();

    signal.setAttr("clid", "");
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::INCORRECT_CLID);

    signal.setAttr("clid", "1!23");
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::INCORRECT_CLID);
    signal.setAttr("clid", "123");

    signal.setAttr("uuid", "1!23");
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::INCORRECT_UUID);
    signal.setAttr("uuid", "123");

    signal.setAttr("clid", string('a', 33));
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::INCORRECT_CLID);
    signal.setAttr("clid", "123");

    signal.setAttr("uuid", string('a', 33));
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::INCORRECT_UUID);
}

BOOST_AUTO_TEST_CASE(check_sanity_test)
{
    SignalData signal = getSignal();

    signal.setAttr("region_id", "20001");
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::NOT_IN_COVERAGE);

    signal.setAttr("region_id", "1");
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::CORRECT_SIGNAL);

    signal.setAttr("longitude", "180.13");
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::LON_OUT_OF_RANGE);

    signal.setAttr("longitude", "-180.13");
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::LON_OUT_OF_RANGE);
    signal.setAttr("longitude", "51.13");

    signal.setAttr("latitude", "90.34");
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::LAT_OUT_OF_RANGE);

    signal.setAttr("latitude", "-90.34");
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::LAT_OUT_OF_RANGE);
    signal.setAttr("latitude", "52.34");

    signal.setAttr("avg_speed", "61.1");
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::SPEED_OUT_OF_RANGE);

    signal.setAttr("avg_speed", "-1.1");
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::SPEED_OUT_OF_RANGE);
    signal.setAttr("avg_speed", "38.1");

    signal.setAttr("direction", "370");
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
            GpsSignal::DIRECTION_OUT_OF_RANGE);

    signal.setAttr("direction", "-1");
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
            GpsSignal::DIRECTION_OUT_OF_RANGE);
    signal.setAttr("direction", "100");

    signal.setAttr("time", timeToString(maps::nowUtc() - minutes(10)));
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::TIME_OUT_OF_RANGE);

    signal.setAttr("time", timeToString(maps::nowUtc() + boost::posix_time::seconds(25)));
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::CORRECT_SIGNAL);

    signal.setAttr("time", timeToString(maps::nowUtc() + boost::posix_time::seconds(30)));
    BOOST_CHECK_EQUAL(Inspector::checkSanity(signal, 300),
                      GpsSignal::TIME_OUT_OF_RANGE);
}

BOOST_AUTO_TEST_CASE(no_speed_test)
{
    Config config("common_config.xml", "config.xml");
    GeoId geoId = getGeoIdTrf();
    const Inspector inspector(&config, geoId);

    {
        SignalData signal;
        signal.setAttr("clid",      "auto");
        signal.setAttr("uuid",      "5");
        signal.setAttr("direction", "1");
        signal.setAttr("longitude", "37.48");
        signal.setAttr("latitude",  "55.78");
        signal.setAttr("time", timeToString(maps::nowUtc() - boost::posix_time::seconds(5)));

        InspectionContext context;
        inspector.inspect(&signal, &context);
        BOOST_CHECK(signal.status().accepted());
     }
     {
        SignalData signal;
        signal.setAttr("clid",      "auto");
        signal.setAttr("uuid",      "5");
        signal.setAttr("avg_speed", "");
        signal.setAttr("direction", "1");
        signal.setAttr("longitude", "37.48");
        signal.setAttr("latitude",  "55.78");
        signal.setAttr("time", timeToString(maps::nowUtc() - boost::posix_time::seconds(5)));

        InspectionContext context;
        inspector.inspect(&signal, &context);
        BOOST_CHECK(signal.status().accepted());
    }
    {
        SignalData signal;
        signal.setAttr("clid",      "1");
        signal.setAttr("uuid",      "5");
        signal.setAttr("direction", "1");
        signal.setAttr("longitude", "37.48");
        signal.setAttr("latitude",  "55.78");
        signal.setAttr("time", timeToString(maps::nowUtc() - boost::posix_time::seconds(5)));

        InspectionContext context;
        BOOST_CHECK_THROW(
            inspector.inspect(&signal, &context),
            yacare::errors::BadRequest
        );
    }
}

BOOST_AUTO_TEST_SUITE_END()
