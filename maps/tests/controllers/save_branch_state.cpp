#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(save_branch_state)
{
WIKI_FIXTURE_TEST_CASE(test_save_branch_state, EditorTestFixture)
{
    auto formatter = Formatter::create(
        common::FormatType::XML,
        make_unique<TestFormatterContext>());

    using namespace revision;

    for (auto state : { BranchState::Normal,
                        BranchState::Normal,
                        BranchState::Progress,
                        BranchState::Progress,
                        BranchState::Normal,
                        BranchState::Unavailable,
                        BranchState::Unavailable,
                        BranchState::Normal })
    {
        const auto noObservers = makeObservers<>();
        SaveBranchState controller(
            noObservers,
            {TESTS_USER, state, TRUNK_BRANCH_ID}
        );
        UNIT_ASSERT_NO_EXCEPTION((*formatter)(*controller()));
    }
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
