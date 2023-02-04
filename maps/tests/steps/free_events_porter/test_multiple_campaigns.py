from decimal import Decimal
from operator import itemgetter

import pytest

from maps_adv.common.helpers import coro_mock, dt
from maps_adv.statistics.beekeeper.lib.steps.base import FreeEventProcessingMode

pytestmark = [pytest.mark.asyncio]


async def test_transfers_events_for_two_campaigns_from_one_order_cpm(
    factory, free_events_porter_step
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
                        "free_event_processing_mode": FreeEventProcessingMode.ONLY_IF_PAID_PRESENT,  # noqa: E501
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
                "event_name": "ACTION_SEARCH",
                "event_group_id": "evgrid1",
            },
            {
                "campaign_id": 12,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid2",
            },
            {
                "campaign_id": 12,
                "device_id": "CCC",
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid3",
            },
            {
                "campaign_id": 11,
                "device_id": "DDD",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid4",
            },
            {
                "campaign_id": 12,
                "device_id": "EEE",
                "receive_timestamp": dt("2000-02-02 01:16:00"),
                "event_name": "ACTION_MAKE_ROUTE",
                "event_group_id": "evgrid5",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:01:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:02:00"),
                "campaign_id": 12,
                "device_id": "BBB",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid2",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:03:00"),
                "campaign_id": 12,
                "device_id": "CCC",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid3",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:04:00"),
                "campaign_id": 11,
                "device_id": "DDD",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid4",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 12,
                "device_id": "EEE",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid5",
            },
        ]
    )

    await free_events_porter_step.run(input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:01:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:02:00"), "BILLBOARD_SHOW", "BBB"),
        (dt("2000-02-02 01:03:00"), "BILLBOARD_TAP", "CCC"),
        (dt("2000-02-02 01:04:00"), "BILLBOARD_TAP", "DDD"),
        (dt("2000-02-02 01:05:00"), "BILLBOARD_SHOW", "EEE"),
        (dt("2000-02-02 01:12:00"), "ACTION_SEARCH", "AAA"),
        (dt("2000-02-02 01:13:00"), "ACTION_CALL", "BBB"),
        (dt("2000-02-02 01:14:00"), "ACTION_CALL", "CCC"),
        (dt("2000-02-02 01:15:00"), "ACTION_CALL", "DDD"),
        (dt("2000-02-02 01:16:00"), "ACTION_MAKE_ROUTE", "EEE"),
    ]


async def test_transfers_events_for_two_campaigns_from_one_order_cpa(
    factory, free_events_porter_step
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
                        "paid_events_count": 2,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 2,
                        "last_paid_event_cost": Decimal("0.5"),
                        "free_event_processing_mode": FreeEventProcessingMode.ALL_EVENTS,  # noqa: E501
                    },
                    {
                        "campaign_id": 12,
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
                        "paid_events_count": 3,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 3,
                        "last_paid_event_cost": Decimal("0.5"),
                        "free_event_processing_mode": FreeEventProcessingMode.ALL_EVENTS,  # noqa: E501
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
                "event_group_id": "evgrid1",
            },
            {
                "campaign_id": 12,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid2",
            },
            {
                "campaign_id": 12,
                "device_id": "CCC",
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid3",
            },
            {
                "campaign_id": 11,
                "device_id": "DDD",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid4",
            },
            {
                "campaign_id": 12,
                "device_id": "EEE",
                "receive_timestamp": dt("2000-02-02 01:16:00"),
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid5",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:01:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:02:00"),
                "campaign_id": 12,
                "device_id": "BBB",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid2",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:03:00"),
                "campaign_id": 12,
                "device_id": "CCC",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid3",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:04:00"),
                "campaign_id": 11,
                "device_id": "DDD",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid4",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 12,
                "device_id": "EEE",
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid5",
            },
        ]
    )

    await free_events_porter_step.run(input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:01:00"), "ACTION_CALL", "AAA"),
        (dt("2000-02-02 01:02:00"), "ACTION_CALL", "BBB"),
        (dt("2000-02-02 01:03:00"), "ACTION_CALL", "CCC"),
        (dt("2000-02-02 01:04:00"), "ACTION_CALL", "DDD"),
        (dt("2000-02-02 01:05:00"), "ACTION_CALL", "EEE"),
        (dt("2000-02-02 01:12:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:13:00"), "BILLBOARD_SHOW", "BBB"),
        (dt("2000-02-02 01:14:00"), "BILLBOARD_SHOW", "CCC"),
        (dt("2000-02-02 01:15:00"), "BILLBOARD_SHOW", "DDD"),
        (dt("2000-02-02 01:16:00"), "BILLBOARD_SHOW", "EEE"),
    ]


async def test_transfers_events_for_two_campaigns_from_different_orders(
    factory, free_events_porter_step
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
                        "free_event_processing_mode": FreeEventProcessingMode.ONLY_IF_PAID_PRESENT,  # noqa: E501
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
                        "paid_events_count": 3,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 3,
                        "last_paid_event_cost": Decimal("0.5"),
                        "free_event_processing_mode": FreeEventProcessingMode.ALL_EVENTS,  # noqa: E501
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
                "event_name": "ACTION_SEARCH",
                "event_group_id": "evgrid1",
            },
            {
                "campaign_id": 13,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid2",
            },
            {
                "campaign_id": 13,
                "device_id": "CCC",
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid3",
            },
            {
                "campaign_id": 11,
                "device_id": "DDD",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid4",
            },
            {
                "campaign_id": 13,
                "device_id": "EEE",
                "receive_timestamp": dt("2000-02-02 01:16:00"),
                "event_name": "ACTION_MAKE_ROUTE",
                "event_group_id": "evgrid5",
            },
            {
                "campaign_id": 21,
                "device_id": "DDD",
                "receive_timestamp": dt("2000-02-02 01:17:00"),
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid4",
            },
            {
                "campaign_id": 21,
                "device_id": "EEE",
                "receive_timestamp": dt("2000-02-02 01:18:00"),
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid5",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:01:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:02:00"),
                "campaign_id": 13,
                "device_id": "BBB",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid2",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:03:00"),
                "campaign_id": 13,
                "device_id": "CCC",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid3",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:04:00"),
                "campaign_id": 11,
                "device_id": "DDD",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid4",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 13,
                "device_id": "EEE",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid5",
            },
        ]
    )

    await free_events_porter_step.run(input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:01:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:02:00"), "BILLBOARD_SHOW", "BBB"),
        (dt("2000-02-02 01:03:00"), "BILLBOARD_TAP", "CCC"),
        (dt("2000-02-02 01:04:00"), "BILLBOARD_TAP", "DDD"),
        (dt("2000-02-02 01:05:00"), "BILLBOARD_SHOW", "EEE"),
        (dt("2000-02-02 01:12:00"), "ACTION_SEARCH", "AAA"),
        (dt("2000-02-02 01:13:00"), "ACTION_CALL", "BBB"),
        (dt("2000-02-02 01:14:00"), "ACTION_CALL", "CCC"),
        (dt("2000-02-02 01:15:00"), "ACTION_CALL", "DDD"),
        (dt("2000-02-02 01:16:00"), "ACTION_MAKE_ROUTE", "EEE"),
        (dt("2000-02-02 01:17:00"), "BILLBOARD_SHOW", "DDD"),
        (dt("2000-02-02 01:18:00"), "BILLBOARD_SHOW", "EEE"),
    ]


async def test_insert_events_in_one_query(mocker, factory, free_events_porter_step):
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
                        "free_event_processing_mode": FreeEventProcessingMode.ONLY_IF_PAID_PRESENT,  # noqa: E501
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
                        "free_event_processing_mode": FreeEventProcessingMode.ONLY_IF_PAID_PRESENT,  # noqa: E501
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
                        "free_event_processing_mode": FreeEventProcessingMode.ONLY_IF_PAID_PRESENT,  # noqa: E501
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
                        "campaign_id": 14,
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
                        "paid_events_count": 2,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 2,
                        "last_paid_event_cost": Decimal("0.5"),
                        "free_event_processing_mode": FreeEventProcessingMode.ALL_EVENTS,  # noqa: E501
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
                "event_name": "ACTION_SEARCH",
                "event_group_id": "evgrid1",
            },
            {
                "campaign_id": 12,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid2",
            },
            {
                "campaign_id": 13,
                "device_id": "CCC",
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid3",
            },
            {
                "campaign_id": 11,
                "device_id": "DDD",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid4",
            },
            {
                "campaign_id": 13,
                "device_id": "EEE",
                "receive_timestamp": dt("2000-02-02 01:16:00"),
                "event_name": "ACTION_MAKE_ROUTE",
                "event_group_id": "evgrid5",
            },
            {
                "campaign_id": 21,
                "device_id": "DDD",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid4",
            },
            {
                "campaign_id": 21,
                "device_id": "EEE",
                "receive_timestamp": dt("2000-02-02 01:16:00"),
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid5",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:01:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:02:00"),
                "campaign_id": 12,
                "device_id": "BBB",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid2",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:03:00"),
                "campaign_id": 13,
                "device_id": "CCC",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid2",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:04:00"),
                "campaign_id": 11,
                "device_id": "DDD",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid4",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 13,
                "device_id": "EEE",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid5",
            },
        ]
    )
    ch_mock = mocker.patch(
        "maps_adv.statistics.beekeeper.lib.steps.free_events_porter.Client.execute",
        coro_mock(),
    )
    ch_mock.coro.side_effect = [
        False,  # Reply for duplicate check
        None,  # Reply for events insertion
    ]

    await free_events_porter_step.run(input_data)

    assert ch_mock.call_count == 2  # One for duplicate check, one for insert


async def test_respects_paid_events_names_per_campaign(
    factory, free_events_porter_step
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
                    },
                    {
                        "campaign_id": 12,
                        "billing_type": "cpm",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["ACTION_CALL"],
                        "paid_events_count": 3,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 3,
                        "last_paid_event_cost": Decimal("0.5"),
                        "free_event_processing_mode": FreeEventProcessingMode.ONLY_IF_PAID_PRESENT,  # noqa: E501
                    },
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
                        "paid_events_names": ["ACTION_CALL", "ACTION_MAKE_ROUTE"],
                        "paid_events_count": 2,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 2,
                        "last_paid_event_cost": Decimal("0.5"),
                        "free_event_processing_mode": FreeEventProcessingMode.ALL_EVENTS,  # noqa: E501
                    },
                    {
                        "campaign_id": 22,
                        "billing_type": "cpa",
                        "tz_name": "UTC",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": ["ACTION_CALL"],
                        "paid_events_count": 3,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 3,
                        "last_paid_event_cost": Decimal("0.5"),
                        "free_event_processing_mode": FreeEventProcessingMode.ALL_EVENTS,  # noqa: E501
                    },
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
                "event_group_id": "evgrid1",
            },
            {
                "campaign_id": 12,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid2",
            },
            {
                "campaign_id": 12,
                "device_id": "CCC",
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid3",
            },
            {
                "campaign_id": 11,
                "device_id": "DDD",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid4",
            },
            {
                "campaign_id": 12,
                "device_id": "EEE",
                "receive_timestamp": dt("2000-02-02 01:16:00"),
                "event_name": "ACTION_SEARCH",
                "event_group_id": "evgrid5",
            },
            {
                "campaign_id": 21,
                "device_id": "DDD",
                "receive_timestamp": dt("2000-02-02 01:17:00"),
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid4",
            },
            {
                "campaign_id": 22,
                "device_id": "EEE",
                "receive_timestamp": dt("2000-02-02 01:18:00"),
                "event_name": "ACTION_SEARCH",
                "event_group_id": "evgrid5",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:01:00"),
                "campaign_id": 11,
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:02:00"),
                "campaign_id": 12,
                "device_id": "BBB",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid2",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:03:00"),
                "campaign_id": 12,
                "device_id": "CCC",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid3",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:04:00"),
                "campaign_id": 11,
                "device_id": "DDD",
                "event_name": "BILLBOARD_TAP",
                "event_group_id": "evgrid4",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:05:00"),
                "campaign_id": 12,
                "device_id": "EEE",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid5",
            },
        ]
    )

    await free_events_porter_step.run(input_data)

    assert factory.get_all_processed(0, 1, 4) == [
        (dt("2000-02-02 01:01:00"), "BILLBOARD_SHOW", "AAA"),
        (dt("2000-02-02 01:02:00"), "BILLBOARD_SHOW", "BBB"),
        (dt("2000-02-02 01:03:00"), "BILLBOARD_TAP", "CCC"),
        (dt("2000-02-02 01:04:00"), "BILLBOARD_TAP", "DDD"),
        (dt("2000-02-02 01:05:00"), "BILLBOARD_SHOW", "EEE"),
        (dt("2000-02-02 01:14:00"), "BILLBOARD_SHOW", "CCC"),
        (dt("2000-02-02 01:16:00"), "ACTION_SEARCH", "EEE"),
        (dt("2000-02-02 01:18:00"), "ACTION_SEARCH", "EEE"),
    ]


async def test_respects_timezone_per_campaign(factory, free_events_porter_step):
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
                        "free_event_processing_mode": FreeEventProcessingMode.ONLY_IF_PAID_PRESENT,  # noqa: E501
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
                        "free_event_processing_mode": FreeEventProcessingMode.ONLY_IF_PAID_PRESENT,  # noqa: E501
                    },
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
                        "tz_name": "Europe/Moscow",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": [
                            "ACTION_CALL",
                            "ACTION_MAKE_ROUTE",
                            "ACTION_SEARCH",
                            "ACTION_OPEN_SITE",
                            "ACTION_OPEN_APP",
                            "ACTION_SAVE_OFFER",
                        ],
                        "paid_events_count": 1,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 1,
                        "last_paid_event_cost": Decimal("0.5"),
                        "free_event_processing_mode": FreeEventProcessingMode.ALL_EVENTS,  # noqa: E501
                    },
                    {
                        "campaign_id": 22,
                        "billing_type": "cpa",
                        "tz_name": "Asia/Novosibirsk",
                        "paid_event_cost": Decimal("0.5"),
                        "paid_events_names": [
                            "ACTION_CALL",
                            "ACTION_MAKE_ROUTE",
                            "ACTION_SEARCH",
                            "ACTION_OPEN_SITE",
                            "ACTION_OPEN_APP",
                            "ACTION_SAVE_OFFER",
                        ],
                        "paid_events_count": 1,
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("100"),
                        "charged": Decimal("50"),
                        "daily_charged": Decimal("20"),
                        "paid_events_to_charge": 1,
                        "last_paid_event_cost": Decimal("0.5"),
                        "free_event_processing_mode": FreeEventProcessingMode.ALL_EVENTS,  # noqa: E501
                    },
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
                "event_name": "ACTION_CALL",
                "event_group_id": "evgrid1",
            },
            {
                "campaign_id": 12,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:13:00"),
                "event_name": "ACTION_SEARCH",
                "event_group_id": "evgrid2",
            },
            {
                "campaign_id": 21,
                "receive_timestamp": dt("2000-02-02 01:14:00"),
                "device_id": "AAA",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "campaign_id": 22,
                "device_id": "BBB",
                "receive_timestamp": dt("2000-02-02 01:15:00"),
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid2",
            },
        ]
    )
    factory.insert_into_processed(
        [
            {
                "receive_timestamp": dt("2000-02-02 01:01:00"),
                "campaign_id": 11,
                "device_id": "EXCLUDE",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid1",
            },
            {
                "receive_timestamp": dt("2000-02-02 01:02:00"),
                "campaign_id": 12,
                "device_id": "EXCLUDE",
                "event_name": "BILLBOARD_SHOW",
                "event_group_id": "evgrid2",
            },
        ]
    )

    await free_events_porter_step.run(input_data)

    processed_events = factory.get_all_processed()
    timezones = list(
        map(itemgetter(13), filter(lambda ev: ev[4] != "EXCLUDE", processed_events))
    )
    assert timezones == [
        "Europe/Moscow",
        "Asia/Novosibirsk",
        "Europe/Moscow",
        "Asia/Novosibirsk",
    ]
