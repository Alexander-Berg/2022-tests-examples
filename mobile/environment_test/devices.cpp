#include <yandex/maps/navikit/devices.h>
#include <yandex/maps/navi/test_environment.h>

namespace yandex::maps::navikit {

bool isRunningInYaAuto() noexcept
{
    return getTestEnvironment()->config()->isRunningInYaAuto();
}

bool isRunningAsNavilib() noexcept
{
    return getTestEnvironment()->config()->isRunningAsNavilib();
}

bool isRunningInYaAutoCarsharing()
{
    return getTestEnvironment()->config()->isRunningInYaAutoCarsharing();
}

bool deviceIsVehicleWithoutYaAuto()
{
    return !isRunningInYaAuto() && (isRunningInMotrex() || isRunningInToyota());
}

bool isRunningInNissan()
{
    return getTestEnvironment()->config()->deviceManufacturer() == "nissan";
}

bool isRunningInChery()
{
    return getTestEnvironment()->config()->deviceManufacturer() == "chery";
}

bool isRunningInYaPhone()
{
    return getTestEnvironment()->config()->isRunningInYaPhone();
}

bool isRunningInMotrex()
{
    return getTestEnvironment()->config()->deviceManufacturer() == "motrex";
}

bool isRunningInGeely()
{
    return getTestEnvironment()->config()->deviceManufacturer() == "geely";
}

bool isRunningInMitsubishi()
{
    return getTestEnvironment()->config()->deviceManufacturer() == "mitsubishi";
}

bool isRunningInLada()
{
    return getTestEnvironment()->config()->deviceManufacturer() == "lada";
}

bool isRunningInToyota()
{
    return getTestEnvironment()->config()->deviceManufacturer() == "toyota";
}

bool isDeviceTablet()
{
    return false;
}

bool deviceHasCamera()
{
    return false;
}

bool deviceHasGooglePlayServices()
{
    return false;
}

bool deviceIsNaviOnTap()
{
    return false;
}

bool deviceIsVWNaviOnTap()
{
    return false;
}

bool deviceLollipopCompatible()
{
    return false;
}

}  // namespace yandex
