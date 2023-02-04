import json
import logging
from decimal import Decimal

import pytest

from maps_adv.common.helpers import dt

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
                        "paid_events_count": 2,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 2,
                        "last_paid_event_cost": Decimal("0.5"),
                    }
                ],
            }
        ],
    }


async def test_returns_input_data_unmodified(
    paid_events_porter_step, common_input_data
):
    result = await paid_events_porter_step.run(common_input_data)

    assert result == common_input_data


async def test_adds_events_to_processed(
    factory, paid_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 11,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "event_name": "BILLBOARD_SHOW",
            },
        ]
    )

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 4) == [
        (dt("2000-02-02 01:12:00"), "AAA"),
        (dt("2000-02-02 01:13:00"), "BBB"),
    ]


@pytest.mark.parametrize(
    ("field", "field_num", "value1", "value2"),
    [
        ("event_name", 1, "BILLBOARD_SHOW", "BILLBOARD_TAP"),
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
async def test_copies_fields_from_normalized(
    factory,
    paid_events_porter_step,
    common_input_data,
    field,
    field_num,
    value1,
    value2,
):
    factory.insert_into_normalized(
        [
            dict(
                receive_timestamp=dt("2000-02-02 01:13:00"),
                campaign_id=11,
                **{field: value1},
            ),
            dict(
                receive_timestamp=dt("2000-02-02 01:14:00"),
                campaign_id=11,
                **{field: value2},
            ),
        ]
    )

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(field_num) == [value1, value2]


@pytest.mark.parametrize(
    "receive_timestamp", [dt("2000-02-02 01:13:00"), dt("2000-02-02 01:14:00")]
)
async def test_copies_receive_timestamp(
    factory, paid_events_porter_step, common_input_data, receive_timestamp
):
    factory.insert_into_normalized(
        [{"receive_timestamp": receive_timestamp, "campaign_id": 11}]
    )
    common_input_data["orders"][0]["campaigns"][0]["paid_events_to_charge"] = 1

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0) == [receive_timestamp]


@pytest.mark.parametrize("tz_name", ["UTC", "Europe/Moscow"])
async def test_sets_campaign_timezone(
    factory, paid_events_porter_step, common_input_data, tz_name
):
    factory.insert_into_normalized(
        [
            {"receive_timestamp": dt("2000-02-02 01:13:00"), "campaign_id": 11},
            {"receive_timestamp": dt("2000-02-02 01:14:00"), "campaign_id": 11},
        ]
    )
    common_input_data["orders"][0]["campaigns"][0]["tz_name"] = tz_name

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(13) == [tz_name, tz_name]


async def test_adds_processing_metadata(
    factory, warden_context_mock, paid_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {"receive_timestamp": dt("2000-02-02 01:13:00"), "campaign_id": 11},
            {"receive_timestamp": dt("2000-02-02 01:14:00"), "campaign_id": 11},
        ]
    )
    common_input_data["last_paid_event_cost"] = Decimal("0.1")

    await paid_events_porter_step.run(common_input_data, warden_context_mock)

    processed_events_metadata = factory.get_all_processed(15)
    processed_metadata = list(map(json.loads, processed_events_metadata))
    assert processed_metadata == [
        {"warden_executor_id": "executor_id_uuid"},
        {"warden_executor_id": "executor_id_uuid"},
    ]


async def test_respects_paid_events_to_charge(
    factory, paid_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
            },
            {
                "campaign_id": 11,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
            },
            {
                "campaign_id": 11,
                "device_id": "CCC",
                "receive_timestamp": dt("2000-02-02 01:14:00"),
            },
        ]
    )
    common_input_data["orders"][0]["campaigns"][0]["paid_events_to_charge"] = 2

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 4) == [
        (dt("2000-02-02 01:12:00"), "AAA"),
        (dt("2000-02-02 01:13:00"), "BBB"),
    ]


@pytest.mark.parametrize("last_paid_event_cost", [Decimal("0.5"), Decimal("0.1")])
async def test_respects_last_paid_event_cost(
    factory, paid_events_porter_step, common_input_data, last_paid_event_cost
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
            },
            {
                "campaign_id": 11,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
            },
        ]
    )
    common_input_data["orders"][0]["campaigns"][0]["paid_events_to_charge"] = 2
    common_input_data["orders"][0]["campaigns"][0][
        "last_paid_event_cost"
    ] = last_paid_event_cost

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 4, 12) == [
        (dt("2000-02-02 01:12:00"), "AAA", Decimal("0.5")),
        (dt("2000-02-02 01:13:00"), "BBB", last_paid_event_cost),
    ]


@pytest.mark.parametrize("last_paid_event_cost", [Decimal("0.5"), Decimal("0.1")])
async def respects_last_paid_event_cost_if_paid_events_to_charge_is_one(
    factory, paid_events_porter_step, common_input_data, last_paid_event_cost
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
            }
        ]
    )
    common_input_data["orders"][0]["campaigns"][0]["paid_events_to_charge"] = 1
    common_input_data["orders"][0]["campaigns"][0][
        "last_paid_event_cost"
    ] = last_paid_event_cost

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 4, 15) == [
        (dt("2000-02-02 01:12:00"), "AAA", last_paid_event_cost)
    ]


async def test_respects_packet_bounds(
    factory, paid_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:09:00"),
                "device_id": "AAA",
            },
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:10:00"),
                "device_id": "BBB",
            },
            {
                "campaign_id": 11,
                "device_id": "CCC",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
            },
            {
                "campaign_id": 11,
                "device_id": "DDD",
                "receive_timestamp": dt("2000-02-02 01:20:00"),
            },
            {
                "campaign_id": 11,
                "device_id": "EEE",
                "receive_timestamp": dt("2000-02-02 01:21:00"),
            },
        ]
    )
    common_input_data["orders"][0]["campaigns"][0]["paid_events_to_charge"] = 3

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 4) == [
        (dt("2000-02-02 01:10:00"), "BBB"),
        (dt("2000-02-02 01:13:00"), "CCC"),
        (dt("2000-02-02 01:20:00"), "DDD"),
    ]


async def test_respects_paid_events_list(
    factory, paid_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
                "event_name": "ACTION_SEARCH",
            },
            {
                "campaign_id": 11,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "event_name": "ACTION_OPEN_SITE",
            },
            {
                "campaign_id": 11,
                "device_id": "CCC",
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "event_name": "ACTION_OPEN_APP",
            },
            {
                "campaign_id": 11,
                "device_id": "DDD",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
                "event_name": "ACTION_SAVE_OFFER",
            },
        ]
    )
    common_input_data["orders"][0]["campaigns"][0]["paid_events_names"] = [
        "ACTION_OPEN_SITE",
        "ACTION_SAVE_OFFER",
    ]

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:13:00"), "ACTION_OPEN_SITE", "BBB"),
        (dt("2000-02-02 01:15:00"), "ACTION_SAVE_OFFER", "DDD"),
    ]


async def test_respects_campaign_id(
    factory, paid_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 12,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
            },
            {
                "campaign_id": 11,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
            },
            {
                "campaign_id": 13,
                "device_id": "CCC",
                "receive_timestamp": dt("2000-02-02 01:14:00"),
            },
            {
                "campaign_id": 11,
                "device_id": "DDD",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
            },
        ]
    )

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:13:00"), "BILLBOARD_SHOW", "BBB"),
        (dt("2000-02-02 01:15:00"), "BILLBOARD_SHOW", "DDD"),
    ]


async def test_adds_nothing_if_paid_events_to_charge_is_zero(
    factory, paid_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
            },
            {
                "campaign_id": 11,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
            },
        ]
    )
    common_input_data["orders"][0]["campaigns"][0]["paid_events_to_charge"] = 0

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 4) == []


async def test_add_nothing_if_no_orders_passed(
    factory, paid_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
            },
            {
                "campaign_id": 11,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
            },
        ]
    )
    common_input_data["orders"] = []

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 4) == []


@pytest.mark.parametrize("billing_success", [False, None])
async def test_not_adds_events_for_campaigns_if_order_not_charged(
    factory, paid_events_porter_step, common_input_data, billing_success
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
            },
            {
                "campaign_id": 11,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
            },
        ]
    )
    common_input_data["orders"][0]["billing_success"] = billing_success

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 4) == []


@pytest.mark.parametrize("billing_success", [False, None])
async def test_adds_events_for_campaigns_if_no_order_id_and_billing_mark_as_not_charged(
    factory, paid_events_porter_step, common_input_data, billing_success
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
            },
            {
                "campaign_id": 11,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
            },
        ]
    )
    common_input_data["orders"][0]["order_id"] = None
    common_input_data["orders"][0]["billing_success"] = billing_success

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 4) == [
        (dt("2000-02-02 01:12:00"), "AAA"),
        (dt("2000-02-02 01:13:00"), "BBB"),
    ]


async def test_adds_events_if_billing_applied_billing_applied_is_false(
    factory, paid_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
            },
            {
                "campaign_id": 11,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
            },
        ]
    )
    common_input_data["billing_applied"] = False

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 4) == [
        (dt("2000-02-02 01:12:00"), "AAA"),
        (dt("2000-02-02 01:13:00"), "BBB"),
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
async def test_adds_nothing_if_paid_events_from_packet_already_present_in_processed_for_order(  # noqa: E501
    factory,
    paid_events_porter_step,
    common_input_data,
    processed_event_receive_timestamp,
    expected_events_in_processed,
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
            },
            {
                "campaign_id": 11,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": processed_event_receive_timestamp,
                "device_id": "XXX",
            }
        ]
    )

    await paid_events_porter_step.run(common_input_data)

    assert len(factory.get_all_processed()) == expected_events_in_processed


@pytest.mark.usefixtures("caplog_set_level_error")
async def test_logs_error_if_paid_events_from_packet_already_present_in_processed(
    caplog, factory, paid_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
            },
            {
                "campaign_id": 11,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "device_id": "XXX",
            }
        ]
    )

    await paid_events_porter_step.run(common_input_data)

    assert len(caplog.records) == 1
    record = caplog.records[0]
    assert record.levelno == logging.ERROR
    assert record.msg == "Ignored duplicate paid events saving for packet (%d, %d, %d)"
    assert record.args == (
        int(dt("2000-02-02 01:10:00").timestamp()),
        int(dt("2000-02-02 01:20:00").timestamp()),
        101,
    )


async def test_not_replaces_existing_events_in_processed(
    factory, paid_events_porter_step, common_input_data
):
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
            },
            {
                "campaign_id": 11,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:09:00"),
                "device_id": "XXX",
            },
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:21:00"),
                "device_id": "YYY",
            },
        ]
    )

    await paid_events_porter_step.run(common_input_data)

    assert factory.get_all_processed(0, 4) == [
        (dt("2000-02-02 01:09:00"), "XXX"),
        (dt("2000-02-02 01:12:00"), "AAA"),
        (dt("2000-02-02 01:13:00"), "BBB"),
        (dt("2000-02-02 01:21:00"), "YYY"),
    ]


async def test_sets_client_settings_for_max_memory_usage_for_queries(
    mocker, factory, paid_events_porter_step
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
        "maps_adv.statistics.beekeeper.lib.steps.paid_events_porter.Client", MockClient
    )

    await paid_events_porter_step.run(input_data)

    assert calls == [{"max_memory_usage": 10737418240}]
