from data_types.price_item import PriceItem, generate_prices, set_moderation_status, generate_image, MAX_GROUP_LENGTH
from data_types.company import Company
from data_types.chain import Chain
from data_types.feed_company import get_feeds_companies_prices
from lib.server import server

import pytest
import random
import re
import rstr

DEFAULT_LIMIT = 20


def check_get_page_by_item_id(user, organization, all_prices, page_size, **kwargs):
    page_count = len(all_prices) // page_size
    if 0 < len(all_prices) % page_size:
        page_count += 1

    for page in range(0, page_count):
        offset = page * page_size
        limit = page_size
        expected_prices = all_prices[offset:offset+limit]

        price = random.choice(expected_prices)

        prices_json = server.get_prices(user, organization, offset_by_item_id=price.id, limit=page_size, **kwargs) >> 200
        prices = PriceItem.list_from_json(prices_json)

        assert expected_prices == prices
        assert prices_json['pager']['limit'] == page_size
        assert prices_json['pager']['offset'] == offset
        assert prices_json['pager']['total'] == len(all_prices)


def test_get_pricelist(user, organization, feeds_companies):
    prices = server.get_prices(user, organization) >> 200
    assert len(prices['items']) == 0

    price_item = PriceItem()
    server.post_price(user, price_item, organization) >> 200

    prices = server.get_prices(user, organization) >> 200
    items = prices['items']
    assert len(items) == 1
    assert PriceItem.from_json(items[0]) == price_item
    assert re.match("^\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z$", items[0]["moderation"]["update_time"])


def test_get_price(user, organization):
    MISSING_PRICE_ID = 1
    resp = server.get_prices_by_id(user, organization, id=MISSING_PRICE_ID) >> 200
    assert len(resp['items']) == 0

    generated_prices = generate_prices(user, organization)
    generated_prices_ids = [price.id for price in generated_prices]

    prices_json = server.get_prices_by_id(user, organization, id=generated_prices_ids) >> 200
    prices = prices_json['items']

    assert len(prices) == len(generated_prices)
    for price in prices:
        assert PriceItem.from_json(price) in generated_prices


def test_get_price_bad_parameters(user, organization):
    resp = server.get_prices_by_id(user, organization) >> 200
    assert len(resp['items']) == 0

    server.get_prices_by_id(user, organization, id='string_id') >> 422


def test_get_deleted_price_reject(user, organization):
    price_item = PriceItem.from_json(server.post_price(user, PriceItem(), organization) >> 200)
    server.delete_prices(user, organization, [price_item.id]) >> 200
    resp = server.get_prices_by_id(user, organization, id=price_item.id) >> 200
    assert len(resp['items']) == 0


def test_get_pricelist_limit_offset(user, organization, feeds_companies):
    total_count = random.randint(DEFAULT_LIMIT * 1.2, DEFAULT_LIMIT * 2)
    generated_prices = generate_prices(user, organization, count=total_count)

    prices_json = server.get_prices(user, organization) >> 200
    prices = PriceItem.list_from_json(prices_json)

    assert prices == generated_prices[:DEFAULT_LIMIT]

    limit = random.randint(1, 10)
    offset = random.randint(0, total_count - limit)
    prices = server.get_prices(
        user,
        organization,
        offset=offset,
        limit=limit) >> 200

    pager = prices['pager']
    assert pager['limit'] == limit
    assert pager['offset'] == offset
    assert pager['total'] == total_count

    prices = PriceItem.list_from_json(prices)
    assert prices == generated_prices[offset:offset+limit]


def test_get_pricelist_popular_info(user, organization, alien_user, alien_company, feeds_companies):
    generate_prices(alien_user, alien_company)

    price_to_delete = PriceItem(is_popular=True)
    price_to_delete = PriceItem.from_json(server.post_price(user, price_to_delete, organization) >> 200)
    server.delete_prices(user, organization, price_ids=[price_to_delete.id]) >> 200

    prices = generate_prices(user, organization)
    popular_count = sum(price.is_popular for price in prices)

    prices_json = server.get_prices(user, organization) >> 200
    popular = prices_json['popular_goods']
    assert popular['max_popular_count'] == PriceItem.MAX_POPULAR_COUNT
    assert popular['popular_count'] == popular_count


@pytest.mark.parametrize("filter_type", [
    ("is_popular", True, False, 'true', 'false'),
    ("is_hidden", True, False, 'true', 'false'),
    ("availability_status", 'Available', 'OutOfStock', 'Available', 'OutOfStock'),
])
def test_get_pricelist_filter_boolean(filter_type, user, organization, feeds_companies):
    filter_name = filter_type[0]
    positive_value_to_set = filter_type[1]
    negative_value_to_set = filter_type[2]
    positive_value_to_search = filter_type[3]
    negative_value_to_search = filter_type[4]

    option_on_count = random.randint(2, 7)
    option_off_count = random.randint(8, 15)
    option_on_prices = generate_prices(
        user,
        organization,
        count=option_on_count,
        **{filter_name: positive_value_to_set})
    option_off_prices = generate_prices(
        user,
        organization,
        count=option_off_count,
        **{filter_name: negative_value_to_set})
    all_prices = option_off_prices + option_on_prices
    get_limit = len(all_prices) * 2

    prices_json = server.get_prices(
        user,
        organization,
        limit=get_limit,
        **{filter_name: positive_value_to_search}) >> 200
    prices = PriceItem.list_from_json(prices_json)
    assert prices_json['pager']['total'] == len(prices)
    assert prices == option_on_prices

    prices_json = server.get_prices(
        user,
        organization,
        limit=get_limit,
        **{filter_name: negative_value_to_search}) >> 200
    prices = PriceItem.list_from_json(prices_json)
    assert prices == option_off_prices

    prices_json = server.get_prices(user, organization, limit=get_limit) >> 200
    prices = PriceItem.list_from_json(prices_json)
    assert prices == all_prices


def test_get_pricelist_invalid_availability_status(user, organization):
    INVALID_AVAILABILITY = "InvalidAvailability"
    server.get_prices(
        user,
        organization,
        availability_status=INVALID_AVAILABILITY) >> 422


def test_get_pricelist_filter_with_photos(user, organization, feeds_companies):
    with_photos_count = random.randint(2, 7)
    without_photos_count = random.randint(8, 15)
    with_photos_prices = generate_prices(
        user,
        organization,
        count=with_photos_count,
        photo_count=random.randint(1, 2))
    without_photos_prices = generate_prices(
        user,
        organization,
        count=without_photos_count,
        photo_count=0)
    all_prices = without_photos_prices + with_photos_prices
    get_limit = len(all_prices) * 2

    prices_json = server.get_prices(
        user,
        organization,
        limit=get_limit,
        with_photos=True) >> 200
    prices = PriceItem.list_from_json(prices_json)
    assert prices_json['pager']['total'] == len(with_photos_prices)
    assert prices == with_photos_prices

    prices_json = server.get_prices(user, organization, limit=get_limit) >> 200
    prices = PriceItem.list_from_json(prices_json)
    assert prices == all_prices


def test_get_pricelist_filter_query_title(user, organization, feeds_companies):
    generate_prices(user, organization)
    price_item = PriceItem(title='White indo-african elefant')
    server.post_price(user, price_item, organization) >> 200

    prices_json = server.get_prices(
        user,
        organization,
        query='wHiT') >> 200
    prices = PriceItem.list_from_json(prices_json)

    assert len(prices) == 1
    assert prices[0] == price_item


def test_get_pricelist_filter_query_price_value(user, organization, feeds_companies):
    generate_prices(user, organization)
    price_item = PriceItem(price={
        'type': 'ExactNumber',
        'currency': 'RUB',
        'value': 1_234_567,
    })
    server.post_price(user, price_item, organization) >> 200

    prices_json = server.get_prices(
        user,
        organization,
        query='23456') >> 200
    prices = PriceItem.list_from_json(prices_json)

    assert len(prices) == 1
    assert prices[0] == price_item


def test_get_pricelist_count(user, organization, feeds_companies):
    prices = generate_prices(user, organization)

    prices_count = server.get_prices_count(
        user,
        organization) >> 200
    assert len(prices) == prices_count['total']


@pytest.mark.skip('Disabled because there are no indices for such queries')
def test_get_pricelist_filter_query_description(user, organization, feeds_companies):
    generate_prices(user, organization)
    price_item = PriceItem(description='Buy our elefants')
    server.post_price(user, price_item, organization) >> 200

    prices_json = server.get_prices(
        user,
        organization,
        query='bUy') >> 200
    prices = PriceItem.list_from_json(prices_json)

    assert len(prices) == 1
    assert prices[0] == price_item


def test_get_pricelist_filter_query_title_cyrillic(user, organization, feeds_companies):
    generate_prices(user, organization)
    price_item = PriceItem(title='Рога и копыта')
    server.post_price(user, price_item, organization) >> 200

    prices_json = server.get_prices(
        user,
        organization,
        query='га и коп') >> 200
    prices = PriceItem.list_from_json(prices_json)

    assert len(prices) == 1
    assert prices[0] == price_item


def test_get_pricelist_filter_query_percentage(user, organization, feeds_companies):
    generate_prices(user, organization, title=rstr.letters(5, 20))
    price_item = PriceItem(title='%Cotton = 100')
    server.post_price(user, price_item, organization) >> 200

    prices_json = server.get_prices(
        user,
        organization,
        query='%') >> 200
    prices = PriceItem.list_from_json(prices_json)

    assert len(prices) == 1
    assert prices[0] == price_item


def test_get_pricelist_query_by_groups(user, organization, feeds_companies):
    generate_prices(user, organization, count=8)

    prices = PriceItem.list_from_json(server.get_prices(user, organization) >> 200)
    groups = [p.group['id'] for p in prices]

    for length in [1, 8]:
        random.shuffle(groups)
        groups_to_query = groups[:length]

        raw_prices = server.get_prices(user, organization, group_id=groups_to_query) >> 200
        prices = PriceItem.list_from_json(raw_prices)

        assert all(p.group['id'] in groups_to_query for p in prices)
        assert len(prices) == length  # all items have unique groups


def test_get_pricelist_query_by_groups_with_limit(user, organization, feeds_companies):
    price_count = 21
    generate_prices(user, organization, count=price_count, group={"name": "same for all"})

    for limit in [None, 13, 7, 1]:
        prices = PriceItem.list_from_json(server.get_prices(user, organization, limit=limit) >> 200)
        group_id = prices[0].group["id"]

        assert all(p.group['id'] == group_id for p in prices)
        assert len(prices) == (limit or DEFAULT_LIMIT)


def test_get_feeds_pricelist(user, company, available_feeds, feeds_companies):
    generate_prices(user, company)

    for source, feed_companies_list in feeds_companies.items():
        expected_prices = get_feeds_companies_prices(feed_companies_list)
        limit = len(expected_prices) * 2

        prices_json = server.get_prices(user, company, source=source, limit=limit) >> 200

        assert prices_json['source']['id'] == source
        assert prices_json['source']['items_count'] == len(expected_prices)
        assert prices_json['source']['name'] == available_feeds[source].name

        pager = prices_json['pager']
        assert pager['limit'] == limit
        assert pager['offset'] == 0
        assert pager['total'] == len(expected_prices)

        popular_count = sum(price.is_popular for price in expected_prices)
        popular = prices_json['popular_goods']
        assert popular['max_popular_count'] == PriceItem.MAX_POPULAR_COUNT
        assert popular['popular_count'] == popular_count

        prices = PriceItem.list_from_json(prices_json)
        assert prices == expected_prices


def test_get_declined_count(user, organization):
    total_count = 5
    generated_prices = generate_prices(user, organization, count=total_count)

    prices_json = server.get_prices(user, organization) >> 200
    assert prices_json['pager']['total'] == total_count
    assert prices_json['moderation_info']['declined_count'] == 0

    declined_count = 2
    declined_price_ids = [price.id for price in generated_prices[:declined_count]]
    set_moderation_status(declined_price_ids, 'Declined')

    prices_json = server.get_prices(user, organization) >> 200
    assert prices_json['pager']['total'] == total_count
    assert prices_json['moderation_info']['declined_count'] == declined_count


def test_get_declined_count_with_filters(user, organization):
    price_item = PriceItem(group={'name': 'group1'}, photos=[generate_image(user)])
    server.post_price(user, price_item, organization) >> 200
    price_item = PriceItem(group={'name': 'group2'})
    server.post_price(user, price_item, organization) >> 200

    prices_json = server.get_prices(user, organization) >> 200
    prices = PriceItem.list_from_json(prices_json)
    assert len(prices) == 2

    set_moderation_status([price.id for price in prices], 'Declined')

    price_item = PriceItem(group={'name': 'group3'})
    server.post_price(user, price_item, organization) >> 200

    prices_json = server.get_prices(user, organization) >> 200
    assert prices_json['moderation_info']['declined_count'] == 2
    prices = PriceItem.list_from_json(prices_json)
    assert len(prices) == 3

    prices_json = server.get_prices(user, organization, group_name='Group1') >> 200
    assert prices_json['moderation_info']['declined_count'] == 2
    prices = PriceItem.list_from_json(prices_json)
    assert len(prices) == 1

    prices_json = server.get_prices(user, organization, with_photos=True) >> 200
    assert prices_json['moderation_info']['declined_count'] == 2
    prices = PriceItem.list_from_json(prices_json)
    assert len(prices) == 1


def test_get_declined_prices_only(user, organization):
    total_count = 5
    generated_prices = generate_prices(user, organization, count=total_count)

    declined_count = 3
    declined_prices = generated_prices[:declined_count]
    set_moderation_status([price.id for price in declined_prices], 'Declined')

    prices_json = server.get_prices(user, organization) >> 200
    assert prices_json['pager']['total'] == total_count
    assert prices_json['moderation_info']['declined_count'] == declined_count
    assert PriceItem.list_from_json(prices_json) == generated_prices

    prices_json = server.get_prices(user, organization, declined_only=False) >> 200
    assert prices_json['pager']['total'] == total_count
    assert prices_json['moderation_info']['declined_count'] == declined_count
    assert PriceItem.list_from_json(prices_json) == generated_prices

    prices_json = server.get_prices(user, organization, declined_only=True) >> 200
    assert prices_json['pager']['total'] == declined_count
    assert prices_json['moderation_info']['declined_count'] == declined_count
    prices = PriceItem.list_from_json(prices_json)
    assert prices == declined_prices


@pytest.mark.parametrize("create_organization", [Company, Chain])
def test_get_pricelist_wrong_organization(user, create_organization):
    invalid_organization = create_organization()
    error = server.get_prices(user, invalid_organization) >> 403
    assert error["code"] == "PERMISSION_DENIED"


def test_get_zero_price(user, organization):
    price_item = PriceItem()
    price_item.price['value'] = 0

    price_item.saveToDb(organization)

    prices_json = server.get_prices(user, organization) >> 200
    prices = PriceItem.list_from_json(prices_json)

    for price in prices:
        assert price == price_item


@pytest.mark.parametrize("exclusion", [True, False])
def test_get_pricelist_query_by_groups_names(user, organization, exclusion):
    total_count = 8
    generate_prices(user, organization, count=total_count)

    prices = PriceItem.list_from_json(server.get_prices(user, organization) >> 200)
    groups = [p.group['name'] for p in prices]

    items_without_groups_count = 1
    generate_prices(user, organization, count=items_without_groups_count, group={"name": ""})

    for length in [1, total_count]:
        random.shuffle(groups)
        groups_to_query = groups[:length]

        raw_prices = server.get_prices(
            user, organization, group_name=groups_to_query, exclude_groups=exclusion) >> 200
        prices = PriceItem.list_from_json(raw_prices)

        if exclusion:
            assert all(p.group is None or p.group.get('name') not in groups_to_query for p in prices)
            assert len(prices) == (total_count - length + items_without_groups_count)  # all items have unique groups
        else:
            assert all(p.group['name'] in groups_to_query for p in prices)
            assert len(prices) == length  # all items have unique groups


@pytest.mark.parametrize("exclusion", [True, False])
def test_get_pricelist_query_by_group_name_limit(user, organization, exclusion):
    generate_prices(user, organization)

    cyrillic_groupname_query_ok = 'и' * MAX_GROUP_LENGTH
    server.get_prices(
        user,
        organization,
        group_name=cyrillic_groupname_query_ok,
        exclude_groups=exclusion) >> 200

    cyrillic_groupname_query_too_long = 'и' * (MAX_GROUP_LENGTH + 1)
    error = server.get_prices(
        user,
        organization,
        group_name=cyrillic_groupname_query_too_long,
        exclude_groups=exclusion) >> 422
    assert error['code'] == 'GROUP_NAME_TOO_LONG'


def test_get_pricelist_query_by_price(user, organization):
    for price in ["", " ", rstr.letters(4, 10)]:  # not numeric should not be listed
        price_item = PriceItem(price={
            'type': 'Other',
            'currency': 'RUB',
            'value': price,
        })
        price_item.saveToDb(organization)

    min_price, max_price = 50, 1000
    price_range = range(min_price, max_price, 50)
    for i in price_range:
        price_item = PriceItem(price={
            'type': 'ExactNumber',
            'currency': 'RUB',
            'value': i,
        })
        server.post_price(user, price_item, organization) >> 200

    test_values = [
        (100, None),
        (None, 200),
        (300, 500),
        (100.5, 324.9999),
        (max_price, None),
        (None, min_price)
    ]
    for cfg_min, cfg_max in test_values:
        raw_prices = server.get_prices(
            user, organization, min_price=cfg_min, max_price=cfg_max) >> 200
        prices = PriceItem.list_from_json(raw_prices)
        cmp = lambda x: (cfg_min or min_price) <= x <= (cfg_max or max_price)

        assert all(cmp(p.price['value']) for p in prices)
        assert len(list(filter(lambda x: cmp(x), price_range))) == len(prices)


def test_invalid_price_filter(user, organization):
    for param in ["min_price", "max_price"]:
        for value in ['', False, rstr.letters(4, 10), '123ads', '123   ']:
            server.get_prices(user, organization, **{param: value}) >> 422
        for value in ['123', ' 123', '123.', '123.5678']:
            server.get_prices(user, organization, **{param: value}) >> 200


def test_invalid_group_filter(user, organization):
    for value in ['', rstr.letters(251)]:
        server.get_prices(user, organization, group_name=value) >> 422
    for value in [' ', rstr.letters(1, 250), rstr.letters(250)]:
        server.get_prices(user, organization, group_name=value) >> 200


def test_conflicted_group_filter(user, organization):
    r = server.get_prices(user, organization, group_name="name", group_id=123) >> 422
    assert r['code'] == 'CONFLICT_GROUP_FILTER'


def test_get_item_page_by_item_id(user, organization):
    page_size = random.randint(1, 10)
    page_count = random.randint(1, 5)

    min_price_count = page_size * (page_count - 1) + 1
    max_price_count = page_size * page_count
    price_count = random.randint(min_price_count, max_price_count)

    all_prices = generate_prices(user, organization, count=price_count)
    check_get_page_by_item_id(user, organization, all_prices, page_size)


@pytest.mark.parametrize("invalid_id_in_upper_values", [True, False])
def test_get_item_page_by_missing_item_id_works(user, organization, invalid_id_in_upper_values):
    page_size = random.randint(1, 10)
    page_count = random.randint(1, 5)

    min_price_count = page_size * (page_count - 1) + 1
    max_price_count = page_size * page_count
    price_count = random.randint(min_price_count, max_price_count)

    all_prices = generate_prices(user, organization, count=price_count)
    price_ids = list(map(lambda p: int(p.id), all_prices))
    min_id = min(price_ids)
    max_id = max(price_ids)

    if invalid_id_in_upper_values:
        min_pos = max(0, max_id + 1)
        max_pos = max(0, max_id + len(all_prices) + 1)
        offset = 0
    else:
        min_pos = max(0, min_id - len(all_prices))
        max_pos = max(0, min_id - 1)
        offset = ((len(all_prices) - 1) // page_size) * page_size

    MISSING_ITEM_ID = str(random.randint(min_pos, max_pos))
    prices_json = server.get_prices(user, organization, offset_by_item_id=MISSING_ITEM_ID, limit=page_size) >> 200
    prices = PriceItem.list_from_json(prices_json)

    assert len(prices) > 0
    assert prices_json['pager']['total'] == len(all_prices)
    assert prices_json['pager']['limit'] == page_size
    assert prices_json['pager']['offset'] == offset


@pytest.mark.parametrize("filter_type", ['is_popular', 'is_hidden'])
def test_filters_dont_work_with_item_id(user, organization, filter_type):
    all_prices = generate_prices(user, organization)
    price = random.choice(all_prices)
    filter_value = not getattr(price, filter_type)

    prices_json = server.get_prices(user, organization, offset_by_item_id=price.id, **{filter_type: filter_value}) >> 200
    prices = PriceItem.list_from_json(prices_json)
    price_ids = [price.id for price in prices]
    assert price.id in price_ids
