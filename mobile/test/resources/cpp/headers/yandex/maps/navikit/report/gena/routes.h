#pragma once

#include <yandex/maps/navikit/report/gena/reportable.h>

#include <map>
#include <optional>
#include <string>

namespace yandex::maps::navikit::report::gena {

namespace routes {

/**
 * Изменение параметра маршрута. Изменение кнопки "Запомнить настройки" тоже логируется этим событием.
 */
Reportable changeOption(std::optional<std::string> routeType, std::optional<std::string> option, std::optional<std::string> state);

}

}

