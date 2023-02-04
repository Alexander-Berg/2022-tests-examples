#define BOOST_TEST_MAIN

#include <maps/analyzer/services/jams_analyzer/modules/usershandler/tests/test_tools/include/test_tools.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/vehicle_info.h>

#include <maps/analyzer/libs/track_generator/include/gpssignal_creator.h>
#include <maps/libs/geolib/include/point.h>
#include <boost/test/included/unit_test.hpp>
#include <boost/test/data/test_case.hpp>
#include <set>
#include <vector>

using boost::posix_time::ptime;
using boost::posix_time::not_a_date_time;
namespace pt = boost::posix_time;
using boost::posix_time::hours;
using boost::unit_test::test_suite;
using std::set;
using std::unique_ptr;
using maps::analyzer::data::GpsSignal;
using maps::analyzer::VehicleId;
using maps::analyzer::track_generator::GpsSignalCreator;

//____________________________________________________________________________//

BOOST_AUTO_TEST_SUITE( VehicleInfoTestSuite )

//Checks that empty vehicle info works correct
BOOST_AUTO_TEST_CASE( CheckEmpty )
{
    VehicleId vehicleId("0", "1");
    VehicleInfo info(vehicleId, 10);

    BOOST_CHECK_EQUAL(info.lastHistoryTime(), not_a_date_time);
    BOOST_CHECK_EQUAL(info.storeTime(), 10);
    BOOST_CHECK_EQUAL(info.vehicleId(), vehicleId);
}

//____________________________________________________________________________//

//Checks that history work correct
BOOST_AUTO_TEST_CASE( CheckHistory )
{
    VehicleId vehicleId("0", "1");
    GpsSignalCreator gpsSignalFactory(vehicleId);
    VehicleInfo info(vehicleId, 10);
    ptime ctime = maps::nowUtc();

    //Too old signal, which will put to history,
    //because history is time independed.
    GpsSignal signalOld = gpsSignalFactory.createSignal(ctime - pt::seconds(12));
    BOOST_CHECK_EQUAL(info.addToHistory(signalOld), VehicleInfo::ADDED);
    BOOST_CHECK(info.isInHistory(signalOld));
    BOOST_CHECK(info.lastHistoryTime() == ctime - pt::seconds(12));

    GpsSignal signalA = gpsSignalFactory.createSignal(ctime);

    BOOST_CHECK_EQUAL(info.addToHistory(signalA), VehicleInfo::ADDED);

    // Old signal deleted
    BOOST_CHECK(!info.isInHistory(signalOld));
    //Duplicates not added
    BOOST_CHECK_EQUAL(info.addToHistory(signalA), VehicleInfo::DUPLICATED);
    BOOST_CHECK(info.lastHistoryTime() == signalA.time());

    GpsSignal signalB = gpsSignalFactory.createSignal(ctime - pt::seconds(2));

    BOOST_CHECK_EQUAL(info.addToHistory(signalB),
            VehicleInfo::NEWER_IN_HISTORY);
    //Not added signalB
    BOOST_CHECK(!info.isInHistory(signalB));
    //last time not changed
    BOOST_CHECK(info.lastHistoryTime() == signalA.time());

    GpsSignal signalC = gpsSignalFactory.createSignal(ctime + pt::seconds(2));

    BOOST_CHECK_EQUAL(info.addToHistory(signalC), VehicleInfo::ADDED);
    //last time changed
    BOOST_CHECK(info.lastHistoryTime() == signalC.time());

    BOOST_CHECK(!info.isInHistory(signalOld));
    BOOST_CHECK(info.isInHistory(signalA));
    BOOST_CHECK(!info.isInHistory(signalB));
    BOOST_CHECK(info.isInHistory(signalC));

    BOOST_CHECK_EQUAL(info.addToHistory(signalOld), VehicleInfo::NEWER_IN_HISTORY);
    BOOST_CHECK(!info.isInHistory(signalOld));
}

//____________________________________________________________________________//

BOOST_AUTO_TEST_SUITE_END()
