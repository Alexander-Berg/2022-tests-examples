#pragma once

#include <yandex/maps/navikit/report/gena/reportable.h>

#include <map>
#include <optional>
#include <string>

namespace yandex::maps::navikit::report::gena {

namespace application {

enum class StartSessionLayerType {
    Map,
    Satellite,
    Hybrid,
};

/**
 * Старт сессии
 * 
 * road_alerts - влючен ли слой дорожных событий
 * zoom_buttons_enabled - включены / выключены кнопки изменения масштаба на карте
 * layer_type - какой слой карты выбран для отображения
 * battery_charge - процент зарядки батареи
 * locale - локаль <язык телефона>_<регион телефона>
 * map_rotation - Вращение карты
 * show_ruler - Масштабная линейка
 * auto_rebuild - Автоперестроение маршрутов
 * auto_update - Автообновление кэшей карт
 * routes_in_navi - Маршруты в Навигаторе
 * wifi_only - Загрузка карт только по вай-фаю
 * avoid_toll_roads - Избегать платных дорог
 * show_public_transport_lables - настройку "Метки общественного транспорта" (только в Windows)
 * language - язык приложения
 * authorized - пользователь авторизован
 * traffic - включены пробки или нет
 * 
 */
Reportable startSession(std::optional<bool> roadAlerts, std::optional<bool> zoomButtonsEnabled, std::optional<StartSessionLayerType> layerType, std::optional<int> batteryCharge, std::optional<std::string> locale, std::optional<bool> mapRotation, std::optional<bool> showRuler, std::optional<bool> autoRebuild, std::optional<bool> autoUpdate, std::optional<bool> routesInNavi, std::optional<bool> wifiOnly, std::optional<bool> avoidTollRoads, std::optional<bool> showPublicTransportLables, std::optional<bool> soundsThroughBluetooth, std::optional<std::string> language, std::optional<bool> authorized, std::optional<bool> traffic);

enum class GetGlobalParamethersNightMode {
    True,
    False,
    Auto,
    System,
};

enum class GetGlobalParamethersLaunchType {
    FreshStart,
    FromBackground,
};

/**
 * Настройки приложения, которые могут требовать время для определения
 * 1. кол-во закладок
 * 1. кол-во списков в "Мои места"
 * 1. добавлена ли точка "Дом"
 * 1. добавлена ли точка "Работа"
 * 1. размер кэша в Гб
 * 1. значение настройки ночного режима
 * 1. если у пользователя 2 списка и 3 закладки в каждом, и стоит для обоих списков "отображать на карте", то посылаем 6; если нет закладок/не отобр-ся на карте, то посылаем 0
 * 1. разрешены ли push-уведомления системой
 * 1. время запуска в секундах
 * 1. для iOS - от начала выполнения функции didFinishLaunchingWithOptions, в которой у нас основная работа по инициализации приложения происходит.
 * 1. время исполнения каждого этапа запуска в виде сериализованного json, ((https://wiki.yandex-team.ru/maps/mobile/analytics/releases/ios/9.1/#izmenenijavlogirovanii описание здесь))
 * 1. тип запуска
 * 4. map_caches — количество офлайн кешей
 * 1. aon - включен ли АОН
 * 1. background-guidance - включено ли фоновое ведение
 * 1. voice - голос в ведении
 * 1. org_review - включены ли пуши про оценку организаций
 * 1. discovery_pushes - включены ли дискавери пуши
 * 1. stops_count — количество сохраненных остановок
 * 1. lines_count — количество сохраненных маршрутов транспортного средства
 * 
 * authorized - пользователь авторизован
 * traffic - включены пробки или нет
 * 
 */
Reportable getGlobalParamethers(std::optional<int> bookmarksCount, std::optional<int> listsCount, std::optional<bool> homeAdded, std::optional<bool> workAdded, std::optional<double> cacheSize, std::optional<GetGlobalParamethersNightMode> nightMode, std::optional<int> showBookmarksOnMap, std::optional<bool> pushNotifications, std::optional<double> launchTime, std::optional<double> launchFinishTime, std::optional<std::string> launchStepsTime, std::optional<GetGlobalParamethersLaunchType> launchType, std::optional<int> mapCaches, std::optional<bool> aon, std::optional<bool> backgroundGuidance, std::optional<std::string> voice, std::optional<bool> orgReview, std::optional<bool> discoveryPushes, std::optional<int> stopsCount, std::optional<int> linesCount, std::optional<bool> authorized, std::optional<bool> traffic);

/**
 * Набор экспериментов из MapKit
 */
Reportable getExperimentsInfo(std::optional<std::map<std::string, std::string>> dictionary);

/**
 * Пользователь увидел экран с предложением оценить приложение
 */
Reportable showRateMeAlert(std::optional<bool> firstTime);

enum class CloseRateMeAlertReason {
    Later,
    Rate,
    OuterTap,
};

/**
 * Пользователь закрыл алерт с просьбой оценить приложение
 */
Reportable closeRateMeAlert(std::optional<CloseRateMeAlertReason> reason, std::optional<int> ratings);

/**
 * Загрузились базовые тайлы и надписи - карта полностью готова к использованию пользователем.
 * 1. time - время, которое потребовалось для полной загрузки карты
 * 1. render_time - время отрисовки после инициализации
 */
Reportable mapReady(std::optional<float> time, std::optional<float> renderTime);

}

}

