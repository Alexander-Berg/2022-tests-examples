import json
import logging
from decimal import Decimal
from operator import itemgetter

import pytest

from maps_adv.common.helpers import dt
from maps_adv.statistics.beekeeper.lib.steps.base import FreeEventProcessingMode

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def common_input_data():
    return {
        "packet_start": dt("2000-02-02 01:10:00"),
        "packet_end": dt("2000-02-02 01:20:00"),
        "billing_applied": True,
        "orders": [
            {
                "order_id": 101,
                "balance": Decimal("300"),
                "amount_to_bill": Decimal("5"),
                "billing_success": True,
                "campaigns": [
                    {
                        "campaign_id": 11,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW", "BILLBOARD_TAP"],
                        "free_event_processing_mode": FreeEventProcessingMode.ONLY_IF_PAID_PRESENT,  # noqa: E501
                        "paid_events_count": 2,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 2,
                        "last_paid_event_cost": Decimal("0.5"),
                    }
                ],
            },
            {
                "order_id": 201,
                "balance": Decimal("300"),
                "amount_to_bill": Decimal("5"),
                "billing_success": True,
                "campaigns": [
                    {
                        "campaign_id": 21,
                        "billing_type": "cpa",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": [
                            "ACTION_CALL",
                            "ACTION_MAKE_ROUTE",
                            "ACTION_SEARCH",
                            "ACTION_OPEN_SITE",
                            "ACTION_OPEN_APP",
                            "ACTION_SAVE_OFFER",
                        ],
                        "free_event_processing_mode": FreeEventProcessingMode.ALL_EVENTS,  # noqa: E501
                        "paid_events_count": 2,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 2,
                        "last_paid_event_cost": Decimal("0.5"),
                    }
                ],
            },
        ],
    }


async def test_returns_input_data_unmodified(
    free_events_porter_step, common_input_data
):
    result = await free_events_porter_step.run(common_input_data)

    assert result == common_input_data


async def test_adds_events_to_processed_if_corresponding_paid_exist_in_same_packet(
    factory, free_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "campaign_id": 11,
                "device_id": "BBB",
                "event_name": "ACTION_MAKE_ROUTE",
                "event_group_id": "evgrid2",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "campaign_id": 11,
                "device_id": "CCC",
                "event_name": "ACTION_SEARCH",
                "event_group_id": "evgrid3",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:10:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:11:30"),
                "campaign_id": 11,
                "device_id": "BBB",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid2",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:20:00"),
                "campaign_id": 11,
                "device_id": "CCC",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid3",
            },
        ]
    )

    await free_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:10:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:11:30"), "BILLBOARD_TAP", "BBB"),
        (dt("2000-02-02 01:12:00"), "ACTION_CALL", "AAA"),
        (dt("2000-02-02 01:13:00"), "ACTION_MAKE_ROUTE", "BBB"),
        (dt("2000-02-02 01:14:00"), "ACTION_SEARCH", "CCC"),
        (dt("2000-02-02 01:20:00"), "BILLBOARD_SHOW", "CCC"),
    ]


async def test_adds_events_to_processed_if_corresponding_paid_exist_earlier_than_packet_start_within_threshold(  # noqa: E501
    factory, free_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:10:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:20:00"),
                "campaign_id": 11,
                "device_id": "BBB",
                "event_name": "ACTION_MAKE_ROUTE",
                "event_group_id": "evgrid2",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:09:30"),
                "campaign_id": 11,
                "device_id": "BBB",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid2",
            },
        ]
    )

    # threshold is 30 minutes it tests
    await free_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:05:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:09:30"), "BILLBOARD_TAP", "BBB"),
        (dt("2000-02-02 01:10:00"), "ACTION_CALL", "AAA"),
        (dt("2000-02-02 01:20:00"), "ACTION_MAKE_ROUTE", "BBB"),
    ]


async def test_not_adds_events_to_processed_if_corresponding_paid_exist_earlier_than_packet_start_not_withing_threshold(  # noqa: E501
    factory, free_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:17:00"),
                "campaign_id": 11,
                "device_id": "BBB",
                "event_name": "ACTION_MAKE_ROUTE",
                "event_group_id": "evgrid2",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 00:37:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 00:39:30"),
                "campaign_id": 11,
                "device_id": "BBB",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid2",
            },
        ]
    )

    # threshold is 30 minutes it tests
    await free_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 00:37:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 00:39:30"), "BILLBOARD_TAP", "BBB"),
    ]


async def test_not_adds_events_to_processed_if_corresponding_paid_not_exist_only_if_paid_present_mode(  # noqa: E501
    factory, free_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            }
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid3",
            }
        ]
    )

    await free_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:05:00"), "BILLBOARD_SHOW", "AAA")
    ]


async def test_adds_events_to_processed_if_corresponding_paid_not_exist_all_events_mode(
    factory, free_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "campaign_id": 21,
                "device_id": "AAA",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid1",
            }
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 21,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid3",
            }
        ]
    )

    await free_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:05:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:13:00"), "BILLBOARD_TAP", "AAA"),
    ]


async def test_respects_packet_bounds(
    factory, free_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:09:00"),
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            },
            {
                "campaign_id": 11,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:21:00"),
                "event_name": "ACTION_MAKE_ROUTE",
                "event_group_id": "evgrid2",
            },
            {
                "campaign_id": 21,
                "receive_timestamp": dt("2000-02-02 01:08:00"),
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid3",
            },
            {
                "campaign_id": 21,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:31:00"),
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid4",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:08:00"),
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:11:00"),
                "device_id": "BBB",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid2",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:07:00"),
                "device_id": "CCC",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid3",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:10:00"),
                "device_id": "DDD",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid4",
            },
        ]
    )

    await free_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:07:00"), "BILLBOARD_SHOW", "CCC"),
        (dt("2000-02-02 01:08:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:10:00"), "BILLBOARD_TAP", "DDD"),
        (dt("2000-02-02 01:11:00"), "BILLBOARD_TAP", "BBB"),
    ]


async def test_add_all_events_with_equal_event_group_ids(
    factory, free_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "campaign_id": 11,
                "device_id": "BBB",
                "event_name": "ACTION_MAKE_ROUTE",
                "event_group_id": "evgrid1",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            }
        ]
    )

    await free_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:05:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:12:00"), "ACTION_CALL", "AAA"),
        (dt("2000-02-02 01:13:00"), "ACTION_MAKE_ROUTE", "BBB"),
    ]


@pytest.mark.parametrize(
    ("field", "field_num", "value1", "value2"),
    [
        ("event_name", 1, "ACTION_SEARCH", "ACTION_OPEN_SITE"),
        ("event_group_id", 3, "grpid1", "grpid2"),
        ("device_id", 4, "deviceid1uid", "deviceid2uid"),
        ("application", 5, "NAVIGATOR", "MOBILE_MAPS"),
        ("app_platform", 6, "ANDROID", "IOS"),
        ("app_version_name", 7, "1.2.3", "3.4.5"),
        ("app_build_number", 8, 456, 678),
        ("user_latitude", 9, Decimal("55.718732876"), Decimal("37.401515797")),
        ("user_longitude", 10, Decimal("37.401515797"), Decimal("55.718732876")),
        ("place_id", 11, "altay:123", "bb:345"),
        ("_normalization_metadata", 14, '{"json1": "data1"}', '{"json2": "data2"}'),
    ],
)
async def test_copies_fields_from_normalized_only_if_paid_present(
    factory,
    free_events_porter_step,
    common_input_data,
    field,
    field_num,
    value1,
    value2,
):
    event1 = {
        "receive_timestamp": dt("2000-02-02 01:13:01"),
        "campaign_id": 11,
        "event_name": "ACTION_CALL",
        "event_group_id": "grpid1",
    }
    event1[field] = value1
    event2 = {
        "receive_timestamp": dt("2000-02-02 01:13:02"),
        "campaign_id": 11,
        "event_name": "ACTION_CALL",
        "event_group_id": "grpid2",
    }
    event2[field] = value2

    factory.insert_into_normalized([event1, event2])
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:08:00"),
                "campaign_id": 11,
                "device_id": "EXCLUDE",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "grpid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:11:00"),
                "campaign_id": 11,
                "device_id": "EXCLUDE",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "grpid2",
            },
        ]
    )

    await free_events_porter_step.run(common_input_data)

    processed_events = factory.get_all_processed()
    free_events = list(filter(lambda ev: ev[4] != "EXCLUDE", processed_events))

    assert len(free_events) == 2
    assert free_events[0][field_num] == value1
    assert free_events[1][field_num] == value2


@pytest.mark.parametrize(
    ("field", "field_num", "value1", "value2"),
    [
        ("event_name", 1, "BILLBOARD_TAP", "BILLBOARD_SHOW"),
        ("event_group_id", 3, "grpid1", "grpid2"),
        ("device_id", 4, "deviceid1uid", "deviceid2uid"),
        ("application", 5, "NAVIGATOR", "MOBILE_MAPS"),
        ("app_platform", 6, "ANDROID", "IOS"),
        ("app_version_name", 7, "1.2.3", "3.4.5"),
        ("app_build_number", 8, 456, 678),
        ("user_latitude", 9, Decimal("55.718732876"), Decimal("37.401515797")),
        ("user_longitude", 10, Decimal("37.401515797"), Decimal("55.718732876")),
        ("place_id", 11, "altay:123", "bb:345"),
        ("_normalization_metadata", 14, '{"json1": "data1"}', '{"json2": "data2"}'),
    ],
)
async def test_copies_fields_from_normalized_all_events(
    factory,
    free_events_porter_step,
    common_input_data,
    field,
    field_num,
    value1,
    value2,
):
    event1 = {
        "receive_timestamp": dt("2000-02-02 01:13:03"),
        "campaign_id": 21,
        "event_name": "BILLBOARD_SHOW",
        "event_group_id": "grpid1",
    }
    event1[field] = value1
    event2 = {
        "receive_timestamp": dt("2000-02-02 01:13:04"),
        "campaign_id": 21,
        "event_name": "BILLBOARD_SHOW",
        "event_group_id": "grpid2",
    }
    event2[field] = value2

    factory.insert_into_normalized([event1, event2])

    await free_events_porter_step.run(common_input_data)

    processed_events = factory.get_all_processed()
    free_events = list(filter(lambda ev: ev[4] != "EXCLUDE", processed_events))

    assert len(free_events) == 2
    assert free_events[0][field_num] == value1
    assert free_events[1][field_num] == value2


@pytest.mark.parametrize(
    "receive_timestamp", [dt("2000-02-02 01:13:00"), dt("2000-02-02 01:14:00")]
)
async def test_copies_receive_timestamp(
    factory, free_events_porter_step, common_input_data, receive_timestamp
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": receive_timestamp,
                "campaign_id": 11,
                "event_name": "ACTION_SEARCH",
                "event_group_id": "grpid1",
            },
            {
                "receive_timestamp": receive_timestamp,
                "campaign_id": 21,
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "grpid2",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:08:00"),
                "campaign_id": 11,
                "device_id": "EXCLUDE",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "grpid1",
            }
        ]
    )

    await free_events_porter_step.run(common_input_data)

    processed_events = factory.get_all_processed()
    free_events = list(filter(lambda ev: ev[4] != "EXCLUDE", processed_events))
    assert len(free_events) == 2
    assert free_events[0][0] == receive_timestamp
    assert free_events[1][0] == receive_timestamp


@pytest.mark.parametrize("tz_name", ["UTC", "Europe/Moscow"])
async def test_sets_campaign_timezone(
    factory, free_events_porter_step, common_input_data, tz_name
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "campaign_id": 11,
                "event_name": "ACTION_SEARCH",
                "event_group_id": "grpid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "campaign_id": 11,
                "event_name": "ACTION_SEARCH",
                "event_group_id": "grpid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:15:00"),
                "campaign_id": 21,
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "grpid1",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:08:00"),
                "campaign_id": 11,
                "device_id": "EXCLUDE",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "grpid1",
            }
        ]
    )
    common_input_data["orders"][0]["campaigns"][0]["tz_name"] = tz_name
    common_input_data["orders"][1]["campaigns"][0]["tz_name"] = tz_name

    await free_events_porter_step.run(common_input_data)

    processed_events = factory.get_all_processed()
    timezones = list(
        map(itemgetter(13), filter(lambda ev: ev[4] != "EXCLUDE", processed_events))
    )

    assert timezones == [tz_name, tz_name, tz_name]


async def test_adds_processing_metadata(
    factory, warden_context_mock, free_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "campaign_id": 11,
                "event_name": "ACTION_SEARCH",
                "event_group_id": "grpid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "campaign_id": 11,
                "event_name": "ACTION_SEARCH",
                "event_group_id": "grpid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:15:00"),
                "campaign_id": 21,
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "grpid1",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:08:00"),
                "campaign_id": 11,
                "device_id": "EXCLUDE",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "grpid1",
            }
        ]
    )

    await free_events_porter_step.run(common_input_data, warden_context_mock)

    processed_events = factory.get_all_processed()
    free_events = filter(lambda ev: ev[4] != "EXCLUDE", processed_events)
    processed_metadata = list(map(lambda ev: json.loads(ev[15]), free_events))
    assert processed_metadata == [
        {"warden_executor_id": "executor_id_uuid"},
        {"warden_executor_id": "executor_id_uuid"},
        {"warden_executor_id": "executor_id_uuid"},
    ]


async def test_respects_paid_events_list(
    factory, free_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:17:00"),
                "campaign_id": 11,
                "device_id": "BBB",
                "event_name": "ACTION_MAKE_ROUTE",
                "event_group_id": "evgrid2",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:18:00"),
                "campaign_id": 21,
                "device_id": "CCC",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid2",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:19:00"),
                "campaign_id": 21,
                "device_id": "CCC",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid2",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:06:00"),
                "campaign_id": 11,
                "device_id": "BBB",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid2",
            },
        ]
    )
    common_input_data["orders"][0]["campaigns"][0]["paid_events_names"] = [
        "BILLBOARD_SHOW",
        "BILLBOARD_TAP",
        "ACTION_CALL",
    ]
    common_input_data["orders"][1]["campaigns"][0]["paid_events_names"] = [
        "ACTION_CALL"
    ]

    await free_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:05:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:06:00"), "BILLBOARD_TAP", "BBB"),
        (dt("2000-02-02 01:17:00"), "ACTION_MAKE_ROUTE", "BBB"),
        (dt("2000-02-02 01:18:00"), "BILLBOARD_SHOW", "CCC"),
    ]


async def test_sets_cost_to_zero(factory, free_events_porter_step, common_input_data):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:10:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:10:00"),
                "campaign_id": 21,
                "device_id": "AAA",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid2",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 11,
                "device_id": "EXCLUDE",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            }
        ]
    )

    await free_events_porter_step.run(common_input_data)

    processed_events = factory.get_all_processed()
    free_events = list(filter(lambda ev: ev[4] != "EXCLUDE", processed_events))

    assert len(free_events) == 2
    assert free_events[0][12] == Decimal(0)
    assert free_events[1][12] == Decimal(0)


async def test_not_adds_events_for_unknown_campaigns(
    factory, free_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:10:00"),
                "campaign_id": 33,
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:20:00"),
                "campaign_id": 34,
                "device_id": "BBB",
                "event_name": "ACTION_MAKE_ROUTE",
                "event_group_id": "evgrid2",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 33,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:06:00"),
                "campaign_id": 34,
                "device_id": "BBB",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid2",
            },
        ]
    )

    await free_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:05:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:06:00"), "BILLBOARD_TAP", "BBB"),
    ]


async def test_adds_nothing_if_no_orders_passed(
    factory, free_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:10:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:20:00"),
                "campaign_id": 11,
                "device_id": "BBB",
                "event_name": "ACTION_MAKE_ROUTE",
                "event_group_id": "evgrid2",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:20:00"),
                "campaign_id": 21,
                "device_id": "BBB",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid3",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:06:00"),
                "campaign_id": 11,
                "device_id": "BBB",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid2",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:07:00"),
                "campaign_id": 21,
                "device_id": "BBB",
                "event_name": "ACTION_MAKE_ROUTE",
                "event_group_id": "evgrid3",
            },
        ]
    )
    common_input_data["orders"] = []

    await free_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:05:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:06:00"), "BILLBOARD_TAP", "BBB"),
        (dt("2000-02-02 01:07:00"), "ACTION_MAKE_ROUTE", "BBB"),
    ]


@pytest.mark.parametrize("billing_success", [False, None])
async def test_adds_events_for_campaigns_if_order_not_charged(
    factory, free_events_porter_step, common_input_data, billing_success
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "campaign_id": 21,
                "device_id": "BBB",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid2",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            }
        ]
    )
    common_input_data["orders"][0]["billing_success"] = billing_success
    common_input_data["orders"][1]["billing_success"] = billing_success

    await free_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:05:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:13:00"), "ACTION_CALL", "AAA"),
        (dt("2000-02-02 01:14:00"), "BILLBOARD_TAP", "BBB"),
    ]


async def test_adds_events_if_billing_applied_billing_applied_is_false(
    factory, free_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "campaign_id": 21,
                "device_id": "BBB",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid2",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            }
        ]
    )
    common_input_data["billing_applied"] = False

    await free_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:05:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:13:00"), "ACTION_CALL", "AAA"),
        (dt("2000-02-02 01:14:00"), "BILLBOARD_TAP", "BBB"),
    ]


@pytest.mark.parametrize(
    ("processed_event_receive_timestamp", "expected_events_in_processed"),
    [
        (dt("2000-02-02 01:09:00"), 3),
        (dt("2000-02-02 01:10:00"), 1),
        (dt("2000-02-02 01:13:00"), 1),
        (dt("2000-02-02 01:20:00"), 1),
        (dt("2000-02-02 01:23:00"), 3),
    ],
)
async def test_adds_nothing_if_free_events_from_packet_already_present_in_processed(
    factory,
    free_events_porter_step,
    common_input_data,
    processed_event_receive_timestamp,
    expected_events_in_processed,
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "campaign_id": 21,
                "device_id": "BBB",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid2",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "campaign_id": 11,
                "device_id": "CCC",
                "event_name": "ACTION_SEARCH",
                "event_group_id": "evgrid5",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:12:30"),
                "campaign_id": 21,
                "device_id": "BBB",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid6",
            },
        ]
    )

    await free_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:05:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:12:00"), "ACTION_SEARCH", "CCC"),
        (dt("2000-02-02 01:12:30"), "BILLBOARD_TAP", "BBB"),
    ]


@pytest.mark.usefixtures("caplog_set_level_error")
async def test_logs_error_if_paid_events_from_packet_already_present_in_processed(
    caplog, factory, free_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            }
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "campaign_id": 11,
                "device_id": "CCC",
                "event_name": "ACTION_SEARCH",
                "event_group_id": "evgrid5",
            },
        ]
    )

    await free_events_porter_step.run(common_input_data)

    assert len(caplog.records) == 1
    record = caplog.records[0]
    assert record.levelno == logging.ERROR
    assert record.msg == "Ignored duplicate free events saving for packet (%d, %d)"
    assert record.args == (
        int(dt("2000-02-02 01:10:00").timestamp()),
        int(dt("2000-02-02 01:20:00").timestamp()),
    )


async def test_not_replaces_existing_free_events_in_processed(
    factory, free_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "campaign_id": 21,
                "device_id": "BBB",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid2",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:06:00"),
                "campaign_id": 11,
                "device_id": "CCC",
                "event_name": "ACTION_SEARCH",
                "event_group_id": "evgrid5",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:07:00"),
                "campaign_id": 21,
                "device_id": "CCC",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid6",
            },
        ]
    )

    await free_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:05:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:06:00"), "ACTION_SEARCH", "CCC"),
        (dt("2000-02-02 01:07:00"), "BILLBOARD_TAP", "CCC"),
        (dt("2000-02-02 01:13:00"), "ACTION_CALL", "AAA"),
        (dt("2000-02-02 01:14:00"), "BILLBOARD_TAP", "BBB"),
    ]


async def test_logs_error_if_unknown_processing_mode(
    free_events_porter_step, caplog
):
    input_data = {
        "packet_start": dt("2000-02-02 01:10:00"),
        "packet_end": dt("2000-02-02 01:20:00"),
        "billing_applied": True,
        "orders": [
            {
                "order_id": 101,
                "balance": Decimal("300"),
                "amount_to_bill": Decimal("5"),
                "billing_success": True,
                "campaigns": [
                    {
                        "campaign_id": 11,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW", "BILLBOARD_TAP"],
                        "free_event_processing_mode": None,
                        "paid_events_count": 2,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 2,
                        "last_paid_event_cost": Decimal("0.5"),
                    },
                    {
                        "campaign_id": 12,
                        "billing_type": "cpa",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW", "BILLBOARD_TAP"],
                        "free_event_processing_mode": FreeEventProcessingMode.ALL_EVENTS,  # noqa: E501
                        "paid_events_count": 2,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 2,
                        "last_paid_event_cost": Decimal("0.5"),
                    },
                ],
            }
        ],
    }

    await free_events_porter_step.run(input_data)

    error_messages = [r for r in caplog.records if r.levelname == "ERROR"]
    assert len(error_messages) == 1
    assert (
        error_messages[0].message == "Unknown free event processing mode None"
    )


async def test_sets_client_settings_for_max_memory_usage_for_queries(
    mocker, factory, free_events_porter_step
):
    input_data = {
        "packet_start": dt("2000-02-02 01:10:00"),
        "packet_end": dt("2000-02-02 01:20:00"),
        "billing_applied": True,
        "orders": [
            {
                "order_id": 101,
                "balance": Decimal("300"),
                "amount_to_bill": Decimal("5"),
                "billing_success": True,
                "campaigns": [
                    {
                        "campaign_id": 11,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW", "BILLBOARD_TAP"],
                        "paid_events_count": 2,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 2,
                        "last_paid_event_cost": Decimal("0.5"),
                        "free_event_processing_mode": FreeEventProcessingMode.ONLY_IF_PAID_PRESENT,  # noqa: E501
                    }
                ],
            }
        ],
    }

    calls = []

    class MockClient:
        def __init__(self, settings=None, *args, **kwargs):
            calls.append(settings)

        async def execute(*args, **kwargs):
            pass

    mocker.patch(
        "maps_adv.statistics.beekeeper.lib.steps.free_events_porter.Client", MockClient
    )

    await free_events_porter_step.run(input_data)

    assert calls == [{"max_memory_usage": 10737418240}]
