#pragma once

#include <yandex/maps/mapkit/location/location.h>
#include <yandex/maps/runtime/connectivity/connectivity_status.h>

#include <boost/optional.hpp>

namespace yandex::maps::mapkit::experiments {

class UiExperimentsManager;

}  // namespace yandex

namespace yandex::maps::navikit {

class EnvironmentConfig {
public:
    enum class Platform {
        Android,
        Ios,
        Darwin,
        Linux
    };

    struct Device {
        enum Feature {
            None          = 0,
            YaAuto        = 1 << 0,
            YaCarsharing  = 1 << 1,
            YaPhone       = 1 << 2
        };

        Platform platform;
        std::string manufacturer;
        std::string model;
        unsigned int features;

        static const Device ANDROID;
        static const Device CAR_NISSAN;
        static const Device IPHONE;
        static const Device YA_AUTO;
        static const Device YA_CARSHARING;

        static const std::vector<Device>& ALL();

        // Vehicles with Navigator and without YaAuto
        static const std::vector<Device>& VEHICLES();
        static const std::vector<Device>& NOT_VEHICLES();

        // Any vehicles with Navigator
        static const std::vector<Device>& AUTOMOTIVE();
        static const std::vector<Device>& NOT_AUTOMOTIVE();

        static const std::vector<Device>& ANDROID_DEVICES();
        static const std::vector<Device>& IOS_DEVICES();
        static const std::vector<Device>& EMBEDDED_DEVICES();
    };

    virtual ~EnvironmentConfig() = default;

    virtual const mapkit::experiments::UiExperimentsManager* uiExperimentsManager() const = 0;

    virtual const std::string& libLanguage() const = 0;

    virtual const std::string& libCountry() const = 0;

    virtual const boost::optional<std::string>& countryByOperatorInfo() const = 0;

    virtual const std::string& appLanguage() const = 0;

    virtual boost::optional<::yandex::maps::mapkit::location::Location> lastKnownLocation() const = 0;

    virtual const std::string& deviceModel() const = 0;

    virtual const std::string& deviceManufacturer() const = 0;

    virtual bool isRunningInYaAuto() const = 0;

    virtual bool isRunningAsNavilib() const = 0;

    virtual bool isRunningInYaAutoCarsharing() const = 0;

    virtual bool isRunningInYaPhone() const = 0;

    virtual Platform buildingFor() const = 0;

    virtual bool isProduction() const = 0;

    virtual bool supportWebview() const = 0;
};

}  // namespace yandex
