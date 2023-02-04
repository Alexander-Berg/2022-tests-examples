#include <maps/infopoint/tools/schools_miner/lib/rubric_tree.h>
#include <maps/libs/common/include/exception.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace maps::infopoint::schools_miner;

Y_UNIT_TEST_SUITE(RubricTreeSuite) {

Y_UNIT_TEST(ChildrenTree)
{
    const auto table = buildChildrenTable({
        {1, NO_RUBRIC_ID},
        {2, 1},
        {3, 1},
        {4, 2},
        {5, 2},
        {6, 4}
    });
    const std::unordered_map<RubricId, std::vector<RubricId>> expected = {
        {1, {2, 3}},
        {2, {4, 5}},
        {3, {}},
        {4, {6}},
        {5, {}},
        {6, {}}
    };
    UNIT_ASSERT_EQUAL(table, expected);

    {
        const auto descendants1 = getRubricAndItsDescendants(table, 1);
        UNIT_ASSERT_EQUAL(descendants1, (std::unordered_set<RubricId>{1, 2, 3, 4, 5, 6}));
    }
    {
        const auto descendants2 = getRubricAndItsDescendants(table, 2);
        UNIT_ASSERT_EQUAL(descendants2, (std::unordered_set<RubricId>{2, 4, 5, 6}));
    }
    {
        const auto descendants3 = getRubricAndItsDescendants(table, 3);
        UNIT_ASSERT_EQUAL(descendants3, (std::unordered_set<RubricId>{3}));
    }
    {
        const auto descendants3 = getRubricAndItsDescendants(table, 4);
        UNIT_ASSERT_EQUAL(descendants3, (std::unordered_set<RubricId>{4, 6}));
    }
    {
        const auto descendants3 = getRubricAndItsDescendants(table, 6);
        UNIT_ASSERT_EQUAL(descendants3, (std::unordered_set<RubricId>{6}));
    }
}

Y_UNIT_TEST(EmptyRubrics)
{
    UNIT_CHECK_GENERATED_EXCEPTION(buildChildrenTable({{}}), maps::Exception);
    const auto table = buildChildrenTable({});
    UNIT_ASSERT(table.empty());
    UNIT_CHECK_GENERATED_EXCEPTION(getRubricAndItsDescendants(table, 1), maps::Exception);
}

Y_UNIT_TEST(Cycle)
{
    {
        const auto table = buildChildrenTable({
            {1, 1}
        });
        UNIT_CHECK_GENERATED_EXCEPTION(getRubricAndItsDescendants(table, 1), maps::Exception);
    }

    {
        const auto table = buildChildrenTable({
            {1, 2},
            {2, 1}
        });
        UNIT_CHECK_GENERATED_EXCEPTION(getRubricAndItsDescendants(table, 1), maps::Exception);
    }
}

}
