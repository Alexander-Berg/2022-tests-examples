#include "common.h"
#include "pretty_print.h"

#include <maps/sprav/callcenter/libs/company_changes/rubric.h>

#include <library/cpp/testing/gtest/gtest.h>


namespace maps::sprav::callcenter::company_changes::tests {

class RubricChangeBuilderTest : public testing::Test {
public:
    RubricChangeBuilder changeBuilder;

    NSprav::RubricId buildRubric(
        NSprav::Action action, uint64_t id,
        std::optional<NSprav::Rubric::Priority> priority = std::nullopt,
        const std::vector<NSprav::Rubric::ExportTarget>& targets = {}
    ) {
        NSprav::RubricId result;
        result.set_action(action);
        result.set_id(id);
        if (priority.has_value()) {
            result.set_priority(priority.value());
        }
        for (const auto& target : targets) {
            result.add_export_target(target);
        }
        return result;
    }

    NSprav::Company buildChanges(std::vector<NSprav::RubricId> rubrics) {
        NSprav::Company result;
        *result.mutable_rubrics() = {rubrics.begin(), rubrics.end()};
        return result;
    }

    NSpravTDS::CompanyRubric buildTDSRubric(uint64_t id, bool isMain, bool exportToSnippet) {
        NSpravTDS::CompanyRubric result;
        result.set_rubric_id(id);
        result.set_is_main(isMain);
        result.set_export_to_snippet(exportToSnippet);
        return result;
    }

    NSpravTDS::Company buildCompany(std::vector<NSpravTDS::CompanyRubric> rubrics) {
        NSpravTDS::Company result;
        *result.mutable_rubrics() = {rubrics.begin(), rubrics.end()};
        return result;
    }
};

TEST_F(RubricChangeBuilderTest, Test) {
    proto::Request request1 = buildRequest(1, buildChanges({
        buildRubric(NSprav::Action::DELETE, 1),
        buildRubric(NSprav::Action::ACTUALIZE, 2, NSprav::Rubric::MAIN),
        buildRubric(NSprav::Action::DELETE, 3),
        buildRubric(NSprav::Action::ACTUALIZE, 3, NSprav::Rubric::MAIN),
        buildRubric(NSprav::Action::ACTUALIZE, 5, NSprav::Rubric::MAIN),
    }));

    proto::Request request2 = buildRequest(2, buildChanges({
        buildRubric(NSprav::Action::ACTUALIZE, 1, std::nullopt, {NSprav::Rubric::SNIPPET}),
        buildRubric(NSprav::Action::DELETE, 4),
    }));

    NSpravTDS::Company company = buildCompany({
        buildTDSRubric(1, false, false),
        buildTDSRubric(3, false, false),
        buildTDSRubric(4, false, false),
    });

    std::vector<AttributeChanges<NSpravTDS::CompanyRubric>> expected = {
        {
            buildTDSRubric(1, false, false),
            {{
                buildTDSRubric(1, false, true),
                {2},
                NSprav::Action::NONE
            }},
            true
        },
        {
            buildTDSRubric(3, false, false),
            {{
                buildTDSRubric(3, true, false),
                {1},
                NSprav::Action::NONE
            }},
            false
        },
        {
            buildTDSRubric(4, false, false),
            {},
            true
        },
        {
            {},
            {{
                buildTDSRubric(2, true, false),
                {1},
                NSprav::Action::NONE
            }},
            false
        },
        {
            {},
            {{
                buildTDSRubric(5, true, false),
                {1},
                NSprav::Action::NONE
            }},
            false
        },
    };

    auto result = changeBuilder.apply({request1, request2}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

} // maps::sprav::callcenter::company_changes::tests
