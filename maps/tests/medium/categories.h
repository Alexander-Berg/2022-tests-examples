#pragma once
#include <yandex/maps/wiki/social/common.h>
#include <yandex/maps/wiki/social/i_feed.h>

#include <optional>
#include <string>
#include <vector>

namespace maps::wiki::social::tests::feed_categories {

void checkFeedCategories(
    const IFeed& iFeed,
    const CategoryIdsSet& categories,
    InclusionPolicy categoriesInclusionPolicy
);

CategoryIdsSet getSomeCategoryIds();

void createOneEventPerCategory(
    pqxx::transaction_base& txn,
    const CategoryIdsSet& categories,
    std::optional<std::string> bounds
);

void createOneEventPerCategory(
    pqxx::transaction_base& txn,
    const CategoryIdsSet& categories
);

} // namespace maps::wiki::social::tests::feed_categories
