#include <yandex_io/services/aliced/capabilities/audio_player_capability/audio_player_preprocessor.h>

#include <yandex_io/libs/base/directives.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace testing;

using namespace YandexIO;

Y_UNIT_TEST_SUITE(AudioPlayerPreprocessorTest) {
    Y_UNIT_TEST(testMarkGetNextDirectives) {
        AudioPlayerPreprocessor preprocessor;

        std::list<std::shared_ptr<Directive>> directives = {
            YandexIO::Directive::createServerAction("get_next"),
            YandexIO::Directive::createLocalAction(Directives::AUDIO_PLAY, Json::Value()),
            YandexIO::Directive::createServerAction("get_next"),
            YandexIO::Directive::createLocalAction(Directives::AUDIO_PLAY, Json::Value()),
            YandexIO::Directive::createServerAction("get_next"),
        };

        preprocessor.preprocessDirectives(directives);

        bool isFirst = true;
        for (const auto& directive : directives) {
            if (isFirst) {
                isFirst = false;
                ASSERT_FALSE(directive->isGetNext());
            } else if (directive->getData().isServerAction()) {
                ASSERT_TRUE(directive->isGetNext());
            }
        }
    }

    Y_UNIT_TEST(testStopLegacyPlayer_WhenTinyIsStarted) {
        AudioPlayerPreprocessor preprocessor;

        std::list<std::shared_ptr<Directive>> directives = {
            YandexIO::Directive::createLocalAction(Directives::AUDIO_PLAY, Json::Value()),
            YandexIO::Directive::createLocalAction(Directives::SHOW_DESCRIPTION, Json::Value()),
            YandexIO::Directive::createLocalAction(Directives::SHOW_DESCRIPTION, Json::Value()),
            YandexIO::Directive::createLocalAction(Directives::AUDIO_PLAY, Json::Value()),
        };

        preprocessor.preprocessDirectives(directives);
        for (const auto& dir : directives) {
            std::cerr << dir->getData().name << ",";
        }

        auto item = directives.begin();
        ASSERT_TRUE(directives.size() == 6);
        ASSERT_TRUE((*item)->is(Directives::STOP_LEGACY_PLAYER));
        item = std::next(item);
        ASSERT_TRUE((*item)->is(Directives::AUDIO_PLAY));
        item = std::next(item);
        ASSERT_TRUE((*item)->is(Directives::SHOW_DESCRIPTION));
        item = std::next(item);
        ASSERT_TRUE((*item)->is(Directives::SHOW_DESCRIPTION));
        item = std::next(item);
        ASSERT_TRUE((*item)->is(Directives::STOP_LEGACY_PLAYER));
        item = std::next(item);
        ASSERT_TRUE((*item)->is(Directives::AUDIO_PLAY));
        item = std::next(item);
    }

    Y_UNIT_TEST(testAddClearQueue_ForLegacyPlayerDirectives) {

        for (const auto name : {
                 quasar::Directives::MUSIC_PLAY,
                 quasar::Directives::RADIO_PLAY,
                 quasar::Directives::VIDEO_PLAY,
                 quasar::Directives::PLAYER_CONTINUE,
                 quasar::Directives::PLAYER_NEXT_TRACK,
                 quasar::Directives::PLAYER_PREVIOUS_TRACK,
                 quasar::Directives::PLAYER_REPLAY,
                 quasar::Directives::PLAYER_DISLIKE,
                 quasar::Directives::GO_HOME})
        {
            AudioPlayerPreprocessor preprocessor;
            std::list<std::shared_ptr<Directive>> directives = {Directive::createLocalAction(name)};

            preprocessor.preprocessDirectives(directives);

            ASSERT_TRUE(directives.size() == 2);
            auto item = directives.begin();
            ASSERT_TRUE((*item)->is(quasar::Directives::CLEAR_QUEUE));
            item = std::next(item);
            ASSERT_TRUE((*item)->is(name));
        }
    }

    Y_UNIT_TEST(testAudioPlayDirectivesMarkedPrefetchBlockers) {

        std::list<std::shared_ptr<Directive>> directives = {
            Directive::createLocalAction(Directives::TTS_PLAY_PLACEHOLDER),
            Directive::createLocalAction(Directives::AUDIO_PLAY),
            Directive::createLocalAction(Directives::ALICE_REQUEST),
        };

        AudioPlayerPreprocessor preprocessor;
        preprocessor.preprocessDirectives(directives);

        for (const auto& directive : directives) {
            if (directive->is(Directives::AUDIO_PLAY)) {
                ASSERT_TRUE(directive->isBlocksSubsequentPrefetch());
            }
        }
    }
}
