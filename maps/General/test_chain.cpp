#include "common.h"
#include "pretty_print.h"

#include <maps/sprav/callcenter/libs/company_changes/chain.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::sprav::callcenter::company_changes::tests {

class ChainChangeBuilderTest : public testing::Test {
public:
    ChainChangeBuilder changeBuilder;

    NSprav::ChainId buildChain(NSprav::Action action, uint64_t id) {
        NSprav::ChainId result;
        result.set_action(action);
        result.set_id(id);
        return result;
    }

    NSprav::Company buildChanges(std::vector<NSprav::ChainId> chains) {
        NSprav::Company result;
        *result.mutable_chain_ids() = {chains.begin(), chains.end()};
        return result;
    }

    NSpravTDS::CompanyParent buildTDSChain(uint64_t id) {
        NSpravTDS::CompanyParent result;
        result.set_company_id(id);
        result.set_type(NSpravTDS::CompanyParent::IsPartOf);
        return result;
    }

    NSpravTDS::Company buildCompany(std::vector<NSpravTDS::CompanyParent> chains) {
        NSpravTDS::Company result;
        *result.mutable_parent_companies() = {chains.begin(), chains.end()};
        return result;
    }
};

TEST_F(ChainChangeBuilderTest, Change) {
    proto::Request request = buildRequest(1, buildChanges({
        buildChain(NSprav::Action::DELETE, 1),
        buildChain(NSprav::Action::ACTUALIZE, 2),
    }));


    NSpravTDS::Company company = buildCompany({buildTDSChain(1)});

    std::vector<AttributeChanges<NSpravTDS::CompanyParent>> expected = {
        {
            buildTDSChain(1),
            {},
            true
        },
        {
            {},
            {{
                buildTDSChain(2),
                {1},
                NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::CompanyParent>> result = changeBuilder.apply({request}, company);
    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

TEST_F(ChainChangeBuilderTest, ChangeEmpty) {
    proto::Request request = buildRequest(1, buildChanges({
        buildChain(NSprav::Action::DELETE, 1),
        buildChain(NSprav::Action::ACTUALIZE, 2),
    }));


    NSpravTDS::Company company = buildCompany({});

    std::vector<AttributeChanges<NSpravTDS::CompanyParent>> expected = {
        {
            {},
            {{
                buildTDSChain(2),
                {1},
                NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::CompanyParent>> result = changeBuilder.apply({request}, company);
    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

} // maps::sprav::callcenter::company_changes::tests
