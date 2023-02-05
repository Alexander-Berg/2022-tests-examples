#pragma once

#include <yandex/maps/mapkit/geometry/geometry.h>
#include <yandex/maps/mapkit/location/location.h>
#include <yandex/maps/proto/offline_recording/record.pb.h>
#include <yandex/maps/runtime/config/config_manager.h>
#include <yandex/maps/runtime/network/request.h>
#include <yandex/maps/runtime/storage/tile_item.h>
#include <yandex/maps/runtime/time.h>

#include <boost/optional.hpp>

#include <string>
#include <unordered_map>
#include <vector>

namespace yandex::maps::mapkit::directions::guidance::internal::tester {

enum class EventType {
    RouteLost,
    ReturnToRoute,
    RouteFinished
};

struct Interval {
    runtime::AbsoluteTimestamp start;
    runtime::TimeInterval duration;

    bool contains(const runtime::AbsoluteTimestamp& ts) const
    {
        return start <= ts && ts <= (start + duration);
    }
};

struct ExpectedEvent {
    EventType type;
    boost::optional<Interval> interval;

    bool matches(EventType type, const runtime::AbsoluteTimestamp& ts) const
    {
        return (this->type == type) && (!interval || interval->contains(ts));
    }
};

struct Event {
    EventType type;
    runtime::AbsoluteTimestamp timestamp;

    boost::optional<location::Location> location;
    boost::optional<ExpectedEvent> expectation;
};

struct Result {
    std::vector<ExpectedEvent> failedExpectations;
    std::vector<Event> unexpectedEvents;
    std::vector<Event> realizedEvents;
    // Geometry of the adjusted track. Contains continuous polylines between
    // each camera jump.
    std::vector<geometry::Polyline> geometries;
    bool neededUnsavedTiles = false;
    bool testShouldFail = false;

    Result(
        std::vector<ExpectedEvent> failedExpectations,
        std::vector<Event> unexpectedEvents,
        std::vector<Event> realizedEvents,
        std::vector<geometry::Polyline> geometries)
        : failedExpectations(std::move(failedExpectations))
        , unexpectedEvents(std::move(unexpectedEvents))
        , realizedEvents(std::move(realizedEvents))
        , geometries(std::move(geometries))
    {
    }

    Result() {}

    bool succeeded() const
    {
        return failedExpectations.empty() && unexpectedEvents.empty() && !neededUnsavedTiles;
    }
};

std::ostream& operator<<(std::ostream& os, EventType type);
std::ostream& operator<<(std::ostream& os, const Interval& ts);
std::ostream& operator<<(std::ostream& os, const ExpectedEvent& expectation);
std::ostream& operator<<(std::ostream& os, const Event& event);

std::string to_string(const runtime::TimeInterval& interval);
std::string to_string(const runtime::AbsoluteTimestamp& ts);
std::string to_string(EventType eventType);

struct TestParameters {
    boost::optional<runtime::network::RequestFactory> requestFactory = boost::none;
    runtime::config::ConfigManager* configManager = nullptr;
    bool realTimeClock = false;
    bool useOfflineTiles = false;
};

// Test config format is described in tools/guidance-tester/README.
Result runTestGuide(const std::string& pathToTestConfig, TestParameters params);

Result runTestGuide(
    const std::vector<proto::offline::recording::record::Record>& protoTrack,
    runtime::network::RequestFactory requestFactory);

Result runTestGuideAndLoadTiles(
    const std::vector<proto::offline::recording::record::Record>& protoTrack,
    const std::vector<ExpectedEvent>& expectedEvents,
    runtime::network::RequestFactory requestFactory,
    runtime::config::ConfigManager* configManager,
    std::unordered_map<runtime::storage::ItemId, runtime::storage::Item>* tiles);

void saveTestToJson(
    const std::string& filename,
    const std::string& pathToTrack,
    const std::vector<ExpectedEvent>& expectedEvents,
    const std::vector<std::pair<std::string, std::string>>& extraInfo);

} // namespace yandex::maps::mapkit::directions::guidance::internal::tester
