#include <yandex_io/libs/protobuf_utils/json.h>

#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>

#include <yandex_io/protos/model_objects.pb.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/sdk/proto/device_sdk.pb.h>

#include <library/cpp/testing/unittest/registar.h>

#include <string>

using namespace quasar;

Y_UNIT_TEST_SUITE(ProtobufJsonTest) {

    Y_UNIT_TEST(TestEmptyQuasarMessageString) {
        proto::QuasarMessage message;
        UNIT_ASSERT_VALUES_EQUAL("{}", convertMessageToJsonString(message));
    }

    Y_UNIT_TEST(TestSimpleQuasarMessageString) {
        proto::QuasarMessage message;
        message.mutable_alice_state()->set_state(quasar::proto::AliceState_State_SPEAKING);
        // will be "aliceState" by default. Proto field extension [json_name = "..."] allows to modify it
        UNIT_ASSERT_VALUES_EQUAL("{\"aliceState\":{\"state\":\"SPEAKING\"}}", convertMessageToJsonString(message));
    }

    Y_UNIT_TEST(TestSimpleQuasarMessageJson) {
        proto::QuasarMessage message;
        message.mutable_alice_state()->set_state(quasar::proto::AliceState_State_SPEAKING);
        const auto jsonOpt = convertMessageToJson(message);
        UNIT_ASSERT(jsonOpt.has_value());

        Json::Value expected;
        expected["aliceState"]["state"] = "SPEAKING";

        YIO_LOG_INFO("Expected: " << jsonToString(expected));
        YIO_LOG_INFO("Actual: " << jsonToString(jsonOpt.value()));
        UNIT_ASSERT_EQUAL(expected, jsonOpt.value());
    }

    Y_UNIT_TEST(TestSearchResultStateJson) {
        proto::AppState appState;
        appState.mutable_video()->set_currentscreen(yandex_io::proto::TVideo::search_results);

        yandex_io::proto::TVideo::TTvInterfaceState interface;
        auto gallery = interface.mutable_searchresultsscreen() -> add_galleries();
        auto item = gallery->add_items();
        item->set_visible(true);
        item->mutable_videoitem()->set_name("qwerty");
        appState.mutable_video()->mutable_tvinterfacestate()->CopyFrom(interface);

        const auto jsonOpt = convertMessageToJson(appState);
        UNIT_ASSERT(jsonOpt.has_value());

        Json::Value expected;
        expected["video"]["current_screen"] = "search_results";
        expected["video"]["tv_interface_state"]["search_results_screen"]["galleries"][0]["items"][0]["visible"] = true;
        expected["video"]["tv_interface_state"]["search_results_screen"]["galleries"][0]["items"][0]["video_item"]["name"] = "qwerty";

        YIO_LOG_INFO("Expected: " << jsonToString(expected));
        YIO_LOG_INFO("Actual: " << jsonToString(jsonOpt.value()));
        UNIT_ASSERT_EQUAL(expected, jsonOpt.value());
    }

    Y_UNIT_TEST(TestVideoPlayingStateJson) {
        proto::AppState appState;
        appState.mutable_video()->set_currentscreen(yandex_io::proto::TVideo::video_player);
        appState.mutable_video()->mutable_player()->set_pause(false);
        appState.mutable_video()->set_playername("com.yandex.tv.videoplayer");

        auto playing = appState.mutable_video() -> mutable_currentlyplaying();
        playing->mutable_rawitem()->set_episode(1);
        playing->mutable_rawitem()->set_name("Rick and Morty");
        playing->mutable_rawitem()->set_provideritemid("4b7e75f953b028e5b856bbaf23d5459f");

        playing->mutable_progress()->set_duration(100);
        playing->mutable_progress()->set_played(50);

        const auto jsonOpt = convertMessageToJson(appState);
        UNIT_ASSERT(jsonOpt.has_value());

        Json::Value expected;
        expected["video"]["current_screen"] = "video_player";
        expected["video"]["player"]["pause"] = false;
        expected["video"]["player_name"] = "com.yandex.tv.videoplayer";
        expected["video"]["currently_playing"]["item"]["episode"] = 1;
        expected["video"]["currently_playing"]["item"]["name"] = "Rick and Morty";
        expected["video"]["currently_playing"]["item"]["provider_item_id"] = "4b7e75f953b028e5b856bbaf23d5459f";
        expected["video"]["currently_playing"]["progress"]["duration"] = 100;
        expected["video"]["currently_playing"]["progress"]["played"] = 50;

        YIO_LOG_INFO("Expected: " << jsonToString(expected));
        YIO_LOG_INFO("Actual: " << jsonToString(jsonOpt.value()));
        UNIT_ASSERT_EQUAL(expected, jsonOpt.value());
    }
}
