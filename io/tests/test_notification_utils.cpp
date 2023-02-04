#include <yandex_io/services/aliced/capabilities/external_command_capability/notifications/notification_utils.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <json/json.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace quasar::proto;

IOutputStream& operator<<(IOutputStream& out, const NotifyDirective_RingType& ringType) {
    out << NotifyDirective_RingType_Name(ringType);
    return out;
}

Y_UNIT_TEST_SUITE_F(NotificationUtilsTest, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testEmptyPayload) {
        Json::Value payload;
        auto result = NotificationUtils::fromJson(payload);
        UNIT_ASSERT(result.notifications_size() == 0);
        UNIT_ASSERT(!result.has_ring());

        std::string payloadString = "{}";
        result = NotificationUtils::fromJsonString(payloadString);
        UNIT_ASSERT(result.notifications_size() == 0);
        UNIT_ASSERT(!result.has_ring());

        payloadString = "";
        result = NotificationUtils::fromJsonString(payloadString);
        UNIT_ASSERT(result.notifications_size() == 0);
        UNIT_ASSERT(!result.has_ring());

        payloadString = "non json";
        result = NotificationUtils::fromJsonString(payloadString);
        UNIT_ASSERT(result.notifications_size() == 0);
        UNIT_ASSERT(!result.has_ring());
    }

    Y_UNIT_TEST(testNoNotifications) {
        std::string payload = "{ "
                              "\"version_id\": \"1\","
                              "\"ring\": \"NoSound\""
                              "}";
        auto result = NotificationUtils::fromJsonString(payload);
        UNIT_ASSERT(result.has_ring());
        UNIT_ASSERT(result.notifications_size() == 0);
        UNIT_ASSERT(result.has_versionid());
        UNIT_ASSERT_VALUES_EQUAL(proto::NotifyDirective_RingType_NO_SOUND, result.ring());
        UNIT_ASSERT_VALUES_EQUAL("1", result.versionid());
    }

    Y_UNIT_TEST(testEmptyNotifications) {
        std::string payload = "{ "
                              "\"notifications\":[],"
                              "\"version_id\": \"1\","
                              "\"ring\": \"NoSound\""
                              "}";
        auto result = NotificationUtils::fromJsonString(payload);
        UNIT_ASSERT(result.has_ring());
        UNIT_ASSERT(result.notifications_size() == 0);
        UNIT_ASSERT(result.has_versionid());
        UNIT_ASSERT_VALUES_EQUAL(proto::NotifyDirective_RingType_NO_SOUND, result.ring());
        UNIT_ASSERT_VALUES_EQUAL("1", result.versionid());
    }

    Y_UNIT_TEST(testNoSoundNotification) {
        std::string payload = "{ "
                              "\"version_id\": \"1\","
                              "\"ring\": \"NoSound\","
                              "\"notifications\":["
                              "{"
                              "\"id\": \"uid1\","
                              "\"text\": \"text1\""
                              "},"
                              "{"
                              "\"id\": \"uid2\","
                              "\"text\": \"text2\""
                              "}"
                              "]"
                              "}";

        auto result = NotificationUtils::fromJsonString(payload);
        UNIT_ASSERT(result.has_ring());
        UNIT_ASSERT(result.notifications_size() == 2);
        UNIT_ASSERT(result.has_versionid());
        UNIT_ASSERT_VALUES_EQUAL(proto::NotifyDirective_RingType_NO_SOUND, result.ring());
        UNIT_ASSERT_VALUES_EQUAL("1", result.versionid());
        UNIT_ASSERT_VALUES_EQUAL("uid1", result.notifications(0).id());
        UNIT_ASSERT_VALUES_EQUAL("text1", result.notifications(0).text());
        UNIT_ASSERT_VALUES_EQUAL("uid2", result.notifications(1).id());
        UNIT_ASSERT_VALUES_EQUAL("text2", result.notifications(1).text());
    }

    Y_UNIT_TEST(testDelicateNotification) {
        std::string payload = "{ "
                              "\"version_id\": \"1\","
                              "\"ring\": \"Delicate\","
                              "\"notifications\":["
                              "{"
                              "\"id\": \"uid1\","
                              "\"text\": \"text1\""
                              "},"
                              "{"
                              "\"id\": \"uid2\","
                              "\"text\": \"text2\""
                              "}"
                              "]"
                              "}";

        auto result = NotificationUtils::fromJsonString(payload);
        UNIT_ASSERT(result.has_ring());
        UNIT_ASSERT(result.notifications_size() == 2);
        UNIT_ASSERT(result.has_versionid());
        UNIT_ASSERT_VALUES_EQUAL(proto::NotifyDirective_RingType_DELICATE, result.ring());
        UNIT_ASSERT_VALUES_EQUAL("1", result.versionid());
        UNIT_ASSERT_VALUES_EQUAL("uid1", result.notifications(0).id());
        UNIT_ASSERT_VALUES_EQUAL("text1", result.notifications(0).text());
        UNIT_ASSERT_VALUES_EQUAL("uid2", result.notifications(1).id());
        UNIT_ASSERT_VALUES_EQUAL("text2", result.notifications(1).text());
    }

    Y_UNIT_TEST(testProactiveNotification) {
        std::string payload = "{ "
                              "\"version_id\": \"1\","
                              "\"ring\": \"Proactive\","
                              "\"notifications\":["
                              "{"
                              "\"id\": \"uid1\","
                              "\"text\": \"text1\""
                              "},"
                              "{"
                              "\"id\": \"uid2\","
                              "\"text\": \"text2\""
                              "}"
                              "]"
                              "}";

        auto result = NotificationUtils::fromJsonString(payload);
        UNIT_ASSERT(result.has_ring());
        UNIT_ASSERT(result.notifications_size() == 2);
        UNIT_ASSERT(result.has_versionid());
        UNIT_ASSERT_VALUES_EQUAL(proto::NotifyDirective_RingType_PROACTIVE, result.ring());
        UNIT_ASSERT_VALUES_EQUAL("1", result.versionid());
        UNIT_ASSERT_VALUES_EQUAL("uid1", result.notifications(0).id());
        UNIT_ASSERT_VALUES_EQUAL("text1", result.notifications(0).text());
        UNIT_ASSERT_VALUES_EQUAL("uid2", result.notifications(1).id());
        UNIT_ASSERT_VALUES_EQUAL("text2", result.notifications(1).text());
    }

} /* test suite */
