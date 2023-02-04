#pragma once

#include <string>

namespace quasar::TestUtils {

    struct Spotter {
        std::string gzipData;
        std::string zipData;
        uint32_t crc32;
    };

    Spotter createSpotter();

} // namespace quasar::TestUtils
