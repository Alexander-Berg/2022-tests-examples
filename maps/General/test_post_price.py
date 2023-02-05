import decimal
import rstr
import random

from data_types.price_item import (
    PriceItem,
    generate_prices,
    generate_prices_in_database,
    EXTERNAL_ID_ALLOWED_SYMBOLS,
)
from data_types.price_overwrite import PriceOverwrite
from data_types.company import Company
from data_types.chain import Chain
from lib.server import server
from lib.random import printable_str


def test_post_big_description_price(user, company):
    description = (
        "Трезфазная мойка с нано-шампунем и покрытие кузова керамическим составом, \n"
        "химчистка кузова, \n"
        "очистка хромированных элемента кузова, \n"
        "уборка салона, \n"
        "легкая химчистка, \n"
        "уборка багажника, \n"
        "чернение, \n"
        "полировка салона, \n"
        "обработка кондиционером всего са"
    )
    assert len(description) == 250

    price_item = PriceItem(description=description)
    server.post_price(user, price_item, company) >> 200


def test_post_price(user, organization):
    price_item = PriceItem(group={'name': 'elephants'})
    response_json = server.post_price(user, price_item, organization) >> 200
    result_price_item = PriceItem.from_json(response_json)

    assert price_item == result_price_item
    assert result_price_item.id
    assert result_price_item.group['id']
    assert result_price_item.group['name'] == 'Elephants'
    assert result_price_item.moderation['status'] == 'OnModeration'


def test_post_item_with_text_price_value(user, organization):
    price_item1 = PriceItem()
    price_item1.price['value'] = 'Some text value'
    assert(price_item1.price['type'] != 'Other')
    server.post_price(user, price_item1, organization) >> 422

    price_item2 = PriceItem()
    price_item2.price['type'] = 'Other'
    price_item2.price['value'] = 'Some text value'
    server.post_price(user, price_item2, organization) >> 422

    price_item2.saveToDb(organization)
    price_item2.price['value'] = 42.5
    server.edit_price(user, price_item2, organization) >> 422


def test_edit_item_with_text_price_value(user, organization):
    price_item = PriceItem()
    price_item.price['type'] = 'Other'
    price_item.price['value'] = 'Some text value'

    price_item.saveToDb(organization)

    price_item.title = printable_str(5, 20)
    server.edit_price(user, price_item, organization) >> 200


def test_edit_price(user, organization):
    price_item = PriceItem()
    response_json = server.post_price(user, price_item, organization) >> 200

    price_item = PriceItem.from_json(response_json)
    price_item.title = printable_str(5, 20)

    response_json = server.edit_price(user, price_item, organization) >> 200
    edited_price = PriceItem.from_json(response_json)

    assert edited_price == price_item


def test_edit_with_no_changes_price_has_same_result(user, organization):
    price_item = PriceItem()
    response_json = server.post_price(user, price_item, organization) >> 200
    price_item = PriceItem.from_json(response_json)

    response_json = server.edit_price(user, price_item, organization) >> 200
    edited_price = PriceItem.from_json(response_json)

    assert edited_price == price_item


def test_edit_with_no_changes_price_with_invalid_fields(user, organization):
    invalid_field_value = 'a' * 5000

    generated_item = generate_prices_in_database(
        organization,
        count=1,
        title=invalid_field_value,
        description=invalid_field_value,
        external_id=invalid_field_value)[0]

    generated_item.is_popular = not generated_item.is_popular
    response_json = server.edit_price(user, generated_item, organization) >> 200
    edited_price = PriceItem.from_json(response_json)
    assert edited_price.title == invalid_field_value
    assert edited_price.description == invalid_field_value
    assert edited_price.external_id == invalid_field_value


def test_edit_deleted_price(user, organization):
    price_item = PriceItem()
    response_json = server.post_price(user, price_item, organization) >> 200

    price_item = PriceItem.from_json(response_json)
    server.delete_prices(user, organization, [price_item.id]) >> 200
    price_item.title = printable_str(5, 20)

    server.edit_price(user, price_item, organization) >> 422


def test_post_price_empty_group_fails(user, organization):
    price_item = PriceItem(group='')
    server.post_price(user, price_item, organization) >> 422


def test_post_price_empty_title_fails(user, organization):
    price_item = PriceItem(title='')
    server.post_price(user, price_item, organization) >> 422


def test_post_price_zero_price_fails(user, organization):
    price_item = PriceItem(price={
        'type': 'ExactNumber',
        'currency': 'RUB',
        'value': 0.0,
    })
    server.post_price(user, price_item, organization) >> 422


def test_post_price_negative_price_fails(user, organization):
    price_item = PriceItem(price={
        'type': 'ExactNumber',
        'currency': 'RUB',
        'value': -(random.random() * 10000),
    })
    server.post_price(user, price_item, organization) >> 422


def test_post_price_for_missing_company_fails(user, organization_type):
    if organization_type == "company":
        invalid_organization = Company()
    else:
        invalid_organization = Chain()
    price_item = PriceItem()
    server.post_price(user, price_item, invalid_organization) >> 403


def test_edit_missing_price_fails(user, organization):
    price_item = PriceItem(id=random.randint(1, 1000))
    server.edit_price(user, price_item, organization) >> 422


def test_edit_as_create_fails(user, organization):
    price_item = PriceItem(id=0)
    server.edit_price(user, price_item, organization) >> 422


def test_post_price_with_invalid_currency_fails(user, organization):
    price_item = PriceItem()
    price_item.price['currency'] = 'ZWL'
    server.post_price(user, price_item, organization) >> 422


def test_post_price_invalid_json_fails(user, organization):
    price_item = PriceItem()
    price_item.price = {}
    server.post_price(user, price_item, organization) >> 422


def test_post_price_limits_value_precision(user, organization):
    price_item = PriceItem()
    price_item.price['value'] = random.uniform(100, 1_000_000)

    response_json = server.post_price(
        user,
        price_item,
        organization,
        use_decimal=True) >> 200
    result_price_item = PriceItem.from_json(response_json)

    original_price = decimal.Decimal(price_item.price['value'])
    result_price = result_price_item.price['value']
    assert result_price == round(original_price, 2), "Price value was not rounded"


def test_post_price_same_external_id(user, company):
    external_id = rstr.rstr(EXTERNAL_ID_ALLOWED_SYMBOLS, 10)
    first_price = PriceItem(external_id=external_id)
    second_price = PriceItem(external_id=external_id)

    first_price = PriceItem.from_json(server.post_price(user, first_price, company) >> 200)
    second_price = PriceItem.from_json(server.post_price(user, second_price, company) >> 200)
    assert first_price.id == second_price.id


def test_post_price_same_external_id_deleted(user, company):
    external_id = rstr.rstr(EXTERNAL_ID_ALLOWED_SYMBOLS, 10)
    first_price = PriceItem(external_id=external_id)
    second_price = PriceItem(external_id=external_id)

    first_price = PriceItem.from_json(server.post_price(user, first_price, company) >> 200)
    server.delete_prices(user, company, price_ids=[first_price.id]) >> 200

    second_price = PriceItem.from_json(server.post_price(user, second_price, company) >> 200)
    assert first_price.id == second_price.id


def test_post_price_same_external_id_other_company(user, company, alien_user, alien_company):
    external_id = rstr.rstr(EXTERNAL_ID_ALLOWED_SYMBOLS, 10)
    first_price = PriceItem(external_id=external_id)
    first_price = PriceItem.from_json(server.post_price(user, first_price, company) >> 200)

    second_price = PriceItem(external_id=external_id)
    second_price = PriceItem.from_json(server.post_price(alien_user, second_price, alien_company) >> 200)

    assert first_price.id != second_price.id


def test_try_edit_external_id(user, company):
    price = PriceItem()
    price = PriceItem.from_json(server.post_price(user, price, company) >> 200)

    price.external_id = rstr.rstr(EXTERNAL_ID_ALLOWED_SYMBOLS, 10)
    server.edit_price_raw_data(user, price.id, price.to_json(patch=False), company) >> 422


def test_cant_create_more_popular_prices_than_allowed(user, organization):
    generate_prices(
        user=user,
        organization=organization,
        count=PriceItem.MAX_POPULAR_COUNT,
        is_popular=True)

    price = PriceItem(is_popular=True)
    server.post_price(user, price, organization) >> 422


def test_can_create_new_unpopular_price(user, company):
    generate_prices(
        user=user,
        organization=company,
        count=PriceItem.MAX_POPULAR_COUNT,
        is_popular=True)

    price = PriceItem(is_popular=False)
    price = PriceItem.from_json(server.post_price(user, price, company) >> 200)

    # test can't make unpopular price popular when there are many popular prices already
    price.is_popular = True
    server.edit_price(user, price, company) >> 422


def test_can_edit_popular_price_when_there_are_lots_of_them(user, company):
    popular_prices = generate_prices(
        user=user,
        organization=company,
        count=PriceItem.MAX_POPULAR_COUNT,
        is_popular=True)

    popular_price = popular_prices[0]
    popular_price.is_hidden = not popular_price.is_hidden
    server.edit_price(user, popular_price, company) >> 200


def test_can_create_new_popular_price_after_one_of_them_is_deleted(user, company):
    popular_prices = generate_prices(
        user=user,
        organization=company,
        count=PriceItem.MAX_POPULAR_COUNT,
        is_popular=True)
    server.delete_prices(user, company, price_ids=[popular_prices[0].id]) >> 200

    price = PriceItem(is_popular=True)
    server.post_price(user, price, company) >> 200


def test_cant_edit_feed_price(user, company, feeds_prices):
    price = feeds_prices[0]
    price.is_hidden = not price.is_hidden
    server.edit_price(user, price, company) >> 422


def test_popular_feeds_prices_dont_interfere_with_api_prices(user, company, feeds_companies):
    feed_company = next(feed for feed in feeds_companies.values())[0]
    generate_prices_in_database(
        company,
        count=PriceItem.MAX_POPULAR_COUNT,
        source=feed_company.source,
        feed_company_id=feed_company.id,
        is_hidden=False,
        is_popular=True)

    price = PriceItem(is_popular=True)
    price = PriceItem.from_json(server.post_price(user, price, company) >> 200)


def test_post_price_with_overwrites_count(user, organization):
    price = PriceItem()
    price = PriceItem.from_json(server.post_price(user, price, organization) >> 200)
    assert price.overwrites_count is None

    price = PriceItem.from_json(server.edit_price(user, price, organization) >> 200)

    if organization.organization_type != 'chain':
        assert price.overwrites_count is None
        return

    assert price.overwrites_count == 0
    chain_company = Company(parent_chain=organization)
    chain_company.register(user=user)
    chain_company.saveToDb()
    overwrites = [PriceOverwrite.create_overwrite(price, 'is_hidden')]
    server.edit_chain_prices(user, overwrites, chain_company) >> 200
    price = PriceItem.from_json(server.edit_price(user, price, organization) >> 200)
    assert price.overwrites_count == 1
