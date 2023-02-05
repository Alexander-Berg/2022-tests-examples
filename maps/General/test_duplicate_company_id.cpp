#include "common.h"
#include "pretty_print.h"

#include <maps/sprav/callcenter/libs/company_changes/duplicate_company_id.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::sprav::callcenter::company_changes::tests {

class DuplicateCompanyIdChangeBuilderTest : public testing::Test {
public:
    DuplicateCompanyIdChangeBuilder changeBuilder;

    NSprav::PermanentId buildDuplicateCompanyId(NSprav::Action action, int64_t value) {
        NSprav::PermanentId result;
        result.set_action(action);
        result.set_value(value);
        return result;
    }

    NSprav::Company buildChanges(const std::vector<NSprav::PermanentId>& duplicateCompanyId) {
        NSprav::Company result;
        *result.mutable_head_company_id() = {duplicateCompanyId.begin(), duplicateCompanyId.end()};
        return result;
    }

    NSpravTDS::Company buildCompany(uint64_t duplicateCompanyId) {
        NSpravTDS::Company result;
        result.set_duplicate_company_id(duplicateCompanyId);
        return result;
    }
};

TEST_F(DuplicateCompanyIdChangeBuilderTest, Change) {
    std::vector<proto::Request> requests = {
        buildRequest(1, buildChanges({
            buildDuplicateCompanyId(NSprav::Action::ACTUALIZE, 1),
            buildDuplicateCompanyId(NSprav::Action::ACTUALIZE, 2)
        })),
        buildRequest(2, buildChanges({
            buildDuplicateCompanyId(NSprav::Action::ACTUALIZE, 2),
            buildDuplicateCompanyId(NSprav::Action::ACTUALIZE, 3)
        })),
    };

    NSpravTDS::Company company = buildCompany(1);

    std::vector<AttributeChanges<int64_t>> expected = {
        {
            1,
            {{
                1,
                {1},
                NSprav::Action::NONE
            }, {
                2,
                {1, 2},
                NSprav::Action::NONE
            }, {
                3,
                {2},
                NSprav::Action::NONE
            }},
            false
        }
    };

    std::vector<AttributeChanges<int64_t>> result = changeBuilder.apply(requests, company);
    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

TEST_F(DuplicateCompanyIdChangeBuilderTest, Replace) {
    std::vector<proto::Request> requests = {
        buildRequest(1, buildChanges({
            buildDuplicateCompanyId(NSprav::Action::DELETE, 1),
            buildDuplicateCompanyId(NSprav::Action::ACTUALIZE, 2)
        })),
        buildRequest(2, buildChanges({
            buildDuplicateCompanyId(NSprav::Action::ACTUALIZE, 2),
            buildDuplicateCompanyId(NSprav::Action::ACTUALIZE, 3)
        })),
    };

    NSpravTDS::Company company = buildCompany(1);

    std::vector<AttributeChanges<int64_t>> expected = {
        {
            1,
            {{
                2,
                {1, 2},
                NSprav::Action::NONE
            }, {
                3,
                {2},
                NSprav::Action::NONE
            }},
            false
        }
    };

    std::vector<AttributeChanges<int64_t>> result = changeBuilder.apply(requests, company);
    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

TEST_F(DuplicateCompanyIdChangeBuilderTest, Delete) {
    std::vector<proto::Request> requests = {
        buildRequest(1, buildChanges({
            buildDuplicateCompanyId(NSprav::Action::DELETE, 1)
        })),
    };

    NSpravTDS::Company company = buildCompany(1);

    std::vector<AttributeChanges<int64_t>> expected = {
        {
            1,
            {},
            true
        }
    };

    std::vector<AttributeChanges<int64_t>> result = changeBuilder.apply(requests, company);
    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

TEST_F(DuplicateCompanyIdChangeBuilderTest, DeleteAndActualize) {
    std::vector<proto::Request> requests = {
        buildRequest(1, buildChanges({
            buildDuplicateCompanyId(NSprav::Action::DELETE, 1)
        })),
        buildRequest(2, buildChanges({
            buildDuplicateCompanyId(NSprav::Action::ACTUALIZE, 2),
            buildDuplicateCompanyId(NSprav::Action::ACTUALIZE, 3)
        })),
    };

    NSpravTDS::Company company = buildCompany(1);

    std::vector<AttributeChanges<int64_t>> expected = {
        {
            1,
            {{
                2,
                {2},
                NSprav::Action::NONE
            }, {
                3,
                {2},
                NSprav::Action::NONE
            }},
            true
        }
    };

    std::vector<AttributeChanges<int64_t>> result = changeBuilder.apply(requests, company);
    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

} // maps::sprav::callcenter::company_changes::tests
