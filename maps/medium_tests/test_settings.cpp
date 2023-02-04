#include <maps/wikimap/feedback/api/src/synctool/lib/push_settings.h>

#include <maps/wikimap/feedback/api/src/synctool/tests/helpers/printers.h>
#include <maps/wikimap/feedback/api/src/synctool/tests/medium_tests/unittest_translations.h>

#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <map>

namespace maps::wiki::feedback::api::sync::tests {

namespace {

const std::string RU_PUSH_TRANSLATOINS_RESPONSE = R"({
    "ru": {
        "toponym_default_title": "Мы проверили ваши исправления",
        "toponym_rejected_incorrect_data_message": "Но информация не подтвердилась, поэтому пока оставим всё как было",
        "toponym_rejected_prohibited_by_rules_message": "Но внести их не получится — пока оставим как было",
        "toponym_published_title": "Ваши исправления добавлены",
        "toponym_published_message": "Спасибо, что помогаете делать карту точнее"
    }
})";

const std::string EN_PUSH_TRANSLATOINS_RESPONSE = R"({
    "en": {
        "toponym_default_title": "We checked your edits",
        "toponym_rejected_incorrect_data_message": "But the information wasn't confirmed, so we'll leave everything as it was for now",
        "toponym_rejected_prohibited_by_rules_message": "But we can't add them, so we'll leave everything as it was for now",
        "toponym_published_title": "Your edits were added",
        "toponym_published_message": "Thank you for helping make Yandex Maps more accurate"
    }
})";

const std::string RU_EMAIL_TRANSLATOINS_RESPONSE = R"({
    "ru": {
        "need_info_default_subject": "Мы проверили ваши правки — пожалуйста, уточните информацию",
        "need_info_need_full_address_subject": "Мы проверили ваши правки — пожалуйста, укажите полный адрес",
        "need_info_remove_need_proof_subject": "Мы проверили ваши правки — пожалуйста, уточните пару моментов",
        "message": "Сообщение",
        "object": "Объект"
    }
})";

const std::string EN_EMAIL_TRANSLATOINS_RESPONSE = R"({
    "en": {
        "need_info_default_subject": "We checked your edits. Please clarify something",
        "need_info_need_full_address_subject": "We checked your edits. Please provide a full address",
        "need_info_remove_need_proof_subject": "We checked your edits. Please clarify a few things",
        "message": "Message",
        "object": "Object"
    }
})";

} // namespace

Y_UNIT_TEST_SUITE(test_settings)
{

Y_UNIT_TEST(test_push_settings)
{
    auto mockTanker = maps::http::addMock(
        "https://tanker/keysets/json/",
        [] (const maps::http::MockRequest& request) {
            std::string paramsPrefix =
                "project-id=maps_feedback&keyset-id=PushNotifications&language=";

            maps::http::MockResponse response;
            if (request.url.params() == paramsPrefix + "ru") {
                response.body = RU_PUSH_TRANSLATOINS_RESPONSE;
            } else if (request.url.params() == paramsPrefix + "en") {
                response.body = EN_PUSH_TRANSLATOINS_RESPONSE;
            } else {
                UNIT_ASSERT(false && "Wrong params in request to tracker");
            }
            response.status = 200;
            return response;
        });

    auto config = maps::json::Value::fromFile(SRC_("data/feedback_api.conf"));

    PushSettings pushSettings(
        config["sup"],
        config["tanker"]["pushes"],
        config["yandexHosts"]["tanker"].as<std::string>(),
        "FAKE_TANKER_TOKEN",
        config["yandexHosts"]["ugcFeedback"].as<std::string>());

    UNIT_ASSERT_VALUES_EQUAL(
        pushSettings.translations.size(),
        PUSHES_TEST_TRANSLATIONS.size());
    UNIT_ASSERT_VALUES_EQUAL(
        pushSettings.translations.at(Lang::Ru),
        PUSHES_TEST_TRANSLATIONS.at(Lang::Ru));
    UNIT_ASSERT_VALUES_EQUAL(
        pushSettings.translations.at(Lang::En),
        PUSHES_TEST_TRANSLATIONS.at(Lang::En));

    UNIT_ASSERT_VALUES_EQUAL(
        pushSettings.clientIdToSupProject.at("ru.yandex.traffic.inhouse"),
        SupProject::Mobmaps);
    UNIT_ASSERT_VALUES_EQUAL(
        pushSettings.clientIdToSupProject.at("ru.yandex.mobile.navigator.sandbox"),
        SupProject::Navi);

    UNIT_ASSERT_VALUES_EQUAL(
        pushSettings.supProjectToPushTags.at(SupProject::Mobmaps).topic,
        "maps_status_of_feedback");
    UNIT_ASSERT_VALUES_EQUAL(
        pushSettings.supProjectToPushTags.at(SupProject::Mobmaps).pushOffTags,
        (std::vector<std::string>{
            "maps_status_of_feedback_off",
            "maps_status_of_feedback",
            "status_of_feedback"
        })
    );
    UNIT_ASSERT_VALUES_EQUAL(
        pushSettings.supProjectToPushTags.at(SupProject::Navi).topic,
        "navi_status_of_feedback");
    UNIT_ASSERT_VALUES_EQUAL(
        pushSettings.supProjectToPushTags.at(SupProject::Navi).pushOffTags,
        std::vector<std::string>{"navi_status_of_feedback_off"});

    UNIT_ASSERT_VALUES_EQUAL(
        pushSettings.destinationUrl,
        "https://l7test.yandex.by/maps/profile/ugc/feedback");
}

Y_UNIT_TEST(test_email_settings)
{
    auto mockTanker = maps::http::addMock(
        "https://tanker/keysets/json/",
        [] (const maps::http::MockRequest& request) {
            std::string paramsPrefix =
                "project-id=maps_feedback&keyset-id=EmailNotifications&language=";

            maps::http::MockResponse response;
            if (request.url.params() == paramsPrefix + "ru") {
                response.body = RU_EMAIL_TRANSLATOINS_RESPONSE;
            } else if (request.url.params() == paramsPrefix + "en") {
                response.body = EN_EMAIL_TRANSLATOINS_RESPONSE;
            } else {
                UNIT_ASSERT(false && "Wrong params in request to tracker");
            }
            response.status = 200;
            return response;
        });

    auto config = maps::json::Value::fromFile(SRC_("data/feedback_api.conf"));

    NeedInfoEmailSettings needInfoEmailSettings(
        config["samsara"]["templates"],
        config["tanker"]["need_info_emails"],
        config["yandexHosts"]["tanker"].as<std::string>(),
        "FAKE_TANKER_TOKEN");

    UNIT_ASSERT_VALUES_EQUAL(
        needInfoEmailSettings.subjectTranslations.size(),
        NEED_INFO_EMAILS_SUBJECT_TEST_TRANSLATIONS.size());
    for (auto templateId : std::vector<RequestTemplateId>{
        RequestTemplateId::ToponymNeedProof,
        RequestTemplateId::NeedFullAddress,
        RequestTemplateId::ObstacleNeedProof,
        RequestTemplateId::RemoveNeedProof,
        RequestTemplateId::NeedMoreInfo,
    }) {
        UNIT_ASSERT_VALUES_EQUAL(
            needInfoEmailSettings.subjectTranslations.at(templateId),
            NEED_INFO_EMAILS_SUBJECT_TEST_TRANSLATIONS.at(templateId));
        UNIT_ASSERT(needInfoEmailSettings.samsaraTemplates.count(templateId) > 0);
    }
}

} // test_settings suite

} // namespace maps::wiki::feedback::api::sync::tests
