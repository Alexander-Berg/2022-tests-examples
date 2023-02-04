from enum import Enum

import pytest
from aiohttp.web import json_response
from marshmallow import ValidationError

from maps_adv.stat_controller.client.lib.normalizer import TaskStatus, UnknownResponse
from maps_adv.stat_controller.client.tests import dt

pytestmark = [pytest.mark.asyncio]

url = "https://example.com/tasks/normalizer/10/"


@pytest.fixture
def rmock(rmock):
    return lambda h: rmock("/tasks/normalizer/10/", "put", h)


class KekEnum(Enum):
    kek = "kek"
    makarek = ""


async def test_request_data_passed_correctly(normalizer_client, rmock):
    async def _handler(request):
        assert await request.json() == {
            "executor_id": "executor0",
            "status": "completed",
        }
        return json_response(
            data={
                "id": 10,
                "timing_from": "2019-05-06T01:00:00.000000+00:00",
                "timing_to": "2019-05-06T01:05:00.000000+00:00",
            }
        )

    rmock(_handler)

    await normalizer_client.update_task(
        task_id=10, executor_id="executor0", status=TaskStatus.completed
    )


async def test_returns_updated_task_data(normalizer_client, rmock):
    rmock(
        json_response(
            data={
                "id": 10,
                "timing_from": "2019-05-06T01:00:00+00:00",
                "timing_to": "2019-05-06T01:05:00+00:00",
            }
        )
    )

    got = await normalizer_client.update_task(
        task_id=10, status=TaskStatus.completed, executor_id="executor0"
    )

    assert got == {
        "id": 10,
        "timing_from": dt("2019-05-06 01:00:00"),
        "timing_to": dt("2019-05-06 01:05:00"),
    }


async def test_raises_for_unknown_response(normalizer_client, rmock):
    rmock(json_response(data={"some": ["Any error text."]}, status=555))

    with pytest.raises(UnknownResponse) as exc_info:
        await normalizer_client.update_task(
            task_id=10, status=TaskStatus.completed, executor_id="executor0"
        )

    assert (
        'Status=555, payload=b\'{"some": ["Any error text."]}\''
    ) in exc_info.value.args


@pytest.mark.parametrize(
    "update",
    (
        {"executor_id": ""},
        {"executor_id": None},
        {"status": KekEnum.kek},
        {"status": KekEnum.makarek},
        {"status": None},
        {"task_id": None},
        {"task_id": ""},
    ),
)
async def test_raises_for_invalid_data(normalizer_client, update):
    data = {"task_id": 10, "status": TaskStatus.completed, "executor_id": "executor0"}
    data.update(update)

    with pytest.raises(ValidationError):
        await normalizer_client.update_task(**data)
