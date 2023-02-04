#include <yandex_io/services/aliced/capabilities/stereo_pair_capability/stereo_pair_directives.h>
#include <yandex_io/services/aliced/capabilities/stereo_pair_capability/stereo_pair_preprocessor.h>

#include <yandex_io/interfaces/stereo_pair/mock/stereo_pair_provider.h>
#include <yandex_io/interfaces/user_config/mock/user_config_provider.h>
#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/logging/logging.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace testing;
using namespace YandexIO;

namespace {

    Y_UNIT_TEST_SUITE(StereoPairPreprocessor) {

        Y_UNIT_TEST(testPreprocess1) {
            std::shared_ptr<mock::UserConfigProvider> mockUserConfigProvider = std::make_shared<mock::UserConfigProvider>(nullptr);
            std::shared_ptr<mock::StereoPairProvider> mockStereoPairProvider = std::make_shared<mock::StereoPairProvider>();
            mockStereoPairProvider->setStereoPairState(
                StereoPairState{
                    .role = StereoPairState::Role::STANDALONE,
                });
            std::list<std::shared_ptr<Directive>> directives = {
                YandexIO::Directive::createLocalAction(Directives::START_MULTIROOM, Json::Value()),
                YandexIO::Directive::createLocalAction(Directives::AUDIO_PLAY, Json::Value()),
                YandexIO::Directive::createServerAction("get_next"),
            };
            auto copyOfDirectives = directives;
            StereoPairPreprocessor pp(mockStereoPairProvider, mockUserConfigProvider);
            pp.preprocessDirectives(directives);
            UNIT_ASSERT(copyOfDirectives == directives);
        }

        Y_UNIT_TEST(testPreprocess2) {
            std::shared_ptr<mock::UserConfigProvider> mockUserConfigProvider = std::make_shared<mock::UserConfigProvider>(nullptr);
            std::shared_ptr<mock::StereoPairProvider> mockStereoPairProvider = std::make_shared<mock::StereoPairProvider>();
            mockStereoPairProvider->setStereoPairState(
                StereoPairState{
                    .role = StereoPairState::Role::LEADER,
                    .stereoPlayerStatus = StereoPairState::StereoPlayerStatus::NO_SYNC,
                });
            std::list<std::shared_ptr<Directive>> directives = {
                YandexIO::Directive::createLocalAction(Directives::START_MULTIROOM, Json::Value()),
                YandexIO::Directive::createLocalAction(Directives::AUDIO_PLAY, Json::Value()),
                YandexIO::Directive::createServerAction("get_next"),
            };

            StereoPairPreprocessor pp(mockStereoPairProvider, mockUserConfigProvider);

            pp.preprocessDirectives(directives);
            UNIT_ASSERT(directives.size() == 1);
            UNIT_ASSERT(directives.front()->is(StereoPairDirectives::PLAYER_NOT_READY_NOTIFICATION));
        }

        Y_UNIT_TEST(testPreprocess3) {
            std::shared_ptr<mock::UserConfigProvider> mockUserConfigProvider = std::make_shared<mock::UserConfigProvider>(nullptr);
            std::shared_ptr<mock::StereoPairProvider> mockStereoPairProvider = std::make_shared<mock::StereoPairProvider>();
            mockStereoPairProvider->setStereoPairState(
                StereoPairState{
                    .role = StereoPairState::Role::LEADER,
                    .stereoPlayerStatus = StereoPairState::StereoPlayerStatus::READY,
                });
            std::list<std::shared_ptr<Directive>> directives = {
                YandexIO::Directive::createLocalAction(Directives::START_MULTIROOM, Json::Value()),
                YandexIO::Directive::createLocalAction(Directives::AUDIO_PLAY, Json::Value()),
                YandexIO::Directive::createServerAction("get_next"),
            };
            auto copyOfDirectives = directives;
            StereoPairPreprocessor pp(mockStereoPairProvider, mockUserConfigProvider);
            pp.preprocessDirectives(directives);
            UNIT_ASSERT(copyOfDirectives == directives);
        }

        Y_UNIT_TEST(testPreprocess4) {
            std::shared_ptr<mock::UserConfigProvider> mockUserConfigProvider = std::make_shared<mock::UserConfigProvider>(nullptr);
            std::shared_ptr<mock::StereoPairProvider> mockStereoPairProvider = std::make_shared<mock::StereoPairProvider>();
            mockStereoPairProvider->setStereoPairState(
                StereoPairState{
                    .role = StereoPairState::Role::FOLLOWER,
                    .stereoPlayerStatus = StereoPairState::StereoPlayerStatus::READY,
                });
            std::list<std::shared_ptr<Directive>> directives = {
                YandexIO::Directive::createLocalAction(Directives::START_MULTIROOM, Json::Value()),
                YandexIO::Directive::createLocalAction(Directives::AUDIO_PLAY, Json::Value()),
                YandexIO::Directive::createServerAction("get_next"),
            };

            StereoPairPreprocessor pp(mockStereoPairProvider, mockUserConfigProvider);

            pp.preprocessDirectives(directives);
            UNIT_ASSERT(directives.size() == 0);
        }

        Y_UNIT_TEST(testPreprocess5) {
            std::shared_ptr<mock::UserConfigProvider> mockUserConfigProvider = std::make_shared<mock::UserConfigProvider>(nullptr);
            std::shared_ptr<mock::StereoPairProvider> mockStereoPairProvider = std::make_shared<mock::StereoPairProvider>();
            mockStereoPairProvider->setStereoPairState(
                StereoPairState{
                    .role = StereoPairState::Role::FOLLOWER,
                    .stereoPlayerStatus = StereoPairState::StereoPlayerStatus::READY,
                });

            StereoPairPreprocessor pp(mockStereoPairProvider, mockUserConfigProvider);

            for (const auto& directive : std::list<std::string>{
                     Directives::AUDIO_PLAY,
                     Directives::MUSIC_PLAY,
                     Directives::RADIO_PLAY,
                     Directives::VIDEO_PLAY,
                     Directives::SET_TIMER,
                     Directives::RESUME_TIMER,
                     Directives::START_BLUETOOTH,
                     Directives::START_MUSIC_RECOGNIZER,
                     Directives::START_MULTIROOM,
                     Directives::BLUETOOTH_PLAYER_PLAY}) {
                std::list<std::shared_ptr<Directive>> directives = {YandexIO::Directive::createLocalAction(directive, {})};
                pp.preprocessDirectives(directives);
                UNIT_ASSERT(directives.size() == 0);
            }
        }

        Y_UNIT_TEST(testPreprocess7) {
            std::shared_ptr<mock::UserConfigProvider> mockUserConfigProvider = std::make_shared<mock::UserConfigProvider>(nullptr);
            std::shared_ptr<mock::StereoPairProvider> mockStereoPairProvider = std::make_shared<mock::StereoPairProvider>();
            mockStereoPairProvider->setStereoPairState(
                StereoPairState{
                    .role = StereoPairState::Role::FOLLOWER,
                    .stereoPlayerStatus = StereoPairState::StereoPlayerStatus::READY,
                });
            std::list<std::shared_ptr<Directive>> directives = {
                YandexIO::Directive::createLocalAction(Directives::PLAYER_PAUSE, {}),
                YandexIO::Directive::createLocalAction(Directives::SOUND_SET_LEVEL, {}),
            };

            StereoPairPreprocessor pp(mockStereoPairProvider, mockUserConfigProvider);

            pp.preprocessDirectives(directives);
            UNIT_ASSERT(directives.size() == 2);
        }

    }

} // namespace
