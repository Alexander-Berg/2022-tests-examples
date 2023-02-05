#pragma once

#include <yandex/maps/navi/environment_config.h>

namespace yandex::maps::navikit {

class EnvironmentConfigImpl : public EnvironmentConfig {
public:
    virtual const mapkit::experiments::UiExperimentsManager* uiExperimentsManager() const override;

    virtual const std::string& libLanguage() const override;

    virtual const std::string& libCountry() const override;

    virtual const boost::optional<std::string>& countryByOperatorInfo() const override;

    virtual const std::string& appLanguage() const override;

    virtual boost::optional<::yandex::maps::mapkit::location::Location> lastKnownLocation() const override;

    virtual const std::string& deviceModel() const override;

    virtual const std::string& deviceManufacturer() const override;

    virtual bool isRunningInYaAuto() const override;

    virtual bool isRunningAsNavilib() const override;

    virtual bool isRunningInYaAutoCarsharing() const override;

    virtual bool isRunningInYaPhone() const override;

    virtual Platform buildingFor() const override;

    virtual bool isProduction() const override;

    virtual bool supportWebview() const override;
};

}  // namespace yandex
