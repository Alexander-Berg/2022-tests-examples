import lib.async_processor as async_processor

from data_types.price_item import generate_prices_in_database
from data_types.feed_company import get_feeds_companies_prices
from data_types.published_item import wait_for_all_published
from lib.server import server

from data_types.company import Company

import random
import rstr


def get_ids(items):
    return [item.id for item in items]


def test_export_prices_without_source_fails(company):
    server.get_export_prices_by_id(company) >> 422


def test_export_prices_for_unexisting_company_fails(company):
    generated = generate_prices_in_database(company, count=10, source='TYCOON')
    wait_for_all_published()

    server.get_export_prices_by_id(Company(), source='TYCOON', ids=get_ids(generated)) >> 422


def test_export_prices_bad_source(company):
    prices = server.get_export_prices_by_id(company, source=rstr.letters(5, 10)) >> 200
    assert len(prices['items']) == 0


def test_export_prices_empty_ids(company):
    prices = server.get_export_prices_by_id(company, source='TYCOON') >> 200
    assert len(prices['items']) == 0


def test_not_published_items_from_tycoon_are_invisible(company):
    items_count = 10
    with async_processor.detached():
        generated = generate_prices_in_database(company, count=items_count, source='TYCOON')
        prices = server.get_export_prices_by_id(company, source='TYCOON', ids=get_ids(generated)) >> 200
        assert len(prices['items']) == 0

    wait_for_all_published()
    prices = server.get_export_prices_by_id(company, source='TYCOON', ids=get_ids(generated)) >> 200
    assert len(prices['items']) == items_count


def get_random_element_ids(full_list):
    choised = random.sample(full_list, k=max(len(full_list) // 2, 1))
    return get_ids(choised)


def compare_ids(items_from_response, generated_price_ids):
    ids_from_response = [item['id'] for item in items_from_response]
    assert sorted(ids_from_response) == sorted(generated_price_ids)


def test_export_prices_from_different_feeds(company, feeds_companies):
    tycoon_prices = generate_prices_in_database(company, count=10, source='TYCOON')
    wait_for_all_published()

    for source, feed_companies_list in list(feeds_companies.items()) + [('TYCOON', [])]:
        prices = tycoon_prices if source == 'TYCOON' else get_feeds_companies_prices(feed_companies_list)
        expected_price_ids = get_random_element_ids(prices)
        response = server.get_export_prices_by_id(company, source=source, ids=expected_price_ids) >> 200
        compare_ids(response['items'], expected_price_ids)


def test_empty_list_for_wrong_source(company, feeds_companies):
    generate_prices_in_database(company, count=10, source='TYCOON')
    wait_for_all_published()

    for source, feed_companies_list in list(feeds_companies.items()):
        assert source != 'TYCOON'
        expected_price_ids = get_random_element_ids(get_feeds_companies_prices(feed_companies_list))
        response = server.get_export_prices_by_id(company, source='TYCOON', ids=expected_price_ids) >> 200
        assert len(response['items']) == 0


def test_empty_list_for_wrong_organization_id(company):
    generated = generate_prices_in_database(company, count=10, source='TYCOON')
    wait_for_all_published()

    empty_company = Company.generate()
    response = server.get_export_prices_by_id(empty_company, source='TYCOON', ids=get_ids(generated)) >> 200
    assert len(response['items']) == 0


def test_get_chain_published_prices(chain, chain_company):
    generated_ids = get_ids(generate_prices_in_database(chain, count=10, source='TYCOON'))
    wait_for_all_published()

    response = server.get_export_prices_by_id(chain_company, source='TYCOON', ids=generated_ids) >> 200
    compare_ids(response['items'], generated_ids)
