import pytest
import lib.cleanweb as cleanweb

import maps.automotive.libs.large_tests.lib.db as db

from data_types.price_item import PriceItem
from data_types.moderation import Moderation

from lib.server import server
import lib.async_processor as async_processor


def test_simple_moderation_completed(user, company):
    price_item = PriceItem()
    prices = server.post_price(user, price_item, company) >> 200
    assert prices["moderation"]["status"] == "OnModeration"

    prices = server.get_prices(user, company) >> 200
    assert len(prices["items"]) == 1
    assert prices["items"][0]["moderation"]["status"] == "OnModeration"

    async_processor.perform_all_work()
    assert (server.get_prices(user, company) >> 200)["items"][0]["moderation"]["status"] in {
        "ReadyForPublishing",
        "Published",
    }

    title_moderation = Moderation.get_moderation(subject_type='Text', subject=price_item.title)
    assert title_moderation.status == 'Approved'
    assert title_moderation.start_time is not None
    assert title_moderation.end_time is not None
    assert title_moderation.start_time <= title_moderation.end_time


@pytest.mark.parametrize("config", [
    {"verdicts": [
        {"name": "clean_web_moderation_end", "synch": False}
    ], "status": "passed", "messages_count": 0},
    {"verdicts": [
        {"name": "text_toloka_obscene", "synch": False}
    ], "status": "declined", "messages_count": 1},
    {"verdicts": [
        {"name": "text_toloka_hard_violation", "synch": False},
        {"name": "clean_web_moderation_end", "synch": False}
    ], "status": "declined", "messages_count": 1},
    {"verdicts": [
        {"name": "text_toloka_is_bad", "synch": False},
        {"name": "media_toloka_ok", "synch": False},
        {"name": "object_offline_checks_required", "synch": False},
        {"name": "clean_web_moderation_end", "synch": False}
    ], "status": "passed", "messages_count": 0},
    {"verdicts": [
        {"name": "65432", "synch": False},  # invalid
        {"name": "text_toloka_threat", "synch": False},
        {"name": "text_toloka_threat_hard", "synch": False}
    ], "status": "declined", "messages_count": 1},
    {"verdicts": [
        {"name": "object_offline_checks_required", "synch": False}
    ], "status": "not_finished", "messages_count": 0},
    {"verdicts": [
        {"name": "1111", "synch": False},  # invalid
    ], "status": "not_finished", "messages_count": 0},
    {"verdicts": [
        {"name": "object_offline_checks_required", "synch": False},
        {"name": "text_auto_caps", "synch": False},
        {"name": "clean_web_moderation_end", "synch": False},
    ], "status": "passed", "messages_count": 0},
])
def test_moderation_check_verdicts(user, company, config):
    price_item = PriceItem()

    body = price_item.title
    messages = [{
        "body": body,
        "name": verdict["name"],
        "value": True,
        "synch": verdict["synch"]
    } for verdict in config["verdicts"]]

    cleanweb.set_response(messages) >> 200

    item = server.post_price(user, price_item, company) >> 200

    last_sync_time = None
    with db.get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT last_moderation_sync_time FROM items WHERE id=%s", (item["id"],))
            last_sync_time = cur.fetchone()[0]

            async_processor.perform_all_work()

            not_synch_count = len([v for v in config["verdicts"] if not v["synch"]])
            has_synch = 1 if next((v for v in config["verdicts"] if v["synch"]), None) is not None else 0

            cur.execute(
                """
                SELECT COUNT(*) FROM moderation_history
                WHERE moderation_id=(SELECT id FROM moderation WHERE subject=%s)
                """,
                (body,))
            assert int(cur.fetchone()[0]) == not_synch_count + has_synch

            if config["status"] != 'not_finished':
                cur.execute("SELECT last_moderation_sync_time > %s FROM items WHERE id=%s", (last_sync_time, item["id"]))
                assert bool(cur.fetchone()[0])

    items_status = lambda: (server.get_prices(user, company) >> 200)["items"][0]["moderation"]["status"]
    expected_statuses = {
        'passed': ("ReadyForPublishing", "Published"),
        'declined': ("Declined",),
        'not_finished': ("OnModeration",)
    }[config["status"]]

    async_processor.perform_all_work()
    assert items_status() in expected_statuses

    msg = (server.get_prices(user, company) >> 200)["items"][0]["moderation"]["message"]
    if config["messages_count"] > 0:
        assert msg
        assert len(msg.split(',')) == config["messages_count"]
    else:
        assert msg == ''
