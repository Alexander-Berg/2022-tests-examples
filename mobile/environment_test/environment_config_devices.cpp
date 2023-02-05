#include <yandex/maps/navi/environment_config.h>

namespace yandex::maps::navikit {

using Device = EnvironmentConfig::Device;

namespace {

enum Category {
    NONE = 0,
    VEHICLE = 1 << 0,
    YA_AUTO = 1 << 1,
    // ==== COMBINED ====
    AUTOMOTIVE = VEHICLE | YA_AUTO
};

struct DeviceInfo {
    Device device;
    Category category;
};

const std::vector<DeviceInfo>& defaultDevices()
{
    static const std::vector<DeviceInfo> devices = {
        { Device::ANDROID, Category::NONE },
        { Device::CAR_NISSAN, Category::VEHICLE },
        { Device::IPHONE, Category::NONE },
        { Device::YA_AUTO, Category::AUTOMOTIVE },
        { Device::YA_CARSHARING, Category::YA_AUTO },
    };
    return devices;
}

std::vector<Device> getDevices(
    const std::function<bool(const DeviceInfo&)>& condition)
{
    std::vector<Device> devices;
    for (const DeviceInfo& deviceInfo : defaultDevices()) {
        if (condition(deviceInfo))
            devices.push_back(deviceInfo.device);
    }
    return devices;
}

}  // anonymous namespace

const Device Device::ANDROID = {
    EnvironmentConfig::Platform::Android,
    "Xiaomi", "Redmi 4X", Device::Feature::None
};

const Device Device::CAR_NISSAN = {
    EnvironmentConfig::Platform::Android,
    "nissan", "juke", Device::Feature::YaAuto
};

const Device Device::IPHONE = {
    EnvironmentConfig::Platform::Ios,
    "Apple", "iPhone 7", Device::Feature::None
};

const Device Device::YA_AUTO = {
    EnvironmentConfig::Platform::Android,
    "rockchip", "d200", Device::Feature::YaAuto
};

const Device Device::YA_CARSHARING = {
    EnvironmentConfig::Platform::Android,
    "tw", "Captur-astar-yaCS",
    Device::Feature::YaAuto | Device::Feature::YaCarsharing
};

const std::vector<Device>& Device::ALL()
{
    static const std::vector<EnvironmentConfig::Device> allDevices = getDevices(
        [](const DeviceInfo&) { return true; });
    return allDevices;
}

const std::vector<Device>& Device::VEHICLES()
{
    static const std::vector<EnvironmentConfig::Device> vehicles = getDevices(
        [](const DeviceInfo& deviceInfo) {
            return deviceInfo.category & Category::VEHICLE;
        });
    return vehicles;
}

const std::vector<Device>& Device::NOT_VEHICLES()
{
    static const std::vector<EnvironmentConfig::Device> notVehicles = getDevices(
        [](const DeviceInfo& deviceInfo) {
            return (deviceInfo.category & Category::VEHICLE) == 0;
        });
    return notVehicles;
}

const std::vector<Device>& Device::AUTOMOTIVE()
{
    static const std::vector<EnvironmentConfig::Device> automotive = getDevices(
        [](const DeviceInfo& deviceInfo) {
            return deviceInfo.category & Category::AUTOMOTIVE;
        });
    return automotive;
}

const std::vector<Device>& Device::NOT_AUTOMOTIVE()
{
    static const std::vector<EnvironmentConfig::Device> notAutomotive = getDevices(
        [](const DeviceInfo& deviceInfo) {
            return (deviceInfo.category & Category::AUTOMOTIVE) == 0;
        });
    return notAutomotive;
}

const std::vector<EnvironmentConfig::Device>& EnvironmentConfig::Device::ANDROID_DEVICES()
{
    static const std::vector<EnvironmentConfig::Device> androidDevices = getDevices(
        [](const DeviceInfo& deviceInfo) {
            return deviceInfo.device.platform == EnvironmentConfig::Platform::Android;
        });
    return androidDevices;
}

const std::vector<EnvironmentConfig::Device>& EnvironmentConfig::Device::IOS_DEVICES()
{
    static const std::vector<EnvironmentConfig::Device> iosDevices = getDevices(
        [](const DeviceInfo& deviceInfo) {
            return deviceInfo.device.platform == EnvironmentConfig::Platform::Ios;
        });
    return iosDevices;
}

const std::vector<EnvironmentConfig::Device>& EnvironmentConfig::Device::EMBEDDED_DEVICES()
{
    static const std::vector<EnvironmentConfig::Device> embeddedDevices = getDevices(
        [](const DeviceInfo& deviceInfo) {
            return deviceInfo.device.platform == EnvironmentConfig::Platform::Darwin;
        });
    return embeddedDevices;
}

}  // namespace yandex
