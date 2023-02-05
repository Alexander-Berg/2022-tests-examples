#include "environment_config_impl.h"

#include <yandex/maps/mapkit/mapkit_factory.h>

#include <yandex/maps/runtime/device/device_info.h>
#include <yandex/maps/runtime/locale/locale.h>
#include <yandex/maps/runtime/platform.h>

namespace yandex::maps::navikit {

const mapkit::experiments::UiExperimentsManager* EnvironmentConfigImpl::uiExperimentsManager() const
{
    return mapkit::getMapKit()->uiExperimentsManager();
}

const std::string& EnvironmentConfigImpl::libLanguage() const
{
    static const std::string libLanguage = runtime::locale::libLanguage();
    return libLanguage;
}

const std::string& EnvironmentConfigImpl::libCountry() const
{
    static const std::string libCountry = runtime::locale::libCountry();
    return libCountry;
}

const boost::optional<std::string>& EnvironmentConfigImpl::countryByOperatorInfo() const
{
    static const boost::optional<std::string> countryByOperatorInfo = runtime::locale::countryByOperatorInfo();
    return countryByOperatorInfo;
}

const std::string& EnvironmentConfigImpl::appLanguage() const
{
    static const std::string appLanguage = "ru";
    return appLanguage;
}

boost::optional<::yandex::maps::mapkit::location::Location> EnvironmentConfigImpl::lastKnownLocation() const
{
    return mapkit::location::lastKnownLocation();
}

const std::string& EnvironmentConfigImpl::deviceModel() const
{
    static const std::string deviceModel = runtime::device::model();
    return deviceModel;
}

const std::string& EnvironmentConfigImpl::deviceManufacturer() const
{
    static const std::string deviceManufacturer = runtime::device::manufacturer();
    return deviceManufacturer;
}

bool EnvironmentConfigImpl::isRunningInYaAuto() const
{
    return false;
}

bool EnvironmentConfigImpl::isRunningAsNavilib() const
{
    return false;
}

bool EnvironmentConfigImpl::isRunningInYaAutoCarsharing() const
{
    return false;
}

bool EnvironmentConfigImpl::isRunningInYaPhone() const
{
    return false;
}

EnvironmentConfig::Platform EnvironmentConfigImpl::buildingFor() const
{
#if defined(BUILDING_FOR_IOS)
    return Platform::Ios;
#elif defined(BUILDING_FOR_ANDROID)
    return Platform::Android;
#elif defined(BUILDING_FOR_DARWIN)
    return Platform::Darwin;
#elif defined(BUILDING_FOR_LINUX)
    return Platform::Linux;
#else
    ASSERT(false);
#endif
}

bool EnvironmentConfigImpl::isProduction() const
{
#ifdef NAVI_PROD
    return true;
#else
    return false;
#endif
}

bool EnvironmentConfigImpl::supportWebview() const
{
#ifdef SUPPORT_WEBVIEW
    return true;
#else
    return false;
#endif
}

}  // namespace yandex
