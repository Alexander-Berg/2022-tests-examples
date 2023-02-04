#include "test_tools.h"

#include <library/cpp/testing/gtest/gtest.h>

#include <maps/analyzer/libs/data/include/log_message.h>
#include <maps/libs/road_graph/include/types.h>

#include <sstream>
#include <string>


using std::string;
using std::ostringstream;
using boost::posix_time::ptime;
using boost::posix_time::from_iso_string;

using maps::road_graph::EdgeId;
using maps::road_graph::SegmentId;
using maps::road_graph::SegmentIndex;
namespace ma = maps::analyzer;
namespace mad = maps::analyzer::data;

TEST(LogMessageSuite, LogMessage) {
    mad::LogMessage m;
    ostringstream testFormat;

    m << "privet " << 5;
    testFormat << "privet {%s}";
    EXPECT_EQ(m.format(), testFormat.str());
    EXPECT_EQ(m.valuesNumber(), 1ul);
    EXPECT_EQ(m.value(0), "5");
    EXPECT_THROW(m.value(1), std::out_of_range);

    m << 15.0;
    testFormat << "{%s}";
    EXPECT_EQ(m.valuesNumber(), 2ul);
    EXPECT_EQ(m.value(1), "15");

    SegmentId segmentId{EdgeId{5}, SegmentIndex{7}};
    m << segmentId;
    testFormat << "{%SegmentId}";
    EXPECT_EQ(m.format(), testFormat.str());
    EXPECT_EQ(m.valuesNumber(), 3ul);
    EXPECT_EQ(boost::lexical_cast<SegmentId>(m.value(2)), segmentId);

    ma::VehicleId vehicleId("5", "7");
    m << vehicleId;
    testFormat << "{%VehicleId}";
    EXPECT_EQ(m.format(), testFormat.str());
    EXPECT_EQ(m.valuesNumber(), 4ul);
    EXPECT_EQ(
        boost::lexical_cast<ma::VehicleId>(m.value(3)),
        vehicleId
    );

    ptime testTime(from_iso_string("20111010T000000"));
    m << testTime;
    testFormat << "{%ptime}";
    EXPECT_EQ(m.format(), testFormat.str());
    EXPECT_EQ(m.valuesNumber(), 5ul);
    EXPECT_EQ(m.value(4), boost::lexical_cast<string>(testTime));
    EXPECT_THROW(m.value(5), std::out_of_range);

    std::string testString("teststring");
    m << testString;
    testFormat << "{%s}";
    EXPECT_EQ(m.format(), testFormat.str());
    EXPECT_EQ(m.valuesNumber(), 6ul);
    EXPECT_EQ(boost::lexical_cast<string>(m.value(5)), testString);
    EXPECT_THROW(m.value(6), std::out_of_range);

    mad::GpsSignal signal;
    signal.setClid("123");
    signal.setLon(0);
    signal.setLat(0);
    m << signal;
    std::string serrializedSignal;
    signal.serializeToString(&serrializedSignal);
    testFormat << "{%GpsSignalData}";
    EXPECT_EQ(m.format(), testFormat.str());
    EXPECT_EQ(m.valuesNumber(), 7ul);
    EXPECT_EQ(m.value(6), serrializedSignal);
    EXPECT_THROW(m.value(7), std::out_of_range);
}
