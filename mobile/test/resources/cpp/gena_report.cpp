#include <yandex/maps/navikit/report/gena/application.h>
#include <yandex/maps/navikit/report/gena/webview.h>
#include <yandex/maps/navikit/report/gena/routes.h>
#include <yandex/maps/navikit/report/gena/map.h>

#include <map>
#include <optional>
#include <string>

namespace yandex::maps::navikit::report::gena {

namespace {

template<typename T>
std::string toString(const T& value)
{
    return std::to_string(value);
}

template<typename T>
std::string toString(const std::optional<T>& value)
{
    return value ? toString<T>(*value) : "";
}

template<>
std::string toString(const bool& value)
{
    return value ? "true" : "false";
}

template<>
std::string toString(const std::string& value)
{
    return value;
}

template<>
std::string toString(const application::StartSessionLayerType& layerType)
{
    switch (layerType) {
    case application::StartSessionLayerType::Map: return "map";
    case application::StartSessionLayerType::Satellite: return "satellite";
    case application::StartSessionLayerType::Hybrid: return "hybrid";
    }
}

template<>
std::string toString(const application::GetGlobalParamethersNightMode& nightMode)
{
    switch (nightMode) {
    case application::GetGlobalParamethersNightMode::True: return "true";
    case application::GetGlobalParamethersNightMode::False: return "false";
    case application::GetGlobalParamethersNightMode::Auto: return "auto";
    case application::GetGlobalParamethersNightMode::System: return "system";
    }
}

template<>
std::string toString(const application::GetGlobalParamethersLaunchType& launchType)
{
    switch (launchType) {
    case application::GetGlobalParamethersLaunchType::FreshStart: return "fresh_start";
    case application::GetGlobalParamethersLaunchType::FromBackground: return "from_background";
    }
}

template<>
std::string toString(const application::CloseRateMeAlertReason& reason)
{
    switch (reason) {
    case application::CloseRateMeAlertReason::Later: return "later";
    case application::CloseRateMeAlertReason::Rate: return "rate";
    case application::CloseRateMeAlertReason::OuterTap: return "outer-tap";
    }
}

template<>
std::string toString(const webview::LoadedType& type)
{
    switch (type) {
    case webview::LoadedType::Direct: return "direct";
    }
}

template<>
std::string toString(const map::ChangeTrafficBackground& background)
{
    switch (background) {
    case map::ChangeTrafficBackground::Map: return "map";
    case map::ChangeTrafficBackground::Route: return "route";
    case map::ChangeTrafficBackground::SearchResults: return "search-results";
    }
}

template<>
std::string toString(const map::ChangeTrafficSource& source)
{
    switch (source) {
    case map::ChangeTrafficSource::ControlOnMap: return "control-on-map";
    case map::ChangeTrafficSource::LayerMenu: return "layer-menu";
    }
}

template<>
std::string toString(const map::ZoomInBackground& background)
{
    switch (background) {
    case map::ZoomInBackground::Map: return "map";
    case map::ZoomInBackground::Route: return "route";
    case map::ZoomInBackground::RoutePoints: return "route-points";
    case map::ZoomInBackground::SearchResults: return "search-results";
    case map::ZoomInBackground::Navigation: return "navigation";
    case map::ZoomInBackground::Roulette: return "roulette";
    }
}

template<>
std::string toString(const map::ZoomInSource& source)
{
    switch (source) {
    case map::ZoomInSource::Gesture: return "gesture";
    case map::ZoomInSource::ZoomButton: return "zoom-button";
    case map::ZoomInSource::ZoomButtonLongTap: return "zoom-button-long-tap";
    case map::ZoomInSource::VolumeButton: return "volume-button";
    }
}

template<>
std::string toString(const map::ChangeTiltType& type)
{
    switch (type) {
    case map::ChangeTiltType::Flat: return "flat";
    case map::ChangeTiltType::Perspective: return "perspective";
    }
}

template<>
std::string toString(const map::ChangeTiltAction& action)
{
    switch (action) {
    case map::ChangeTiltAction::Gesture: return "gesture";
    case map::ChangeTiltAction::Button: return "button";
    }
}

template<>
std::string toString(const map::LocateUserState& state)
{
    switch (state) {
    case map::LocateUserState::Locate: return "locate";
    case map::LocateUserState::ArrowOn: return "arrow-on";
    case map::LocateUserState::ArrowOff: return "arrow-off";
    case map::LocateUserState::StartSearching: return "start-searching";
    case map::LocateUserState::StopSearching: return "stop-searching";
    case map::LocateUserState::Error: return "error";
    }
}

template<>
std::string toString(const map::LongTapBackground& background)
{
    switch (background) {
    case map::LongTapBackground::Route: return "route";
    case map::LongTapBackground::Navigation: return "navigation";
    case map::LongTapBackground::Map: return "map";
    case map::LongTapBackground::SearchResults: return "search-results";
    }
}

}

}

namespace yandex::maps::navikit::report::gena {

namespace application {

Reportable startSession(std::optional<bool> roadAlerts, std::optional<bool> zoomButtonsEnabled, std::optional<StartSessionLayerType> layerType, std::optional<int> batteryCharge, std::optional<std::string> locale, std::optional<bool> mapRotation, std::optional<bool> showRuler, std::optional<bool> autoRebuild, std::optional<bool> autoUpdate, std::optional<bool> routesInNavi, std::optional<bool> wifiOnly, std::optional<bool> avoidTollRoads, std::optional<bool> showPublicTransportLables, std::optional<bool> soundsThroughBluetooth, std::optional<std::string> language, std::optional<bool> authorized, std::optional<bool> traffic)
{
    return Reportable {
        "application.start-session",
        {
            { "road_alerts", toString(roadAlerts) },
            { "zoom_buttons_enabled", toString(zoomButtonsEnabled) },
            { "layer_type", toString(layerType) },
            { "battery_charge", toString(batteryCharge) },
            { "locale", toString(locale) },
            { "map_rotation", toString(mapRotation) },
            { "show_ruler", toString(showRuler) },
            { "auto_rebuild", toString(autoRebuild) },
            { "auto_update", toString(autoUpdate) },
            { "routes_in_navi", toString(routesInNavi) },
            { "wifi_only", toString(wifiOnly) },
            { "avoid_toll_roads", toString(avoidTollRoads) },
            { "show_public_transport_lables", toString(showPublicTransportLables) },
            { "sounds_through_bluetooth", toString(soundsThroughBluetooth) },
            { "language", toString(language) },
            { "authorized", toString(authorized) },
            { "traffic", toString(traffic) },
        }
    };
}

Reportable getGlobalParamethers(std::optional<int> bookmarksCount, std::optional<int> listsCount, std::optional<bool> homeAdded, std::optional<bool> workAdded, std::optional<double> cacheSize, std::optional<GetGlobalParamethersNightMode> nightMode, std::optional<int> showBookmarksOnMap, std::optional<bool> pushNotifications, std::optional<double> launchTime, std::optional<double> launchFinishTime, std::optional<std::string> launchStepsTime, std::optional<GetGlobalParamethersLaunchType> launchType, std::optional<int> mapCaches, std::optional<bool> aon, std::optional<bool> backgroundGuidance, std::optional<std::string> voice, std::optional<bool> orgReview, std::optional<bool> discoveryPushes, std::optional<int> stopsCount, std::optional<int> linesCount, std::optional<bool> authorized, std::optional<bool> traffic)
{
    return Reportable {
        "application.get-global-paramethers",
        {
            { "bookmarks_count", toString(bookmarksCount) },
            { "lists_count", toString(listsCount) },
            { "home_added", toString(homeAdded) },
            { "work_added", toString(workAdded) },
            { "cache_size", toString(cacheSize) },
            { "night_mode", toString(nightMode) },
            { "show_bookmarks_on_map", toString(showBookmarksOnMap) },
            { "push_notifications", toString(pushNotifications) },
            { "launch_time", toString(launchTime) },
            { "launch_finish_time", toString(launchFinishTime) },
            { "launch_steps_time", toString(launchStepsTime) },
            { "launch_type", toString(launchType) },
            { "map_caches", toString(mapCaches) },
            { "aon", toString(aon) },
            { "background_guidance", toString(backgroundGuidance) },
            { "voice", toString(voice) },
            { "org_review", toString(orgReview) },
            { "discovery_pushes", toString(discoveryPushes) },
            { "stops_count", toString(stopsCount) },
            { "lines_count", toString(linesCount) },
            { "authorized", toString(authorized) },
            { "traffic", toString(traffic) },
        }
    };
}

Reportable getExperimentsInfo(std::optional<std::map<std::string, std::string>> dictionary)
{
    if (dictionary) {
        return Reportable { "application.get-experiments-info", std::move(*dictionary) };
    } else {
        return Reportable { "application.get-experiments-info", {} };
    }
}

Reportable showRateMeAlert(std::optional<bool> firstTime)
{
    return Reportable {
        "application.show-rate-me-alert",
        {
            { "first_time", toString(firstTime) },
        }
    };
}

Reportable closeRateMeAlert(std::optional<CloseRateMeAlertReason> reason, std::optional<int> ratings)
{
    return Reportable {
        "application.close-rate-me-alert",
        {
            { "reason", toString(reason) },
            { "ratings", toString(ratings) },
        }
    };
}

Reportable mapReady(std::optional<float> time, std::optional<float> renderTime)
{
    return Reportable {
        "application.map-ready",
        {
            { "time", toString(time) },
            { "render_time", toString(renderTime) },
        }
    };
}

}

namespace webview {

Reportable loaded(std::optional<LoadedType> type)
{
    return Reportable {
        "webview.loaded",
        {
            { "type", toString(type) },
        }
    };
}

}

namespace routes {

Reportable changeOption(std::optional<std::string> routeType, std::optional<std::string> option, std::optional<std::string> state)
{
    return Reportable {
        "routes.change-option",
        {
            { "route_type", toString(routeType) },
            { "option", toString(option) },
            { "state", toString(state) },
        }
    };
}

}

namespace map {

Reportable changeTraffic(std::optional<bool> landscape, std::optional<bool> state, std::optional<ChangeTrafficBackground> background, std::optional<ChangeTrafficSource> source)
{
    return Reportable {
        "map.change-traffic",
        {
            { "landscape", toString(landscape) },
            { "state", toString(state) },
            { "background", toString(background) },
            { "source", toString(source) },
        }
    };
}

Reportable zoomIn(std::optional<ZoomInBackground> background, std::optional<bool> landscape, std::optional<ZoomInSource> source)
{
    return Reportable {
        "map.zoom-in",
        {
            { "background", toString(background) },
            { "landscape", toString(landscape) },
            { "source", toString(source) },
        }
    };
}

Reportable arrowOff(std::optional<float> currentScale, std::optional<bool> landscape)
{
    return Reportable {
        "map.arrow-off",
        {
            { "current_scale", toString(currentScale) },
            { "landscape", toString(landscape) },
        }
    };
}

Reportable changeTilt(std::optional<float> currentScale, std::optional<ChangeTiltType> type, std::optional<ChangeTiltAction> action)
{
    return Reportable {
        "map.change-tilt",
        {
            { "current_scale", toString(currentScale) },
            { "type", toString(type) },
            { "action", toString(action) },
        }
    };
}

Reportable locateUser(std::optional<LocateUserState> state, std::optional<bool> landscape)
{
    return Reportable {
        "map.locate-user",
        {
            { "state", toString(state) },
            { "landscape", toString(landscape) },
        }
    };
}

Reportable longTap(std::optional<LongTapBackground> background, std::optional<float> lat, std::optional<float> lon, std::optional<float> currentScale)
{
    return Reportable {
        "map.long-tap",
        {
            { "background", toString(background) },
            { "lat", toString(lat) },
            { "lon", toString(lon) },
            { "current_scale", toString(currentScale) },
        }
    };
}

Reportable routeVia(std::optional<float> lat, std::optional<float> lon)
{
    return Reportable {
        "map.route-via",
        {
            { "lat", toString(lat) },
            { "lon", toString(lon) },
        }
    };
}

Reportable whatHere()
{
    return Reportable { "map.what-here", {} };
}

}

}

