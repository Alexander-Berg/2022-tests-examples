#include <maps/wikimap/feedback/api/src/yacare/lib/add_task.h>
#include <maps/wikimap/feedback/api/src/yacare/lib/params.h>

#include <maps/wikimap/feedback/api/src/libs/common/types.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <maps/wikimap/feedback/api/src/libs/common/validator.h>

namespace maps::wiki::feedback::api::tests {

Y_UNIT_TEST_SUITE(test_params)
{

Y_UNIT_TEST(update_task_params_v1)
{
    const std::string requestBodyFull = R"({
        "service" : "nmaps",
        "status" : "accepted",
        "request_template_id": "unknown-id"
    })";
    UNIT_ASSERT_EXCEPTION_CONTAINS(UpdateTaskParams{requestBodyFull}, BadInputDataError, "deprecated");
}

Y_UNIT_TEST(update_task_params_v2)
{
    const std::string requestBody = R"({
        "task" : {
            "service" : "sprav",
            "status" : "rejected",
            "request_template_id": "need-full-address",
            "resolution": "no-data"
        },
        "service" : "nmaps",
        "message" : "some message"
    })";
    const auto updateParams = UpdateTaskParams{requestBody};
    UNIT_ASSERT_VALUES_EQUAL(*updateParams.newService, Service::Sprav);
    UNIT_ASSERT_VALUES_EQUAL(*updateParams.newStatus, TaskStatus::Rejected);
    UNIT_ASSERT_VALUES_EQUAL(*updateParams.initiatorService, Service::Nmaps);
    UNIT_ASSERT_VALUES_EQUAL(*updateParams.message, "some message");
    UNIT_ASSERT_VALUES_EQUAL(*updateParams.resolution, "no-data");
    UNIT_ASSERT_VALUES_EQUAL(*updateParams.requestTemplateId, RequestTemplateId::NeedFullAddress);
}

Y_UNIT_TEST(update_task_params_v2_bad_resolution)
{
    const std::string requestBody = R"({
        "task" : {
            "service" : "sprav",
            "status" : "rejected",
            "request_template_id": "need-full-address",
            "resolution": "unknown resolution"
        },
        "service" : "nmaps",
        "message" : "some message"
    })";
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        UpdateTaskParams{requestBody},
        BadInputDataError,
        "Unsupported resolution from nmaps"
    );
}

Y_UNIT_TEST(update_task_params_v2_need_info)
{
    const std::string requestBody = R"({
        "task" : {
            "service" : "nmaps",
            "status" : "need_info",
            "request_template_id": "need-full-address"
        },
        "service" : "nmaps",
        "message" : "some message"
    })";
    const auto updateParams = UpdateTaskParams{requestBody};
    UNIT_ASSERT_VALUES_EQUAL(*updateParams.newService, Service::Nmaps);
    UNIT_ASSERT_VALUES_EQUAL(*updateParams.newStatus, TaskStatus::NeedInfo);
    UNIT_ASSERT_VALUES_EQUAL(*updateParams.initiatorService, Service::Nmaps);
    UNIT_ASSERT_VALUES_EQUAL(*updateParams.message, "some message");
    UNIT_ASSERT(!updateParams.resolution.has_value());
    UNIT_ASSERT_VALUES_EQUAL(*updateParams.requestTemplateId, RequestTemplateId::NeedFullAddress);
}

Y_UNIT_TEST(update_task_params_v2_need_info_wrong_template)
{
    {
        const std::string requestBody = R"({
            "task" : {
                "service" : "nmaps",
                "status" : "need_info",
                "request_template_id": "wrong-template"
            },
            "service" : "nmaps",
            "message" : "some message"
        })";
        UNIT_ASSERT_EXCEPTION_CONTAINS(
            UpdateTaskParams{requestBody},
            BadInputDataError,
            "'Need info' request has unknown request_template_id");
    }
    // FIXME(@gradksov): valid 'other' need-info
    {
        const std::string requestBody = R"({
            "task" : {
                "service" : "nmaps",
                "status" : "need_info"
            },
            "service" : "nmaps",
            "message" : "some message"
        })";
        UNIT_ASSERT_NO_EXCEPTION(UpdateTaskParams{requestBody});
    }
}

Y_UNIT_TEST(fix_ry_suffix)
{
    UNIT_ASSERT_VALUES_EQUAL(
        internal::fixRySuffix("example@yandex.ry"),
        "example@yandex.ru");
    UNIT_ASSERT_VALUES_EQUAL(
        internal::fixRySuffix("example@yandex.ru"),
        "example@yandex.ru");
    UNIT_ASSERT_VALUES_EQUAL(
        internal::fixRySuffix("ry@yandex.ry"),
        "ry@yandex.ru");
}

Y_UNIT_TEST(fix_repeated_dots)
{
    UNIT_ASSERT_VALUES_EQUAL(
        internal::fixRepeatedDots("sample....email@yandex...by"),
        "sample.email@yandex.by");
}

Y_UNIT_TEST(fix_user_email_typos)
{
    UNIT_ASSERT_VALUES_EQUAL(
        internal::fixUserEmailTypos("ry..ry@yandex...ry"),
        "ry.ry@yandex.ru");
}

Y_UNIT_TEST(form_original_task)
{
    const auto json = maps::json::Value::fromString(R"({
        "form_id": "organization",
        "form_point": {
            "lon": 37.37,
            "lat": 55.55
        },
        "message": "test",
        "metadata": {
            "uid": 1,
            "uuid": "u-u-i-d-1",
            "locale": "ru_RU",
            "client_id": "nmaps",
            "version": "1.0"
        },
        "user_email": "sample..mail...with....typos@yandex.ry",
        "answer_id": "know",
        "object_uri": "ymapsbm1://org?oid=42",
        "question_id": "organization_entrance_problem"
    })");
    const OriginalTask originalTask = formOriginalTask(json, HttpData{"cookie", "", "fingerprint"});
    const OriginalTask expectedTask(maps::json::Value::fromString(R"({
        "form_id": "organization",
        "form_point": {
            "lon": 37.37,
            "lat": 55.55
        },
        "message": "test",
        "metadata": {
            "uid": 1,
            "uuid": "u-u-i-d-1",
            "locale": "ru_RU",
            "client_id": "nyak",
            "version": "1.0",
            "cookie": "cookie",
            "fingerprint": "fingerprint"
        },
        "user_email": "sample.mail.with.typos@yandex.ru",
        "answer_id": "moved",
        "object_uri": "ymapsbm1://org?oid=42",
        "object_url" : "https://yandex.ru/maps/org/42",
        "question_id": "entrance_problem"
    })"));

    UNIT_ASSERT_VALUES_EQUAL(originalTask, expectedTask);
}

Y_UNIT_TEST(form_original_task_errors)
{
    const auto json = maps::json::Value::fromString(R"({
        "form_id": "route",
        "form_point": {
            "lon": 37.37,
            "lat": 55.55
        },
        "message": "test",
        "metadata": {
            "uid": 1,
            "uuid": "u-u-i-d-1",
            "locale": "ru_RU",
            "client_id": "nmaps",
            "version": "1.0"
        },
        "user_email": "sample..mail...with....typos@yandex.ry",
        "answer_id": "report_route",
        "object_uri": "ymapsbm1://org?oid=42",
        "question_id": "wrong_route"
    })");
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        validateNewTask(formOriginalTask(json, HttpData{})),
        BadInputDataError,
        "must have 'question_context'");
    const auto jsonWithQuestionContext = maps::json::Value::fromString(R"({
        "form_id": "route",
        "form_point": {
            "lon": 37.37,
            "lat": 55.55
        },
        "message": "test",
        "metadata": {
            "uid": 1,
            "uuid": "u-u-i-d-1",
            "locale": "ru_RU",
            "client_id": "nmaps",
            "version": "1.0"
        },
        "user_email": "sample..mail...with....typos@yandex.ry",
        "answer_id": "report_route",
        "object_uri": "ymapsbm1://org?oid=42",
        "question_id": "wrong_route",
        "question_context": {}
    })");
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        validateNewTask(formOriginalTask(jsonWithQuestionContext, HttpData{})),
        BadInputDataError,
        "must have 'answer_context'");
}

Y_UNIT_TEST(timepoint_parsing)
{
    yacare::Parser<maps::chrono::TimePoint> parser;

    const std::string sqlDateTime = "2020-09-17 21:00:00+00:00";
    const maps::chrono::TimePoint sqlTimePoint = parser(sqlDateTime);
    UNIT_ASSERT_VALUES_EQUAL(
        maps::chrono::formatSqlDateTime(sqlTimePoint),
        sqlDateTime);

    const std::string isoDateTime = "2020-09-17T21:00:00.000Z";
    const maps::chrono::TimePoint isoTimePoint = parser(isoDateTime);
    UNIT_ASSERT_VALUES_EQUAL(
        maps::chrono::formatSqlDateTime(isoTimePoint),
        sqlDateTime);

    UNIT_ASSERT_EXCEPTION(parser("2020:09-17 21:00:00+00"), std::bad_cast);
    UNIT_ASSERT_EXCEPTION(parser("2020-09-17t21:00:00.000Z"), std::bad_cast);
}

} // test_params suite

} // namespace maps::wiki::feedback::api::tests
