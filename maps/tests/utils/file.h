#pragma once

#include <cstddef>
#include <string>
#include <vector>
#include <fstream>

namespace yandex::maps::jsapi::vector::test::utils {

    std::vector<std::ifstream::char_type> readFile(const std::string& path);

}
