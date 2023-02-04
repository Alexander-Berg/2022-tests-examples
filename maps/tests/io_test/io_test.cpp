#define BOOST_TEST_MAIN

#include <maps/analyzer/libs/data/include/gpssignal.h>
#include <maps/analyzer/libs/common/include/io_helpers.h>

#include <maps/libs/geolib/include/test_tools/test_tools.h>
#include <maps/libs/geolib/include/test_tools/pseudo_random.h>

#include <maps/libs/common/include/profiletimer.h>

#include <boost/test/unit_test.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/tuple/tuple.hpp>
#include <boost/tuple/tuple_io.hpp>
#include <boost/tuple/tuple_comparison.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/assign/std/vector.hpp>

#include <sstream>
#include <typeinfo>

namespace ma = maps::analyzer;
namespace mad = maps::analyzer::data;
namespace gtt = maps::geolib3::test_tools;
namespace pt = boost::posix_time;
namespace greg = boost::gregorian;
namespace proto = yandex::maps::proto::analyzer;

namespace maps {
namespace geolib3 {
    using io::operator <<;
} //namespace maps::geolib3
} //namespace maps

template<size_t N>
void testFixedInteger(int integer, const std::string& correct)
{
    std::ostringstream oss;
    ma::writeFixedInteger(oss, integer, N);
    BOOST_CHECK_EQUAL(oss.str(), correct);
}

BOOST_AUTO_TEST_CASE( fixed_integer_tests )
{
    testFixedInteger<5>(1543, "01543");
    testFixedInteger<5>(43, "00043");
    testFixedInteger<2>(0, "00");
    testFixedInteger<2>(1, "01");
    testFixedInteger<2>(1543, "1543");
}

void testDuration(const pt::time_duration& tm, const std::string& expected)
{
    BOOST_CHECK_EQUAL(ma::serializeDuration(tm), expected);
    BOOST_CHECK_EQUAL(ma::parseDuration(expected), tm);
}

BOOST_AUTO_TEST_CASE( time_duration_tests )
{
    testDuration(pt::time_duration(-15, 4, 3), "-15:04:03");
    testDuration(pt::time_duration(-101, 8, 0), "-101:08:00");
    testDuration(pt::time_duration(0, 0, 0), "00:00:00");
    testDuration(pt::time_duration(15, 43, 43), "15:43:43");
    testDuration(pt::time_duration(1543, 7, 1), "1543:07:01");
    testDuration(pt::time_duration(0, 1, 5, 43), "00:01:05.000043");
    testDuration(pt::time_duration(256, 18, 3, 654321), "256:18:03.654321");

    BOOST_CHECK_THROW(ma::parseDuration("00:00:0x"), std::exception);
    BOOST_CHECK_THROW(ma::parseDuration("00:00:0"), std::exception);
    BOOST_CHECK_THROW(ma::parseDuration("00:1:43"), std::exception);
    BOOST_CHECK_THROW(ma::parseDuration("00x15:43"), std::exception);
    BOOST_CHECK_THROW(ma::parseDuration("1x43:00:00"), std::exception);
    BOOST_CHECK_THROW(ma::parseDuration("00:15:43."), std::exception);
    BOOST_CHECK_THROW(ma::parseDuration("00:15:43.123"), std::exception);
    BOOST_CHECK_THROW(ma::parseDuration("00:15:43.12453"), std::exception);
    BOOST_CHECK_THROW(ma::parseDuration("00:15:43.12345678"), std::exception);
}

// Tests IO for object wrappers with known fixed string representation.
void testSignal(const mad::GpsSignal& signal)
{
    gtt::ValueChecker<gtt::EmptyGeometry> checker((gtt::EmptyGeometry()));
    checker.setPrecision(1E-14);

    const mad::GpsSignal newSignal = mad::GpsSignal::createFromData(
        ma::parseProtobuf<proto::GpsSignalData>(ma::serializeProtobuf(signal.data()))
    );

    BOOST_CHECK(checker.check("point", signal.point(), newSignal.point()));
    BOOST_CHECK(checker.check("time", signal.time(), newSignal.time()));
    BOOST_CHECK(checker.check("receive time",
        signal.receiveTime(), newSignal.receiveTime()));
    BOOST_CHECK(checker.check("dispatcher receive time",
        signal.dispatcherReceiveTime(), newSignal.dispatcherReceiveTime()));
    BOOST_CHECK(checker.check("vehicle id",
        signal.vehicleId(), newSignal.vehicleId()));
    BOOST_CHECK(checker.check("direction",
        signal.direction(), newSignal.direction()));
    BOOST_CHECK(checker.check("average speed",
        signal.averageSpeed(), newSignal.averageSpeed()));
    BOOST_CHECK(checker.check("region id",
        signal.regionId(), newSignal.regionId()));
    BOOST_CHECK(checker.check("is vehicle slow",
        signal.isSlowVehicle(), newSignal.isSlowVehicle()));
}

BOOST_AUTO_TEST_CASE(gps_signal_tests)
{
    mad::GpsSignal signal;
    signal.setLon(15.0);
    signal.setLat(43.0);
    testSignal(signal);

    signal.setTime(pt::ptime(
        greg::date(2010, 3, 1), pt::time_duration(0, 0, 0)));
    signal.setReceiveTime(pt::ptime(
        greg::date(2010, 3, 1), pt::time_duration(0, 5, 0)));
    signal.setDispatcherReceiveTime(pt::ptime(
        greg::date(2010, 3, 1), pt::time_duration(0, 5, 0)));
    testSignal(signal);

    signal.setClid("clid");
    signal.setUuid("uuid");
    signal.setDirection(90);
    signal.setAverageSpeed(60.0);
    testSignal(signal);

    signal.setSlowVehicle(false);
    signal.setRegionId(1);
    testSignal(signal);
}

void testTimePeriod(const pt::time_period& tm, const std::string& expected)
{
    BOOST_CHECK_EQUAL(ma::serializeTimePeriod(tm), expected);
    BOOST_CHECK_EQUAL(ma::parseTimePeriod(expected), tm);
}

BOOST_AUTO_TEST_CASE(time_period_test)
{
    testTimePeriod(
        pt::time_period(
            pt::ptime(greg::date(2010, 3, 1), pt::time_duration(0, 0, 0)),
            pt::ptime(greg::date(2010, 3, 1), pt::time_duration(0, 5, 0))
        ),
        "20100301T000000 20100301T000500"
    );
    testTimePeriod(
        pt::time_period(
            pt::ptime(greg::date(2010, 3, 10), pt::time_duration(10, 0, 0)),
            pt::ptime(greg::date(2011, 6, 1), pt::time_duration(0, 5, 0))
        ),
        "20100310T100000 20110601T000500"
    );
}

template<class Time, class Fn>
void testWritePerformace(const Time& time, const Fn& fn, size_t nIterations,
    double maxWorkingSeconds)
{
    std::ostringstream oss;
    ProfileTimer timer;
    for (size_t i = 0; i < nIterations; ++i) {
        oss << fn(time) << " ";
    }
    const double workingSeconds = boost::lexical_cast<double>(
        timer.getElapsedTime());
    BOOST_CHECK_PREDICATE(std::less<double>(),
        (workingSeconds)(maxWorkingSeconds));
}

template<class Time, class Fn>
void testReadPerformace(const std::string& time, const Fn& fn, size_t nIterations,
    double maxWorkingSeconds)
{
    std::string manyTimeStrings;
    for (size_t i = 0; i < nIterations; ++i) {
        manyTimeStrings += (time + " ");
    }
    std::ostringstream oss;
    std::istringstream iss(manyTimeStrings);
    ProfileTimer timer;
    std::string strTime;
    Time tm;
    for (size_t i = 0; i < nIterations; ++i) {
        iss >> strTime;
        tm = fn(strTime);
    }
    // prevent from optimizing, need to use the variable
    oss << tm;
    const double workingSeconds = boost::lexical_cast<double>(
        timer.getElapsedTime());
    BOOST_CHECK_PREDICATE(std::less<double>(),
        (workingSeconds)(maxWorkingSeconds));
}

BOOST_AUTO_TEST_CASE( time_io_performance_test )
{
#ifdef NDEBUG
    const double TIME_MAX_WRITE = 8.0;
    const double TIME_MAX_READ = 5.0;
    const double DURATION_MAX_WRITE = 1.0;
    const double DURATION_MAX_READ = 1.0;
#else
    const double TIME_MAX_WRITE = 24.0;
    const double TIME_MAX_READ = 12.0;
    const double DURATION_MAX_WRITE = 3.0;
    const double DURATION_MAX_READ = 2.0;
#endif

    testWritePerformace(
        pt::ptime(greg::date(2010, 3, 1), pt::time_duration(0, 5, 0)),
        [] (const pt::ptime& tm) { return pt::to_iso_string(tm); },
        1000000,
        TIME_MAX_WRITE
    );
    testWritePerformace(
        pt::time_duration(15, 43, 0),
        [] (const pt::time_duration& tm) { return ma::serializeDuration(tm); },
        1000000,
        DURATION_MAX_WRITE
    );

    testReadPerformace<pt::ptime>(
        "20100301T000500",
        [] (const std::string& s) { return pt::from_iso_string(s); },
        1000000,
        TIME_MAX_READ
    );
    testReadPerformace<pt::time_duration>(
        "15:43:00",
        [] (const std::string& s) { return ma::parseDuration(s); },
        1000000,
        DURATION_MAX_READ
    );
}

