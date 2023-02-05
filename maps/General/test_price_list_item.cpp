#include <library/cpp/testing/unittest/registar.h>

#include <maps/goods/lib/goods_db/entities/price_list_item.h>

using namespace maps::goods;


Y_UNIT_TEST_SUITE(test_price_list_item) {

Y_UNIT_TEST(test_base_price_item_correctly_initializes_primitive_typed_fields)
{
    BasePriceItem item;
    UNIT_ASSERT_VALUES_EQUAL(item.id, 0);
    UNIT_ASSERT_VALUES_EQUAL(item.organizationId, 0);
    UNIT_ASSERT(!item.isHidden);
    UNIT_ASSERT(!item.isPopular);
    UNIT_ASSERT_VALUES_EQUAL(item.availabilityStatus, Availability::Available);
}

Y_UNIT_TEST(test_price_list_item_correctly_initializes_primitive_typed_fields)
{
    PriceListItem item;
    UNIT_ASSERT_VALUES_EQUAL(item.status, ItemStatus::OnModeration);
}

}
