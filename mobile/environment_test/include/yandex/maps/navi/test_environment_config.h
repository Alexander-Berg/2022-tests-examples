#pragma once

#include <yandex/maps/navi/environment_config.h>

namespace yandex::maps::navikit {

class TestEnvironmentConfig : public EnvironmentConfig {
public:
    TestEnvironmentConfig(const EnvironmentConfig::Device& device);

    virtual const mapkit::experiments::UiExperimentsManager* uiExperimentsManager() const override;
    TestEnvironmentConfig& setUiExperimentsManager(
            const std::shared_ptr<mapkit::experiments::UiExperimentsManager>& value);

    virtual const std::string& libLanguage() const override;
    TestEnvironmentConfig& setLibLanguage(const std::string& value);

    virtual const std::string& libCountry() const override;
    TestEnvironmentConfig& setLibCountry(const std::string& value);

    virtual const boost::optional<std::string>& countryByOperatorInfo() const override;
    TestEnvironmentConfig& setCountryByOperatorInfo(const boost::optional<std::string>& value);

    virtual const std::string& appLanguage() const override;
    TestEnvironmentConfig& setAppLanguage(const std::string& value);

    virtual boost::optional<::yandex::maps::mapkit::location::Location>
    lastKnownLocation() const override;
    TestEnvironmentConfig& setLastKnownLocation(
            const boost::optional<::yandex::maps::mapkit::location::Location>& value);

    virtual const std::string& deviceModel() const override;

    virtual const std::string& deviceManufacturer() const override;

    virtual bool isRunningInYaAuto() const override;

    virtual bool isRunningAsNavilib() const override;

    virtual bool isRunningInYaAutoCarsharing() const override;

    virtual bool isRunningInYaPhone() const override;

    virtual Platform buildingFor() const override;

    virtual bool isProduction() const override;
    TestEnvironmentConfig& setProduction(bool value);

    virtual bool supportWebview() const override;
    TestEnvironmentConfig& setSupportWebview(bool value);

private:
    const Device device_;

    // Experiment
    std::shared_ptr<mapkit::experiments::UiExperimentsManager> uiExperimentsManager_;

    // Library locale
    std::string libLanguage_;
    std::string libCountry_;
    boost::optional<std::string> countryByOperatorInfo_;

    // App language
    std::string appLanguage_;

    // Location
    boost::optional<::yandex::maps::mapkit::location::Location> lastKnownLocation_;

    // Build info
    bool production_ = false;
    bool supportWebview_ = false;
};


}  // namespace yandex
