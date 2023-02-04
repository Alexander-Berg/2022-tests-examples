#pragma once

#include <maps/libs/json/include/value.h>

#include <string>
#include <vector>

namespace maps::renderer::data_set_test_util {

void matchSchema(const std::string& sourceSchemaPath,
                 const std::string& generatedSchema);

std::string generateSchema(
    const std::vector<std::string>& v3Tiles);

} // namespace maps::renderer::data_set_test_util
