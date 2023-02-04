#pragma once

#include <maps/indoor/libs/db/include/radiomap_transmitter.h>
#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/lib/include/types.h>

#include <map>
#include <set>
#include <vector>

namespace maps::mirc::radiomap_evaluator::tests {

using Transmitter = db::ugc::Transmitter;
using Transmitters = std::vector<Transmitter>;
using TransmitterIds = std::set<TransmitterId>;
using TransmittersById = std::map<TransmitterId, db::ugc::Transmitter>;
using TransmittersAtLevels = std::map<IndoorLevelId, TransmitterIds>;
using TransmittersAtLevelsById = std::map<IndoorLevelId, TransmittersById>;

using RSSI = int;
using RssiByTxId = std::unordered_map<TransmitterId, RSSI>;

class IndoorModel {
public:
    IndoorModel(IndoorLevelIds);

    TransmitterIds makeTransmitters(const IndoorLevelId&, size_t);
    void makeTransmittersVisibleAtLevels(const TransmitterIds&, const IndoorLevelIds&);

    const IndoorPlanId& indoorPlanId() const { return planId_; }
    const IndoorLevelIds& indoorLevelIds() const { return levels_; }

    const geolib3::Point2& geoPoint() const { return geoPoint_; }
    double xSize() const { return xSize_; }
    double ySize() const { return ySize_; }

    const TransmittersById& allTransmitters() const { return transmittersById_; }
    TransmittersById transmittersVisibleAtLevel(const IndoorLevelId&) const;
    RssiByTxId generateModelSignal(const geolib3::Point2& geoPoint, const IndoorLevelId&) const;

private:

    double getLevelHeight(const IndoorLevelId&) const;
    double getVerticalDistanceBetweenLevels(const IndoorLevelId&, const IndoorLevelId&) const;

    IndoorPlanId planId_;
    IndoorLevelIds levels_;

    geolib3::Point2 geoPoint_;
    // Indoor width and length in local coordinates in meters:
    double xSize_;
    double ySize_;

    TransmittersById transmittersById_;
    TransmittersAtLevels transmittersAtLevels_;
};

} // namespace maps::mirc::radiomap_evaluator::tests
