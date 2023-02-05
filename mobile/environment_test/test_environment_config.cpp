#include <yandex/maps/navikit/mocks/mock_ui_experiments_manager.h>
#include <yandex/maps/navi/test_environment_config.h>

namespace yandex::maps::navikit {

using Device = EnvironmentConfig::Device;

////////////////////////////////////////////////////////////////////////////////
//
// TestEnvironmentConfig implementation
//

TestEnvironmentConfig::TestEnvironmentConfig(const Device& device)
    : device_(device)
    , uiExperimentsManager_(
        std::make_shared<testing::NiceMock<mapkit::experiments::MockUiExperimentsManager>>())
    , libLanguage_("ru")
    , libCountry_("RU")
    , appLanguage_("ru")
{}

const mapkit::experiments::UiExperimentsManager* TestEnvironmentConfig::uiExperimentsManager() const
{
    return uiExperimentsManager_.get();
}

TestEnvironmentConfig& TestEnvironmentConfig::setUiExperimentsManager(
        const std::shared_ptr<mapkit::experiments::UiExperimentsManager>& value)
{
    uiExperimentsManager_ = value;

    return *this;
}

const std::string& TestEnvironmentConfig::libLanguage() const
{
    return libLanguage_;
}

TestEnvironmentConfig& TestEnvironmentConfig::setLibLanguage(const std::string& value)
{
    libLanguage_ = value;

    return *this;
}

const std::string& TestEnvironmentConfig::libCountry() const
{
    return libCountry_;
}

TestEnvironmentConfig& TestEnvironmentConfig::setLibCountry(const std::string& value)
{
    libCountry_ = value;

    return *this;
}

const std::string& TestEnvironmentConfig::appLanguage() const
{
    return appLanguage_;
}

TestEnvironmentConfig& TestEnvironmentConfig::setAppLanguage(const std::string& value)
{
    appLanguage_ = value;

    return *this;
}

boost::optional<::yandex::maps::mapkit::location::Location> TestEnvironmentConfig::lastKnownLocation() const
{
    return lastKnownLocation_;
}

TestEnvironmentConfig& TestEnvironmentConfig::setLastKnownLocation(
    const boost::optional<::yandex::maps::mapkit::location::Location>& value)
{
    lastKnownLocation_ = value;

    return *this;
}

const boost::optional<std::string>& TestEnvironmentConfig::countryByOperatorInfo() const
{
    return countryByOperatorInfo_;
}

TestEnvironmentConfig& TestEnvironmentConfig::setCountryByOperatorInfo(
    const boost::optional<std::string>& value)
{
    countryByOperatorInfo_ = value;

    return *this;
}

const std::string& TestEnvironmentConfig::deviceModel() const
{
    return device_.model;
}

const std::string& TestEnvironmentConfig::deviceManufacturer() const
{
    return device_.manufacturer;
}

bool TestEnvironmentConfig::isRunningInYaAuto() const
{
    return device_.features & Device::Feature::YaAuto;
}

bool TestEnvironmentConfig::isRunningAsNavilib() const
{
    return device_.features & Device::Feature::YaAuto;
}

bool TestEnvironmentConfig::isRunningInYaAutoCarsharing() const
{
    return device_.features & Device::Feature::YaCarsharing;
}

bool TestEnvironmentConfig::isRunningInYaPhone() const
{
    return device_.features & Device::Feature::YaPhone;
}

TestEnvironmentConfig::Platform TestEnvironmentConfig::buildingFor() const
{
    return device_.platform;
}

bool TestEnvironmentConfig::isProduction() const
{
    return production_;
}

TestEnvironmentConfig& TestEnvironmentConfig::setProduction(bool value)
{
    production_ = value;

    return *this;
}

bool TestEnvironmentConfig::supportWebview() const
{
    return supportWebview_;
}

TestEnvironmentConfig& TestEnvironmentConfig::setSupportWebview(bool value)
{
    supportWebview_ = value;

    return *this;
}

}  // namespace yandex
