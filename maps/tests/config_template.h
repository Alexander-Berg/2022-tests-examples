#pragma once

#include <maps/libs/json/include/value.h>

namespace maps::wiki::autocart::pipeline::tests {

json::Value makeConfigFromTemplate(
    const std::string& configTempatePath, const std::string& tileSourceUrl);

} // namespace maps::wiki::autocart::pipeline::tests
