from data_types.price_item import PriceItem, generate_prices
from data_types.published_item import PublishedItem
from data_types.company import Company
from data_types.chain import Chain
from data_types.feed_company import get_feeds_companies_prices
from lib.server import server
import lib.async_processor as async_processor
import random
import pytest


def validate_groups_and_prices(user, organization):
    prices = server.get_prices(user, organization) >> 200
    prices = PriceItem.list_from_json(prices)

    response = server.get_company_groups(user, organization) >> 200
    groups = response["groups"]
    groups = {g['id'] for g in groups}
    assert all(p.group['id'] in groups for p in prices)
    assert len(groups) == len(prices)


def test_delete_pricelist(user, organization, chain_company):
    generated_prices = generate_prices(user, organization)
    async_processor.perform_all_work()

    server.delete_pricelist(user, organization) >> 200

    prices = server.get_prices(user, organization) >> 200
    prices = PriceItem.list_from_json(prices)
    assert len(prices) == 0

    published_items = PublishedItem.get_all()
    assert(len(published_items) == len(generated_prices))
    assert(all(item.status == 'Deleted' for item in published_items))


def test_delete_empty_pricelist(user, organization):
    server.delete_pricelist(user, organization) >> 204


def test_delete_pricelist_missing_company(user, organization_type):
    if organization_type == "company":
        invalid_organization = Company()
    else:
        invalid_organization = Chain()
    server.delete_pricelist(user, invalid_organization) >> 403


def test_delete_prices(user, organization, chain_company):
    prices = generate_prices(user, organization)
    async_processor.perform_all_work()

    price_ids = [price.id for price in prices]

    validate_groups_and_prices(user, organization)

    server.delete_prices(user, organization, price_ids=price_ids) >> 200

    prices = server.get_prices(user, organization) >> 200
    prices = PriceItem.list_from_json(prices)
    assert len(prices) == 0

    published_items = PublishedItem.get_all()
    assert(sum(item.status == 'Deleted' for item in published_items) == len(price_ids))

    validate_groups_and_prices(user, organization)

    server.delete_prices(user, organization, price_ids=price_ids) >> 422


def test_delete_some_prices(user, organization, chain_company):
    prices = generate_prices(user, organization)
    async_processor.perform_all_work()

    validate_groups_and_prices(user, organization)

    all_price_ids = [price.id for price in prices]
    deleted_price_ids = random.choices(all_price_ids)
    server.delete_prices(user, organization, price_ids=deleted_price_ids) >> 200

    remaining_prices = list(filter(lambda price: price.id not in deleted_price_ids, prices))
    prices = server.get_prices(user, organization) >> 200
    prices = PriceItem.list_from_json(prices)
    assert remaining_prices == prices

    published_items = PublishedItem.get_all()
    assert(sum(item.status == 'Deleted' for item in published_items) == len(deleted_price_ids))

    validate_groups_and_prices(user, organization)


def test_delete_prices_invalid_ids(user, organization):
    price_ids = [str(random.randint(1, 1000)) for _ in range(20)]
    server.delete_prices(user, organization, price_ids=price_ids) >> 422


def test_delete_prices_other_company(user, organization):
    prices = generate_prices(user, organization)
    if organization.organization_type == "company":
        other_organization = Company.generate(user=user)
    else:
        other_organization = Chain.generate(user=user)
    other_prices = generate_prices(user, other_organization)

    other_price_ids = [price.id for price in other_prices]
    print(f"Other price ids: {other_price_ids}")
    server.delete_prices(user, organization, price_ids=other_price_ids) >> 422

    received_other_prices = server.get_prices(user, other_organization) >> 200
    received_other_prices = PriceItem.list_from_json(received_other_prices)
    assert len(received_other_prices) == len(other_prices)

    received_prices = server.get_prices(user, organization) >> 200
    received_prices = PriceItem.list_from_json(received_prices)
    assert len(received_prices) == len(prices)


def test_delete_empty_prices(user, organization):
    server.delete_prices(user, organization, price_ids=[]) >> 204


@pytest.mark.parametrize("create_organization", [Company, Chain])
def test_delete_prices_missing_company(user, create_organization):
    invalid_organization = create_organization()
    server.delete_prices(user, invalid_organization, price_ids=[]) >> 403


def test_cant_delete_feed_prices(user, company, feeds_prices):
    price_ids = (price.id for price in feeds_prices)
    server.delete_prices(user, company, price_ids=price_ids) >> 422


def test_global_delete_preserves_feed_prices(user, company, feeds_companies):
    generate_prices(user, company)
    server.delete_pricelist(user, company) >> 200
    server.delete_pricelist(user, company) >> 204

    for feed_id in feeds_companies.keys():
        expected_prices = get_feeds_companies_prices(feeds_companies[feed_id])
        limit = len(expected_prices) * 2

        prices_json = server.get_prices(user, company, limit=limit, source=feed_id) >> 200
        prices = PriceItem.list_from_json(prices_json)
        assert prices == expected_prices


def test_delete_chain_pricelist(user, chain, chain_company):
    generated_prices = generate_prices(user, chain)
    async_processor.perform_all_work()

    assert(len(PublishedItem.get_all()) == len(generated_prices))

    server.delete_pricelist(user, chain) >> 200

    published_items = PublishedItem.get_all()
    assert(len(published_items) == len(generated_prices))
    assert(all(item.status == 'Deleted' for item in published_items))


def test_delete_chain_prices(user, chain, chain_company):
    generated_prices = generate_prices(user, chain)
    async_processor.perform_all_work()

    assert(len(PublishedItem.get_all()) == len(generated_prices))

    items_to_delete_count = random.randint(1, len(generated_prices))
    items_to_delete = random.sample(generated_prices, items_to_delete_count)
    deleted_price_ids = [item.id for item in items_to_delete]

    server.delete_prices(user, chain, price_ids=deleted_price_ids) >> 200

    published_items = PublishedItem.get_all()
    assert(len(published_items) == len(generated_prices))
    assert(sum(item.status == 'Deleted' for item in published_items) == items_to_delete_count)
