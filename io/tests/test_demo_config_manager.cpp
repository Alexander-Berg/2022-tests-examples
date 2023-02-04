#include "demo_config_manager.h"

#include <yandex_io/modules/demo_mode/demo_provider_interface/null/null_demo_provider.h>

#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/tempdir.h>

#include <fstream>
#include <future>
#include <memory>
#include <stdlib.h>

using namespace quasar;
using namespace YandexIO;

namespace {
    class Fixture: public QuasarUnitTestFixture {
    public:
        TTempDir demoModeCacheDir;
        TTempDir demoModeConfigDir;
        std::string demoModeCachePath;
        std::string demoModeConfigPath;

        Fixture() {
            demoModeConfigPath = demoModeConfigDir.Name() + "/demo_config.json";
            demoModeCachePath = demoModeCacheDir.Name();
        }

        Json::Value createConfig() const {
            Json::Value res;
            res["demoModeCachePath"] = demoModeCachePath;
            res["demoModeConfigPath"] = demoModeCachePath;
            return res;
        }
    };

    class TestDemoProvider: public NullDemoProvider {
    public:
        TestDemoProvider(const std::string& cachePath, const std::string& itemUrl)
            : item_(cachePath, itemUrl)
        {
        }

        void updateDemoItems(const Json::Value& items) override {
            if (items.isString()) {
                item_.sound.url = items.asString();
            }
            updatePromise.set_value();
        }

        std::deque<DemoItem> getAllDemoItems() const override {
            return {item_};
        }

        Json::Value toJson() const override {
            return Json::Value(item_.sound.url);
        }

        std::promise<void> updatePromise;

    private:
        DemoItem item_;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(DemoConfigManagerTest, Fixture) {
    Y_UNIT_TEST(testDemoUrlConfig) {
        auto provider = std::make_shared<TestDemoProvider>(demoModeCachePath, "some_url");
        DemoConfigManager configManager(createConfig(), getDeviceForTests(), provider);
        std::string url = "/weird_url,/:1234";
        configManager.onSystemConfig(DemoConfigManager::DEMO_MODE_SOUND_URL, "\"" + url + "\"");
        provider->updatePromise.get_future().get();
        UNIT_ASSERT_EQUAL(provider->getAllDemoItems().front().sound.url, url);
    }

    Y_UNIT_TEST(testDemoConfig) {
        auto provider = std::make_shared<TestDemoProvider>(demoModeCachePath, "/weird_url,/:1234");
        DemoConfigManager configManager(createConfig(), getDeviceForTests(), provider);
        UNIT_ASSERT(!configManager.getPauseBetweenItems().has_value());
        configManager.onSystemConfig(DemoConfigManager::DEMO_MODE_CONFIG, "{\"pauseBetweenStoriesMs\": 1000}");
        provider->updatePromise.get_future().get();
        UNIT_ASSERT_EQUAL(configManager.getPauseBetweenItems().value(), std::chrono::seconds(1));
    }
}
