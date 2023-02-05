#include <yandex/maps/navikit/check_context.h>
#include <yandex/maps/navikit/geometry/math.h>
#include <yandex/maps/navikit/report/report.h>
#include <yandex/maps/navi/view/performance_tests.h>

#include <yandex/maps/mapkit/animation.h>
#include <yandex/maps/mapkit/geometry/point.h>
#include <yandex/maps/mapkit/map/map.h>
#include <yandex/maps/mapkit/map/map_load_statistics.h>
#include <yandex/maps/mapkit/map/map_loaded_listener.h>
#include <yandex/maps/mapkit/map/map_window.h>

#include <yandex/maps/runtime/async/dispatcher.h>
#include <yandex/maps/runtime/logging/logger.h>
#include <yandex/maps/runtime/view/platform_view.h>

#include <boost/algorithm/string.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/regex.hpp>

#include <cctype>
#include <cstddef>
#include <functional>
#include <string>
#include <vector>

#define UI_NOCORO_REF(code) \
    yandex::maps::runtime::async::ui()->spawn([&] { code; }).wait();

namespace yandex::maps::navi::view {

namespace {

Millis steadyMillis()
{
    return runtime::now<runtime::RelativeTimestamp>().time_since_epoch().count();
}

/**
 * Interpolates position from start to end with given millis.
 */
mapkit::map::CameraPosition midStepPosition(
    const mapkit::map::CameraPosition& start,
    const mapkit::map::CameraPosition& end,
    Millis passedMillis,
    Millis fullMillis)
{
    const auto factor = static_cast<float>(passedMillis) / fullMillis;

    return {
        mapkit::geometry::Point(
            navikit::geometry::lerp(start.target.latitude, end.target.latitude, factor),
            navikit::geometry::lerp(start.target.longitude, end.target.longitude, factor)),
        navikit::geometry::lerp(start.zoom, end.zoom, factor),
        navikit::geometry::lerp(start.azimuth, end.azimuth, factor),
        0.0f  // Navigator decides on its own tilt
    };
}

/**
 * No scenarios should be run before map is loaded!
 */
class PromiseBasedMapLoadedListener : public mapkit::map::MapLoadedListener {
public:
    PromiseBasedMapLoadedListener(runtime::async::Promise<int> onMapLoadedPromise)
        : onMapLoadedPromise_(std::move(onMapLoadedPromise))
    {
    }

private:
    virtual void onMapLoaded(const mapkit::map::MapLoadStatistics& /* statistics */) override
    {
        onMapLoadedPromise_.setValue(1);
    }

    runtime::async::Promise<int> onMapLoadedPromise_;
};

void waitForMapLoad(mapkit::map::Map* map)
{
    navikit::assertNotUi();

    runtime::async::Promise<int> onMapLoadedPromise;
    auto onMapLoadedFuture = onMapLoadedPromise.future();

    auto listener = std::make_shared<PromiseBasedMapLoadedListener>(std::move(onMapLoadedPromise));

    runtime::async::ui()
        ->spawn(
            [map](auto listener) {
                // Listener will be notified right away if map is already loaded:
                map->setMapLoadedListener(listener);
            },
            std::move(listener))
        .wait();

    onMapLoadedFuture.wait();

    // Reset the listener (based on TestApp code):
    runtime::async::ui()->spawn([map] { map->setMapLoadedListener({}); }).wait();
}

struct Step {
    // Where to move the map:
    mapkit::map::CameraPosition position;

    // How long to animate the movement:
    Millis duration;
};

std::vector<Step> scrollSteps(  // Circular scroll
    const mapkit::map::CameraPosition& basePosition,
    Millis scenarioDuration)
{
    static const Millis STEP_DURATION = 300;
    static const std::size_t LOOP_STEPS_COUNT = 60;

    const std::size_t loopsCount =
        static_cast<std::size_t>(scenarioDuration / LOOP_STEPS_COUNT / STEP_DURATION);

    const auto& baseTarget = basePosition.target;

    const double longitudeRadius = std::pow(2.0, 10.0 - basePosition.zoom);
    const double latitudeRadius = 0.5 * longitudeRadius;

    std::vector<Step> steps(loopsCount * LOOP_STEPS_COUNT);
    for (std::size_t loopIndex = 0; loopIndex < loopsCount; ++loopIndex) {
        for (std::size_t stepIndex = 0; stepIndex < LOOP_STEPS_COUNT; ++stepIndex) {
            const double angle =
                mapkit::geometry::degreesToRadians(360.0f / LOOP_STEPS_COUNT * stepIndex);

            auto& step = steps[loopIndex * LOOP_STEPS_COUNT + stepIndex];

            step.position = {
                {baseTarget.latitude + latitudeRadius * std::sin(angle),
                 baseTarget.longitude + longitudeRadius * std::cos(angle)},
                basePosition.zoom,
                0.0f,
                0.0f};

            step.duration = STEP_DURATION;
        }
    }

    return steps;
}

std::vector<Step> zoomSteps(mapkit::map::CameraPosition basePosition, Millis scenarioDuration)
{
    static const Millis STEP_DURATION = 4000;

    static const float ZOOM_OFFSET = 5.0f;
    static const float MAX_USEFUL_ZOOM = 17.0f;  // Legacy map does not support more

    const float maxZoom = std::min(basePosition.zoom + ZOOM_OFFSET, MAX_USEFUL_ZOOM);
    basePosition.zoom = maxZoom - ZOOM_OFFSET;

    static const std::size_t ITERATION_STEPS_COUNT = 4;
    const std::size_t iterationsCount =
        static_cast<std::size_t>(scenarioDuration / STEP_DURATION / ITERATION_STEPS_COUNT);

    std::vector<Step> steps(iterationsCount * ITERATION_STEPS_COUNT);
    for (std::size_t i = 0; i < iterationsCount * ITERATION_STEPS_COUNT;
         i += ITERATION_STEPS_COUNT) {
        steps[i].position = basePosition;
        steps[i].position.zoom += ZOOM_OFFSET;
        steps[i].duration = STEP_DURATION;

        steps[i + 1].position = basePosition;
        steps[i + 1].duration = STEP_DURATION;

        steps[i + 2].position = basePosition;
        steps[i + 2].position.zoom -= ZOOM_OFFSET;
        steps[i + 2].duration = STEP_DURATION;

        steps[i + 3].position = basePosition;
        steps[i + 3].duration = STEP_DURATION;
    }

    return steps;
}

std::string makeMetricName(const std::string& logLabel)
{
    std::vector<std::string> words;
    boost::algorithm::split(words, logLabel, boost::algorithm::is_any_of(" "));
    for (std::string& word : words) {
        if (word.empty()) {
            continue;
        }
        word[0] = toupper(word[0]);
    }
    return boost::regex_replace(boost::algorithm::join(words, ""), boost::regex("[^a-zA-Z-]"), "");
}

int getPercentile(const std::string& distributionString, int percentile)
{
    // distributionString looks like "0-55;1-79;2-16;3-7;4-7;"
    std::vector<std::string> valueStrings;
    boost::algorithm::split(valueStrings, distributionString, boost::algorithm::is_any_of(";"));
    std::vector<std::pair<int, int>> distribution;
    int totalCount = 0;
    for (std::size_t i = 0; i < valueStrings.size() - 1; i++) {
        std::vector<std::string> values;
        boost::algorithm::split(values, valueStrings[i], boost::algorithm::is_any_of("-"));
        int second = boost::lexical_cast<int>(values.at(1));
        distribution.push_back({boost::lexical_cast<int>(values[0]), second});
        totalCount += second;
    }
    int needed = totalCount * percentile / 100;
    int curCount = 0;
    for (const auto& item : distribution) {
        curCount += item.second;
        if (curCount >= needed) {
            return item.first;
        }
    }
    if (distribution.empty()) {
        return -1;
    }
    return distribution.back().first;
}

void reportForPerfTest(const std::string& scenarioName, const std::vector<std::string>& dataLines)
{
    const int percentiles[] = {50, 75, 90, 95};
    for (std::size_t i = 0; i + 1 < dataLines.size(); i += 2) {
        const auto& label = dataLines[i];
        const auto& data = dataLines[i + 1];
        for (int perc : percentiles) {
            navikit::report::reportPerf(
                scenarioName + "_" + makeMetricName(label) + "-" +
                    boost::lexical_cast<std::string>(perc),
                (int64_t)getPercentile(data, perc));
        }
    }
}

class ScenarioExecutor {
public:
    ScenarioExecutor(
        mapkit::map::MapWindow* mapWindow,
        PerformanceTestDelegate* delegate,
        const mapkit::map::CameraPosition& initialPosition)
        : mapWindow_(mapWindow), delegate_(delegate), initialPosition_(initialPosition)
    {
        ASSERT(mapWindow_);
        ASSERT(delegate_);

        ASSERT(initialPosition_.tilt == 0.0f);  // Make sure there are no surprises!
    }

    void executeScenarios(ScenarioDurations durations)
    {
        if (durations.scroll > 0) {
            executeStandardScenario("scroll", scrollSteps(initialPosition_, durations.scroll));
        }

        if (durations.zoom > 0) {
            executeStandardScenario("zoom", zoomSteps(initialPosition_, durations.zoom));
        }

        if (durations.overview > 0) {
            executeOverviewScenario(durations.overview);
        }
    }

private:
    void executeStandardScenario(const std::string& name, const std::vector<Step>& steps)
    {
        ASSERT(!name.empty());
        ASSERT(!steps.empty());

        UI_NOCORO_REF(delegate_->moveCamera(initialPosition_));

        measure(name, [&] {
            const auto* previousStepEndPosition = &initialPosition_;
            auto previousStepEndMillis = steadyMillis();

            for (const auto& step : steps) {
                ASSERT(step.position.tilt == 0.0f);  // Make sure there are no surprises!

                executeStep(previousStepEndPosition, previousStepEndMillis, step);
            }
        });
    }

    void executeOverviewScenario(Millis scenarioMillis)
    {
        ASSERT(scenarioMillis > 0);

        UI_NOCORO_REF(delegate_->moveCamera(initialPosition_));

        measure("overview", [&] {
            static const Millis ITERATION_DURATION = 6000;

            const std::size_t iterationsCount =
                static_cast<std::size_t>(scenarioMillis / ITERATION_DURATION);

            auto iterationStart = runtime::now<runtime::RelativeTimestamp>();
            for (std::size_t i = 0; i < iterationsCount; ++i) {
                UI_NOCORO_REF(delegate_->showOverview(false));

                // Short sleep time for tile load measurement:
                iterationStart += std::chrono::milliseconds(ITERATION_DURATION) / 2;
                runtime::async::sleepUntil(iterationStart);

                UI_NOCORO_REF(delegate_->showOverview(true));

                // Short sleep time for tile load measurement:
                iterationStart += std::chrono::milliseconds(ITERATION_DURATION) / 2;
                runtime::async::sleepUntil(iterationStart);
            }
        });
    }

    template <typename Function>
    void measure(const std::string& name, Function function)
    {
        UI_NOCORO_REF(delegate_->startMetricsCapture());

        function();

        runtime::async::ui()
            ->spawn([&] {
                const auto text = delegate_->stopMetricsCapture();

                std::vector<std::string> originalLines;
                boost::algorithm::split(originalLines, text, boost::algorithm::is_any_of("\n"));

                static const std::size_t MAX_LENGTH = 512;
                std::vector<std::string> limitedLines;
                for (const auto& line : originalLines) {
                    for (std::size_t i = 0; i < line.length(); i += MAX_LENGTH) {
                        const auto subLength = std::min(MAX_LENGTH, line.length() - i);
                        limitedLines.push_back(line.substr(i, subLength));
                    }
                }

                MAPS_WARN() << name << " PERFORMANCE " << limitedLines.size() << ":";
                for (const auto& line : limitedLines) {
                    MAPS_WARN() << "PERF_LINE: " << line;
                }

                reportForPerfTest(name, originalLines);
            })
            .wait();
    }

    // Animates map state according to the given step:
    void executeStep(
        const mapkit::map::CameraPosition*& previousStepEndPosition,
        Millis& previousStepEndMillis,
        const Step& step)
    {
        for (;;) {
            const auto millis = steadyMillis() - previousStepEndMillis;
            if (millis > step.duration) {
                previousStepEndPosition = &step.position;
                previousStepEndMillis += step.duration;
                break;
            }

            const auto position =
                midStepPosition(*previousStepEndPosition, step.position, millis, step.duration);

            UI_NOCORO_REF(delegate_->moveCamera(position));

            static const Millis FRAME_DURATION = 1000 / runtime::view::DEFAULT_MAX_FPS;

            // Updating twice per frame ensures smooth map movement. If we
            // did step updates in generateRenderState, we wouldn't need
            // twice the speed of updates.
            static const Millis SLEEP_DURATION = FRAME_DURATION / 2;
            runtime::async::sleepFor(std::chrono::milliseconds(SLEEP_DURATION));
        }
    }

    mapkit::map::MapWindow* mapWindow_;
    PerformanceTestDelegate* delegate_;

    mapkit::map::CameraPosition initialPosition_;
};

}  // namespace

runtime::async::Handle runPerformanceTests(
    mapkit::map::MapWindow* mapWindow,
    PerformanceTestDelegate* delegate,
    ScenarioDurations scenarioDurations)
{
    navikit::assertUi();

    static const mapkit::geometry::Point SAINT_PETERSBURG(59.931186, 30.353729);
    static const mapkit::map::CameraPosition SAINT_PETERSBURG_Z16(
        SAINT_PETERSBURG, 16.0f, 0.0f, 0.0f);

    return runtime::async::system()->spawn([=] {
        waitForMapLoad(mapWindow->map());

        // Show route:
        runtime::async::Promise<int> routeChangeFinishPromise;
        auto routeChangeFinishFuture = routeChangeFinishPromise.future();

        runtime::async::ui()
            ->spawn(
                [&](auto promise) { delegate->showRoute(true, std::move(promise)); },
                std::move(routeChangeFinishPromise))
            .wait();

        routeChangeFinishFuture.wait();

        // Hide overview:
        UI_NOCORO_REF(delegate->showOverview(false));

        // Short sleep time for tiles to load:
        runtime::async::sleepFor(std::chrono::seconds(2));

        // Show jams:
        UI_NOCORO_REF(delegate->showJams(true));

        ScenarioExecutor executor(mapWindow, delegate, SAINT_PETERSBURG_Z16);
        executor.executeScenarios(scenarioDurations);
    });
}

}
