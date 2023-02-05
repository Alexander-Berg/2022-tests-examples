#include <yandex/maps/navi/test_environment.h>
#include <yandex/maps/navi/ui/features/features.h>

#include <boost/test/unit_test.hpp>

#include <vector>

namespace yandex::maps::navi::ui::features {

using Device = navikit::EnvironmentConfig::Device;

BOOST_AUTO_TEST_CASE(testSupportNewOfflineCache)
{
    const struct {
        std::vector<Device> devices;
        bool supportNewOfflineCache;
    }
    tests[] = {
        { Device::IOS_DEVICES(),      false },
        { Device::ANDROID_DEVICES(),  true  },
        { Device::EMBEDDED_DEVICES(), true  },
    };

    UI(
        for (const auto& test : tests) {
            for (const auto& device: test.devices) {
                auto config = std::make_unique<navikit::TestEnvironmentConfig>(device);
                auto handle = navikit::getTestEnvironment()->setConfig(std::move(config));

                BOOST_CHECK(supportNewOfflineCache() == test.supportNewOfflineCache);
            }
        }
    );
}

}
