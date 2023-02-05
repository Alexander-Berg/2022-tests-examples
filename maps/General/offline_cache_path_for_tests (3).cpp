#include <yandex/maps/mapkit/directions/driving/offline/data_facade.h>

namespace yandex::maps::mapkit::directions::driving::internal {

// TODO: use the same code, as used for the 'linux-ui' platform; as for now one
// of the dependencies, 'library/cpp/diff', cannot be built for the 'darwin-ui'
// platform.
std::string offlineCachePathForTests()
{
    return "";
}

} // namespace yandex::maps::mapkit::directions::driving::internal
