#include <maps/wikimap/mapspro/libs/social/tests/helpers/event_creator.h>
#include "categories.h"
#include "helpers.h"
#include "bounds.h"

#include <maps/libs/common/include/exception.h>
#include <maps/libs/geolib/include/serialization.h>
#include <yandex/maps/wiki/social/common.h>
#include <yandex/maps/wiki/social/gateway.h>
#include <yandex/maps/wiki/social/i_feed.h>
#include <yandex/maps/wiki/social/region_feed.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <pqxx/pqxx>
#include <string>
#include <vector>

namespace maps::wiki::social::tests {

namespace {

const uint64_t TRUNK_BRANCH_ID = 0;

void checkFeedPage(
    const std::pair<Events, HasMore>& feedPage,
    HasMore hasMore,
    size_t eventsCount,
    TId frontEventId,
    TId backEventId)
{
    UNIT_ASSERT(feedPage.second == hasMore);
    UNIT_ASSERT_EQUAL(feedPage.first.size(), eventsCount);
    UNIT_ASSERT_EQUAL(feedPage.first.front().id(), frontEventId);
    UNIT_ASSERT_EQUAL(feedPage.first.back().id(), backEventId);
}

} // namespace

Y_UNIT_TEST_SUITE(region_feed_tests_suite) {

Y_UNIT_TEST_F(feed_within_geom, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    auto ev1 = EventCreator(txn).action("action").bounds(BOUNDS_1).create();
    auto ev2 = EventCreator(txn).action("action").bounds(BOUNDS_2).create();

    {
        // check with region containing ev1 but not ev2
        const auto& region = REGION_CONTAINING_BOUNDS_1;
        std::stringstream regionWkb;
        geolib3::WKB::write(region, regionWkb);

        auto feed =
            gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());

        auto feedEvents = feed->eventsHead(10);
        UNIT_ASSERT(feedEvents.second == HasMore::No);
        UNIT_ASSERT_EQUAL(feedEvents.first.size(), 1);
        UNIT_ASSERT_EQUAL(feedEvents.first.front().id(), ev1.id());
    }

    {
        // check with region containing ev2 but not ev1
        const auto& region = REGION_CONTAINING_BOUNDS_2;
        std::stringstream regionWkb;
        geolib3::WKB::write(region, regionWkb);
        auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());

        auto feedEvents = feed->eventsHead(10);
        UNIT_ASSERT(feedEvents.second == HasMore::No);
        UNIT_ASSERT_EQUAL(feedEvents.first.size(), 1);
        UNIT_ASSERT_EQUAL(feedEvents.first.front().id(), ev2.id());
    }
}

Y_UNIT_TEST_F(feed_within_geom_eventsHead_not_full, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    const size_t limit = 10;
    const size_t eventsCount = limit - 1;

    // Generate series of commits: <eventsCount> in bounds_1.
    std::vector<Event> eventsInBounds;
    for (size_t i = 0; i < eventsCount; ++i) {
        eventsInBounds.push_back(
            EventCreator(txn).action("action").bounds(BOUNDS_1)
        );
    }
    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);

    auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());
    auto feedEvents = feed->eventsHead(limit);

    size_t expectedFrontId = eventsInBounds.at(eventsCount - 1).id();
    size_t expectedBackId = eventsInBounds.at(0).id();

    checkFeedPage(
        feedEvents, HasMore::No, limit - 1, expectedFrontId, expectedBackId);
}

Y_UNIT_TEST_F(feed_within_geom_eventsHead_full, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    const size_t limit = 10;
    const size_t eventsCount = limit;

    // Generate series of commits: <eventsCount> in bounds_1.
    std::vector<Event> eventsInBounds;
    for (size_t i = 0; i < eventsCount; ++i) {
        eventsInBounds.push_back(
            EventCreator(txn).action("action").bounds(BOUNDS_1)
        );
    }
    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);

    auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());
    auto feedEvents = feed->eventsHead(limit);

    size_t expectedFrontId = eventsInBounds.back().id();
    size_t expectedBackId = eventsInBounds.front().id();

    checkFeedPage(
        feedEvents, HasMore::No, limit, expectedFrontId, expectedBackId);
}

Y_UNIT_TEST_F(feed_within_geom_eventsHead_has_more, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    const size_t limit = 10;
    const size_t eventsCount = limit + 1;

    // Generate series of commits: <eventsCount> in bounds_1.
    std::vector<Event> eventsInBounds;
    for (size_t i = 0; i < eventsCount; ++i) {
        eventsInBounds.push_back(
            EventCreator(txn).action("action").bounds(BOUNDS_1)
        );
    }
    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);

    auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());
    auto feedEvents = feed->eventsHead(limit);

    size_t expectedFrontId = eventsInBounds.back().id();
    size_t expectedBackId = eventsInBounds.at(1).id();

    checkFeedPage(
        feedEvents, HasMore::Yes, limit, expectedFrontId, expectedBackId);
}

Y_UNIT_TEST_F(feed_within_geom_eventsBefore_last_page_not_full, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    const size_t limit = 10;
    const size_t lastPageEventsCount = limit - 1;
    const size_t fullPagesCount = 1;
    const size_t feedPagesCount = fullPagesCount + 1;
    const size_t eventsCount =
        fullPagesCount * limit + lastPageEventsCount;

    // Generate series of commits: <eventsCount> in each bounds.
    std::vector<Event> eventsInBounds;
    for (size_t i = 0; i < eventsCount; ++i) {
        eventsInBounds.push_back(
            EventCreator(txn).action("action").bounds(BOUNDS_1)
        );
    }
    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);

    auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());
    auto feedEvents = feed->eventsHead(limit);
    // Query the next page up to and including the last page.
    for (size_t i = 1; i < feedPagesCount; ++i) {
        feedEvents = feed->eventsBefore(feedEvents.first.back().id(), limit);
    }

    size_t expectedFrontId = eventsInBounds.at(lastPageEventsCount - 1).id();
    size_t expectedBackId = eventsInBounds.at(0).id();
    checkFeedPage(
        feedEvents,
        HasMore::No,
        lastPageEventsCount,
        expectedFrontId,
        expectedBackId);
}

Y_UNIT_TEST_F(feed_within_geom_eventsBefore_last_page_full, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    const size_t limit = 10;
    const size_t fullPagesCount = 2;
    const size_t eventsCount = fullPagesCount * limit;

    // Generate series of commits: <eventsCount> in bounds_1.
    std::vector<Event> eventsInBounds;
    for (size_t i = 0; i < eventsCount; ++i) {
        eventsInBounds.push_back(
            EventCreator(txn).action("action").bounds(BOUNDS_1)
        );
    }
    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);

    auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());
    auto feedEvents = feed->eventsHead(limit);
    // Query the next page up to and including the last page.
    for (size_t i = 1; i < fullPagesCount; ++i) {
        feedEvents = feed->eventsBefore(feedEvents.first.back().id(), limit);
    }

    size_t expectedFrontId = eventsInBounds.at(limit - 1).id();
    size_t expectedBackId = eventsInBounds.at(0).id();

    checkFeedPage(
        feedEvents, HasMore::No, limit, expectedFrontId, expectedBackId);
}

Y_UNIT_TEST_F(feed_within_geom_eventsBefore_scroll_test, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    const size_t limit = 10;
    const size_t fullPagesCount = 2;
    const size_t lastPageEventsCount = 1;
    const size_t feedPagesCount = fullPagesCount + 1;
    const size_t eventsCount =
        fullPagesCount * limit + lastPageEventsCount;

    // Generate series of commits: <eventsCount> in bounds_1.
    std::vector<Event> eventsInBounds;
    for (size_t i = 0; i < eventsCount; ++i) {
        eventsInBounds.push_back(
            EventCreator(txn).action("action").bounds(BOUNDS_1)
        );
    }
    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);

    auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());
    // Query the first page.
    auto feedEvents = feed->eventsHead(limit);
    // Query the next page up to and NOT including the last one.
    for (size_t i = 1; i < feedPagesCount - 1; ++i) {
        feedEvents = feed->eventsBefore(feedEvents.first.back().id(), limit);
    }

    // Check last page.
    size_t expectedFrontId = eventsInBounds.at(limit).id();
    size_t expectedBackId = eventsInBounds.at(1).id();

    checkFeedPage(
        feedEvents, HasMore::Yes, limit, expectedFrontId, expectedBackId);
}

Y_UNIT_TEST_F(feed_within_geom_eventsAfter_last_page_not_full, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);

    const size_t limit = 10;
    const size_t lastPageEventsCount = limit - 1;
    const size_t fullPagesCount = 1;
    const size_t feedPagesCount = fullPagesCount + 1;
    const size_t eventsCount =
        fullPagesCount * limit + lastPageEventsCount;

    // Generate series of commits: <eventsCount> for each bounds.
    std::vector<Event> eventsInBounds;
    for (size_t i = 0; i < eventsCount; ++i) {
        eventsInBounds.push_back(
            EventCreator(txn).action("action").bounds(BOUNDS_1)
        );
    }
    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);

    auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());
    auto feedEvents = feed->eventsAfter(0, limit);
    // Query the next page up to and including the last page.
    for (size_t i = 1; i < feedPagesCount; ++i) {
        feedEvents = feed->eventsAfter(feedEvents.first.front().id(), limit);
    }
    // Check last page.
    size_t expectedFrontId = eventsInBounds.back().id();
    size_t expectedBackId =
        eventsInBounds.at(eventsCount - lastPageEventsCount).id();
    checkFeedPage(
        feedEvents,
        HasMore::No,
        lastPageEventsCount,
        expectedFrontId,
        expectedBackId);
}

Y_UNIT_TEST_F(feed_within_geom_eventsAfter_last_page_full, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    const size_t limit = 10;
    const size_t fullPagesCount = 2;
    const size_t eventsCount = fullPagesCount * limit;

    // Generate series of commits: <eventsCount> inside bounds_1.
    std::vector<Event> eventsInBounds;
    for (size_t i = 0; i < eventsCount; ++i) {
        eventsInBounds.push_back(
            EventCreator(txn).action("action").bounds(BOUNDS_1)
        );
    }
    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);

    auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());
    auto feedEvents = feed->eventsAfter(0, limit);
    // Query the next page up to and including the last page.
    for (size_t i = 1; i < fullPagesCount; ++i) {
        feedEvents = feed->eventsAfter(feedEvents.first.front().id(), limit);
    }

    size_t expectedFrontId = eventsInBounds.back().id();
    size_t expectedBackId = eventsInBounds.at(eventsCount - limit).id();
    checkFeedPage(
        feedEvents, HasMore::No, limit, expectedFrontId, expectedBackId);
}

Y_UNIT_TEST_F(feed_within_geom_eventsAfter_prelast_page, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);

    const size_t limit = 10;
    const size_t fullPagesCount = 2;
    const size_t eventsCount = fullPagesCount * limit + 1;

    // Generate series of commits: <eventsCount> inside bounds_1.
    std::vector<Event> eventsInBounds;
    for (size_t i = 0; i < eventsCount; ++i) {
        eventsInBounds.push_back(
            EventCreator(txn).action("action").bounds(BOUNDS_1)
        );
    }
    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);

    auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());
    auto feedEvents = feed->eventsAfter(0, limit);
    // Query the next page up to and including the last page.
    for (size_t i = 1; i < fullPagesCount; ++i) {
        feedEvents = feed->eventsAfter(feedEvents.first.front().id(), limit);
    }

    size_t expectedBackId =
        eventsInBounds.at((fullPagesCount - 1) * limit).id();
    size_t expectedFrontId =
        eventsInBounds.at(fullPagesCount * limit - 1).id();

    checkFeedPage(
        feedEvents, HasMore::Yes, limit, expectedFrontId, expectedBackId);
}

Y_UNIT_TEST_F(feed_within_geom_no_events_inside, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);

    size_t limit = 10;
    const size_t eventsCount = 100;
    size_t maxEventId = 0;
    // Generate series of commits: <eventsCount> only in BOUNDS_2.
    for (size_t i = 0; i < eventsCount; ++i) {
        auto ev = EventCreator(txn).action("action").bounds(BOUNDS_2).create();
        if (ev.id() > maxEventId) {
            maxEventId = ev.id();
        }
    }

    // check with region not covering bounds.
    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);
    auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());

    {
        auto feedEvents = feed->eventsHead(limit);
        UNIT_ASSERT_EQUAL(feedEvents.first.size(), 0);
        UNIT_ASSERT(feedEvents.second == HasMore::No);
    }

    {
        auto feedEvents = feed->eventsBefore(maxEventId + 1, limit);
        UNIT_ASSERT_EQUAL(feedEvents.first.size(), 0);
        UNIT_ASSERT(feedEvents.second == HasMore::No);
    }

    {
        auto feedEvents = feed->eventsAfter(0, limit);
        UNIT_ASSERT_EQUAL(feedEvents.first.size(), 0);
        UNIT_ASSERT(feedEvents.second == HasMore::No);
    }
}

Y_UNIT_TEST_F(region_feed_default_categories_filter, DbFixture)
{
    using namespace feed_categories;
    const auto categories = getSomeCategoryIds();

    pqxx::work txn(conn);
    Gateway gw(txn);
    createOneEventPerCategory(txn, categories, BOUNDS_1);

    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);

    // Check excluded categories.
    auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());

    const auto& [filterCategories, policy] = feed->categoryIdsFilter();

    UNIT_ASSERT_EQUAL(InclusionPolicy::Excluding, policy);
    UNIT_ASSERT_EQUAL(filterCategories.size(), 0);
}

Y_UNIT_TEST_F(region_feed_exclude_categories, DbFixture)
{
    using namespace feed_categories;
    const auto categories = getSomeCategoryIds();

    pqxx::work txn(conn);
    Gateway gw(txn);
    createOneEventPerCategory(txn, categories, BOUNDS_1);

    size_t chosenCategoriesNum = categories.size() / 2;
    ASSERT(chosenCategoriesNum > 0);
    social::CategoryIdsSet remainingCategories;
    social::CategoryIdsSet excludedCategories;
    size_t i = 0;
    for (const auto& cat : categories) {
        if (i < chosenCategoriesNum) {
            excludedCategories.insert(cat);
        } else {
            remainingCategories.insert(cat);
        }
        ++i;
    }

    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);

    // Check excluded categories.
    auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());
    feed->setCategoryIdsFilter({excludedCategories, InclusionPolicy::Excluding});

    const auto& [filterCategories, policy] = feed->categoryIdsFilter();

    UNIT_ASSERT_EQUAL(InclusionPolicy::Excluding, policy);
    UNIT_ASSERT(filterCategories == excludedCategories);

    checkFeedCategories(*feed, excludedCategories, InclusionPolicy::Excluding);
    checkFeedCategories(*feed, remainingCategories, InclusionPolicy::Including);
}

Y_UNIT_TEST_F(region_feed_exclude_no_categories, DbFixture)
{
    using namespace feed_categories;
    const auto categories = getSomeCategoryIds();

    pqxx::work txn(conn);
    Gateway gw(txn);
    createOneEventPerCategory(txn, categories, BOUNDS_1);

    const social::CategoryIdsSet& remainingCategories = categories;
    const social::CategoryIdsSet excludedCategories = {};

    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);

    // Check excluded categories.
    auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());
    feed->setCategoryIdsFilter({excludedCategories, InclusionPolicy::Excluding});

    const auto& [filterCategories, policy] = feed->categoryIdsFilter();

    UNIT_ASSERT_EQUAL(InclusionPolicy::Excluding, policy);
    UNIT_ASSERT(filterCategories.empty());

    checkFeedCategories(*feed, remainingCategories, InclusionPolicy::Including);
}

Y_UNIT_TEST_F(region_feed_include_no_categories, DbFixture)
{
    using namespace feed_categories;
    const auto categories = getSomeCategoryIds();

    pqxx::work txn(conn);
    Gateway gw(txn);
    createOneEventPerCategory(txn, categories, BOUNDS_1);

    const social::CategoryIdsSet includedCategories = {};

    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);

    // Check included categories.
    auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());
    feed->setCategoryIdsFilter({includedCategories, InclusionPolicy::Including});

    const auto& [filterCategories, policy] = feed->categoryIdsFilter();

    UNIT_ASSERT_EQUAL(InclusionPolicy::Including, policy);
    UNIT_ASSERT(filterCategories.empty());

    const auto& [events, hasMore] = feed->eventsHead(categories.size());
    UNIT_ASSERT_EQUAL(hasMore, HasMore::No);
    UNIT_ASSERT(events.empty());
}

Y_UNIT_TEST_F(region_feed_include_categories, DbFixture)
{
    using namespace feed_categories;
    const auto categories = getSomeCategoryIds();

    pqxx::work txn(conn);
    Gateway gw(txn);
    createOneEventPerCategory(txn, categories, BOUNDS_1);

    size_t chosenCategoriesNum = categories.size() / 2;
    ASSERT(chosenCategoriesNum > 0);
    social::CategoryIdsSet remainingCategories;
    social::CategoryIdsSet includedCategories;
    size_t i = 0;
    for (const auto& cat : categories) {
        if (i < chosenCategoriesNum) {
            includedCategories.insert(cat);
        } else {
            remainingCategories.insert(cat);
        }
        ++i;
    }

    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);

    // Check excluded categories.
    auto feed = gw.regionFeed(TRUNK_BRANCH_ID, regionWkb.str());
    feed->setCategoryIdsFilter({includedCategories, InclusionPolicy::Including});

    const auto& [filterCategories, policy] = feed->categoryIdsFilter();

    UNIT_ASSERT_EQUAL(InclusionPolicy::Including, policy);
    UNIT_ASSERT(filterCategories == includedCategories);

    checkFeedCategories(*feed, includedCategories, InclusionPolicy::Including);
    checkFeedCategories(*feed, remainingCategories, InclusionPolicy::Excluding);
}

Y_UNIT_TEST_F(region_feed_exclude_created_by_uids, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gw(txn);
    TUids uids{1, 2, 3, 4, 5};
    for (auto uid : uids) {
        EventCreator(txn).action("action").bounds(BOUNDS_1).uid(uid).create();
    }

    TUids skippedUids{1, 2, 5};

    const auto& region = REGION_CONTAINING_BOUNDS_1;
    std::stringstream regionWkb;
    geolib3::WKB::write(region, regionWkb);

    // Check excluded categories.
    auto feed = RegionFeed(txn, TRUNK_BRANCH_ID, regionWkb.str());

    feed.setSkippedUids(skippedUids);

    auto [events, hasMore] =  feed.eventsHead(uids.size());
    UNIT_ASSERT_EQUAL(hasMore, HasMore::No);
    UNIT_ASSERT_EQUAL(events.size(), 2);
    UNIT_ASSERT_EQUAL(events.front().createdBy(), 4);
    UNIT_ASSERT_EQUAL(events.back().createdBy(), 3);
}

}

} // namespace maps::wiki::social::tests
