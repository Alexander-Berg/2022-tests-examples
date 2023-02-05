#include <yandex/maps/navikit/build_info.h>
#include <yandex/maps/navi/test_environment.h>

namespace yandex::maps::navikit {

bool buildingForAndroid()
{
    return getTestEnvironment()->config()->buildingFor() == EnvironmentConfig::Platform::Android;
}

bool buildingForIos()
{
    return getTestEnvironment()->config()->buildingFor() == EnvironmentConfig::Platform::Ios;
}

bool buildingForDarwin()
{
    return getTestEnvironment()->config()->buildingFor() == EnvironmentConfig::Platform::Darwin;
}

bool buildingForLinux()
{
    return getTestEnvironment()->config()->buildingFor() == EnvironmentConfig::Platform::Linux;
}

bool buildingForEmbeddedSystem()
{
    return buildingForDarwin() || buildingForLinux();
}

bool isProduction()
{
    return getTestEnvironment()->config()->isProduction();
}

bool supportWebview()
{
    return getTestEnvironment()->config()->supportWebview();
}

}  // namespace yandex
