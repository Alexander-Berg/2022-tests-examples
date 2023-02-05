#pragma once

#include <yandex/maps/navikit/report/gena/reportable.h>

#include <map>
#include <optional>
#include <string>

namespace yandex::maps::navikit::report::gena {

namespace webview {

enum class LoadedType {
    Direct,
};

/**
 * Событие окончания загрузки вебвью.
 */
Reportable loaded(std::optional<LoadedType> type);

}

}

