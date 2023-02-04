from decimal import Decimal

import pytest
from aiohttp.web import Response

from maps_adv.adv_store.api.proto.charger_api_pb2 import (
    CampaignForCharger,
    CampaignForChargerList,
    Money,
)
from maps_adv.billing_proxy.proto.orders_charge_pb2 import (
    OrderChargeOutput,
    OrdersChargeOutput,
)
from maps_adv.billing_proxy.proto.orders_for_stat_pb2 import (
    OrdersStatInfo,
    OrderStatInfo,
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
        (
            # 3456: no events in time scope
            # 4242
            (4242, 86500),  # will have special cost by daily limit
            (4242, 86600),
            (4242, 86700),
            (4242, 86800),
            # 4356
            (4356, 86550),  # will be ignored by order limit
            (4356, 86750),  # will have special cost by order limit
            # 1242
            (1242, 86550),
            (1242, 86555),
            # 3456
            (3456, 86000),  # event out of time scope in past
            (3456, 86900),  # event out of scope in future
        ),
    )


@pytest.fixture
def setup_charged_table(ch_client):
    setup_charged_db(
        ch_client,
        (
            # 4242
            (4242, 10, Decimal(10)),  # prev day
            (4242, 10000, Decimal(10)),
            (4242, 86450, Decimal(10)),  # current day (for UTC+0)
            (4242, 86460, Decimal(10)),
            (4242, 86470, Decimal(10)),
            (4242, 86480, Decimal(10)),
            (4242, 86490, Decimal(10)),  # negative daily charge
            # 4356
            (4356, 86300, Decimal(5)),  # prev day
            (4356, 86450, Decimal(5)),  # current day
            (4356, 150000, Decimal(5)),
            (4356, 150000, Decimal(5)),  # negative charge
            # 1242
            (1242, 86000, Decimal(2)),  # prev day
            (1242, 172000, Decimal(10)),  # current day
            # 7653
            (7653, 86450, Decimal(10)),  # campaign is closed
        ),
    )


@pytest.fixture
def mock_collected_data(
    adv_store_receive_active_campaigns_rmock, billing_receive_orders_rmock
):
    adv_store_message = CampaignForChargerList(
        campaigns=[
            CampaignForCharger(
                campaign_id=4242,
                order_id=567382,
                cost=Money(value=30000000),
                budget=Money(value=2000000),
                daily_budget=Money(value=410000),
                timezone="UTC",
            ),
            CampaignForCharger(
                campaign_id=4356,
                order_id=567382,
                cost=Money(value=50000000),
                budget=Money(value=180000),
                timezone="UTC",
            ),
            CampaignForCharger(
                campaign_id=1242,
                order_id=423773,
                cost=Money(value=50000000),
                budget=Money(value=2000000),
                daily_budget=Money(value=300000),
                timezone="UTC",
            ),
            CampaignForCharger(
                campaign_id=3456,
                order_id=423773,
                cost=Money(value=50000000),
                budget=Money(value=2000000),
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
            OrderStatInfo(order_id=567382, balance="150"),
            OrderStatInfo(order_id=423773, balance="300"),
        ]
    ).SerializeToString()
    billing_receive_orders_rmock(Response(body=billing_message, status=200))


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
def pipeline_from_start(
    setup_normalized_table,
    setup_charged_table,
    mock_charger_find_new_task,
    mock_charger_update_task,
    mock_collected_data,
    adv_store_send_campaigns_to_stop_rmock,
    mock_charging_funds,
    charger_pipeline,
):
    adv_store_send_campaigns_to_stop_rmock(Response(status=200))
    mock_charger_find_new_task.return_value = {
        "timing_from": dt(86500),
        "timing_to": dt(86800),
        "id": 100,
        "status": TaskStatus.accepted,
    }
    mock_charger_update_task.side_effect = [
        {
            "id": 100,
            "timing_from": dt(86500),
            "timing_to": dt(86800),
            "executor_id": "keker",
            "status": TaskStatus.context_received,
        },
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


async def test_full_execution_from_start_with_negative_charge(
    pipeline_from_start, ch_client
):
    await pipeline_from_start()

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
            (4242, 86480, Decimal(10)),
            (4242, 86490, Decimal(10)),  # negative daily charge
            # 4356
            (4356, 86300, Decimal(5)),  # prev day
            (4356, 86450, Decimal(5)),  # current day
            (4356, 150000, Decimal(5)),
            (4356, 150000, Decimal(5)),  # negative charge
            # 1242
            (1242, 86000, Decimal(2)),  # prev day
            (1242, 172000, Decimal(10)),  # current day
            # 7653
            (7653, 86450, Decimal(10)),  # campaign is closed
            # -----
            # new events added
            # 1242
            (1242, 86550, Decimal(5)),
            (1242, 86555, Decimal(5)),
        )
    ]

    assert len(set(existing_data_in_db)) == len(set(expected_events))

    assert set(existing_data_in_db) == set(expected_events)
