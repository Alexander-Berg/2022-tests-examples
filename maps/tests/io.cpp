#include "io.h"

#include "magic_string.h"

#include <ostream>

namespace maps {
namespace wiki {
namespace routing {
namespace tests {

template<>
std::string print(const Point& point)
{
    std::stringstream out;
    out << "[" << point.x() << ", " << point.y() << "]";
    return out.str();
}

template<>
std::string print(const Direction& direction)
{
    switch (direction) {
        case Direction::Forward:
            return STR_FORWARD;
        case Direction::Backward:
            return STR_BACKWARD;
        default:
            return STR_BOTH;
    }
}

template<>
std::string print(const DirectedElementID& directedElementId)
{
    std::stringstream out;
    out << directedElementId.id() << "." << print(directedElementId.direction());
    return out.str();
}

template<>
std::string print(const TracePoint& point)
{
    std::stringstream out;

    out << "TracePoint(directedElementId=" << print(point.directedElementId());
    if (point.stopSnap()) {
        const auto& stopSnap = *point.stopSnap();
        out << ", stopSnap=("
            << "stopId=" << stopSnap.stopId() << ", "
            << "locationOnElement=" << stopSnap.locationOnElement() << ", "
            << "point=" << print(stopSnap.point())
            << ")";
    }
    out << ")";

    return out.str();
}


template<>
std::string print(const Stop& stop)
{
    std::stringstream out;

    out << "Stop("
        << "id=" << stop.id() << ", "
        << "geom=" << print(stop.geom())
        << ")";

    return out.str();
}

template<>
std::string print(const ImpossibleSnapStopError& error)
{
    std::stringstream out;

    out << "ImpossibleSnapStopError("
        << "stopIds=" << printCollection(error.stopIds())
        << ")";

    return out.str();

}

template<>
std::string print(const AmbiguousPathError& error)
{
    std::stringstream out;

    out << "AmbiguousPathError("
        << "fromStopId=" << error.fromStopId() << ", "
        << "toStopId=" << error.toStopId() << ", "
        << "elementId " << error.elementId()
         << ")";

     return out.str();
}

template<>
std::string print(const NoPathError& error)
{
    std::stringstream out;

    out << "NoPathError("
        << "fromStopId=" << error.fromStopId() << ", "
        << "toStopId=" << error.toStopId()
         << ")";

     return out.str();
}

} // namespace tests
} // namespace routing
} // namespace wiki
} // namespace maps
