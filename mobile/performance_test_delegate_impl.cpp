#include "navikit/view/performance_test_delegate_impl.h"

#include "NavigatorView.h"
#include "ui/screencontrollers/NaviScreenController.h"
#include "ui/screens/BaseOverviewScreen.h"
#include "util/CoordConversion.h"

#include <yandex/maps/navikit/route_editor/route_editor.h>
#include <yandex/maps/navikit/route_editor/route_editor_listener.h>
#include <yandex/maps/navikit/route_point.h>
#include <yandex/maps/navi/extended_app_component.h>

#include <yandex/maps/runtime/async/dispatcher.h>
#include <yandex/maps/runtime/async/future.h>
#include <yandex/maps/runtime/time.h>

#include <memory>
#include <sstream>

namespace yandex::maps::navi::view {

namespace {

void printDistribution(
    const std::map<std::size_t, std::size_t>& distribution, std::stringstream& outputStream)
{
    for (auto distrib : distribution) {
        outputStream << distrib.first << "-" << distrib.second << ";";
    }
}

runtime::RelativeTimestamp now()
{
    return runtime::now<runtime::RelativeTimestamp>();
}

/**
 * About type parameters: global() is a runtime::async::Dispatcher, and
 * ui() is a runtime::async::CallbackDispatcher. They have a common
 * base class runtime::async::DispatcherBase, but we cannot easily use it
 * to execute async tasks.
 */
template <typename SourceDispatcher, typename DestinationDispatcher>
runtime::async::Handle measureContextSwitches(
    SourceDispatcher* source,
    DestinationDispatcher* destination,
    ActionMetricsAccumulator* accumulator)
{
    return runtime::async::global()->spawn([=] {
        auto iterationStart = now();

        for (;;) {
            // One of the dispatchers may be on UI thread, so we should
            // not wait inside any of them:
            runtime::async::Handle toDestination;

            runtime::RelativeTimestamp start, end;
            source
                ->spawn([&] {
                    start = now();
                    toDestination = destination->spawn([&] { end = now(); });
                })
                .wait();
            toDestination.wait();

            runtime::async::ui()->spawn([=] { accumulator->handleAction(start, end); }).wait();

            static const auto PERIOD_IN_MILLIS = std::chrono::milliseconds(100);
            iterationStart += PERIOD_IN_MILLIS;
            runtime::async::sleepUntil(iterationStart);
        }
    });
}

}  // namespace

void ActionMetricsAccumulator::handleAction(
    runtime::RelativeTimestamp start, runtime::RelativeTimestamp end)
{
    if (!lastStartTime_) {
        lastStartTime_ = start;
        return;
    }

    auto startDelta = (start - *lastStartTime_).count();
    intervalDistribution_[startDelta]++;

    auto endDelta = (end - start).count();
    durationDistribution_[endDelta]++;

    lastStartTime_ = start;
}

std::string ActionMetricsAccumulator::toString(const std::string& label) const
{
    std::stringstream outputStream;

    outputStream << label << " duration distribution : " << std::endl;
    printDistribution(durationDistribution_, outputStream);
    outputStream << std::endl;

    outputStream << label << " interval distribution : " << std::endl;
    printDistribution(intervalDistribution_, outputStream);
    outputStream << std::endl;

    return outputStream.str();
}

PerformanceTestDelegateImpl::PerformanceTestDelegateImpl(
    NavigatorView* navigatorView,
    intent_parser::ActionVisitor* commandsExecutor,
    ScenarioDurations scenarioDurations)
    : navigatorView_(navigatorView), commandsExecutor_(commandsExecutor)
{
    getAppComponent()->defaultLocationProvider()->addListener(subscriptionFromThis());

    mapPerfTestHandle_ =
        runPerformanceTests(navigatorView_->mapkitMapWindow(), this, scenarioDurations);
}

PerformanceTestDelegateImpl::~PerformanceTestDelegateImpl()
{
    getAppComponent()->defaultLocationProvider()->removeListener(subscriptionFromThis());
}

void PerformanceTestDelegateImpl::onLocationChanged()
{
    if (!locationMetricsAccumulator_) {
        return;
    }

    auto location = getAppComponent()->defaultLocationProvider()->location();
    if (!location) {
        return;
    }

    const auto start = location->location.relativeTimestamp;
    const auto end = now();
    locationMetricsAccumulator_->handleAction(start, end);
}

void PerformanceTestDelegateImpl::onRouteCreated(
    const std::shared_ptr<
        runtime::bindings::SharedVector<mapkit::directions::driving::Route>>& /* routes */,
    navikit::route_editor::UserAction /* action */)
{
    if (mapPerfRouteCreationPromise_) {
        navigatorView_->getMap()->getRouteEditor()->removeListener(subscriptionFromThis());

        mapPerfRouteCreationPromise_->setValue(1);
        mapPerfRouteCreationPromise_.reset();
    }
}

void PerformanceTestDelegateImpl::showRoute(bool show, runtime::async::Promise<int> finishPromise)
{
    if (show) {
        navigatorView_->getMap()->getRouteEditor()->addListener(
            subscriptionFromThis());  // Will notify us when route is created

        mapPerfRouteCreationPromise_ = std::move(finishPromise);

        navikit::RoutePoint from, to;
        from.location = mapkit::geometry::Point(59.935916, 30.318867);
        to.location = mapkit::geometry::Point(59.947352, 30.541919);
        commandsExecutor_->onRouteAction(to, {}, from, {});
    } else {
        commandsExecutor_->onClearRoute();

        finishPromise.setValue(1);
    }
}

void PerformanceTestDelegateImpl::showOverview(bool show)
{
    if (show) {
        commandsExecutor_->onShowRouteOverview();
    } else {
        auto* screenController = navigatorView_->getScreenController();
        if (screenController->getBackId() == UI::ScreenId::STATE_MAP_ROUTE_OVERVIEW) {
            std::static_pointer_cast<UI::Screens::BaseOverviewScreen>(
                screenController->getBackScreen())
                ->onGoButtonClick();
        }

        // Moving back from overview is not enough, need to zoom in:
        navigatorView_->getMap()->getCamera()->getZoomAnimator().animateTo(17.0f);
    }

    // Avoids 10 FPS limit in overview-only tests:
    navigatorView_->reportUserActivity();
}

void PerformanceTestDelegateImpl::showJams(bool show)
{
    navigatorView_->getMap()->setJamsVisible(show, "perf tests");
}

void PerformanceTestDelegateImpl::moveCamera(const mapkit::map::CameraPosition& position)
{
    auto* cameraController = navigatorView_->getMap()->getCameraController().get();

    // Location may significantly change, enabling cursor following:
    if (cameraController->getCameraFollows() != Maps::CameraController::CAMERA_UNFOLLOWS) {
        cameraController->setCameraFollows(Maps::CameraController::CAMERA_UNFOLLOWS);
    }

    cameraController->setCameraView(
        CoordConversion::toXY(position.target), position.zoom, position.azimuth);
    navigatorView_->reportUserActivity();  // Avoids 10 FPS limit
}

void PerformanceTestDelegateImpl::startMetricsCapture()
{
    locationMetricsAccumulator_ = ActionMetricsAccumulator();

    uiToGlobalContextSwitchMetrics_ = ContextSwitchMetrics();
    uiToGlobalContextSwitchMetrics_->measurerHandle = measureContextSwitches(
        runtime::async::ui(),
        runtime::async::global(),
        &uiToGlobalContextSwitchMetrics_->accumulator);

    globalToUiContextSwitchMetrics_ = ContextSwitchMetrics();
    globalToUiContextSwitchMetrics_->measurerHandle = measureContextSwitches(
        runtime::async::global(),
        runtime::async::ui(),
        &globalToUiContextSwitchMetrics_->accumulator);

    globalToGlobalContextSwitchMetrics_ = ContextSwitchMetrics();
    globalToGlobalContextSwitchMetrics_->measurerHandle = measureContextSwitches(
        runtime::async::global(),
        runtime::async::global(),
        &globalToGlobalContextSwitchMetrics_->accumulator);

    navigatorView_->mapkitMapWindow()->startPerformanceMetricsCapture();
}

std::string PerformanceTestDelegateImpl::stopMetricsCapture()
{
    ASSERT(locationMetricsAccumulator_);

    auto metrics = navigatorView_->mapkitMapWindow()->stopPerformanceMetricsCapture();

    metrics += '\n' + locationMetricsAccumulator_->toString("Location");

    metrics += '\n' + uiToGlobalContextSwitchMetrics_->accumulator.toString("ui -> global");
    metrics += '\n' + globalToUiContextSwitchMetrics_->accumulator.toString("global -> ui");
    metrics += '\n' + globalToGlobalContextSwitchMetrics_->accumulator.toString("global -> global");

    globalToGlobalContextSwitchMetrics_ = boost::none;
    globalToUiContextSwitchMetrics_ = boost::none;
    uiToGlobalContextSwitchMetrics_ = boost::none;

    locationMetricsAccumulator_ = boost::none;

    return metrics;
}

}
