#include <yandex/maps/mapkit/directions/driving/offline/data_facade.h>

#include <library/cpp/testing/common/env.h>

#include <cstdlib>
#include <string>

namespace yandex::maps::mapkit::directions::driving::internal {

std::string offlineCachePathForTests()
{
    return BinaryPath(
        "maps/mobile/libs/directions/driving/tests/offline-test-data/"
        "moscow.data");
}

} // namespace yandex::maps::mapkit::directions::driving::internal
