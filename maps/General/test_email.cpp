#include "common.h"
#include "pretty_print.h"

#include <maps/sprav/callcenter/libs/company_changes/email.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::sprav::callcenter::company_changes::tests {

class EmailChangeBuilderTest : public testing::Test {
public:
    EmailChangeBuilder changeBuilder;

    NSprav::Email buildEmail(NSprav::Action action, const std::string& value) {
        NSprav::Email result;
        result.set_action(action);
        result.set_value(value.c_str());
        return result;
    }

    NSprav::Company buildChanges(std::vector<NSprav::Email> emails) {
        NSprav::Company result;
        *result.mutable_emails() = {emails.begin(), emails.end()};
        return result;
    }

    NSpravTDS::CompanyEmail buildTDSEmail(const std::string& value) {
        NSpravTDS::CompanyEmail result;
        result.set_value(value.c_str());
        return result;
    }

    NSpravTDS::Company buildCompany(std::vector<NSpravTDS::CompanyEmail> emails) {
        NSpravTDS::Company result;
        *result.mutable_emails() = {emails.begin(), emails.end()};
        return result;
    }
};

TEST_F(EmailChangeBuilderTest, est) {
    proto::Request request1 = buildRequest(1, buildChanges({
        buildEmail(
            NSprav::Action::DELETE,
            "test@ya.ru"
        ),
    }));

    proto::Request request2 = buildRequest(2, buildChanges({
        buildEmail(
            NSprav::Action::DELETE,
            "test1@ya.ru"
        ),
        buildEmail(
            NSprav::Action::ACTUALIZE,
            "test2@ya.ru"
        ),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSEmail("test@ya.ru")});

    std::vector<AttributeChanges<NSpravTDS::CompanyEmail>> expected = {
        {
            buildTDSEmail("test@ya.ru"),
            {},
            true
        },
        {
            {},
            {{
                buildTDSEmail("test2@ya.ru"),
                {2},
                NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::CompanyEmail>> result = changeBuilder.apply({request1, request2}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

} // maps::sprav::callcenter::company_changes::tests
