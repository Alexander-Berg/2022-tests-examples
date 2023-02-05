from lib.server import server
from data_types.price_item import PriceItem, wait_for_status, generate_prices
from data_types.published_item import wait_for_all_published
from data_types.feed_company import get_feeds_companies_prices
from data_types.company import Company

import lib.cleanweb as cleanweb

import rstr
import random
import copy
import pytest
import logging


FORBIDDING_VERDICTS = [
    {"name": "text_toloka_hard_violation", "synch": False},
    {"name": "clean_web_moderation_end", "synch": False},
]

PERMITTING_VERDICTS = [
    {"name": "clean_web_moderation_end", "synch": False},
]

TEST_SORT_BY_VALUES = [None, '', 'id', 'photo_presence']


def set_moderation_verdicts(text, is_positive):
    verdicts = PERMITTING_VERDICTS if is_positive else FORBIDDING_VERDICTS

    messages = [{
        "body": text,
        "name": verdict["name"],
        "value": True,
        "synch": verdict["synch"]
    } for verdict in verdicts]

    cleanweb.set_response(messages) >> 200


def sort_using_order(prices, sort_by):
    sort_by = sort_by or "id"
    if sort_by == "id":
        return sorted(prices, key=lambda p: int(p.id), reverse=True)
    elif sort_by == "photo_presence":
        def photos_to_int(photos):
            if photos:
                return 0
            else:
                return 1
        return sorted(prices, key=lambda p: (photos_to_int(p.photos), -int(p.id)))
    else:
        raise Exception(f"Unknown sort order: {sort_by}")


def generate_prices_with_order(user, company, sort_by):
    with_photos_count = random.randint(2, 7)
    without_photos_count = random.randint(8, 15)

    with_photos_prices = generate_prices(
        user,
        company,
        count=with_photos_count,
        photo_count=random.randint(1, 2))
    without_photos_prices = generate_prices(
        user,
        company,
        count=without_photos_count,
        photo_count=0)

    return sort_using_order(with_photos_prices + without_photos_prices, sort_by)


def test_export_pricelist_without_source_fails(company, feeds_companies):
    server.get_export_prices(company, source=None) >> 422


def test_export_pricelist_bad_source(company, feeds_companies):
    prices = server.get_export_prices(company, source=rstr.letters(5, 10)) >> 200
    assert len(prices['items']) == 0


def test_export_pricelist_empty_company(company, feeds_companies):
    prices = server.get_export_prices(company) >> 200
    assert len(prices['items']) == 0


def test_export_pricelist_empty_chain(chain, chain_company, feeds_companies):
    prices = server.get_export_prices(chain_company) >> 200
    assert len(prices['items']) == 0


def test_export_pricelist_declined_company(user, company, feeds_companies):
    price_item = PriceItem()
    set_moderation_verdicts(price_item.title, is_positive=False)
    price_id = (server.post_price(user, price_item, company) >> 200)["id"]

    wait_for_status(user, company, price_id, status="Declined")

    prices = server.get_export_prices(company) >> 200
    assert len(prices['items']) == 0
    assert prices['pager']['total'] == 0


def test_export_pricelist_declined_chain(user, chain, chain_company, feeds_companies):
    price_item = PriceItem()
    set_moderation_verdicts(price_item.title, is_positive=False)
    price_id = (server.post_price(user, price_item, chain) >> 200)["id"]

    wait_for_status(user, chain, price_id, status="Declined")

    prices = server.get_export_prices(chain_company) >> 200
    assert len(prices['items']) == 0
    assert prices['pager']['total'] == 0


def test_export_pricelist_published_company(user, company, feeds_companies):
    price_item = PriceItem()
    set_moderation_verdicts(price_item.title, is_positive=True)
    price_id = (server.post_price(user, price_item, company) >> 200)["id"]

    wait_for_status(user, company, price_id, status="Published")

    prices = server.get_export_prices(company) >> 200

    assert 'source' not in prices
    assert 'popular_goods' not in prices
    for item in prices['items']:
        assert 'moderation' not in item

    items = prices['items']
    assert len(items) == 1
    assert prices['pager']['total'] == 1
    assert PriceItem.from_json(items[0]) == price_item


def test_export_pricelist_published_company_filter_photos(user, company, feeds_companies):
    with_photos_count = random.randint(2, 7)
    without_photos_count = random.randint(8, 15)

    with_photos_prices = generate_prices(
        user,
        company,
        count=with_photos_count,
        photo_count=random.randint(1, 2))
    without_photos_prices = generate_prices(
        user,
        company,
        count=without_photos_count,
        photo_count=0)
    all_prices = without_photos_prices + with_photos_prices

    wait_for_all_published()

    prices_json = server.get_export_prices(
        company,
        with_photos=True) >> 200
    prices = PriceItem.list_from_json(prices_json)
    assert prices_json['pager']['total'] == len(with_photos_prices)
    assert prices == with_photos_prices

    prices_json = server.get_export_prices(company, limit=len(all_prices)) >> 200
    prices = PriceItem.list_from_json(prices_json)
    assert prices == all_prices


def test_export_pricelist_published_chain(user, chain, chain_company, feeds_companies):
    price_item = PriceItem()
    set_moderation_verdicts(price_item.title, is_positive=True)
    price_id = (server.post_price(user, price_item, chain) >> 200)["id"]

    wait_for_status(user, chain, price_id, status="Published")

    prices = server.get_export_prices(chain_company) >> 200

    assert 'source' not in prices
    assert 'popular_goods' not in prices
    for item in prices['items']:
        assert 'moderation' not in item

    items = prices['items']
    assert len(items) == 1
    assert prices['pager']['total'] == 1
    assert PriceItem.from_json(items[0]) == price_item


def test_export_pricelist_feed_prices_filter_photos(user, company, feeds_companies):
    for source, feed_companies_list in feeds_companies.items():
        all_prices = get_feeds_companies_prices(feed_companies_list)
        limit = len(all_prices) * 2
        expected_prices = list(filter(lambda price: price.photos, all_prices))

        prices_json = server.get_export_prices(company, source=source, limit=limit, with_photos=True) >> 200
        prices = PriceItem.list_from_json(prices_json)
        assert prices == expected_prices


def test_export_pricelist_feed_prices(company, available_feeds, feeds_companies):
    for source, feed_companies_list in feeds_companies.items():
        expected_prices = get_feeds_companies_prices(feed_companies_list)
        limit = len(expected_prices) * 2

        prices_json = server.get_export_prices(company, source=source, limit=limit) >> 200

        assert 'source' not in prices_json
        assert 'popular_goods' not in prices_json
        for item in prices_json['items']:
            assert 'moderation' not in item

        pager = prices_json['pager']
        assert pager['limit'] == limit
        assert pager['offset'] == 0
        assert pager['total'] == len(expected_prices)

        prices = PriceItem.list_from_json(prices_json)
        assert prices == expected_prices


def test_export_pricelist_unpublished_changes_are_invisible(user, chain, chain_company, feeds_companies):
    price_item = PriceItem()
    set_moderation_verdicts(price_item.title, is_positive=True)
    price_item = PriceItem.from_json(server.post_price(user, price_item, chain) >> 200)

    wait_for_status(user, chain, price_item.id, status="Published")

    edited_price = copy.deepcopy(price_item)
    edited_price.title = edited_price.title[:-10] + '_changed'
    set_moderation_verdicts(edited_price.title, is_positive=False)
    server.edit_price(user, edited_price, chain) >> 200

    wait_for_status(user, chain, edited_price.id, status="Declined")

    prices = server.get_export_prices(chain_company) >> 200
    items = prices['items']
    assert len(items) == 1
    assert prices['pager']['total'] == 1
    assert PriceItem.from_json(items[0]) == price_item


def test_export_pricelist_missing_company():
    missing_company = Company()
    prices = server.get_export_prices(missing_company) >> 200
    assert len(prices['items']) == 0


@pytest.mark.parametrize("sort_by", TEST_SORT_BY_VALUES)
def test_export_pricelist_order(user, company, sort_by):
    all_prices = generate_prices_with_order(user, company, sort_by)
    wait_for_all_published()

    prices_json = server.get_export_prices(company, limit=len(all_prices), sort_by=sort_by) >> 200
    prices = PriceItem.list_from_json(prices_json)
    assert prices == all_prices


@pytest.mark.parametrize("sort_by", TEST_SORT_BY_VALUES)
def test_export_pricelist_order_feeds(user, company, feeds_companies, sort_by):
    for source, feed_companies_list in feeds_companies.items():
        expected_prices = sort_using_order(get_feeds_companies_prices(feed_companies_list), sort_by)
        limit = len(expected_prices) * 2

        logging.info(expected_prices)

        prices_json = server.get_export_prices(company, source=source, limit=limit, sort_by=sort_by) >> 200
        prices = PriceItem.list_from_json(prices_json)
        assert prices == expected_prices


def test_export_pricelist_invalid_sort_by(company):
    INVALID_SORT_BY = 'invalid_sort_by'
    server.get_export_prices(company, sort_by=INVALID_SORT_BY) >> 422
