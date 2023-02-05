from data_types.price_item import generate_prices_in_database
from data_types.feed_company import get_feeds_companies_prices
from data_types.published_item import wait_for_all_published
from data_types.price_item import PriceItem, generate_image
from data_types.group import Group

from lib.server import server

import logging

DEFAULT_ORGANIZATIONS_GROUPS_LIMIT = 100


def sorted_groups_from_prices(prices):
    unique_groups = {p.group["name"] for p in prices}
    groups = [{"name": g} for g in unique_groups]
    groups.sort(key=lambda x: x["name"].lower().replace('\n', ' '))
    return groups


def test_no_source_prices(company, available_feeds):
    for source in available_feeds.keys():
        response = server.get_export_groups(company, source=source) >> 200
        assert len(response["groups"]) == 0
        assert response["pager"]["limit"] == DEFAULT_ORGANIZATIONS_GROUPS_LIMIT
        assert response["pager"]["offset"] == 0
        assert response["pager"]["total"] == 0


def test_export_pricelist_feed_prices(company, available_feeds, feeds_companies):
    tycoon_prices = generate_prices_in_database(company, count=10, alphanumeric_rstr=True, source='TYCOON')
    wait_for_all_published()

    for source, feed_companies_list in list(feeds_companies.items()) + [("TYCOON", [])]:
        expected_prices = tycoon_prices if source == 'TYCOON' else get_feeds_companies_prices(feed_companies_list)
        price_count = len(expected_prices)
        sorted_groups = sorted_groups_from_prices(expected_prices)
        logging.info('All groups sorted: ' + str(sorted_groups))
        for limit in set([1, max(1, price_count // 2), price_count]):
            for offset in range(0, price_count, limit):
                response = server.get_export_groups(company, source=source, limit=limit, offset=offset) >> 200
                groups = response["groups"]

                logging.info('Expected groups: ' + str(sorted_groups[offset:offset+limit]))
                assert len(groups) == min(limit, price_count - offset)
                assert groups == sorted_groups[offset:offset+limit]
                assert response["pager"]["limit"] == limit
                assert response["pager"]["offset"] == offset
                assert response["pager"]["total"] == min(price_count, offset + 10 * limit)


def make_test_company_groups(user, organization):
    Group(1, 'Elephants').register()
    Group(2, 'Hippopotamus').register()

    server.post_price(user, PriceItem(group={'id': '1'}, photos=[generate_image(user)]), organization) >> 200
    server.post_price(user, PriceItem(group={'id': '2'}), organization) >> 200

    wait_for_all_published()


def test_export_get_company_groups_with_photos(user, company, feeds_companies):
    make_test_company_groups(user, company)
    groups_with_photo = ['Elephants']  # see make_test_company_groups()

    response_json = server.get_export_groups(company, source='TYCOON', with_photos=True) >> 200

    assert len(response_json["groups"]) == 1

    for group in response_json["groups"]:
        assert group['name'] in groups_with_photo

    response_json = server.get_export_groups(company, source='TYCOON') >> 200

    assert len(response_json["groups"]) == 2
