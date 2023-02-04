#pragma once
#include <yandex/maps/mapkit/directions/driving/route.h>
#include <yandex/maps/mapkit/geometry/ostream_helpers.h>
#include <yandex/maps/mapkit/geometry/tools.h>

namespace yandex::maps::mapkit {

inline bool operator==(const RequestPoint& l, const RequestPoint& r) {
    return l.point == r.point
           && l.type == r.type
           && l.pointContext == r.pointContext;
}

inline std::ostream& operator<<(std::ostream& os, const RequestPoint& p) {
    os << "(" << p.point << ", ";
    switch (p.type) {
        case RequestPointType::Viapoint:
            os << "via, ";
            break;
        case RequestPointType::Waypoint:
            os << "way, ";
            break;
    }
    if (p.pointContext) {
        os << "pctx=" << *p.pointContext;
    }
    os << ")";
    return os;
}

} // namespace yandex::maps::mapkit
