#include <yandex/maps/navikit/location.h>
#include <yandex/maps/navi/test_environment.h>

namespace yandex::maps::navikit {

boost::optional<mapkit::location::Location> lastKnownLocation()
{
    return getTestEnvironment()->config()->lastKnownLocation();
}

}  // namespace yandex
