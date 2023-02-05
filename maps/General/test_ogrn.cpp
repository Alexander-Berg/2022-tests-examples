#include "common.h"
#include "pretty_print.h"

#include <maps/sprav/callcenter/libs/company_changes/ogrn.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::sprav::callcenter::company_changes::tests {

class OgrnChangeBuilderTest : public testing::Test {
public:
    OgrnChangeBuilder changeBuilder;

    NSprav::Ogrn buildOgrn(NSprav::Action action, const std::string& id) {
        NSprav::Ogrn result;
        result.set_action(action);
        result.set_id(id.c_str());
        return result;
    }

    NSprav::Company buildChanges(const NSprav::Ogrn& ogrn) {
        NSprav::Company result;
        result.mutable_ogrn()->CopyFrom(ogrn);
        return result;
    }

    NSpravTDS::CompanyLegalInfo buildTDSLegalInfo(const std::string& ogrn) {
        NSpravTDS::CompanyLegalInfo result;
        result.set_ogrn(ogrn.c_str());
        return result;
    }

    NSpravTDS::Company buildCompany(const NSpravTDS::CompanyLegalInfo& legalInfo) {
        NSpravTDS::Company result;
        result.mutable_legal_info()->CopyFrom(legalInfo);
        return result;
    }
};

TEST_F(OgrnChangeBuilderTest, Change) {
    std::vector<proto::Request> requests = {
        buildRequest(2, buildChanges(buildOgrn(NSprav::Action::ACTUALIZE, "ogrn2"))),
        buildRequest(3, buildChanges(buildOgrn(NSprav::Action::ACTUALIZE, "ogrn3"))),
        buildRequest(4, buildChanges(buildOgrn(NSprav::Action::ACTUALIZE, "ogrn3"))),
    };

    NSpravTDS::Company company = buildCompany(buildTDSLegalInfo("ogrn1"));

    std::vector<AttributeChanges<std::string>> expected = {
        {
            "ogrn1",
            {{
                "ogrn2",
                {2},
                NSprav::Action::NONE
            }, {
                "ogrn3",
                {3, 4},
                NSprav::Action::NONE
            }},
            false
        }
    };

    std::vector<AttributeChanges<std::string>> result = changeBuilder.apply(requests, company);
    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

TEST_F(OgrnChangeBuilderTest, Delete) {
    std::vector<proto::Request> requests = {
        buildRequest(1, buildChanges(buildOgrn(NSprav::Action::DELETE, "ogrn1"))),
        buildRequest(3, buildChanges(buildOgrn(NSprav::Action::ACTUALIZE, "ogrn3"))),
        buildRequest(4, buildChanges(buildOgrn(NSprav::Action::ACTUALIZE, "ogrn3"))),
    };

    NSpravTDS::Company company = buildCompany(buildTDSLegalInfo("ogrn1"));

    std::vector<AttributeChanges<std::string>> expected = {
        {
            "ogrn1",
            {{
                "ogrn3",
                {3, 4},
                NSprav::Action::NONE
            }},
            true
        }
    };

    std::vector<AttributeChanges<std::string>> result = changeBuilder.apply(requests, company);
    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(OgrnChangeBuilderTest, DeleteAndActualize) {
    std::vector<proto::Request> requests = {
        buildRequest(1, buildChanges(buildOgrn(NSprav::Action::DELETE, "ogrn1"))),
        buildRequest(2, buildChanges(buildOgrn(NSprav::Action::ACTUALIZE, "ogrn1"))),
        buildRequest(3, buildChanges(buildOgrn(NSprav::Action::DELETE, "ogrn3"))),
    };

    NSpravTDS::Company company = buildCompany(buildTDSLegalInfo("ogrn1"));

    std::vector<AttributeChanges<std::string>> expected = {
        {
            "ogrn1",
            {{
                "ogrn1",
                {2},
                NSprav::Action::NONE
            }},
            true
        }
    };

    std::vector<AttributeChanges<std::string>> result = changeBuilder.apply(requests, company);
    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


} // maps::sprav::callcenter::company_changes::tests
