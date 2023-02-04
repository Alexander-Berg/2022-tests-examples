#include <maps/analyzer/libs/data/tests/include/test_tools.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <maps/analyzer/libs/data/include/masstransit_signal.h>

#include <string>

namespace data = maps::analyzer::data;

TEST(MasstransitSignalTests, MasstransitSignal) {
    GpsSignalFactory factory;
    data::GpsSignal gpsSignal = factory.createSignal("tst_clid", "test_uuid", maps::nowUtc());

    data::MasstransitSignal mtSignal {gpsSignal, "test_route"};

    EXPECT_EQ(mtSignal.signal().debugString(), gpsSignal.debugString());
    EXPECT_EQ(mtSignal.route(), "test_route");

    mtSignal.setRoute("another_route");
    EXPECT_EQ(mtSignal.route(), "another_route");
}
