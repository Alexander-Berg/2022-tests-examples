#include <yandex_io/services/aliced/capabilities/alice_capability/preprocessors/alice_capability_preprocessor.h>

#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/logging/logging.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace testing;

using namespace YandexIO;

Y_UNIT_TEST_SUITE(AliceCapabilityPreprocessorTest) {
    Y_UNIT_TEST(testDrawLedDirectiveMovesToBeginning) {
        AliceDeviceState deviceState("", nullptr, nullptr, EnvironmentStateHolder("", nullptr));
        AliceCapabilityPreprocessor preprocessor(true, deviceState);

        Json::Value payload1;
        payload1["payload1"] = "";
        Json::Value payload2;
        payload2["payload2"] = "";

        std::list<std::shared_ptr<Directive>> directives = {
            YandexIO::Directive::createLocalAction(Directives::AUDIO_PLAY, Json::Value()),
            YandexIO::Directive::createLocalAction(Directives::DRAW_LED_SCREEN, payload1),
            YandexIO::Directive::createLocalAction(Directives::SHOW_VIDEO_SETTINGS, Json::Value()),
            YandexIO::Directive::createLocalAction(Directives::DRAW_LED_SCREEN, payload2)};

        preprocessor.preprocessDirectives(directives);

        ASSERT_TRUE(directives.size() == 4);

        auto item = directives.begin();
        ASSERT_TRUE((*item)->is(Directives::DRAW_LED_SCREEN));
        ASSERT_TRUE((*item)->getData().payload.isMember("payload1"));

        item = std::next(item);
        ASSERT_TRUE((*item)->is(Directives::DRAW_LED_SCREEN));
        ASSERT_TRUE((*item)->getData().payload.isMember("payload2"));

        item = std::next(item);
        ASSERT_TRUE((*item)->is(Directives::AUDIO_PLAY));

        item = std::next(item);
        ASSERT_TRUE((*item)->is(Directives::SHOW_VIDEO_SETTINGS));
    }

    Y_UNIT_TEST(testGoHomeDirectiveExpanding) {
        AliceDeviceState deviceState("", nullptr, nullptr, EnvironmentStateHolder("", nullptr));
        AliceCapabilityPreprocessor preprocessor(true, deviceState);

        std::list<std::shared_ptr<Directive>> directives = {
            YandexIO::Directive::createLocalAction(Directives::ALICE_RESPONSE),
            YandexIO::Directive::createLocalAction(Directives::GO_HOME)};

        preprocessor.preprocessDirectives(directives);

        ASSERT_TRUE(directives.size() == 3);

        auto item = directives.begin();
        ASSERT_TRUE((*item)->is(Directives::ALICE_RESPONSE));
        item = std::next(item);
        ASSERT_TRUE((*item)->is(Directives::PLAYER_PAUSE));
        item = std::next(item);
        ASSERT_TRUE((*item)->is(Directives::SHOW_HOME_SCREEN));
    }
}
