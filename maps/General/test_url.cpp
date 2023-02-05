#include "common.h"
#include "pretty_print.h"

#include <maps/sprav/callcenter/libs/company_changes/url.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::sprav::callcenter::company_changes::tests {

class UrlChangeBuilderTest : public testing::Test {
public:
    UrlChangeBuilder changeBuilder;

    NSprav::Url buildUrl(NSprav::Action action, const std::string& value) {
        NSprav::Url result;
        result.set_action(action);
        result.set_value(value.c_str());
        return result;
    }

    NSprav::Company buildChanges(std::vector<NSprav::Url> urls) {
        NSprav::Company result;
        *result.mutable_urls() = {urls.begin(), urls.end()};
        return result;
    }

    NSpravTDS::CompanyUrl buildTDSUrl(const std::string& value) {
        NSpravTDS::CompanyUrl result;
        result.set_value(value.c_str());
        result.set_type(NSpravTDS::CompanyUrl::Main);
        result.set_hide(false);
        result.set_page_type(NSpravTDS::CompanyUrl::Undefined);
        return result;
    }

    NSpravTDS::Company buildCompany(std::vector<NSpravTDS::CompanyUrl> urls) {
        NSpravTDS::Company result;
        *result.mutable_urls() = {urls.begin(), urls.end()};
        return result;
    }
};

TEST_F(UrlChangeBuilderTest, Test) {
    proto::Request request1 = buildRequest(1, buildChanges({
        buildUrl(
            NSprav::Action::DELETE,
            "test@ya.ru"
        ),
    }));

    proto::Request request2 = buildRequest(2, buildChanges({
        buildUrl(
            NSprav::Action::DELETE,
            "test1@ya.ru"
        ),
        buildUrl(
            NSprav::Action::ACTUALIZE,
            "test2@ya.ru"
        ),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSUrl("test@ya.ru")});

    std::vector<AttributeChanges<NSpravTDS::CompanyUrl>> expected = {
        {
            buildTDSUrl("test@ya.ru"),
            {},
            true
        },
        {
            {},
            {{
                buildTDSUrl("test2@ya.ru"),
                {2},
                NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::CompanyUrl>> result = changeBuilder.apply({request1, request2}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

} // maps::sprav::callcenter::company_changes::tests
