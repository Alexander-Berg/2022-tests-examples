#include <maps/wikimap/mapspro/libs/revision_meta/impl/preapproved_commits_relations.h>

#include <maps/wikimap/mapspro/libs/revision_meta/tests/fixtures/fixtures.h>

#include <yandex/maps/wiki/unittest/arcadia.h>
#include <yandex/maps/wiki/unittest/query_helpers_arcadia.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace maps::wiki::unittest;

namespace maps::wiki::revision_meta::tests {

Y_UNIT_TEST_SUITE_F(preapproved_commits_relations, unittest::ArcadiaDbFixture) {

Y_UNIT_TEST(should_not_throw_when_add_empty_relation) {
    auto txn = pool().masterWriteableTransaction();

    const TCommitIds relatesToCommitIds = {};
    UNIT_ASSERT_NO_EXCEPTION(
        addCommitRelations(*txn, 1, relatesToCommitIds.cbegin(), relatesToCommitIds.cend())
    );

    UNIT_ASSERT(getAllRelations(*txn).empty());
}

Y_UNIT_TEST(should_add_several_relations_sequentially)
{
    auto txn = pool().masterWriteableTransaction();

    TCommitIds relatesToCommitIds = {2};
    addCommitRelations(*txn, 1, relatesToCommitIds.cbegin(), relatesToCommitIds.cend());

    relatesToCommitIds = {3};
    addCommitRelations(*txn, 1, relatesToCommitIds.cbegin(), relatesToCommitIds.cend());

    UNIT_ASSERT_EQUAL(
        getAllRelations(*txn),
        Relations({{1, 2}, {1, 3}})
    );
 }

Y_UNIT_TEST(should_add_several_relations_simultaneously) {
    auto txn = pool().masterWriteableTransaction();

    TCommitIds relatesToCommitIds = {2, 3};
    addCommitRelations(*txn, 1, relatesToCommitIds.cbegin(), relatesToCommitIds.cend());
    UNIT_ASSERT_EQUAL(
        getAllRelations(*txn),
        Relations({{1, 2}, {1, 3}})
    );
}

Y_UNIT_TEST(should_not_throw_adding_relation_twice) {
    auto txn = pool().masterWriteableTransaction();

    const TCommitIds relatesToCommitIds = {2};
    addCommitRelations(*txn, 1, relatesToCommitIds.cbegin(), relatesToCommitIds.cend());
    UNIT_ASSERT_NO_EXCEPTION(
        addCommitRelations(*txn, 1, relatesToCommitIds.cbegin(), relatesToCommitIds.cend())
    );

    UNIT_ASSERT_EQUAL(
        getAllRelations(*txn),
        Relations({{1, 2}})
    );
}

Y_UNIT_TEST(should_not_throw_when_delete_no_commits) {
    auto txn = pool().masterWriteableTransaction();

    UNIT_ASSERT_NO_EXCEPTION(
        deleteCommitRelations(*txn, {})
    );
}

Y_UNIT_TEST(should_not_throw_when_delete_unexisting_commit) {
    auto txn = pool().masterWriteableTransaction();

    helpers::addCommitRelations(*txn, {{1, 2}});

    UNIT_ASSERT_NO_EXCEPTION(
        deleteCommitRelations(*txn, {3})
    );

    UNIT_ASSERT_EQUAL(
        getAllRelations(*txn),
        Relations({{1, 2}})
    );
}

Y_UNIT_TEST(should_remove_relations_by_commit_id) {
    auto txn = pool().masterWriteableTransaction();

    helpers::addCommitRelations(*txn, {{1, 2}, {3, 4}, {5, 6}, {7, 8}});

    deleteCommitRelations(*txn, {3, 7});

    UNIT_ASSERT_EQUAL(
        getAllRelations(*txn),
        Relations({{1, 2}, {5, 6}})
    );
}

Y_UNIT_TEST(should_remove_relations_by_relates_to) {
    auto txn = pool().masterWriteableTransaction();

    helpers::addCommitRelations(*txn, {{1, 2}, {3, 4}, {5, 6}, {7, 8}});

    deleteCommitRelations(*txn, {2, 8});

    UNIT_ASSERT_EQUAL(
        getAllRelations(*txn),
        Relations({{3, 4}, {5, 6}})
    );
}

Y_UNIT_TEST(should_remove_relations_by_both_columns_simultaneously) {
    auto txn = pool().masterWriteableTransaction();

    helpers::addCommitRelations(*txn, {{1, 2}, {3, 4}, {5, 6}, {7, 8}});

    deleteCommitRelations(*txn, {3, 6});

    UNIT_ASSERT_EQUAL(
        getAllRelations(*txn),
        Relations({{1, 2}, {7, 8}})
    );
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::revision_meta::tests
