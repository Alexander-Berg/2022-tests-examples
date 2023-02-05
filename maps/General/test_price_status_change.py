from data_types.price_overwrite import PriceOverwrite
from data_types.price_item import PriceItem
from lib.server import server
from lib.random import printable_str

import lib.cleanweb as cleanweb
import lib.async_processor as async_processor
import maps.automotive.libs.large_tests.lib.db as db

import pytest


def edit(user, organization, price_item):
    price_item.title = printable_str(5, 20)
    return server.edit_price(user, price_item, organization).code == 200


def delete(user, organization, price_item):
    price_ids = [price_item.id]
    return server.delete_prices(user, organization, price_ids=price_ids).code == 200


def overwrite(user, chain_company, price_item):
    overwrite = PriceOverwrite(
        item_id=price_item.id,
        is_hidden=price_item.is_hidden,
        price=price_item.price,
    )
    return server.edit_chain_prices(user, [overwrite], chain_company).code == 200


def create_item_with_status(user, organization, status):
    price_item = PriceItem.from_json(server.post_price(user, PriceItem(), organization) >> 200)
    with db.get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                UPDATE items SET
                    status = %s
                WHERE
                    id = %s
                """,
                (status, price_item.id))
            assert cur.rowcount == 1
    price_item.moderation["status"] = status
    return price_item


def get_price_item_status(price_item):
    with db.get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT
                    status
                FROM items
                WHERE
                    id = %s
                """,
                (price_item.id,))
            return cur.fetchone()[0]


def forbid_item(price_item):
    messages = [{
        "body": price_item.title,
        "name": "text_toloka_obscene",
        "value": True,
        "synch": False,
    }]
    cleanweb.set_response(messages) >> 200


def test_initial_price_item_status(user, organization):
    price_item = PriceItem.from_json(server.post_price(user, PriceItem(), organization) >> 200)
    assert price_item.moderation["status"] == 'OnModeration'


@pytest.mark.parametrize("change_setup", [
    {"from": "ApproveRequired", "to": "OnModeration", "action": edit},
    {"from": "ApproveRequired", "to": "Deleted",      "action": delete},

    {"from": "OnModeration", "to": "OnModeration", "action": edit},
    {"from": "OnModeration", "to": "Deleted",      "action": delete},

    {"from": "Declined", "to": "OnModeration", "action": edit},
    {"from": "Declined", "to": "Deleted",      "action": delete},

    {"from": "ReadyForPublishing", "to": "OnModeration", "action": edit},
    {"from": "ReadyForPublishing", "to": "Deleted",      "action": delete},

    {"from": "Published", "to": "OnModeration", "action": edit},
    {"from": "Published", "to": "Deleted",      "action": delete},
])
def test_status_changes_correctly(user, organization, change_setup):
    price_item = create_item_with_status(user, organization, change_setup["from"])
    assert change_setup["action"](user, organization, price_item)
    assert get_price_item_status(price_item) == change_setup["to"]


@pytest.mark.parametrize("change_setup", [
    {"from": "OnModeration",       "to": "OnModeration",       "action": overwrite},
    {"from": "Declined",           "to": "Declined",           "action": overwrite},
    {"from": "ReadyForPublishing", "to": "ReadyForPublishing", "action": overwrite},
    {"from": "Published",          "to": "Published",          "action": overwrite},
])
def test_status_changes_correctly_for_overwrites(user, chain, chain_company, change_setup):
    price_item = create_item_with_status(user, chain, change_setup["from"])
    assert change_setup["action"](user, chain_company, price_item)
    assert get_price_item_status(price_item) == change_setup["to"]


@pytest.mark.parametrize("change_setup", [
    {"from": "ReadyForDeletion", "action": edit},
    {"from": "ReadyForDeletion", "action": delete},

    {"from": "Deleted", "action": edit},
    {"from": "Deleted", "action": delete},
])
def test_status_change_fails_for_forbidden_transitions(user, organization, change_setup):
    price_item = create_item_with_status(user, organization, change_setup["from"])
    assert change_setup["action"](user, organization, price_item) is False


@pytest.mark.parametrize("change_setup", [
    {"from": "ApproveRequired",  "action": overwrite},
    {"from": "ReadyForDeletion", "action": overwrite},
    {"from": "Deleted",          "action": overwrite},
])
def test_status_change_fails_for_forbidden_transitions_in_overwrite(user, chain, chain_company, change_setup):
    price_item = create_item_with_status(user, chain, change_setup["from"])
    assert change_setup["action"](user, chain_company, price_item) is False


@pytest.mark.parametrize("change_setup", [
    {
        "from": "OnModeration",
        "to":   "ReadyForPublishing",
        "workers": ["ModerationSender", "ModerationReceiver", "ModerationMerger"],
    },
    {
        "from": "OnModeration",
        "to":   "Declined",
        "action": forbid_item,
        "workers": ["ModerationSender", "ModerationReceiver", "ModerationMerger"],
    },
    {
        "from": "ReadyForPublishing",
        "to":   "Published",
        "workers": ["Publisher"],
    },
    {
        "from": "ReadyForDeletion",
        "to":   "ReadyForDeletion",
        "workers": ["ItemsDeleter"],
    },
])
def test_status_change_for_background_processes(user, company, change_setup):
    price_item = create_item_with_status(user, company, change_setup["from"])
    if "action" in change_setup:
        change_setup["action"](price_item)
    async_processor.perform_work(change_setup["workers"])
    assert get_price_item_status(price_item) == change_setup["to"]
