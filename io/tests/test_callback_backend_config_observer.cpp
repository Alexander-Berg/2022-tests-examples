#include <yandex_io/modules/backend_config_observer/callback_backend_config_observer.h>

#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace YandexIO;

Y_UNIT_TEST_SUITE_F(TestCallbackBackendConfigObserver, QuasarUnitTestFixture) {
    Y_UNIT_TEST(skipUnknownNames) {

        std::vector<std::string> configsNames;

        CallbackBackendConfigObserver observer({"name1", "name2", "name3"}, [&](const auto name, const auto& /*payload*/) {
            configsNames.push_back(std::string(name));
        });

        observer.onSystemConfig("name2", "{}");
        observer.onSystemConfig("name4", "{}");
        observer.onSystemConfig("name1", "{}");
        observer.onSystemConfig("name5", "{}");
        observer.onSystemConfig("name3", "{}");
        observer.onSystemConfig("name2", "{}");

        UNIT_ASSERT_VALUES_EQUAL(configsNames.size(), 4);
        UNIT_ASSERT_VALUES_EQUAL(configsNames[0], "name2");
        UNIT_ASSERT_VALUES_EQUAL(configsNames[1], "name1");
        UNIT_ASSERT_VALUES_EQUAL(configsNames[2], "name3");
        UNIT_ASSERT_VALUES_EQUAL(configsNames[3], "name2");
    }

    Y_UNIT_TEST(brokenJson) {
        std::vector<std::string> configsNames;

        CallbackBackendConfigObserver observer({"name1", "name2", "name3"}, [&](const auto name, const auto& /*payload*/) {
            configsNames.push_back(std::string(name));
        });

        observer.onSystemConfig("name2", "");
        observer.onSystemConfig("name1", "{}");
        observer.onSystemConfig("name3", "not a json");

        UNIT_ASSERT_VALUES_EQUAL(configsNames.size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(configsNames[0], "name1");
    }

    Y_UNIT_TEST(verifyJson) {
        std::vector<std::string> configsNames;

        CallbackBackendConfigObserver observer({"name1", "name2", "name3"}, [&](const auto name, const auto& json) {
            configsNames.push_back(std::string(name));
            if (name == "name1") {
                UNIT_ASSERT(json.isNumeric());
                UNIT_ASSERT_VALUES_EQUAL(json.asInt(), 7);
            } else if (name == "name2") {
                UNIT_ASSERT(json.isString());
                UNIT_ASSERT_VALUES_EQUAL(json.asString(), "string");
            } else if (name == "name3") {
                UNIT_ASSERT(json.isNull());
            }
        });

        observer.onSystemConfig("name1", quasar::jsonToString(Json::Value(7)));
        observer.onSystemConfig("name2", quasar::jsonToString(Json::Value("string")));
        observer.onSystemConfig("name3", quasar::jsonToString(Json::nullValue));

        UNIT_ASSERT_VALUES_EQUAL(configsNames.size(), 3);
    }

} // TestCallbackBackendConfigObserver suite
