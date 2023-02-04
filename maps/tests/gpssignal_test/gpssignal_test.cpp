#include "test_tools.h"

#include <library/cpp/testing/gtest/gtest.h>

#include <maps/analyzer/libs/data/include/gpssignal.h>
#include <maps/analyzer/libs/data/include/gpssignal_comparators.h>

#include <boost/date_time/posix_time/posix_time.hpp>

#include <string>

namespace ma = maps::analyzer;
namespace mad = maps::analyzer::data;
namespace pt = boost::posix_time;
namespace greg = boost::gregorian;

using std::string;
using boost::posix_time::ptime;


TEST(GpsSignalTests, CheckGpsSignal) {
    mad::GpsSignal s;

    ptime time = maps::fromTimestamp(3910);
    s.setTime(time);
    EXPECT_EQ(s.time(), time);

    ptime receiveTime = maps::fromTimestamp(3911);
    s.setReceiveTime(receiveTime);
    EXPECT_EQ(s.receiveTime(), receiveTime);

    ptime dispatcherReceiveTime = maps::fromTimestamp(3911);
    EXPECT_TRUE(!s.hasDispatcherReceiveTime());

    s.setDispatcherReceiveTime(dispatcherReceiveTime);
    EXPECT_TRUE(s.hasDispatcherReceiveTime());
    EXPECT_EQ(s.dispatcherReceiveTime(), dispatcherReceiveTime);

    double lon = 12.0;
    s.setLon(lon);
    EXPECT_EQ(s.lon(), lon);

    double lat = 13.0;
    s.setLat(lat);
    EXPECT_EQ(s.lat(), lat);

    double direction = 15.0;
    s.setDirection(direction);
    EXPECT_EQ(s.direction(), direction);

    double averageSpeed = 16.0;
    s.setAverageSpeed(averageSpeed);
    EXPECT_EQ(s.averageSpeed(), averageSpeed);

    ma::VehicleId vehicleId("14", "13");
    s.setVehicleId(vehicleId);
    EXPECT_EQ(s.vehicleId(), vehicleId);

    s.setSlowVehicle(true); EXPECT_TRUE(s.isSlowVehicle());
    s.setSlowVehicle(false); EXPECT_TRUE(!s.isSlowVehicle());

    std::string clid = "29";
    s.setClid(clid); EXPECT_EQ(s.clid(), clid);

    std::string uuid = "49";
    s.setUuid(uuid); EXPECT_EQ(s.uuid(), uuid);

    size_t regionId = 497;
    s.setRegionId(regionId); EXPECT_EQ(s.regionId(), regionId);

    size_t speedCategory = 2734;
    EXPECT_TRUE(!s.speedCategory());
    s.setSpeedCategory(speedCategory);
    EXPECT_TRUE(s.speedCategory());
    EXPECT_EQ(*s.speedCategory(), speedCategory);

}

TEST(GpsSignalTests, CheckGpsSignalComparators) {
    GpsSignalFactory factory;
    ptime ctime = maps::nowUtc();
    mad::GpsSignal signal1 = factory.createSignal("123", "421", ctime - pt::seconds(2));
    mad::GpsSignal signal2 = factory.createSignal("123", "321", ctime);
    mad::GpsSignal signal3 = factory.createSignal("123", "421", ctime);
    mad::GpsSignal signal4 = factory.createSignal("124", "221", ctime);
    mad::GpsSignalTimeLess timeLess;
    EXPECT_TRUE(timeLess(signal1, signal2));
    EXPECT_TRUE(!timeLess(signal2, signal1));
    EXPECT_TRUE(!timeLess(signal1, signal1));

    mad::GpsSignalTimeVehicleIdLess timeVehicleIdLess;
    EXPECT_TRUE(timeVehicleIdLess(signal1, signal2));
    EXPECT_TRUE(timeVehicleIdLess(signal1, signal3));
    EXPECT_TRUE(timeVehicleIdLess(signal1, signal4));
    EXPECT_TRUE(!timeVehicleIdLess(signal2, signal1));
    EXPECT_TRUE(!timeVehicleIdLess(signal1, signal1));

    EXPECT_TRUE(timeVehicleIdLess(signal2, signal3));
    EXPECT_TRUE(timeVehicleIdLess(signal2, signal4));
    EXPECT_TRUE(timeVehicleIdLess(signal3, signal4));

    EXPECT_TRUE(!timeVehicleIdLess(signal3, signal2));
    EXPECT_TRUE(!timeVehicleIdLess(signal4, signal2));
    EXPECT_TRUE(!timeVehicleIdLess(signal4, signal3));
}
