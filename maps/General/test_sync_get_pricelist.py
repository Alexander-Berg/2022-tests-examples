from data_types.price_item import PriceItem
from data_types.synchronization import (
    upload_items_yml_to_s3,
    generate_item_for_sync,
    generate_items,
    generate_items_and_upload_to_s3,
    generate_sync_and_upload
)
from lib.server import server

import pytest
import random


def compare_sync_items_omit_order(prices, uploaded_prices):
    actual = {p.external_id: p.to_import_file_format() for p in prices}
    expected = {g["id"]: g for g in uploaded_prices}
    assert actual == expected


@pytest.mark.skip('Disabled because YML does not support is_hidden and is_out_of_stock')
@pytest.mark.parametrize("filter_type", [
    ("is_popular", True, False, 'true', 'false'),
    ("availability_status", 'Available', 'OutOfStock', 'Available', 'OutOfStock'),
])
def test_get_sync_pricelist_filter_boolean(filter_type, user, company):
    filter_name = filter_type[0]
    positive_value_to_set = filter_type[1]
    negative_value_to_set = filter_type[2]
    positive_value_to_search = filter_type[3]
    negative_value_to_search = filter_type[4]

    option_on_count = random.randint(2, 7)
    option_off_count = random.randint(8, 15)
    option_on_prices = generate_items(
        count=option_on_count,
        **{filter_name: positive_value_to_set})
    option_off_prices = generate_items(
        count=option_off_count,
        **{filter_name: negative_value_to_set})
    all_prices = option_off_prices + option_on_prices

    url = upload_items_yml_to_s3(all_prices)
    sync = generate_sync_and_upload(user, company, url)

    get_limit = len(all_prices) * 2

    prices_json = server.get_prices(
        user,
        company,
        sync_id=sync.id,
        limit=get_limit,
        **{filter_name: positive_value_to_search}) >> 200
    prices = PriceItem.list_from_json(prices_json)
    assert prices_json['pager']['total'] == len(prices)
    compare_sync_items_omit_order(prices, option_on_prices)

    prices_json = server.get_prices(
        user,
        company,
        sync_id=sync.id,
        limit=get_limit,
        **{filter_name: negative_value_to_search}) >> 200
    prices = PriceItem.list_from_json(prices_json)
    compare_sync_items_omit_order(prices, option_off_prices)

    prices_json = server.get_prices(user, company, sync_id=sync.id, limit=get_limit) >> 200
    prices = PriceItem.list_from_json(prices_json)
    compare_sync_items_omit_order(prices, all_prices)


def test_get_sync_pricelist_filter_with_photos(user, company):
    with_photo_count = random.randint(2, 7)
    without_photo_count = random.randint(8, 15)
    with_photo_prices = generate_items(
        count=with_photo_count,
        with_photo_count=with_photo_count)
    without_photo_prices = generate_items(
        count=without_photo_count,
        with_photo_count=0)
    all_prices = without_photo_prices + with_photo_prices

    url = upload_items_yml_to_s3(all_prices)
    sync = generate_sync_and_upload(user, company, url)
    get_limit = len(all_prices) * 2

    prices_json = server.get_prices(
        user,
        company,
        sync_id=sync.id,
        limit=get_limit,
        with_photos=True) >> 200
    prices = PriceItem.list_from_json(prices_json)
    assert prices_json['pager']['total'] == len(with_photo_prices)
    compare_sync_items_omit_order(prices, with_photo_prices)

    prices_json = server.get_prices(user, company, sync_id=sync.id, limit=get_limit) >> 200
    prices = PriceItem.list_from_json(prices_json)
    compare_sync_items_omit_order(prices, all_prices)


def test_get_sync_pricelist_filter_query_title_cyrillic(user, company, feeds_companies):
    generated_items = generate_items()

    price_item = generate_item_for_sync(title='Рога и копыта').to_import_file_format()
    generated_items.append(price_item)

    url = upload_items_yml_to_s3(generated_items)
    sync = generate_sync_and_upload(user, company, url)

    prices_json = server.get_prices(
        user,
        company,
        sync_id=sync.id,
        query='га и коп') >> 200
    prices = PriceItem.list_from_json(prices_json)

    assert len(prices) == 1
    assert prices[0].to_import_file_format() == price_item


def test_get_sync_pricelist_query_by_price(user, company):
    min_price, max_price = 50, 1000
    price_range = range(min_price, max_price, 50)
    generated_items = []
    for i in price_range:
        generated_items += generate_items(count=1, price={
            'type': 'ExactNumber',
            'currency': 'RUB',
            'value': i,
        })

    url = upload_items_yml_to_s3(generated_items)
    sync = generate_sync_and_upload(user, company, url)

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
            user, company, sync_id=sync.id, min_price=cfg_min, max_price=cfg_max) >> 200
        prices = PriceItem.list_from_json(raw_prices)
        cmp = lambda x: (cfg_min or min_price) <= x <= (cfg_max or max_price)

        assert all(cmp(p.price['value']) for p in prices)
        assert len(list(filter(lambda x: cmp(x), price_range))) == len(prices)


@pytest.mark.parametrize("exclusion", [True, False])
def test_get_sync_pricelist_query_by_groups_names(user, company, exclusion):
    total_count = 8
    generated_items = generate_items(count=total_count)

    groups = [i['group'] for i in generated_items]

    items_without_groups_count = 1
    generated_items += generate_items(count=items_without_groups_count, group={"name": ""})

    url = upload_items_yml_to_s3(generated_items)
    sync = generate_sync_and_upload(user, company, url)

    for length in [1, total_count]:
        random.shuffle(groups)
        groups_to_query = groups[:length]

        raw_prices = server.get_prices(
            user, company, sync_id=sync.id,
            group_name=groups_to_query, exclude_groups=exclusion) >> 200
        prices = PriceItem.list_from_json(raw_prices)

        if exclusion:
            assert all(p.group is None or p.group.get('name') not in groups_to_query for p in prices)
            assert len(prices) == (total_count - length + items_without_groups_count)  # all items have unique groups
        else:
            assert all(p.group['name'] in groups_to_query for p in prices)
            assert len(prices) == length  # all items have unique groups


def test_get_sync_alien_company_items(user, company, alien_user, alien_company):
    generated_items = generate_items()
    url = upload_items_yml_to_s3(generated_items)
    sync = generate_sync_and_upload(user, company, url)

    server.get_prices(user, company, sync_id=sync.id) >> 200

    rsp = server.get_prices(alien_user, company, sync_id=sync.id) >> 403
    assert rsp['code'] == 'PERMISSION_DENIED'

    rsp = server.get_prices(alien_user, alien_company, sync_id=sync.id) >> 422
    assert rsp['code'] == 'SYNCHRONIZATION_NOT_FOUND'


def test_get_sync_disabled_items(user, company):
    items, url = generate_items_and_upload_to_s3()
    sync = generate_sync_and_upload(user, company, url)
    prices_json = server.get_prices(user, company, sync_id=sync.id) >> 200
    assert prices_json['pager']['total'] == len(items)

    sync.is_enabled = False
    server.edit_synchronization(user, company, sync) >> 200
    prices_json = server.get_prices(user, company, sync_id=sync.id) >> 200
    assert prices_json['pager']['total'] == len(items)


def test_get_sync_pricelist_by_ids(user, company):
    prices = generate_items(count=10)

    url = upload_items_yml_to_s3(prices)
    sync = generate_sync_and_upload(user, company, url)

    prices_json = server.get_prices(
        user,
        company,
        sync_id=sync.id) >> 200
    prices = PriceItem.list_from_json(prices_json)

    selected_prices = random.sample(prices, len(prices) // 2)
    prices_ids = [p.id for p in selected_prices]

    prices_json = server.get_prices_by_id(
        user,
        company,
        sync_id=sync.id,
        id=prices_ids) >> 200
    actual_prices = PriceItem.list_from_json(prices_json)

    selected_prices = sorted(selected_prices, reverse=True, key=lambda p: int(p.id))
    assert actual_prices == selected_prices


def test_get_sync_groups(user, company):
    prices = generate_items(count=10)
    url = upload_items_yml_to_s3(prices)
    sync = generate_sync_and_upload(user, company, url)

    prices_json = server.get_prices(
        user,
        company,
        sync_id=sync.id) >> 200
    prices = PriceItem.list_from_json(prices_json)

    groups = {p.group["name"] for p in prices}

    for query in [
        random.choice(list(groups)),  # full group name
        random.choice(list(groups))[:len(groups) // 2]  # prefix search
    ]:
        groups_json = server.get_company_groups(user, company, sync_id=sync.id, query=query) >> 200
        assert len(groups_json["groups"]) > 0, groups_json
        for g in groups_json["groups"]:
            g["name"].startswith(query)

    # no query filter
    groups_json = server.get_company_groups(user, company, sync_id=sync.id) >> 200
    assert len(groups_json["groups"]) == len(groups)
    assert {g["name"] for g in groups_json["groups"]} == groups


def test_get_sync_export_prices_without_sync_id_fails(company):
    err = server.get_export_prices_by_id(company, source='SYNC', ids=[12345]) >> 422
    assert err['code'] == 'MISSING_SYNCHRONIZATION_ID'
