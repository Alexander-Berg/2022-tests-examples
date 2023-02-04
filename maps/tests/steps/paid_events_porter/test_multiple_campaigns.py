from decimal import Decimal

import pytest

from maps_adv.common.helpers import coro_mock, dt

pytestmark = [pytest.mark.asyncio]


async def test_transfers_events_for_two_campaigns_from_one_order(
    factory, paid_events_porter_step
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
                    },
                    {
                        "campaign_id": 12,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW", "BILLBOARD_TAP"],
                        "paid_events_count": 3,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 3,
                        "last_paid_event_cost": Decimal("0.5"),
                    },
                ],
            }
        ],
    }
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 12,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "event_name": "BILLBOARD_TAP",
            },
            {
                "campaign_id": 12,
                "device_id": "CCC",
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 11,
                "device_id": "DDD",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
                "event_name": "BILLBOARD_TAP",
            },
            {
                "campaign_id": 12,
                "device_id": "EEE",
                "receive_timestamp": dt("2000-02-02 01:16:00"),
                "event_name": "BILLBOARD_SHOW",
            },
        ]
    )

    await paid_events_porter_step.run(input_data)

    assert factory.get_all_processed(0, 2, 4) == [
        (dt("2000-02-02 01:12:00"), 11, "AAA"),
        (dt("2000-02-02 01:13:00"), 12, "BBB"),
        (dt("2000-02-02 01:14:00"), 12, "CCC"),
        (dt("2000-02-02 01:15:00"), 11, "DDD"),
        (dt("2000-02-02 01:16:00"), 12, "EEE"),
    ]


async def test_transfers_events_for_two_campaigns_from_different_orders(
    factory, paid_events_porter_step
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
            },
            {
                "order_id": 102,
                "balance": Decimal("300"),
                "amount_to_bill": Decimal("5"),
                "billing_success": True,
                "campaigns": [
                    {
                        "campaign_id": 13,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW", "BILLBOARD_TAP"],
                        "paid_events_count": 3,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 3,
                        "last_paid_event_cost": Decimal("0.5"),
                    }
                ],
            },
        ],
    }

    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 13,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "event_name": "BILLBOARD_TAP",
            },
            {
                "campaign_id": 13,
                "device_id": "CCC",
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 11,
                "device_id": "DDD",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
                "event_name": "BILLBOARD_TAP",
            },
            {
                "campaign_id": 13,
                "device_id": "EEE",
                "receive_timestamp": dt("2000-02-02 01:16:00"),
                "event_name": "BILLBOARD_SHOW",
            },
        ]
    )

    await paid_events_porter_step.run(input_data)

    assert factory.get_all_processed(0, 2, 4) == [
        (dt("2000-02-02 01:12:00"), 11, "AAA"),
        (dt("2000-02-02 01:13:00"), 13, "BBB"),
        (dt("2000-02-02 01:14:00"), 13, "CCC"),
        (dt("2000-02-02 01:15:00"), 11, "DDD"),
        (dt("2000-02-02 01:16:00"), 13, "EEE"),
    ]


async def test_insert_events_in_one_query_for_each_order(
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
                    },
                    {
                        "campaign_id": 12,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW", "BILLBOARD_TAP"],
                        "paid_events_count": 3,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 3,
                        "last_paid_event_cost": Decimal("0.5"),
                    },
                ],
            },
            {
                "order_id": 102,
                "balance": Decimal("300"),
                "amount_to_bill": Decimal("5"),
                "billing_success": True,
                "campaigns": [
                    {
                        "campaign_id": 13,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW", "BILLBOARD_TAP"],
                        "paid_events_count": 3,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 3,
                        "last_paid_event_cost": Decimal("0.5"),
                    }
                ],
            },
            {
                "order_id": 103,
                "balance": Decimal("300"),
                "amount_to_bill": Decimal("5"),
                "billing_success": True,
                "campaigns": [
                    {
                        "campaign_id": 14,
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
            },
        ],
    }
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 12,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:05"),
                "event_name": "BILLBOARD_TAP",
            },
            {
                "campaign_id": 12,
                "device_id": "CCC",
                "receive_timestamp": dt("2000-02-02 01:13:10"),
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 13,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:14:05"),
                "event_name": "BILLBOARD_TAP",
            },
            {
                "campaign_id": 13,
                "device_id": "CCC",
                "receive_timestamp": dt("2000-02-02 01:14:10"),
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 11,
                "device_id": "DDD",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
                "event_name": "BILLBOARD_TAP",
            },
            {
                "campaign_id": 13,
                "device_id": "EEE",
                "receive_timestamp": dt("2000-02-02 01:16:10"),
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 14,
                "device_id": "EEE",
                "receive_timestamp": dt("2000-02-02 01:16:20"),
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 14,
                "device_id": "EEE",
                "receive_timestamp": dt("2000-02-02 01:16:30"),
                "event_name": "BILLBOARD_SHOW",
            },
        ]
    )
    ch_mock = mocker.patch(
        "maps_adv.statistics.beekeeper.lib.steps.paid_events_porter.Client.execute",  # noqa: E501
        coro_mock(),
    )
    ch_mock.coro.side_effect = [
        False,  # Reply for duplicate check
        None,  # Reply for events insertion
        False,  # Reply for duplicate check
        None,  # Reply for events insertion
        False,  # Reply for duplicate check
        None,  # Reply for events insertion
    ]

    await paid_events_porter_step.run(input_data)

    assert ch_mock.call_count == 6  # Per order: one for duplicate check, one for insert


async def test_respects_paid_events_names_per_campaign(
    factory, paid_events_porter_step
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
                        "paid_events_names": ["ACTION_CALL"],
                        "paid_events_count": 1,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 1,
                        "last_paid_event_cost": Decimal("0.5"),
                    },
                    {
                        "campaign_id": 12,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW", "BILLBOARD_TAP"],
                        "paid_events_count": 1,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 1,
                        "last_paid_event_cost": Decimal("0.5"),
                    },
                ],
            }
        ],
    }
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
                "event_name": "BILLBOARD_TAP",
            },
            {
                "campaign_id": 12,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "event_name": "ACTION_CALL",
            },
            {
                "campaign_id": 12,
                "device_id": "CCC",
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "event_name": "ACTION_SEARCH",
            },
            {
                "campaign_id": 11,
                "device_id": "DDD",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
                "event_name": "ACTION_CALL",
            },
            {
                "campaign_id": 12,
                "device_id": "EEE",
                "receive_timestamp": dt("2000-02-02 01:16:00"),
                "event_name": "BILLBOARD_SHOW",
            },
        ]
    )

    await paid_events_porter_step.run(input_data)

    assert factory.get_all_processed(0, 2, 4) == [
        (dt("2000-02-02 01:15:00"), 11, "DDD"),
        (dt("2000-02-02 01:16:00"), 12, "EEE"),
    ]


async def test_respects_timezone_per_campaign(factory, paid_events_porter_step):
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
                        "tz_name": "Europe/Moscow",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW", "BILLBOARD_TAP"],
                        "paid_events_count": 1,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 1,
                        "last_paid_event_cost": Decimal("0.5"),
                    },
                    {
                        "campaign_id": 12,
                        "billing_type": "cpm",
                        "tz_name": "Asia/Novosibirsk",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["BILLBOARD_SHOW", "BILLBOARD_TAP"],
                        "paid_events_count": 1,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 1,
                        "last_paid_event_cost": Decimal("0.5"),
                    },
                ],
            }
        ],
    }
    factory.insert_into_normalized(
        [
            {
                "campaign_id": 11,
                "receive_timestamp": dt("2000-02-02 01:12:00"),
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
            },
            {
                "campaign_id": 12,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "event_name": "BILLBOARD_TAP",
            },
        ]
    )

    await paid_events_porter_step.run(input_data)

    assert factory.get_all_processed(13) == ["Europe/Moscow", "Asia/Novosibirsk"]
