#pragma once

#include <yandex/maps/wiki/routing/common.h>
#include <yandex/maps/wiki/routing/condition.h>
#include <yandex/maps/wiki/routing/element.h>
#include <yandex/maps/wiki/routing/route.h>
#include <yandex/maps/wiki/routing/stop.h>

#include <maps/libs/json/include/value.h>

namespace maps {
namespace wiki {
namespace routing {
namespace tests {

template<typename T>
T fromJson(const json::Value& value) { return value.as<T>(); }

template<typename T>
T fromJson(const json::Value& value, const T& defaultValue) {
    return value.exists() ? fromJson<T>(value) : defaultValue;
}

template<>
Point fromJson(const json::Value& point);

template<>
Polyline fromJson(const json::Value& polyline);

template<>
Direction fromJson(const json::Value& direction);

template<>
DirectedElementID fromJson(const json::Value& directedElementId);

template<>
ElementEnd fromJson(const json::Value& elementEnd);

template<>
Elements fromJson(const json::Value& elements);

template<>
common::ConditionType fromJson(const json::Value& type);

template<>
Conditions fromJson(const json::Value& conditions);

template<>
Stop fromJson(const json::Value& stop);

template<>
Stops fromJson(const json::Value& stops);

template<>
StopSnap fromJson(const json::Value& stopSnap);

template<>
Trace fromJson(const json::Value& route);

template<>
NoPathErrors fromJson(const json::Value& noPathErrors);

template<>
AmbiguousPathError fromJson(const json::Value& ambiguousPathError);

template<>
RestoreResult fromJson(const json::Value& result);

} // namespace tests
} // namespace routing
} // namespace wiki
} // namespace maps
