#define BOOST_TEST_MAIN

#include <maps/analyzer/services/jams_analyzer/modules/usershandler/tests/test_tools/include/test_tools.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/track.h>

#include <boost/test/included/unit_test.hpp>

#include <maps/analyzer/libs/track_generator/include/gpssignal_creator.h>
#include <maps/libs/deprecated/boost_time/utils.h>
#include <maps/libs/geolib/include/point.h>

using maps::analyzer::data::GpsSignal;
using maps::analyzer::VehicleId;
using maps::analyzer::track_generator::GpsSignalCreator;
using maps::geolib3::Point2;

void checkBroken(const Track& track)
{
    BOOST_CHECK(track.isBroken());
    BOOST_CHECK_EQUAL(track.numMatchedSegments(), 0);
    BOOST_CHECK_EQUAL(track.startTime(), boost::posix_time::not_a_date_time);
}

BOOST_AUTO_TEST_CASE( CheckTrack )
{
    GpsSignalCreator signalFactory;

    Track track;
    checkBroken(track);

    VehicleId id("0", "1");
    Point2 p2(0.0, 0.0);
    boost::posix_time::ptime ctime = maps::nowUtc();
    GpsSignal signalA = signalFactory.createSignal(
            id, p2, 0.0, 0, ctime, ctime);
    GpsSignal signalB = signalFactory.createSignal(
            id, p2, 0.0, 0, ctime + boost::posix_time::seconds(10),
            ctime + boost::posix_time::seconds(10));
    GpsSignal signalC = signalFactory.createSignal(
            id, p2, 0.0, 0, ctime + boost::posix_time::seconds(20),
            ctime + boost::posix_time::seconds(20));
    GpsSignal signalD = signalFactory.createSignal(
            id, p2, 0.0, 0, ctime + boost::posix_time::seconds(30),
            ctime + boost::posix_time::seconds(30));

    auto safePathA = std::pair(signalA, false);
    auto safePathB = std::pair(signalB, false);
    auto brokenPath = std::pair(signalA, true);

    track.proceed(brokenPath.first, brokenPath.second);//1
    checkBroken(track);

    track.proceed(safePathA.first, safePathA.second);//2
    track.setNumMatchedSegments(track.numMatchedSegments() + 1);
    BOOST_CHECK(!track.isBroken());
    BOOST_CHECK_EQUAL(track.numMatchedSegments(), 1);
    BOOST_CHECK_EQUAL(track.startTime(), ctime);

    track.proceed(safePathB.first, safePathB.second);//3
    track.setNumMatchedSegments(track.numMatchedSegments() + 1);
    BOOST_CHECK(!track.isBroken());
    BOOST_CHECK_EQUAL(track.numMatchedSegments(), 2);
    BOOST_CHECK_EQUAL(track.startTime(), ctime);

    track.proceed(brokenPath.first, brokenPath.second);//4
    checkBroken(track);

    track.proceed(safePathB.first, safePathB.second);//5
    track.setNumMatchedSegments(track.numMatchedSegments() + 1);
    BOOST_CHECK(!track.isBroken());
    BOOST_CHECK_EQUAL(track.numMatchedSegments(), 1);
    BOOST_CHECK_EQUAL(track.startTime(), ctime
            + boost::posix_time::seconds(10));
}
