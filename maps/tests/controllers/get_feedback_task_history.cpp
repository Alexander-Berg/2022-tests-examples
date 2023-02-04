#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/wikimap/mapspro/services/editor/src/observers/feedback_task_binder.h>
#include <maps/wikimap/mapspro/services/editor/src/observers/social.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(get_feedback_task_history)
{
WIKI_FIXTURE_TEST_CASE(test_get_feedback_task_history_and_stat, EditorTestFixture)
{
    auto feedbackTask = addFeedbackTask();
    feedbackTask = acquireFeedbackTask(feedbackTask.id(), TESTS_USER);

    // to avoid trust level requirement in social observer
    setModerationRole(TESTS_USER, social::ModerationMode::Supervisor, 0);

    {
        auto jsonResultValue = performAndValidateJsonGetRequest<GetSocialFeedbackTaskStat>(
            TESTS_USER,
            feedbackTask.id(),
            /*dbToken=*/ ""
        );
        UNIT_ASSERT_EQUAL(jsonResultValue["commitsCount"].as<int>(), 0);
    }

    {
        performSaveObjectRequest("tests/data/create_simple_vegetation.json");
    }

    {
        TOid vegId{0};

        auto branchCtx = BranchContextFacade::acquireRead(0, "");
        ObjectsCache cache(branchCtx, boost::none);

        auto vedIds = objectIdsByCategory(cache, "vegetation");
        WIKI_TEST_REQUIRE_EQUAL(vedIds.size(), 1);
        vegId = *vedIds.begin();

        ObjectsUpdateState::Request deleteRequest(
            {TESTS_USER, {}},
            vegId,
            "deleted",
            0,
            "",
            common::FormatType::JSON,
            feedbackTask.id()
        );
        UNIT_ASSERT_NO_EXCEPTION(
            ObjectsUpdateState(
                makeObservers<FeedbackTaskBinder, SocialObserver>(),
                deleteRequest
            )()
        );
    }

    {
        auto jsonResultValue = performAndValidateJsonGetRequest<GetSocialFeedbackTaskHistory>(
            TESTS_USER,
            feedbackTask.id(),
            /*dbToken=*/ "",
            /*perPage=*/ static_cast<size_t>(10),
            /*beforeEventId=*/ boost::none,
            /*afterEventId=*/ boost::none
        );
        UNIT_ASSERT_EQUAL(jsonResultValue["events"].size(), 1);
    }
    {
        auto jsonResultValue = performAndValidateJsonGetRequest<GetSocialFeedbackTaskStat>(
            TESTS_USER,
            feedbackTask.id(),
            /*dbToken=*/ ""
        );
        UNIT_ASSERT_EQUAL(jsonResultValue["commitsCount"].as<int>(), 1);
    }

}

namespace {

void
checkPageFetching(
    const std::vector<int>& allItems,
    size_t perPage,
    const boost::optional<int>& anchor,
    const boost::optional<actions::AnchorDirection>& anchorDirection,
    const std::vector<int>& expectedItems,
    bool expectedHasMore)
{
    typedef std::function<bool (const int&)> Predicate;
    Predicate anchorPredicate;
    if (anchor) {
        anchorPredicate = [&](const int& i) {
            return i == *anchor;
        };
    }
    auto page = actions::fetchPage(allItems, perPage, anchorPredicate, anchorDirection);
    WIKI_TEST_EQUAL_COLLECTIONS(
        page.items.begin(),
        page.items.end(),
        expectedItems.begin(),
        expectedItems.end());
    UNIT_ASSERT_EQUAL(page.hasMore, expectedHasMore);

}

} // namespace

Y_UNIT_TEST(test_commit_ids_on_page)
{
    std::vector<int> allItems{1, 2, 3, 4, 5, 6, 7};

    checkPageFetching(allItems, 2, boost::none, boost::none, {1, 2}, true);
    checkPageFetching(allItems, 2, 4, actions::AnchorDirection::Before, {2, 3}, true);
    checkPageFetching(allItems, 2, 3, actions::AnchorDirection::Before, {1, 2}, false);
    checkPageFetching(allItems, 2, 2, actions::AnchorDirection::Before, {1}, false);
    checkPageFetching(allItems, 2, 4, actions::AnchorDirection::After, {5, 6}, true);
    checkPageFetching(allItems, 3, 4, actions::AnchorDirection::After, {5, 6, 7}, false);
    checkPageFetching(allItems, 4, 4, actions::AnchorDirection::After, {5, 6, 7}, false);
}

}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
