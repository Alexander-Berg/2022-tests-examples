#include <maps/wikimap/feedback/api/src/yacare/lib/sync_utils.h>

#include <maps/wikimap/feedback/api/src/libs/common/config.h>
#include <maps/wikimap/feedback/api/src/libs/common/original_task.h>

#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::tests {

Y_UNIT_TEST_SUITE(test_sync_utils)
{

namespace {

const FeedbackTask feedbackTask()
{
    const auto now = maps::chrono::TimePoint::clock::now();
    return FeedbackTask{
        .id = TaskId{"1"},
        .service = Service::Sprav,
        .status = TaskStatus::InProgress,
        .originalTask = OriginalTask{
            maps::json::Value::fromFile(SRC_("data/original_task1.json"))},
        .createdAt = now,
        .updatedAt = now
    };
}

} // namespace

Y_UNIT_TEST(test_sync_feedback_task_with_nmaps)
{
    FeedbackTask task = feedbackTask();
    task.service = Service::Nmaps;
    setServiceObjectFromOriginalTask(task);
    task.updateIntegration();

    Integration expectedIntegration;
    expectedIntegration.addServiceOrThrow(
        Service::Nmaps,
        ServiceDesc{
            ServiceObjectId{"56359637"},
            "https://n.maps.yandex.ru/#!/objects/56359637",
            ServiceDesc::NO_RESOLUTION});
    UNIT_ASSERT_VALUES_EQUAL(task.serviceObjectId.value(), "56359637");
    UNIT_ASSERT_VALUES_EQUAL(
        task.serviceObjectUrl,
        "https://n.maps.yandex.ru/#!/objects/56359637");
    UNIT_ASSERT_VALUES_EQUAL(task.integration, expectedIntegration);
}

Y_UNIT_TEST(test_sync_feedback_task_with_support)
{
    FeedbackTask task = feedbackTask();
    task.service = Service::Support;

    UNIT_ASSERT_EXCEPTION(
        transferFeedbackTaskToSamsaraAsync(task),
        yacare::errors::BadRequest);
}

} // test_sync_utils suite

} // namespace maps::wiki::feedback::api::tests
