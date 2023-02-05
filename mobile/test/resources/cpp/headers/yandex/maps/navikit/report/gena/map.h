#pragma once

#include <yandex/maps/navikit/report/gena/reportable.h>

#include <map>
#include <optional>
#include <string>

namespace yandex::maps::navikit::report::gena {

namespace map {

enum class ChangeTrafficBackground {
    Map,
    Route,
    SearchResults,
};

enum class ChangeTrafficSource {
    ControlOnMap,
    LayerMenu,
};

/**
 * Пользователь включил / отключил отображение пробок
 * 1. Горизонтальная ориентация
 * 2. Вкл./Откл. пробоки
 * 3. в каком режиме находится пользователь (map - просто открыта карта, route - построен маршрут,  search-results - пины результатов поиска)
 */
Reportable changeTraffic(std::optional<bool> landscape, std::optional<bool> state, std::optional<ChangeTrafficBackground> background, std::optional<ChangeTrafficSource> source);

enum class ZoomInBackground {
    Map,
    Route,
    RoutePoints,
    SearchResults,
    Navigation,
    Roulette,
};

enum class ZoomInSource {
    Gesture,
    ZoomButton,
    ZoomButtonLongTap,
    VolumeButton,
};

/**
 * Пользователь увеличил зум на карте кнопками
 */
Reportable zoomIn(std::optional<ZoomInBackground> background, std::optional<bool> landscape, std::optional<ZoomInSource> source);

/**
 * Пользователь выключил вращение карты
 */
Reportable arrowOff(std::optional<float> currentScale, std::optional<bool> landscape);

enum class ChangeTiltType {
    Flat,
    Perspective,
};

enum class ChangeTiltAction {
    Gesture,
    Button,
};

/**
 * Пользователь изменил наклон карты
 */
Reportable changeTilt(std::optional<float> currentScale, std::optional<ChangeTiltType> type, std::optional<ChangeTiltAction> action);

enum class LocateUserState {
    Locate,
    ArrowOn,
    ArrowOff,
    StartSearching,
    StopSearching,
    Error,
};

/**
 * Пользователь нажал на кнопку «Моё местоположение»
 */
Reportable locateUser(std::optional<LocateUserState> state, std::optional<bool> landscape);

enum class LongTapBackground {
    Route,
    Navigation,
    Map,
    SearchResults,
};

/**
 * Пользователь сделал лонг-тап на карте. Параметр - что именно отображено на карте в момент лонг-тапа
 */
Reportable longTap(std::optional<LongTapBackground> background, std::optional<float> lat, std::optional<float> lon, std::optional<float> currentScale);

/**
 * Пользователь нажал на заехать через точку
 */
Reportable routeVia(std::optional<float> lat, std::optional<float> lon);

/**
 * Пользователь нажал на  «Что здесь?»
 */
Reportable whatHere();

}

}

