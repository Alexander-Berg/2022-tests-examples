#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/automotive/remote_access/server/lib/autostart/data_types/db_schedule.h>

Y_UNIT_TEST_SUITE(test_database_schedule_operations) {

using namespace maps::automotive;
using namespace maps::automotive::autostart;

Y_UNIT_TEST(test_create_from_json)
{
    const CarInformation::Uuid scheduleTelematicsHashId = "12345678";
    const auto scheduleJson = maps::json::Value::fromString(
        R"(
{
    "isEnabled": true,
    "days": ["mon", "tue", "wed", "thu", "fri"],
    "time": {
        "hours": 8,
        "minutes": 30
    }
}
        )"
        );
    const DatabaseSchedule createdSchedule(scheduleTelematicsHashId, scheduleJson);
    ASSERT_EQ(createdSchedule.getTelematicsHashId(), scheduleTelematicsHashId);
    ASSERT_EQ(createdSchedule.getDays(), std::bitset<7>(0b0011111));
    ASSERT_EQ(createdSchedule.getIsEnabled(), true);
    ASSERT_EQ(createdSchedule.getTimeHours(), std::chrono::hours(8));
    ASSERT_EQ(createdSchedule.getTimeMinutes(), std::chrono::minutes(30));
    ASSERT_FALSE(createdSchedule.getId());
}

Y_UNIT_TEST(test_json_deserialize_serialize)
{
    const DatabaseSchedule::IdType scheduleId = 1234;
    const CarInformation::Uuid scheduleTelematicsHashId = "12345678";

    auto getScheduleJson = [](bool withId) {
        const std::string jsonEnd =         R"(
    "isEnabled": true,
    "days": ["mon", "tue", "wed", "thu", "fri"],
    "time": {
        "hours": 8,
        "minutes": 30
    }
}
        )";
        return maps::json::Value::fromString(
            "{\n" +
            (withId ? "\"id\": " + std::to_string(scheduleId) + ",\n" : "") +
            jsonEnd
            );
    };

    const auto inputJson = getScheduleJson(false);
    DatabaseSchedule createdSchedule(scheduleTelematicsHashId, inputJson);
    createdSchedule.setId(scheduleId);
    const auto expectedOutputJson = getScheduleJson(true);
    const auto outputJson = maps::json::Value::fromString(createdSchedule.toJsonString());
    ASSERT_EQ(outputJson, expectedOutputJson);
}

Y_UNIT_TEST(test_patch_full_json)
{
    const DatabaseSchedule::IdType scheduleId = 1234;
    const CarInformation::Uuid scheduleTelematicsHashId = "12345678";
    const std::bitset<7> days(0b0011111);
    const bool isEnabled = true;
    const std::chrono::hours timeHours(8);
    const std::chrono::minutes timeMinutes(30);

    DatabaseSchedule scheduleForPatch(
        scheduleId,
        scheduleTelematicsHashId,
        days,
        isEnabled,
        timeHours,
        timeMinutes
        );

    const auto patchJson = maps::json::Value::fromString(R"(
{
    "isEnabled": false,
    "days": ["mon", "wed", "thu"],
    "time": {
        "hours": 9,
        "minutes": 40
    }
}
        )");
    scheduleForPatch.patch(patchJson);

    ASSERT_EQ(scheduleForPatch.getIsEnabled(), false);
    ASSERT_EQ(scheduleForPatch.getDays(), std::bitset<7>(0b0001101));
    ASSERT_EQ(scheduleForPatch.getTimeHours(), std::chrono::hours(9));
    ASSERT_EQ(scheduleForPatch.getTimeMinutes(), std::chrono::minutes(40));
}

Y_UNIT_TEST(test_patch_partial_json)
{
    const DatabaseSchedule::IdType scheduleId = 1234;
    const CarInformation::Uuid scheduleTelematicsHashId = "12345678";
    const std::bitset<7> days(0b0011111);
    const bool isEnabled = true;
    const std::chrono::hours timeHours(8);
    const std::chrono::minutes timeMinutes(30);

    DatabaseSchedule scheduleForPatch(
        scheduleId,
        scheduleTelematicsHashId,
        days,
        isEnabled,
        timeHours,
        timeMinutes
    );

    const auto patchJson = maps::json::Value::fromString(R"(
{
    "time": {
        "hours": 9,
        "minutes": 40
    }
}
        )");
    scheduleForPatch.patch(patchJson);

    ASSERT_EQ(scheduleForPatch.getIsEnabled(), isEnabled);
    ASSERT_EQ(scheduleForPatch.getDays(), days);
    ASSERT_EQ(scheduleForPatch.getTimeHours(), std::chrono::hours(9));
    ASSERT_EQ(scheduleForPatch.getTimeMinutes(), std::chrono::minutes(40));
}

}