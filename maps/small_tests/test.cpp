#include <maps/wikimap/feedback/api/src/synctool/lib/sync.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::sync::tests {

namespace {

FeedbackTask makeTestTask(TaskStatus status, NmapsResolution resolution) {
    return FeedbackTask{
        TaskId{"fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885"},
        Service::Support,
        ServiceObjectId("serviceObjectId"),
        "http://serviceObjectUrl",
        status,
        api::tests::EMPTY_ORIGINAL_TASK,
        Integration(
            std::map<Service, ServiceDesc>{{
                Service::Nmaps,
                ServiceDesc{
                    ServiceObjectId{"serviceObjectId"},
                    std::nullopt, // serviceObjectUrl,
                    std::string{toString(resolution)}
                }
            }},
            {}, // samsaraTicketsHistory
            {} // trackerTasksHistory
        ),
        maps::chrono::parseIsoDateTime("2020-04-01 01:00:00+00:00"),
        maps::chrono::parseIsoDateTime("2020-04-02 01:00:00+00:00")
    };
}

} // namespace

Y_UNIT_TEST_SUITE(test_ugc)
{

Y_UNIT_TEST(ugc_status)
{
    const TaskId TASK_ID("fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885");

    FeedbackTask NO_INTEGRATION_TASK{
        TASK_ID,
        Service::Support,
        ServiceObjectId("serviceObjectId"),
        "http://serviceObjectUrl",
        TaskStatus::NeedInfo,
        api::tests::EMPTY_ORIGINAL_TASK,
        {/*integration*/},
        maps::chrono::parseIsoDateTime("2020-04-01 01:00:00+00:00"),
        maps::chrono::parseIsoDateTime("2020-04-02 01:00:00+00:00")
    };
    UNIT_ASSERT_VALUES_EQUAL(
        internal::getStatusForUgcSync(NO_INTEGRATION_TASK),
        TaskStatus::NeedInfo
    );

    UNIT_ASSERT_VALUES_EQUAL(
        internal::getStatusForUgcSync(makeTestTask(
            TaskStatus::NeedInfo,
            NmapsResolution::RedirectToSupport
        )),
        TaskStatus::NeedInfo
    );
    UNIT_ASSERT_VALUES_EQUAL(
        internal::getStatusForUgcSync(makeTestTask(
            TaskStatus::Rejected,
            NmapsResolution::RedirectToSupport
        )),
        TaskStatus::InProgress
    );
    UNIT_ASSERT_VALUES_EQUAL(
        internal::getStatusForUgcSync(makeTestTask(
            TaskStatus::Rejected,
            NmapsResolution::RedirectToContentAuto
        )),
        TaskStatus::InProgress
    );
    UNIT_ASSERT_VALUES_EQUAL(
        internal::getStatusForUgcSync(makeTestTask(
            TaskStatus::Rejected,
            NmapsResolution::NoInfo
        )),
        TaskStatus::Rejected
    );
}

} // test_ugc suite

} // namespace maps::wiki::feedback::api::sync::tests
