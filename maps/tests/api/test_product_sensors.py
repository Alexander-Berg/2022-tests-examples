import asyncio
from typing import List, Union

import pytest
from smb.common.http_client import BaseHttpClientException

from maps_adv.common.helpers import AsyncIterator, dt
from maps_adv.geosmb.booking_yang.server.lib.tasks import import_processed_tasks
from maps_adv.geosmb.booking_yang.server.tests.utils import (
    make_pb_order_input,
    make_yang_list_tasks_response,
)

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.real_db,
    # Wednesday, 14:00 Europe/Moscow
    pytest.mark.freeze_time(dt("2019-12-25 11:00:00")),
]


create_order_url = "/v1/orders/"
sensors_url = "/product_sensors/"


def make_create_task_response(orders_count: int) -> List[Union[dict, Exception]]:
    return [
        dict(id=f"{idx}", created_at=dt("2020-01-01 01:01:01"))
        for idx in range(orders_count)
    ]


def make_list_tasks_response(tasks_count: int) -> AsyncIterator:
    return AsyncIterator(
        [
            make_yang_list_tasks_response(task_suite_id=f"{idx}")
            for idx in range(tasks_count)
        ]
    )


async def setup_app(
    api,
    domain,
    yang_mock,
    *,
    created_task_response: List[Union[dict, Exception]],
    list_tasks_response: AsyncIterator,
):
    """Prepare application to integration test:
    - legally create some orders by /orders/ API
    - let background order-related tasks be done
    - import processed tasks from Yang
    """
    yang_mock.create_task_suite.coro.side_effect = created_task_response
    yang_mock.list_accepted_assignments = list_tasks_response

    for _ in range(len(created_task_response)):
        await api.post(
            create_order_url,
            proto=make_pb_order_input(),
            expected_status=201,
        )

    await asyncio.sleep(0.1)

    await import_processed_tasks(domain=domain)


@pytest.mark.parametrize("orders_count", [0, 1, 2])
async def test_returns_metric(api, domain, yang_mock, orders_count):
    await setup_app(
        api,
        domain,
        yang_mock,
        created_task_response=make_create_task_response(orders_count),
        list_tasks_response=make_list_tasks_response(0),
    )

    resp = await api.get(sensors_url, expected_status=200)

    assert resp == {
        "sensors": [
            {
                "labels": {"metric_group": "pending_yang_tasks"},
                "type": "IGAUGE",
                "value": orders_count,
            }
        ]
    }


async def test_metric_ignores_processed_orders(api, domain, yang_mock):
    await setup_app(
        api,
        domain,
        yang_mock,
        created_task_response=make_create_task_response(3),
        list_tasks_response=make_list_tasks_response(3),
    )

    resp = await api.get(sensors_url, expected_status=200)

    await asyncio.sleep(0.1)

    assert resp == {
        "sensors": [
            {
                "labels": {"metric_group": "pending_yang_tasks"},
                "type": "IGAUGE",
                "value": 0,
            }
        ]
    }


async def test_metric_ignores_orders_not_pushed_to_yang(api, domain, yang_mock):
    await setup_app(
        api,
        domain,
        yang_mock,
        created_task_response=[BaseHttpClientException() for _ in range(3)],
        list_tasks_response=make_list_tasks_response(0),
    )

    resp = await api.get(sensors_url, expected_status=200)

    assert resp == {
        "sensors": [
            {
                "labels": {"metric_group": "pending_yang_tasks"},
                "type": "IGAUGE",
                "value": 0,
            }
        ]
    }


async def test_metric_does_not_accumulates(api, domain, yang_mock):
    await setup_app(
        api,
        domain,
        yang_mock,
        created_task_response=make_create_task_response(3),
        list_tasks_response=make_list_tasks_response(0),
    )

    resp = None
    for _ in range(5):
        resp = await api.get(sensors_url, expected_status=200)

    await asyncio.sleep(0.1)

    assert resp == {
        "sensors": [
            {
                "labels": {"metric_group": "pending_yang_tasks"},
                "type": "IGAUGE",
                "value": 3,
            }
        ]
    }
