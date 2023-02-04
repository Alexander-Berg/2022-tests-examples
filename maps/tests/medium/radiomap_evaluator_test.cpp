#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/lib/include/radiomap_evaluator.h>

#include <maps/indoor/libs/db/include/radiomap_transmitter_gateway.h>
#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/lib/impl/experimental_levels.h>
#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/lib/impl/utils.h>
#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/tests/utils/indoor_model.h>
#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/tests/utils/prepare_db.h>

#include "maps/indoor/libs/unittest/fixture.h"

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <algorithm>
#include <memory>

namespace maps::mirc::radiomap_evaluator::tests {
using namespace ::testing;

namespace {

TransmittersById loadTransmitters(const IndoorPlanId& planId, const IndoorLevelId& level, pgpool3::Pool& pool)
{
    auto txn = pool.slaveTransaction();
    const auto transmitters = db::ugc::TransmitterGateway{*txn}.load(
        db::ugc::table::Transmitter::indoorLevelId == level &&
        db::ugc::table::Transmitter::indoorPlanId == planId
    );

    TransmittersById restoredTransmitters;
    for(const auto& transmitter : transmitters) {
        const auto [_, ok] = restoredTransmitters
            .try_emplace(transmitter.txId(), transmitter);
        REQUIRE(ok, "Duplicate transmitter id: '" << transmitter.txId() << "'");
    }
    return restoredTransmitters;
}

std::unique_ptr<unittest::MockS3Storage> prepareDbAndS3(
    const std::vector<IndoorModel>& indoors,
    pgpool3::Pool& pool)
{
    auto taskPathsTracks = prepareDataBase(indoors, pool);
    return std::make_unique<unittest::MockS3Storage>(
            [tracks = std::move(taskPathsTracks)] (const TaskPathTrackId& id) {
                return tracks.at(id);
            }
        );
}

} // namespace


Y_UNIT_TEST_SUITE_F(radiomap_evaluator_tests, unittest::Fixture) {

Y_UNIT_TEST(check_transmitters_visible_at_another_level_are_restored_at_own_level)
{
    const auto indoor = [] { // Indoor with transmitters only on 0-level.
        IndoorModel indoor({"0", "1"});
        const auto TXIDS_FROM_0_LEVEL = indoor.makeTransmitters(indoor.indoorLevelIds().at(0), 100);
        indoor.makeTransmittersVisibleAtLevels(TXIDS_FROM_0_LEVEL, indoor.indoorLevelIds());
        return indoor;
    }();

    auto& pool = pgPool();

    {
        auto mockS3Storage = prepareDbAndS3({indoor}, pool);
        auto evaluator = RadioMapEvaluator(pool, std::move(mockS3Storage), nullptr, nullptr);
        evaluator.evaluate();
    }

    { // Check transmitters from 0-level are restored to 0-level.
        const auto& level = indoor.indoorLevelIds().at(0);

        const double expected = indoor.transmittersVisibleAtLevel(level).size();
        const double restored = loadTransmitters(indoor.indoorPlanId(), level, pool).size();

        const double missingPercent = 100.0 * (expected - restored) / expected;
        EXPECT_LT(missingPercent, 2.0);
    }
}

Y_UNIT_TEST(check_transmitters_visible_at_another_level_are_restored_at_another_level)
{
    const auto indoor = [] { // Indoor with transmitters only on 0-level.
        IndoorModel indoor({"0", "1"});
        const auto TXIDS_FROM_0_LEVEL = indoor.makeTransmitters(indoor.indoorLevelIds().at(0), 100);
        indoor.makeTransmittersVisibleAtLevels(TXIDS_FROM_0_LEVEL, indoor.indoorLevelIds());
        return indoor;
    }();

    auto& pool = pgPool();

    {
        auto mockS3Storage = prepareDbAndS3({indoor}, pool);
        auto evaluator = RadioMapEvaluator(pool, std::move(mockS3Storage), nullptr, nullptr);
        evaluator.evaluate();
    }

    { // Check transmitters from 0-level visible at 1st level are restored to 1-level.
        const auto& level = indoor.indoorLevelIds().at(1);
        const double expected = indoor.transmittersVisibleAtLevel(level).size();
        const double restored = loadTransmitters(indoor.indoorPlanId(), level, pool).size();

        const double missingPercent = 100.0 * (expected - restored) / expected;
        EXPECT_LT(missingPercent, 5.0);
    }
}

} // Y_UNIT_TEST_SUITE_F

Y_UNIT_TEST_SUITE_F(static_transmitters_evaluation_tests, unittest::Fixture) {

Y_UNIT_TEST(single_task_static_transmitters_count)
{
    auto& pool = pgPool();
    const std::string LEVEL_ID = "1a";

    const auto [indoor, STATIC_ID] = [&LEVEL_ID, &pool] () {
        IndoorModel indoor({LEVEL_ID});
        indoor.makeTransmitters(LEVEL_ID, 10);

        // Make one of the transmitters static.
        const auto& [txId, tx] = *(indoor.allTransmitters().begin());
        const auto& staticTransmitter = makeStaticTxAndWriteToStaticTxsTable(tx, pool);

        return std::make_tuple(indoor, staticTransmitter.txId());
    }();

    {
        auto mockS3 = prepareDbAndS3({indoor}, pool);
        RadioMapEvaluator evaluator{pool, std::move(mockS3), nullptr, nullptr};
        evaluator.evaluate();
    }

    {
        const auto expected = indoor.transmittersVisibleAtLevel(LEVEL_ID);
        const auto actual = loadTransmitters(indoor.indoorPlanId(), LEVEL_ID, pool);
        EXPECT_EQ(expected.size(), actual.size());

        const auto staticIdCount = std::count_if(actual.begin(), actual.end(),
            [&id = STATIC_ID] (const auto& el) { return el.first == id; });
        EXPECT_EQ(staticIdCount, 1u);
    }
}

Y_UNIT_TEST(two_tasks_check_static_transmitter_count)
{
    auto& pool = pgPool();
    const std::string LEVEL_ID = "1a";

    const auto [indoorWithStatic, STATIC_ID] = [&LEVEL_ID, &pool] () {
        IndoorModel indoor({LEVEL_ID});
        indoor.makeTransmitters(LEVEL_ID, 10);

        // Make one of the transmitters static.
        const auto& [txId, tx] = *(indoor.allTransmitters().begin());
        const auto& staticTransmitter = makeStaticTxAndWriteToStaticTxsTable(tx, pool);

        return std::make_tuple(indoor, staticTransmitter.txId());
    }();

    const auto indoorWithOutStatic = []() {
        IndoorModel indoor({"some_level", "another_level"});
        const auto ids = indoor.makeTransmitters("some_level", 17);
        return indoor;
    }();

    {
        auto mockS3 = prepareDbAndS3({indoorWithStatic, indoorWithOutStatic}, pool);
        RadioMapEvaluator evaluator{pool, std::move(mockS3), nullptr, nullptr};
        evaluator.evaluate();
    }

    {
        const auto expected = indoorWithStatic.transmittersVisibleAtLevel(LEVEL_ID);
        const auto actual = loadTransmitters(indoorWithStatic.indoorPlanId(), LEVEL_ID, pool);

        EXPECT_EQ(expected.size(), actual.size());

        const auto staticCount = std::count_if(actual.begin(), actual.end(),
            [&id = STATIC_ID] (const auto& el) { return el.first == id; });
        EXPECT_EQ(staticCount, 1u);
    }
}

} // Y_UNIT_TEST_SUITE_F

} // namespace maps::mirc::radiomap_evaluator::tests
