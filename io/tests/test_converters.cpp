#include <library/cpp/testing/unittest/registar.h>

#include <yandex_io/libs/protobuf_utils/json.h>
#include <yandex_io/capabilities/device_state/converters/converters.h>

Y_UNIT_TEST_SUITE(ConvertersTest) {

    Y_UNIT_TEST(testVideoStateConverter) {
        yandex_io::proto::TVideo sdkVideo;
        sdkVideo.SetLastPlayTimestamp(1653902166567);
        sdkVideo.SetCurrentScreen(yandex_io::proto::TVideo::video_player);
        sdkVideo.MutablePlayer()->SetPause(false);
        sdkVideo.MutablePlayerCapabilities()->Add(yandex_io::proto::TVideo::pause);
        sdkVideo.SetPlayerName("com.yandex.tv.videoplayer");

        std::string currentlyPlaying = "{\"progress\":{\"played\":516.301, \"duration\":1242.999},\"item\":{\"type\":\"tv_show_episode\",\"provider_name\":\"kinopoisk\",\"provider_item_id\":\"43a9f42f96cbab51b7c0a5a90f017eb8\",\"tv_show_season_id\":\"4dfd00c593259aadad6b24c28fb3eab4\",\"tv_show_item_id\":\"41a1e1771efdc75bb3bc66e1988bbe93\",\"episode\":19,\"season\":9,\"name\":\"19. Везувий\",\"genre\":\"комедия, мелодрама\",\"release_year\":2005,\"provider_info\":[{\"type\":\"tv_show_episode\",\"provider_name\":\"kinopoisk\",\"provider_item_id\":\"43a9f42f96cbab51b7c0a5a90f017eb8\",\"tv_show_season_id\":\"4dfd00c593259aadad6b24c28fb3eab4\",\"tv_show_item_id\":\"41a1e1771efdc75bb3bc66e1988bbe93\",\"episode\":19,\"season\":9}],\"skippable_fragments\":[{\"start_time\":115,\"end_time\":127,\"type\":\"intro\"},{\"start_time\":1215,\"end_time\":1243,\"type\":\"credits\"}]},\"tv_show_item\":{\"type\":\"tv_show\",\"provider_name\":\"kinopoisk\",\"provider_item_id\":\"41a1e1771efdc75bb3bc66e1988bbe93\",\"name\":\"Как я встретил вашу маму\",\"provider_info\":[    {\"type\":\"tv_show\",\"provider_name\":\"kinopoisk\",\"provider_item_id\":\"41a1e1771efdc75bb3bc66e1988bbe93\"}]}}";
        auto protoCurrentlyPlaying = quasar::convertJsonToProtobuf<yandex_io::proto::TVideo::TCurrentlyPlaying>(currentlyPlaying).value();
        sdkVideo.MutableCurrentlyPlaying()->Swap(&protoCurrentlyPlaying);

        const auto sdkProtoJson = quasar::convertMessageToJson(sdkVideo).value();
        const auto mmVideo = YandexIO::convertVideoState(sdkVideo);
        const auto mmProtoJson = quasar::convertMessageToJson(mmVideo).value();

        UNIT_ASSERT_EQUAL(sdkProtoJson, mmProtoJson);
    }

    Y_UNIT_TEST(testVideoStateReverdedConverted) {
        NAlice::TDeviceState::TVideo videoState;
        videoState.SetLastPlayTimestamp(1653902166567);
        videoState.SetCurrentScreen("video_player");
        videoState.MutablePlayer()->SetPause(false);
        videoState.MutablePlayerCapabilities()->Add(yandex_io::proto::TVideo::pause);
        videoState.SetPlayerName("com.yandex.tv.videoplayer");

        std::string currentlyPlaying = "{\"progress\":{\"played\":516.301, \"duration\":1242.999},\"item\":{\"type\":\"tv_show_episode\",\"provider_name\":\"kinopoisk\",\"provider_item_id\":\"43a9f42f96cbab51b7c0a5a90f017eb8\",\"tv_show_season_id\":\"4dfd00c593259aadad6b24c28fb3eab4\",\"tv_show_item_id\":\"41a1e1771efdc75bb3bc66e1988bbe93\",\"episode\":19,\"season\":9,\"name\":\"19. Везувий\",\"genre\":\"комедия, мелодрама\",\"release_year\":2005,\"provider_info\":[{\"type\":\"tv_show_episode\",\"provider_name\":\"kinopoisk\",\"provider_item_id\":\"43a9f42f96cbab51b7c0a5a90f017eb8\",\"tv_show_season_id\":\"4dfd00c593259aadad6b24c28fb3eab4\",\"tv_show_item_id\":\"41a1e1771efdc75bb3bc66e1988bbe93\",\"episode\":19,\"season\":9}],\"skippable_fragments\":[{\"start_time\":115,\"end_time\":127,\"type\":\"intro\"},{\"start_time\":1215,\"end_time\":1243,\"type\":\"credits\"}]},\"tv_show_item\":{\"type\":\"tv_show\",\"provider_name\":\"kinopoisk\",\"provider_item_id\":\"41a1e1771efdc75bb3bc66e1988bbe93\",\"name\":\"Как я встретил вашу маму\",\"provider_info\":[    {\"type\":\"tv_show\",\"provider_name\":\"kinopoisk\",\"provider_item_id\":\"41a1e1771efdc75bb3bc66e1988bbe93\"}]}}";
        auto protoCurrentlyPlaying = quasar::convertJsonToProtobuf<NAlice::TDeviceState::TVideo::TCurrentlyPlaying>(currentlyPlaying).value();
        videoState.MutableCurrentlyPlaying()->Swap(&protoCurrentlyPlaying);

        const auto sdkProtoJson = quasar::convertMessageToJson(videoState).value();
        const auto sdkVideo = YandexIO::convertVideoState(videoState);
        const auto mmProtoJson = quasar::convertMessageToJson(sdkVideo).value();

        UNIT_ASSERT_EQUAL(sdkProtoJson, mmProtoJson);
    }

};
