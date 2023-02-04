#include <maps/wikimap/feedback/api/src/notifications/lib/sender_client.h>
#include <maps/wikimap/feedback/api/src/notifications/tests/helpers/printers.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::notifications::tests {

Y_UNIT_TEST_SUITE(test_sender)
{

Y_UNIT_TEST(sender_request)
{
    Notification notification {
        NotificationId{1},
        TaskId{"taskId"},
        std::nullopt,
        {"email@test.test"},
        MailNotificationType::ToponymProhibitedByRules,
        Lang::Ru,
        NotificationData{
            std::nullopt,
            std::nullopt,
            {geolib3::Point2(37.37, 55.55)},
            std::nullopt,
            "user_comment",
        },
        maps::chrono::parseIsoDateTime("2020-04-01 01:00:00+00:00")
    };

    std::string expected = "{"
        "\"async\":true,"
        "\"ignore_empty_email\":true,"
        "\"to_email\":\"email@test.test\","
        "\"args\":{"
            "\"task_id\":\"taskId\","
            "\"title1\":\"geocodedData\","
            "\"comment1\":\"user_comment\","
            "\"url1\":\"https://maps.yandex.ru/?whatshere[point]=37.37%2C55.55&whatshere[zoom]=17.0&ll=37.37%2C55.55&z=17\""
        "}"
    "}";
    UNIT_ASSERT_VALUES_EQUAL(
        internal::makeSenderRequestBody(notification, "geocodedData"),
        expected);
}

} // test_sender suite

} // namespace maps::wiki::feedback::api::notifications::tests
