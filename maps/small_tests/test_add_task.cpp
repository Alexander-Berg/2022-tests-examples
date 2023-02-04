#include <maps/wikimap/feedback/api/src/yacare/lib/add_task.h>
#include <maps/wikimap/feedback/api/src/yacare/lib/sync_utils.h>

#include <maps/wikimap/feedback/api/src/libs/common/original_task.h>
#include <maps/wikimap/feedback/api/src/libs/samsara/samsara_client.h>
#include <maps/wikimap/feedback/api/src/libs/sprav/sprav_client.h>

#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::tests {

Y_UNIT_TEST_SUITE(test_add_task)
{

namespace {

const OriginalTask ORIGINAL_TASK{maps::json::Value::fromString(R"(
{
    "form_id": "organization",
    "form_point": {
        "lon": 37.37,
        "lat": 55.55
    },
    "message": "test",
    "metadata": {
        "uid": 42,
        "uuid": "u-u-i-d-1",
        "locale": "en_EN",
        "client_id": "mobile_maps_android",
        "version": "1.0",
        "ip": "42.42.42.42",
        "application_version": "12.6.1",
        "device_id": "a6a8f89108dd48571c0d70b468c9464a",
        "user_region_id": 10990,
        "map_layer": "yandex#map",
        "map_zoom": 20
    },
    "answer_id": "entrance",
    "object_uri": "ymapsbm1://org?oid=28397857759",
    "object_id": "121312312",
    "object_url": "object_url",
    "question_id": "add_object",
    "answer_context": {
        "company": {
            "name": "Изумруд",
            "status": "open",
            "address": "Россия, Ростовская область, Шахты, переулок Вишневского, 7",
            "coordinates": {
                "lat": 47.683122,
                "lon": 40.238885
            },
            "entrances": [
                {
                    "name": "main",
                    "center_point": {
                        "lat": 47.683122,
                        "lon": 40.238885
                    }
                }
            ],
            "attached_photos": [
                "1906188/2a0000016eea8f4a64e29c7a20ff12b60a6e"
            ]
        }
    },
    "attached_photos": [
        "1895250/2a000001740a793daaec2195b31b1a6c01af"
    ]
})")};

const OriginalTask PROVIDER_TASK{maps::json::Value::fromString(R"(
{
    "form_id": "toponym",
    "form_point": {
        "lon": 37.37,
        "lat": 55.55
    },
    "message": "test",
    "metadata": {
        "uid": 42,
        "uuid": "u-u-i-d-1",
        "locale": "en_EN",
        "client_id": "mobile_maps_android",
        "version": "1.0",
        "ip": "42.42.42.42",
        "application_version": "12.6.1",
        "device_id": "a6a8f89108dd48571c0d70b468c9464a",
        "user_region_id": 10990,
        "map_layer": "yandex#map",
        "map_zoom": 20
    },
    "answer_id": "provider",
    "object_uri": "ymapsbm1://org?oid=28397857759",
    "object_id": "121312312",
    "object_url": "object_url",
    "question_id": "add_object",
    "answer_context": {
        "provider": {
            "name": "Provider name",
            "site_url": "https://some-site.ru",
            "text": "Provider organization",
            "uri": ""
        }
    }
})")};

} // namespace

Y_UNIT_TEST(test_merge_original_task_photos)
{
    OriginalTask originalTask(ORIGINAL_TASK);
    mergeOriginalTaskPhotos(originalTask);

    const std::vector<std::string> expected {
        "1895250/2a000001740a793daaec2195b31b1a6c01af",
        "1906188/2a0000016eea8f4a64e29c7a20ff12b60a6e"
    };
    UNIT_ASSERT_VALUES_EQUAL(
        originalTask.attachedPhotos(),
        expected);
}

Y_UNIT_TEST(test_create_draft_task)
{
    auto draftTask = DraftTask(ORIGINAL_TASK, TaskId("1"));
    const FeedbackTask& task = draftTask.task();
    UNIT_ASSERT_VALUES_EQUAL(task.service, Service::Sprav);
    UNIT_ASSERT_VALUES_EQUAL(task.status, TaskStatus::InProgress);
    UNIT_ASSERT_VALUES_EQUAL(task.originalTask, ORIGINAL_TASK);
    UNIT_ASSERT_VALUES_EQUAL(task.id.value(), "1");
    UNIT_ASSERT_VALUES_EQUAL(
        maps::chrono::formatSqlDateTime(task.createdAt),
        maps::chrono::formatSqlDateTime(task.updatedAt));
}

Y_UNIT_TEST(test_create_provider_task)
{
    auto draftTask = DraftTask(PROVIDER_TASK, TaskId("1"));
    const FeedbackTask& task = draftTask.task();
    UNIT_ASSERT_VALUES_EQUAL(task.service, Service::Support);
    UNIT_ASSERT_VALUES_EQUAL(task.status, TaskStatus::Rejected);
    UNIT_ASSERT_VALUES_EQUAL(task.originalTask, PROVIDER_TASK);
    UNIT_ASSERT_VALUES_EQUAL(task.id.value(), "1");
    UNIT_ASSERT_VALUES_EQUAL(
        maps::chrono::formatSqlDateTime(task.createdAt),
        maps::chrono::formatSqlDateTime(task.updatedAt));
}

} // test_add_task suite

} // namespace maps::wiki::feedback::api::tests
