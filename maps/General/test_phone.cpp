#include "common.h"
#include "pretty_print.h"

#include <maps/sprav/callcenter/libs/company_changes/phone.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::sprav::callcenter::company_changes::tests {

class PhoneChangeBuilderTest : public testing::Test {
public:
    PhoneChangeBuilder changeBuilder;

    NSprav::Phone buildPhone(
        NSprav::Action action, const std::string& countryCode,
        const std::string& regionCode, const std::string& number, const std::string& ext
    ) {
        NSprav::Phone result;
        result.set_action(action);
        result.set_country_code(countryCode.c_str());
        result.set_region_code(regionCode.c_str());
        result.set_number(number.c_str());
        result.set_ext(ext.c_str());
        return result;
    }

    NSprav::Company buildChanges(std::vector<NSprav::Phone> phones) {
        NSprav::Company result;
        *result.mutable_phones() = {phones.begin(), phones.end()};
        return result;
    }

    NSpravTDS::CompanyPhone buildTDSPhone(
        const std::string& countryCode, const std::string& regionCode,
        const std::string& number, const std::string& ext
    ) {
        NSpravTDS::CompanyPhone result;
        result.set_country_code(countryCode.c_str());
        result.set_region_code(regionCode.c_str());
        result.set_number(number.c_str());
        result.set_ext(ext.c_str());
        result.set_type(NSpravTDS::CompanyPhone::Phone);
        result.set_hide(false);
        return result;
    }

    NSpravTDS::Company buildCompany(std::vector<NSpravTDS::CompanyPhone> phones) {
        NSpravTDS::Company result;
        *result.mutable_phones() = {phones.begin(), phones.end()};
        return result;
    }
};

TEST_F(PhoneChangeBuilderTest, Creation) {
    proto::Request request1 = buildRequest(1, buildChanges({
        buildPhone(
            NSprav::Action::ACTUALIZE,
            "7", "812", "3334455", "12"
        ),
    }));

    proto::Request request2 = buildRequest(2, buildChanges({
        buildPhone(
            NSprav::Action::ACTUALIZE,
            "7", "812", "3334455", "12"
        ),
        buildPhone(
            NSprav::Action::ACTUALIZE,
            "7", "812", "3334455", "15"
        ),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSPhone("7", "812", "1111111", "")});

    std::vector<AttributeChanges<NSpravTDS::CompanyPhone>> expected = {
        {buildTDSPhone("7", "812", "1111111", ""), {}, false},
        {
            {},
            {{
                buildTDSPhone("7", "812", "3334455", "15"),
                {2},
                NSprav::Action::NONE
            }},
            false
        },
        {
            {},
            {{
                buildTDSPhone("7", "812", "3334455", "12"),
                {1, 2},
                NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::CompanyPhone>> result = changeBuilder.apply({request1, request2}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

TEST_F(PhoneChangeBuilderTest, Delete) {
    proto::Request request = buildRequest(1, buildChanges({
        buildPhone(
            NSprav::Action::DELETE,
            "7", "812", "1111111", ""
        ),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSPhone("7", "812", "1111111", "")});

    std::vector<AttributeChanges<NSpravTDS::CompanyPhone>> expected = {
        {
            buildTDSPhone("7", "812", "1111111", ""),
            {},
            true
        },
    };

    std::vector<AttributeChanges<NSpravTDS::CompanyPhone>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

} // maps::sprav::callcenter::company_changes::tests
