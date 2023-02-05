#include "common.h"
#include "pretty_print.h"

#include <maps/sprav/callcenter/libs/company_changes/publishing_status.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::sprav::callcenter::company_changes::tests {

class PublishingStatusChangeBuilderTest : public testing::Test {
public:
    PublishingStatusChangeBuilder changeBuilder;

    NSprav::Company buildChanges(NSprav::PublishingStatus publishingStatus) {
        NSprav::Company result;
        result.set_publishing_status(publishingStatus);
        return result;
    }


    NSpravTDS::Company buildCompany(NSpravTDS::Company::PublishingStatus publishingStatus) {
        NSpravTDS::Company result;
        result.set_publishing_status(publishingStatus);
        return result;
    }
};

TEST_F(PublishingStatusChangeBuilderTest, HasChange) {
    proto::Request request = buildRequest(1, buildChanges(NSprav::CLOSED));

    NSpravTDS::Company company = buildCompany(NSpravTDS::Company::Publish);

    std::vector<AttributeChanges<NSpravTDS::Company::PublishingStatus>> expected = {
        {
            NSpravTDS::Company::Publish,
            {{
                NSpravTDS::Company::Closed,
                {1},
                NSprav::Action::NONE
            }},
            false
        },
    };

    auto result = changeBuilder.apply({request}, company);
    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

TEST_F(PublishingStatusChangeBuilderTest, NoChange) {
    proto::Request request = buildRequest(1, NSprav::Company());

    NSpravTDS::Company company = buildCompany(NSpravTDS::Company::Publish);

    std::vector<AttributeChanges<NSpravTDS::Company::PublishingStatus>> expected = {
        {
            NSpravTDS::Company::Publish,
            {},
            false
        },
    };

    auto result = changeBuilder.apply({request}, company);
    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

} // maps::sprav::callcenter::company_changes::tests
