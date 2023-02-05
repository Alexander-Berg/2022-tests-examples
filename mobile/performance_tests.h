#pragma once

#include <yandex/maps/mapkit/map/camera_position.h>

#include <yandex/maps/runtime/async/future.h>
#include <yandex/maps/runtime/async/promise.h>
#include <yandex/maps/runtime/time.h>

namespace yandex::maps {

namespace mapkit::map { class MapWindow; }

namespace navi::view {

class PerformanceTestDelegate {
public:
    virtual ~PerformanceTestDelegate() = default;

    virtual void showRoute(bool show, runtime::async::Promise<int> finishPromise) = 0;
    virtual void showOverview(bool show) = 0;
    virtual void showJams(bool show) = 0;

    /**
     * Moves camera instantly to given position.
     */
    virtual void moveCamera(const mapkit::map::CameraPosition& position) = 0;

    virtual void startMetricsCapture() = 0;
    virtual std::string stopMetricsCapture() = 0;
};

using Millis = long long;

// Divisible without a remainder by a number of iterations in each scenario,
// and is not too short:
const Millis DEFAULT_SCENARIO_MILLIS = 144000;

struct ScenarioDurations {
    Millis scroll;
    Millis zoom;
    Millis overview;

    ScenarioDurations(
        boost::optional<Millis> scroll,
        boost::optional<Millis> zoom,
        boost::optional<Millis> overview)
        : scroll(scroll.value_or(DEFAULT_SCENARIO_MILLIS))
        , zoom(zoom.value_or(DEFAULT_SCENARIO_MILLIS))
        , overview(overview.value_or(DEFAULT_SCENARIO_MILLIS))
    {
    }
};

runtime::async::Handle runPerformanceTests(
    mapkit::map::MapWindow* mapWindow,
    PerformanceTestDelegate* delegate,
    ScenarioDurations scenarioDurations);

}
}
