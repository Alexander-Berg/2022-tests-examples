#include <yandex/maps/navikit/build_info.h>
#include <yandex/maps/navikit/devices.h>
#include <yandex/maps/navikit/features.h>
#include <yandex/maps/navi/test_environment.h>

#include <boost/test/unit_test.hpp>

namespace yandex {
namespace maps {
namespace navikit {

using namespace runtime;
using Device = EnvironmentConfig::Device;

namespace {

class EnvironmentTestTestFixture {
public:
    EnvironmentTestTestFixture() {}

    Handle setEnvironment(
        const Device& device,
        const std::function<void(TestEnvironmentConfig*)>& editConfig = [](TestEnvironmentConfig*){})
    {
        auto config = std::make_unique<TestEnvironmentConfig>(device);
        editConfig(config.get());
        return getTestEnvironment()->setConfig(std::move(config));
    }
};

} // anonymous namespace

BOOST_FIXTURE_TEST_SUITE(EnvironmentTestTests, EnvironmentTestTestFixture)

BOOST_AUTO_TEST_CASE(testLocale)
{
    UI(({
        const struct {
            std::string appLanguage;
            std::string libCountry;
            bool isRussianLocale;
        }
        tests[] = {
            { "ru", "RU", true  },
            { "en", "US", false }
        };

        for (const auto& test : tests) {
            auto handle = setEnvironment(Device::IPHONE, [&test](TestEnvironmentConfig* config) {
                config->
                    setAppLanguage(test.appLanguage).
                    setLibCountry(test.libCountry);
            });

            BOOST_CHECK(isRussianLocale() == test.isRussianLocale);
        }
    }));
}

BOOST_AUTO_TEST_CASE(testIsRunningIn)
{
    UI(({
        const struct {
            Device device;
            bool isRunningInNissan;
        }
        tests[] = {
            { Device::CAR_NISSAN, true  },
            { Device::IPHONE,     false }
        };

        for (const auto& test : tests) {
            auto handle = setEnvironment(test.device);

            BOOST_CHECK(isRunningInNissan() == test.isRunningInNissan);
        }
    }));
}

BOOST_AUTO_TEST_CASE(testBuildingFor)
{
    UI(({
        const struct {
            Device device;
            bool buildingForAndroid;
            bool buildingForDarwin;
            bool buildingForIos;
        }
        tests[] = {
            { Device::ANDROID, true,  false, false },
            { Device::IPHONE,  false, false, true  },
        };

        for (const auto& test : tests) {
            auto handle = setEnvironment(test.device);

            BOOST_CHECK(buildingForAndroid() == test.buildingForAndroid);
            BOOST_CHECK(buildingForDarwin() == test.buildingForDarwin);
            BOOST_CHECK(buildingForIos() == test.buildingForIos);
            BOOST_CHECK(buildingForEmbeddedSystem() == test.buildingForDarwin);
        }
    }));
}

BOOST_AUTO_TEST_CASE(testDeviceIsVehicleWithoutYaAuto)
{
    UI(({
        const struct {
            const Device& device;
            bool isVehicleWithoutYaAuto;
        } tests[] = {{Device::ANDROID, false},
                     {Device::CAR_NISSAN, false},
                     {Device::IPHONE, false},
                     {Device::YA_AUTO, false},
                     {Device::YA_CARSHARING, false},
        };

        for (const auto& test : tests) {
            auto handle = setEnvironment(test.device);
            BOOST_CHECK(deviceIsVehicleWithoutYaAuto() == test.isVehicleWithoutYaAuto);
        }
    }));
}

BOOST_AUTO_TEST_CASE(testIsAdSupported)
{
    UI(({
        const struct {
            const std::vector<Device>& devices;
            bool isAdSupported;
        }
        tests[] = {
            { Device::AUTOMOTIVE(),     false },
            { Device::NOT_AUTOMOTIVE(), true  }
        };

        for (const auto& test : tests) {
            for (const Device& device : test.devices) {
                auto handle = setEnvironment(device);

                BOOST_CHECK(isAdSupported() == test.isAdSupported);
            }
        }
    }));
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace navikit
} // namespace maps
} // namespace yandex
