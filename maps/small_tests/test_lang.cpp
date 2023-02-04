#include <maps/wikimap/feedback/api/src/libs/common/lang.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>


namespace maps::wiki::feedback::api::tests {

Y_UNIT_TEST_SUITE(test_lang)
{

Y_UNIT_TEST(test_language_by_locale)
{
    // https://wiki.yandex-team.ru/users/likynushka/feedback-notifications/#lokalizacija
    const std::vector<Lang> languages{Lang::Ru, Lang::En};

    // .ru
    UNIT_ASSERT_VALUES_EQUAL(determineLanguageByLocale("ru_RU", languages), Lang::Ru);

    // .com
    UNIT_ASSERT_VALUES_EQUAL(determineLanguageByLocale("en_RU", languages), Lang::En);
    UNIT_ASSERT_VALUES_EQUAL(determineLanguageByLocale("en_UA", languages), Lang::En);
    UNIT_ASSERT_VALUES_EQUAL(determineLanguageByLocale("en_TR", languages), Lang::En);
    UNIT_ASSERT_VALUES_EQUAL(determineLanguageByLocale("en_US", languages), Lang::En);
    UNIT_ASSERT_VALUES_EQUAL(determineLanguageByLocale("en_IL", languages), Lang::En);
    UNIT_ASSERT_VALUES_EQUAL(determineLanguageByLocale("en_AZ", languages), Lang::En);

    // .kz
    UNIT_ASSERT_VALUES_EQUAL(determineLanguageByLocale("ru_KZ", languages), Lang::Ru);
    UNIT_ASSERT_VALUES_EQUAL(determineLanguageByLocale("kk_KZ", languages), Lang::Ru);
}

Y_UNIT_TEST(test_project_locale)
{
    const std::vector<std::string> locales{
        "ru_RU",
        "en_US",
        "en_RU",
        "ru_UA",
        "uk_UA",
        "tr_TR",
    };

    UNIT_ASSERT_VALUES_EQUAL(projectLocale("ru_RU", locales), "ru_RU");
    UNIT_ASSERT_VALUES_EQUAL(projectLocale("en_RU", locales), "en_RU");
    UNIT_ASSERT_VALUES_EQUAL(projectLocale("fr_RU", locales), "en_RU");
    UNIT_ASSERT_VALUES_EQUAL(projectLocale("tr_GR", locales), "en_US");
}

} // test_lang suite

} // namespace maps::wiki::feedback::api::tests

