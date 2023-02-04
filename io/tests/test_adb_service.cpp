#include <yandex_io/services/adbd/adb_service.h>

#include <yandex_io/interfaces/user_config/mock/user_config_provider.h>
#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

using namespace quasar;
using namespace quasar::TestUtils;

namespace {
    class Api: public AdbService::Api {
    public:
        Api(std::function<int(const std::string&)> system)
            : system_(std::move(system))
        {
        }
        int system(const std::string& commandLine) const override {
            if (system_) {
                return system_(commandLine);
            }
            return -1;
        }

    private:
        std::function<int(const std::string&)> system_;
    };

    struct Fixture: public QuasarUnitTestFixture {
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            config["adbd"]["quasmodromActivateCmd"] = "run quasmodromActivateCmd";
            config["adbd"]["quasmodromNetworkActivateCmd"] = "run quasmodromNetworkActivateCmd";
            config["adbd"]["quasmodromDeactivateCmd"] = "run quasmodromDeactivateCmd";
            config["adbd"]["allDeactivateCmd"] = "run allDeactivateCmd";

            userConfigProvider = std::make_shared<mock::UserConfigProvider>(nullptr);
            lifecycle = std::make_shared<NamedCallbackQueue>("adbdTest");
            api = std::make_shared<Api>([this](auto cmd) { executedCommands.push_back(cmd); return 0; });
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        YandexIO::Configuration::TestGuard testGuard;

        std::vector<std::string> executedCommands;
        std::shared_ptr<mock::UserConfigProvider> userConfigProvider;
        std::shared_ptr<NamedCallbackQueue> lifecycle;
        std::shared_ptr<Api> api;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(AdbService, Fixture) {
    Y_UNIT_TEST(test)
    {
        AdbService adbService(getDeviceForTests(), userConfigProvider, lifecycle, api);

        std::atomic<bool> ready{false};

        lifecycle->add([&] { ready = true; });
        waitUntil([&] { return !!ready; });
        UNIT_ASSERT_VALUES_EQUAL(executedCommands.size(), 0);

        adbService.start();

        ready = false;
        lifecycle->add([&] { ready = true; });
        waitUntil([&] { return !!ready; });
        UNIT_ASSERT_VALUES_EQUAL(executedCommands.size(), 0); // no any actions until UserConfig will be applied

        UserConfig userConfig;
        userConfig.auth = UserConfig::Auth::SUCCESS;

        // (I) Turn on addb via quasmodrom
        userConfig.system["adbd"]["remoteActivation"] = true;
        userConfigProvider->setUserConfig(userConfig);

        ready = false;
        lifecycle->add([&] { ready = true; });
        waitUntil([&] { return !!ready; });
        UNIT_ASSERT_VALUES_EQUAL(executedCommands.size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(executedCommands.back(), "run quasmodromActivateCmd");

        // (II) Add extra flag "network"
        userConfig.system["adbd"]["remoteActivation"] = true;
        userConfig.system["adbd"]["network"] = true;
        userConfigProvider->setUserConfig(userConfig);

        ready = false;
        lifecycle->add([&] { ready = true; });
        waitUntil([&] { return !!ready; });
        UNIT_ASSERT_VALUES_EQUAL(executedCommands.size(), 2);
        UNIT_ASSERT_VALUES_EQUAL(executedCommands.back(), "run quasmodromNetworkActivateCmd");

        // (III) Turn off adb via quasmodrom
        userConfig.system["adbd"]["remoteActivation"] = false;
        userConfig.system["adbd"]["network"] = true;
        userConfigProvider->setUserConfig(userConfig);

        ready = false;
        lifecycle->add([&] { ready = true; });
        waitUntil([&] { return !!ready; });
        UNIT_ASSERT_VALUES_EQUAL(executedCommands.size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(executedCommands.back(), "run quasmodromDeactivateCmd");

        // (IV) Remove all instead of "network"
        userConfig.system = Json::Value();
        userConfig.system["adbd"]["network"] = true;
        userConfigProvider->setUserConfig(userConfig);

        ready = false;
        lifecycle->add([&] { ready = true; });
        waitUntil([&] { return !!ready; });
        UNIT_ASSERT_VALUES_EQUAL(executedCommands.size(), 3); // No actions

        // (V) forceDisable have maximum priority
        userConfig.system = Json::Value();
        userConfig.system["adbd"]["forceDisable"] = true;
        userConfig.system["adbd"]["remoteActivation"] = true;
        userConfig.system["adbd"]["network"] = true;
        userConfigProvider->setUserConfig(userConfig);

        ready = false;
        lifecycle->add([&] { ready = true; });
        waitUntil([&] { return !!ready; });
        UNIT_ASSERT_VALUES_EQUAL(executedCommands.size(), 4);
        UNIT_ASSERT_VALUES_EQUAL(executedCommands.back(), "run allDeactivateCmd");

        // (VI) forceDisable have maximum priority
        userConfig.system = Json::Value();
        userConfig.system["adbd"]["forceDisable"] = true;
        userConfig.system["adbd"]["remoteActivation"] = false;
        userConfig.system["adbd"]["network"] = true;
        userConfigProvider->setUserConfig(userConfig);

        ready = false;
        lifecycle->add([&] { ready = true; });
        waitUntil([&] { return !!ready; });
        UNIT_ASSERT_VALUES_EQUAL(executedCommands.size(), 4); // no actions
    }
}
