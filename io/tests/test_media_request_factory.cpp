#include <yandex_io/services/aliced/capabilities/audio_player_capability/media_request_factory.h>

#include <yandex_io/sdk/interfaces/directive.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace testing;

namespace {

    Json::Value makeDefaultPayload(const std::string& streamId) {
        Json::Value json;
        json["name"] = "audio_play";
        json["type"] = "client_action";
        json["payload"]["stream"]["id"] = streamId;
        return json;
    }

} // namespace

Y_UNIT_TEST_SUITE_F(TestMediaRequestFactory, QuasarUnitTestFixture) {
    Y_UNIT_TEST(CheckValidNormalization) {
        const auto doublesEquals = [](double left, double right) -> bool {
            return std::abs(left - right) < std::numeric_limits<double>::epsilon();
        };

        auto json = makeDefaultPayload("stream-id");
        constexpr double truePeak = 0.7;
        constexpr double loudness = 3.4;
        json["payload"]["stream"]["normalization"]["true_peak"] = truePeak;
        json["payload"]["stream"]["normalization"]["integrated_loudness"] = loudness;
        auto directive = std::make_shared<YandexIO::Directive>(YandexIO::Directive::Data::fromJson(json));

        auto command = MediaRequestFactory::createPlayCommand(directive, proto::AudioPlayerDescriptor(), false);
        ASSERT_TRUE(command->has_media_request() && command->media_request().has_play_audio());

        const auto& playAudio = command->media_request().play_audio();
        ASSERT_TRUE(playAudio.has_normalization());

        const auto& normalization = playAudio.normalization();
        ASSERT_TRUE(doublesEquals(normalization.true_peak(), truePeak));
        ASSERT_TRUE(doublesEquals(normalization.integrated_loudness(), loudness));
    }

    Y_UNIT_TEST(CheckInvalidNormalization) {

        auto json = makeDefaultPayload("stream-id");
        // make sure that invalid normalization values won't cause any crash
        json["payload"]["stream"]["normalization"]["true_peak"] = "not a double";
        json["payload"]["stream"]["normalization"]["integrated_loudness"] = Json::objectValue; // not a double
        auto directive = std::make_shared<YandexIO::Directive>(YandexIO::Directive::Data::fromJson(json));

        auto command = MediaRequestFactory::createPlayCommand(directive, proto::AudioPlayerDescriptor(), false);
        ASSERT_TRUE(command->has_media_request() && command->media_request().has_play_audio());

        const auto& playAudio = command->media_request().play_audio();
        ASSERT_FALSE(playAudio.has_normalization());
    }

} // suite
