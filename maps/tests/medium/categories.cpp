#include "categories.h"

#include <maps/wikimap/mapspro/libs/social/tests/helpers/event_creator.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::social::tests::feed_categories {

void checkFeedCategories(
    const IFeed& iFeed,
    const CategoryIdsSet& categories,
    InclusionPolicy categoriesInclusionPolicy)
{
    const auto catSize = categories.size();
    const auto [events, hasMore] = iFeed.eventsAfter(0, catSize);

    const long expectedOccurrenceCount =
        categoriesInclusionPolicy == InclusionPolicy::Including ? 1 : 0;

    for (const auto& ev: events) {
        UNIT_ASSERT_EQUAL(
            std::count(
                categories.begin(),
                categories.end(),
                ev.primaryObjectData()->categoryId()),
            expectedOccurrenceCount);
    }
};

CategoryIdsSet getSomeCategoryIds()
{
    return {"ad", // Some categories to test on.
            "ad_jc",
            "addr",
            "bld",
            "cond",
            "poi_edu",
            "rd_el",
            "rb_jc"};
}

void createOneEventPerCategory(
    pqxx::transaction_base& txn,
    const CategoryIdsSet& categories,
    std::optional<std::string> bounds
)
{
    const std::string screenLabel = "";
    const std::string editNotes = "";

    size_t i = 0;
    for (const auto& cat : categories) {
        const auto primaryObjData = PrimaryObjectData(i, cat, screenLabel, editNotes);

        auto eventCreator = EventCreator(txn).action("action").primaryObjData(primaryObjData);

        if (bounds) {
            eventCreator.bounds(*bounds);
        }

        eventCreator.create();
        ++i;
    }
}

void createOneEventPerCategory(
    pqxx::transaction_base& txn,
    const CategoryIdsSet& categories
)
{
    createOneEventPerCategory(txn, categories, std::nullopt);
}

} // namespace maps::wiki::social::tests::feed_categories
