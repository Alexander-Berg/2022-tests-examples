import pytest
from aiohttp.web import json_response
from marshmallow import ValidationError

from maps_adv.stat_controller.client.lib.collector import (
    NoCollectorTaskFound,
    UnknownResponse,
)
from maps_adv.stat_controller.client.tests import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def rmock(rmock):
    return lambda h: rmock("/tasks/collector/", "post", h)


@pytest.mark.parametrize(
    "timing_from, timing_to",
    (
        ["2019-01-01T12:00:00+00:00", "2019-01-01T12:05:00+00:00"],
        ["2019-01-01T12:00:00.000000+00:00", "2019-01-01T12:05:00.000000+00:00"],
    ),
)
async def test_returns_created_task_data(
    timing_from, timing_to, collector_client, rmock
):
    rmock(
        json_response(
            data={"id": 10, "timing_from": timing_from, "timing_to": timing_to},
            status=201,
        )
    )

    got = await collector_client.find_new_task(executor_id="executor0")

    assert got == {
        "id": 10,
        "timing_from": dt("2019-01-01 12:00:00"),
        "timing_to": dt("2019-01-01 12:05:00"),
    }


async def test_request_data_passed_correctly(collector_client, rmock):
    async def _handler(request):
        assert await request.json() == {"executor_id": "executor0"}
        return json_response(
            data={
                "id": 10,
                "timing_from": "2019-01-01T12:00:00+00:00",
                "timing_to": "2019-01-01T12:05:00+00:00",
            },
            status=201,
        )

    rmock(_handler)

    await collector_client.find_new_task(executor_id="executor0")


async def test_raises_if_no_task_found(collector_client, rmock):
    rmock(json_response(data={}, status=200))

    with pytest.raises(NoCollectorTaskFound):
        await collector_client.find_new_task(executor_id="executor0")


async def test_raises_for_unknown_response(collector_client, rmock):
    rmock(json_response(data={"error_message": "Something happened."}, status=555))

    with pytest.raises(UnknownResponse) as exc_info:
        await collector_client.find_new_task(executor_id="executor0")

    assert (
        'Status=555, payload=b\'{"error_message": "Something happened."}\''
    ) in exc_info.value.args


@pytest.mark.parametrize("value", ("", None))
async def test_raises_for_invalid_executor_id(value, collector_client):
    with pytest.raises(ValidationError):
        await collector_client.find_new_task(executor_id=value)
