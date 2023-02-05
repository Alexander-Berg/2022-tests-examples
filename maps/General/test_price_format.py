from lib.server import server
from lib.random import printable_str
import lib.async_processor as async_processor
from data_types.price_item import PriceItem, \
    EXTERNAL_ID_ALLOWED_SYMBOLS, MAX_DESCRIPTION_LENGTH, MAX_URL_LENGTH, MAX_GROUP_LENGTH, MAX_TITLE_LENGTH, MAX_EXTERNAL_ID_LENGTH

import maps.automotive.libs.large_tests.lib.db as db

import pytest
import rstr
import string
import re


def get_published_item_fields(price_id):
    with db.get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT price_currency
                FROM published_items
                WHERE id = %s
                """,
                (price_id,))
            row = cur.fetchone()
            if row:
                return {"currency": row[0]}
    return None


def set_item_status(price_id, status):
    with db.get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                UPDATE items SET status=%s
                WHERE id = %s
                """,
                (status, price_id))
            conn.commit()


def set_item_currency(price_id, currency):
    with db.get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                UPDATE items SET price_currency=%s
                WHERE id = %s
                """,
                (currency, price_id))
            conn.commit()


def test_invalid_currency(user, company):
    invalid_currency = printable_str(10, 30)
    price = PriceItem.from_json(server.post_price(user, PriceItem(), company) >> 200)

    set_item_currency(price.id, invalid_currency)

    prices = PriceItem.list_from_json(server.get_prices(user, company) >> 200)
    assert len(prices) == 1

    price = prices[0]
    assert price.price["currency"] == invalid_currency

    set_item_status(price.id, "ReadyForPublishing")

    async_processor.perform_all_work()
    assert get_published_item_fields(price.id) is not None
    assert get_published_item_fields(price.id)["currency"] == invalid_currency

    price.price["value"] = 120
    server.edit_price(user, price, company) >> 422

    price.price["currency"] = "RUB"
    server.edit_price(user, price, company) >> 200

    set_item_status(price.id, "ReadyForPublishing")
    async_processor.perform_all_work()
    assert get_published_item_fields(price.id)["currency"] == "RUB"


@pytest.mark.parametrize("currency", ["RUB", "KZT", "UAH", "BYN", "USD", "EUR", "UZS", "TRY"])
def test_supported_currencies(user, company, currency):
    item = PriceItem()
    item.price["currency"] = currency

    price = PriceItem.from_json(server.post_price(user, item, company) >> 200)
    assert price.price["currency"] == currency


def test_null_currency_is_publishable(user, company):
    price = PriceItem.from_json(server.post_price(user, PriceItem(), company) >> 200)
    set_item_currency(price.id, None)

    set_item_status(price.id, "ReadyForPublishing")
    async_processor.perform_all_work()
    assert get_published_item_fields(price.id)
    assert get_published_item_fields(price.id)["currency"] is None


@pytest.mark.parametrize("value", ['', None])
@pytest.mark.parametrize("variable", ['description', 'external_id', 'market_url'])
def test_post_price_treat_value_as_null(user, organization, variable, value):
    price = PriceItem()
    setattr(price, variable, value)

    price = PriceItem.from_json(server.post_price(user, price, organization) >> 200)
    assert getattr(price, variable) is None


def invalid_symbols_for_external_id():
    return re.sub(EXTERNAL_ID_ALLOWED_SYMBOLS, '', string.printable)


@pytest.mark.parametrize("variable_and_value", [
    ("external_id", rstr.rstr(EXTERNAL_ID_ALLOWED_SYMBOLS, MAX_EXTERNAL_ID_LENGTH + 1)),
    ("title", printable_str(MAX_TITLE_LENGTH + 1)),
    ("description", printable_str(MAX_DESCRIPTION_LENGTH + 1)),
    ("market_url", rstr.urlsafe(MAX_URL_LENGTH + 1)),
    ("group.name", printable_str(MAX_GROUP_LENGTH + 1))
])
def test_invalid_fields_formats(user, company, variable_and_value):
    variable = variable_and_value[0]
    value = variable_and_value[1]

    price = PriceItem()
    parts = variable.split(".")

    if len(parts) > 1:
        getattr(price, parts[0])[parts[1]] = value
    else:
        setattr(price, variable, value)

    server.post_price(user, price, company) >> 422
