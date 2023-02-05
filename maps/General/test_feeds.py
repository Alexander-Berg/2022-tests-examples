import pytest

from lib.server import server
from data_types.available_feed import DEFAULT_FEED, AVAILABLE_FEEDS, AvailableFeed
from data_types.company import Company
from data_types.user import User
from data_types.price_item import PriceItem, generate_prices
from data_types.feed_company import get_most_priority_feed_id, get_feeds_companies_prices

import maps.automotive.libs.large_tests.lib.db as db
import lib.sprav as sprav


FEED_ID_TO_FEED = AvailableFeed.feeds_to_dict(AVAILABLE_FEEDS)


def test_get_feeds_no_settings_no_feeds_items(user, company, available_feeds):
    feeds_info = server.get_feeds(user, company) >> 200
    assert feeds_info['settings']['used_feed'] == DEFAULT_FEED.source
    assert feeds_info['settings']['disable_feeds'] is False


@pytest.mark.parametrize("with_photos_count", [True, False])
def test_get_feeds_no_settings_returns_expected_values(user, company, available_feeds, feeds_companies, with_photos_count):
    feeds_info = server.get_feeds(user, company, count_items_with_photos=with_photos_count) >> 200

    assert feeds_info['settings']['disable_feeds'] is False
    assert feeds_info['settings']['used_feed'] != DEFAULT_FEED.source

    assert len(feeds_info['feeds']) == len(available_feeds)

    present_feeds_dict = {feed['id']: feed for feed in feeds_info['feeds']}

    assert available_feeds.keys() == present_feeds_dict.keys()

    for feed_id in available_feeds.keys():
        feed = present_feeds_dict[feed_id]
        available_feed = available_feeds[feed_id]
        assert feed['name'] == available_feed.name
        if feed_id in feeds_companies:
            companies = feeds_companies[feed_id]
            assert feed['items_count'] == sum(len(company.items) for company in companies)
            if with_photos_count:
                items_with_photo_count = sum(sum(1 for item in company.items if item.photos) for company in companies)
                assert feed['items_with_photos_count'] == items_with_photo_count
            assert 'last_update_time' in feed
            cabinet_template_url = available_feed.cabinet_template_url
            if cabinet_template_url and len(companies) > 0:
                possible_urls = set(company.get_source_url(cabinet_template_url) for company in companies)
                assert feed['feed_cabinet_url'] in possible_urls
            else:
                assert 'feed_cabinet_url' not in feed
        else:
            assert feed['items_count'] == 0
            prohibited_fields = [
                'last_update_time',
                'feed_cabinet_url'
            ]
            for prohibited_field in prohibited_fields:
                assert prohibited_field not in feed


def generate_prices_for_chain(chain, user):
    generated_prices = generate_prices(user, chain, count=6, photo_cycle=True)
    prices = server.get_prices(user, chain) >> 200
    prices = PriceItem.list_from_json(prices)
    assert prices == generated_prices
    return generated_prices


@pytest.mark.parametrize("with_photos_count", [True, False])
def test_correct_chain_prices_count(regional_chain_companies, user, with_photos_count):
    expected_pricelists = []
    for company in regional_chain_companies:
        expected_pricelists.append(generate_prices_for_chain(company.parent_chain, user))

    for chain_company, expected_pricelist in zip(regional_chain_companies, expected_pricelists):
        feeds_info = server.get_feeds(user, chain_company, count_items_with_photos=with_photos_count) >> 200

        assert feeds_info["feeds"][0]["items_count"] == len(expected_pricelist)
        if with_photos_count:
            assert feeds_info["feeds"][0]["items_with_photos_count"] == sum(1 for item in expected_pricelist if item.photos)


def test_get_feeds_no_settings_returns_expected_selected_feed(user, company, available_feeds, feeds_companies):
    most_priority_feed_id = get_most_priority_feed_id(available_feeds, feeds_companies)

    feeds_info = server.get_feeds(user, company) >> 200
    assert feeds_info['settings']['used_feed'] == most_priority_feed_id


def test_can_select_feed(user, company, feeds_companies):
    feeds_info = server.get_feeds(user, company) >> 200
    originally_used_feed = feeds_info['settings']['used_feed']

    feed_to_select = next(filter(
        lambda source_id: source_id != originally_used_feed,
        feeds_companies.keys()))
    feeds_settings = server.post_feeds_settings(user, company, selected_feed=feed_to_select) >> 200

    selected_feed_name = FEED_ID_TO_FEED[feed_to_select].name
    expected_settings = {
        'used_feed': feed_to_select,
        'used_feed_name': selected_feed_name,
        'disable_feeds': False,
        'selected_feed': feed_to_select,
        'selected_feed_name': selected_feed_name
    }

    assert feeds_settings == expected_settings

    feeds_info = server.get_feeds(user, company) >> 200
    assert feeds_info['settings'] == expected_settings


def test_can_disable_feeds(user, company, feeds_companies):
    feeds_settings = server.post_feeds_settings(user, company, selected_feed=None) >> 200
    expected_settings = {
        'disable_feeds': True,
    }

    assert feeds_settings == expected_settings

    feeds_info = server.get_feeds(user, company) >> 200
    assert feeds_info['settings'] == expected_settings

    prices_json = server.get_prices(user, company, get_used_feed=False) >> 200
    assert prices_json["feed_settings"]["disable_feeds"]


def test_can_disable_feeds_when_no_feeds_companies_exist(user, company):
    feeds_settings = server.post_feeds_settings(user, company, selected_feed=None) >> 200
    expected_settings = {
        'disable_feeds': True,
    }

    assert feeds_settings == expected_settings

    feeds_info = server.get_feeds(user, company) >> 200
    assert feeds_info['settings'] == expected_settings


def test_cannot_select_invalid_feed(user, company):
    invalid_feed = "NoSuchFeedExists"
    server.post_feeds_settings(user, company, selected_feed=invalid_feed) >> 422


def test_cannot_get_missing_organization_feeds(user):
    invalid_company = Company()
    server.post_feeds_settings(user, invalid_company, selected_feed=None) >> 403


def test_cannot_set_missing_organization_settings(user):
    invalid_company = Company()
    server.post_feeds_settings(user, invalid_company, selected_feed=None) >> 403


def test_get_feeds_pricelist_get_used_source(user, company, available_feeds, feeds_companies):
    most_priority_feed_id = get_most_priority_feed_id(available_feeds, feeds_companies)
    expected_prices = get_feeds_companies_prices(feeds_companies[most_priority_feed_id])
    limit = len(expected_prices) * 2

    prices_json = server.get_prices(user, company, get_used_feed=False) >> 200
    assert prices_json["source"]["id"] == 'TYCOON'
    assert prices_json["feed_settings"] == {
        "disable_feeds": False,
        "used_feed": most_priority_feed_id,
        "used_feed_name": FEED_ID_TO_FEED[most_priority_feed_id].name
    }
    assert len(PriceItem.list_from_json(prices_json)) == 0

    prices_json = server.get_prices(user, company, limit=limit, get_used_feed=True) >> 200
    assert prices_json['source']['id'] == most_priority_feed_id
    assert prices_json["feed_settings"] == {
        "disable_feeds": False,
        "used_feed": most_priority_feed_id,
        "used_feed_name": FEED_ID_TO_FEED[most_priority_feed_id].name
    }
    prices = PriceItem.list_from_json(prices_json)
    assert prices == expected_prices

    expected_groups = sorted({(price.group['id'], price.group['name']) for price in expected_prices})

    groups_json = server.get_company_groups(user, company, get_used_feed=True) >> 200
    groups = sorted([(group_json['id'], group_json['name']) for group_json in groups_json["groups"]])
    assert groups == expected_groups


def test_get_feeds_pricelist_get_used_source_in_chain_returns_cabinet_prices(user, chain, available_feeds):
    prices = PriceItem.list_from_json(server.get_prices(user, chain, get_used_feed=True) >> 200)
    assert len(prices) == 0

    expected_prices = generate_prices(user, chain)
    limit = len(expected_prices) * 2

    prices_json = server.get_prices(user, chain, limit=limit, get_used_feed=True) >> 200
    prices = PriceItem.list_from_json(prices_json)
    assert expected_prices == prices

    assert prices_json["feed_settings"] == {
        "disable_feeds": False,
        "used_feed": "TYCOON",
        "used_feed_name": FEED_ID_TO_FEED["TYCOON"].name
    }

    expected_groups = sorted({(price.group['id'], price.group['name']) for price in expected_prices})

    groups_json = server.get_company_groups(user, chain, get_used_feed=True) >> 200
    groups = sorted([(group_json['id'], group_json['name']) for group_json in groups_json["groups"]])
    assert groups == expected_groups


def test_get_feeds_pricelist_get_used_source_source_when_selected_feed_exists(user, company, feeds_companies):
    feeds_info = server.get_feeds(user, company) >> 200
    originally_used_feed = feeds_info['settings']['used_feed']

    feed_to_select = next(filter(
        lambda source_id: source_id != originally_used_feed,
        feeds_companies.keys()))
    server.post_feeds_settings(user, company, selected_feed=feed_to_select) >> 200

    expected_prices = get_feeds_companies_prices(feeds_companies[feed_to_select])
    limit = len(expected_prices) * 2

    prices_json = server.get_prices(user, company, limit=limit, get_used_feed=True) >> 200
    assert prices_json['source']['id'] == feed_to_select

    selected_feed_name = FEED_ID_TO_FEED[feed_to_select].name
    assert prices_json["feed_settings"] == {
        "disable_feeds": False,
        "used_feed": feed_to_select,
        "used_feed_name": selected_feed_name,
        "selected_feed": feed_to_select,
        'selected_feed_name': selected_feed_name
    }

    prices = PriceItem.list_from_json(prices_json)
    assert prices == expected_prices

    expected_groups = sorted({(price.group['id'], price.group['name']) for price in expected_prices})

    groups_json = server.get_company_groups(user, company, get_used_feed=True) >> 200
    groups = sorted([(group_json['id'], group_json['name']) for group_json in groups_json["groups"]])
    assert groups == expected_groups


def test_cannot_get_invalid_feed_prices(user, company):
    invalid_feed = "NoSuchFeedExists"
    server.get_prices(user, company, source=invalid_feed) >> 422
    server.get_company_groups(user, company, source=invalid_feed) >> 422


def test_get_handle_returns_correct_items_count_for_deleted_items(user, organization, feeds_companies):
    expected_prices = generate_prices(user, organization)

    prices_to_delete = generate_prices(user, organization)
    price_ids_to_delete = [price.id for price in prices_to_delete]
    server.delete_prices(user, organization, price_ids=price_ids_to_delete) >> 200

    prices_json = server.get_prices(user, organization) >> 200
    assert prices_json['source']['id'] == DEFAULT_FEED.source
    assert prices_json['source']['items_count'] == len(expected_prices)


def test_feed_audit_info_change(user, company, feeds_companies):
    def get_audit_info():
        with db.get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("""
                    SELECT author_uid, last_edit_time
                    FROM feeds_settings
                    WHERE organization_id = %s
                """, (company.db_id,))
                row = cur.fetchone()
                return str(row[0]), row[1]

    another_user = User()
    another_user.register()
    sprav.add_company_permission(company, another_user)

    feeds_info = server.get_feeds(user, company) >> 200
    feed_id = feeds_info['settings']['used_feed']

    server.post_feeds_settings(user, company, selected_feed=feed_id) >> 200
    prev_author_uid, prev_last_edit_time = get_audit_info()
    assert prev_author_uid == user.uid

    server.post_feeds_settings(another_user, company, selected_feed=feed_id) >> 200
    new_author_uid, new_last_edit_time = get_audit_info()
    assert new_author_uid == another_user.uid
    assert new_last_edit_time > prev_last_edit_time
