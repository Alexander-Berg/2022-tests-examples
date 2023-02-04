#pragma once

#include <maps/analyzer/libs/common/include/types.h>
#include <maps/analyzer/libs/data/include/gpssignal.h>
#include <maps/libs/deprecated/boost_time/utils.h>
#include <maps/libs/road_graph/include/types.h>

#include <optional>
#include <ostream>
#include <istream>

namespace std {

template <typename T>
ostream& operator<< (ostream& ostr, const optional<T>& value) {
    if (!value) {
        return ostr << "nullopt";
    }
    return ostr << "{" << value.value() << "}";
}

istream& operator>> (istream& istr, maps::road_graph::SegmentId& seg) {
    return istr >> seg.edgeId.value() >> std::ws >> seg.segmentIndex.value();
}

ostream& operator<< (ostream& ostr, const maps::analyzer::shortest_path::LongSegmentId& seg) {
    return ostr << seg.edgeId << " " << seg.segmentIndex;
}

} // std

class GpsSignalFactory
{
public:
    maps::analyzer::data::GpsSignal createSignal(
        const std::string& clid,
        const std::string& uuid,
        const boost::posix_time::ptime& time)
    {
        return createSignal(clid, uuid, 51.13, 32.14, 56.18, 200, time,
            maps::nowUtc());
    }

    maps::analyzer::data::GpsSignal createSignal(
        const std::string& clid,
        const std::string& uuid,
        double lat,
        double lon,
        double speed,
        int direction,
        const boost::posix_time::ptime& time,
        const boost::posix_time::ptime& timeReceive)
    {
        maps::analyzer::data::GpsSignal signal;
        signal.setLat(lat);
        signal.setVehicleId(maps::analyzer::VehicleId(clid, uuid));
        signal.setLon(lon);
        signal.setAverageSpeed(speed);
        signal.setDirection(direction);
        signal.setTime(time);
        signal.setReceiveTime(timeReceive);
        return signal;
    }
};
