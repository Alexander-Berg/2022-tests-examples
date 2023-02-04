#include <yandex_io/services/aliced/capabilities/bluetooth_capability/preprocessors/bluetooth_capability_preprocessor.h>

#include <yandex_io/libs/base/directives.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <iterator>

using namespace quasar;
using namespace testing;

using namespace YandexIO;

Y_UNIT_TEST_SUITE(BluetoothDirectivePreprocessorTest) {
    Y_UNIT_TEST(testPlaybackDirectivesNotConverted_WhenPlayerIsNotBluetooth) {
        AliceDeviceState deviceState("", nullptr, nullptr, EnvironmentStateHolder("", nullptr));
        BluetoothCapabilityPreprocessor preprocessor(deviceState);

        Json::Value payload;
        payload["player"] = "music";
        auto continueDirective = Directive::createLocalAction(Directives::PLAYER_CONTINUE, payload);
        auto nextDirective = Directive::createLocalAction(Directives::PLAYER_NEXT_TRACK, payload);
        auto prevDirective = Directive::createLocalAction(Directives::PLAYER_PREVIOUS_TRACK, payload);

        std::list<std::shared_ptr<Directive>> input = {continueDirective, nextDirective, prevDirective};
        std::list<std::shared_ptr<Directive>> expected = input;
        preprocessor.preprocessDirectives(input);

        EXPECT_THAT(input, ContainerEq(expected));
    }

    Y_UNIT_TEST(testPlaybackDirectivesConverted_WhenPlayerIsBluetooth) {
        AliceDeviceState deviceState("", nullptr, nullptr, EnvironmentStateHolder("", nullptr));
        BluetoothCapabilityPreprocessor preprocessor(deviceState);

        Json::Value payload;
        payload["player"] = "bluetooth";
        std::list<std::shared_ptr<Directive>> input = {
            Directive::createLocalAction(Directives::PLAYER_CONTINUE, payload),
            Directive::createLocalAction(Directives::PLAYER_NEXT_TRACK, payload),
            Directive::createLocalAction(Directives::PLAYER_PREVIOUS_TRACK, payload)};

        preprocessor.preprocessDirectives(input);

        ASSERT_TRUE(input.size() == 3);
        auto item = input.begin();
        ASSERT_TRUE((*item)->is(Directives::BLUETOOTH_PLAYER_PLAY));
        item = std::next(item);
        ASSERT_TRUE((*item)->is(Directives::BLUETOOTH_PLAYER_NEXT));
        item = std::next(item);
        ASSERT_TRUE((*item)->is(Directives::BLUETOOTH_PLAYER_PREV));
    }

    Y_UNIT_TEST(testPlayerPausePreprocessing) {
        AliceDeviceState deviceState("", nullptr, nullptr, EnvironmentStateHolder("", nullptr));
        BluetoothCapabilityPreprocessor preprocessor(deviceState);

        {
            Json::Value payload;
            payload["player"] = "bluetooth";
            auto pauseDirective = Directive::createLocalAction(Directives::PLAYER_PAUSE, payload);
            std::list<std::shared_ptr<Directive>> input = {pauseDirective};

            preprocessor.preprocessDirectives(input);

            ASSERT_TRUE(input.size() == 1);
            ASSERT_TRUE((*input.begin())->is(Directives::PLAYER_PAUSE));
        }

        {
            proto::AppState appState;
            appState.mutable_bluetooth_player_state()->set_is_paused(false);
            deviceState.setAppState(appState);

            auto directive = Directive::createLocalAction(Directives::SOUND_LOUDER, Json::Value());
            std::list<std::shared_ptr<Directive>> input = {directive};

            preprocessor.preprocessDirectives(input);

            ASSERT_TRUE(input.size() == 1);
            ASSERT_TRUE((*input.begin())->is(Directives::SOUND_LOUDER));
        }

        {
            proto::AppState appState;
            appState.mutable_bluetooth_player_state()->set_is_paused(false);
            deviceState.setAppState(appState);

            for (const auto& mediaStartDirectiveName : {Directives::AUDIO_PLAY, Directives::MUSIC_PLAY, Directives::RADIO_PLAY, Directives::VIDEO_PLAY}) {
                auto directive = Directive::createLocalAction(mediaStartDirectiveName, Json::Value());
                std::list<std::shared_ptr<Directive>> input = {Directive::createLocalAction(Directives::TTS_PLAY_PLACEHOLDER, Json::Value()), directive};

                preprocessor.preprocessDirectives(input);

                ASSERT_TRUE(input.size() == 3);
                auto item = std::next(input.begin());
                ASSERT_TRUE((*item)->is(Directives::BLUETOOTH_PLAYER_PAUSE));
                item = std::next(item);
                ASSERT_TRUE((*item)->is(mediaStartDirectiveName));
            }
        }
    }
}
