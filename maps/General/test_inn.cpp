#include "common.h"
#include "pretty_print.h"

#include <maps/sprav/callcenter/libs/company_changes/inn.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::sprav::callcenter::company_changes::tests {

class InnChangeBuilderTest : public testing::Test {
public:
    InnChangeBuilder changeBuilder;

    NSprav::Inn buildInn(NSprav::Action action, const std::string& id) {
        NSprav::Inn result;
        result.set_action(action);
        result.set_id(id.c_str());
        return result;
    }

    NSprav::Company buildChanges(const NSprav::Inn& inn) {
        NSprav::Company result;
        result.mutable_inn()->CopyFrom(inn);
        return result;
    }

    NSpravTDS::CompanyLegalInfo buildTDSLegalInfo(const std::string& inn) {
        NSpravTDS::CompanyLegalInfo result;
        result.set_inn(inn.c_str());
        return result;
    }

    NSpravTDS::Company buildCompany(const NSpravTDS::CompanyLegalInfo& legalInfo) {
        NSpravTDS::Company result;
        result.mutable_legal_info()->CopyFrom(legalInfo);
        return result;
    }
};

TEST_F(InnChangeBuilderTest, Change) {
    std::vector<proto::Request> requests = {
        buildRequest(2, buildChanges(buildInn(NSprav::Action::ACTUALIZE, "inn2"))),
        buildRequest(3, buildChanges(buildInn(NSprav::Action::ACTUALIZE, "inn3"))),
        buildRequest(4, buildChanges(buildInn(NSprav::Action::ACTUALIZE, "inn3"))),
    };

    NSpravTDS::Company company = buildCompany(buildTDSLegalInfo("inn1"));

    std::vector<AttributeChanges<std::string>> expected = {
        {
            "inn1",
            {{
                "inn2",
                {2},
                NSprav::Action::NONE
            }, {
                "inn3",
                {3, 4},
                NSprav::Action::NONE
            }},
            false
        }
    };

    std::vector<AttributeChanges<std::string>> result = changeBuilder.apply(requests, company);
    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

TEST_F(InnChangeBuilderTest, Delete) {
    std::vector<proto::Request> requests = {
        buildRequest(1, buildChanges(buildInn(NSprav::Action::DELETE, "inn1"))),
        buildRequest(3, buildChanges(buildInn(NSprav::Action::ACTUALIZE, "inn3"))),
        buildRequest(4, buildChanges(buildInn(NSprav::Action::ACTUALIZE, "inn3"))),
    };

    NSpravTDS::Company company = buildCompany(buildTDSLegalInfo("inn1"));

    std::vector<AttributeChanges<std::string>> expected = {
        {
            "inn1",
            {{
                "inn3",
                {3, 4},
                NSprav::Action::NONE
            }},
            true
        }
    };

    std::vector<AttributeChanges<std::string>> result = changeBuilder.apply(requests, company);
    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(InnChangeBuilderTest, DeleteAndActualize) {
    std::vector<proto::Request> requests = {
        buildRequest(1, buildChanges(buildInn(NSprav::Action::DELETE, "inn1"))),
        buildRequest(2, buildChanges(buildInn(NSprav::Action::ACTUALIZE, "inn1"))),
        buildRequest(3, buildChanges(buildInn(NSprav::Action::DELETE, "inn3"))),
    };

    NSpravTDS::Company company = buildCompany(buildTDSLegalInfo("inn1"));

    std::vector<AttributeChanges<std::string>> expected = {
        {
            "inn1",
            {{
                "inn1",
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
