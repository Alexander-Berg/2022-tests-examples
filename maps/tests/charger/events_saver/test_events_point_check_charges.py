from datetime import datetime
from decimal import Decimal

import pytest

from maps_adv.stat_tasks_starter.lib.base.exceptions import UnexpectedNaiveDateTime
from maps_adv.stat_tasks_starter.lib.charger.events_saver import EventsPoint
from maps_adv.stat_tasks_starter.tests.tools import dt, make_event

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def setup_charged_table(ch_client):
    charged_events = [
        make_event(*args)
        for args in (
            (1, 1000, "pin.show", Decimal("3")),
            (1, 1010, "pin.show", Decimal("3")),
            (1, 1020, "pin.show", Decimal("3")),
            (2, 1010, "pin.show", Decimal("3")),
            (2, 2000, "pin.show", Decimal("3")),
            (3, 3000, "pin.show", Decimal("3")),
        )
    ]
    ch_client.execute("INSERT INTO stat.accepted_sample VALUES", charged_events)


@pytest.fixture
def events_point(loop):
    return EventsPoint(
        normalized_table="normalized_sample",
        charged_table="accepted_sample",
        database="stat",
        host="localhost",
        port=9001,
    )


@pytest.mark.parametrize(
    "timing_from, timing_to",
    [
        (dt(1000), dt(1100)),
        (dt(1000), dt(1000)),
        (dt(2000), dt(2100)),
        (dt(2900), dt(3000)),
        (dt(999), dt(4000)),
    ],
)
async def test_returns_true_if_charged_events_in_period(
    timing_from, timing_to, events_point, ch_client
):
    got = await events_point.check_charging_withing_period(timing_from, timing_to)

    assert got is True


@pytest.mark.parametrize(
    "timing_from, timing_to",
    [(dt(3001), dt(4000)), (dt(2001), dt(2999)), (dt(100), dt(999))],
)
async def test_returns_false_if_no_charged_events_in_period(
    timing_from, timing_to, events_point, ch_client
):
    got = await events_point.check_charging_withing_period(timing_from, timing_to)

    assert got is False


@pytest.mark.parametrize(
    "timing_from, timing_to",
    (
        (dt("2019-03-27 19:57:59"), datetime(2019, 3, 27, 20, 4, 55)),
        (datetime(2019, 3, 27, 19, 57, 59), dt("2019-03-27 20:4:55")),
        (datetime(2019, 3, 27, 19, 57, 59), datetime(2019, 3, 27, 20, 4, 55)),
    ),
)
async def test_raises_for_naive_datetime(timing_from, timing_to, events_point):
    with pytest.raises(UnexpectedNaiveDateTime):
        await events_point.check_charging_withing_period(timing_from, timing_to)
