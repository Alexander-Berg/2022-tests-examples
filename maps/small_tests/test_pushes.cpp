#include <maps/wikimap/feedback/api/src/synctool/lib/pushes.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::sync::tests {

namespace sq = sync_queue;

Y_UNIT_TEST_SUITE(test_pushes)
{

Y_UNIT_TEST(make_push_uri)
{
    const auto jsonValue = json::Value::fromString(R"--({
        "push_type": "toponym_published",
        "locale": "ru_RU",
        "client_id": "ru.yandex.yandexmaps",
        "receiver_uid": 220317986,
        "contribution_id": "fb:1a1da2cf-8c80-c2cf-1ac4-82e547a014c0"
    })--");
    sq::SupSendPushParams params(jsonValue);

    {
        auto uri = internal::makePushUri(
            params,
            "https://l7test.yandex.by/maps/profile/ugc/feedback",
            SupProject::Mobmaps);
        UNIT_ASSERT(uri);
        UNIT_ASSERT_VALUES_EQUAL(
            *uri,
            "yandexmaps://open_webview?url=https%3A%2F%2Fl7test.yandex.by%2Fmaps%2Fprofile%2Fugc%2Ffeedback"
                "%3Fclient_id%3Dru.yandex.yandexmaps%26uid%3D220317986%26webview%3Dtrue"
                "%26contribution_id%3Dfb%253A1a1da2cf-8c80-c2cf-1ac4-82e547a014c0&add_cross=false");
    }

    params.clientId = "ru.yandex.yandexnavi";
    {
        auto uri = internal::makePushUri(
            params,
            "https://l7test.yandex.by/maps/profile/ugc/feedback",
            SupProject::Navi);
        UNIT_ASSERT(uri);
        UNIT_ASSERT_VALUES_EQUAL(
            *uri,
            "yandexnavi://show_web_view?link=https%3A%2F%2Fl7test.yandex.by%2Fmaps%2Fprofile%2Fugc%2Ffeedback"
                "%3Fclient_id%3Dru.yandex.yandexnavi%26uid%3D220317986%26webview%3Dtrue"
                "%26contribution_id%3Dfb%253A1a1da2cf-8c80-c2cf-1ac4-82e547a014c0%26authenticate%3Duse_auth");
    }
}

} // test_pushes suite

} // namespace maps::wiki::feedback::api::sync::tests
