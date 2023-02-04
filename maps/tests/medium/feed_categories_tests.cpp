#include "categories.h"
#include "helpers.h"

#include <maps/libs/common/include/exception.h>
#include <yandex/maps/wiki/social/common.h>
#include <yandex/maps/wiki/social/gateway.h>
#include <yandex/maps/wiki/social/i_feed.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <pqxx/pqxx>
#include <string>
#include <vector>

namespace maps::wiki::social::tests::feed_categories {

Y_UNIT_TEST_SUITE(feed_categories_tests_suite) {

Y_UNIT_TEST_F(feed_categories_filter_set_in_constructor, DbFixture)
{
    const auto categories = getSomeCategoryIds();

    pqxx::work txn(conn);
    Gateway gw(txn);

    { // Default filter.
        auto feed = gw.feed(0, 0, FeedType::Suspicious, FeedFilter());
        const auto [filterCategories, policy] = feed.categoryIdsFilter();
        UNIT_ASSERT(filterCategories.empty());
        UNIT_ASSERT_EQUAL(policy, InclusionPolicy::Excluding);
    }
    { // Filter with chosenCategories included.
        FeedFilter filter;
        filter.categoryIds({categories, InclusionPolicy::Including});

        auto feed = gw.feed(0, 0, FeedType::Suspicious, filter);
        const auto [filterCategories, policy] = feed.categoryIdsFilter();

        UNIT_ASSERT(filterCategories == categories);
        UNIT_ASSERT_EQUAL(policy, InclusionPolicy::Including);
    }
    { // Filter with chosenCategories excluded.
        FeedFilter filter;
        filter.categoryIds({categories, InclusionPolicy::Excluding});

        auto feed = gw.feed(0, 0, FeedType::Suspicious, filter);
        const auto [filterCategories, policy] = feed.categoryIdsFilter();

        UNIT_ASSERT(filterCategories == categories);
        UNIT_ASSERT_EQUAL(policy, InclusionPolicy::Excluding);
    }
}

Y_UNIT_TEST_F(feed_include_categories, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    const auto categories = getSomeCategoryIds();
    createOneEventPerCategory(txn, categories);

    size_t chosenCategoriesNum = categories.size() / 2;
    const auto including = InclusionPolicy::Including;

    ASSERT(chosenCategoriesNum > 0);
    auto chosenEndIt = categories.begin();
    std::advance(chosenEndIt, chosenCategoriesNum);
    social::CategoryIdsSet chosenCategories = {categories.begin(), chosenEndIt};

    auto feed = gw.feed(0, 0, FeedType::Suspicious, FeedFilter());
    feed.setCategoryIdsFilter({chosenCategories, including});

    const auto& [filterCategories, policy] = feed.categoryIdsFilter();

    UNIT_ASSERT_EQUAL(policy, including);
    UNIT_ASSERT_EQUAL(filterCategories.size(), chosenCategories.size());
    UNIT_ASSERT(filterCategories == chosenCategories);

    checkFeedCategories(feed, chosenCategories, including);
}

Y_UNIT_TEST_F(feed_include_zero_categories, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    const auto categories = getSomeCategoryIds();
    createOneEventPerCategory(txn, categories);

    const social::CategoryIdsSet emptyChosenCategories = {};
    const auto including = InclusionPolicy::Including;

    auto feed = gw.feed(0, 0, FeedType::Suspicious, FeedFilter());
    feed.setCategoryIdsFilter({emptyChosenCategories, including});

    const auto& [filterCategories, policy] = feed.categoryIdsFilter();

    UNIT_ASSERT_EQUAL(policy, including);
    UNIT_ASSERT_EQUAL(filterCategories.size(), 0);
    UNIT_ASSERT(filterCategories == emptyChosenCategories);

    const auto& [events, hasMore] = feed.eventsHead(categories.size());
    UNIT_ASSERT(events.empty());
    UNIT_ASSERT(hasMore == HasMore::No);
}

Y_UNIT_TEST_F(feed_exclude_categories, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    const auto categories = getSomeCategoryIds();
    createOneEventPerCategory(txn, categories);

    size_t chosenCategoriesNum = categories.size() / 2;
    ASSERT(chosenCategoriesNum > 0);
    social::CategoryIdsSet remainingCategories;
    social::CategoryIdsSet excludedCategories;

    size_t i = 0;
    for (const auto& cat: categories) {
        if (i < chosenCategoriesNum) {
            remainingCategories.insert(cat);
        } else {
            excludedCategories.insert(cat);
        }
        ++i;
    }

    auto feed = gw.feed(0, 0, FeedType::Suspicious, FeedFilter());
    feed.setCategoryIdsFilter({excludedCategories, InclusionPolicy::Excluding});

    const auto& [filterCategories, policy] = feed.categoryIdsFilter();

    UNIT_ASSERT_EQUAL(policy, InclusionPolicy::Excluding);
    UNIT_ASSERT_EQUAL(filterCategories.size(), excludedCategories.size());
    UNIT_ASSERT(filterCategories == excludedCategories);

    checkFeedCategories(feed, excludedCategories, InclusionPolicy::Excluding);
    checkFeedCategories(feed, remainingCategories, InclusionPolicy::Including);
}

}

} // namespace maps::wiki::social::tests
