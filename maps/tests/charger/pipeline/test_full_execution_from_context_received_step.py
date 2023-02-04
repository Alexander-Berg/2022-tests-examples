from decimal import Decimal

import pytest
from aiohttp.web import Response

from maps_adv.billing_proxy.proto.orders_charge_pb2 import (
    OrderChargeOutput,
    OrdersChargeOutput,
)
from maps_adv.stat_controller.client.lib.charger import TaskStatus
from maps_adv.stat_tasks_starter.tests.tools import (
    dt,
    make_charged_event,
    setup_charged_db,
    setup_normalized_db,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def setup_normalized_table(ch_client):
    setup_normalized_db(
        ch_client,
        [
            # no events for 3456 campaign in time scope
            (4242, 86500),  # will have special cost by daily limit
            (4242, 86600),
            (4242, 86700),
            (4242, 86800),
            (4356, 86550),  # will be ignored by order limit
            (4356, 86750),  # will have special cost by order limit
            (1242, 86550),
            (1242, 86555),
            (3456, 86000),  # event out of time scope in past
            (3456, 86900),  # event out of scope in future
        ],
    )


@pytest.fixture
def setup_charged_table(ch_client):
    setup_charged_db(
        ch_client,
        (
            (4242, 10, Decimal(10)),  # prev day
            (4242, 10000, Decimal(10)),
            (4242, 86450, Decimal(10)),  # current day
            (4242, 86460, Decimal(10)),
            (4242, 86470, Decimal(10)),
            (4356, 86300, Decimal(5)),  # prev day
            (4356, 86450, Decimal(5)),  # current day
            (4356, 150000, Decimal(5)),
            (1242, 86000, Decimal(2)),  # prev day
            (1242, 172000, Decimal(10)),  # current day
            (7653, 86450, Decimal(10)),  # campaign is closed
        ),
    )


@pytest.fixture
def mock_charging_funds(billing_submit_charges_rmock):
    message = OrdersChargeOutput(
        charge_result=[
            OrderChargeOutput(order_id=567382, success=True),
            OrderChargeOutput(order_id=423773, success=True),
        ]
    ).SerializeToString()
    billing_submit_charges_rmock(Response(body=message, status=201))


@pytest.fixture
def pipeline_from_context_received(
    setup_normalized_table,
    setup_charged_table,
    mock_charging_funds,
    mock_charger_find_new_task,
    mock_charger_update_task,
    adv_store_send_campaigns_to_stop_rmock,
    charger_pipeline,
):
    adv_store_send_campaigns_to_stop_rmock(Response(status=200))
    mock_charger_find_new_task.return_value = {
        "timing_from": dt(86500),
        "timing_to": dt(86800),
        "id": 100,
        "status": TaskStatus.context_received,
        "execution_state": [
            {
                "order_id": 567382,
                "budget_balance": Decimal("15"),
                "campaigns": [
                    {
                        "campaign_id": 4242,
                        "tz_name": "UTC",
                        "cpm": Decimal("3000"),
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("41"),
                        "charged": Decimal("50"),
                        "charged_daily": Decimal("30"),
                        "events_count": 4,
                    },
                    {
                        "campaign_id": 4356,
                        "tz_name": "UTC",
                        "cpm": Decimal("5000"),
                        "budget": Decimal("300"),
                        "daily_budget": Decimal("Infinity"),
                        "charged": Decimal("15"),
                        "charged_daily": Decimal("10"),
                        "events_count": 2,
                    },
                ],
            },
            {
                "order_id": 423773,
                "budget_balance": Decimal("300"),
                "campaigns": [
                    {
                        "campaign_id": 1242,
                        "tz_name": "UTC",
                        "cpm": Decimal("5000"),
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("30"),
                        "charged": Decimal("12"),
                        "charged_daily": Decimal("10"),
                        "events_count": 2,
                    },
                    {
                        "campaign_id": 3456,
                        "tz_name": "UTC",
                        "cpm": Decimal("5000"),
                        "budget": Decimal("200"),
                        "daily_budget": Decimal("30"),
                        "charged": Decimal("0"),
                        "charged_daily": Decimal("0"),
                        "events_count": 0,
                    },
                ],
            },
        ],
    }
    mock_charger_update_task.side_effect = [
        {
            "id": 100,
            "timing_from": dt(86500),
            "timing_to": dt(86800),
            "current_log_id": 102,
            "status": TaskStatus.calculation_completed,
        },
        {
            "id": 100,
            "timing_from": dt(86500),
            "timing_to": dt(86800),
            "current_log_id": 102,
            "status": TaskStatus.billing_notified,
        },
        {
            "id": 100,
            "timing_from": dt(86500),
            "timing_to": dt(86800),
            "current_log_id": 102,
            "status": TaskStatus.charged_data_sent,
        },
        {
            "id": 100,
            "timing_from": dt(86500),
            "timing_to": dt(86800),
            "current_log_id": 102,
            "status": TaskStatus.completed,
        },
    ]

    return charger_pipeline


async def test_full_execution_from_context_received_step(
    pipeline_from_context_received, ch_client
):
    await pipeline_from_context_received()

    existing_data_in_db = ch_client.execute("SELECT * FROM stat.accepted_sample")

    expected_events = [
        make_charged_event(*args)
        for args in (
            # 4242
            (4242, 10, Decimal(10)),  # prev day
            (4242, 10000, Decimal(10)),
            (4242, 86450, Decimal(10)),  # current day (for UTC+0)
            (4242, 86460, Decimal(10)),
            (4242, 86470, Decimal(10)),
            # 4356
            (4356, 86300, Decimal(5)),  # prev day
            (4356, 86450, Decimal(5)),  # current day
            (4356, 150000, Decimal(5)),
            # 1242
            (1242, 86000, Decimal(2)),  # prev day
            (1242, 172000, Decimal(10)),  # current day
            # 7653
            (7653, 86450, Decimal(10)),  # campaign is closed
            # -----
            # new events added
            # 4242
            (4242, 86500, Decimal(2)),
            (4242, 86600, Decimal(3)),
            (4242, 86700, Decimal(3)),
            (4242, 86800, Decimal(3)),
            # 4356
            (4356, 86750, Decimal(4)),
            # 1242
            (1242, 86550, Decimal(5)),
            (1242, 86555, Decimal(5)),
        )
    ]

    assert set(existing_data_in_db) == set(expected_events)
