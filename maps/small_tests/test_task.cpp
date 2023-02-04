#include <maps/wikimap/feedback/api/src/libs/common/feedback_task.h>
#include <maps/wikimap/feedback/api/src/libs/dbqueries/constants.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/printers.h>
#include <maps/libs/common/include/file_utils.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::tests {

Y_UNIT_TEST_SUITE(test_task)
{

// TODO: validate result with json schema from
// https://a.yandex-team.ru/review/1321206/files#file-0-38935411

Y_UNIT_TEST(task_to_json)
{
    const std::string jsonStr = maps::common::readFileToString(SRC_("data/task1.json"));
    const auto jsonValue = maps::json::Value::fromString(jsonStr);
    const FeedbackTask expected{
        TaskId{"0c645c17-8422-4c29-9a4e-8309cf7b767c"},
        Service::Support,
        ServiceObjectId{"109272369"},
        "https://api.samsara.yandex-team.ru/api/v2/tickets/109272369",
        TaskStatus::InProgress,
        OriginalTask{jsonValue["original_task"]},
        Integration{
            {
                {
                    Service::Support,
                    ServiceDesc{
                        {ServiceObjectId{"109272369"}},
                        {"https://api.samsara.yandex-team.ru/api/v2/tickets/109272369"},
                        ServiceDesc::NO_RESOLUTION
                    }
                }
            },
            {ServiceObjectId{"ticket1"}, ServiceObjectId{"ticket2"}},
            {ServiceObjectId{"NMAPS-14057"}}
        },
        maps::chrono::parseIsoDateTime("2020-07-07T12:38:01.000Z"),
        maps::chrono::parseIsoDateTime("2020-07-07T12:38:01.000Z")
    };
    const std::string jsonRes = (maps::json::Builder() << expected).str();
    UNIT_ASSERT_VALUES_EQUAL(
        jsonValue,
        maps::json::Value::fromString(jsonRes));
    UNIT_ASSERT_VALUES_EQUAL(
        FeedbackTask::fromJson(jsonValue),
        expected);
}

} // test_task suite

} // namespace maps::wiki::feedback::api::tests
