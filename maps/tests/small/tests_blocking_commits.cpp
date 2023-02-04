#include <maps/wikimap/mapspro/libs/revision_meta/impl/blocking_commits_impl.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::revision_meta::tests {

namespace {
const TCommitIds NO_COMMITS_WITH_ACTIVE_TASKS;
const TCommitIds NO_PREAPPROVED_COMMITS;
const Relations NO_RELATIONS;
} // namespace

Y_UNIT_TEST_SUITE(get_all_components) {
    Y_UNIT_TEST(should_split_empty_relations) {
        UNIT_ASSERT(
            impl::getAllComponents(
                NO_PREAPPROVED_COMMITS,
                NO_RELATIONS
            ).empty()
        );
    }

    Y_UNIT_TEST(should_get_components) {
        const TCommitIds preApprovedCommits = {
            1, 2, 7
        };
        const Relations relations = {
            {10, 13}, {14, 17}, {17, 10},
            {25, 23}, {23, 28},
            {33, 34}
        };
        auto components = impl::getAllComponents(preApprovedCommits, relations);

        std::sort(
            components.begin(),
            components.end(),
            [](const auto& lhs, const auto& rhs) {
                return *lhs.cbegin() < *rhs.cbegin();
            }
        );

        UNIT_ASSERT_EQUAL(
            components,
            Graph<TCommitId>::Components({{1}, {2}, {7}, {10, 13, 14, 17}, {23, 25, 28}, {33, 34}})
        );
    }
} // Y_UNIT_TEST_SUITE(get_all_components)

Y_UNIT_TEST_SUITE(get_component) {
    Y_UNIT_TEST(should_get_empty_component_if_components_empty) {
        UNIT_ASSERT(impl::getComponent({}, 42).empty());
    }

    Y_UNIT_TEST(should_get_empty_component_if_commit_absent) {
        const Graph<TCommitId>::Components components({
            {10, 11, 12}, {25, 24, 23}, {36, 37, 38}
        });

        UNIT_ASSERT(
            impl::getComponent(components, 49).empty()
        );
    }

    Y_UNIT_TEST(should_get_component) {
        const Graph<TCommitId>::Components components({
            {10, 11, 12}, {25, 24, 23}, {36, 37, 38}
        });

        UNIT_ASSERT_EQUAL(
            impl::getComponent(components, 10),
            Graph<TCommitId>::Component({10, 11, 12})
        );
        UNIT_ASSERT_EQUAL(
            impl::getComponent(components, 24),
            Graph<TCommitId>::Component({23, 24, 25})
        );
        UNIT_ASSERT_EQUAL(
            impl::getComponent(components, 38),
            Graph<TCommitId>::Component({36, 37, 38})
        );
        UNIT_ASSERT(impl::getComponent(components, 49).empty());
    }
} // Y_UNIT_TEST_SUITE(get_component)


Y_UNIT_TEST_SUITE(get_blocking_commits) {
    Y_UNIT_TEST(should_not_get_commits_from_empty_graph) {
        UNIT_ASSERT(
            impl::getBlockingCommits(
                2,
                NO_PREAPPROVED_COMMITS,
                NO_RELATIONS,
                NO_COMMITS_WITH_ACTIVE_TASKS
            ).empty()
        );
        UNIT_ASSERT(
            impl::getBlockingCommits(
                2,
                NO_PREAPPROVED_COMMITS,
                NO_RELATIONS,
                {1, 2, 3}
            ).empty()
        );
    }

    Y_UNIT_TEST(should_not_get_commits_if_no_active_tasks) {
        UNIT_ASSERT(
            impl::getBlockingCommits(
                2,
                {1, 2, 3},
                {{2, 3}, {3, 4}},
                NO_COMMITS_WITH_ACTIVE_TASKS
            ).empty()
        );
    }

    Y_UNIT_TEST(should_not_get_commits_if_active_tasks_block_nothing) {
        UNIT_ASSERT(
            impl::getBlockingCommits(
                2,
                {1, 2, 3},
                {{2, 3}, {3, 4}},
                {6, 7}
            ).empty()
        );
    }

    Y_UNIT_TEST(should_not_get_commits_if_active_tasks_block_another_component) {
        UNIT_ASSERT(
            impl::getBlockingCommits(
                2,
                {1, 2, 3, 5, 6},
                {{2, 3}, {3, 4}, {5, 6}, {6, 7}, {5, 8}},
                {6, 7}
            ).empty()
        );
    }

    Y_UNIT_TEST(should_not_get_commits_if_commit_blocks_itself) {
        // The following cases rather synthetic. A commit with an active task
        // has not been enqueued into pre-approved queue yet. Therefore, this
        // commit must not be a subject of getting its blocking commits.

        UNIT_ASSERT(
            impl::getBlockingCommits(
                2,
                {1, 2},
                {{1, 2}, {2, 3}},
                {2}
            ).empty()
        );

        UNIT_ASSERT_EQUAL(
            impl::getBlockingCommits(
                1,
                {1, 2},
                {{1, 2}, {2, 3}},
                {1, 3}
            ),
            TCommitIds({3})
        );

        UNIT_ASSERT(
            impl::getBlockingCommits(
                4,
                {1, 2},
                {{1, 2}, {2, 3}},
                {4}
            ).empty()
        );
    }

    Y_UNIT_TEST(should_not_get_commits_if_commit_has_no_relations) {
        // The commit to be checked is in pre-approved queue, however there are
        // no draft commits contributing to it nor commits created by the same
        // service task.

        UNIT_ASSERT(
            impl::getBlockingCommits(
                4,
                {1, 2},
                {{1, 2}, {2, 3}},
                {}
            ).empty()
        );

        UNIT_ASSERT(
            impl::getBlockingCommits(
                4,
                {1, 2},
                {{1, 2}, {2, 3}},
                {5, 6}
            ).empty()
        );
    }

    Y_UNIT_TEST(should_get_blocking_commits) {
        UNIT_ASSERT_EQUAL(
            impl::getBlockingCommits(
                2,
                {1, 3},
                {{1, 2}, {3, 4}},
                {1, 4}
            ),
            TCommitIds({1})
        );

        UNIT_ASSERT_EQUAL(
            impl::getBlockingCommits(
                3,
                {1, 3},
                {{1, 2}, {3, 4}},
                {1, 4}
            ),
            TCommitIds({4})
        );
    }

    Y_UNIT_TEST(should_get_chained_blocking_commits) {
        UNIT_ASSERT_EQUAL(
            impl::getBlockingCommits(
                3,
                {1, 2, 3, 4, 5, 10},
                {{1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {10, 20}},
                {1, 6, 10, 20}
            ),
            TCommitIds({1, 6})
        );
    }
} // Y_UNIT_TEST_SUITE(get_blocking_commits)

Y_UNIT_TEST_SUITE(get_ready_for_approve) {
    Y_UNIT_TEST(should_not_get_commits_if_components_empty) {
        UNIT_ASSERT(
            impl::getCommitsReadyForApprove(
                NO_PREAPPROVED_COMMITS,
                NO_RELATIONS,
                NO_COMMITS_WITH_ACTIVE_TASKS
            ).empty()
        );

        UNIT_ASSERT(
            impl::getCommitsReadyForApprove(
                NO_PREAPPROVED_COMMITS,
                NO_RELATIONS,
                {1, 2, 3}
            ).empty()
        );
    }

    Y_UNIT_TEST(should_get_all_commits_if_no_active_tasks) {
        UNIT_ASSERT_EQUAL(
            impl::getCommitsReadyForApprove(
                {1, 2, 4, 10},
                {{1, 2}, {2, 3}, {4, 5}, {4, 6}},
                NO_COMMITS_WITH_ACTIVE_TASKS
            ),
            TCommitIds({1, 2, 3, 4, 5, 6, 10})
        );
    }

    Y_UNIT_TEST(should_get_all_commits_if_active_tasks_unrelated) {
        UNIT_ASSERT_EQUAL(
            impl::getCommitsReadyForApprove(
                {1, 2, 4, 10},
                {{1, 2}, {2, 3}, {4, 5}, {4, 6}},
                {100, 200, 300}
            ),
            TCommitIds({1, 2, 3, 4, 5, 6, 10})
        );
    }

    Y_UNIT_TEST(should_get_commits_from_different_components) {
        UNIT_ASSERT_EQUAL(
            impl::getCommitsReadyForApprove(
                {1, 2, 4, 6, 8, 10},
                {{1, 2}, {2, 3}, {4, 5}, {6, 7}, {8, 7}},
                {5}
            ),
            TCommitIds({1, 2, 3, 6, 7, 8, 10})
        );
    }

    Y_UNIT_TEST(should_not_get_commits_if_all_components_blocked) {
        UNIT_ASSERT(
            impl::getCommitsReadyForApprove(
                {1, 2, 4, 6, 8, 10},
                {{1, 2}, {2, 3}, {4, 5}, {6, 7}, {8, 7}},
                {1, 5, 8, 10}
            ).empty()
        );
    }
} // Y_UNIT_TEST_SUITE(get_ready_for_approve)

} // namespace maps::wiki::revision_meta::tests
