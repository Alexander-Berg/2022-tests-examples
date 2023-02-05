#include "maps/goods/lib/goods_db/entities/price.h"
#include "maps/goods/lib/limits/limits.h"
#include "maps/libs/common/include/exception.h"
#include "maps/libs/json/include/value.h"
#include <library/cpp/testing/unittest/registar.h>

#include <maps/goods/lib/goods_db/entities/price_list.h>

using namespace maps::goods;

constexpr auto ITEM_BODY_TO_TEST = R"(
{
    "external_info": {
        "item_id": "qe123ad"
    },
    "title": "Мужская стрижка",
    "description": "Стрижем ножницами и машинкой",
    "group": {
        "name": "Новая категория"
    },
    "price": {
        "type": "ExactNumber",
        "currency": "RUB",
        "value": 123.20
    },
    "photos": [
        "123",
        "321"
    ],
    "is_popular": true,
    "availability": {
        "status": "OutOfStock"
    },
    "is_hidden": false
}
)";

Y_UNIT_TEST_SUITE(test_complete_price_list_item) {

CompletePriceListItem getItem()
{
    auto json = maps::json::Value::fromString(ITEM_BODY_TO_TEST);
    return CompletePriceListItem::fromJson(json);
}

Y_UNIT_TEST(check_validate_item_texts_length)
{
    auto item = getItem();
    item.priceListItem.title = std::string(limits::MAX_TITLE_LENGTH + 1, 'a');
    UNIT_ASSERT_EXCEPTION(item.validateUserInputAndCompare(std::nullopt), maps::Exception);

    item = getItem();
    item.priceListItem.externalId = std::string(limits::MAX_EXTERNAL_ID_LENGTH + 1, 'a');
    UNIT_ASSERT_EXCEPTION(item.validateUserInputAndCompare(std::nullopt), maps::Exception);

    item = getItem();
    item.priceListItem.description = std::string(limits::MAX_DESCRIPTION_LENGTH + 1, 'a');
    UNIT_ASSERT_EXCEPTION(item.validateUserInputAndCompare(std::nullopt), maps::Exception);

    item = getItem();
    item.priceListItem.marketUrl = std::string(limits::MAX_URL_LENGTH + 1, 'a');
    UNIT_ASSERT_EXCEPTION(item.validateUserInputAndCompare(std::nullopt), maps::Exception);

    item = getItem();
    item.priceListItem.price.value = std::string(limits::MAX_PRICE_STR_LENGTH + 1, '1');
    UNIT_ASSERT_EXCEPTION(item.validateUserInputAndCompare(std::nullopt), maps::Exception);

    item = getItem();
    item.group->setName(std::string(limits::MAX_GROUP_LENGTH + 1, '1'));
    UNIT_ASSERT_EXCEPTION(item.validateUserInputAndCompare(std::nullopt), maps::Exception);
}

Y_UNIT_TEST(check_no_validate_not_changed_item_texts_length)
{
    auto item = getItem();
    item.priceListItem.title = std::string(limits::MAX_TITLE_LENGTH + 1, 'a');
    UNIT_ASSERT_NO_EXCEPTION(item.validateUserInputAndCompare(item));

    item = getItem();
    item.priceListItem.externalId = std::string(limits::MAX_EXTERNAL_ID_LENGTH + 1, 'a');
    UNIT_ASSERT_NO_EXCEPTION(item.validateUserInputAndCompare(item));

    item = getItem();
    item.priceListItem.description = std::string(limits::MAX_DESCRIPTION_LENGTH + 1, 'a');
    UNIT_ASSERT_NO_EXCEPTION(item.validateUserInputAndCompare(item));

    item = getItem();
    item.priceListItem.price.value = std::string(limits::MAX_PRICE_STR_LENGTH + 1, '1');
    UNIT_ASSERT_NO_EXCEPTION(item.validateUserInputAndCompare(item));

    item = getItem();
    item.group->setName(std::string(limits::MAX_GROUP_LENGTH + 1, '1'));
    UNIT_ASSERT_NO_EXCEPTION(item.validateUserInputAndCompare(item));
}

Y_UNIT_TEST(check_item_is_changed)
{
    auto item = getItem();

    UNIT_ASSERT(item.validateUserInputAndCompare(std::nullopt));

    item = getItem();
    UNIT_ASSERT(!item.validateUserInputAndCompare(item));

    item = getItem();
    auto changedItem = item;
    changedItem.priceListItem.externalId = std::nullopt;
    UNIT_ASSERT(!changedItem.validateUserInputAndCompare(item));

    item = getItem();
    changedItem = item;
    changedItem.priceListItem.title.pop_back();
    UNIT_ASSERT(changedItem.validateUserInputAndCompare(item));

    item = getItem();
    changedItem = item;
    changedItem.priceListItem.description->pop_back();
    UNIT_ASSERT(changedItem.validateUserInputAndCompare(item));

    item = getItem();
    changedItem = item;
    auto changedGroupName = changedItem.group->getName();
    changedGroupName.pop_back();
    changedItem.group->setName(changedGroupName);
    UNIT_ASSERT(changedItem.validateUserInputAndCompare(item));

    item = getItem();
    changedItem = item;
    changedItem.photos->pop_back();
    UNIT_ASSERT(changedItem.validateUserInputAndCompare(item));

    item = getItem();
    changedItem = item;
    changedItem.priceListItem.price.currency = std::nullopt;
    UNIT_ASSERT(changedItem.validateUserInputAndCompare(item));

    item = getItem();
    changedItem = item;
    changedItem.priceListItem.price.value.pop_back();
    UNIT_ASSERT(changedItem.validateUserInputAndCompare(item));

    item = getItem();
    changedItem = item;
    changedItem.priceListItem.isHidden = !changedItem.priceListItem.isHidden;
    UNIT_ASSERT(changedItem.validateUserInputAndCompare(item));

    item = getItem();
    changedItem = item;
    changedItem.priceListItem.isPopular = !changedItem.priceListItem.isPopular;
    UNIT_ASSERT(changedItem.validateUserInputAndCompare(item));

    item = getItem();
    changedItem = item;
    changedItem.priceListItem.availabilityStatus = Availability::Available;
    UNIT_ASSERT(changedItem.validateUserInputAndCompare(item));
}

}
