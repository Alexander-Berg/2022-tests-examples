#pragma once

#include <maps/libs/json/include/value.h>

#include <exception>
#include <optional>

namespace infopoint {

void assertJsonValuesEqual(
    const maps::json::Value& actual,
    const maps::json::Value& expected,
    std::string_view prefix = "");

} // namespace infopoint
