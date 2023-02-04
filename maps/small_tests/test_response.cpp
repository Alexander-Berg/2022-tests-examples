#include <maps/wikimap/feedback/api/src/yacare/lib/response.h>

#include <maps/wikimap/feedback/api/src/libs/test_helpers/printers.h>
#include <maps/wikimap/feedback/api/src/libs/common/original_task.h>
#include <maps/wikimap/feedback/api/src/libs/common/types.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::tests {

namespace {

const FeedbackTask SAMPLE_TASK {
    TaskId{"0c645c17-8422-4c29-9a4e-8309cf7b767c"},
    Service::Support,
    ServiceObjectId{"109272369"},
    "https://api.samsara.yandex-team.ru/api/v2/tickets/109272369",
    TaskStatus::Rejected,
    OriginalTask{maps::json::Value::fromFile(SRC_("data/original_task1.json"))},
    Integration{{
        {
            Service::Support,
            ServiceDesc{
                {ServiceObjectId{"109272369"}},
                {"https://api.samsara.yandex-team.ru/api/v2/tickets/109272369"},
                ServiceDesc::NO_RESOLUTION
            }
        }
    }},
    maps::chrono::parseIsoDateTime("2020-07-07T12:38:01.000Z"),
    maps::chrono::parseIsoDateTime("2020-07-07T13:26:03.954Z")
};

const History SAMPLE_TASK_HISTORY{
    {
        TaskChangeId{1441563},
        TaskId{"0c645c17-8422-4c29-9a4e-8309cf7b767c"},
        TaskStatus::Rejected,
        Service::Support,
        {"message"},
        maps::chrono::parseIsoDateTime("2020-07-07T13:26:03.954Z")
    },
    {
        TaskChangeId{1439151},
        TaskId{"0c645c17-8422-4c29-9a4e-8309cf7b767c"},
        TaskStatus::InProgress,
        std::nullopt,
        std::nullopt,
        maps::chrono::parseIsoDateTime("2020-07-07T12:38:01.000Z")
    },
};

} // namespace

Y_UNIT_TEST_SUITE(test_responses)
{

// TODO: validate result with swagger schema from
// https://a.yandex-team.ru/review/1321206/files#file-0-38935409

Y_UNIT_TEST(task_by_id_without_history)
{
    const auto expected = maps::json::Value::fromFile(
        SRC_("data/task_without_history.json"));
    const auto result = maps::json::Value::fromString(makeTaskByIdJson(
        SAMPLE_TASK, "https://avatars.mds.yandex.net", std::nullopt));

    UNIT_ASSERT_VALUES_EQUAL(result, expected);
}

Y_UNIT_TEST(task_by_id_with_history)
{
    const auto expected = maps::json::Value::fromFile(
        SRC_("data/task_with_history.json"));
    const auto result = maps::json::Value::fromString(makeTaskByIdJson(
        SAMPLE_TASK, "https://avatars.mds.yandex.net", SAMPLE_TASK_HISTORY));

    UNIT_ASSERT_VALUES_EQUAL(result, expected);
}

Y_UNIT_TEST(tasks_without_history)
{
    const auto expected = maps::json::Value::fromFile(
        SRC_("data/tasks_without_history.json"));

    const auto result = maps::json::Value::fromString(makeTasksJson(
        {SAMPLE_TASK},
        /* totalTaskCount = */ 739,
        /* offset = */ 2,
        /* limit = */ 42,
        "https://avatars.mds.yandex.net",
        std::nullopt));

    UNIT_ASSERT_VALUES_EQUAL(result, expected);
}

Y_UNIT_TEST(tasks_with_empty_history)
{
    const auto expected = maps::json::Value::fromFile(
        SRC_("data/tasks_with_empty_history.json"));

    const auto result = maps::json::Value::fromString(makeTasksJson(
        {SAMPLE_TASK},
        /* totalTaskCount = */ 739,
        /* offset = */ 2,
        /* limit = */ 42,
        "https://avatars.mds.yandex.net",
        TaskHistories{}));

    UNIT_ASSERT_VALUES_EQUAL(result, expected);
}

Y_UNIT_TEST(tasks_with_history)
{
    const auto expected = maps::json::Value::fromFile(
        SRC_("data/tasks_with_history.json"));

    TaskHistories taskHistories;
    taskHistories[SAMPLE_TASK.id] = SAMPLE_TASK_HISTORY;
    const auto result = maps::json::Value::fromString(makeTasksJson(
        {SAMPLE_TASK},
        /* totalTaskCount = */ 739,
        /* offset = */ 2,
        /* limit = */ 42,
        "https://avatars.mds.yandex.net",
        taskHistories));

    UNIT_ASSERT_VALUES_EQUAL(result, expected);
}

Y_UNIT_TEST(task_id)
{
    const TaskId taskId{"0c645c17-8422-4c29-9a4e-8309cf7b767c"};
    UNIT_ASSERT_VALUES_EQUAL(
        makeTaskIdJson(taskId),
        R"({"id":"0c645c17-8422-4c29-9a4e-8309cf7b767c"})");
}

Y_UNIT_TEST(original_task_meta)
{
    const std::vector<std::string> formTypeValues{
        "address/add",
        "address/edit",
    };
    const std::vector<std::string> clientIdValues{
        "desktop-maps",
        "mobile_maps_android",
        "mobile_maps_ios",
        "web",
    };
    const std::vector<std::string> formContextIdValues{
        "toponym.building",
        "ugc_profile.add_address_assignment.submit",
    };
    const std::vector<std::string> clientContextIdValues{
        "context.footer",
        "organization.push_notifications",
    };
    const auto result = maps::json::Value::fromString(makeOriginalTaskMetaResponse(
        formTypeValues,
        clientIdValues,
        formContextIdValues,
        clientContextIdValues));

    const auto expected = maps::json::Value::fromFile(
        SRC_("data/original_task_meta.json"));

    UNIT_ASSERT_VALUES_EQUAL(result["form_type"], expected["form_type"]);
    UNIT_ASSERT_VALUES_EQUAL(result["client_id"], expected["client_id"]);
    UNIT_ASSERT_VALUES_EQUAL(result["form_context_id"], expected["form_context_id"]);
    UNIT_ASSERT_VALUES_EQUAL(result["client_context_id"], expected["client_context_id"]);

    UNIT_ASSERT_VALUES_EQUAL(result["form_id"], expected["form_id"]);
    UNIT_ASSERT_VALUES_EQUAL(
        result["question_id"].size(),
        maps::enum_io::enumerateValues<QuestionId>().size());
    UNIT_ASSERT_VALUES_EQUAL(
        result["answer_id"].size(),
        maps::enum_io::enumerateValues<AnswerId>().size());
}

Y_UNIT_TEST(filter_by_regex)
{
    std::vector<std::string> formTypes{
        "organization/edit-info",
        "assignment/barrier/location/edit",
        "road/add",
        "route/incorrect/obstruction",
        "Caps",
        "_front",
        "/front",
        "-front",
        "back_",
        "back/",
        "back-",
        "multiple__delims",
        "multiple//delims",
        "multiple--delims",
    };
    std::vector<std::string> filteredFormTypes{
        "organization/edit-info",
        "assignment/barrier/location/edit",
        "road/add",
        "route/incorrect/obstruction",
    };
    UNIT_ASSERT_VALUES_EQUAL(
        filterByRegex(formTypes, validateFormType), filteredFormTypes);
}

} // test_get_tasks_responses suite

} // namespace maps::wiki::feedback::api::tests
