#include <boost/test/unit_test.hpp>

#include <maps/indoor/libs/indoor_positioning/include/device_capabilities.h>
#include <maps/indoor/libs/indoor_positioning/include/indoor_positioning_client.h>
#include <maps/indoor/libs/indoor_positioning/include/level_index.h>
#include <maps/indoor/libs/indoor_positioning/include/sensor.h>
#include <maps/indoor/libs/indoor_positioning/include/transmitter.h>
#include <maps/libs/log8/include/log8.h>

#include "geometry.h"

using namespace boost::unit_test;
using namespace INDOOR_POSITIONING_NAMESPACE;

namespace {

const auto TX_TYPE = TransmitterType::BEACON;

const auto DEVICE_CAPABILITIES = DeviceCapabilities{
    .hasAccelerometer = false,
    .hasGyroscope = false,
    .hasMagnetometer = false,
    .hasBarometer = false,
    .hasLocation = false,
    .hasBeacons = true,
    .hasWifi = false,
    .hasBle = false,
    .hasWifiThrottling = false};

struct TestLevelSelector {
    TestLevelSelector()
        : transmitters({
            {"tx1", TX_TYPE, {"plan", "1"}, {30.000, 40.000}, TransmitterRssiModel()},
            {"tx2", TX_TYPE, {"plan", "2"}, {30.000, 40.000}, TransmitterRssiModel()}})
        , levelIndex(createLevelIndex(transmitters))
    {
        client = createIndoorPositioningClient(levelIndex, DEVICE_CAPABILITIES);

        Timestamp timestamp;

        // Creating measurements for 300 seconds interval
        const double rssiMax = -20;
        const double rssiMin = -90;
        const unsigned int maxCount = 300; // 300 seconds

        for(unsigned int count = 0; count <= maxCount; ++count) {
            double rssi1 = (rssiMin * count + rssiMax * (maxCount - count)) / maxCount;
            double rssi2 = (rssiMax * count + rssiMin * (maxCount - count)) / maxCount;
            TransmitterMeasurements txMsr;
            txMsr.push_back({"tx1", TransmitterType::BEACON, rssi1, timestamp});
            txMsr.push_back({"tx2", TransmitterType::BEACON, rssi2, timestamp});
            txMsrs.emplace_back(std::move(txMsr));
            timestamp += std::chrono::milliseconds(1000);
        }
    }

    Transmitters transmitters;
    std::shared_ptr<LevelIndex> levelIndex;
    std::shared_ptr<IndoorPositioningClient> client;
    std::vector<TransmitterMeasurements> txMsrs;
};

} // namespace

BOOST_AUTO_TEST_CASE(indoor_level_selector)
{
    TestLevelSelector test;

    int counterL1 = 0;
    int counterL2 = 0;
    const int totalCount = test.txMsrs.size();

    // How quickly (in seconds) should the level be switched from 1 to 2
    // when the signal from the tx2-beacon becomes stronger than
    // the signal from the tx1-beacon.
    const int levelSwitchTimeThreshold = 40;

    Timestamp timestamp;
    test.client->position({}, {}, timestamp);

    for(const auto& txMsr : test.txMsrs) {
        timestamp += std::chrono::milliseconds(1000);
        auto position = test.client->position(txMsr, {}, timestamp);
        if (position) {
            // Level should be always determined
            BOOST_CHECK(
                position->indoorLevelId == "1" ||
                position->indoorLevelId == "2");

            if (position->indoorLevelId == "1") {
                BOOST_CHECK_EQUAL(counterL2, 0);
                ++counterL1;
            }
            else if (position->indoorLevelId == "2") {
                ++counterL2;
            }
        }
    }

    INFO() << "indoor_level_selector:: positioning attempts: " << totalCount;
    INFO() << "indoor_level_selector:: level 1: " << counterL1;
    INFO() << "indoor_level_selector:: level 2: " << counterL2;

    BOOST_CHECK_EQUAL(counterL1 + counterL2, totalCount);
    BOOST_CHECK(totalCount/2 <= counterL1 && counterL1 <= totalCount/2 + levelSwitchTimeThreshold);
    BOOST_CHECK(totalCount/2 - levelSwitchTimeThreshold <= counterL2 && counterL2 <= totalCount/2);
}
