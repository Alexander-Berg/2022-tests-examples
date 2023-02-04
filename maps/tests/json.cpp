#include "json.h"

#include <maps/libs/common/include/exception.h>

#include "magic_string.h"

#include "../route_impl.h"

namespace maps {
namespace wiki {
namespace routing {
namespace tests {

template<>
Point fromJson(const json::Value& point) {
    return point.construct<Point>(0, 1);
}

template<>
Polyline fromJson(const json::Value& polyline) {
    geolib3::PointsVector points;

    for (const auto& point: polyline) {
        points.push_back(fromJson<Point>(point));
    }

    return Polyline(points);
}

template<>
Direction fromJson(const json::Value& direction)
{
    static const std::map<std::string, Direction> directionStringToValue {
        {STR_FORWARD, Direction::Forward},
        {STR_BACKWARD, Direction::Backward},
        {STR_BOTH, Direction::Both},
    };

    const auto it = directionStringToValue.find(direction.as<std::string>());
    REQUIRE(
        it != directionStringToValue.end(),
        "Invalid direction '" <<  direction.toString() << "'"
    );

    return it->second;
}

template<>
ElementEnd fromJson(const json::Value& elementEnd)
{
    return elementEnd.construct<ElementEnd>("junctionId", "zlev");
}

template<>
Elements fromJson(const json::Value& elements)
{
    Elements result;

    for (const auto& element: elements) {
        result.emplace_back(
            element["id"].as<uint64_t>(),
            fromJson<Direction>(element["direction"]),
            fromJson<Polyline>(element["geom"]),
            fromJson<ElementEnd>(element["start"]),
            fromJson<ElementEnd>(element["end"])
        );
    }

    return result;
}

template<>
common::ConditionType fromJson(const json::Value& type)
{
    static const std::map<std::string, common::ConditionType> conditionTypeStringToValue {
        {STR_TURNABOUT, common::ConditionType::Uturn},
        {STR_FORBIDDEN, common::ConditionType::Prohibited},
    };

    const auto it = conditionTypeStringToValue.find(type.as<std::string>());
    REQUIRE(
        it != conditionTypeStringToValue.end(),
        "Invalid condition type '" <<  type.toString() << "'"
    );

    return it->second;
}

template<>
Conditions fromJson(const json::Value& conditions)
{
    Conditions result;

    for (const auto& condition: conditions) {
        result.emplace_back(
            condition["id"].as<uint64_t>(),
            fromJson<common::ConditionType>(condition["type"]),
            condition["fromElementId"].as<uint64_t>(),
            condition["viaJunctionId"].as<uint64_t>(),
            condition["toElementIds"].as<std::vector<uint64_t>>()
        );
    }

    return result;
}

template<>
Stop fromJson(const json::Value& stop)
{
    return {stop["id"].as<uint64_t>(), fromJson<Point>(stop["geom"])};
}

template<>
Stops fromJson(const json::Value& stops)
{
    Stops result;

    for (const auto& stop: stops) {
        result.push_back(fromJson<Stop>(stop));
    }

    return result;
}

template<>
DirectedElementID fromJson(const json::Value& directedElementId) {
    return {
        directedElementId["id"].as<uint64_t>(),
        fromJson<Direction>(directedElementId["direction"])
    };
}

template<>
StopSnap fromJson(const json::Value& stopSnap) {
    return PImplFactory::create<StopSnap>(
        stopSnap["stopId"].as<uint64_t>(),
        stopSnap["locationOnElement"].as<double>(),
        fromJson<Point>(stopSnap["point"])
    );
}

template<>
Trace fromJson(const json::Value& route)
{
    Trace result;

    for (const auto& point: route) {
        const DirectedElementID directedElementId = fromJson<DirectedElementID>(point["directedElementId"]);
        const auto& stopSnap = point["stopSnap"];

        if (stopSnap.exists()) {
            result.emplace_back(
                PImplFactory::create<TracePoint>(
                    directedElementId,
                    fromJson<StopSnap>(stopSnap)
                )
            );
        } else {
            result.emplace_back(
                PImplFactory::create<TracePoint>(directedElementId)
            );
        }
    }

    return result;
}

template<>
NoPathErrors fromJson(const json::Value& noPathErrors)
{
    NoPathErrors result;

    for (const auto& error: noPathErrors) {
        result.emplace_back(
            PImplFactory::create<NoPathError>(
                error["fromStopId"].as<ID>(),
                error["toStopId"].as<ID>()
            )
        );
    }

    return result;
}

template<>
AmbiguousPathErrors fromJson(const json::Value& ambiguousPathError)
{
    AmbiguousPathErrors result;

    for (const auto& error: ambiguousPathError) {
        result.push_back(
            PImplFactory::create<AmbiguousPathError>(
                error["fromStopId"].as<ID>(),
                error["toStopId"].as<ID>(),
                error["elementId"].as<ID>()
            )
        );
    }

    return result;
}

template<>
RestoreResult fromJson(const json::Value& result)
{
    return PImplFactory::create<RestoreResult>(
        fromJson<Trace>(result["trace"], {}),
        fromJson<IdSet>(result["unusedElementIds"], {}),
        fromJson<AmbiguousPathErrors>(result["forwardAmbiguousPathErrors"], {}),
        fromJson<AmbiguousPathErrors>(result["backwardAmbiguousPathErrors"], {}),
        fromJson<NoPathErrors>(result["noPathErrors"], {})
    );
}


} // namespace tests
} // namespace routing
} // namespace wiki
} // namespace maps
