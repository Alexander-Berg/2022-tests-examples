#include "common.h"
#include "pretty_print.h"

#include <maps/sprav/callcenter/libs/company_changes/name.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::sprav::callcenter::company_changes::tests {

class NameChangeBuilderTest : public testing::Test {
public:
    NameChangeBuilder changeBuilder;

    NSprav::Name buildName(NSprav::Action action, const std::string& name, NSprav::Name::Type type) {
        NSprav::Name result;
        result.set_action(action);
        result.set_value(name.c_str());
        result.set_type(type);
        result.set_lang(NSprav::NLanguage::Language::RU);
        return result;
    }

    NSprav::Company buildChanges(std::vector<NSprav::Name> names) {
        NSprav::Company result;
        *result.mutable_names() = {names.begin(), names.end()};
        return result;
    }

    NSpravTDS::CompanyName buildTDSName(const std::string& name, NSpravTDS::CompanyName::Type type) {
        NSpravTDS::CompanyName result;
        result.set_type(type);
        result.mutable_value()->set_value(name.c_str());
        result.mutable_value()->mutable_lang()->set_locale("RU");
        return result;
    }

    NSpravTDS::Company buildCompany(std::vector<NSpravTDS::CompanyName> names) {
        NSpravTDS::Company result;
        *result.mutable_names() = {names.begin(), names.end()};
        return result;
    }
};

TEST_F(NameChangeBuilderTest, MainNameChanges) {
    proto::Request request = buildRequest(1, buildChanges({
        buildName(
            NSprav::Action::ACTUALIZE,
            "Рога & Копыта",
            NSprav::Name::MAIN
        ),
        buildName(
            NSprav::Action::DELETE,
            "Рога и Копыта",
            NSprav::Name::MAIN
        )
    }));

    NSpravTDS::Company company = buildCompany({buildTDSName("Рога и Копыта", NSpravTDS::CompanyName::Main)});

    std::vector<AttributeChanges<NSpravTDS::CompanyName>> expected = {{
        buildTDSName("Рога и Копыта", NSpravTDS::CompanyName::Main),
        {{
            buildTDSName("Рога & Копыта", NSpravTDS::CompanyName::Main),
            {1},
            NSprav::Action::NONE
        }},
        false
    }};

    std::vector<AttributeChanges<NSpravTDS::CompanyName>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

TEST_F(NameChangeBuilderTest, Complicated) {
    proto::Request request1 = buildRequest(1, buildChanges({
        buildName(
            NSprav::Action::ACTUALIZE,
            "Рога & Копыта",
            NSprav::Name::MAIN
        ),
        buildName(
            NSprav::Action::DELETE,
            "Рога и Копыта",
            NSprav::Name::MAIN
        )
    }));
    proto::Request request2 = buildRequest(2, buildChanges({
        buildName(
            NSprav::Action::DELETE,
            "ООО Рога и Копыта",
            NSprav::Name::SYNONYM
        ),
        buildName(
            NSprav::Action::ACTUALIZE,
            "ООО Рога & Копыта",
            NSprav::Name::SYNONYM
        ),
        buildName(
            NSprav::Action::ACTUALIZE,
            "ООО Рога + Копыта",
            NSprav::Name::SYNONYM
        ),
    }));

    NSpravTDS::Company company = buildCompany({
            buildTDSName("Рога и Копыта", NSpravTDS::CompanyName::Main),
            buildTDSName("РИК", NSpravTDS::CompanyName::Short),
            buildTDSName("ООО Раздолбай", NSpravTDS::CompanyName::Legal),
            buildTDSName("ООО Рога и Копыта", NSpravTDS::CompanyName::Synonym),
    });

    std::vector<AttributeChanges<NSpravTDS::CompanyName>> expected = {
        {
            buildTDSName("Рога и Копыта", NSpravTDS::CompanyName::Main),
            {{
                buildTDSName("Рога & Копыта", NSpravTDS::CompanyName::Main),
                {1},
                NSprav::Action::NONE
            }},
            false
        },
        {
            buildTDSName("РИК", NSpravTDS::CompanyName::Short),
            {},
            false
        },
        {
            buildTDSName("ООО Раздолбай", NSpravTDS::CompanyName::Legal),
            {},
            false
        },
        {
            buildTDSName("ООО Рога и Копыта", NSpravTDS::CompanyName::Synonym),
            {},
            false
        },
        {
            {},
            {{
                buildTDSName("ООО Рога + Копыта", NSpravTDS::CompanyName::Synonym),
                {2},
                NSprav::Action::NONE
            }},
            false
        },
        {
            {},
            {{
                buildTDSName("ООО Рога & Копыта", NSpravTDS::CompanyName::Synonym),
                {2},
                NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::CompanyName>> result = changeBuilder.apply({request1, request2}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

} // namespace maps::sprav::callcenter::company_changes::tests
