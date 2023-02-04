#pragma once

#include "tests/boost-tests/include/hotspot_tools.h"
#include "tests/boost-tests/include/tools/transform_tools.h"
#include <yandex/maps/renderer/geometry/integer_types.h>

#include <yandex/maps/renderer5/core/MultiLayersFilter.h>

#include <rapidjson/document.h>

namespace maps { namespace renderer5 { namespace test { namespace vector {

inline uint32_t findUint(
    const rapidjson::Value& json,
    std::string name,
    uint32_t defValue)
{
    return json.HasMember(name.c_str())
        ? json.FindMember(name.c_str())->value.GetUint()
        : defValue;
}

inline uint32_t findInt(
    const rapidjson::Value& json,
    std::string name,
    uint32_t defValue)
{
    return json.HasMember(name.c_str())
        ? json.FindMember(name.c_str())->value.GetInt()
        : defValue;
}

inline double findDouble(
    const rapidjson::Value& json,
    std::string name,
    double defValue)
{
    return json.HasMember(name.c_str())
        ? json.FindMember(name.c_str())->value.GetDouble()
        : defValue;
}

} } } } // namespace maps::renderer5::test::vector
