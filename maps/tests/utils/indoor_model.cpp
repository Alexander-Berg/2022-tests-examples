#include "indoor_model.h"

#include "utils.h"

#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/lib/impl/utils.h>
#include <maps/libs/geolib/include/distance.h>

#include <algorithm>


namespace maps::mirc::radiomap_evaluator::tests {

namespace {

const db::ugc::TransmitterType TEST_TRANSMITTER_TYPE = TransmitterType::Ble;

Transmitter makeTransmitter(
    const IndoorModel& indoor,
    const IndoorLevelId& level)
{
    const auto txId = toUpperCase(generateUUID());
    const double txX = getRand(0, indoor.xSize());
    const double txY = getRand(0, indoor.ySize());
    const double txA = getRand(-20, -90);
    const double txB = getRand(1, 20);
    const double rssiDeviation = getRand(1, 10);
    const std::optional<double> rssiThreshold;

    const auto signalModel = db::ugc::SignalModelParameters{txA, txB, rssiDeviation, rssiThreshold};
    const auto geodeticPoint = localPlanarToGeodetic(geolib3::Point2(txX, txY), indoor.geoPoint());

    return Transmitter{
        indoor.indoorPlanId(),
        level,
        TEST_TRANSMITTER_TYPE,
        txId,
        geodeticPoint,
        signalModel,
        false
    };
}

} // namespace

IndoorModel::IndoorModel(IndoorLevelIds levels) :
    planId_(generateUUID()),
    levels_(levels),
    geoPoint_({getRand(30.0, 60.0), getRand(30.0, 60.0)}),
    xSize_(100),
    ySize_(100)
    {
        REQUIRE(levels_.size() > 0, "0 levels indoor has no sense");
        for (const auto& level : levels_) {
            const auto [_, ok] = transmittersAtLevels_.try_emplace(level);
            REQUIRE(ok, "Duplicates in indoor level ids");
        }
    }


TransmittersById IndoorModel::transmittersVisibleAtLevel(const IndoorLevelId& level) const {
    TransmittersById result;
    for (const auto& txId : transmittersAtLevels_.at(level)) {
        result.emplace(std::make_pair(txId, transmittersById_.at(txId)));
    }
    return result;
}

double IndoorModel::getLevelHeight(const IndoorLevelId& level) const
{
    const double LEVEL_HEIGHT = 5.0; // [meters] between levels.

    for (size_t i = 0; i < levels_.size(); ++i) {
        if (level == levels_.at(i)) {
            return i * LEVEL_HEIGHT;
        }
    }

    throw RuntimeError() << "Unexpected IndoorLevelId: '" << level << "'";
}

double IndoorModel::getVerticalDistanceBetweenLevels(
    const IndoorLevelId& someLevelId,
    const IndoorLevelId& anotherLevelId) const
{
    return std::abs(getLevelHeight(someLevelId) - getLevelHeight(anotherLevelId));
}

TransmitterIds IndoorModel::makeTransmitters(
    const IndoorLevelId& level,
    const size_t count)
{
    REQUIRE(std::find(levels_.begin(), levels_.end(), level) != levels_.end(), "Unexpected level");
    TransmitterIds newIds;

    auto& visibleTxs = transmittersAtLevels_[level];
    for (size_t i = 0; i < count; ++i) {
        auto tx = makeTransmitter(*this, level);

        newIds.insert(tx.txId());
        visibleTxs.insert(tx.txId());

        transmittersById_.emplace(std::make_pair(tx.txId(), std::move(tx)));
    }

    return newIds;
}

void IndoorModel::makeTransmittersVisibleAtLevels(const TransmitterIds& txIds, const IndoorLevelIds& levels) {
    for (const auto& level : levels) {
        REQUIRE(std::find(levels_.begin(), levels_.end(), level) != levels_.end(), "Unknown level id");

        for (const auto& txId : txIds) {
            REQUIRE(transmittersById_.contains(txId), "Unknown transmitter id");

            transmittersAtLevels_[level].insert(txId);
        }
    }
}

RssiByTxId IndoorModel::generateModelSignal(
    const geolib3::Point2& geoPoint,
    const IndoorLevelId& pathLevelId) const
{
    RssiByTxId result;

    const auto& visibleIds = transmittersAtLevels_.at(pathLevelId);
    for (const auto& txId : visibleIds) {
        const auto& tx = transmittersById_.at(txId);

        const double horizontalDistance = geolib3::geoDistance(tx.geodeticPoint(), geoPoint);
        const double verticalDistance = getVerticalDistanceBetweenLevels(tx.indoorLevelId(), pathLevelId);

        const double r = std::sqrt(std::pow(horizontalDistance, 2) + std::pow(verticalDistance, 2));
        auto rssi = round(tx.signalParams().a - tx.signalParams().b * std::log(r));

        result[txId] = rssi;
    }

    return result;
}

} // namespace maps::mirc::radiomap_evaluator::tests
