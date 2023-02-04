#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/wikimap/feedback/api/src/yacare/lib/tracker.h>

namespace maps::wiki::feedback::api::tests {

Y_UNIT_TEST_SUITE(test_tracker)
{

Y_UNIT_TEST(issue_summary)
{
    const std::string jsonStr = maps::common::readFileToString(SRC_("data/task_for_tracker_1.json"));
    const auto jsonValue = maps::json::Value::fromString(jsonStr);
    auto task = FeedbackTask::fromJson(jsonValue);
    auto summary = internal::issueSummary(task);
    UNIT_ASSERT_VALUES_EQUAL(summary, "other.comment:20257409");
}

Y_UNIT_TEST(issue_body)
{
    const std::string jsonStr = maps::common::readFileToString(SRC_("data/task_for_tracker_1.json"));
    const auto jsonValue = maps::json::Value::fromString(jsonStr);
    auto task = FeedbackTask::fromJson(jsonValue);
    internal::IssueBodyConfig config {
        .redirectMessage = "redirect message",
        .nmaps = "https://n.maps.yandex.ru",
        .feedbackAdmin = "https://feedback-admin.c.maps.yandex-team.ru",
        .webMaps = "https://yandex.ru/maps",
        .avatars = "https://avatars.mds.yandex.net",
        .needFbapiAdminBlock = true
    };
    auto body = internal::issueBody(config, task);
    INFO() << "BODY: \n" << body;
    auto expected = maps::common::readFileToString(SRC_("data/tracker_issue_body_expected_1.md"));
    UNIT_ASSERT_VALUES_EQUAL(body, expected);
}

} // test_tracker suite

} // namespace maps::wiki::feedback::api::tests
