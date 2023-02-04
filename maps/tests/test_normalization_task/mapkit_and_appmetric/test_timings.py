from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.common.helpers import dt
from maps_adv.statistics.beekeeper.lib.normalizer import NoNewData

pytestmark = [pytest.mark.asyncio, pytest.mark.mapkit]

APP_FILTER = {"ios_maps_build": 201}
DEPLOY_DATE = int(datetime(2022, 6, 21, 0, 0, tzinfo=timezone.utc).timestamp())
DEPLOY_DURATION = 50 * 24 * 60 * 60
DEPLOY_FINISH = DEPLOY_DATE + DEPLOY_DURATION


@pytest.mark.parametrize(
    ["datatube", "mapkittube"],
    [
        (
            [
                {"_timestamp": 100, "DeviceID": "AAA"},
                {"_timestamp": 110, "DeviceID": "BBB"},
                {"_timestamp": 120, "DeviceID": "CCC"},
                {"_timestamp": 130, "DeviceID": "DDD"},
            ],
            [],
        ),
        (
            [],
            [
                {"receive_timestamp": 100, "device_id": "AAA"},
                {"receive_timestamp": 110, "device_id": "BBB"},
                {"receive_timestamp": 120, "device_id": "CCC"},
                {"receive_timestamp": 130, "device_id": "DDD"},
            ],
        ),
        (
            [
                {"_timestamp": 100, "DeviceID": "AAA"},
                {"_timestamp": 110, "DeviceID": "BBB"},
            ],
            [
                {"receive_timestamp": 120, "device_id": "CCC"},
                {"receive_timestamp": 130, "device_id": "DDD"},
            ],
        ),
        (
            [
                {"_timestamp": 120, "DeviceID": "CCC"},
                {"_timestamp": 130, "DeviceID": "DDD"},
            ],
            [
                {"receive_timestamp": 100, "device_id": "AAA"},
                {"receive_timestamp": 110, "device_id": "BBB"},
            ],
        ),
    ],
)
async def test_normalizes_unprocessed(
    datatube, mapkittube, factory, task_factory, warden_client_mock
):
    factory.insert_source_datatube(datatube)
    factory.insert_source_mapkit(mapkittube)
    factory.insert_into_normalized(
        [
            {"receive_timestamp": 100, "device_id": "AAA"},
            {"receive_timestamp": 110, "device_id": "BBB"},
        ]
    )

    task = task_factory(app_filter=APP_FILTER)
    await task(warden_client=warden_client_mock)

    events_in_normalized = factory.get_all_normalized(0, 4)
    assert events_in_normalized == [
        (dt(100), "AAA"),
        (dt(110), "BBB"),
        (dt(120), "CCC"),
        (dt(130), "DDD"),
    ]


async def test_respects_min_pack_size_for_datatube(
    factory, task_factory, warden_client_mock
):
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

    task = task_factory(app_filter=APP_FILTER, min_packet_size=timedelta(seconds=60))
    with pytest.raises(NoNewData):
        await task(warden_client=warden_client_mock)


async def test_respects_min_pack_size_for_mapkittube(
    factory, task_factory, warden_client_mock
):
    timestamp = int(datetime.now().timestamp())
    factory.insert_source_mapkit(
        [
            {"receive_timestamp": timestamp - 20, "device_id": "AAA"},
            {"receive_timestamp": timestamp - 10, "device_id": "BBB"},
        ]
    )
    factory.insert_into_normalized(
        [{"receive_timestamp": timestamp - 20, "device_id": "AAA"}]
    )

    task = task_factory(app_filter=APP_FILTER, min_packet_size=timedelta(seconds=60))
    with pytest.raises(NoNewData):
        await task(warden_client=warden_client_mock)


@pytest.mark.parametrize(
    ["datatube", "mapkittube"],
    [
        (
            [
                {"_timestamp": 100, "DeviceID": "AAA"},
                {"_timestamp": 110, "DeviceID": "BBB"},
                {"_timestamp": 210, "DeviceID": "CCC"},
                {"_timestamp": 220, "DeviceID": "DDD"},
            ],
            [],
        ),
        (
            [],
            [
                {"receive_timestamp": 100, "device_id": "AAA"},
                {"receive_timestamp": 110, "device_id": "BBB"},
                {"receive_timestamp": 210, "device_id": "CCC"},
                {"receive_timestamp": 220, "device_id": "DDD"},
            ],
        ),
    ],
)
async def test_respects_max_pack_size(
    datatube, mapkittube, factory, task_factory, warden_client_mock
):
    factory.insert_source_datatube(datatube)
    factory.insert_source_mapkit(mapkittube)
    factory.insert_into_normalized([{"receive_timestamp": 100, "device_id": "AAA"}])

    task = task_factory(app_filter=APP_FILTER, max_packet_size=timedelta(seconds=60))
    await task(warden_client=warden_client_mock)

    events_in_normalized = factory.get_all_normalized(0, 4)
    assert events_in_normalized == [(dt(100), "AAA"), (dt(110), "BBB")]


async def test_respects_lag_for_datatube(factory, task_factory, warden_client_mock):
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

    task = task_factory(app_filter=APP_FILTER, lag=timedelta(10))
    with pytest.raises(NoNewData):
        await task(warden_client=warden_client_mock)


async def test_respects_lag_for_mapkittube(factory, task_factory, warden_client_mock):
    timestamp = int(datetime.now().timestamp())
    factory.insert_source_datatube(
        [
            {"receive_timestamp": timestamp - 25, "device_id": "AAA"},
            {"receive_timestamp": timestamp - 15, "device_id": "BBB"},
            {"receive_timestamp": timestamp - 5, "device_id": "CCC"},
        ]
    )
    factory.insert_into_normalized(
        [
            {"receive_timestamp": timestamp - 25, "device_id": "AAA"},
            {"receive_timestamp": timestamp - 15, "device_id": "BBB"},
        ]
    )

    task = task_factory(app_filter=APP_FILTER, lag=timedelta(10))
    with pytest.raises(NoNewData):
        await task(warden_client=warden_client_mock)


async def test_raises_if_sources_is_empty(factory, task_factory, warden_client_mock):
    factory.insert_into_normalized(
        [
            {"receive_timestamp": 100, "device_id": "AAA"},
            {"receive_timestamp": 110, "device_id": "BBB"},
        ]
    )

    task = task_factory(app_filter=APP_FILTER)
    with pytest.raises(NoNewData):
        await task(warden_client=warden_client_mock)


@pytest.mark.parametrize(
    ["datatube", "mapkittube"],
    [
        (
            [
                {"_timestamp": 100, "DeviceID": "AAA"},
                {"_timestamp": 110, "DeviceID": "BBB"},
            ],
            [],
        ),
        (
            [],
            [
                {"receive_timestamp": 100, "device_id": "AAA"},
                {"receive_timestamp": 110, "device_id": "BBB"},
            ],
        ),
    ],
)
async def test_transfers_events_if_normalized_is_empty(
    datatube, mapkittube, factory, task_factory, warden_client_mock
):
    factory.insert_source_datatube(datatube)
    factory.insert_source_mapkit(mapkittube)

    task = task_factory(app_filter=APP_FILTER)
    await task(warden_client=warden_client_mock)

    events_in_normalized = factory.get_all_normalized(0, 4)
    assert events_in_normalized == [(dt(100), "AAA"), (dt(110), "BBB")]


@pytest.mark.parametrize(
    ["datatube", "mapkittube"],
    [
        (
            [
                {"_timestamp": 100, "DeviceID": "AAA"},
                {"_timestamp": 110, "DeviceID": "BBB"},
            ],
            [],
        ),
        (
            [],
            [
                {"receive_timestamp": 100, "device_id": "AAA"},
                {"receive_timestamp": 110, "device_id": "BBB"},
            ],
        ),
    ],
)
async def test_raises_if_no_new_data(
    datatube, mapkittube, factory, task_factory, warden_client_mock
):
    factory.insert_source_datatube(datatube)
    factory.insert_source_mapkit(mapkittube)
    factory.insert_into_normalized(
        [
            {"receive_timestamp": 100, "device_id": "AAA"},
            {"receive_timestamp": 110, "device_id": "BBB"},
        ]
    )

    task = task_factory(app_filter=APP_FILTER)
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
async def test_ignores_data_gaps_for_datatube(
    factory, task_factory, warden_client_mock, gap_size
):
    factory.insert_source_datatube(
        [
            {"_timestamp": 100, "DeviceID": "AAA"},
            {"_timestamp": 110, "DeviceID": "BBB"},
            {"_timestamp": 110 + gap_size, "DeviceID": "CCC"},
            {"_timestamp": 110 + gap_size + 1, "DeviceID": "DDD"},
        ]
    )
    factory.insert_into_normalized(
        [
            {"receive_timestamp": 100, "device_id": "AAA"},
            {"receive_timestamp": 110, "device_id": "BBB"},
        ]
    )

    task = task_factory(
        app_filter=APP_FILTER,
        min_packet_size=timedelta(seconds=20),
        max_packet_size=timedelta(seconds=30),
        lag=timedelta(seconds=10),
    )
    await task(warden_client=warden_client_mock)

    events_in_normalized = factory.get_all_normalized(0, 4)
    assert events_in_normalized == [
        (dt(100), "AAA"),
        (dt(110), "BBB"),
        (dt(110 + gap_size), "CCC"),
        (dt(110 + gap_size + 1), "DDD"),
    ]


@pytest.mark.parametrize(
    "gap_size",
    [
        5,  # GAP_SIZE < lag < min_packet_size < max_packet_size
        15,  # lag < GAP_SIZE < min_packet_size < max_packet_size
        25,  # lag < min_packet_size < GAP_SIZE < max_packet_size
        35,  # lag < min_packet_size < max_packet_size < GAP_SIZE
    ],
)
async def test_ignores_data_gaps_for_mapkittube(
    factory, task_factory, warden_client_mock, gap_size
):
    factory.insert_source_mapkit(
        [
            {"receive_timestamp": 100, "device_id": "AAA"},
            {"receive_timestamp": 110, "device_id": "BBB"},
            {"receive_timestamp": 110 + gap_size, "device_id": "CCC"},
            {"receive_timestamp": 110 + gap_size + 1, "device_id": "DDD"},
        ]
    )
    factory.insert_into_normalized(
        [
            {"receive_timestamp": 100, "device_id": "AAA"},
            {"receive_timestamp": 110, "device_id": "BBB"},
        ]
    )

    task = task_factory(
        app_filter=APP_FILTER,
        min_packet_size=timedelta(seconds=20),
        max_packet_size=timedelta(seconds=30),
        lag=timedelta(seconds=10),
    )
    await task(warden_client=warden_client_mock)

    events_in_normalized = factory.get_all_normalized(0, 4)
    assert events_in_normalized == [
        (dt(100), "AAA"),
        (dt(110), "BBB"),
        (dt(110 + gap_size), "CCC"),
        (dt(110 + gap_size + 1), "DDD"),
    ]


@pytest.mark.parametrize(
    ["mapkittube", "normalized", "normalized_expected"],
    [
        (
            [
                {"receive_timestamp": 100, "event_group_id": "AAA", "event": "billboard.click"},
                {"receive_timestamp": 101, "event_group_id": "AAA", "event": "billboard.click"},
                {"receive_timestamp": 102, "event_group_id": "BBB", "event": "billboard.show"},
                {"receive_timestamp": 103, "event_group_id": "BBB", "event": "billboard.show"},
                {"receive_timestamp": 104, "event_group_id": "CCC", "event": "billboard.show", "campaign_id": 1},
                {"receive_timestamp": 105, "event_group_id": "CCC", "event": "billboard.show", "campaign_id": 1},
            ],
            [
                {"receive_timestamp": 95, "event_group_id": "aaa", "event_name": "BILLBOARD_TAP"},
            ],
            [
                ("aaa", "BILLBOARD_TAP"),
                ("AAA", "BILLBOARD_TAP"),
                ("AAA", "BILLBOARD_TAP"),
                ("BBB", "BILLBOARD_SHOW"),
                ("CCC", "BILLBOARD_SHOW"),
                ("CCC", "BILLBOARD_SHOW"),
            ],
        ),
        (
            [
                {"receive_timestamp": 100, "event_group_id": "AAA", "event": "billboard.show"},
                {"receive_timestamp": 101, "event_group_id": "AAA", "event": "billboard.show"},
                {"receive_timestamp": 102, "event_group_id": "AAA", "event": "billboard.click"},
                {"receive_timestamp": 103, "event_group_id": "AAA", "event": "billboard.click"},
                {"receive_timestamp": 104, "event_group_id": "BBB", "event": "billboard.show"},
                {"receive_timestamp": 105, "event_group_id": "BBB", "event": "billboard.show"},
                {"receive_timestamp": 106, "event_group_id": "BBB", "event": "billboard.click"},
                {"receive_timestamp": 107, "event_group_id": "BBB", "event": "billboard.click"},
            ],
            [
                {"receive_timestamp": 95, "event_group_id": "AAA", "event_name": "BILLBOARD_SHOW"},
                {"receive_timestamp": 95, "event_group_id": "BBB", "event_name": "BILLBOARD_TAP"},
            ],
            [
                ("AAA", "BILLBOARD_SHOW"),
                ("BBB", "BILLBOARD_TAP"),
                ("AAA", "BILLBOARD_TAP"),
                ("AAA", "BILLBOARD_TAP"),
                ("BBB", "BILLBOARD_SHOW"),
                ("BBB", "BILLBOARD_TAP"),
                ("BBB", "BILLBOARD_TAP"),
            ],
        ),
        (
            [
                {"receive_timestamp": 100, "event_group_id": "AAA", "event": "billboard.show"},
                {"receive_timestamp": 101, "event_group_id": "BBB", "event": "billboard.show"},
            ],
            [
                {"receive_timestamp": 85, "event_group_id": "AAA", "event_name": "BILLBOARD_SHOW"},
                {"receive_timestamp": 95, "event_group_id": "BBB", "event_name": "BILLBOARD_SHOW"},
            ],
            [
                ("AAA", "BILLBOARD_SHOW"),
                ("BBB", "BILLBOARD_SHOW"),
                ("AAA", "BILLBOARD_SHOW"),
            ],
        ),
    ],
)
async def test_deduplication(
    mapkittube, normalized, normalized_expected, factory, task_factory, warden_client_mock
):
    factory.insert_source_mapkit(mapkittube)
    factory.insert_into_normalized(normalized)

    task = task_factory(app_filter=APP_FILTER)
    await task(warden_client=warden_client_mock)

    events_in_normalized = factory.get_all_normalized(3, 1)
    assert events_in_normalized == normalized_expected


@pytest.mark.parametrize(
    ["mapkittube", "normalized", "normalized_expected"],
    [
        (
            [                                                                           # ` hash % M   time  need_filter
                {"receive_timestamp": DEPLOY_DATE - 11, "event_group_id": "24d4f42c"},  # `       5  > -11       0
                {"receive_timestamp": DEPLOY_DATE - 11, "event_group_id": "24d4f42c"},  # `       5  > -11       0
                {"receive_timestamp": DEPLOY_DATE - 10, "event_group_id": "3f900955"},  # `      15  > -10       0
                {"receive_timestamp": DEPLOY_DATE - 10, "event_group_id": "3f900955"},  # `      15  > -10       0
                {"receive_timestamp": DEPLOY_DATE + 10, "event_group_id": "24d4f42c"},  # `       5  <  10       1
                {"receive_timestamp": DEPLOY_DATE + 10, "event_group_id": "24d4f42c"},  # `       5  <  10       1
                {"receive_timestamp": DEPLOY_DATE + 11, "event_group_id": "3f900955"},  # `      15  >  11       0
                {"receive_timestamp": DEPLOY_DATE + 11, "event_group_id": "3f900955"},  # `      15  >  11       0
            ],
            [
                {"receive_timestamp": DEPLOY_DATE - 20, "event_group_id": "aaaaaaaa", "event_name": "BILLBOARD_TAP"},
            ],
            [
                ("aaaaaaaa"),
                ("24d4f42c"),
                ("24d4f42c"),
                ("3f900955"),
                ("3f900955"),
                ("24d4f42c"),
                ("3f900955"),
                ("3f900955"),
            ],
        ),
        (
            [                                                                             # ` hash % M    time     need_filter
                {"receive_timestamp": DEPLOY_FINISH - 12, "event_group_id": "3bf37091"},  # `  M - 15  <  M - 12       1
                {"receive_timestamp": DEPLOY_FINISH - 12, "event_group_id": "3bf37091"},  # `  M - 15  <  M - 12       1
                {"receive_timestamp": DEPLOY_FINISH - 11, "event_group_id": "1a625efe"},  # `   M - 5  >  M - 11       0
                {"receive_timestamp": DEPLOY_FINISH - 11, "event_group_id": "1a625efe"},  # `   M - 5  >  M - 11       0
                {"receive_timestamp": DEPLOY_FINISH - 10, "event_group_id": "3f900955"},  # `      15  <  M - 10       1
                {"receive_timestamp": DEPLOY_FINISH - 10, "event_group_id": "3f900955"},  # `      15  <  M - 10       1
                {"receive_timestamp": DEPLOY_FINISH + 10, "event_group_id": "a89015dc"},  # `  M - 15  <  M + 10       1
                {"receive_timestamp": DEPLOY_FINISH + 10, "event_group_id": "a89015dc"},  # `  M - 15  <  M + 10       1
                {"receive_timestamp": DEPLOY_FINISH + 11, "event_group_id": "1a625efe"},  # `   M - 5  <  M + 11       1
                {"receive_timestamp": DEPLOY_FINISH + 11, "event_group_id": "1a625efe"},  # `   M - 5  <  M + 11       1
                {"receive_timestamp": DEPLOY_FINISH + 12, "event_group_id": "bb5726d2"},  # `      15  <  M + 12       1
                {"receive_timestamp": DEPLOY_FINISH + 12, "event_group_id": "bb5726d2"},  # `      15  <  M + 12       1
            ],
            [
                {"receive_timestamp": DEPLOY_FINISH - 20, "event_group_id": "aaaaaaaa", "event_name": "BILLBOARD_TAP"},
            ],
            [
                ("aaaaaaaa"),
                ("3bf37091"),
                ("1a625efe"),
                ("1a625efe"),
                ("3f900955"),
                ("a89015dc"),
                ("1a625efe"),
                ("bb5726d2"),
            ],
        ),
    ],
)
async def test_deduplication_deploy(
    mapkittube, normalized, normalized_expected, factory, task_factory, warden_client_mock
):
    for item in mapkittube:
        item.update({"event": "billboard.show", "campaign_id": 1})

    factory.insert_source_mapkit(mapkittube)
    factory.insert_into_normalized(normalized)

    task = task_factory(app_filter=APP_FILTER, testing_future_events=True)
    await task(warden_client=warden_client_mock)

    events_in_normalized = factory.get_all_normalized(3)
    assert events_in_normalized == normalized_expected
