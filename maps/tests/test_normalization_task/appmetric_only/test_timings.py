from datetime import datetime, timedelta

import pytest

from maps_adv.common.helpers import dt
from maps_adv.statistics.beekeeper.lib.normalizer import NoNewData

pytestmark = [pytest.mark.asyncio]


async def test_normalizes_unprocessed(factory, task_factory, warden_client_mock):
    timestamp = int(datetime.now().timestamp())
    factory.insert_source_datatube(
        [
            {"_timestamp": timestamp - 90, "DeviceID": "AAA"},
            {"_timestamp": timestamp - 80, "DeviceID": "BBB"},
            {"_timestamp": timestamp - 70, "DeviceID": "CCC"},
            {"_timestamp": timestamp - 60, "DeviceID": "DDD"},
        ]
    )
    factory.insert_into_normalized(
        [
            {"receive_timestamp": timestamp - 90, "device_id": "AAA"},
            {"receive_timestamp": timestamp - 80, "device_id": "BBB"},
        ]
    )

    task = task_factory()
    await task(warden_client=warden_client_mock)

    events_in_normalized = factory.get_all_normalized(0, 4)
    assert events_in_normalized == [
        (dt(timestamp - 90), "AAA"),
        (dt(timestamp - 80), "BBB"),
        (dt(timestamp - 70), "CCC"),
        (dt(timestamp - 60), "DDD"),
    ]


async def test_respects_min_pack_size(factory, task_factory, warden_client_mock):
    timestamp = int(datetime.now().timestamp())
    factory.insert_source_datatube(
        [
            {"_timestamp": timestamp - 20, "DeviceID": "AAA"},
            {"_timestamp": timestamp - 10, "DeviceID": "BBB"},
        ]
    )
    factory.insert_into_normalized(
        [{"receive_timestamp": timestamp - 20, "device_id": "AAA"}]
    )

    task = task_factory(min_packet_size=timedelta(seconds=60))
    with pytest.raises(NoNewData):
        await task(warden_client=warden_client_mock)


async def test_respects_max_pack_size(factory, task_factory, warden_client_mock):
    timestamp = int(datetime.now().timestamp())
    factory.insert_source_datatube(
        [
            {"_timestamp": timestamp - 130, "DeviceID": "AAA"},
            {"_timestamp": timestamp - 120, "DeviceID": "BBB"},
            {"_timestamp": timestamp - 20, "DeviceID": "CCC"},
            {"_timestamp": timestamp - 10, "DeviceID": "DDD"},
        ]
    )
    factory.insert_into_normalized(
        [{"receive_timestamp": timestamp - 130, "device_id": "AAA"}]
    )

    task = task_factory(max_packet_size=timedelta(seconds=60))
    await task(warden_client=warden_client_mock)

    events_in_normalized = factory.get_all_normalized(0, 4)
    assert events_in_normalized == [
        (dt(timestamp - 130), "AAA"),
        (dt(timestamp - 120), "BBB"),
    ]


async def test_respects_lag(factory, task_factory, warden_client_mock):
    timestamp = int(datetime.now().timestamp())
    factory.insert_source_datatube(
        [
            {"_timestamp": timestamp - 25, "DeviceID": "AAA"},
            {"_timestamp": timestamp - 15, "DeviceID": "BBB"},
            {"_timestamp": timestamp - 5, "DeviceID": "CCC"},
        ]
    )
    factory.insert_into_normalized(
        [
            {"receive_timestamp": timestamp - 25, "device_id": "AAA"},
            {"receive_timestamp": timestamp - 15, "device_id": "BBB"},
        ]
    )

    task = task_factory(lag=timedelta(10))
    with pytest.raises(NoNewData):
        await task(warden_client=warden_client_mock)


async def test_raises_if_source_is_empty(factory, task_factory, warden_client_mock):
    timestamp = int(datetime.now().timestamp())
    factory.insert_into_normalized(
        [
            {"receive_timestamp": timestamp - 90, "device_id": "AAA"},
            {"receive_timestamp": timestamp - 80, "device_id": "BBB"},
        ]
    )

    task = task_factory()
    with pytest.raises(NoNewData):
        await task(warden_client=warden_client_mock)


async def test_transfers_events_if_normalized_is_empty(
    factory, task_factory, warden_client_mock
):
    timestamp = int(datetime.now().timestamp())
    factory.insert_source_datatube(
        [
            {"_timestamp": timestamp - 90, "DeviceID": "AAA"},
            {"_timestamp": timestamp - 80, "DeviceID": "BBB"},
        ]
    )

    task = task_factory()
    await task(warden_client=warden_client_mock)

    events_in_normalized = factory.get_all_normalized(0, 4)
    assert events_in_normalized == [
        (dt(timestamp - 90), "AAA"),
        (dt(timestamp - 80), "BBB"),
    ]


async def test_raises_if_no_new_data(factory, task_factory, warden_client_mock):
    timestamp = int(datetime.now().timestamp())
    factory.insert_source_datatube(
        [
            {"_timestamp": timestamp - 90, "DeviceID": "AAA"},
            {"_timestamp": timestamp - 80, "DeviceID": "BBB"},
        ]
    )
    factory.insert_into_normalized(
        [
            {"receive_timestamp": timestamp - 90, "device_id": "AAA"},
            {"receive_timestamp": timestamp - 80, "device_id": "BBB"},
        ]
    )

    task = task_factory()
    with pytest.raises(NoNewData):
        await task(warden_client=warden_client_mock)


@pytest.mark.parametrize(
    "gap_size",
    [
        5,  # GAP_SIZE < lag < min_packet_size < max_packet_size
        15,  # lag < GAP_SIZE < min_packet_size < max_packet_size
        25,  # lag < min_packet_size < GAP_SIZE < max_packet_size
        35,  # lag < min_packet_size < max_packet_size < GAP_SIZE
    ],
)
async def test_ignores_data_gaps(factory, task_factory, warden_client_mock, gap_size):
    timestamp = int(datetime.now().timestamp())
    factory.insert_source_datatube(
        [
            {"_timestamp": timestamp - 90, "DeviceID": "AAA"},
            {"_timestamp": timestamp - 80, "DeviceID": "BBB"},
            {"_timestamp": timestamp - 80 + gap_size, "DeviceID": "CCC"},
            {"_timestamp": timestamp - 80 + gap_size + 1, "DeviceID": "DDD"},
        ]
    )
    factory.insert_into_normalized(
        [
            {"receive_timestamp": timestamp - 90, "device_id": "AAA"},
            {"receive_timestamp": timestamp - 80, "device_id": "BBB"},
        ]
    )

    task = task_factory(
        min_packet_size=timedelta(seconds=20),
        max_packet_size=timedelta(seconds=30),
        lag=timedelta(seconds=10),
    )
    await task(warden_client=warden_client_mock)

    events_in_normalized = factory.get_all_normalized(0, 4)
    assert events_in_normalized == [
        (dt(timestamp - 90), "AAA"),
        (dt(timestamp - 80), "BBB"),
        (dt(timestamp - 80 + gap_size), "CCC"),
        (dt(timestamp - 80 + gap_size + 1), "DDD"),
    ]
