#include <yandex/maps/navi/test_environment.h>

namespace yandex::maps::navikit {

std::string deviceModel()
{
    return getTestEnvironment()->config()->deviceModel();
}

std::string deviceManufacturer()
{
    return getTestEnvironment()->config()->deviceManufacturer();
}

}  // namespace yandex
