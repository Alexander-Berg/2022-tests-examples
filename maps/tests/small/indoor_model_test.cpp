#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/tests/utils/indoor_model.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::mirc::radiomap_evaluator::tests {

void checkIndoor(
    const IndoorModel& indoor,
    const std::unordered_map<IndoorLevelId, size_t>& expectedCount,
    const size_t expectedTxsCount)
{
    for (size_t i = 0; i < indoor.indoorLevelIds().size(); ++i) {
        const auto& level = indoor.indoorLevelIds().at(i);
        ASSERT_EQ(expectedCount.at(level), indoor.transmittersVisibleAtLevel(level).size());
    }

    std::unordered_set<TransmitterId> ids;
    for (const auto& [id, _] : indoor.allTransmitters()) {
        ids.insert(id);
    }

    ASSERT_EQ(expectedTxsCount, ids.size());
}

Y_UNIT_TEST_SUITE(indoor_model) {

Y_UNIT_TEST(constructor)
{
    ASSERT_THROW(IndoorModel({}), maps::RuntimeError);
    ASSERT_THROW(IndoorModel({"1", "1"}), maps::RuntimeError);
    ASSERT_NO_THROW(IndoorModel({"1", "2"}));
}

Y_UNIT_TEST(make_transmitters)
{
    IndoorModel indoor({"0", "1", "2"});
    size_t expectedTxsCount = 0;
    std::unordered_map<IndoorLevelId, size_t> expectedVisibleTxsCountByLevel;
    for (const auto& level : indoor.indoorLevelIds()) {
        expectedVisibleTxsCountByLevel[level] = 0;
    }

    checkIndoor(indoor, expectedVisibleTxsCountByLevel, expectedTxsCount);

    EXPECT_THROW(indoor.makeTransmitters("Non-existent level", 42), maps::RuntimeError);
    checkIndoor(indoor, expectedVisibleTxsCountByLevel, expectedTxsCount);

    EXPECT_NO_THROW(indoor.makeTransmitters(indoor.indoorLevelIds().at(0), 0));
    checkIndoor(indoor, expectedVisibleTxsCountByLevel, expectedTxsCount);

    {
        const auto& level = indoor.indoorLevelIds().at(0);
        const auto ids = indoor.makeTransmitters(level, 1);
        expectedVisibleTxsCountByLevel[level] += 1;
        expectedTxsCount += 1;
        EXPECT_EQ(ids.size(), 1u);
        checkIndoor(indoor, expectedVisibleTxsCountByLevel, expectedTxsCount);
    }

    {
        const auto& level = indoor.indoorLevelIds().at(2);
        const auto ids = indoor.makeTransmitters(level, 2);
        expectedVisibleTxsCountByLevel[level] += 2;
        expectedTxsCount += 2;
        EXPECT_EQ(ids.size(), 2u);
        checkIndoor(indoor, expectedVisibleTxsCountByLevel, expectedTxsCount);
    }
}

Y_UNIT_TEST(make_transmitters_visible_at_other_levels_throw)
{
    IndoorModel indoor({"0", "1", "2"});

    const auto TXIDS_1_LEVEL = indoor.makeTransmitters(indoor.indoorLevelIds().at(1), 1);
    const auto TXIDS_2_LEVEL = indoor.makeTransmitters(indoor.indoorLevelIds().at(2), 2);

    EXPECT_THROW(
        indoor.makeTransmittersVisibleAtLevels(
            {(*TXIDS_1_LEVEL.begin()), "Non-existent txId"},
            {indoor.indoorLevelIds()}),
        maps::RuntimeError
    );

    EXPECT_THROW(
        indoor.makeTransmittersVisibleAtLevels(
            TXIDS_2_LEVEL,
            {indoor.indoorLevelIds().at(0), "Non-existent level"}),
        maps::RuntimeError
    );
}

Y_UNIT_TEST(make_transmitters_visible_at_other_levels)
{
    IndoorModel indoor({"0", "1", "2"});
    size_t expectedTxsCount = 0;
    std::unordered_map<IndoorLevelId, size_t> expectedVisibleTxsCountByLevel;
    for (const auto& level : indoor.indoorLevelIds()) {
        expectedVisibleTxsCountByLevel[level];
    }

    const auto TXIDS_1_LEVEL = indoor.makeTransmitters(indoor.indoorLevelIds().at(1), 1);
    const auto TXIDS_2_LEVEL = indoor.makeTransmitters(indoor.indoorLevelIds().at(2), 2);

    expectedTxsCount += TXIDS_1_LEVEL.size();
    expectedTxsCount += TXIDS_2_LEVEL.size();
    expectedVisibleTxsCountByLevel[indoor.indoorLevelIds().at(1)] += TXIDS_1_LEVEL.size();
    expectedVisibleTxsCountByLevel[indoor.indoorLevelIds().at(2)] += TXIDS_2_LEVEL.size();

    {   // Making 0 transmitters visible at all levels.
        ASSERT_NO_THROW(
            indoor.makeTransmittersVisibleAtLevels({}, indoor.indoorLevelIds())
        );
        checkIndoor(indoor, expectedVisibleTxsCountByLevel, expectedTxsCount);
    }

    {   // Add from level 2 to 1 level.
        indoor.makeTransmittersVisibleAtLevels(TXIDS_2_LEVEL, {indoor.indoorLevelIds().at(1)});
        expectedVisibleTxsCountByLevel[indoor.indoorLevelIds().at(1)] += TXIDS_2_LEVEL.size();
        checkIndoor(indoor, expectedVisibleTxsCountByLevel, expectedTxsCount);

        // And add again to check that nothing changes.
        indoor.makeTransmittersVisibleAtLevels(TXIDS_2_LEVEL, {indoor.indoorLevelIds().at(1)});
        checkIndoor(indoor, expectedVisibleTxsCountByLevel, expectedTxsCount);
    }

    {   // Check that making txs visible at levels, where they are already visible, changes nothing.
        indoor.makeTransmittersVisibleAtLevels(
            TXIDS_2_LEVEL,
            {indoor.indoorLevelIds().at(1), indoor.indoorLevelIds().at(2)}
        );
        checkIndoor(indoor, expectedVisibleTxsCountByLevel, expectedTxsCount);
    }

    {   // Check that creating new transmitters after
        //  some were already created and made visible on other levels
        //  works as usually.
        indoor.makeTransmitters(
            indoor.indoorLevelIds().at(0),
            3
        );
        expectedVisibleTxsCountByLevel[indoor.indoorLevelIds().at(0)] += 3;
        expectedTxsCount += 3;
        checkIndoor(indoor, expectedVisibleTxsCountByLevel, expectedTxsCount);
    }

    {
        indoor.makeTransmittersVisibleAtLevels(
            TXIDS_1_LEVEL,
            {indoor.indoorLevelIds().at(0)}
        );
        expectedVisibleTxsCountByLevel[indoor.indoorLevelIds().at(0)] += TXIDS_1_LEVEL.size();
        checkIndoor(indoor, expectedVisibleTxsCountByLevel, expectedTxsCount);
    }
}

Y_UNIT_TEST(generate_model_signal_from_all_visible_transmitters)
{
    IndoorModel indoor({"0", "1", "2"});

    const auto TXIDS_1_LEVEL = indoor.makeTransmitters(indoor.indoorLevelIds().at(1), 1);
    const auto TXIDS_2_LEVEL = indoor.makeTransmitters(indoor.indoorLevelIds().at(2), 2);
    indoor.makeTransmittersVisibleAtLevels(TXIDS_2_LEVEL, {indoor.indoorLevelIds().at(1)});

    {
        const auto signals = indoor.generateModelSignal(indoor.geoPoint(), indoor.indoorLevelIds().at(0));
        ASSERT_TRUE(signals.empty());
    }
    for (size_t i = 0; i < indoor.indoorLevelIds().size(); ++i) {
        const auto& level = indoor.indoorLevelIds().at(i);
        const auto signals = indoor.generateModelSignal(indoor.geoPoint(), level);

        const auto visibleTxs = indoor.transmittersVisibleAtLevel(level);
        ASSERT_EQ(visibleTxs.size(), signals.size());
        for (const auto& [txId, _] : visibleTxs) {
            ASSERT_TRUE(signals.contains(txId));
        }
    }
}

Y_UNIT_TEST(generate_model_signal_monotonically_fades_on_other_levels)
{
    IndoorModel indoor({"TRANSMITTER_LEVEL", "1", "2", "4"});

    const auto TXIDS = indoor.makeTransmitters("TRANSMITTER_LEVEL", 1);
    indoor.makeTransmittersVisibleAtLevels(TXIDS, indoor.indoorLevelIds());

    ASSERT_EQ(TXIDS.size(), 1u);
    const auto& TXID = *(TXIDS.begin());
    const auto transmitter = indoor.allTransmitters().at(TXID);

    const double SMALL_SHIFT = 1e-6;
    const geolib3::Point2 MEASUREMENT_POINT{
        // The shift is needed not to make measurement right at transmitter
        transmitter.geodeticPoint().x() + SMALL_SHIFT,
        transmitter.geodeticPoint().y() + SMALL_SHIFT
    };

    std::vector<RSSI> signals;
    for (size_t i = 0; i < indoor.indoorLevelIds().size(); ++i) {
        const auto& level = indoor.indoorLevelIds().at(i);
        const auto signal = indoor.generateModelSignal(MEASUREMENT_POINT, level);
        ASSERT_EQ(signal.size(), 1u);
        signals.push_back(signal.at(TXID));
    }

    for (size_t i = 1; i < signals.size(); ++i) {
        EXPECT_GT(signals.at(i-1), signals.at(i));
    }
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::mirc::radiomap_evaluator::tests
