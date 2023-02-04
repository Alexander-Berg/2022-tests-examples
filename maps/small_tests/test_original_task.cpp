#include <maps/wikimap/feedback/api/src/libs/common/original_task.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/printers.h>
#include <maps/libs/json/include/value.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::tests {

Y_UNIT_TEST_SUITE(test_original_task)
{

// TODO: validate result with json schema from
// https://a.yandex-team.ru/review/1321206/files#file-0-38935410

Y_UNIT_TEST(original_task_json)
{
    const auto& jsonTask = maps::json::Value::fromFile(SRC_("data/original_task1.json"));
    const OriginalTask fromJson{jsonTask};
    const std::string jsonRes = (maps::json::Builder() << fromJson).str();
    UNIT_ASSERT_VALUES_EQUAL(
        jsonTask,
        maps::json::Value::fromString(jsonRes));
}

Y_UNIT_TEST(trim_question_id_prefix)
{
    OriginalTask originalTask(maps::json::Value::fromString(R"(
    {
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
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "answer_id": "entrance",
        "object_id": "object_id",
        "question_id": "toponym_add_object"
    })"));
    UNIT_ASSERT_VALUES_EQUAL(originalTask.questionId(), QuestionId::AddObject);

    UNIT_ASSERT_VALUES_EQUAL(
        internal::trimQuestionIdPrefix("organization_add_object"),
        "add_object");
    UNIT_ASSERT_VALUES_EQUAL(
        internal::trimQuestionIdPrefix("organization_entrance"),
        "organization_entrance");
    UNIT_ASSERT_VALUES_EQUAL(
        internal::trimQuestionIdPrefix("random_string"),
        "random_string");

    auto originalTaskJson = maps::json::Value::fromString(R"(
    {
        "form_id": "organization",
        "form_point": {
            "lon": 37.37,
            "lat": 55.55
        },
        "message": "test",
        "metadata": {
            "locale": "ru_RU",
            "client_id": "mobile_maps_android",
            "version": "1.0",
            "bebr_session_id": "1"
        },
        "answer_id": "random_string",
        "object_id": "object_id",
        "question_id": "random_string"
    })");
    UNIT_ASSERT_EXCEPTION(OriginalTask{originalTaskJson}, BadInputDataError);
}

Y_UNIT_TEST(original_task_restore_object_url_exception)
{
    OriginalTask originalTask(maps::json::Value::fromString(R"(
    {
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
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "answer_id": "entrance",
        "question_id": "add_object"
    })"));

    UNIT_ASSERT_EXCEPTION(originalTask.restoreObjectUrl(), maps::RuntimeError);
}

Y_UNIT_TEST(original_task_restore_object_url_route)
{
    OriginalTask originalTask(maps::json::Value::fromString(R"(
    {
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
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "answer_id": "entrance",
        "object_id": "object_id",
        "question_id": "add_object",
        "question_context": {
            "route": {
                "way_points": [
                    {
                        "lat": 55.55,
                        "lon": 37.37
                    },
                    {
                        "lat": 42.42,
                        "lon": 22.22
                    }
                ]
            }
        }
    })"));

    UNIT_ASSERT_NO_EXCEPTION(originalTask.restoreObjectUrl());
    UNIT_ASSERT(originalTask.objectUrl());
    UNIT_ASSERT_VALUES_EQUAL(
        *originalTask.objectUrl(),
        "https://yandex.ru/maps/?mode=routes&routes&"
        "rtext=37.370000,55.550000~22.220000,42.420000&rtt=comparison&ruri=~");
}

Y_UNIT_TEST(original_task_restore_object_url_toponym)
{
    OriginalTask originalTask1(maps::json::Value::fromString(R"(
    {
        "form_id": "toponym",
        "form_point": {
            "lon": 37.37,
            "lat": 55.55
        },
        "message": "test",
        "metadata": {
            "uid": 1,
            "uuid": "u-u-i-d-1",
            "locale": "ru_RU",
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "answer_id": "entrance",
        "object_id": "object_id",
        "question_id": "add_object"
    })"));
    UNIT_ASSERT_NO_EXCEPTION(originalTask1.restoreObjectUrl());
    UNIT_ASSERT(originalTask1.objectUrl());
    UNIT_ASSERT_VALUES_EQUAL(
        *originalTask1.objectUrl(),
        "https://n.maps.yandex.ru/#!/objects/object_id");

    OriginalTask originalTask2(maps::json::Value::fromString(R"(
    {
        "form_id": "toponym",
        "form_point": {
            "lon": 37.37,
            "lat": 55.55
        },
        "message": "test",
        "metadata": {
            "uid": 1,
            "uuid": "u-u-i-d-1",
            "locale": "ru_RU",
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "answer_id": "entrance",
        "object_uri": "object_uri",
        "question_id": "add_object"
    })"));
    UNIT_ASSERT_NO_EXCEPTION(originalTask2.restoreObjectUrl());
    UNIT_ASSERT(originalTask2.objectUrl());
    UNIT_ASSERT_VALUES_EQUAL(
        *originalTask2.objectUrl(),
        "https://yandex.ru/maps?ol=geo&ouri=object_uri");
}

Y_UNIT_TEST(original_task_restore_object_url_organization)
{
    OriginalTask originalTask1(maps::json::Value::fromString(R"(
    {
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
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "answer_id": "entrance",
        "object_id": "object_id",
        "question_id": "add_object"
    })"));
    UNIT_ASSERT_NO_EXCEPTION(originalTask1.restoreObjectUrl());
    UNIT_ASSERT(originalTask1.objectUrl());
    UNIT_ASSERT_VALUES_EQUAL(
        *originalTask1.objectUrl(),
        "https://yandex.ru/maps/org/object_id");

    OriginalTask originalTask2(maps::json::Value::fromString(R"(
    {
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
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "answer_id": "entrance",
        "object_uri": "ymapsbm1://org?oid=28397857759",
        "question_id": "add_object"
    })"));
    UNIT_ASSERT_NO_EXCEPTION(originalTask2.restoreObjectUrl());
    UNIT_ASSERT(originalTask2.objectUrl());
    UNIT_ASSERT_VALUES_EQUAL(
        *originalTask2.objectUrl(),
        "https://yandex.ru/maps/org/28397857759");
}

Y_UNIT_TEST(original_task_bizdir_sps_feedback)
{
    {
        // Bizdir SPS feedback
        OriginalTask originalTask(maps::json::Value::fromString(R"(
        {
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
                "client_id": "mobile_maps_android",
                "version": "1.0"
            },
            "answer_id": "organization",
            "object_uri": "ymapsbm1://org?oid=28397857759",
            "question_id": "add_object",
            "answer_context": {
                "company": {
                    "rubric_names": [
                        "МФЦ"
                    ]
                }
            }
        })"));

        UNIT_ASSERT(originalTask.isBizdirSpsFeedback());
    }

    {
        // Missing the right rubrics
        OriginalTask originalTask(maps::json::Value::fromString(R"(
        {
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
                "client_id": "mobile_maps_android",
                "version": "1.0"
            },
            "answer_id": "organization",
            "object_uri": "ymapsbm1://org?oid=28397857759",
            "question_id": "add_object",
            "answer_context": {
                "company": {
                    "rubric_names": [
                        "ТРЦ"
                    ]
                }
            }
        })"));

        UNIT_ASSERT(!originalTask.isBizdirSpsFeedback());
    }

    {
        // Wrong location
        OriginalTask originalTask(maps::json::Value::fromString(R"(
        {
            "form_id": "organization",
            "form_point": {
                "lon": 27.5,
                "lat": 54.0
            },
            "message": "test",
            "metadata": {
                "uid": 1,
                "uuid": "u-u-i-d-1",
                "locale": "ru_RU",
                "client_id": "mobile_maps_android",
                "version": "1.0"
            },
            "answer_id": "organization",
            "object_uri": "ymapsbm1://org?oid=28397857759",
            "question_id": "add_object",
            "answer_context": {
                "company": {
                    "rubric_names": [
                        "МФЦ"
                    ]
                }
            }
        })"));

        UNIT_ASSERT(!originalTask.isBizdirSpsFeedback());
    }
}

Y_UNIT_TEST(original_task_validation)
{
    UNIT_ASSERT(validateClientId("ru.yandex.mobile.navigator.inhouse"));
    UNIT_ASSERT(validateClientId("ru.yandex.traffic"));
    UNIT_ASSERT(validateClientId("nyak"));
    UNIT_ASSERT(validateClientId("desktop-maps"));
    UNIT_ASSERT(validateClientId("mobile_maps_android"));
    UNIT_ASSERT(!validateClientId("Caps"));
    UNIT_ASSERT(!validateClientId("_front"));
    UNIT_ASSERT(!validateClientId(".front"));
    UNIT_ASSERT(!validateClientId("-front"));
    UNIT_ASSERT(!validateClientId("back_"));
    UNIT_ASSERT(!validateClientId("back."));
    UNIT_ASSERT(!validateClientId("back-"));
    UNIT_ASSERT(!validateClientId("multiple__delims"));
    UNIT_ASSERT(!validateClientId("multiple..delims"));
    UNIT_ASSERT(!validateClientId("multiple--delims"));
    for (auto&& symbol : std::string{"!@#$%^&*()+=~`{}[]\\|,<>/?:;'\" 1234567890"}) {
        UNIT_ASSERT(!validateClientId(
            std::string{"strange"} + symbol + "symbol"));
    }

    UNIT_ASSERT(validateClientContextId("ugc.entrances_edit.push_notifications"));
    UNIT_ASSERT(validateClientContextId("navi_ribbon_menu"));
    UNIT_ASSERT(validateClientContextId("context.footer"));
    UNIT_ASSERT(!validateClientContextId("Caps"));
    UNIT_ASSERT(!validateClientContextId("_front"));
    UNIT_ASSERT(!validateClientContextId(".front"));
    UNIT_ASSERT(!validateClientContextId("-front"));
    UNIT_ASSERT(!validateClientContextId("back_"));
    UNIT_ASSERT(!validateClientContextId("back."));
    UNIT_ASSERT(!validateClientContextId("back-"));
    UNIT_ASSERT(!validateClientContextId("multiple__delims"));
    UNIT_ASSERT(!validateClientContextId("multiple..delims"));
    UNIT_ASSERT(!validateClientContextId("multiple--delims"));
    for (auto&& symbol : std::string{"!@#$%^&*()+=~`{}[]\\|,<>/?:;'\" 1234567890"}) {
        UNIT_ASSERT(!validateClientContextId(
            std::string{"strange"} + symbol + "symbol"));
    }

    UNIT_ASSERT(validateFormContextId("ugc_profile.add_address_assignment.submit"));
    UNIT_ASSERT(validateFormContextId("mapeditor_mobile_landing"));
    UNIT_ASSERT(validateFormContextId("context.footer"));
    UNIT_ASSERT(validateFormContextId("other"));
    UNIT_ASSERT(!validateFormContextId("<scRipt>ns(0x0361A8)</scRipt>"));
    UNIT_ASSERT(!validateFormContextId("\\';netsparker(0x0361C7);///"));
    UNIT_ASSERT(!validateFormContextId("Caps"));
    UNIT_ASSERT(!validateFormContextId("_front"));
    UNIT_ASSERT(!validateFormContextId(".front"));
    UNIT_ASSERT(!validateFormContextId("-front"));
    UNIT_ASSERT(!validateFormContextId("back_"));
    UNIT_ASSERT(!validateFormContextId("back."));
    UNIT_ASSERT(!validateFormContextId("back-"));
    UNIT_ASSERT(!validateFormContextId("multiple__delims"));
    UNIT_ASSERT(!validateFormContextId("multiple..delims"));
    UNIT_ASSERT(!validateFormContextId("multiple--delims"));
    for (auto&& symbol : std::string{"!@#$%^&*()+=~`{}[]\\|,<>/?:;'\" 1234567890"}) {
        UNIT_ASSERT(!validateFormContextId(
            std::string{"strange"} + symbol + "symbol"));
    }

    UNIT_ASSERT(validateFormType("organization/edit-info"));
    UNIT_ASSERT(validateFormType("assignment/barrier/location/edit"));
    UNIT_ASSERT(validateFormType("road/add"));
    UNIT_ASSERT(validateFormType("route/incorrect/obstruction"));
    UNIT_ASSERT(!validateFormType("Caps"));
    UNIT_ASSERT(!validateFormType("_front"));
    UNIT_ASSERT(!validateFormType("/front"));
    UNIT_ASSERT(!validateFormType("-front"));
    UNIT_ASSERT(!validateFormType("back_"));
    UNIT_ASSERT(!validateFormType("back/"));
    UNIT_ASSERT(!validateFormType("back-"));
    UNIT_ASSERT(!validateFormType("multiple__delims"));
    UNIT_ASSERT(!validateFormType("multiple//delims"));
    UNIT_ASSERT(!validateFormType("multiple--delims"));
    for (auto&& symbol : std::string{"!@#$%^&*()+=~`{}[]\\|,<>.?:;'\" 1234567890"}) {
        UNIT_ASSERT(!validateFormType(
            std::string{"strange"} + symbol + "symbol"));
    }
}

Y_UNIT_TEST(original_task_sensitive_data)
{
    auto json = maps::json::Value::fromString(R"(
    {
        "form_id": "organization",
        "form_point": {
            "lon": 27.5,
            "lat": 54.0
        },
        "metadata": {
            "locale": "ru_RU",
            "client_id": "mobile_maps_android",
            "version": "1.0",
            "cookie": "cookie",
            "user_agent": "userAgent"
        },
        "answer_id": "organization",
        "question_id": "add_object"
    })");

    std::string uid = "42";
    std::string userEmail = "sample@yandex.ru";

    OriginalTask originalTask(json, HttpData{"cookie", "userAgent", ""}, uid, userEmail);
    UNIT_ASSERT_VALUES_EQUAL(originalTask.uid()->value(), 42ull);
    UNIT_ASSERT_VALUES_EQUAL(*originalTask.userEmail(), userEmail);

    auto jsonWithUid = maps::json::Value::fromString(R"(
    {
        "form_id": "organization",
        "form_point": {
            "lon": 27.5,
            "lat": 54.0
        },
        "metadata": {
            "uid": "42",
            "locale": "ru_RU",
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "answer_id": "organization",
        "question_id": "add_object"
    })");
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        OriginalTask(jsonWithUid, HttpData{}, uid, userEmail),
        BadInputDataError,
        "uid");

    auto jsonWithEmail = maps::json::Value::fromString(R"(
    {
        "form_id": "organization",
        "form_point": {
            "lon": 27.5,
            "lat": 54.0
        },
        "metadata": {
            "locale": "ru_RU",
            "client_id": "mobile_maps_android",
            "version": "1.0",
            "user_agent": "userAgent"
        },
        "answer_id": "organization",
        "question_id": "add_object",
        "user_email": "sample@yandex.ru"
    })");
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        OriginalTask(jsonWithEmail, HttpData{"cookie", "userAgent", ""}, uid, userEmail),
        BadInputDataError,
        "user_email");
}


Y_UNIT_TEST(original_task_photo_uris)
{
    OriginalTask originalTask(maps::json::Value::fromString(R"({
        "answer_id": "organization",
        "attached_photos": [
            "1000000/2a000001234567890abcdef1234567890000",
            "1000001/2a000001234567890abcdef1234567890001",
            "https://avatars.mds.yandex.net/get-maps-feedback/1000002/2a000001234567890abcdef1234567890002/orig",
            "https://avatars.mds.yandex.net/get-maps-feedback/1000003/2a000001234567890abcdef1234567890003/orig"
        ],
        "form_id": "organization",
        "form_point": {
            "lon": 27.5,
            "lat": 54.0
        },
        "metadata": {
            "locale": "ru_RU",
            "client_id": "mobile_maps_android",
            "version": "1.0"
        },
        "question_id": "add_object"
    })"));

    UNIT_ASSERT_VALUES_EQUAL(
        originalTask.photoUris("https://avatars.mds.yandex.net"),
        (std::vector<std::string>{
            "https://avatars.mds.yandex.net/get-maps-feedback/1000000/2a000001234567890abcdef1234567890000/orig",
            "https://avatars.mds.yandex.net/get-maps-feedback/1000001/2a000001234567890abcdef1234567890001/orig",
            "https://avatars.mds.yandex.net/get-maps-feedback/1000002/2a000001234567890abcdef1234567890002/orig",
            "https://avatars.mds.yandex.net/get-maps-feedback/1000003/2a000001234567890abcdef1234567890003/orig"
        })
    );
}

} // test_task_change suite

} // namespace maps::wiki::feedback::api::tests
