from data_types.price_item import PriceItem, generate_prices, MAX_POPULAR_ITEMS_COUNT
from data_types.price_overwrite import PriceOverwrite
from data_types.company import Company
from data_types.chain import Chain
from data_types.util import coalesce
from lib.server import server
import random
import pytest


def generate_chain_prices(user, chain_company, chain, count=None, field_to_overwrite='is_hidden'):
    generated_prices = generate_prices(user, chain, count=count)

    assert generated_prices
    edit_count = random.randint(1, max(len(generated_prices) // 2, 1))
    indices_to_edit = random.sample(range(len(generated_prices)), k=edit_count)
    overwrites = []
    for index in indices_to_edit:
        price = generated_prices[index]
        overwrite = PriceOverwrite.create_overwrite(price, field_to_overwrite)
        overwrites.append(overwrite)
    server.edit_chain_prices(user, overwrites, chain_company) >> 200

    return generated_prices


def test_get_chain_pricelist(user, chain_company, chain):
    generated_prices = generate_prices(user, chain)

    prices_json = server.get_prices(user, chain_company, is_chain=True) >> 200
    prices = PriceItem.list_from_json(prices_json)

    assert prices == generated_prices
    assert prices_json["popular_goods"]["popular_count"] == 0
    assert prices_json["popular_goods"]["max_popular_count"] == MAX_POPULAR_ITEMS_COUNT


def test_get_chain_pricelist_popular(user, chain_company, chain):
    popular_count = random.randint(1, MAX_POPULAR_ITEMS_COUNT)
    popular_prices = generate_prices(user, chain, is_popular=True, count=popular_count)

    prices_json = server.get_prices(user, chain_company, is_chain=True) >> 200
    prices = PriceItem.list_from_json(prices_json)

    assert prices == popular_prices
    assert prices_json["popular_goods"]["popular_count"] == popular_count
    assert prices_json["popular_goods"]["max_popular_count"] == MAX_POPULAR_ITEMS_COUNT


def test_get_chain_pricelist_limits_output(user, chain_company, chain):
    total_count = random.randint(21, 25)
    generated_prices = generate_prices(user, chain, count=total_count)

    response = server.get_prices(user, chain_company, is_chain=True) >> 200
    prices = PriceItem.list_from_json(response)

    DEFAULT_LIMIT = 20
    assert prices == generated_prices[:DEFAULT_LIMIT]
    assert response['pager']['total'] == total_count


def test_get_empty_chain_pricelist(user, chain_company, chain):
    prices = server.get_prices(user, chain_company, is_chain=True) >> 200
    assert len(prices['items']) == 0


@pytest.mark.parametrize("field_to_overwrite", ["is_hidden", "is_popular", "availability"])
def test_patch_chain_pricelist(user, chain_company, chain, field_to_overwrite):
    generated_prices = generate_chain_prices(user, chain_company, chain, field_to_overwrite=field_to_overwrite)
    expected_popular_count = sum(1 for price in generated_prices if price.is_popular)

    prices = server.get_prices(user, chain_company, is_chain=True) >> 200
    actual_popular_count = prices['popular_goods']['popular_count']
    prices = PriceItem.list_from_json(prices)

    assert prices == generated_prices
    assert actual_popular_count == expected_popular_count


@pytest.mark.parametrize("original_is_hidden", [False, True])
@pytest.mark.parametrize("edited_is_hidden", [False, True, None])
@pytest.mark.parametrize("original_price_value_idx", [0, 1])
@pytest.mark.parametrize("edited_price_value_idx", [1, None])
def test_get_chain_pricelist_search_single(
    user,
    chain_company,
    chain,
    original_is_hidden,
    edited_is_hidden,
    original_price_value_idx,
    edited_price_value_idx
):
    price_values = [
        round(random.uniform(100, 500_000), 2),
        round(random.uniform(500_001, 1_000_000), 2)
    ]

    def get_price_value(idx):
        return price_values[idx] if idx is not None else None

    original_price_value = get_price_value(original_price_value_idx)
    edited_price_value = get_price_value(edited_price_value_idx)
    price_item = PriceItem(
        is_hidden=original_is_hidden,
        price={
            'type': 'ExactNumber',
            'currency': 'RUB',
            'value': original_price_value,
        })
    price_item = PriceItem.from_json(server.post_price(user, price_item, chain) >> 200)

    if edited_is_hidden is not None or edited_price_value is not None:
        price_item.is_hidden = edited_is_hidden or False
        price_item.price['value'] = edited_price_value
        overwrite = PriceOverwrite(
            item_id=price_item.id,
            is_hidden=price_item.is_hidden,
            price=price_item.price,
        )
        server.edit_chain_prices(user, [overwrite], chain_company) >> 200

    expected_is_hidden = original_is_hidden or coalesce(edited_is_hidden, False)
    price_item.is_hidden = expected_is_hidden

    expected_price_value = coalesce(edited_price_value, original_price_value)
    price_item.price['value'] = expected_price_value

    response = server.get_prices(
        user,
        chain_company,
        is_hidden=expected_is_hidden,
        is_chain=True) >> 200

    pager = response['pager']
    assert pager['total'] == 1

    got_price = PriceItem.from_json(response['items'][0])
    assert price_item == got_price


@pytest.mark.parametrize("field_to_overwrite", [
    ("is_hidden", [False, True, None]),
    ("is_popular", [False, True, None]),
    ("availability", ['OutOfStock', 'Available', None]),
])
@pytest.mark.parametrize("filter_value_index", [0, 1, 2])
def test_get_chain_pricelist_search_multiple(user, chain_company, chain, field_to_overwrite, filter_value_index):
    attr_name, filter_values = field_to_overwrite
    filter_value = filter_values[filter_value_index]

    total_count = random.randint(15, 20)
    generated_prices = generate_chain_prices(
        user,
        chain_company,
        chain,
        count=total_count,
        field_to_overwrite=attr_name
    )

    filtered_prices = generated_prices
    filters = {}
    if filter_value is not None:
        if attr_name == 'availability':
            filter_name = 'availability_status'
            checked_value = {'status': filter_value}
        else:
            filter_name = attr_name
            checked_value = filter_value

        filters[filter_name] = filter_value
        filtered_prices = [price for price in filtered_prices if getattr(price, attr_name) == checked_value]
    filtered_count = len(filtered_prices)

    limit = random.randint(1, 10)
    offset = random.randint(0, max(0, filtered_count - limit))
    response = server.get_prices(
        user,
        chain_company,
        offset=offset,
        limit=limit,
        is_chain=True,
        **filters) >> 200

    pager = response['pager']
    assert pager['limit'] == limit
    assert pager['offset'] == offset
    assert pager['total'] == filtered_count

    prices = PriceItem.list_from_json(response)
    assert prices == filtered_prices[offset:offset+limit]


def test_get_chain_pricelist_no_chain(user, company):
    response = server.get_prices(user, company, is_chain=True) >> 200
    assert len(PriceItem.list_from_json(response)) == 0


def test_get_chain_pricelist_no_company(user):
    invalid_organization = Company()
    server.get_prices(user, invalid_organization, is_chain=True) >> 403


def test_patch_chain_pricelist_no_chain(user, company):
    price_item = PriceItem()
    price_item = PriceItem.from_json(server.post_price(user, price_item, company) >> 200)

    price_item.is_hidden = not price_item.is_hidden
    overwrite = PriceOverwrite(item_id=price_item.id, is_hidden=price_item.is_hidden)
    server.edit_chain_prices(user, [overwrite], company) >> 422


def test_patch_chain_pricelist_not_a_chain_pricelist(user, chain_company):
    price_item = PriceItem()
    price_item = PriceItem.from_json(server.post_price(user, price_item, chain_company) >> 200)

    price_item.is_hidden = not price_item.is_hidden
    overwrite = PriceOverwrite(item_id=price_item.id, is_hidden=price_item.is_hidden)
    server.edit_chain_prices(user, [overwrite], chain_company) >> 422


def test_patch_chain_pricelist_missing(user, chain_company):
    overwrite = PriceOverwrite(item_id=random.randint(1, 1_000_000), is_hidden=random.choice([True, False]))
    server.edit_chain_prices(user, [overwrite], chain_company) >> 422


def test_patch_chain_pricelist_deleted(user, chain_company, chain):
    price_item = PriceItem()
    price_item = PriceItem.from_json(server.post_price(user, price_item, chain) >> 200)

    server.delete_pricelist(user, chain) >> 200

    price_item.is_hidden = not price_item.is_hidden
    overwrite = PriceOverwrite(item_id=price_item.id, is_hidden=price_item.is_hidden)
    server.edit_chain_prices(user, [overwrite], chain_company) >> 422


def test_patch_chain_pricelist_other_chain(user, chain_company):
    other_chain = Chain.generate(user=user)
    price_item = PriceItem()
    price_item = PriceItem.from_json(server.post_price(user, price_item, other_chain) >> 200)

    price_item.is_hidden = not price_item.is_hidden
    overwrite = PriceOverwrite(item_id=price_item.id, is_hidden=price_item.is_hidden)
    server.edit_chain_prices(user, [overwrite], chain_company) >> 422


def test_patch_chain_pricelist_twice(user, chain_company, chain):
    price_item = PriceItem(is_hidden=False)
    price_item = PriceItem.from_json(server.post_price(user, price_item, chain) >> 200)

    price_item.is_hidden = True
    overwrite = PriceOverwrite(item_id=price_item.id, is_hidden=price_item.is_hidden)
    server.edit_chain_prices(user, [overwrite], chain_company) >> 200

    response = server.get_prices(
        user,
        chain_company,
        is_chain=True,
        is_hidden=False) >> 200
    assert len(response['items']) == 0

    response = server.get_prices(
        user,
        chain_company,
        is_chain=True,
        is_hidden=True) >> 200
    assert PriceItem.list_from_json(response) == [price_item]

    price_item.is_hidden = False
    overwrite = PriceOverwrite(item_id=price_item.id, is_hidden=price_item.is_hidden)
    server.edit_chain_prices(user, [overwrite], chain_company) >> 200

    response = server.get_prices(
        user,
        chain_company,
        is_chain=True,
        is_hidden=True) >> 200
    assert len(response['items']) == 0

    response = server.get_prices(
        user,
        chain_company,
        is_chain=True,
        is_hidden=False) >> 200
    assert PriceItem.list_from_json(response) == [price_item]


def test_get_chain_and_company_prices(user, chain, chain_company):
    generated_chain_prices = generate_prices(user, chain)
    generated_company_prices = generate_prices(user, chain_company)

    chain_prices_json = server.get_prices(user, chain_company, is_chain=True) >> 200
    chain_prices = PriceItem.list_from_json(chain_prices_json)
    assert chain_prices == generated_chain_prices
    for price in chain_prices:
        assert price.is_chain is True

    company_prices_json = server.get_prices(user, chain_company, is_chain=False) >> 200
    company_prices = PriceItem.list_from_json(company_prices_json)
    assert company_prices == generated_company_prices
    for price in company_prices:
        assert price.is_chain is False

    prices_json = server.get_prices(user, chain_company) >> 200
    prices = PriceItem.list_from_json(prices_json)
    assert prices == generated_company_prices + generated_chain_prices
    assert prices == company_prices + chain_prices


def test_add_popular_chain_items_after_company_popular_items(user, chain, chain_company):
    generated_company_prices = generate_prices(
        user, chain_company, is_popular=True, count=MAX_POPULAR_ITEMS_COUNT)
    generated_chain_prices = generate_prices(
        user, chain, is_popular=True, count=MAX_POPULAR_ITEMS_COUNT)

    prices_json = server.get_prices(user, chain_company) >> 200
    prices = PriceItem.list_from_json(prices_json)

    assert prices == generated_chain_prices + generated_company_prices
    assert prices_json["popular_goods"]["popular_count"] == MAX_POPULAR_ITEMS_COUNT * 2
    assert prices_json["popular_goods"]["max_popular_count"] == MAX_POPULAR_ITEMS_COUNT


def test_add_popular_company_items_after_chain_popular_items(user, chain, chain_company):
    generated_chain_prices = generate_prices(
        user, chain, is_popular=True, count=MAX_POPULAR_ITEMS_COUNT)
    price_item = PriceItem(is_popular=True)
    server.post_price(user, price_item, chain_company) >> 422

    prices_json = server.get_prices(user, chain_company) >> 200
    prices = PriceItem.list_from_json(prices_json)

    assert prices == generated_chain_prices
    assert prices_json["popular_goods"]["popular_count"] == MAX_POPULAR_ITEMS_COUNT
    assert prices_json["popular_goods"]["max_popular_count"] == MAX_POPULAR_ITEMS_COUNT


def test_get_chain_and_company_prices_count(user, chain, chain_company):
    chain_prices = generate_prices(user, chain)
    company_prices = generate_prices(user, chain_company)
    prices_count = server.get_prices_count(user, chain_company) >> 200
    assert len(chain_prices) + len(company_prices) == prices_count['total']


def test_get_overwrites_count(user, chain_company, chain):
    TOTAL_COUNT = 10
    OVERWRITES_COUNT = 5

    generated_prices = generate_prices(user, chain, count=TOTAL_COUNT)
    overwrited_item_ids = []
    overwrites = []
    for index in range(OVERWRITES_COUNT):
        price = generated_prices[index]
        overwrited_item_ids.append(price.id)
        overwrite = PriceOverwrite.create_overwrite(price, 'is_hidden')
        overwrites.append(overwrite)
    server.edit_chain_prices(user, overwrites, chain_company) >> 200

    prices = server.get_prices(user, chain) >> 200
    prices = PriceItem.list_from_json(prices)

    total_overwrites_count = 0
    for price in prices:
        if price.id in overwrited_item_ids:
            assert price.overwrites_count == 1
        else:
            assert price.overwrites_count == 0
        total_overwrites_count += price.overwrites_count

    assert total_overwrites_count == OVERWRITES_COUNT

    prices = server.get_prices(user, chain_company) >> 200
    prices = PriceItem.list_from_json(prices)
    for price in prices:
        assert price.overwrites_count is None


def test_get_offices_with_overwrites(user, chain):
    generated_prices = generate_prices(user, chain, count=2, is_hidden=False)

    def make_office():
        company = Company(parent_chain=chain)
        company.register(user=user)
        company.saveToDb()
        return company

    office1 = make_office()
    office2 = make_office()
    office3 = make_office()

    price_to_overwrite = generated_prices[0]

    overwrite1 = PriceOverwrite.create_overwrite(price_to_overwrite, 'is_popular')
    server.edit_chain_prices(user, [overwrite1], office1) >> 200
    overwrite2 = PriceOverwrite.create_overwrite(price_to_overwrite, 'availability')
    server.edit_chain_prices(user, [overwrite2], office2) >> 200
    overwrite3 = PriceOverwrite.create_overwrite(price_to_overwrite, 'is_hidden')
    server.edit_chain_prices(user, [overwrite3], office3) >> 200

    response = server.get_offices_with_overwrites(user, chain, price_to_overwrite.id) >> 200
    assert response['pager']['total'] == 3
    companies = response['companies']
    assert len(companies) == 3

    for company in companies:
        if company['permalink'] == str(office1.permalink):
            assert company['address'] == office1.address['value']
            overwrite = company['overwrite']
            assert 'is_hidden' not in overwrite
            assert overwrite['is_popular'] == overwrite1.is_popular
            assert 'availability' not in overwrite
        elif company['permalink'] == str(office2.permalink):
            assert company['address'] == office2.address['value']
            overwrite = company['overwrite']
            assert 'is_hidden' not in overwrite
            assert 'is_popular' not in overwrite
            assert overwrite['availability'] == overwrite2.availability['status']
        elif company['permalink'] == str(office3.permalink):
            assert company['address'] == office3.address['value']
            overwrite = company['overwrite']
            assert overwrite['is_hidden']
            assert 'is_popular' not in overwrite
            assert 'availability' not in overwrite
        else:
            assert False

    price_without_overwrite = generated_prices[1]
    response = server.get_offices_with_overwrites(user, chain, price_without_overwrite.id) >> 200
    companies = response['companies']
    assert response['pager']['total'] == 0
    assert len(companies) == 0


def test_get_empty_overwrite(user, chain):
    generated_prices = generate_prices(user, chain, count=1)

    office = Company(parent_chain=chain)
    office.register(user=user)
    office.saveToDb()

    price = generated_prices[0]
    overwrite = PriceOverwrite(item_id=price.id, is_hidden=False)
    server.edit_chain_prices(user, [overwrite], office) >> 200

    response = server.get_offices_with_overwrites(user, chain, price.id) >> 200
    assert response['pager']['total'] == 1
    companies = response['companies']
    assert len(companies) == 1
    assert companies[0]['overwrite'] == {}
