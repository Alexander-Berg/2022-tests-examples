#pragma once

#include <yandex/maps/navikit/enable_subscription_from_this.h>
#include <yandex/maps/navikit/location/location_provider.h>
#include <yandex/maps/navikit/route_editor/empty_route_editor_listener.h>
#include <yandex/maps/navi/intent_parser/action_visitor.h>
#include <yandex/maps/navi/view/performance_tests.h>

class NavigatorView;

namespace yandex::maps::navi::view {

class ActionMetricsAccumulator {
public:
    void handleAction(runtime::RelativeTimestamp start, runtime::RelativeTimestamp end);

    std::string toString(const std::string& label) const;

private:
    std::map<std::size_t, std::size_t> durationDistribution_;
    std::map<std::size_t, std::size_t> intervalDistribution_;

    boost::optional<runtime::RelativeTimestamp> lastStartTime_;
};

struct ContextSwitchMetrics {
    ActionMetricsAccumulator accumulator;
    runtime::async::Handle measurerHandle;
};

class PerformanceTestDelegateImpl
    : public PerformanceTestDelegate
    , private navikit::EnableSubscriptionFromThis<
          navikit::location::LocationProviderListener,
          navikit::route_editor::EmptyRouteEditorListener> {
public:
    PerformanceTestDelegateImpl(
        NavigatorView* navigatorView,
        intent_parser::ActionVisitor* commandsExecutor,
        ScenarioDurations scenarioDurations);

    virtual ~PerformanceTestDelegateImpl() override;

private:
    // LocationProviderListener
    virtual void onLocationChanged() override;

    // EmptyRouteEditorListener
    virtual void onRouteCreated(
        const std::shared_ptr<runtime::bindings::SharedVector<mapkit::directions::driving::Route>>&
            routes,
        navikit::route_editor::UserAction action) override;

    // PerformanceTestDelegate
    virtual void showRoute(bool show, runtime::async::Promise<int> finishPromise) override;
    virtual void showOverview(bool show) override;
    virtual void showJams(bool show) override;

    virtual void moveCamera(const mapkit::map::CameraPosition& position) override;

    virtual void startMetricsCapture() override;
    virtual std::string stopMetricsCapture() override;

    NavigatorView* const navigatorView_;
    intent_parser::ActionVisitor* const commandsExecutor_;

    boost::optional<ActionMetricsAccumulator> locationMetricsAccumulator_;

    boost::optional<ContextSwitchMetrics> uiToGlobalContextSwitchMetrics_;
    boost::optional<ContextSwitchMetrics> globalToUiContextSwitchMetrics_;
    boost::optional<ContextSwitchMetrics> globalToGlobalContextSwitchMetrics_;

    runtime::async::Handle mapPerfTestHandle_;
    boost::optional<runtime::async::Promise<int>> mapPerfRouteCreationPromise_;
};

}
