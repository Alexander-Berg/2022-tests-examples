#pragma once

#include <maps/wikimap/feedback/api/src/libs/common/lang.h>
#include <maps/wikimap/feedback/api/src/libs/common/types.h>

#include <maps/wikimap/feedback/api/src/synctool/lib/need_info_email_settings.h>
#include <maps/wikimap/feedback/api/src/synctool/lib/types.h>

#include <maps/wikimap/mapspro/libs/http/include/yandex/maps/wiki/http/tanker/tanker.h>

#include <maps/libs/json/include/value.h>

namespace maps::wiki::feedback::api::sync::tests {

using maps::wiki::tanker::TranslationMap;

const std::map<Lang, PushTypeToTranslations> PUSHES_TEST_TRANSLATIONS {
    {
        Lang::En,
        {
            {
                sync_queue::PushType::ToponymPublished,
                {
                    .title = "Your edits were added",
                    .message = "Thank you for helping make Yandex Maps more accurate",
                },
            },
            {
                sync_queue::PushType::ToponymRejectedProhibitedByRules,
                {
                    .title = "We checked your edits",
                    .message = "But we can't add them, so we'll leave everything as it was for now",
                },
            },
            {
                sync_queue::PushType::ToponymRejectedIncorrectData,
                {
                    .title = "We checked your edits",
                    .message = "But the information wasn't confirmed, so we'll leave everything as it was for now",
                },
            }
        }
    },
    {
        Lang::Ru,
        {
            {
                sync_queue::PushType::ToponymPublished,
                {
                    .title = "Ваши исправления добавлены",
                    .message = "Спасибо, что помогаете делать карту точнее",
                },
            },
            {
                sync_queue::PushType::ToponymRejectedProhibitedByRules,
                {
                    .title = "Мы проверили ваши исправления",
                    .message = "Но внести их не получится — пока оставим как было",
                },
            },
            {
                sync_queue::PushType::ToponymRejectedIncorrectData,
                {
                    .title = "Мы проверили ваши исправления",
                    .message = "Но информация не подтвердилась, поэтому пока оставим всё как было",
                },
            }
        }
    }
};

const std::map<Lang, TranslationMap> NEED_INFO_EMAILS_TEST_TRANSLATIONS {
    {Lang::Ru, {
        {"need_info_default_subject", "Мы проверили ваши правки — пожалуйста, уточните информацию"},
        {"need_info_need_full_address_subject", "Мы проверили ваши правки — пожалуйста, укажите полный адрес"},
        {"need_info_remove_need_proof_subject", "Мы проверили ваши правки — пожалуйста, уточните пару моментов"},
        {"message", "Сообщение"},
        {"object", "Объект"},
    }},
    {Lang::En, {
        {"need_info_default_subject", "We checked your edits. Please clarify something"},
        {"need_info_need_full_address_subject", "We checked your edits. Please provide a full address"},
        {"need_info_remove_need_proof_subject", "We checked your edits. Please clarify a few things"},
        {"message", "Message"},
        {"object", "Object"},
    }},
};

const std::map<Lang, TranslationMap> SAMSARA_TRANSLATIONS {
    {
        Lang::Ru, {
            {"message", "Сообщение"},
            {"object", "Объект"},
        }
    },
    {
        Lang::En, {
            {"message", "Message"},
            {"object", "Object"},
        }
    },
};

const std::map<RequestTemplateId, SubjectTranslations> NEED_INFO_EMAILS_SUBJECT_TEST_TRANSLATIONS {
    {
        RequestTemplateId::ToponymNeedProof,
        SubjectTranslations{
            {Lang::Ru, "Мы проверили ваши правки — пожалуйста, уточните информацию"},
            {Lang::En, "We checked your edits. Please clarify something"},
        },
    },
    {
        RequestTemplateId::NeedFullAddress,
        SubjectTranslations{
            {Lang::Ru, "Мы проверили ваши правки — пожалуйста, укажите полный адрес"},
            {Lang::En, "We checked your edits. Please provide a full address"},
        },
    },
    {
        RequestTemplateId::ObstacleNeedProof,
        SubjectTranslations{
            {Lang::Ru, "Мы проверили ваши правки — пожалуйста, уточните информацию"},
            {Lang::En, "We checked your edits. Please clarify something"},
        },
    },
    {
        RequestTemplateId::RemoveNeedProof,
        SubjectTranslations{
            {Lang::Ru, "Мы проверили ваши правки — пожалуйста, уточните пару моментов"},
            {Lang::En, "We checked your edits. Please clarify a few things"},
        },
    },
    {
        RequestTemplateId::NeedMoreInfo,
        SubjectTranslations{
            {Lang::Ru, "Мы проверили ваши правки — пожалуйста, уточните информацию"},
            {Lang::En, "We checked your edits. Please clarify something"},
        },
    },
};

const std::map<Lang, TranslationMap> EMAILS_TEST_TRANSLATIONS {
    {Lang::Ru, {
        {"add_object__toponym__toponym", "Добавлен адрес"},
        {"add_object__entrance__organization", "Добавлен вход в организацию"},
        {"add_object__entrance__toponym", "Добавлен вход в здание"},
        {"wrong_route__report_route__route", "Исправлен маршрут"},
    }},
    {Lang::En, {
        {"add_object__toponym__toponym", "Address added"},
        {"add_object__entrance__organization", "Business entrance added"},
        {"add_object__entrance__toponym", "Entrance added"},
        {"wrong_route__report_route__route", "Route updated"},
    }},
};

} // namespace maps::wiki::feedback::api::sync::tests
