#pragma once

#include <yandex/maps/wiki/routing/common.h>
#include <yandex/maps/wiki/routing/exception.h>
#include <yandex/maps/wiki/routing/route.h>
#include <yandex/maps/wiki/routing/stop.h>

#include <yandex/maps/wiki/common/string_utils.h>

#include <ostream>
#include <string>

namespace maps {
namespace wiki {
namespace routing {
namespace tests {

template<typename T>
std::string print(const T& value)
{
    std::stringstream out;
    out << value;
    return out.str();
}

template<typename TCollection>
std::string printCollection(const TCollection& collection)
{
    return "[" + common::join(collection, print<typename TCollection::value_type>, ", ")  + "]";
}

template<>
std::string print(const Point& point);

template<>
std::string print(const Direction& direction);

template<>
std::string print(const DirectedElementID& directedElementID);

template<>
std::string print(const TracePoint& point);

template<>
std::string print(const Trace& trace);

template<>
std::string print(const Stop& stop);

template<>
std::string print(const ImpossibleSnapStopError& error);

template<>
std::string print(const AmbiguousPathError& error);

template<>
std::string print(const NoPathError& error);

} // namespace tests
} // namespace routing
} // namespace wiki
} // namespace maps
