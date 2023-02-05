package ru.yandex.navi;

import com.google.common.collect.ImmutableMap;
import io.qameta.allure.Step;
import ru.yandex.navi.tf.MobileUser;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// https://wiki.yandex-team.ru/users/vralex/navigator-intents/
public final class Commands {
    private final MobileUser user;

    public Commands(MobileUser user) {
        this.user = user;
    }

    public void addBookmark(GeoPoint pt) {
        sendCommand("add_bookmark",
            ImmutableMap.of("title", pt.name, "lat", pt.getLatitude(), "lon", pt.getLongitude()));
    }

    public void addExp(Map<String, Object> params) {
        sendCommand("add_exp", params);
    }

    @Step("Произнести команду '{text}'")
    public void askAlice(String text) {
        sendCommand("ask_alice", ImmutableMap.of("text", text));
    }

    public void buildRoute(GeoPoint from, GeoPoint to) {
        buildRoute(from, to, null);
    }

    public void buildRoute(GeoPoint from, GeoPoint to, List<RoutePoint> via) {
        Map<String, Object> params = new HashMap<>();
        if (from != null)
            addParam(params, "from", from);
        addParam(params, "to", to);
        if (via != null) {
            for (int i = 0; i < via.size(); ++i) {
                RoutePoint point = via.get(i);
                addParam(params, "via_" + i, point.point);
                if (point.type == RoutePoint.Type.VIA)
                    params.put("type_via_" + i, "via");
            }
        }

        sendCommand("build_route_on_map", params);
    }

    private static void addParam(Map<String, Object> params, String key, GeoPoint point) {
        params.put("lat_" + key, point.getLatitude());
        params.put("lon_" + key, point.getLongitude());
    }

    public void clearRoute() {
        sendCommand("clear_route");
    }

    public void downloadCache(int regionId, Duration timeout) {
        startDownloadCache(regionId);
        user.waitForLog("download_completed", timeout);
    }

    public void mapSearch(String text) {
        sendCommand("map_search", ImmutableMap.of("text", text));
    }

    public void setCachesOutdated() {
        sendCommand("simulate_cache_update");
    }

    public void setCachesNeedUpdate() {
        for (int i = 0; i < 3; ++i)
            sendCommand("simulate_cache_update");
    }

    public void setCacheUnsupported(int regionId) {
        sendCommand("simulate_cache_update", ImmutableMap.of("unsupported_region_id", regionId));
    }

    public void setRoute(String routeBytes) {
        sendCommand("set_route", ImmutableMap.of("route_bytes", routeBytes));
    }

    public void setRouteUri(String routeUri) {
        sendCommand("set_route", ImmutableMap.of("route_uri", routeUri));
    }

    public void showPointOnMap(GeoPoint point) {
        showPointOnMap(point, null, null);
    }

    public void showPointOnMap(GeoPoint point, Integer zoom, Boolean noBalloon) {
        Map<String, Object> params = new HashMap<>();
        params.put("lat", point.getLatitude());
        params.put("lon", point.getLongitude());
        if (zoom != null)
            params.put("zoom", zoom);
        if (noBalloon != null)
            params.put("no-balloon", noBalloon ? 1 : 0);
        sendCommand("show_point_on_map", params);
    }

    public void showUi(String path) {
        sendCommand("show_ui" + path);
    }

    public void showWebView(String url, String title) {
        sendCommand("show_web_view", ImmutableMap.of("link", url, "title", title));
    }

    private void startDownloadCache(int regionId) {
        sendCommand("download_offline_cache", ImmutableMap.of("region_id", regionId));
    }

    public void toggleDebugDriving() {
        sendCommand("toggle_debug_driving");
    }

    public void toggleDebugDriving(double speed) {
        sendCommand("toggle_debug_driving", ImmutableMap.of("speed", speed));
    }

    private void sendCommand(String command) {
        sendCommand(command, null);
    }

    private void sendCommand(String command, Map<String, Object> params) {
        user.openNaviUrl(command, params);
    }
}
