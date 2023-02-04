from decimal import Decimal

import pytest

from maps_adv.stat_tasks_starter.lib.collector import Collector
from maps_adv.stat_tasks_starter.tests.tools import dt, make_event

pytestmark = [pytest.mark.asyncio]

charged_events = [
    [1, 100, "pin.show", Decimal("10"), "1"],
    [1, 200, "pin.show", Decimal("10"), "2"],
    [1, 300, "pin.show", Decimal("10"), "3"],
]

normalized_events = [
    # group 1
    [1, 40, "pin.openSite", None, "unexpected0"],
    [1, 100, "pin.show", None, "1"],
    [1, 130, "pin.click", None, "1"],
    [1, 140, "pin.openSite", None, "1"],
    # group 2
    [1, 200, "pin.show", None, "2"],
    [1, 210, "pin.click", None, "2"],
    [1, 220, "pin.click", None, "unexpected1"],
    [1, 240, "pin.openSite", None, "2"],
    # group 3
    [1, 300, "pin.show", None, "3"],
    [1, 330, "pin.tap", None, "3"],
    [1, 400, "pin.tap", None, "unexpected2"],
    [1, 410, "pin.tap", None, "unexpected3"],
    [1, 430, "pin.call", None, "3"],
]


@pytest.fixture
def setup_table(ch_client):
    def _setup(table: str, events: list):
        events = [make_event(*e) for e in events]
        ch_client.execute(f"insert into {table} values", events)

    return _setup


@pytest.fixture
async def make_collector():
    def _make(**kwargs):
        settings = dict(
            host="localhost",
            port=9001,
            user="default",
            password="",
            database="stat",
            normalized_table="normalized_sample",
            accepted_groups_table="accepted_sample_event_group_ids",
            accepted_table="accepted_sample",
            secure=False,
            ca_certs=None,
            lag=50,
        )
        settings.update(kwargs)

        return Collector(**settings)

    return _make


@pytest.mark.parametrize(
    "timings, expected",
    (
        [(dt(10), dt(90)), []],
        [
            (dt(200), dt(260)),
            [
                [1, 210, "pin.click", Decimal("0"), "2"],
                [1, 240, "pin.openSite", Decimal("0"), "2"],
            ],
        ],
        [(dt(300), dt(340)), [[1, 330, "pin.tap", Decimal("0"), "3"]]],
        [(dt(320), dt(340)), [[1, 330, "pin.tap", Decimal("0"), "3"]]],
        [
            (dt(320), dt(440)),
            [
                [1, 330, "pin.tap", Decimal("0"), "3"],
                [1, 430, "pin.call", Decimal("0"), "3"],
            ],
        ],
        [
            (dt(0), dt(500)),
            [
                [1, 130, "pin.click", Decimal("0"), "1"],
                [1, 140, "pin.openSite", Decimal("0"), "1"],
                [1, 210, "pin.click", Decimal("0"), "2"],
                [1, 240, "pin.openSite", Decimal("0"), "2"],
                [1, 330, "pin.tap", Decimal("0"), "3"],
                [1, 430, "pin.call", Decimal("0"), "3"],
            ],
        ],
    ),
)
async def test_will_transfer_only_events_for_already_charged(
    timings, expected, setup_table, ch_client, make_collector
):
    setup_table("stat.accepted_sample", charged_events)
    setup_table("stat.normalized_sample", normalized_events)
    collector = make_collector()

    await collector(*timings)

    in_db = ch_client.execute("SELECT * FROM stat.accepted_sample")
    assert set(in_db) == {make_event(*a) for a in expected + charged_events}


@pytest.mark.parametrize(
    "lag, timings, expected",
    (
        [0, (dt(300), dt(340)), [[1, 330, "pin.tap", Decimal("0"), "3"]]],
        [0, (dt(320), dt(340)), []],
        [20, (dt(320), dt(340)), [[1, 330, "pin.tap", Decimal("0"), "3"]]],
        [20, (dt(400), dt(450)), []],
        [100, (dt(400), dt(450)), [[1, 430, "pin.call", Decimal("0"), "3"]]],
    ),
)
async def test_event_group_matching_depends_on_lag_size(
    lag, timings, expected, setup_table, ch_client, make_collector
):
    setup_table("stat.accepted_sample", charged_events)
    setup_table("stat.normalized_sample", normalized_events)
    collector = make_collector(lag=lag)

    await collector(*timings)

    in_db = ch_client.execute("SELECT * FROM stat.accepted_sample")
    assert set(in_db) == {make_event(*a) for a in expected + charged_events}
