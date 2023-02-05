#include <maps/wikimap/mapspro/libs/sender/include/email_template_params.h>
#include <maps/wikimap/mapspro/libs/sender/include/gateway.h>

#include <maps/libs/http/include/test_utils.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(sender_suite) {

Y_UNIT_TEST(test_email_template_params)
{
    sender::EmailTemplateParams params;
    params.addParam("name1", "value");
    params.addParam("name2", 23);
    params.addUrlParam("url1", "http://ya.ru");
    params.addUrlParam("url2", "ya.ru");
    auto data = params.data();
    UNIT_ASSERT_EQUAL(data.at("name1"), "value");
    UNIT_ASSERT_EQUAL(data.at("name2"), "23");
    UNIT_ASSERT_EQUAL(data.at("url1"), "ya.ru");
    UNIT_ASSERT_EQUAL(data.at("url2"), "ya.ru");
    UNIT_ASSERT_EQUAL(
        params.toJsonString(),
        R"({"args":{"name1":"value","name2":"23","url1":"ya.ru","url2":"ya.ru"}})"
    );
}

Y_UNIT_TEST(test_sender)
{
    sender::EmailTemplateParams params;
    params.addParam("user", "Petya");
    sender::Gateway sender{
        "sender.endpoint.ru",
        sender::Credentials{"testUserId", "testAccountSlug"},
        maps::common::RetryPolicy{}
    };
    auto mockHandle = maps::http::addMock(
        "https://sender.endpoint.ru/api/0/testAccountSlug/"
            "transactional/some-slug-id/send?to_email=petya%40yandex.ru",
        [](const maps::http::MockRequest& request) {
            UNIT_ASSERT_EQUAL(request.body, R"({"args":{"user":"Petya"}})");
            return request.body;
        }
    );

    auto result = sender.sendToEmail("some-slug-id", "petya@yandex.ru", params);
    UNIT_ASSERT_EQUAL(result, sender::Result::Sent);
}

} // sender_suite

} //namespace maps::wiki::tests
