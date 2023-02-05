#pragma once
#include <yandex/maps/navikit/mocks/mock_route_editor.h>

#include <yandex/maps/runtime/subscription/subscription.h>

namespace yandex::maps::navikit::route_editor {

class RouteEditorStub : public testing::NiceMock<MockRouteEditor> {
public:
    /**
     * Adds listener.
     */
    void addListener(const std::shared_ptr<RouteEditorListener>& editorListener) override
    {
        subscription_.subscribe(editorListener);
    }

    /**
     * Removes listener.
     */
    void removeListener(const std::shared_ptr<RouteEditorListener>& editorListener) override
    {
        subscription_.unsubscribe(editorListener);
    }

    void notifyRouteCreated(
        const std::shared_ptr<runtime::bindings::SharedVector<mapkit::directions::driving::Route>>&
            routes,
        UserAction action)
    {
        subscription_.notify(&RouteEditorListener::onRouteCreated, routes, action);
    }

private:
    runtime::subscription::Subscription<RouteEditorListener> subscription_;
};

}  // namespace yandex::maps::navikit::route_editor
