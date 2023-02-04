#include <cstdint>
#include "file.h"

namespace yandex::maps::jsapi::vector::test::utils {

std::vector<std::ifstream::char_type> readFile(const std::string& path) {
    std::ifstream input(path, std::ios::in | std::ios::binary);

    input.seekg(0, std::ios_base::end);
    std::vector<std::ifstream::char_type> data(input.tellg());
    input.seekg(0).read(data.data(), data.size());

    return data;
}

}
