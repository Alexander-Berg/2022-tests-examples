#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/lib/include/radiomap_evaluator.h>
#include <maps/indoor/libs/db/include/radiomap_meta_gateway.h>
#include <maps/indoor/libs/db/include/radiomap_transmitter_gateway.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include "maps/indoor/libs/unittest/fixture.h"

namespace maps::mirc::radiomap_evaluator::tests {
using namespace ::testing;

namespace {

std::string mockData(const std::string& /* key */)
{
    return std::string();
}

class RadioMapEvaluatorTest : public RadioMapEvaluator
{
public:
    RadioMapEvaluatorTest(
        pgpool3::Pool& pool)
        : RadioMapEvaluator(
            pool,
            std::make_unique<unittest::MockS3Storage>(mockData),
            nullptr,
            nullptr)
        , indoorPlanId_("1")
    {
        txStates_.emplace_back(TransmitterState{
            .txType = TransmitterType::Ble,
            .levelStates = {{"0", TransmitterLevelState{
                .rssiPositions = {},
                .position = IndoorPosition{{0.1, 0.1}, "0"},
                .valueA = 0.,
                .valueB = 0.,
                .rssiDeviation = 0.}
            }},
            .transmitterId = "00:00:00:00:00:00",
        });
        txStates_.emplace_back(TransmitterState{
            .txType = TransmitterType::Ble,
            .levelStates = {{"0", TransmitterLevelState{
                .rssiPositions = {},
                .position = IndoorPosition{{0.2, 0.2}, "0"},
                .valueA = 0.,
                .valueB = 0.,
                .rssiDeviation = 0.}
            }},
            .transmitterId = "00:00:00:00:00:11",
        });
        txStates_.emplace_back(TransmitterState{
            .txType = TransmitterType::Ble,
            .levelStates = {{"1", TransmitterLevelState{
                .rssiPositions = {},
                .position = IndoorPosition{{1.1, 1.1}, "1"},
                .valueA = 1.,
                .valueB = 1.,
                .rssiDeviation = 0.}
            }},
            .transmitterId = "00:00:00:00:11:00",
        });
        txStates_.emplace_back(TransmitterState{
            .txType = TransmitterType::Ble,
            .levelStates = {{"1", TransmitterLevelState{
                .rssiPositions = {},
                .position = IndoorPosition{{1.2, 1.2}, "1"},
                .valueA = 1.,
                .valueB = 1.,
                .rssiDeviation = 0.}
            }},
            .transmitterId = "00:00:00:00:11:11",
        });
        txStates_.emplace_back(TransmitterState{
            .txType = TransmitterType::Ble,
            .levelStates = {{"2", TransmitterLevelState{
                .rssiPositions = {},
                .position = IndoorPosition{{2.1, 2.1}, "2"},
                .valueA = 2.,
                .valueB = 2.,
                .rssiDeviation = 0.}
            }},
            .transmitterId = "00:00:00:00:22:00",
        });
        txStates_.emplace_back(TransmitterState{
            .txType = TransmitterType::Ble,
            .levelStates = {{"2", TransmitterLevelState{
                .rssiPositions = {},
                .position = IndoorPosition{{2.2, 2.2}, "2"},
                .valueA = 2.,
                .valueB = 2.,
                .rssiDeviation = 0.}
            }},
            .transmitterId = "00:00:00:00:22:11",
        });
    }

    bool saveData()
    {
        return RadioMapEvaluator::save(createRadiomap());
    }

    const std::string& indoorPlanId()
    {
        return indoorPlanId_;
    }

    const TxStates& txStates()
    {
        return txStates_;
    }

private:
    RadioMap createRadiomap()
    {
        return RadioMap{
            indoorPlanId_, // indoorPlanId
            geolib3::Point2{0, 0}, // originPoint
            {}, // trajectory
            {}, // measurements
            {}, // outdoor measurements
            txStates_};
    }

    const std::string indoorPlanId_;
    TxStates txStates_;
};

}

Y_UNIT_TEST_SUITE_F(radiomap_save_test, unittest::Fixture) {

Y_UNIT_TEST(radiomap_double_save_test)
{
    pgpool3::Pool& pool = pgPool();
    auto evaluator = RadioMapEvaluatorTest(pool);

    auto txn = pool.slaveTransaction();

    ASSERT_EQ(evaluator.saveData(), true);
    auto firstRadiomapMeta = db::ugc::RadiomapMetaGateway{*txn}.load(
        db::ugc::table::RadiomapMeta::indoorPlanId == evaluator.indoorPlanId());

    ASSERT_EQ(firstRadiomapMeta.size(), 1u);

    auto transmitters = db::ugc::TransmitterGateway{*txn}.load(
        db::ugc::table::Transmitter::indoorPlanId == evaluator.indoorPlanId());

    ASSERT_EQ(transmitters.size(), evaluator.txStates().size());

    ASSERT_EQ(evaluator.saveData(), false);
    auto secondRadiomapMeta = db::ugc::RadiomapMetaGateway{*txn}.load(
        db::ugc::table::RadiomapMeta::indoorPlanId == evaluator.indoorPlanId());
    ASSERT_EQ(secondRadiomapMeta.size(), 1u);

    transmitters = db::ugc::TransmitterGateway{*txn}.load(
        db::ugc::table::Transmitter::indoorPlanId == evaluator.indoorPlanId());

    ASSERT_EQ(transmitters.size(), evaluator.txStates().size());

    ASSERT_EQ(firstRadiomapMeta.front().hash(), secondRadiomapMeta.front().hash());
}

} // Y_UNIT_TEST_SUITE_F

} // namespace maps::mirc::radiomap_evaluator::tests
