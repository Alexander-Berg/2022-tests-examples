from decimal import Decimal

import pytest
from aiohttp.web import Response

from maps_adv.adv_store.api.proto.charger_api_pb2 import (
    CampaignForCharger,
    CampaignForChargerList,
    Money,
)
from maps_adv.billing_proxy.proto.orders_for_stat_pb2 import (
    OrdersStatInfo,
    OrderStatInfo,
)
from maps_adv.stat_tasks_starter.lib.charger.clients.exceptions import UnknownResponse
from maps_adv.stat_tasks_starter.tests.tools import (
    dt,
    dt_timestamp,
    setup_charged_db,
    setup_normalized_db,
)

pytestmark = [pytest.mark.asyncio]


async def test_updates_task_execution_state_with_collected_data(
    adv_store_receive_active_campaigns_rmock,
    billing_receive_orders_rmock,
    mock_charger_update_task,
    ch_client,
    charger_pipeline,
):
    adv_store_message = CampaignForChargerList(
        campaigns=[
            CampaignForCharger(
                campaign_id=4242,
                order_id=567382,
                cost=Money(value=30000),
                budget=Money(value=2000000),
                daily_budget=Money(value=200000),
                timezone="UTC",
            ),
            CampaignForCharger(
                campaign_id=4356,
                order_id=567382,
                cost=Money(value=40000),
                budget=Money(value=3000000),
                daily_budget=Money(value=300000),
                timezone="UTC",
            ),
            CampaignForCharger(
                campaign_id=1242,
                order_id=423773,
                cost=Money(value=50000),
                daily_budget=Money(value=300000),
                timezone="UTC",
            ),
        ]
    ).SerializeToString()
    adv_store_receive_active_campaigns_rmock(
        Response(body=adv_store_message, status=200)
    )

    billing_message = OrdersStatInfo(
        orders_info=[
            OrderStatInfo(order_id=567382, balance="100"),
            OrderStatInfo(order_id=423773, balance="300"),
        ]
    ).SerializeToString()
    billing_receive_orders_rmock(Response(body=billing_message, status=200))

    setup_normalized_db(
        ch_client,
        (
            (4242, 100),
            (4242, 200),
            (4242, 86550),  # future event, will be ignored
            (4242, 95500),  # future event, will be ignored
            (4356, 50),  # past event, will be ignored
            (1242, 100),
            (1242, 250),
            (1242, 300),
        ),
    )

    setup_charged_db(
        ch_client,
        (
            (4242, 10, Decimal(2)),
            (4242, 50, Decimal(2)),
            (4242, 86500, Decimal(2)),
            (4242, 95000, Decimal(2)),
            (4356, 350, Decimal(2)),
            (1242, 86500, Decimal(2)),
        ),
    )

    task_data = {
        "timing_from": dt(100),
        "timing_to": dt(300),
        "id": 100,
        "status": "accepted",
    }

    await charger_pipeline.collect_data(task_data)

    assert task_data["execution_state"] == [
        {
            "order_id": 567382,
            "budget_balance": Decimal(100),
            "campaigns": [
                {
                    "campaign_id": 4242,
                    "tz_name": "UTC",
                    "cpm": Decimal(3),
                    "budget": Decimal(200),
                    "daily_budget": Decimal(20),
                    "charged": Decimal(8),
                    "charged_daily": Decimal(4),
                    "events_count": 2,
                },
                {
                    "campaign_id": 4356,
                    "tz_name": "UTC",
                    "cpm": Decimal(4),
                    "budget": Decimal(300),
                    "daily_budget": Decimal(30),
                    "charged": Decimal(2),
                    "charged_daily": Decimal(2),
                    "events_count": 0,
                },
            ],
        },
        {
            "order_id": 423773,
            "budget_balance": Decimal(300),
            "campaigns": [
                {
                    "campaign_id": 1242,
                    "tz_name": "UTC",
                    "cpm": Decimal(5),
                    "budget": Decimal("Infinity"),
                    "daily_budget": Decimal(30),
                    "charged": Decimal(2),
                    "charged_daily": Decimal(0),
                    "events_count": 3,
                }
            ],
        },
    ]


@pytest.mark.parametrize(
    # winter/summer year_month for checking DST
    "tz_name, year_month, expected_daily_charged",
    (
        ["utc", "2019-01", Decimal(300)],
        ["utc", "2019-06", Decimal(300)],
        # UTC+3, DST agnostic
        ["Europe/Moscow", "2019-01", Decimal(320)],
        ["Europe/Moscow", "2019-06", Decimal(320)],
        # UTC+1, with DST
        ["Europe/Bratislava", "2019-01", Decimal(310)],
        ["Europe/Bratislava", "2019-06", Decimal(320)],
        # UTC-2, DST agnostic
        ["America/Noronha", "2019-01", Decimal(100)],
        ["America/Noronha", "2019-06", Decimal(100)],
        # UTC-1, with DST
        ["America/Scoresbysund", "2019-01", Decimal(200)],
        ["America/Scoresbysund", "2019-06", Decimal(300)],
    ),
)
async def test_collects_data_according_to_campaign_tz(
    adv_store_receive_active_campaigns_rmock,
    billing_receive_orders_rmock,
    mock_charger_update_task,
    ch_client,
    charger_pipeline,
    tz_name,
    year_month,
    expected_daily_charged,
):
    adv_store_message = CampaignForChargerList(
        campaigns=[
            CampaignForCharger(
                campaign_id=1,
                order_id=567382,
                cost=Money(value=30000),
                budget=Money(value=2000000),
                daily_budget=Money(value=200000),
                timezone=tz_name,
            )
        ]
    ).SerializeToString()
    adv_store_receive_active_campaigns_rmock(
        Response(body=adv_store_message, status=200)
    )

    billing_message = OrdersStatInfo(
        orders_info=[OrderStatInfo(order_id=567382, balance="100")]
    ).SerializeToString()
    billing_receive_orders_rmock(Response(body=billing_message, status=200))

    setup_normalized_db(
        ch_client,
        (
            (1, dt_timestamp(f"{year_month}-18 22:30:00")),
            (1, dt_timestamp(f"{year_month}-19 22:30:00")),
            (1, dt_timestamp(f"{year_month}-19 23:30:00")),
            (1, dt_timestamp(f"{year_month}-20 00:30:00")),
            (1, dt_timestamp(f"{year_month}-20 01:30:00")),
            (1, dt_timestamp(f"{year_month}-20 02:30:00")),
            (1, dt_timestamp(f"{year_month}-20 02:33:00")),
            (1, dt_timestamp(f"{year_month}-21 02:33:00")),
        ),
    )

    setup_charged_db(
        ch_client,
        (
            (1, dt_timestamp(f"{year_month}-18 22:30:00"), Decimal(1)),
            (1, dt_timestamp(f"{year_month}-19 22:30:00"), Decimal(10)),
            (1, dt_timestamp(f"{year_month}-19 23:30:00"), Decimal(10)),
            (1, dt_timestamp(f"{year_month}-20 00:30:00"), Decimal(100)),
            (1, dt_timestamp(f"{year_month}-20 01:30:00"), Decimal(100)),
            (1, dt_timestamp(f"{year_month}-20 02:30:00"), Decimal(100)),
            (1, dt_timestamp(f"{year_month}-21 02:30:00"), Decimal(1000)),
        ),
    )

    task_data = {
        "timing_from": dt(f"{year_month}-20 02:31:00"),
        "timing_to": dt(f"{year_month}-20 02:41:00"),
        "id": 100,
        "status": "accepted",
    }
    await charger_pipeline.collect_data(task_data)

    assert task_data["execution_state"] == [
        {
            "order_id": 567382,
            "budget_balance": Decimal(100),
            "campaigns": [
                {
                    "campaign_id": 1,
                    "tz_name": tz_name,
                    "cpm": Decimal(3),
                    "budget": Decimal(200),
                    "daily_budget": Decimal(20),
                    "charged": Decimal(1321),
                    "charged_daily": expected_daily_charged,
                    "events_count": 1,
                }
            ],
        }
    ]


async def test_collects_data_for_campaigns_with_diffrent_tz(
    adv_store_receive_active_campaigns_rmock,
    billing_receive_orders_rmock,
    mock_charger_update_task,
    ch_client,
    charger_pipeline,
):

    adv_store_message = CampaignForChargerList(
        campaigns=[
            CampaignForCharger(
                campaign_id=1,
                order_id=567382,
                cost=Money(value=30000),
                budget=Money(value=2000000),
                daily_budget=Money(value=200000),
                timezone="UTC",
            ),
            CampaignForCharger(
                campaign_id=2,
                order_id=567382,
                cost=Money(value=30000),
                budget=Money(value=2000000),
                daily_budget=Money(value=200000),
                timezone="Pacific/Honolulu",  # -10:00
            ),
            CampaignForCharger(
                campaign_id=3,
                order_id=567382,
                cost=Money(value=30000),
                budget=Money(value=2000000),
                daily_budget=Money(value=200000),
                timezone="Europe/Moscow",  # +03:00
            ),
            CampaignForCharger(
                campaign_id=4,
                order_id=567382,
                cost=Money(value=30000),
                budget=Money(value=2000000),
                daily_budget=Money(value=200000),
                timezone="Asia/Kamchatka",  # +12:00
            ),
        ]
    ).SerializeToString()
    adv_store_receive_active_campaigns_rmock(
        Response(body=adv_store_message, status=200)
    )

    billing_message = OrdersStatInfo(
        orders_info=[OrderStatInfo(order_id=567382, balance="100")]
    ).SerializeToString()
    billing_receive_orders_rmock(Response(body=billing_message, status=200))

    setup_normalized_db(
        ch_client,
        (
            # utc
            (1, dt_timestamp("2019-06-19 23:59:59")),  # yesterday
            (1, dt_timestamp("2019-06-20 00:30:00")),  # today
            (1, dt_timestamp("2019-06-21 00:00:00")),  # tomorrow
            # Hawaii, -10:00
            (2, dt_timestamp("2019-06-20 09:59:59")),  # yesterday
            (2, dt_timestamp("2019-06-20 10:30:00")),  # today
            (2, dt_timestamp("2019-06-21 10:00:00")),  # tomorrow
            # Moscow, +03:00
            (3, dt_timestamp("2019-06-19 20:59:59")),  # yesterday
            (3, dt_timestamp("2019-06-19 21:30:00")),  # today
            (3, dt_timestamp("2019-06-19 21:00:00")),  # tomorrow
            # Kamchatka, +12:00
            (4, dt_timestamp("2019-06-19 11:59:59")),  # yesterday
            # today (inside interval)
            (4, dt_timestamp("2019-06-19 12:30:00")),
            # today (inside interval)
            (4, dt_timestamp("2019-06-19 12:31:00")),
            # today (outside interval)
            (4, dt_timestamp("2019-06-19 13:00:00")),
            # today (outside interval)
            (4, dt_timestamp("2019-06-19 14:00:00")),
            # today (outside interval)
            (4, dt_timestamp("2019-06-19 15:00:00")),
            (4, dt_timestamp("2019-06-20 12:00:00")),  # tomorrow
        ),
    )

    setup_charged_db(
        ch_client,
        (
            # utc
            (1, dt_timestamp("2019-06-19 23:59:59"), Decimal(1)),  # yesterday
            (1, dt_timestamp("2019-06-20 00:30:00"), Decimal(1)),  # today
            (1, dt_timestamp("2019-06-21 00:00:00"), Decimal(1)),  # tomorrow
            # Hawaii, -10:00
            (2, dt_timestamp("2019-06-20 09:59:59"), Decimal(1)),  # yesterday
            (2, dt_timestamp("2019-06-20 10:30:00"), Decimal(1)),  # today
            (2, dt_timestamp("2019-06-21 10:00:00"), Decimal(1)),  # tomorrow
            # Moscow, +03:00
            (3, dt_timestamp("2019-06-19 20:59:59"), Decimal(1)),  # yesterday
            (3, dt_timestamp("2019-06-19 21:30:00"), Decimal(1)),  # today
            (3, dt_timestamp("2019-06-20 21:00:00"), Decimal(1)),  # tomorrow
            # Kamchatka, +12:00
            (4, dt_timestamp("2019-06-19 11:59:59"), Decimal(1)),  # yesterday
            # today (inside interval)
            (4, dt_timestamp("2019-06-19 12:30:00"), Decimal(1)),
            # today (inside interval)
            (4, dt_timestamp("2019-06-19 12:31:00"), Decimal(1)),
            # today (outside interval)
            (4, dt_timestamp("2019-06-19 13:00:00"), Decimal(1)),
            # today (outside interval)
            (4, dt_timestamp("2019-06-19 14:00:00"), Decimal(1)),
            # today (outside interval)
            (4, dt_timestamp("2019-06-19 15:00:00"), Decimal(1)),
            (4, dt_timestamp("2019-06-20 12:00:00"), Decimal(1)),  # tomorrow
        ),
    )

    task_data = {
        "timing_from": dt("2019-06-19 12:29:00"),
        "timing_to": dt("2019-06-19 12:41:00"),
        "id": 100,
        "status": "accepted",
    }

    await charger_pipeline.collect_data(task_data)

    assert task_data["execution_state"] == [
        {
            "order_id": 567382,
            "budget_balance": Decimal(100),
            "campaigns": [
                # UTC
                {
                    "campaign_id": 1,
                    "tz_name": "UTC",
                    "cpm": Decimal(3),
                    "budget": Decimal(200),
                    "daily_budget": Decimal(20),
                    "charged": Decimal(3),
                    "charged_daily": Decimal(1),
                    "events_count": 0,
                },
                # Hawaii, -10:00
                {
                    "campaign_id": 2,
                    "tz_name": "Pacific/Honolulu",
                    "cpm": Decimal(3),
                    "budget": Decimal(200),
                    "daily_budget": Decimal(20),
                    "charged": Decimal(3),
                    "charged_daily": Decimal(1),
                    "events_count": 0,
                },
                # Moscow, +03:00
                {
                    "campaign_id": 3,
                    "tz_name": "Europe/Moscow",
                    "cpm": Decimal(3),
                    "budget": Decimal(200),
                    "daily_budget": Decimal(20),
                    "charged": Decimal(3),
                    "charged_daily": Decimal(1),
                    "events_count": 0,
                },
                # Kamchatka, +12:00
                {
                    "campaign_id": 4,
                    "tz_name": "Asia/Kamchatka",
                    "cpm": Decimal(3),
                    "budget": Decimal(200),
                    "daily_budget": Decimal(20),
                    "charged": Decimal(7),
                    "charged_daily": Decimal(5),
                    "events_count": 2,
                },
            ],
        }
    ]


async def test_collects_if_empty_campaigns_list(
    adv_store_receive_active_campaigns_rmock, charger_pipeline
):
    adv_store_receive_active_campaigns_rmock(Response(body=b"", status=200))

    task_data = {
        "timing_from": dt(100),
        "timing_to": dt(300),
        "id": 100,
        "status": "accepted",
    }

    await charger_pipeline.collect_data(task_data)

    assert task_data["execution_state"] == []


async def test_includes_campaign_without_events(
    adv_store_receive_active_campaigns_rmock,
    billing_receive_orders_rmock,
    mock_charger_update_task,
    ch_client,
    charger_pipeline,
):
    adv_store_message = CampaignForChargerList(
        campaigns=[
            CampaignForCharger(
                campaign_id=4242,
                order_id=567382,
                cost=Money(value=30000),
                budget=Money(value=3000000),
                daily_budget=Money(value=200000),
                timezone="UTC",
            ),
            CampaignForCharger(
                campaign_id=4356,
                order_id=567382,
                cost=Money(value=40000),
                budget=Money(value=3000000),
                daily_budget=Money(value=200000),
                timezone="UTC",
            ),
        ]
    ).SerializeToString()
    adv_store_receive_active_campaigns_rmock(
        Response(body=adv_store_message, status=200)
    )

    billing_message = OrdersStatInfo(
        orders_info=[OrderStatInfo(order_id=567382, balance="100")]
    ).SerializeToString()
    billing_receive_orders_rmock(Response(body=billing_message, status=200))

    setup_normalized_db(ch_client, ((4242, 86500), (4242, 86600)))

    setup_charged_db(ch_client, ((4242, 86500, Decimal(2)),))

    task_data = {
        "timing_from": dt(86550),
        "timing_to": dt(90000),
        "id": 100,
        "status": "accepted",
    }
    await charger_pipeline.collect_data(task_data)

    assert task_data["execution_state"] == [
        {
            "order_id": 567382,
            "budget_balance": Decimal(100),
            "campaigns": [
                {
                    "campaign_id": 4242,
                    "tz_name": "UTC",
                    "cpm": Decimal(3),
                    "budget": Decimal(300),
                    "daily_budget": Decimal(20),
                    "charged": Decimal(2),
                    "charged_daily": Decimal(2),
                    "events_count": 1,
                },
                {
                    "campaign_id": 4356,
                    "tz_name": "UTC",
                    "cpm": Decimal(4),
                    "budget": Decimal(300),
                    "daily_budget": Decimal(20),
                    "charged": Decimal(0),
                    "charged_daily": Decimal(0),
                    "events_count": 0,
                },
            ],
        }
    ]


async def test_collects_data_for_campaigns_with_optional_order(
    adv_store_receive_active_campaigns_rmock,
    billing_receive_orders_rmock,
    mock_charger_update_task,
    ch_client,
    charger_pipeline,
):
    adv_store_message = CampaignForChargerList(
        campaigns=[
            # with order
            CampaignForCharger(
                campaign_id=1111,
                order_id=567382,
                cost=Money(value=30000),
                budget=Money(value=3000000),
                daily_budget=Money(value=200000),
                timezone="UTC",
            ),
            # without order
            CampaignForCharger(
                campaign_id=2222,
                cost=Money(value=30000),
                budget=Money(value=3000000),
                daily_budget=Money(value=200000),
                timezone="UTC",
            ),
            CampaignForCharger(
                campaign_id=3333,
                cost=Money(value=30000),
                budget=Money(value=3000000),
                daily_budget=Money(value=200000),
                timezone="UTC",
            ),
        ]
    ).SerializeToString()
    adv_store_receive_active_campaigns_rmock(
        Response(body=adv_store_message, status=200)
    )

    billing_message = OrdersStatInfo(
        orders_info=[OrderStatInfo(order_id=567382, balance="100")]
    ).SerializeToString()
    billing_receive_orders_rmock(Response(body=billing_message, status=200))

    setup_normalized_db(ch_client, ((1111, 86600), (2222, 86600), (3333, 86600)))

    task_data = {
        "timing_from": dt(86500),
        "timing_to": dt(90000),
        "id": 100,
        "status": "accepted",
    }

    await charger_pipeline.collect_data(task_data)

    assert task_data["execution_state"] == [
        {
            "order_id": 567382,
            "budget_balance": Decimal(100),
            "campaigns": [
                {
                    "campaign_id": 1111,
                    "tz_name": "UTC",
                    "cpm": Decimal(3),
                    "budget": Decimal(300),
                    "daily_budget": Decimal(20),
                    "charged": Decimal(0),
                    "charged_daily": Decimal(0),
                    "events_count": 1,
                }
            ],
        },
        {
            "order_id": None,
            "budget_balance": Decimal("Inf"),
            "campaigns": [
                {
                    "campaign_id": 2222,
                    "tz_name": "UTC",
                    "cpm": Decimal(3),
                    "budget": Decimal(300),
                    "daily_budget": Decimal(20),
                    "charged": Decimal(0),
                    "charged_daily": Decimal(0),
                    "events_count": 1,
                },
                {
                    "campaign_id": 3333,
                    "tz_name": "UTC",
                    "cpm": Decimal(3),
                    "budget": Decimal(300),
                    "daily_budget": Decimal(20),
                    "charged": Decimal(0),
                    "charged_daily": Decimal(0),
                    "events_count": 1,
                },
            ],
        },
    ]


async def test_collects_data_for_no_order_campaigns(
    adv_store_receive_active_campaigns_rmock,
    billing_receive_orders_rmock,
    mock_charger_update_task,
    ch_client,
    charger_pipeline,
):
    adv_store_message = CampaignForChargerList(
        campaigns=[
            # without order
            CampaignForCharger(
                campaign_id=1111,
                cost=Money(value=30000),
                budget=Money(value=3000000),
                daily_budget=Money(value=200000),
                timezone="UTC",
            )
        ]
    ).SerializeToString()
    adv_store_receive_active_campaigns_rmock(
        Response(body=adv_store_message, status=200)
    )

    setup_normalized_db(ch_client, ((1111, 86600),))

    task_data = {
        "timing_from": dt(86500),
        "timing_to": dt(90000),
        "id": 100,
        "status": "accepted",
    }

    await charger_pipeline.collect_data(task_data)

    assert task_data["execution_state"] == [
        {
            "order_id": None,
            "budget_balance": Decimal("Inf"),
            "campaigns": [
                {
                    "campaign_id": 1111,
                    "tz_name": "UTC",
                    "cpm": Decimal(3),
                    "budget": Decimal(300),
                    "daily_budget": Decimal(20),
                    "charged": Decimal(0),
                    "charged_daily": Decimal(0),
                    "events_count": 1,
                }
            ],
        }
    ]


async def test_raises_for_unknown_response_in_adv_store(
    adv_store_receive_active_campaigns_rmock, charger_pipeline
):
    adv_store_receive_active_campaigns_rmock(Response(body=b"{}", status=404))

    with pytest.raises(UnknownResponse) as exc_info:
        await charger_pipeline.collect_data(
            {"timing_from": dt(0), "timing_to": dt(300), "id": 100}
        )

    assert "Status=404, payload=b'{}'" in exc_info.value.args


async def test_raises_for_unknown_response_in_billing(
    adv_store_receive_active_campaigns_rmock,
    billing_receive_orders_rmock,
    ch_client,
    charger_pipeline,
):
    adv_store_message = CampaignForChargerList(
        campaigns=[
            CampaignForCharger(
                campaign_id=4242,
                order_id=567382,
                cost=Money(value=30000),
                budget=Money(value=2000000),
                daily_budget=Money(value=200000),
                timezone="UTC",
            )
        ]
    ).SerializeToString()
    adv_store_receive_active_campaigns_rmock(
        Response(body=adv_store_message, status=200)
    )

    billing_receive_orders_rmock(Response(body=b"{}", status=404))

    setup_charged_db(
        ch_client,
        (
            (4242, 10, Decimal(2)),
            (4242, 50, Decimal(2)),
            (4242, 86500, Decimal(2)),
            (4242, 95000, Decimal(2)),
            (4356, 350, Decimal(2)),
            (1242, 86500, Decimal(2)),
        ),
    )

    with pytest.raises(UnknownResponse) as exc_info:
        await charger_pipeline.collect_data(
            {
                "timing_from": dt(0),
                "timing_to": dt(300),
                "id": 100,
                "status": "accepted",
            }
        )

    assert "Status=404, payload=b'{}'" in exc_info.value.args


async def test_raises_for_any_exception_in_events_stat(
    adv_store_receive_active_campaigns_rmock,
    billing_receive_orders_rmock,
    mock_events_stat,
    charger_pipeline,
):
    adv_store_message = CampaignForChargerList(
        campaigns=[
            CampaignForCharger(
                campaign_id=4242,
                order_id=567382,
                cost=Money(value=30000),
                budget=Money(value=2000000),
                daily_budget=Money(value=200000),
                timezone="UTC",
            )
        ]
    ).SerializeToString()
    adv_store_receive_active_campaigns_rmock(
        Response(body=adv_store_message, status=200)
    )

    billing_message = OrdersStatInfo(
        orders_info=[
            OrderStatInfo(order_id=567382, balance="100"),
            OrderStatInfo(order_id=423773, balance="300"),
        ]
    ).SerializeToString()
    billing_receive_orders_rmock(Response(body=billing_message, status=200))

    mock_events_stat.side_effect = RuntimeError()

    with pytest.raises(RuntimeError):
        await charger_pipeline.collect_data(
            {
                "timing_from": dt(0),
                "timing_to": dt(300),
                "id": 100,
                "status": "accepted",
            }
        )
