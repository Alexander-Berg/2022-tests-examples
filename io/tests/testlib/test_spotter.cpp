#include "test_spotter.h"

#include <yandex_io/libs/base/utils.h>

#include <library/cpp/testing/common/env.h>

using namespace quasar::TestUtils;
using namespace quasar;

Spotter quasar::TestUtils::createSpotter() {
    std::string spotterTgz = getFileContent(ArcadiaSourceRoot() + "/yandex_io/misc/custom_spotter/spotter.tgz");
    std::string spotterZip = getFileContent(ArcadiaSourceRoot() + "/yandex_io/misc/custom_spotter/spotter.zip");

    Spotter result;
    result.gzipData = spotterTgz;
    result.zipData = spotterZip;
    result.crc32 = 4042486118;

    return result;
}
