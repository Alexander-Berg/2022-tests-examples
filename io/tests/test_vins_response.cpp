#include "mocks/mock_i_player.h"

#include <yandex_io/libs/base/directives.h>
#include <yandex_io/services/aliced/capabilities/alice_capability/vins/vins_response.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <speechkit/core/include/speechkit/UniProxy.h>

using namespace YandexIO;
using namespace testing;

Y_UNIT_TEST_SUITE(VinsResponseTest) {
    Y_UNIT_TEST(testAliceResponseAdded_WhenTtsPlayerIsNull) {
        Json::Value payload;
        payload["header"]["request_id"] = "";
        payload["response"]["directives"] = Json::arrayValue;
        payload["voice_response"] = Json::objectValue;

        auto vinsResponse = VinsResponse::parse(nullptr, SpeechKit::UniProxy::Header{}, payload, nullptr, {});
        const auto& directives = vinsResponse.directives;

        ASSERT_TRUE(directives.size() == 1);
        ASSERT_TRUE((*directives.begin())->is(quasar::Directives::ALICE_RESPONSE));
    }

    Y_UNIT_TEST(testAliceResponseAdded_WhenTtsPlayerIsNotNull) {
        Json::Value payload;
        payload["header"]["request_id"] = "";
        payload["response"]["directives"] = Json::arrayValue;
        payload["voice_response"] = Json::objectValue;
        auto player = std::make_shared<MockIPlayer>();

        auto vinsResponse = VinsResponse::parse(nullptr, SpeechKit::UniProxy::Header{}, payload, player, {});
        const auto& directives = vinsResponse.directives;

        ASSERT_TRUE(directives.size() == 2);
        auto item = directives.begin();
        ASSERT_TRUE((*item)->is(quasar::Directives::ALICE_RESPONSE));
        item = std::next(item);
        ASSERT_TRUE((*item)->is(quasar::Directives::TTS_PLAY_PLACEHOLDER));
    }
}
