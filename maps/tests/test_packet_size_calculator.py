from datetime import timedelta
from decimal import Decimal

import pytest

from maps_adv.common.helpers import dt
from maps_adv.statistics.beekeeper.lib.packet_size_calculator import (
    NoNewNormalizedEventsFound,
    PacketSizeCalculator,
)
from maps_adv.statistics.beekeeper.tests.tools import dt_timestamp

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def calculator():
    return PacketSizeCalculator(
        time_lag=timedelta(seconds=30),
        min_packet_size=timedelta(seconds=60),
        max_packet_size=timedelta(seconds=1200),
        host="localhost",
        port=9001,
    )


@pytest.fixture(autouse=True)
def setup_normalized_events_table(factory):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 4356,
                "receive_timestamp": dt_timestamp("2019-05-05 14:23:00"),
            },
            {
                "campaign_id": 1242,
                "receive_timestamp": dt_timestamp("2019-05-05 14:20:00"),
            },
            {
                "campaign_id": 1242,
                "receive_timestamp": dt_timestamp("2019-05-05 14:19:00"),
            },
            {
                "campaign_id": 1242,
                "receive_timestamp": dt_timestamp("2019-05-05 14:17:00"),
            },
            {
                "campaign_id": 4356,
                "receive_timestamp": dt_timestamp("2019-05-05 04:23:00"),
            },
            {
                "campaign_id": 1242,
                "receive_timestamp": dt_timestamp("2019-05-05 04:20:00"),
            },
            {
                "campaign_id": 1242,
                "receive_timestamp": dt_timestamp("2019-05-05 04:19:00"),
            },
            {
                "campaign_id": 1242,
                "receive_timestamp": dt_timestamp("2019-05-05 04:17:00"),
            },
            {
                "campaign_id": 3456,
                "receive_timestamp": dt_timestamp("2019-05-05 04:15:00"),
            },
            {
                "campaign_id": 4356,
                "receive_timestamp": dt_timestamp("2019-05-05 04:05:00"),
            },
            {
                "campaign_id": 1242,
                "receive_timestamp": dt_timestamp("2019-05-05 03:55:00"),
            },
            {
                "campaign_id": 1242,
                "receive_timestamp": dt_timestamp("2019-05-05 03:47:00"),
            },
            {
                "campaign_id": 3456,
                "receive_timestamp": dt_timestamp("2019-05-05 03:45:00"),
            },
            {
                "campaign_id": 1242,
                "receive_timestamp": dt_timestamp("2019-05-05 03:40:00"),
            },
        ]
    )


@pytest.mark.freeze_time(dt("2019-05-05 20:00:00"))
async def test_raises_if_there_are_no_new_normalized_events(factory, calculator):
    factory.insert_into_processed(
        [
            {
                "campaign_id": 1242,
                "receive_timestamp": dt_timestamp("2019-05-05 14:23:00"),
                "cost": Decimal(10),
            },
            {
                "campaign_id": 3456,
                "receive_timestamp": dt_timestamp("2019-05-05 04:15:00"),
                "cost": Decimal(10),
            },
        ]
    )

    with pytest.raises(NoNewNormalizedEventsFound):
        await calculator()


@pytest.mark.parametrize(
    "now, expected_start, expected_end",
    (
        # min packet range
        [
            dt("2019-05-05 04:20:30"),
            dt("2019-05-05 04:19:00"),
            dt("2019-05-05 04:20:00"),
        ],
        # max packet range
        [
            dt("2019-05-05 04:50:00"),
            dt("2019-05-05 04:19:00"),
            dt("2019-05-05 04:23:00"),
        ],
    ),
)
async def test_returns_expected_packet_size(
    now, expected_start, expected_end, calculator, factory, freezer
):
    freezer.move_to(now)

    factory.insert_into_processed(
        [
            {
                "campaign_id": 1242,
                "receive_timestamp": dt_timestamp("2019-05-05 04:17:00"),
                "cost": Decimal(10),
            },
            {
                "campaign_id": 3456,
                "receive_timestamp": dt_timestamp("2019-05-05 04:15:00"),
                "cost": Decimal(10),
            },
        ]
    )

    got = await calculator()

    assert got == {"packet_start": expected_start, "packet_end": expected_end}


@pytest.mark.freeze_time(dt("2019-05-05 04:10:00"))
async def test_does_not_violate_hour_boundaries(calculator, factory):
    factory.insert_into_processed(
        [
            {
                "campaign_id": 1242,
                "receive_timestamp": dt_timestamp("2019-05-05 03:45:00"),
                "cost": Decimal(10),
            },
            {
                "campaign_id": 3456,
                "receive_timestamp": dt_timestamp("2019-05-05 03:40:00"),
                "cost": Decimal(10),
            },
        ]
    )

    got = await calculator()

    assert got == {
        "packet_start": dt("2019-05-05 03:47:00"),
        "packet_end": dt("2019-05-05 03:55:00"),
    }


@pytest.mark.freeze_time(dt("2019-05-05 14:27:30"))
async def test_ignores_big_gaps_in_data(calculator, factory):
    factory.insert_into_processed(
        [
            {
                "campaign_id": 1242,
                "receive_timestamp": dt_timestamp("2019-05-05 04:23:00"),
                "cost": Decimal(10),
            },
            {
                "campaign_id": 3456,
                "receive_timestamp": dt_timestamp("2019-05-05 04:20:00"),
                "cost": Decimal(10),
            },
        ]
    )

    got = await calculator()

    assert got == {
        "packet_start": dt("2019-05-05 14:17:00"),
        "packet_end": dt("2019-05-05 14:23:00"),
    }


@pytest.mark.freeze_time(dt("2019-05-05 04:10:00"))
async def test_returns_packet_size_if_nothing_processed(calculator):
    got = await calculator()

    assert got == {
        "packet_start": dt("2019-05-05 03:40:00"),
        "packet_end": dt("2019-05-05 03:55:00"),
    }


@pytest.mark.freeze_time(dt("2019-05-05 04:06:29"))
async def test_returns_none_if_violates_min_packet_size(calculator, factory):
    factory.insert_into_processed(
        [
            {
                "campaign_id": 1242,
                "receive_timestamp": dt_timestamp("2019-05-05 04:01:00"),
                "cost": Decimal(10),
            },
            {
                "campaign_id": 3456,
                "receive_timestamp": dt_timestamp("2019-05-05 04:00:00"),
                "cost": Decimal(10),
            },
        ]
    )

    got = await calculator()

    assert got is None


@pytest.mark.freeze_time(dt("2019-05-05 03:59:40"))
async def test_returns_none_if_violates_hour_end_buffer(calculator, factory):
    factory.insert_into_processed(
        [
            {
                "campaign_id": 1242,
                "receive_timestamp": dt_timestamp("2019-05-05 03:45:00"),
                "cost": Decimal(10),
            },
            {
                "campaign_id": 3456,
                "receive_timestamp": dt_timestamp("2019-05-05 03:40:00"),
                "cost": Decimal(10),
            },
        ]
    )

    got = await calculator()

    assert got is None
